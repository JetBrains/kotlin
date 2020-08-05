package javapackage.one;

import javapackage.two.JavaClassTwo;
import kotlinpackage.one.KotlinClassOneKt;
import kotlinpackage.two.KotlinClassTwoKt;

public class JavaClassOne {

    public Integer otherMethod() {
        return 42;
    }

    public JavaClassTwo toJavaClassTwo() {
        return new JavaClassTwo();
    }
}