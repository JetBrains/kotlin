#import "objcInstancetype.h"

static id returnNil() {
    return nil;
}

static Foo* global = nil;

@implementation Foo
- (instancetype _Nonnull)atReturnType {
    return self;
}

+ (instancetype (^ _Nonnull)(void))atBlockReturnType {
    return ^(){ return [self new]; };
}

- (instancetype (*)(void))atFunctionReturnType {
    return &returnNil;
}

- (__strong instancetype*)atPointerType {
    return &global;
}

- (__strong Foo** (^(^ _Nonnull)(void))(void))atComplexType {
    return (__strong Foo** (^(^ _Nonnull)(void))(void))^(){ return nil; };
}
@end

@implementation Baz
- (instancetype) protocolMethod {
    return self;
}

+ (instancetype _Nonnull) protocolClassMethod {
    return [Baz new];
}
@end