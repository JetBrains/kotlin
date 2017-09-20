package konan.test

interface TestStatistics {
    val total: Int
    val passed: Int
    val failed: Int
    val ignored: Int

    val totalSuites: Int

    val failedTests: Collection<TestCase>
    val hasFailedTests: Boolean

    object EMPTY: TestStatistics {
        override val total: Int       get() = 0
        override val passed: Int      get() = 0
        override val failed: Int      get() = 0
        override val ignored: Int     get() = 0
        override val totalSuites: Int get() = 0

        override val failedTests: Collection<TestCase> = emptyList()
        override val hasFailedTests: Boolean = false
    }
}

class MutableTestStatistics: TestStatistics {

    override var total:   Int = 0; private set
    override var passed:  Int = 0; private set
    override var ignored: Int = 0; private set

    override var totalSuites: Int = 0; private set

    override val failed: Int
        get() = failedTests_.size

    override val hasFailedTests: Boolean
        get() = failedTests_.isNotEmpty()

    private val failedTests_ = mutableListOf<TestCase>()
    override val failedTests: Collection<TestCase>
        get() = failedTests_

    fun registerSuite() { totalSuites++ }

    fun registerPass() { total++; passed++ }
    fun registerFail(testCase: TestCase) { total++; failedTests_.add(testCase) }
    fun registerIgnore() { total++; ignored++ }

    fun reset() {
        total = 0
        passed = 0
        ignored = 0
        totalSuites = 0
        failedTests_.clear()
    }
}
