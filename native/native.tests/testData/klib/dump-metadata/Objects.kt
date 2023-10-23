// B.Companion and B.C are serialized in a different order in K1 and K2
// MUTED_WHEN: K1
object A {
    fun a() {}
}

class B {
    companion object {
        fun b() {}
    }

    object C {
        fun c() {}
    }
}

class D {
    companion object E {
        fun e() {}
    }
}
