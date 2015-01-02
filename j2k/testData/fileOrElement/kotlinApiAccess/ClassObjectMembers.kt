import kotlinApi.KotlinClass

class C {
    fun foo(): Int {
        KotlinClass.staticVar = KotlinClass.staticVar * 2
        KotlinClass.staticProperty = KotlinClass.staticVar + KotlinClass.staticProperty
        return KotlinClass.staticFun(1)
    }
}