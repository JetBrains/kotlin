@interface MyClass
@end

@interface MyClass (MyClassCategory)
-(instancetype) init;
-(void) instanceMethod;
+(void) classMethod;
@property int instanceProperty;
@property (class) id classProperty;
@end