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
*
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
*
## 施工中
