import kotlin.test.FrameworkAdapter
import kotlin.collections.*

private var sortingContext = SortingContext()

private var bodyContext: TestBodyContext? = null

fun call(name: String) = bodyContext!!.call(name)

fun raise(name: String): Nothing {
    bodyContext!!.raised(name)
    throw Exception(name)
}

// Adapter should be initialized eagerly
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")
@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
private val underscore = kotlin.test.setAdapter(object : FrameworkAdapter {
    override fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) {
        sortingContext.suite(name, ignored) { suiteFn() }
    }

    override fun test(name: String, ignored: Boolean, testFn: () -> Any?) {
        sortingContext.test(name, ignored) { returned(testFn()) }
    }
})

interface SuiteContext {
    fun suite(name: String, ignored: Boolean = false, body: SuiteContext.() -> Unit)

    fun test(name: String, ignored: Boolean = false, body: TestBodyContext.() -> Unit = {})
}


interface TestBodyContext {
    fun call(name: String)

    fun raised(msg: String)

    fun caught(msg: String)

    fun returned(msg: Any?)
}

private sealed class Entity(val name: String,
                    val ignored: Boolean)

private class Suite(name: String, ignored: Boolean, val body: SuiteContext.() -> Unit): Entity(name, ignored)

private class Test(name: String, ignored: Boolean, val body: TestBodyContext.() -> Unit): Entity(name, ignored)


private class SortingContext: SuiteContext {

    val structure = mutableListOf<Entity>()

    override fun suite(name: String, ignored: Boolean, body: SuiteContext.() -> Unit) {
        structure += Suite(name, ignored, body)
    }

    override fun test(name: String, ignored: Boolean, body: TestBodyContext.() -> Unit) {
        structure += Test(name, ignored, body)
    }

    fun <T: SuiteContext> replayInto(context: T): T {
        structure.sortedBy { it.name }.forEach {
            when (it) {
                is Suite -> context.suite(it.name, it.ignored) {
                    val oldSorter = sortingContext

                    sortingContext = SortingContext()
                    it.body(sortingContext)
                    sortingContext.replayInto(this)

                    sortingContext = oldSorter
                }
                is Test -> context.test(it.name, it.ignored) {
                    bodyContext = this
                    it.body(this)
                    bodyContext = null
                }
            }
        }

        return context
    }
}

private class LoggingContext : SuiteContext, TestBodyContext{
    val log: String
        get() = logHead + (lastRecord ?: "")

    private var indentation = ""

    override fun suite(name: String, ignored: Boolean, body: SuiteContext.() -> Unit) = indent {
        record("suite(\"$name\"${optionalIgnore(ignored)}) {")
        runSafely { this.body() }
        record("}")
    }

    override fun test(name: String, ignored: Boolean, body: TestBodyContext.() -> Unit) = indent {
        val num = record("test(\"$name\"${optionalIgnore(ignored)}) {")

        runSafely { this.body() }

        if (!writtenSince(num)) {
            record("test(\"$name\"${optionalIgnore(ignored)})", replaceLast = true)
        }
        else {
            record("}")
        }
    }

    override fun call(name: String) = indent {
        record("call(\"$name\")")
    }

    override fun raised(msg: String) = indent {
        record("raised(\"$msg\")")
    }

    override fun caught(msg: String) = indent {
        record("caught(\"$msg\")")
    }

    override fun returned(msg: Any?) = indent {
        if (msg is String) record("returned(\"$msg\")")
    }

    private fun runSafely(body: () -> Unit) {
        try {
            body()
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

fun checkLog(wrapInEmptySuite: Boolean = true, body: SuiteContext.() -> Unit): String {
    val expectedContext = SortingContext()
    if (wrapInEmptySuite) {
        expectedContext.suite("") {
            body()
        }
    } else {
        expectedContext.body()
    }

    val expectedLog = expectedContext.replayInto(LoggingContext()).log
    val actualLog = sortingContext.replayInto(LoggingContext()).log

    if (actualLog != expectedLog) {
        return "Failed test structure check. Expected: \"${expectedLog}\"; actual: \"${actualLog}\"."
    }
    else {
        return "OK"
    }
}
