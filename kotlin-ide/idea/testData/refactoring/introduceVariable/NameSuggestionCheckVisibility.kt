open class Base {
    private val xxx = 1
}

class For : Base() {
    fun foo(f: For) {
        <selection>f.xxx</selection>
    }
}

val For.xxx: For
    get() = For()
