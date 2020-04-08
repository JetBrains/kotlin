public @interface Ann {
    int value();
    String arg1();
    Class<?> arg2();
    int arg3() default 0;
}
