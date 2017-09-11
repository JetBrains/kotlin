package konan.test

import kotlin.AssertionError

interface TestCase {
    val name: String
}

interface TestSuite {
    val name: String
    val testCases: Map<String, TestCase>
    fun run(listener: TestListener)
}

enum class TestFunctionKind {
    BEFORE,
    AFTER,
    BEFORE_CLASS,
    AFTER_CLASS
}

abstract class AbstractTestSuite<F: Function<Unit>>(override val name: String): TestSuite {
    override fun toString(): String = name

    // TODO: Make inner and remove the type param when the bug is fixed.
    class BasicTestCase<F: Function<Unit>>(override val name: String, val testFunction: F): TestCase {
        override fun toString(): String = name
    }

    private val _testCases = mutableMapOf<String, BasicTestCase<F>>()
    override val testCases: Map<String, BasicTestCase<F>>
        get() = _testCases

    private val specialFunctions = mutableMapOf<TestFunctionKind, MutableSet<F>>()
    private fun Map<TestFunctionKind, Set<F>>.getFunctions(type: TestFunctionKind) =
            specialFunctions.getOrPut(type) { mutableSetOf() }

    val before:      Collection<F>  get() = specialFunctions.getFunctions(TestFunctionKind.BEFORE)
    val after:       Collection<F>  get() = specialFunctions.getFunctions(TestFunctionKind.AFTER)

    // TODO: Must be in companions. Support it.
    val beforeClass: Collection<F>  get() = specialFunctions.getFunctions(TestFunctionKind.BEFORE_CLASS)
    val afterClass:  Collection<F>  get() = specialFunctions.getFunctions(TestFunctionKind.AFTER_CLASS)

    fun registerTestCase(name: String, testFunction: F) = registerTestCase(BasicTestCase(name, testFunction))

    protected fun registerTestCase(testCase: BasicTestCase<F>) = _testCases.put(testCase.name, testCase)
    protected fun registerTestCases(testCases: Collection<BasicTestCase<F>>) = testCases.forEach { registerTestCase(it) }
    protected fun registerTestCases(vararg testCases: BasicTestCase<F>) = registerTestCases(testCases.toList())

    protected fun registerFunction(type: TestFunctionKind, function: F) =
            specialFunctions.getFunctions(type).add(function)

    protected abstract fun doBeforeClass()
    protected abstract fun doAfterClass()

    protected abstract fun doTest(testCase: BasicTestCase<F>)

    init {
        TestRunner.register(this)
    }

    override fun run(listener: TestListener) {
        doBeforeClass()
        testCases.values.forEach {
            try {
                doTest(it)
                listener.pass(it)
                // TODO: merge catches?
            } catch (e: AssertionError) {
                listener.fail(it, e)
            } catch (e: Throwable) {
                listener.error(it, e)
            }

        }
        doAfterClass()
    }
}

abstract class BaseClassSuite<T>(name: String): AbstractTestSuite<T.() -> Unit>(name) {

    abstract fun createInstance(): T

    // TODO: What about companions?
    override fun doBeforeClass() {} // = beforeClass.forEach { createInstance().it() }
    override fun doAfterClass() {} // = afterClass.forEach { createInstance().it() }

    override fun doTest(testCase: BasicTestCase<T.() -> Unit>) {
        val instance = createInstance()
        val testFunction = testCase.testFunction
        before.forEach { instance.it() }
        instance.testFunction()
        after.forEach { instance.it() }
    }
}

class BaseTopLevelSuite(name: String): AbstractTestSuite<() -> Unit>(name) {

    override fun doBeforeClass() = beforeClass.forEach { it() }
    override fun doAfterClass() = afterClass.forEach { it() }

    override fun doTest(testCase: BasicTestCase<() -> Unit>) {
        before.forEach { it() }
        testCase.testFunction()
        after.forEach { it() }
    }
}