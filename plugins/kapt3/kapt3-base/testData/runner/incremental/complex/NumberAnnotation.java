package test;

public @interface NumberAnnotation {
    Class[] classReferences() default { NumberManager.class};
    MyEnum enumReference() default MyEnum.VALUE;
    BaseAnnotation otherAnnotation() default @test.BaseAnnotation;
}

@interface BaseAnnotation {}