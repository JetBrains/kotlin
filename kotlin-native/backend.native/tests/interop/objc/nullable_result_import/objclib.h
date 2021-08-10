#import <Foundation/Foundation.h>

@interface Resource : NSObject
-(void)async_get_result:(void (^_Nonnull)(NSString *_Nullable_result result, NSError *_Nullable err))completionHandler;
@end
