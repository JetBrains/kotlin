package pkg;

public @interface FooImpl {
    String value() default "abc";  // should be OK
}