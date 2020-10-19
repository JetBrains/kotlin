fun foo() {
    listOf(1, 2, 3, 4, 5).forEach lit@{
        if (it == 3) <warning descr="SSR">return@lit</warning>
    }
}