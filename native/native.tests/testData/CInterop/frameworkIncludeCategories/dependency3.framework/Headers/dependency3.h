@interface MyClass
- (id)initWithFloat:(float)number;
- (instancetype)initWithPointer:(void*)any;
@end;

@interface MyClass(IncludeCategory)
- (id)initWithAnother:(MyClass*) instance;
@end;
