(function(){
"use strict";

// constructor
function IEXAudio(el, opts){
    console.log('iex-audio constructor');
    
    var eventEmitter = new EventEmitter();
    $.extend(this, eventEmitter);
    
    //
    this.paused = true;
    
    //
    this.theSrc = '';
    
    //
    this.theCurrentTime = 0;
    
    // 
    this.duration = 0;
    
    //
    this.volume = 1;
    
    this.addGettersAndSetters();
    // register event listener with native
    cordovaRef.exec(this.eventHandler.bind(this), this.errorHandler.bind(this), "IEXAudio", "eventHandler", []);
    console.log('IEXAudio installed');
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
            cordovaRef.exec(null, null, "IEXAudio", "setSource", [url]);
        },
        enumerable : true
    });
    
    // audio currentTime
    Object.defineProperty(this, "currentTime", {
        get: function() {
            return this.theCurrentTime;
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
console.log('iex-audio');
}()); // end wrapper