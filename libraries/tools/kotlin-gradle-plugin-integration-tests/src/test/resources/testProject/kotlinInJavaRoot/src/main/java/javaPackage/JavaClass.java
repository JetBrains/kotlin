package javaPackage;

import kotlinPackage.*;
import kotlinPackage2.*;

public class JavaClass {
    public void foo() {

    }

    public static void main(String[] args) {
        new KotlinClass().foo();
        new KotlinClass2().foo();
    }
}
