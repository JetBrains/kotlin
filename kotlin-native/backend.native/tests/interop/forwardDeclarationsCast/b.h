#define NS_FORMAT_ARGUMENT(X)
#import <Foundation/Foundation.h>

@protocol ForwardDeclaredProtocol
@end

@interface ForwardDeclaredProtocolImpl : NSObject<ForwardDeclaredProtocol>
@end;


@interface ForwardDeclaredClass : NSObject
@end;

struct ForwardDeclaredStruct {};

id<ForwardDeclaredProtocol> produceProtocol();
ForwardDeclaredClass* produceClass();
struct ForwardDeclaredStruct* produceStruct();