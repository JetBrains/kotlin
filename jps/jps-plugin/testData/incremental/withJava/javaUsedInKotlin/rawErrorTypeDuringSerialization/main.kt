class Main {
    fun init() {
        Foo<Foo<*>>() //this is the call that causes the build exception, if you comment it out everything works fine
            .ctxId("")
    }
}
fun box() = "OK"