// NEVER_VALIDATE

class NoPrimaryConstructor {
    val a: Int

    constructor(n: Int) {
        a = n
    }

    constructor(b: Boolean) {
        a = 0
    }
}

class BothConstructors(val a: Int) {
    constructor(b: Boolean) : this(0) {}
}

fun <!VIPER_TEXT!>onlySecondConstructors<!>() {
    val npc1 = NoPrimaryConstructor(true)
    val npc2 = NoPrimaryConstructor(42)
}

fun <!VIPER_TEXT!>primaryAndSecondConstructor<!>() {
    val bc1 = BothConstructors(false)
    val bc2 = BothConstructors(42)
}