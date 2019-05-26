var exec = require('cordova/exec');


module.exports = {
    startRecognizer: function (message, onSuccess, onError) {
        exec(onSuccess, onError, "Speech", "startRecognizer", [message]);
    },
    stopRecognizer: function (message, onSuccess, onError) {
        exec(onSuccess, onError, "Speech", "stopRecognizer", [message]);
    },
    stopRecognizeCallback: function (data) {
		data = JSON.stringify(data);
		var event = JSON.parse(data);
		cordova.fireDocumentEvent("speech.stopRecognizeCallback", event);
    }
};
