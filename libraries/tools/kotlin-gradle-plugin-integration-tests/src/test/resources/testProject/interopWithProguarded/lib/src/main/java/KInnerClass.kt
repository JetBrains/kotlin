package kclass

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

public class KInnerClass {
    @Retention(RetentionPolicy.RUNTIME)
    annotation class Foo

    inner class Inner(@Foo val foo: String){}
}
