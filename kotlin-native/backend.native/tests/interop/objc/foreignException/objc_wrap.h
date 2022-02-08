#import <Foundation/Foundation.h>

void raiseExc(id name, id reason);
id logExc(id exception);

@interface Foo : NSObject
- (void)instanceMethodThrow:(id)name reason:(id)reason;
+ (void)classMethodThrow:(id)name reason:(id)reason;
@end
