/* SuspendKt */suspend fun foo() {
    /* SuspendKt$foo$2 */suspend fun bar() {
        block /* SuspendKt$foo$2$1 */{
            print("bar")
        }
    }

    block /* SuspendKt$foo$3 */{
        print("foo")

        block /* SuspendKt$foo$3$1 */{
            print("foo2")
        }
    }
}

fun block(block: () -> Unit) {
    block()
}