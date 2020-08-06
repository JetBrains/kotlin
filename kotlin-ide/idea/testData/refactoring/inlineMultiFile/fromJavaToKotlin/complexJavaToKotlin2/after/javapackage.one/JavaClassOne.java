package javapackage.one;

import usage.KotlinClassOne;
import usage.KotlinClassTwo;

public class JavaClassOne {

    public int field = MAGIC_CONST;

    public int convertToInt() {
        return 42;
    }

    public static JavaClassOne build(int number) {
        return new JavaClassOne();
    }

    public static int MAGIC_CONST = 42;
}