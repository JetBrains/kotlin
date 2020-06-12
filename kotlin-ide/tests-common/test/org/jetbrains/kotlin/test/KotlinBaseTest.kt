/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File

abstract class KotlinBaseTest<F : KotlinBaseTest.TestFile> : KtUsefulTestCase() {

    @JvmField
    protected var coroutinesPackage: String = ""

    @Throws(Exception::class)
    override fun setUp() {
        coroutinesPackage = ""
        super.setUp()
    }

    @Throws(java.lang.Exception::class)
    protected open fun doTestWithCoroutinesPackageReplacement(filePath: String, coroutinesPackage: String) {
        this.coroutinesPackage = coroutinesPackage
        doTest(filePath)
    }

    @Throws(java.lang.Exception::class)
    protected open fun doTest(filePath: String) {
        val file = File(filePath)
        var expectedText = KotlinTestUtils.doLoadFile(file)
        if (coroutinesPackage.isNotEmpty()) {
            expectedText = expectedText.replace("COROUTINES_PACKAGE", coroutinesPackage)
        }
        doMultiFileTest(file, createTestFilesFromFile(file, expectedText))
    }

    protected abstract fun createTestFilesFromFile(file: File, expectedText: String): List<F>

    @Throws(java.lang.Exception::class)
    protected open fun doMultiFileTest(
        wholeFile: File,
        files: List<F>
    ) {
        throw UnsupportedOperationException("Multi-file test cases are not supported in this test")
    }

    protected open fun getTestJdkKind(files: List<F>): TestJdkKind {
        for (file in files) {
            if (InTextDirectivesUtils.isDirectiveDefined(file.content, "FULL_JDK")) {
                return TestJdkKind.FULL_JDK
            }
        }
        return TestJdkKind.MOCK_JDK
    }

    protected open fun extractConfigurationKind(files: List<F>): ConfigurationKind {
        return Companion.extractConfigurationKind(files)
    }


    open class TestFile @JvmOverloads constructor(
        @JvmField val name: String,
        @JvmField val content: String,
        @JvmField val directives: Directives = Directives()
    ) : Comparable<TestFile> {
        override operator fun compareTo(other: TestFile): Int {
            return name.compareTo(other.name)
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is TestFile && other.name == name
        }

        override fun toString(): String {
            return name
        }

    }

    open class TestModule(
        @JvmField val name: String,
        @JvmField val dependenciesSymbols: List<String>,
        @JvmField val friendsSymbols: List<String>
    ) : Comparable<TestModule> {

        val dependencies: MutableList<TestModule> = arrayListOf()
        val friends: MutableList<TestModule> = arrayListOf()

        override fun compareTo(other: TestModule): Int = name.compareTo(other.name)

        override fun toString(): String = name
    }

    companion object {
        fun extractConfigurationKind(files: List<TestFile>): ConfigurationKind {
            val addReflect = files.any { InTextDirectivesUtils.isDirectiveDefined(it.content, "WITH_REFLECT") }
            return if (addReflect) ConfigurationKind.STDLIB_REFLECT else ConfigurationKind.STDLIB
        }
    }
}