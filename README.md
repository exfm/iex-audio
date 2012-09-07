# iex-audio

This object provides access to a native audio player that mimics the HTML5 Audio API. (iOS)
In Cordova.plist/Plugins, add this mapping (key:IEXAudio, value:IEXAudio)

## Install


     npm install iex-audio

## Testing

    git clone 
    npm install
    open test/index.html
    
## Usage

    var audio = new IEXAudio();
    audio.src = 'file.mp3';
    audio.load();
    audio.addEventListener(
        'canplay',
        function(e){
            audio.play();
        }    
    )
