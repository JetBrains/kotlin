#import <Foundation/NSObject.h>

@class DeallocListener;

@interface DeallocExecutor : NSObject
@property DeallocListener* deallocListener;
@end

@interface DeallocListener : NSObject
@property (weak) DeallocExecutor* deallocExecutor;
@property BOOL deallocated;
-(BOOL)deallocExecutorIsNil;
@end
