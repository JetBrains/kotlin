/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("KDocUnresolvedReference")

package org.jetbrains.kotlin.konan.test.blackbox.support

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase.WithTestRunnerExtras
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule.Companion.allRegularDependencies
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule.Companion.allDependsOnDependencies
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule.Companion.allFriendDependencies
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.util.*
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.test.services.impl.RegisteredDirectivesParser
import org.jetbrains.kotlin.test.util.joinToArrayString
import java.io.File

/**
 * Represents a single file that will be supplied to the compiler.
 */
class TestFile<M : TestModule> private constructor(
    val location: File,
    val module: M,
    private var state: State
) {
    private sealed interface State {
        object Committed : State
        class Uncommitted(var text: String) : State
    }

    private val uncommittedState: State.Uncommitted
        get() = when (val state = state) {
            is State.Uncommitted -> state
            is State.Committed -> fail { "File $location is already committed." }
        }

    val text: String
        get() = uncommittedState.text

    fun update(transformation: (String) -> String) {
        val uncommittedState = uncommittedState
        uncommittedState.text = transformation(uncommittedState.text)
    }

    // An optimization to release the memory occupied by numerous file texts.
    fun commit() {
        when (val state = state) {
            is State.Uncommitted -> {
                location.parentFile.mkdirs()
                location.writeText(state.text)
                this.state = State.Committed
            }
            is State.Committed -> {
                // Nothing to do. File is already saved to the disk.
            }
        }
    }

    override fun equals(other: Any?) = other === this || (other as? TestFile<*>)?.location?.path == location.path
    override fun hashCode() = location.path.hashCode()
    override fun toString() = "TestFile(location=$location, module.name=${module.name}, state=${state::class.java.simpleName})"

    companion object {
        fun <M : TestModule> createUncommitted(location: File, module: M, text: CharSequence) =
            TestFile(location, module, State.Uncommitted(text.toString()))

        fun <M : TestModule> createCommitted(location: File, module: M) =
            TestFile(location, module, State.Committed)
    }
}

/**
 * Represents a module in terms of Kotlin compiler. Includes one or more [TestFile]s. Can be compiled to executable file, KLIB
 * or any other artifact supported by the Kotlin/Native compiler.
 *
 * [TestModule.Exclusive] represents a collection of [TestFile]s used exclusively for an individual [TestCase].
 * [TestModule.Shared] represents a "shared" module, i.e. the auxiliary module that can be used in multiple [TestCase]s.
 *                     Such module is compiled to KLIB
 */
sealed class TestModule {
    abstract val name: String
    abstract val files: Set<TestFile<*>>

