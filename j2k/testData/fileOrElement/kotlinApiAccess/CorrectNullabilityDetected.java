//file
import kotlinApi.*;

class A {
    int foo(KotlinClass c) {
        return c.getNullableProperty().length()
               + c.getProperty().length()
               + KotlinClass.Default.getNullableStaticVar()
               + KotlinClass.Default.getStaticVar()
               + KotlinClass.Default.nullableStaticFun(1)
               + KotlinClass.Default.staticFun(1)
               + KotlinApiPackage.nullableGlobalFunction("").length()
               + KotlinApiPackage.globalFunction("").length();
    }
}