class MyClass {
    <warning descr="SSR">val fooThree: (Int, String) -> Unit = { i: Int, s: String -> }</warning>
    val fooTwo: (String) -> Unit = {}
    val fooOne: () -> Unit = {}
}