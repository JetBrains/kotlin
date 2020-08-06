package javapackage.one;

import javapackage.two.JavaClassTwo;
import kotlinpackage.one.KotlinClassOneKt;
import kotlinpackage.two.KotlinClassTwoKt;

public class JavaClassOne {
    public Integer a() {
        return KotlinClassTwoKt.extensionSelf(KotlinClassOneKt.extensionSelf(new JavaClassOne()).toJavaClassTwo()).returnSelf().toJavaOne().otherMethod();
    }

    public Integer otherMethod() {
        return 42;
    }

    public JavaClassTwo toJavaClassTwo() {
        return new JavaClassTwo();
    }
}