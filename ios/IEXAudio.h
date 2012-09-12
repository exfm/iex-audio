//
//  IEXAudio.h
//  iphone-phonegap
//

#import <AVFoundation/AVFoundation.h>

#import <Cordova/CDVPlugin.h>


typedef enum {
    IEXAudioPlayerPlaying,
    IEXAudioPlayerPaused,
    IEXAudioPlayerBuffering,
    IEXAudioPlayerStopped
} IEXAudioPlayerStatus;

typedef enum {
    IEXAudioMediaStateNothing,
    IEXAudioMediaStateMetadata,
    IEXAudioMediaStateCurrentData,
    IEXAudioMediaStateFutureData,
    IEXAudioMediaStateEnoughData
} IEXAudioMediaState;

typedef enum {
    IEXAudioNetworkStateEmpty,
    IEXAudioNetworkStateIdle,
    IEXAudioNetworkStateLoading,
    IEXAudioNetworkStateNoSource
} IEXAudioNetworkState;

extern NSString *kIEXAudioRemoteEventPlay;
extern NSString *kIEXAudioRemoteEventNext;
extern NSString *kIEXAudioRemoteEventPrevious;

@interface IEXAudio : CDVPlugin <AVAudioSessionDelegate> {   
    BOOL interrupted;
    id timeObserver;
    
    IEXAudioPlayerStatus status;
    IEXAudioMediaState readyState;
    IEXAudioNetworkState networkState;
    
    UIBackgroundTaskIdentifier backgroundTaskId;
}

@property (nonatomic, copy) NSString *url;
@property (nonatomic, copy) NSString *callback;
@property (nonatomic, retain) AVPlayer *player;
@property (nonatomic, retain) AVPlayerItem *playerItem;
@property (nonatomic, retain) AVURLAsset *asset;
@property (nonatomic) IEXAudioPlayerStatus status;

- (void)setSource:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options;

- (void)play:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options;

- (void)load:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options;

- (void)pause:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options;

- (void)stop:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options;

- (void)seek:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options;

- (void)nowPlaying:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options;

- (void)eventHandler:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options;

@end
