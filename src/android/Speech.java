package cordova.plugin.bakaan.speech;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.idst.token.AccessToken;
import com.alibaba.idst.util.NlsClient;
import com.alibaba.idst.util.SpeechRecognizer;
import com.alibaba.idst.util.SpeechRecognizerCallback;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import static android.media.AudioRecord.STATE_UNINITIALIZED;

/**
 * This class echoes a string called from JavaScript.
 */
public class Speech extends CordovaPlugin implements SpeechRecognizerCallback {

    private static final String TAG = "AliSpeechDemo";

    private NlsClient client;
    private Speech instance;
    private SpeechRecognizer speechRecognizer;
    private RecordTask recordTask;

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
        recordTask.stop();
        speechRecognizer.stop();
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
                String format = "Speech.stopRecognizeCallback(%s,%s);";
                String js = String.format(format, fullResult, result);
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
                String format = "Speech.stopRecognizeCallback(%s,%s);";
                String js = String.format(format, fullResult, result);
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

}
