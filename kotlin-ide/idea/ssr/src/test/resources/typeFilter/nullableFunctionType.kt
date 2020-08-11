fun testFun(body: (() -> Unit)?) {
    <warning descr="SSR">testFun2(body)</warning>
}


fun testFun2(body: (() -> Unit)?) {
    body?.invoke()
}