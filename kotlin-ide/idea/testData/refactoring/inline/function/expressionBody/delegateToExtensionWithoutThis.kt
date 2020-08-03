class KotlinClass {
    fun <caret>a(): Int = extension()
}

fun test() {
    KotlinClass().a()
}

fun KotlinClass.extension(): Int = 42
