#import "Foundation/NSString.h"
#import "Foundation/NSObject.h"

@interface Foo : NSObject
- (instancetype _Nonnull)atReturnType;

// https://youtrack.jetbrains.com/issue/KT-59597/KN-Usage-of-instancetype-in-block-return-type-crashes
+ (instancetype (^ _Nonnull)(void))atBlockReturnType;

- (instancetype (*)(void))atFunctionReturnType;
- (__strong instancetype*)atPointerType;
- (__strong instancetype** (^(^ _Nonnull)(void))(void))atComplexType;

//All these declarations are prohibited by Clang:
//@property (class) instancetype classProperty;
//@property instancetype instanceProperty;
//- (void (^)(instancetype))atBlockParameterType;
//- (void (*)(instancetype))atFunctionParameterType;
//- (struct { instancetype f; })atStructField;
//- (instancetype[])atArrayElementType;
//- (instancetype[42])atConstArrayElementType;
@end

@protocol Bar
- (instancetype) protocolMethod;
+ (instancetype _Nonnull) protocolClassMethod;
@end

@interface Baz : Foo <Bar>
@end