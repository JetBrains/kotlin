internal annotation class An(val value: String)


class Test {
    @get:An(value = "get")
    @set:An(value = "set")
    var id: Int = 0
}
