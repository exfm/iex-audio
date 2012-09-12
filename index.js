(function(){
"use strict";

// constructor
function IEXAudio(el, opts){
    console.log('iex-audio constructor');
    
    var EventEmitter = require('event-emitter');
    var eventEmitter = new EventEmitter();
    $.extend(this, eventEmitter);
    
    // boolean to keep paused state
    this.paused = true;
    
    // internal var for audio source
    this.theSrc = '';
    
    // internal var for audio currentTime
    this.theCurrentTime = 0;
    
    // audio duration
    this.duration = 0;
    
    // audio volume
    this.volume = 1;
    
    this.addGettersAndSetters();
    // register event listener with native
    cordovaRef.exec(this.eventHandler.bind(this), this.errorHandler.bind(this), "IEXAudio", "eventHandler", []);
    console.log('IEXAudio installed');
}

// send to native
IEXAudio.prototype.sendToNative = function(func, vars){
    cordovaRef.exec(null, null, "IEXAudio", func, vars);
}

// getters & setters
IEXAudio.prototype.addGettersAndSetters = function(){
    
    // audio source
    Object.defineProperty(this, "src", {
        get: function() {
            return this.theSrc;
        },
        set: function(url) {
            this.theSrc = url;
            IEXAudio.prototype.sendToNative("setSource", [url]);
        },
        enumerable : true
    });
    
    // audio currentTime
    Object.defineProperty(this, "currentTime", {
        get: function() {
            return this.theCurrentTime;
        },
        set: function(time) {
            IEXAudio.prototype.sendToNative("setCurrentTime", [time]);
        },
        enumerable : true
    });
}

// load a new song
IEXAudio.prototype.load = function(){
    IEXAudio.prototype.sendToNative("load", []);
};

// play a song
IEXAudio.prototype.play = function(){
    IEXAudio.prototype.sendToNative("play", []);
};

// pause the current playing song
IEXAudio.prototype.pause = function(){
    IEXAudio.prototype.sendToNative("pause", []);
};

// set nowPlaying. Sets lock screen on iOS
IEXAudio.prototype.nowPlaying = function(artist, title, album, imageUrl){
    IEXAudio.prototype.sendToNative("nowPlaying", [artist, title, album, imageUrl]);
};

// listen for events from native. emit out to js listeners
IEXAudio.prototype.eventHandler = function(event){
    switch (event.name){
        case 'timeupdate':
            this.theCurrentTime = event.currentTime;
        break;
        case 'durationchanged':
            this.duration = event.duration;
        break;
        case 'pause':
            this.paused = true;
        break;
        case 'playing':
            this.paused = false;
        break;
        case 'error':
            throw new TypeError(JSON.stringify(event));
        break;
        default:
            
        break;
    }
    this.emit(event.name,
        {
            'target': 
                {
                    'src': this.theSrc,
                    'paused': this.paused,
                    'currentTime': this.theCurrentTime,
                    'duration': this.duration,
                    'volume': this.volume
                }
        }
    );
}

// listen for errors from native. Throw js error
IEXAudio.prototype.errorHandler = function(event){
    throw new TypeError(event.error);
}


// register plugin with cordova
// check if we've got require
/*
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
*/
// check if we've got require
if(typeof module !== "undefined"){
    module.exports = IEXAudio;
}
else{
    window.IEXAudio = IEXAudio;
}
console.log('iex-audio');
}()); // end wrapper