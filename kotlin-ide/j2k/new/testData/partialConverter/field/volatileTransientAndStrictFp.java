class A {
    @Deprecated
    volatile int field1 = 0;

    transient int <caret>field2 = 1;

    // Should work even for bad modifiers
    strictfp double field3 = 2;
}
