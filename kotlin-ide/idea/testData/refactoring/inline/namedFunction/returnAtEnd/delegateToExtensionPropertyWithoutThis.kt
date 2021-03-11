class KotlinClass {
    fun <caret>a(): Int {
        return extension
    }
}

fun test() {
    KotlinClass().a()
}

val KotlinClass.extension: Int get() = 42