/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.coreCases

import org.jetbrains.kotlin.project.model.infra.KpmTestCase

/**
 * Marker-supertype for all KPM Core Cases
 *
 * # Main idea:
 * - provide unified format for declaring project structure of core cases
 * - sync tested core cases across all subsystems
 *
 * # How it works:
 * - Core cases are defined inside [org.jetbrains.kotlin.project.model.coreCases]
 *   (see "Conventions" below for exact format to follow) with the help of a DSL in
 *   [org.jetbrains.kotlin.project.model.testDsl] package
 *
 * - Inherit [KpmCoreCasesTestRunner] in your test runner. It will:
 *   a) ensure that all core cases are covered and warn you when new one is added
 *   b) inject core cases instances for you automatically
 *
 * - You can also use `libraries/tools/kotlin-project-model-tests-generator/src/test/kotlin/org/jetbrains/kotlin/kpm/GenerateKpmTests.kt`
 *   if you don't need custom per-test-method assertions; it will generate test
 *   cases which just call `doTest` on a passed [KpmTestCase] in a manner, similar
 *   to other `*TestsGenerated`-runners
 *
 * # Conventions
 * 1. All Core Cases should inherit [KpmTestCaseWrapper] marker
 * 2. All Core cases must reside in the [org.jetbrains.kotlin.project.model.coreCases]-package
 * 3. Each Core Case should be declared in a separate .kt-file, with the name equal
 *    to the name of the case itself (`MyTestCase` -> `MyTestCase.kt`)
 * 4. Test Runner-class should consist of methods, which are named in the following
 *    pattern: `test$caseName`
 * 5. (Optional) Test methods can declare a parameter of a [KpmTestCase]-type. Testing
 *    infrastructure will inject an instance of corresponding [KpmTestCase]
 *    (correspondence is determined based on test method naming convention from pt. 4)
 *
 * # Custom data, assertions, etc.
 * It is an explicit non-goal of this infrastructure to provide a DSL capable of
 * expressing assertions needed by an aribtrary subsystem. Instead, two extensibility
 * mechanisms are provided:
 *   - `extras`, as in [KpmTestCase.extras]: essentially a way to attach custom userdata
 *     to a given entity
 *   - `fork`, as in `[KpmTestCase.fork]: copies the test-case
 *
 * So the intended pattern is to fork a case and decorate it with any necessary data or
 * assertions via `extras`.
 */
sealed interface KpmTestCaseWrapper {
    val case: KpmTestCase

    companion object {
        val allCasesByNames: Map<String, KpmTestCaseWrapper> by lazy {
            doGetAllCases()
        }

        val allCasesNames: Set<String>
            get() = allCasesByNames.keys

        private fun doGetAllCases(): Map<String, KpmTestCaseWrapper> = KpmTestCaseWrapper::class.sealedSubclasses
            .map {
                requireNotNull(it.objectInstance) {
                    "Can't get object instance for $it. Check that it is declared as an `object` "
                }
            }
            .associateBy { it.case.name }

    }
}
