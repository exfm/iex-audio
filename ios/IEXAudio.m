//
//  IEXAudio.m
//  iphone-phonegap
//

#import "IEXAudio.h"
#import <AudioToolbox/AudioToolbox.h>
#import <MediaPlayer/MediaPlayer.h>
#import <objc/runtime.h>

static NSString *kIEXAudioEventDurationChanged = @"durationchanged";
static NSString *kIEXAudioEventCanPlay = @"canplay";
static NSString *kIEXAudioEventCanPlayThrough = @"canplaythrough";
static NSString *kIEXAudioEventPaused = @"pause";
static NSString *kIEXAudioEventProgress = @"progress";
static NSString *kIEXAudioEventPlaying = @"playing";
static NSString *kIEXAudioEventEnded = @"ended";
static NSString *kIEXAudioEventError = @"error";
static NSString *kIEXAudioEventPlay = @"play";
static NSString *kIEXAudioEventSeeking = @"seeking";
static NSString *kIEXAudioEventSeeked = @"seeked";
static NSString *kIEXAudioEventStalled = @"stalled";
static NSString *kIEXAudioEventTimeUpdate = @"timeupdate";
static NSString *kIEXAudioEventWaiting = @"waiting";
static NSString *kIEXAudioEventLoadStart = @"loadstart";
static NSString *kIEXAudioEventLoadedMetadata = @"loadedmetadata";
static NSString *kIEXAudioEventRemoteNext = @"remotenext";
static NSString *kIEXAudioEventRemotePrevious = @"remoteprevious";

NSString *kIEXAudioRemoteEventPlay = @"kIEXAudioRemoteEventPlay";
NSString *kIEXAudioRemoteEventNext = @"kIEXAudioRemoteEventNext";
NSString *kIEXAudioRemoteEventPrevious = @"kIEXAudioRemoteEventPrevious";

void audioRouteChangeListenerCallback (void *inUserData,
                                       AudioSessionPropertyID inPropertyID,
                                       UInt32                 inPropertyValueSize,
                                       const void             *inPropertyValue) 
{
    IEXAudio *player = (IEXAudio *)inUserData;
    
    if (player.status == IEXAudioPlayerPaused) {
        return;
    }
    
    CFDictionaryRef routeChangeDictionary = inPropertyValue;
    CFNumberRef routeChangeReasonRef =
    CFDictionaryGetValue (routeChangeDictionary,
                          CFSTR (kAudioSession_AudioRouteChangeKey_Reason));
    
    SInt32 routeChangeReason;
    CFNumberGetValue (routeChangeReasonRef, kCFNumberSInt32Type, &routeChangeReason);
    
    if (routeChangeReason == kAudioSessionRouteChangeReason_OldDeviceUnavailable) {
        [player pause:nil withDict:nil];
    }
}

@implementation IEXAudio

@synthesize url;
@synthesize callback;
@synthesize player;
@synthesize playerItem;
@synthesize asset;
@synthesize status;

- (void)setup {
    player = nil;
    playerItem = nil;
    
    interrupted = NO;
    status = IEXAudioPlayerStopped;

    [[UIApplication sharedApplication] beginReceivingRemoteControlEvents];
    
    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    [audioSession setCategory:AVAudioSessionCategoryPlayback error:nil];
    audioSession.delegate = self;
    
    AudioSessionAddPropertyListener(kAudioSessionProperty_AudioRouteChange,
                                    audioRouteChangeListenerCallback, self);
    
    NSNotificationCenter *nc = [NSNotificationCenter defaultCenter];
    [nc addObserver:self selector:@selector(remoteEventPlay) name:kIEXAudioRemoteEventPlay object:nil];
    [nc addObserver:self selector:@selector(remoteEventNext) name:kIEXAudioRemoteEventNext object:nil];
    [nc addObserver:self selector:@selector(remoteEventPrevious) name:kIEXAudioRemoteEventPrevious object:nil];
}

- (CDVPlugin *)initWithWebView:(UIWebView *)theWebView settings:(NSDictionary *)classSettings {
    self = [super initWithWebView:theWebView settings:classSettings];
    if (self) {
        [self setup];
    }
    
    return self;
}

- (CDVPlugin *)initWithWebView:(UIWebView *)theWebView {
    self = [super initWithWebView:theWebView];
    if (self) {
        [self setup];
    }
    
    return self;
}

