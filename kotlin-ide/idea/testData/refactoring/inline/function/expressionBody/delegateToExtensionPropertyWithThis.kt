class KotlinClass {
    fun <caret>a(): Int = this.extension
}

fun test() {
    KotlinClass().a()
}

val KotlinClass.extension: Int get() = 42