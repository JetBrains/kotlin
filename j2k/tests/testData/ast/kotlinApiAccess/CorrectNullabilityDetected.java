//file
import kotlinApi.*;

class A {
    int foo(KotlinClass c) {
        return c.getNullableProperty().length()
               + c.getProperty().length()
               + KotlinClass.object$.getNullableStaticVar()
               + KotlinClass.object$.getStaticVar()
               + KotlinClass.object$.nullableStaticFun(1)
               + KotlinClass.object$.staticFun(1)
               + KotlinApiPackage.nullableGlobalFunction("").length()
               + KotlinApiPackage.globalFunction("").length();
    }
}