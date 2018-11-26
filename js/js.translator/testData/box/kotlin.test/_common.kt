package common

import kotlin.test.FrameworkAdapter

private val context = TestContext()

fun call(name: String) = context.call(name)

fun raise(name: String): Nothing {
    context.raised(name)
    throw Exception(name)
}

@Suppress("INVISIBLE_MEMBER")
val underscore = kotlin.test.setAdapter(object : FrameworkAdapter {
    override fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) {
        context.suite(name, ignored) { suiteFn() }
    }

    override fun test(name: String, ignored: Boolean, testFn: () -> dynamic) {
        context.test(name, ignored) { returned(testFn()) }
    }
})

class TestContext {
    val log: String
        get() = logHead + (lastRecord ?: "")

    var indentation = ""

    fun suite(name: String, ignored: Boolean = false, body: TestContext.() -> Unit) = indent {
        record("suite(\"$name\"${optionalIgnore(ignored)}) {")
        body.runSafely()
        record("}")
    }


    fun test(name: String, ignored: Boolean = false, body: TestContext.() -> Unit = {}) = indent {
        val num = record("test(\"$name\"${optionalIgnore(ignored)}) {")

        body.runSafely()

        if (!writtenSince(num)) {
            record("test(\"$name\"${optionalIgnore(ignored)})", replaceLast = true)
        }
        else {
            record("}")
        }
    }

    fun call(name: String) = indent {
        record("call(\"$name\")")
    }

    fun raised(msg: String) = indent {
        record("raised(\"$msg\")")
    }

    fun caught(msg: String) = indent {
        record("caught(\"$msg\")")
    }

    fun returned(msg: dynamic) = indent {
        if (msg is String) record("returned(\"$msg\")")
    }

    private fun (TestContext.() -> Unit).runSafely() {
        try {
            this()
        }
        catch (t: Throwable) {
            caught(t.message ?: "")
        }
    }

    private fun indent(body: () -> Unit) {
        val prevIndentation = indentation
        indentation += "    "
        body()
        indentation = prevIndentation
    }


    private var logHead: String = ""
    private var lastRecord: String? = null
    private var counter = 0

    private fun writtenSince(num: Int) = counter > num

    private fun record(s: String, replaceLast: Boolean = false): Int {
        if (!replaceLast && lastRecord != null) {
            logHead += lastRecord
        }

        lastRecord = indentation + s + "\n"

        return ++counter
    }

    private fun optionalIgnore(ignored: Boolean) = if (ignored) ", true" else ""
}

fun checkLog(wrapInEmptySuite: Boolean = true, body: TestContext.() -> Unit): String {
    val expectedContext = TestContext()
    if (wrapInEmptySuite) {
        expectedContext.suite("") {
            body()
        }
    } else {
        expectedContext.body()
    }
    if (context.log != expectedContext.log) {
        return "Failed test structure check. Expected: ${expectedContext.log}; actual: ${context.log}."
    }
    else {
        return "OK"
    }
}