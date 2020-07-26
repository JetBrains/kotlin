open class A {
    open fun <caret>a() = Unit
    fun ds() = a()
}

open class B: A() {
    override fun a() = Unit
    fun c() = a()
}

open class B2: A() {
    override fun a() = Unit
    fun c() = a()
}

class C : B() {
    override fun a() = Unit
}

class C2 : B() {
    override fun a() = Unit
}

interface KotlinInterface {
    fun a()
}

open class B3: A(), KotlinInterface {
    override fun a() = Unit
}

interface NextKotlinInterface : KotlinInterface

open class B4: A(), NextKotlinInterface {
    override fun a() = Unit
}
