import platform.darwin.*

class Foo : NSObject() {
    companion object : NSObjectMeta() {
        fun bar() {
            super.hash()
        }
    }
}
