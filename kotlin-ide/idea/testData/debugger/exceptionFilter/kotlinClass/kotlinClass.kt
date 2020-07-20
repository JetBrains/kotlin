class A {
    fun foo() {
        null!!
    }
}

fun box() {
    A().foo()
}

// MAIN_CLASS: KotlinClassKt
// FILE: kotlinClass.kt
// LINE: 3