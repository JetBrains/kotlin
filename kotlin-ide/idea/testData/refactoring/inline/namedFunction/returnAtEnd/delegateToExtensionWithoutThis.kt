class KotlinClass {
    fun <caret>a(): Int {
        return extension()
    }
}

fun test() {
    KotlinClass().a()
}

fun KotlinClass.extension(): Int = 42
