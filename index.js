(function(){


// constructor
function IEXAudio(){
    var eventEmitter;
    if(typeof module !== "undefined"){
        var EventEmitter = require('event-emitter');
        eventEmitter = new EventEmitter();
    }
    else{
        eventEmitter = new window.EventEmitter();
    }
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
    
    // audio buffered array
    this.buffered = {
        'length': 0,
        'start': 0,
        'end': 0
    };
    
    this.addGettersAndSetters();
    
      
    // register event listener with native
    window.cordova.exec(this.eventHandler.bind(this), this.errorHandler.bind(this), "IEXAudio", "eventHandler", []);
    console.log('IEXAudio installed');
}

// send to native
IEXAudio.prototype.sendToNative = function(func, vars){
    window.cordova.exec(null, null, "IEXAudio", func, vars);
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
            IEXAudio.prototype.sendToNative("seek", [time]);
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
        case 'play':
            this.paused = false;
        break;
        case 'progress':
            this.buffered.length = event.ranges.length;
            this.buffered.start = event.ranges[0].start;
            this.buffered.end = event.ranges[0].duration;
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
                    'volume': this.volume,
                    'buffered': this.buffered
                }
        }
    );
}

// listen for errors from native. Throw js error
IEXAudio.prototype.errorHandler = function(event){
    throw new TypeError(event.error);
}


if(typeof module !== "undefined"){
    module.exports = IEXAudio;
}
else{
    window.IEXAudio = IEXAudio;
}

}()); // end wrapper