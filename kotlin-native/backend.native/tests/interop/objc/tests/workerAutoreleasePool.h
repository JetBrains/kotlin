#import <Foundation/NSObject.h>

@interface CreateAutoreleaseDeallocated : NSObject
@property BOOL value;
@end;

@interface CreateAutorelease : NSObject
+(void)createAutorelease:(CreateAutoreleaseDeallocated*)deallocated;
@end;
