package javapackage.one;

import javapackage.two.JavaClassTwo;
import kotlinpackage.one.KotlinClassOneKt;
import kotlinpackage.two.KotlinClassTwoKt;
import parentpack.JavaParent;

public class JavaClassOne extends JavaParent {
    @Override
    public Integer superClassMethod() {
        return 24;
    }

    public Integer otherMethod() {
        return 42;
    }

    public JavaClassTwo toJavaClassTwo() {
        return new JavaClassTwo();
    }
}