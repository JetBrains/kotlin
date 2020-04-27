/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator

import junit.framework.TestCase
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.*
import java.util.regex.Pattern

class TestGroup(
        private val moduleAbsolutePath: String,
        val testDataAbsoluteRoot: String,
        val testRunnerMethodName: String,
        val additionalRunnerArguments: List<String> = emptyList(),
        val annotations: List<AnnotationModel> = emptyList()
) {
    inline fun <reified T : TestCase> testClass(
        suiteTestClassName: String = getDefaultSuiteTestClassName(T::class.java.simpleName),
        useJunit4: Boolean = false,
        annotations: List<AnnotationModel> = emptyList(),
        noinline init: TestClass.() -> Unit
    ) {
        testClass(T::class.java.name, suiteTestClassName, useJunit4, annotations, init)
    }

    fun testClass(
        baseTestClassName: String,
        suiteTestClassName: String = getDefaultSuiteTestClassName(baseTestClassName.substringAfterLast('.')),
        useJunit4: Boolean,
        annotations: List<AnnotationModel> = emptyList(),
        init: TestClass.() -> Unit
    ) {
        TestGenerator(
                "${moduleAbsolutePath}/test",
                suiteTestClassName,
                baseTestClassName,
                TestClass(annotations).apply(init).testModels,
                useJunit4
        ).generateAndSave()
    }

    inner class TestClass(val annotations: List<AnnotationModel>) {
        val testModels = ArrayList<TestClassModel>()

        fun model(
            relativeRootPath: String,
            recursive: Boolean = true,
            excludeParentDirs: Boolean = false,
            extension: String? = "kt", // null string means dir (name without dot)
            pattern: String = if (extension == null) """^([^\.]+)$""" else "^(.+)\\.$extension\$",
            excludedPattern: String? = null,
            testMethod: String = "doTest",
            singleClass: Boolean = false,
            testClassName: String? = null,
            targetBackend: TargetBackend = TargetBackend.ANY,
            excludeDirs: List<String> = listOf(),
            filenameStartsLowerCase: Boolean? = null,
            skipIgnored: Boolean = false,
            deep: Int? = null
        ) {
            val rootFile = File("$testDataAbsoluteRoot/$relativeRootPath")
            val compiledPattern = Pattern.compile(pattern)
            val compiledExcludedPattern = excludedPattern?.let { Pattern.compile(it) }
            val className = testClassName ?: TestGeneratorUtil.fileNameToJavaIdentifier(rootFile)
            testModels.add(
                if (singleClass) {
                    if (excludeDirs.isNotEmpty()) error("excludeDirs is unsupported for SingleClassTestModel yet")
                    SingleClassTestModel(
                            rootFile, compiledPattern, compiledExcludedPattern, filenameStartsLowerCase, testMethod,
                            className, targetBackend, skipIgnored, testRunnerMethodName, additionalRunnerArguments, annotations
                    )
                } else {
                    SimpleTestClassModel(
                            rootFile, recursive, excludeParentDirs, compiledPattern, compiledExcludedPattern, filenameStartsLowerCase,
                            testMethod, className, targetBackend, excludeDirs, skipIgnored, testRunnerMethodName, additionalRunnerArguments,
                            deep,
                            annotations
                    )
                }
            )
        }
    }
}

fun testGroup(
        moduleRelativePath: String,
        testDataRootRelativePath: String = "${moduleRelativePath}/testData",
        testRunnerMethodName: String = RunTestMethodModel.METHOD_NAME,
        additionalRunnerArguments: List<String> = emptyList(),
        init: TestGroup.() -> Unit
) {
    val moduleAbsolutePath = "${KotlinTestUtils.getHomeDirectory()}/$moduleRelativePath"
    val testDataAbsolutePath = "${KotlinTestUtils.getHomeDirectory()}/$testDataRootRelativePath"
    TestGroup(moduleAbsolutePath, testDataAbsolutePath, testRunnerMethodName, additionalRunnerArguments).init()
}

fun getDefaultSuiteTestClassName(baseTestClassName: String): String {
    require(baseTestClassName.startsWith("Abstract")) { "Doesn't start with \"Abstract\": $baseTestClassName" }
    return baseTestClassName.substringAfter("Abstract") + "Generated"
}
