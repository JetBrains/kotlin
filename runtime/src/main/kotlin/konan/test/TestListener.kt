package konan.test

interface TestListener {
    fun startTesting(runner: TestRunner)
    fun endTesting(runner: TestRunner, timeMillis: Long)

    fun startSuite(suite: TestSuite)
    fun endSuite(suite: TestSuite, timeMillis: Long)
    fun ignoreSuite(suite: TestSuite)

    fun start(testCase: TestCase)
    fun pass(testCase: TestCase, timeMillis: Long)
    fun fail(testCase: TestCase, e: Throwable, timeMillis: Long)
    fun ignore(testCase: TestCase)
}

interface TestStatistics {
    val total: Int
    val passed: Int
    val failed: Int
    val ignored: Int

    // TODO: Do we need such properties for other cases?
    val failedTests: Collection<TestCase>
    val hasFailedTests: Boolean
}

abstract class AbstractTestStatistics: TestStatistics {
    override var total:   Int = 0; protected set
    override var passed:  Int = 0; protected set
    override var failed:  Int = 0; protected set
    override var ignored: Int = 0; protected set

    protected fun registerPass() { total++; passed++ }
    protected fun registerFail() { total++; failed++ }
    protected fun registerIgnore() { total++; ignored++ }
}