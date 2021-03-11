public @interface Ann {
    int value();
    String arg1();
    Class<?>[] arg2();
    Class<?> arg3();
    int arg4() default 0;
}
