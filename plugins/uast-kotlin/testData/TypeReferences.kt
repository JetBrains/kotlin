fun foo(parameter: Int): String {
    val varWithType: String? = "Not Null"
    val varWithoutType = "lorem ipsum"
    var result = varWithType + varWithoutType
    return result
}


fun <T> parameterizedFoo(arg: T?) {
    val a = arg
    val at: T = arg ?: return

    val tl: List<T> = listOf(at)
    val tsl: List<String> = tl.map { it.toString() }
    val lls: List<List<String>>
    val llt: List<List<T>>

    parameterizedFoo<List<String>>(emptyList())

}
