//file
import kotlinApi.*;

class A {
    int foo(KotlinClass c) {
        return c.getNullableProperty().length()
               + c.getProperty().length()
               + KotlinClass.Companion.getNullableStaticVar()
               + KotlinClass.Companion.getStaticVar()
               + KotlinClass.Companion.nullableStaticFun(1)
               + KotlinClass.Companion.staticFun(1)
               + KotlinApiKt.nullableGlobalFunction("").length()
               + KotlinApiKt.globalFunction("").length();
    }
}