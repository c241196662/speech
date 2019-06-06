var exec = require('cordova/exec');


module.exports = {
    startRecognizer: function (message, onSuccess, onError) {
        exec(onSuccess, onError, "Speech", "startRecognizer", [message]);
    },
    stopRecognizer: function (message, onSuccess, onError) {
        exec(onSuccess, onError, "Speech", "stopRecognizer", [message]);
    },
    startSynthesizer: function (message, onSuccess, onError) {
        exec(onSuccess, onError, "Speech", "startSynthesizer", [message]);
    },
    cancelSynthesizer: function (message, onSuccess, onError) {
        exec(onSuccess, onError, "Speech", "cancelSynthesizer", [message]);
    },
    stopRecognizeCallback: function (data) {
		data = JSON.stringify(data);
		var event = JSON.parse(data);
		cordova.fireDocumentEvent("speech.stopRecognizeCallback", event);
    }
};
