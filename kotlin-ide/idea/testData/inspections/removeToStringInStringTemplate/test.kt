val z = "a${"b".toString()}"

open class X {
    override fun toString() = "X()"
}

class Y : X() {
    override fun toString() = "Y() ${super.toString()}"
}