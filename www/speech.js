var exec = require('cordova/exec');


module.exports = {
    startRecognizer: function (message, onSuccess, onError) {
        exec(onSuccess, onError, "Speech", "startRecognizer", [message]);
    },
    stopRecognizer: function (message, onSuccess, onError) {
        exec(onSuccess, onError, "Speech", "stopRecognizer", [message]);
    },
    stopRecognizeCallback: function (data) {
        if (device.platform === "Android") {
            data = JSON.stringify(data);
            var event = JSON.parse(data);
            cordova.fireDocumentEvent("Speech.stopRecognizeCallback", event);
        }
    }
};
