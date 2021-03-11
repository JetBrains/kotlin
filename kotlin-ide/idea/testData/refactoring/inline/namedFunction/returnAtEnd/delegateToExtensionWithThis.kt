class KotlinClass {
    fun <caret>a(): Int {
        return this.extension()
    }
}

fun test() {
    KotlinClass().a()
}

fun KotlinClass.extension(): Int = 42
