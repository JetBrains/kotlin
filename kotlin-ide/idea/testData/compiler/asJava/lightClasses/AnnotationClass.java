@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Anno {
    int i();

    int j() default 5;

    java.lang.String value() default "a";

    double d() default 0.0;

    int[] ia();

    int[] ia2() default {1, 2, 3};
}