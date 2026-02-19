@protocol MyProtocol
-(void) wasInMyClass;
@end

@interface MyClass
@end

@interface MyClass () <MyProtocol>
@end