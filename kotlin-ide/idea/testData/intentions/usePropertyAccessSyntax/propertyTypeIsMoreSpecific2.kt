// WITH_RUNTIME

abstract class KotlinClass : JavaInterface {
    override fun getSomething(): String = ""
}

fun foo(k: KotlinClass, p: Any) {
    if (p is String) {
        k.<caret>setSomething(p)
    }
}

