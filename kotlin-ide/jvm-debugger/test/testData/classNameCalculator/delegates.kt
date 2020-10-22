/* foo.DelegatesKt */package foo

/* foo.Foo */class Foo {
    val a by lazy /* foo.Foo$a$2 */{ 1 + 2 }

    fun foo() {
        val b by lazy /* foo.Foo$foo$b$2 */{ 3 + 4 }
    }
}

val c by lazy /* foo.DelegatesKt$c$2 */{ 5 + 6 }