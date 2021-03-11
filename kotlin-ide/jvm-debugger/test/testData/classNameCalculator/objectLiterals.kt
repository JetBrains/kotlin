/* ObjectLiteralsKt */fun foo() {
    /* ObjectLiteralsKt$foo$1 */object : Runnable [
        override /* ObjectLiteralsKt$foo$2 */fun run() {
            block /* ObjectLiteralsKt$foo$2$1 */{
                print("foo")
            }
        }
    ]
}

fun block(block: () -> Unit) {
    block()
}