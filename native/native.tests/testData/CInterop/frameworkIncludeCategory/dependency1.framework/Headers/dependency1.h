@interface MyClass
-(void) instanceMethod;
+(void) classMethod;
@end;

@interface MyClass(IncludeCategory)
-(void) categoryInstanceMethod;
+(void) categoryClassMethod;
@end;

@interface MyClass(IncludeCategory2)
-(void) categoryInstanceMethod2;
+(void) categoryClassMethod2;
@end;

@interface SkipClass
-(void) instanceMethod;
+(void) classMethod;
@end

@interface SkipClass(IncludeCategory)
-(void) categoryInstanceMethod;
+(void) categoryClassMethod;
@end;
