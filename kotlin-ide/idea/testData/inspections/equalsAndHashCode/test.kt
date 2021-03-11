class C1 {
    override fun equals(other: Any?) = true
}

class C2 {
    override fun hashCode() = 0
}

class C3 {
    override fun equals(other: Any?) = true
    override fun hashCode() = 0
}

object O1 {
    override fun equals(other: Any?) = true
}

object O2 {
    override fun hashCode() = 0
}

object O3 {
    override fun equals(other: Any?) = true
    override fun hashCode() = 0
}

class C4 {
    override fun equals(other: С4) = true
}

interface I {
    override fun equals(other: Any?) = true
    override fun hashCode() = 0
}

enum E {
    override fun equals(other: Any?) = true
    override fun hashCode() = 0
}

abstract class T

object O4 : A() {
    override fun equals(other: Any?) = true
}

object O5 : A() {
    override fun hashCode() = 0
}