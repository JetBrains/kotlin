package foo

fun MyController(`$scope`: String): String {
    return "Hello " + `$scope` + "!"
}

fun box(): String {
    return MyController("world")
}