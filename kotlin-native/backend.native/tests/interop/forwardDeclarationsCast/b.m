#import "b.h"

@implementation ForwardDeclaredProtocolImpl : NSObject
@end;

@implementation ForwardDeclaredClass : NSObject
@end;

id<ForwardDeclaredProtocol> produceProtocol() {
  return [ForwardDeclaredProtocolImpl new];
}

ForwardDeclaredClass* produceClass() {
  return [ForwardDeclaredClass new];
}

struct ForwardDeclaredStruct S;

struct ForwardDeclaredStruct* produceStruct() {
  return &S;
}