- (void)dealloc {
    NSNotificationCenter *nc = [NSNotificationCenter defaultCenter];
    [nc removeObserver:self];
    
    self.url = nil;
    self.callback = nil;
    self.asset = nil;
    
    [super dealloc];
}

- (void)sendDictionaryToCallback:(NSDictionary *)dict {
    if (callback) {
        dispatch_async(dispatch_get_main_queue(), ^{
            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                          messageAsDictionary:dict];
            [pluginResult setKeepCallbackAsBool:YES];
            
            [self success:pluginResult callbackId:callback];
        });
    }
}

- (void)sendEventToCallback:(NSString *)event {
    NSDictionary *response = [NSDictionary dictionaryWithObjectsAndKeys:event, @"name", nil];
    [self sendDictionaryToCallback:response];
}

- (void)sendErrorToCallback:(NSString *)message withStatus:(NSString *)code {
    NSDictionary *errorDict = [NSDictionary dictionaryWithObjectsAndKeys:
                               @"load", @"method",
                               kIEXAudioEventError, @"name",
                               @"status", code,
                               @"message", message,
                               nil];
    [self sendDictionaryToCallback:errorDict];
}

- (void)loadDuration {
    [asset loadValuesAsynchronouslyForKeys:[NSArray arrayWithObject:@"duration"] 
                         completionHandler:^{
                             NSError *error = nil;
                             AVKeyValueStatus trackStatus = [asset statusOfValueForKey:@"duration" error:&error];
                             if (trackStatus == AVKeyValueStatusLoaded) {
                                 NSNumber *seconds = [NSNumber numberWithFloat:CMTimeGetSeconds(playerItem.duration)];
                                 NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
                                                        kIEXAudioEventDurationChanged, @"name",
                                                        seconds, @"duration",
                                                        nil];
                                 [self sendDictionaryToCallback:event];
                             }
                         }];
}

- (void)registerTimeObserver {
    if (player) {
        timeObserver = [player addPeriodicTimeObserverForInterval:CMTimeMakeWithSeconds(1, 2)
                                                        queue:dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_LOW, 0) 
                                                   usingBlock:
                    ^(CMTime time) {                    
                        CMTime currentTime = player.currentTime;
                        if (!CMTIME_IS_VALID(currentTime)) {
                            return;
                        }
                        
                        NSNumber *seconds = [NSNumber numberWithFloat:CMTimeGetSeconds(currentTime)];
                        NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
                                               kIEXAudioEventTimeUpdate, @"name",
                                               seconds, @"currentTime",
                                               nil];
                        [self sendDictionaryToCallback:event];
                    }];            
        [timeObserver retain];
    }
}

- (void)destroyPlayer {
    if (playerItem) {
        [[NSNotificationCenter defaultCenter] removeObserver:self 
                                                        name:AVPlayerItemDidPlayToEndTimeNotification
                                                      object:playerItem];
        
        [playerItem removeObserver:self forKeyPath:@"status"];
        [playerItem removeObserver:self forKeyPath:@"playbackLikelyToKeepUp"];
        [playerItem removeObserver:self forKeyPath:@"loadedTimeRanges"];
        [playerItem release];
        playerItem = nil;
    }
    
    if (player) {
        if (timeObserver) {
            [player removeTimeObserver:timeObserver];
            [timeObserver release];
            timeObserver = nil;
        }
        
        [player removeObserver:self forKeyPath:@"status"];
        [player removeObserver:self forKeyPath:@"rate"];
        [player release];
        player = nil;
    }
}

- (void)setSource:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options {
    self.url = [arguments objectAtIndex:1];
    networkState = IEXAudioNetworkStateEmpty;
}

