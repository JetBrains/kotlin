package enums

interface Base {
    fun f() {
    }
}

enum class E(val i: Int = 0): Base {
    E1() {
        override fun f() {
        }
    },
    E2(3) {
        override fun f() {
        }
    },
    E3
}