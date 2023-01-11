@interface MyClass
-(void) instanceMethod;
+(void) classMethod;
@end;

@interface MyClass(IncludeCategory)
-(void) categoryInstanceMethod;
+(void) categoryClassMethod;
@end;