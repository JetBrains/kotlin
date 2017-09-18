package konan.test

object TestRunner {

    private val _suites = mutableListOf<TestSuite>()
    val suites: Collection<TestSuite>  get() = _suites

    fun register(suite: TestSuite) = _suites.add(suite)
    fun register(suites: Iterable<TestSuite>) = _suites.addAll(suites)
    fun register(vararg suites: TestSuite) = _suites.addAll(suites)

    fun run(listener: TestListener) = with(listener) {
        startTesting(this@TestRunner)
        suites.forEach {
            if (it.ignored) {
                ignoreSuite(it)
            } else {
                startSuite(it)
                it.run(listener)
                endSuite(it)
            }
        }
        endTesting(this@TestRunner)
    }
}

