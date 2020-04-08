public @interface Ann {
    int value();
    String arg1();
    int arg2() default 0;
}