- (void)load:(NSMutableArray *)arguments withDict:(NSMutableDictionary*)options {
    if (url == nil) {
        [self sendErrorToCallback:@"Source must be set before calling load()."
                       withStatus:@"NO_SOURCE"];
        return;
    }
    
    self.status = IEXAudioPlayerBuffering;
    self.asset = [AVURLAsset URLAssetWithURL:[NSURL URLWithString:self.url] options:nil];    

    [self destroyPlayer];
    [self sendEventToCallback:kIEXAudioEventLoadStart];
        
    NSLog(@"Loading: %@", self.url);
    
    // Request background time so that the app doesn't suspend while
    // the song is buffering.
    UIBackgroundTaskIdentifier newTaskId = UIBackgroundTaskInvalid;
    newTaskId = [[UIApplication sharedApplication] beginBackgroundTaskWithExpirationHandler:^{ 
        [[UIApplication sharedApplication] endBackgroundTask:backgroundTaskId]; 
        backgroundTaskId = UIBackgroundTaskInvalid;
    }];
    
    if (backgroundTaskId != UIBackgroundTaskInvalid) {
        [[UIApplication sharedApplication] endBackgroundTask:backgroundTaskId];
    }
    
    backgroundTaskId = newTaskId;
    
    [asset loadValuesAsynchronouslyForKeys:[NSArray arrayWithObject:@"tracks"] 
                         completionHandler:^{
                             NSError *error = nil;
                             AVKeyValueStatus trackStatus = [asset statusOfValueForKey:@"tracks" error:&error];
                             if (trackStatus == AVKeyValueStatusLoaded) {
                                 NSLog(@"Loaded: %@", self.url);
                                 
                                 self.playerItem = [AVPlayerItem playerItemWithAsset:asset];
                                 self.player = [AVPlayer playerWithPlayerItem:playerItem];
                                 
                                 [self registerTimeObserver];
                                 [self loadDuration];
                                 
                                 [player addObserver:self forKeyPath:@"status" options:NSKeyValueObservingOptionNew context:nil];
                                 [player addObserver:self forKeyPath:@"rate" options:NSKeyValueObservingOptionNew context:nil];   
                                                 
                                 [playerItem addObserver:self forKeyPath:@"status" options:NSKeyValueObservingOptionNew context:nil];
                                 [playerItem addObserver:self forKeyPath:@"playbackLikelyToKeepUp" options:NSKeyValueObservingOptionNew context:nil];
                                 [playerItem addObserver:self forKeyPath:@"loadedTimeRanges" options:NSKeyValueObservingOptionNew context:nil];
                                 
                                 [[NSNotificationCenter defaultCenter] addObserver:self
                                                                          selector:@selector(handleAVPlayerItemDidPlayToEndTimeNotification:)
                                                                              name:AVPlayerItemDidPlayToEndTimeNotification
                                                                            object:playerItem];

                                 if (status == IEXAudioPlayerPlaying) {
                                     [player play];
                                 }
                             } else if (trackStatus == AVKeyValueStatusFailed) {
                                 [self sendErrorToCallback:[error localizedFailureReason]
                                                withStatus:@"ERROR_LOADING"];
                             }
                         }];
}

- (void)play:(NSMutableArray*)arguments withDict:(NSMutableDictionary *)options {
    if (player) {
        self.status = IEXAudioPlayerPlaying;
        [player play];
    }
}

- (void)pause:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options {
    if (player) {
        self.status = IEXAudioPlayerPaused;
        [player pause];
    }
}

- (void)stop:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options {
    [self destroyPlayer];
}

- (void)seek:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options {
    if(player) {
        @try {
            NSNumber *seek = [arguments objectAtIndex:1];
            CMTime seekTime = CMTimeMakeWithSeconds([seek floatValue], 1);
            [player seekToTime:seekTime];
        } @catch (id exception) {
            NSLog(@"Failed to seek: %@", exception);
        }
    }
}

- (void)nowPlaying:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options {
    if (NSClassFromString(@"MPNowPlayingInfoCenter")) {            
        NSString *artist = [arguments objectAtIndex:1];
        NSString *songTitle = [arguments objectAtIndex:2];
        NSString *albumTitle = [arguments objectAtIndex:3];
        NSString *imageUrl = [arguments objectAtIndex:4];

        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            NSMutableDictionary *info = [NSMutableDictionary dictionary];
            
            if (artist) [info setObject:artist forKey:MPMediaItemPropertyArtist];
            if (songTitle)  [info setObject:songTitle forKey:MPMediaItemPropertyTitle];
            if (albumTitle)  [info setObject:albumTitle forKey:MPMediaItemPropertyAlbumTitle];
            
            if (imageUrl) {
                NSURLRequest *request = [NSURLRequest requestWithURL:[NSURL URLWithString:imageUrl]];
                NSURLResponse *response = nil;
                NSError *error = nil;
                NSData *data = [NSURLConnection sendSynchronousRequest:request 
                                                      returningResponse:&response 
                                                                  error:&error];

                if (!error) {
                    UIImage *image = [UIImage imageWithData:data];
                    MPMediaItemArtwork *artwork = [[MPMediaItemArtwork alloc] initWithImage:image];
                    [info setObject:artwork forKey:MPMediaItemPropertyArtwork];
                    [artwork release];                    
                }
            }
           
            dispatch_async(dispatch_get_main_queue(), ^{
                MPNowPlayingInfoCenter *np = [MPNowPlayingInfoCenter defaultCenter];
                np.nowPlayingInfo = info;                
            });
        });
    }
}

