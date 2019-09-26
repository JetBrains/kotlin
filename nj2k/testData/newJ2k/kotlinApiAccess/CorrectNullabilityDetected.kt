import kotlinApi.KotlinClass
import kotlinApi.KotlinClass.Companion.nullableStaticFun
import kotlinApi.KotlinClass.Companion.nullableStaticVar
import kotlinApi.KotlinClass.Companion.staticFun
import kotlinApi.KotlinClass.Companion.staticVar
import kotlinApi.globalFunction
import kotlinApi.nullableGlobalFunction

internal class A {
    fun foo(c: KotlinClass): Int {
        return (c.nullableProperty!!.length
                + c.property.length
                + nullableStaticVar!!
                + staticVar
                + nullableStaticFun(1)!!
                + staticFun(1)
                + nullableGlobalFunction("")!!.length
                + globalFunction("").length)
    }
}