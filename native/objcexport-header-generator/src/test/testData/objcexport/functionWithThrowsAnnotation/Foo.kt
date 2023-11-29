class MyError : Throwable()

@Throws(MyError::class)
fun foo() = Unit

@Throws(Throwable::class)
fun bar() = Unit

@Throws(Throwable::class)
fun someString() = ""