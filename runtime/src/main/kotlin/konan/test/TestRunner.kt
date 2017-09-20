package konan.test

import kotlin.system.measureTimeMillis

object TestRunner {

    private val _suites = mutableListOf<TestSuite>()
    val suites: Collection<TestSuite>  get() = _suites

    fun register(suite: TestSuite) = _suites.add(suite)
    fun register(suites: Iterable<TestSuite>) = _suites.addAll(suites)
    fun register(vararg suites: TestSuite) = _suites.addAll(suites)

    fun run(listener: TestListener) = with(listener) {
        startTesting(this@TestRunner)
        val totalTime = measureTimeMillis {
            suites.forEach {
                if (it.ignored) {
                    ignoreSuite(it)
                } else {
                    startSuite(it)
                    val time = measureTimeMillis { it.run(listener) }
                    endSuite(it, time)
                }
            }
        }
        endTesting(this@TestRunner, totalTime)
    }
}
