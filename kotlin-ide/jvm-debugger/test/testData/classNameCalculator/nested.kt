/* NestedKt */fun foo() {
    block /* NestedKt$foo$1 */{
        block /* NestedKt$foo$1$1 */{
            block /* NestedKt$foo$1$1$1 */{
                block /* NestedKt$foo$1$1$1$1 */{
                    block /* NestedKt$foo$1$1$1$1$1 */{
                        block /* NestedKt$foo$1$1$1$1$1$1 */{
                            block /* NestedKt$foo$1$1$1$1$1$1$1 */{
                                print("foo")
                            }
                        }
                    }
                }

                block /* NestedKt$foo$1$1$1$2 */{
                    block /* NestedKt$foo$1$1$1$2$1 */{
                        print("bar")
                    }
                }
            }

            block /* NestedKt$foo$1$1$2 */{
                print("baz")
            }
        }
    }
}

fun block(block: () -> Unit) {
    block()
}