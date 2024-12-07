#import <Foundation/NSObject.h>

@interface DeallocFlagHolder : NSObject
@property BOOL deallocated;
@end

@interface ObjectWithDeallocFlag : NSObject
@property (nonnull) DeallocFlagHolder* deallocFlagHolder;
- (instancetype _Nonnull)sameObject;
@end
