import kotlinApi.KotlinClass
import kotlinApi.KotlinClass.Companion
import kotlinApi.KotlinClass.Companion.staticFun
import kotlinApi.KotlinClass.Companion.staticProperty
import kotlinApi.KotlinClass.Companion.staticVar

internal class C {
    fun foo(): Int {
        staticVar = staticVar * 2
        staticProperty = staticVar + staticProperty
        return staticFun(1)
    }
}