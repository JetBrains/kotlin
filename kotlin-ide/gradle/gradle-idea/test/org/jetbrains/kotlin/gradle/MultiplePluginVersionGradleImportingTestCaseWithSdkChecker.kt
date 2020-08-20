package org.jetbrains.kotlin.gradle

import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.jetbrains.kotlin.idea.test.KotlinSdkCreationChecker
import org.jetbrains.kotlin.idea.test.runAll

abstract class MultiplePluginVersionGradleImportingTestCaseWithSdkChecker : MultiplePluginVersionGradleImportingTestCase() {
    private var mySdkCreationChecker: KotlinSdkCreationChecker? = null

    val sdkCreationChecker: KotlinSdkCreationChecker
        get() = mySdkCreationChecker ?: error("SDK creation checker is not initialized")

    override fun setUp() {
        super.setUp()
        mySdkCreationChecker = KotlinSdkCreationChecker()
    }

    override fun tearDown() {
        runAll(
            ThrowableRunnable {
                mySdkCreationChecker!!.removeNewKotlinSdk()
                mySdkCreationChecker = null
            },
            ThrowableRunnable { super.tearDown() }
        )
    }
}