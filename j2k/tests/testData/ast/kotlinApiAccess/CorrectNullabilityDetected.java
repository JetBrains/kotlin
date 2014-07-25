//file
import kotlinApi.*;

class A {
    int foo(KotlinClass c) {
        return c.getNullableProperty().length()
               + c.getProperty().length()
               + KotlinClass.OBJECT$.getNullableStaticVar()
               + KotlinClass.OBJECT$.getStaticVar()
               + KotlinClass.OBJECT$.nullableStaticFun(1)
               + KotlinClass.OBJECT$.staticFun(1)
               + KotlinApiPackage.nullableGlobalFunction("").length()
               + KotlinApiPackage.globalFunction("").length();
    }
}