fun a() {
    val f = {
        null!!
    }
    f()
}

fun box() {
    a()
}

// FILE: kt2489_2.kt
// LINE: 3
