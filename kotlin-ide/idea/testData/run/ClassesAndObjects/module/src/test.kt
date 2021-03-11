package q

// RUN: q.Foo
object Foo {
    // RUN: q.Foo
    @JvmStatic fun main(s: Array<String>) {
        println("Foo")
    }

    // RUN: q.Foo$InnerFoo
    class InnerFoo {
        companion object {
            // RUN: q.Foo$InnerFoo
            @JvmStatic fun main(s: Array<String>) {
                println("InnerFoo")
            }
        }
    }

    // RUN: q.Foo
    class InnerFoo2 {
        // RUN: q.Foo
        @JvmStatic fun main(s: Array<String>) {
            println("InnerFoo")
        }
    }
}

// RUN: q.TestKt
object Foo2 {
    // RUN: q.TestKt
    fun main(s: Array<String>) {
        println("Foo2")
    }
}

// RUN: q.Bar
class Bar {
    companion object {
        // RUN: q.Bar
        @JvmStatic fun main(s: Array<String>) {
            println("Bar")
        }
    }
}

// RUN: q.TestKt
class Bar2 {
    companion object {
        // RUN: q.TestKt
        fun main(s: Array<String>) {
            println("Bar2")
        }
    }
}

// RUN: q.TestKt
class Baz {
    // RUN: q.TestKt
    @JvmStatic fun main(s: Array<String>) {
        println("Baz")
    }
}

// RUN: q.TestKt
class Baz2 {
    // RUN: q.TestKt
    fun main(s: Array<String>) {
        println("Baz2")
    }
}

// RUN: q.TestKt
fun main(s: Array<String>) {
    println("Top-level")
}