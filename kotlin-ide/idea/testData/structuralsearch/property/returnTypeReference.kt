class MyClass {
    val funOne: (String) -> String = { it }
    <warning descr="SSR">val funTwo: (String) -> Unit = { print(it) }</warning>
}