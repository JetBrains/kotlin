/* CallableReferencesKt */fun foo() {

    block /* CallableReferencesKt$foo$1 */{
        print("foo")
    }

    /* CallableReferencesKt$foo$2 */::block.hashCode()

    block /* CallableReferencesKt$foo$3 */{
        print("bar")
    }

    /* CallableReferencesKt$foo$4 */::block.invoke /* CallableReferencesKt$foo$5 */{
        print("boo")
    }

    block /* CallableReferencesKt$foo$6 */{
        print("zoo")
    }
}

fun block(block: () -> Unit) {
    block()
}