    data class Exclusive(
        override val name: String,
        val directRegularDependencySymbols: Set<String>,
        val directFriendDependencySymbols: Set<String>,
        val directDependsOnDependencySymbols: Set<String>, // mimics the name from ModuleStructureExtractorImpl, thought later converted to `-Xfragment-refines` parameter
        val directives: MutableList<RegisteredDirectivesParser.ParsedDirective> = mutableListOf()
    ) : TestModule() {
        init {
            val intersection = buildSet {
                addAll(directRegularDependencySymbols intersect directFriendDependencySymbols)
                addAll(directRegularDependencySymbols intersect directDependsOnDependencySymbols)
                addAll(directFriendDependencySymbols intersect directDependsOnDependencySymbols)
            }
            require(intersection.isEmpty()) {
                val m = if (intersection.size == 1) "module" else "modules"
                val names = if (intersection.size == 1) "`${intersection.first()}`" else intersection.joinToArrayString()
                """Module `$name` depends on $m $names with different kinds simultaneously"""
            }
        }

        override val files: FailOnDuplicatesSet<TestFile<Exclusive>> = FailOnDuplicatesSet()

        private lateinit var directRegularDependencies: Set<TestModule>
        private lateinit var directFriendDependencies: Set<TestModule>
        private lateinit var directDependsOnDependencies: Set<TestModule>

        // N.B. The following two properties throw an exception on attempt to resolve cyclic dependencies.
        val allRegularDependencies: Set<TestModule> by SM.lazyNeighbors({ directRegularDependencies }, { it.allRegularDependencies })
        val allFriendDependencies: Set<TestModule> by SM.lazyNeighbors({ directFriendDependencies }, { it.allFriendDependencies })
        val allDependsOnDependencies: Set<TestModule> by SM.lazyNeighbors({ directDependsOnDependencies }, { it.allDependsOnDependencies })

        lateinit var testCase: TestCase
            private set

        fun commit() {
            files.forEach { it.commit() }
        }

        /** Initialize all lateinit properties */
        fun initialize(
            testCase: TestCase,
            directRegularDependencies: Set<TestModule>,
            directFriendDependencies: Set<TestModule>,
            directDependsOnDependencies: Set<TestModule>
        ) {
            this.testCase = testCase

            this.directRegularDependencies = directRegularDependencies
            this.directFriendDependencies = directFriendDependencies
            this.directDependsOnDependencies = directDependsOnDependencies
        }

        fun haveSameSymbols(other: Exclusive) =
            other.directRegularDependencySymbols == directRegularDependencySymbols &&
                    other.directFriendDependencySymbols == directFriendDependencySymbols &&
                    other.directDependsOnDependencySymbols == directDependsOnDependencySymbols
    }

    data class Shared(override val name: String) : TestModule() {
        override val files: FailOnDuplicatesSet<TestFile<Shared>> = FailOnDuplicatesSet()
    }

    data class Given(val klibFile: File) : TestModule() {
        override val name: String get() = klibFile.name
        override val files: Set<TestFile<*>> get() = emptySet()
    }

    final override fun equals(other: Any?) =
        other === this || (other is TestModule && other.javaClass == javaClass && other.name == name && other.files == files)

    final override fun hashCode() = (javaClass.hashCode() * 31 + name.hashCode()) * 31 + files.hashCode()
    final override fun toString() = "${javaClass.canonicalName}[name=$name]"

    companion object {
        fun newDefaultModule() = Exclusive(DEFAULT_MODULE_NAME, emptySet(), emptySet(), emptySet())

        val TestModule.allRegularDependencies: Set<TestModule>
            get() = when (this) {
                is Exclusive -> allRegularDependencies
                is Shared, is Given -> emptySet()
            }

        val TestModule.allFriendDependencies: Set<TestModule>
            get() = when (this) {
                is Exclusive -> allFriendDependencies
                is Shared, is Given -> emptySet()
            }

        val TestModule.allDependsOnDependencies: Set<TestModule>
            get() = when (this) {
                is Exclusive -> allDependsOnDependencies
                is Shared, is Given -> emptySet()
            }

        private val SM = LockBasedStorageManager(TestModule::class.java.name)
    }
}

/**
 * A unique identifier of [TestCase].
 *
 * [testCaseGroupId] - a unique ID of [TestCaseGroup] this [TestCase] belongs to.
 */
interface TestCaseId {
    val testCaseGroupId: TestCaseGroupId

    data class TestDataFile(val file: File) : TestCaseId {
        override val testCaseGroupId = TestCaseGroupId.TestDataDir(file.parentFile) // The directory, containing testData file.
        override fun toString(): String = file.path
    }

    data class Named(val uniqueName: String) : TestCaseId {
        override val testCaseGroupId = TestCaseGroupId.Named(uniqueName) // The single test case inside the test group.
        override fun toString() = "[$uniqueName]"
    }
}

/**
 * A collection of one or more [TestModule]s that results in testable executable file.
 *
 * [modules] - the collection of [TestModule.Exclusive] modules with [TestFile]s that need to be compiled to run this test.
 *             Note: There can also be [TestModule.Shared] modules as dependencies of either of [TestModule.Exclusive] modules.
 *             See [TestModule.Exclusive.allDependencies] for details.
 * [id] - the unique ID of the test case.
 * [nominalPackageName] - the unique package name that was computed for this [TestCase] based on [id].
 *                        Note: It depends on the concrete [TestKind] whether the package name will be enforced for the [TestFile]s or not.
 */
