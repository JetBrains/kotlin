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

open class BaseTestListener: TestListener {
    override fun startTesting(runner: TestRunner) {}
    override fun endTesting(runner: TestRunner, timeMillis: Long) {}
    override fun startSuite(suite: TestSuite) {}
    override fun endSuite(suite: TestSuite, timeMillis: Long) {}
    override fun ignoreSuite(suite: TestSuite) {}
    override fun start(testCase: TestCase) {}
    override fun pass(testCase: TestCase, timeMillis: Long) {}
    override fun fail(testCase: TestCase, e: Throwable, timeMillis: Long) {}
    override fun ignore(testCase: TestCase) {}
}
