package test;

public @interface AnnotationA {
    int value() default (B.INT_VALUE + B.INT_VALUE) ;
}