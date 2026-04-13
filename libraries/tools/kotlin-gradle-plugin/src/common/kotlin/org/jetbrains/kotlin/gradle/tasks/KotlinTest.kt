/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.internal.testing.KotlinTestRunnerListener
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutor
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import javax.inject.Inject

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class KotlinTest
@Inject
internal constructor(
    private val execOps: ExecOperations,
) : AbstractTestTask() {

    @Input
    @Optional
    var targetName: String? = null

    protected val filterExt: DefaultTestFilter
        @Internal get() = filter as DefaultTestFilter

    init {
        filterExt.isFailOnNoMatchingTests = false
    }

    val includePatterns: Set<String>
        @Input get() = filterExt.includePatterns + filterExt.commandLineIncludePatterns

    val excludePatterns: Set<String>
        @Input get() = filterExt.excludePatterns

    private val runListeners = mutableListOf<KotlinTestRunnerListener>()

    @Internal
    var ignoreRunFailures: Boolean = false

    fun addRunListener(listener: KotlinTestRunnerListener) {
        runListeners.add(listener)
    }

    private val ignoreTcsmOverflow = PropertiesProvider(project).ignoreTcsmOverflow

    // This method is called on execution time
    override fun createTestExecuter(): TestExecuter<*> =
        TCServiceMessagesTestExecutor(
            description = path,
            runListeners = runListeners,
            ignoreTcsmOverflow = ignoreTcsmOverflow.get(),
            ignoreRunFailures = ignoreRunFailures,
            execOps = execOps,
        )
}
