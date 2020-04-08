<info descr="null">inline</info> fun <T> run(f: () -> T) = f()
fun run2(f: () -> Unit) = f()

fun inline() {
    val x = 1
    run { x }

    val x1 = 1
    run ({ x1 })

    val x2 = 1
    run (<info descr="null">f =</info> { x2 })

    val x3 = 1
    run {
        run {
            x3
        }
    }
}

fun notInline() {
    val y2 = 1
    run { <info descr="Value captured in a closure">y2</info> }
    run2 { <warning><info descr="Value captured in a closure">y2</info></warning> }

    val y3 = 1
    run2 { <warning><info descr="Value captured in a closure">y3</info></warning> }
    run { <info descr="Value captured in a closure">y3</info> }

    // wrapped, using in not inline
    val z = 2
    { <info descr="Value captured in a closure">z</info> }()

    val z1 = 3
    run2 { <warning><info descr="Value captured in a closure">z1</info></warning> }
}

fun nestedDifferent() { // inline within non-inline and vice-versa
    val y = 1
    {
        run {
            <info descr="Value captured in a closure">y</info>
        }
    }()

    val y1 = 1
    run {
        { <info descr="Value captured in a closure">y1</info> }()
    }
}

fun localFunctionAndClass() {
    val u = 1
    fun localFun() {
        run {
            <info descr="Value captured in a closure">u</info>
        }
    }

    val v = 1
    class LocalClass {
        fun f() {
            run {
                <info descr="Value captured in a closure">v</info>
            }
        }
    }
}

fun objectExpression() {
    val u1 = 1
    object : Any() {
        fun f() {
            run {
                <info descr="Value captured in a closure">u1</info>
            }
        }
    }

    val u2 = 1
    object : Any() {
        val prop = run {
            <info descr="Value captured in a closure">u2</info>
        }
    }

    val u3 = ""
    object : Throwable(run { <info descr="Value captured in a closure">u3</info> }) {
    }
}

<info>inline</info> fun withNoInlineParam(<info>noinline</info> task1: () -> Unit, task2: () -> Unit) {
    task1()
    task2()
}

fun usage(param1: Int, param2: Int) {
    withNoInlineParam({ println(<info descr="Value captured in a closure">param1</info>) }, { println(param2) })
}

fun println(<warning>a</warning>: Any) {}