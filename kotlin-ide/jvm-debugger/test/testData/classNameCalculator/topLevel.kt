/* Boo */
@file:JvmName("Boo")

fun foo() {
    /* Boo$foo$1 */fun bar() {
        print("bar")
    }

    /* Boo$foo$2 */fun baz() {
        print("baz")

        /* Boo$foo$2$1 */fun zoo() {
            print("zoo")
        }

        block /* Boo$foo$2$2 */{
            zoo()
        }
    }

    val boo = /* Boo$foo$boo$1 */fun() {
        print("boo")
    }

    baz()
    bar()
    boo()
}

fun block(block: () -> Unit) {
    block()
}