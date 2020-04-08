package companionObjectWithStatics

class Foo {
    companion object {
        @JvmStatic fun foo() {
        }

        @JvmField val CONST = 111
    }
}