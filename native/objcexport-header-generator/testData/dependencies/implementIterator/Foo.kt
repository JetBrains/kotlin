class Foo: Iterator<Int> {
    override fun hasNext(): Boolean { return false }
    override fun next(): Int { return 0 }
}