- (void)eventHandler:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options {
    self.callback = [arguments objectAtIndex:0];
}

#pragma mark -
#pragma mark Observers

- (void)handleAVPlayerItemDidPlayToEndTimeNotification:(NSNotification *)notification {
    [self sendEventToCallback:kIEXAudioEventEnded];
}

- (void)observe_AVPlayer_rate {
    if (player.rate > 0.0) {
        [self sendEventToCallback:kIEXAudioEventPlay];
    } else if (player.rate == 0.0 && self.status == IEXAudioPlayerPaused) {
        [self sendEventToCallback:kIEXAudioEventPaused];
    }
}

- (void)observe_AVPlayer_status {
    if (player.status == AVPlayerStatusReadyToPlay) {
        [self sendEventToCallback:kIEXAudioEventCanPlay];
    } else if (player.status == AVPlayerStatusFailed) {
        [self sendErrorToCallback:[player.error localizedFailureReason]
                       withStatus:@"PLAYER_FAILURE"];
    } else if (player.status == AVPlayerStatusUnknown) {
        NSLog(@"Unknown player status.");
    }
}

- (void)observe_AVPlayerItem_status {
    if (playerItem.status == AVPlayerItemStatusReadyToPlay) {
        [self sendEventToCallback:kIEXAudioEventCanPlay];
    } else if (playerItem.status == AVPlayerItemStatusFailed) {
        [self sendErrorToCallback:[playerItem.error localizedFailureReason]
                       withStatus:@"PLAYER_ITEM_FAILURE"];
    } else if (player.status == AVPlayerItemStatusUnknown) {
        NSLog(@"Unknown player status.");
    }
}

- (void)observe_AVPlayerItem_playbackLikelyToKeepUp {
    [self sendEventToCallback:kIEXAudioEventCanPlayThrough];
}

- (void)observe_AVPlayerItem_loadedTimeRanges {
    NSMutableArray *ranges = [NSMutableArray new];
    for (NSValue *value in playerItem.loadedTimeRanges) {
        CMTimeRange range = [value CMTimeRangeValue];        
        NSNumber *start = [NSNumber numberWithFloat:CMTimeGetSeconds(range.start)];
        NSNumber *duration = [NSNumber numberWithFloat:CMTimeGetSeconds(range.duration)];
        NSDictionary *timeRange = [NSDictionary dictionaryWithObjectsAndKeys:
                                   start, @"start",
                                   duration, @"duration", nil];
        [ranges addObject:timeRange];
    }
    
    NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
                           kIEXAudioEventProgress, @"name",
                           ranges, @"ranges", nil];
    [self sendDictionaryToCallback:event];
}

- (void)observeValueForKeyPath:(NSString *)keyPath 
                      ofObject:(id)object 
                        change:(NSDictionary *)change 
                       context:(void *)context {
    
    NSString *handle = [NSString stringWithFormat:@"observe_%s_%@",
                        class_getName([object class]), 
                        keyPath];
    SEL selector = sel_registerName([handle cStringUsingEncoding:NSUTF8StringEncoding]);
    
    NSLog(@"Observer handle: %@", handle);
    
    if ([self respondsToSelector:selector]) {
        [self performSelector:selector];
    }
}

#pragma mark -
#pragma mark Observers for Remote Events (lock screen play/pause, etc)

- (void)remoteEventPlay {
    if (status == IEXAudioPlayerPlaying) {
        [self pause:nil withDict:nil];
    } else {
        [self play:nil withDict:nil];
    }
}

- (void)remoteEventNext {
    [self sendEventToCallback:kIEXAudioEventRemoteNext];
}

- (void)remoteEventPrevious {
    [self sendEventToCallback:kIEXAudioEventRemotePrevious];
}

#pragma mark -
#pragma mark AVAudioSessionDelegate

- (void)beginInterruption {
    if ((self.status == IEXAudioPlayerPlaying || self.status == IEXAudioPlayerBuffering)) {
        interrupted = YES;
        [self pause:nil withDict:nil];
    }    
}

- (void)endInterruptionWithFlags:(NSUInteger)flags {
    if (interrupted) {
        AudioSessionSetActive(YES);
        [player play];
        interrupted = NO;
    }
}

@end
