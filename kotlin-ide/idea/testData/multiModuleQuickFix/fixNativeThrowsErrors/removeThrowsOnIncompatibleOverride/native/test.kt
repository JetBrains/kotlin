// "Remove annotation" "true"

class MyException : Throwable()

interface Base {
    @Throws(Throwable::class)
    fun foo()
}

class Derived : Base {
    <caret>@Throws(MyException::class)
    override fun foo() {}
}
