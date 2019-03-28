import kotlinApi.*

internal class A {
    fun foo(c: KotlinClass): Int {
        return (c.nullableProperty!!.length
                + c.property.length
                + KotlinClass.nullableStaticVar!!
                + KotlinClass.staticVar
                + KotlinClass.nullableStaticFun(1)!!
                + KotlinClass.staticFun(1)
                + nullableGlobalFunction("")!!.length
                + globalFunction("").length)
    }
}