class TestCase(
    val id: TestCaseId,
    val kind: TestKind,
    val modules: Set<TestModule.Exclusive>,
    val freeCompilerArgs: TestCompilerArgs,
    val nominalPackageName: PackageName,
    checks: TestRunChecks,
    val extras: Extras,
    val fileCheckStage: String? = null, // KT-62157: TODO move it to extras
    val expectedFailure: Boolean = false,
) {
    val checks = when (kind) {
        TestKind.STANDALONE_NO_TR, TestKind.STANDALONE_LLDB -> checks
        TestKind.REGULAR, TestKind.STANDALONE -> checks.copy(
            // With these two kinds tests will be run with `--ktest_no_exit_code`, so there must be a TCTestOutputFilter present.
            testFiltering = TestRunCheck.TestFiltering(TCTestOutputFilter)
        )
    }

    sealed interface Extras
    class NoTestRunnerExtras(val entryPoint: String? = null, val inputDataFile: File? = null, val arguments: List<String> = emptyList()) : Extras
    class WithTestRunnerExtras(val runnerType: TestRunnerType, val ignoredTests: Set<String> = emptySet()) : Extras

    init {
        when (kind) {
            TestKind.STANDALONE_NO_TR, TestKind.STANDALONE_LLDB -> assertTrue(extras is NoTestRunnerExtras)
            TestKind.REGULAR, TestKind.STANDALONE -> assertTrue(extras is WithTestRunnerExtras)
        }
    }

    inline fun <reified T : Extras> extras(): T = extras as T
    inline fun <reified T : Extras> safeExtras(): T? = extras as? T

    // The set of modules that have no incoming dependency arcs.
    val rootModules: Set<TestModule.Exclusive> by lazy {
        val allModules = hashSetOf<TestModule>()
        modules.forEach { module ->
            allModules += module
            allModules += module.allRegularDependencies
            allModules += module.allFriendDependencies
            allModules += module.allDependsOnDependencies
        }

        val rootModules = allModules.toHashSet()
        allModules.forEach { module ->
            rootModules -= module.allRegularDependencies
            rootModules -= module.allFriendDependencies
            rootModules -= module.allDependsOnDependencies
        }

        assertTrue(rootModules.isNotEmpty()) { "$id: No root modules in test case." }

        val nonExclusiveRootTestModules = rootModules.filter { module -> module !is TestModule.Exclusive }
        assertTrue(nonExclusiveRootTestModules.isEmpty()) {
            "$id: There are non-exclusive root test modules in test case. Modules: $nonExclusiveRootTestModules"
        }

        @Suppress("UNCHECKED_CAST")
        rootModules as Set<TestModule.Exclusive>
    }

    // All shared modules used in the current test case.
    val sharedModules: Set<TestModule.Shared> by lazy {
        buildSet {
            modules.forEach { module ->
                module.allRegularDependencies.forEach { dependency ->
                    if (dependency is TestModule.Shared) this += dependency
                }
            }
        }
    }

    fun initialize(
        givenModules: Set<TestModule.Given>?,
        findSharedModule: ((moduleName: String) -> TestModule.Shared?)?
    ) {
        // Check that there are no duplicated files among different modules.
        val duplicatedFiles = modules.flatMap { it.files }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertTrue(duplicatedFiles.isEmpty()) { "$id: Duplicated test files encountered: $duplicatedFiles" }

        // Check that there are modules with duplicated names.
        val exclusiveModules: Map</* regular module name */ String, TestModule.Exclusive> = modules.toIdentitySet()
            .groupingBy { module -> module.name }
            .aggregate { moduleName, _: TestModule.Exclusive?, module, isFirst ->
                assertTrue(isFirst) { "$id: Multiple test modules with the same name found: $moduleName" }
                module
            }

        fun findModule(moduleName: String): TestModule = exclusiveModules[moduleName]
            ?: findSharedModule?.invoke(moduleName)
            ?: fail { "$id: Module $moduleName not found" }

        modules.forEach { module ->
            module.commit() // Save to the file system and release the memory.

            module.initialize(
                testCase = this,
                directRegularDependencies = buildSet {
                    module.directRegularDependencySymbols.mapTo(this, ::findModule)
                    givenModules?.let(this@buildSet::addAll)
                },
                directFriendDependencies = module.directFriendDependencySymbols.mapToSet(::findModule),
                directDependsOnDependencies = module.directDependsOnDependencySymbols.mapToSet(::findModule)
            )
        }
    }
}

