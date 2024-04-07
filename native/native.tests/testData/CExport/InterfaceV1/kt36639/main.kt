interface Foo {
    fun Any.extfoo()
}

class CFoo1:Foo {
    override fun Any.extfoo() {
        when(this) {
            is CFoo1 -> println("CFoo1::extfoo")
            is CFoo2 -> println("CFoo2::extfoo")
            is Int -> println("Int::extfoo")
        }
    }

    fun callMe(arg: Any) = arg.extfoo()
}

class CFoo2:Foo {
    override fun Any.extfoo() {
        TODO("Not yet implemented")
    }
}