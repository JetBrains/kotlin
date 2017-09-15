package konan.test

object TestRunner {

    object SimpleTestListener: TestListener {
        override fun startTesting(runner: TestRunner) = println("Starting testing")
        override fun endTesting(runner: TestRunner) = println("Testing finished")

        override fun startSuite(suite: TestSuite) = println("Starting test suite: $suite")
        override fun endSuite(suite: TestSuite) = println("Test suite finished: $suite")

        override fun start(testCase: TestCase) = println("Start test case: $testCase")
        override fun pass(testCase: TestCase) = println("Pass: $testCase")
        override fun fail(testCase: TestCase, e: Throwable) {
            println("Fail: $testCase. Exception:")
            e.printStackTrace()
        }
        override fun ignore(testCase: TestCase) = println("Ignore: $testCase")
    }

    private val _suites = mutableListOf<TestSuite>()
    val suites: Collection<TestSuite>  get() = _suites

    fun register(suite: TestSuite) = _suites.add(suite)
    fun register(suites: Iterable<TestSuite>) = _suites.addAll(suites)
    fun register(vararg suites: TestSuite) = _suites.addAll(suites)

    fun run() {
        SimpleTestListener.startTesting(this)
        suites.forEach {
            SimpleTestListener.startSuite(it)
            it.run(SimpleTestListener)
            SimpleTestListener.endSuite(it)
        }
        SimpleTestListener.endTesting(this)
    }
}

