class SimpleClass {
    operator fun invoke<caret>(s: String) = s
}

fun test() {
    val simpleClass = SimpleClass()
    val str = simpleClass("test1")
    val str2 = simpleClass.invoke("test2")
    val str3 = (SimpleClass() as? SimpleClass)?.invoke("test3")
}