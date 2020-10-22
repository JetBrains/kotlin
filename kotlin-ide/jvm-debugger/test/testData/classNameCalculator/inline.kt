/* InlineKt */fun foo() {
    block /* InlineKt$foo$1 */{
        inlineBlock /* InlineKt$foo$1$1 */{
            block /* InlineKt$foo$1$1$1 */{
                inlineBlock /* InlineKt$foo$1$1$1$1 */{
                    inlineBlock /* InlineKt$foo$1$1$1$1$1 */{
                        print("foo")
                    }
                }
            }

            inlineBlock /* InlineKt$foo$1$1$2 */{
                block /* InlineKt$foo$1$1$2$1 */{
                    print("bar")
                }
            }
        }
    }
}

fun block(block: () -> Unit) {
    block()
}

fun inlineBlock(block: () -> Unit) {
    block()
}