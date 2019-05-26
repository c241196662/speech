# speech
## 安装
1. 通过cordova命令行安装 cordova plugin add https://github.com/c241196662/speech.git
  网络不好有极大可能性安装失败
  这时候请用方法2安装
2. 通过git命令行安装
2.1.先下载个github
2.2.在项目根目录下 通过git的命令行安装 git clone https://github.com/c241196662/speech.git
  网络不好的情况下还是有安装失败的可能性
  ----------------------------------
  remote: Enumerating objects: 44, done.
  remote: Counting objects: 100% (44/44), done.
  remote: Compressing objects: 100% (31/31), done.
  error: RPC failed; curl 56 OpenSSL SSL_read: SSL_ERROR_SYSCALL, errno 10054
  fatal: the remote end hung up unexpectedly
  fatal: early EOF
  fatal: unpack-objects failed
  ----------------------------------
  如果报错信息是这样的,那么通过命令行解决:
    git config --global http.postBuffer 524288000
    git config --global http.sslVerify "false"
  输入完成后再次输入2.2.的命令行即可
2.3.通过cordova本地命令行安装 cordova plugin add speech
## 使用
```javascript
var token = '7b7e605d265a40af9f9ad0502ec2ad5f'; // 测试用token, 可能已过期, 过期请联系我生成新的token
var appkey = 'gvMGprU3vTOPzVQC';
// 启动录音
speech.startRecognizer({
	token: token, // 从自己的服务器上获取token,或者从阿里的项目管理中获取token
	appkey: appkey // appkey, 项目的appkey
}, function (msg) {
	// 成功启动录音的返回
	//setaction2('startRecognizer_success_' + msg);
 }, function (e) {
	// 失败启动录音的返回
	//setaction2('startRecognizer_error_' + e);
 });
 // 停止录音, 注意: 停止录音后并不会获取到录音的解析内容, 需要通过监听器来监听底层发送的录音解析结果
 speech.stopRecognizer({}, function (msg) {
	// 成功停止录音的返回
	setaction2('stopRecognizer_success_' + msg);
}, function (e) {
	// 失败停止录音的返回
	setaction2('stopRecognizer_error_' + e);
});
// 录音解析监听
document.addEventListener('speech.stopRecognizeCallback', function (msg) {
	// 获取录音解析的真实内容, 是一个标准的JSON对象
	/*
	{"isTrusted":false,"header":{"namespace":"SpeechRecognizer","name":"RecognitionCompleted","status":20000000,"message_id":"220bd260651b4607b6a051b23ef55fdf","task_id":"0fce0c8faf1d44e6b209e4c98b00507a","status_text":"Gateway:SUCCESS:Success."},"payload":{"result":"测试","duration":1060}}
	*/
	setresult(JSON.stringify(msg));
});
```
## 施工中
