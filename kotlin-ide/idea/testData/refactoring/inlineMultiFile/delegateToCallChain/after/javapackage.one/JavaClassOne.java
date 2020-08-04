package javapackage.one

import javapackage.two.JavaClassTwo;
import kotlinpackage.one.KotlinClassOne;
import kotlinpackage.two.KotlinClassTwo;

public class JavaClassOne {

    public Integer otherMethod() {
        return 42;
    }

    public JavaClassTwo toJavaClassTwo() {
        return new JavaClassTwo();
    }
}