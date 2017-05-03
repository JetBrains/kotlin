package konan.test

interface TestCase {
    val name: String
}

interface TestSuite {
    val name: String
    val testCases: Map<String, TestCase>
    fun run(listener: TestListener)
}

enum class FunctionKind {
    BEFORE,
    AFTER,
    BEFORE_CLASS,
    AFTER_CLASS
}

abstract class AbstractTestSuite<F: Function<Unit>>(override val name: String): TestSuite {
    override fun toString(): String = name

    inner class BasicTestCase(override val name: String, val testFunction: F): TestCase {
        override fun toString(): String = name
    }

    private val _testCases = mutableMapOf<String, BasicTestCase>()
    override val testCases: Map<String, BasicTestCase>
        get() = _testCases

    private val specialFunctions = mutableMapOf<FunctionKind, MutableSet<F>>()
    private fun Map<FunctionKind, Set<F>>.getFunctions(type: FunctionKind) =
            specialFunctions.getOrPut(type) { mutableSetOf() }

    val before:      Collection<F>  get() = specialFunctions.getFunctions(FunctionKind.BEFORE)
    val after:       Collection<F>  get() = specialFunctions.getFunctions(FunctionKind.AFTER)

    // TODO: Must be in companions. Support it.
    val beforeClass: Collection<F>  get() = specialFunctions.getFunctions(FunctionKind.BEFORE_CLASS)
    val afterClass:  Collection<F>  get() = specialFunctions.getFunctions(FunctionKind.AFTER_CLASS)

    protected fun registerTestCase(testCase: BasicTestCase) = _testCases.put(testCase.name, testCase)
    protected fun registerTestCases(testCases: Collection<BasicTestCase>) = testCases.forEach { registerTestCase(it) }
    protected fun registerTestCases(vararg testCases: BasicTestCase) = registerTestCases(testCases.toList())

    protected fun registerFunction(type: FunctionKind, function: F) =
            specialFunctions.getFunctions(type).add(function)

    protected abstract fun doBeforeClass()
    protected abstract fun doAfterClass()

    protected abstract fun doTest(testCase: BasicTestCase)

    override fun run(listener: TestListener) {
        doBeforeClass()
        testCases.values.forEach {
            try {
                doTest(it)
                // TODO: merge catches?
            } catch (e: AssertionError) {
                listener.fail(it, e)
            } catch (e: Throwable) {
                listener.error(it, e)
            }
            listener.pass(it)
        }
        doAfterClass()
    }
}

abstract class TestClass<T>(name: String): AbstractTestSuite<T.() -> Unit>(name) {

    abstract fun createInstance(): T

    // TODO: What about companions?
    override fun doBeforeClass() {} // = beforeClass.forEach { createInstance().it() }
    override fun doAfterClass() {} // = afterClass.forEach { createInstance().it() }

    override fun doTest(testCase: BasicTestCase) {
        val instance = createInstance()
        val testFunction = testCase.testFunction
        before.forEach { instance.it() }
        instance.testFunction()
        after.forEach { instance.it() }
    }
}

class TopLevelTestSuite(name: String): AbstractTestSuite<() -> Unit>(name) {
    override fun doBeforeClass() = beforeClass.forEach { it() }
    override fun doAfterClass() = afterClass.forEach { it() }

    override fun doTest(testCase: BasicTestCase) {
        before.forEach { it() }
        testCase.testFunction()
        after.forEach { it() }
    }
}