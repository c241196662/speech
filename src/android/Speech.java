package cordova.plugin.bakaan.speech;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.idst.util.NlsClient;
import com.alibaba.idst.util.SpeechRecognizer;
import com.alibaba.idst.util.SpeechRecognizerCallback;
import com.alibaba.idst.util.SpeechSynthesizer;
import com.alibaba.idst.util.SpeechSynthesizerCallback;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import static android.media.AudioRecord.STATE_UNINITIALIZED;

/**
 * This class echoes a string called from JavaScript.
 */
public class Speech extends CordovaPlugin implements SpeechRecognizerCallback, SpeechSynthesizerCallback {

    private static final String TAG = "AliSpeechDemo";

    private NlsClient client; // 阿里client
    private Speech instance;
    private SpeechRecognizer speechRecognizer; // 语音解析
    private SpeechSynthesizer speechSynthesizer; // 语音播报
    private RecordTask recordTask; // 语音解析任务
    private String[] voices; // 所有语音
    private String chosenVoice; // 使用语音
    private int speechRate; // 语音播报速度
    private static AudioPlayer audioPlayer; // 语音播报器

    private static Context mContext;
    private static Activity cordovaActivity;

    public Speech() {
        instance = this;
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mContext = cordova.getActivity().getApplicationContext();
        cordovaActivity = cordova.getActivity();

        voices = new String[]{
                SpeechSynthesizer.VOICE_AMEI,
                SpeechSynthesizer.VOICE_NINGER,
                SpeechSynthesizer.VOICE_RUOXI,
                SpeechSynthesizer.VOICE_SICHENG,
                SpeechSynthesizer.VOICE_SIJIA,
                SpeechSynthesizer.VOICE_SIQI,
                SpeechSynthesizer.VOICE_SITONG,
                SpeechSynthesizer.VOICE_SIYUE,
                SpeechSynthesizer.VOICE_XIAOBEI,
                SpeechSynthesizer.VOICE_XIAOGANG,
                SpeechSynthesizer.VOICE_XIAOMEI,
                SpeechSynthesizer.VOICE_XIAOMENG,
                SpeechSynthesizer.VOICE_XIAOWEI,
                SpeechSynthesizer.VOICE_XIAOXUE,
                SpeechSynthesizer.VOICE_XIAOYUN,
                SpeechSynthesizer.VOICE_YINA};

        chosenVoice = voices[0];
        audioPlayer = new AudioPlayer();
        //第一步，创建client实例，client只需要创建一次，可以用它多次创建recognizer
        client = new NlsClient();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("startRecognizer")) {
            this.startRecognizer(args.getJSONObject(0), callbackContext);
            return true;
        } else if (action.equals("stopRecognizer")) {
            String message = args.getString(0);
            this.stopRecognizer(message, callbackContext);
            return true;
        } else if (action.equals("startSynthesizer")) {
            this.startSynthesizer(args.getJSONObject(0), callbackContext);
            return true;
        } else if (action.equals("cancelSynthesizer")) {
            String message = args.getString(0);
            this.cancelSynthesizer(message, callbackContext);
            return true;
        }
        return false;
    }


    /**
     * 启动录音和识别
     */
    private void startRecognizer(org.json.JSONObject params, CallbackContext callbackContext) {
        try {
            String token = params.getString("token");
            String appkey = params.getString("appkey");
            if ("".equals(token)) {
                callbackContext.error("startRecognizer with token is empty");
                return;
            }
            if ("".equals(appkey)) {
                callbackContext.error("startRecognizer with appkey is empty");
                return;
            }
            // 第二步，新建识别回调类

            // 第三步，创建识别request
            speechRecognizer = client.createRecognizerRequest(this);
            // 第四步，设置相关参数
            // Token有有效期，请使用https://help.aliyun.com/document_detail/72153.html 动态生成token
//            speechRecognizer.setToken("d3e9bbf64c6b483f92515676cc2e36f6");
            speechRecognizer.setToken(token);
            // 请使用阿里云语音服务管控台(https://nls-portal.console.aliyun.com/)生成您的appkey
//            speechRecognizer.setAppkey("gvMGprU3vTOPzVQC");
            speechRecognizer.setAppkey(appkey);
            // 以下为设置各种识别参数，请按需选择，更多参数请见文档
            // 开启ITN
            speechRecognizer.enableInverseTextNormalization(true);
            // 开启标点
            speechRecognizer.enablePunctuationPrediction(false);
            // 不返回中间结果
            speechRecognizer.enableIntermediateResult(false);
            // 设置打开服务端VAD
            speechRecognizer.enableVoiceDetection(true);
            speechRecognizer.setMaxStartSilence(3000);
            speechRecognizer.setMaxEndSilence(600);
            // 设置定制模型和热词
            // speechRecognizer.setCustomizationId("yourCustomizationId");
            // speechRecognizer.setVocabularyId("yourVocabularyId");
            speechRecognizer.start();

            //启动录音线程
            recordTask = new RecordTask(this);
            recordTask.execute();
            callbackContext.success("startRecognizer");

        } catch (JSONException e) {
            e.printStackTrace();
            callbackContext.error("startRecognizer with JSONException");
        }
    }

    /**
     * 停止录音和识别
     */
    private void stopRecognizer(String message, CallbackContext callbackContext) {
        // 停止录音
        Log.i(TAG, "Stoping recognizer...");
        recordTask.stop();
        speechRecognizer.stop();
        callbackContext.success("stopRecognizer");
    }

    /**
     * 启动语音播报
     *
     * @param params
     * @param callbackContext
     */
    private void startSynthesizer(org.json.JSONObject params, CallbackContext callbackContext) {
        try {
            String token = params.getString("token");
            String appkey = params.getString("appkey");
            String text = params.getString("text");
            if ("".equals(token)) {
                callbackContext.error("startRecognizer with token is empty");
                return;
            }
            if ("".equals(appkey)) {
                callbackContext.error("startRecognizer with appkey is empty");
                return;
            }
            if ("".equals(text)) {
                callbackContext.error("startRecognizer with text is empty");
                return;
            }

            // 第二步，定义语音合成回调类
            // 第三步，创建语音合成对象
            speechSynthesizer = client.createSynthesizerRequest(this);


            Log.i(TAG, "start speechSynthesizer... " + token);
            // 第四步，设置token和appkey
            // Token有有效期，请使用https://help.aliyun.com/document_detail/72153.html 动态生成token
            speechSynthesizer.setToken(token);
            // 请使用阿里云语音服务管控台(https://nls-portal.console.aliyun.com/)生成您的appkey
            speechSynthesizer.setAppkey(appkey);

            // 第五步，设置相关参数，以下选项都会改变最终合成的语音效果，可以按文档调整试听效果
            // 设置人声
            Log.i(TAG, "Set chosen voice " + chosenVoice);
            speechSynthesizer.setVoice(chosenVoice);
            // 设置语速
            Log.i(TAG, "User set speechRate " + speechRate);
            speechSynthesizer.setSpeechRate(speechRate);
            // 设置要转为语音的文本
            speechSynthesizer.setText(text);
            // 设置语音数据采样率
            speechSynthesizer.setSampleRate(SpeechSynthesizer.SAMPLE_RATE_16K);
            // 设置语音编码，pcm编码可以直接用audioTrack播放，其他编码不行
            speechSynthesizer.setFormat(SpeechSynthesizer.FORMAT_PCM);
            // 设置音量
            //speechSynthesizer.setVolume(50);
            // 设置语速
            //speechSynthesizer.setSpeechRate(0);
            // 设置语调
            //speechSynthesizer.setPitchRate(0);

            // 第六步，开始合成
            if (speechSynthesizer.start() < 0) {
                Toast.makeText(cordovaActivity, "启动语音合成失败！", Toast.LENGTH_LONG).show();
                speechSynthesizer.stop();
                return;
            }
            Log.d(TAG, "speechSynthesizer start done");

        } catch (JSONException e) {
            e.printStackTrace();
            callbackContext.error("startRecognizer with JSONException");
        }
    }

    /**
     * 取消语音播报
     *
     * @param message
     * @param callbackContext
     */
    private void cancelSynthesizer(String message, CallbackContext callbackContext) {
        Log.i(TAG, "canceling Synthesizer...");
        speechSynthesizer.cancel();
        audioPlayer.stop();
        callbackContext.success("cancelSynthesizer");
    }

    // 语音识别回调类，用户在这里得到语音识别结果，加入您自己的业务处理逻辑
    // 注意不要在回调方法里执行耗时操作
    @Override
    public void onRecognizedStarted(String msg, int code) {
        Log.d(TAG, "OnRecognizedStarted " + msg + ": " + String.valueOf(code));
    }

    // 请求失败
    @Override
    public void onTaskFailed(String msg, int code) {
        Log.d(TAG, "OnTaskFailed: " + msg + ": " + String.valueOf(code));
        if (recordTask != null) {
            recordTask.stop();
        }
        if (speechRecognizer != null) {
            speechRecognizer.stop();
        }
        if (speechSynthesizer != null) {
            speechSynthesizer.stop();
        }
        final String fullResult = msg;
        cordovaActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String result = null;
                if (!fullResult.equals("")) {
                    JSONObject jsonObject = JSONObject.parseObject(fullResult);
                    if (jsonObject.containsKey("payload")) {
                        result = jsonObject.getJSONObject("payload").getString("result");
                    }
                }
//                mFullEdit.setText(fullResult);
//                mResultEdit.setText(result);
            }
        });
    }

    // 识别返回中间结果，只有enableIntermediateResult(true)时才会回调
    @Override
    public void onRecognizedResultChanged(final String msg, int code) {
        Log.d(TAG, "OnRecognizedResultChanged " + msg + ": " + String.valueOf(code));
        final String fullResult = msg;
        cordovaActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String result = null;
                if (!fullResult.equals("")) {
                    JSONObject jsonObject = JSONObject.parseObject(fullResult);
                    if (jsonObject.containsKey("payload")) {
                        result = jsonObject.getJSONObject("payload").getString("result");
                    }
                }
                String format = "speech.stopRecognizeCallback(%s);";
                String js = String.format(format, fullResult);
                instance.webView.loadUrl("javascript:" + js);
//                mFullEdit.setText(fullResult);
//                mResultEdit.setText(result);
            }
        });
    }

    // 第七步，识别结束，得到最终完整结果
    @Override
    public void onRecognizedCompleted(final String msg, int code) {
        Log.d(TAG, "OnRecognizedCompleted " + msg + ": " + String.valueOf(code));
        recordTask.stop();
        final String fullResult = msg;
        cordovaActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String result = null;
                if (!fullResult.equals("")) {
                    JSONObject jsonObject = JSONObject.parseObject(fullResult);
                    if (jsonObject.containsKey("payload")) {
                        result = jsonObject.getJSONObject("payload").getString("result");
                    }
                }
                String format = "speech.stopRecognizeCallback(%s);";
                String js = String.format(format, fullResult);
                instance.webView.loadUrl("javascript:" + js);
//                mFullEdit.setText(fullResult);
//                mResultEdit.setText(result);
//                button.setEnabled(true);
            }
        });
    }

    // 请求结束，关闭连接
    @Override
    public void onChannelClosed(String msg, int code) {

        Log.d(TAG, "OnChannelClosed " + msg + ": " + String.valueOf(code));
    }

    static class RecordTask extends AsyncTask<Void, Integer, Void> {
        final static int SAMPLE_RATE = 16000;
        final static int SAMPLES_PER_FRAME = 640;

        private boolean sending;

        WeakReference<Speech> speechWeakReference;

        public RecordTask(Speech activity) {
            speechWeakReference = new WeakReference<>(activity);
        }

        public void stop() {
            sending = false;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Speech speech = speechWeakReference.get();
            Log.d(TAG, "Init audio recorder");
            int bufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes * 2);

            if (mAudioRecorder == null || mAudioRecorder.getState() == STATE_UNINITIALIZED) {
                throw new IllegalStateException("Failed to initialize AudioRecord!");
            }
            mAudioRecorder.startRecording();

            ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
            sending = true;
            while (sending) {
                buf.clear();
                // 采集语音
                int readBytes = mAudioRecorder.read(buf, SAMPLES_PER_FRAME);
                byte[] bytes = new byte[SAMPLES_PER_FRAME];
                buf.get(bytes, 0, SAMPLES_PER_FRAME);
                if (readBytes > 0 && sending) {
                    // 第六步，发送语音数据到识别服务
                    int code = speech.speechRecognizer.sendAudio(bytes, bytes.length);
                    if (code < 0) {
                        Log.i(TAG, "Failed to send audio!");
                        speech.speechRecognizer.stop();
                        break;
                    }
                    Log.d(TAG, "Send audio data length: " + bytes.length);
                }
                buf.position(readBytes);
                buf.flip();
            }
            speech.speechRecognizer.stop();
            mAudioRecorder.stop();
            return null;
        }
    }

    // 语音合成开始的回调
    @Override
    public void onSynthesisStarted(String msg, int code) {
        Log.d(TAG, "OnSynthesisStarted " + msg + ": " + String.valueOf(code));
    }

    // 第七步，获取音频数据的回调，在这里把音频写入播放器
    @Override
    public void onBinaryReceived(byte[] data, int code) {
        Log.i(TAG, "binary received length: " + data.length);
        audioPlayer.setAudioData(data);
    }

    // 语音合成完成的回调，在这里关闭播放器
    @Override
    public void onSynthesisCompleted(final String msg, int code) {
        Log.d(TAG, "OnSynthesisCompleted " + msg + ": " + String.valueOf(code));
        // 第八步，结束合成
        speechSynthesizer.stop();
    }
}
