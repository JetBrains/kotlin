package foo

fun MyController(`$scope`: String): String {
    return "Hello " + `$scope` + "!"
}

fun box(): String {
    assertEquals("Hello world!", MyController("world"))
    return "OK"
}
