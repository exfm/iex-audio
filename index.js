(function(){
"use strict";

// constructor
function IEXAudio(el, opts){
    
    // The data we send on triggered events
    this.eventObj = {
        'src': "",
        'currentTime': 0,
        'duration': 0,
        'paused': true
    }
    
    // register event listener with native
    cordovaRef.exec(this.eventHandler, this.errorHandler, "IEXAudio", "eventHandler", []);
    console.log('IEXAudio installed');
}

// getters & setters
IEXAudio.prototype.addGettersAndSetters = function(){
    
    // audio source
    Object.defineProperty(this, "src", {
        get: function() {
            return this.eventObj.src;
        },
        set: function(url) {
            this.eventObj.src = url;
            cordovaRef.exec(null, null, "IEXAudio", "setSource", [url]);
        },
        enumerable : true
    });
    
    // audio currentTime
    Object.defineProperty(this, "currentTime", {
        get: function() {
            return this.eventObj.currentTime;
        },
        set: function(time) {
            cordovaRef.exec(null, null, "IEXAudio", "setCurrentTime", [time]);
        },
        enumerable : true
    });
}

// load a new song
IEXAudio.prototype.load = function(){
    cordovaRef.exec(null, null, "IEXAudio", "load", []);
};

// play a song
IEXAudio.prototype.play = function(){
    cordovaRef.exec(null, null, "IEXAudio", "play", []);
};

// pause the current playing song
IEXAudio.prototype.pause = function(){
    cordovaRef.exec(null, null, "IEXAudio", "pause", []);
};

// set nowPlaying. Sets lock screen on iOS
IEXAudio.prototype.nowPlaying = function(artist, title, album, imageUrl){
    cordovaRef.exec(null, null, "IEXAudio", "nowPlaying", [artist, title, album, imageUrl]);
};

// listen for events from native. emit out to js listeners
IEXAudio.prototype.eventHandler = function(event){
    switch (event.name){
        case 'timeupdate':
            this.eventObj.currentTime = event.currentTime;
        break;
        case 'durationchanged':
            this.eventObj.duration = event.duration;
        break;
        case 'pause':
            this.eventObj.paused = true;
        break;
        case 'playing':
            this.eventObj.paused = false;
        break;
        case 'error':
            throw new TypeError(JSON.stringify(event));
        break;
        default:
            
        break;
    }
    $(this).trigger(
        {
            'type': event.name,
            'target': this.eventObj
        }
    );
}

// listen for errors from native. Throw js error
IEXAudio.prototype.errorHandler = function(event){
    throw new TypeError(event.error);
}


// register plugin with cordova
// check if we've got require
var cordovaRef = window.PhoneGap || window.Cordova || window.cordova; 
if(cordovaRef){
    cordovaRef.addConstructor(function(){
        if (!window.plugins) {
            window.plugins = {};
        } 
    	window.plugins.IEXAudio = IEXAudio;
    	if(typeof module !== "undefined"){
            module.exports = window.plugins.IEXAudio;
        }
        else{
            window.IEXAudio = window.plugins.IEXAudio;
        }
    });
}
else{
    throw new TypeError("Cordova not found");
}

}()); // end wrapper