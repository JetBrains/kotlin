class KotlinClass {
    fun <caret>a(): Int = extension
}

fun test() {
    KotlinClass().a()
}

val KotlinClass.extension: Int get() = 42