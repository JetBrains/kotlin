package companionObjectUsedInJava

class A {
    companion object {
        @JvmField val CONST = 42
    }
}

class B {
    companion object {
        @JvmStatic fun foo() {
        }
    }
}

class C {
    companion object Named {
        @JvmField val CONST = 42
    }
}

class D {
    companion object Named {
        @JvmStatic fun foo() {
        }
    }
}
