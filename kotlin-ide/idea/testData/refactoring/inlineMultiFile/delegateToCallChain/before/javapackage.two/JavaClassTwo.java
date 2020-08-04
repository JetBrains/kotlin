package javapackage.two;

import javapackage.one.JavaClassOne;

public class JavaClassTwo {
    public JavaClassTwo returnSelf() {
        return this;
    }

    public JavaClassOne toJavaOne() {
        return new JavaClassOne();
    }
}