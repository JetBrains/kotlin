fun a() {
    val f = {
        null!!
    }
    f()
}

fun box() {
    a()
}

// MAIN_CLASS: Kt2489_2Kt
// FILE: kt2489_2.kt
// LINE: 3