/**
 * A unique identified of [TestCaseGroup].
 */
interface TestCaseGroupId {
    data class TestDataDir(val dir: File) : TestCaseGroupId
    data class Named(val uniqueName: String) : TestCaseGroupId
}

/**
 * A group of [TestCase]s that were obtained from the same origin (ex: same testData directory).
 *
 * [TestCase]s inside of the group with similar [TestCompilerArgs] can be compiled to the single
 * executable file to reduce the time spent for compiling and speed-up overall test execution.
 */
sealed interface TestCaseGroup {
    fun isEnabled(testCaseId: TestCaseId): Boolean
    fun getByName(testCaseId: TestCaseId): TestCase?

    fun getRegularOnly(
        freeCompilerArgs: TestCompilerArgs,
        sharedModules: Set<TestModule.Shared>,
        runnerType: TestRunnerType
    ): Collection<TestCase>

    class Default(
        private val disabledTestCaseIds: Set<TestCaseId>,
        testCases: Iterable<TestCase>
    ) : TestCaseGroup {
        private val testCasesById = testCases.associateBy { it.id }

        override fun isEnabled(testCaseId: TestCaseId) = testCaseId !in disabledTestCaseIds
        override fun getByName(testCaseId: TestCaseId) = testCasesById[testCaseId]

        override fun getRegularOnly(
            freeCompilerArgs: TestCompilerArgs,
            sharedModules: Set<TestModule.Shared>,
            runnerType: TestRunnerType
        ) = testCasesById.values.filter { testCase ->
            testCase.kind == TestKind.REGULAR
                    && testCase.freeCompilerArgs == freeCompilerArgs
                    && testCase.sharedModules == sharedModules
                    && testCase.extras<WithTestRunnerExtras>().runnerType == runnerType
        }

        override fun toString() = "TestCaseGroup.Default::${testCasesById.keys.firstNotNullOfOrNull { it.testCaseGroupId }}"
    }

    data class MetaGroup(val testCaseGroupId: TestCaseGroupId, val testGroups: Set<TestCaseGroup>) : TestCaseGroup {
        override fun isEnabled(testCaseId: TestCaseId): Boolean = testGroups.all { it.isEnabled(testCaseId) }

        override fun getByName(testCaseId: TestCaseId): TestCase? = testGroups.firstNotNullOfOrNull { it.getByName(testCaseId) }

        override fun getRegularOnly(
            freeCompilerArgs: TestCompilerArgs,
            sharedModules: Set<TestModule.Shared>,
            runnerType: TestRunnerType,
        ): Collection<TestCase> = testGroups.flatMap { it.getRegularOnly(freeCompilerArgs, sharedModules, runnerType) }
    }

    data object AllDisabled : TestCaseGroup {
        override fun isEnabled(testCaseId: TestCaseId) = false
        override fun getByName(testCaseId: TestCaseId) = unsupported()

        override fun getRegularOnly(
            freeCompilerArgs: TestCompilerArgs,
            sharedModules: Set<TestModule.Shared>,
            runnerType: TestRunnerType
        ) = unsupported()

        private fun unsupported(): Nothing = fail { "This function should not be called" }
    }
}
