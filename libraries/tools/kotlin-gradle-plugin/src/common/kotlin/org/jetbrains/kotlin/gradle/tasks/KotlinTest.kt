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
import org.jetbrains.kotlin.gradle.utils.getExecOperations
import org.jetbrains.kotlin.gradle.utils.injected
import javax.inject.Inject

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class KotlinTest
@Inject
internal constructor(
    execOps: ExecOperations?,
) : AbstractTestTask() {

    @Deprecated(message = "Extending this class is deprecated. Scheduled for removal in Kotlin 2.4.")
    // Note to KGP developers: subtypes are still supported for KGP. We just want to prevent users from extending this task.
    constructor() : this(
        execOps = null,
    )

    // TODO KT-75294 When the deprecated secondary constructor is removed, move execOps to a non-null constructor property.
    private val execOps: ExecOperations =
        execOps ?: @Suppress("DEPRECATION") project.getExecOperations()

    @Input
    @Optional
    var targetName: String? = null

    @Internal // Taken into account by excludePatterns.
    @Deprecated(
        "Use filter.excludePatterns instead. Scheduled for removal in Kotlin 2.3.",
        ReplaceWith("filter.excludePatterns"),
        level = DeprecationLevel.ERROR
    )
    var excludes = mutableSetOf<String>()

    protected val filterExt: DefaultTestFilter
        @Internal get() = filter as DefaultTestFilter

    init {
        filterExt.isFailOnNoMatchingTests = false
    }

    val includePatterns: Set<String>
        @Input get() = filterExt.includePatterns + filterExt.commandLineIncludePatterns

    @Suppress("DEPRECATION_ERROR")
    val excludePatterns: Set<String>
        @Input get() = excludes + filterExt.excludePatterns

    @get:Internal
    @Deprecated(
        "FileResolver is an internal Gradle API and must be removed to support Gradle 9.0. Please remove usages of this property. Scheduled for removal in Kotlin 2.4.",
        ReplaceWith("TODO(\"FileResolver is an internal Gradle API and must be removed to support Gradle 9.0. Please remove usages of this property.\")"),
        DeprecationLevel.ERROR,
    )
    @Suppress("unused")
    open val fileResolver: Nothing
        get() = injected

    @get:Internal
    @Deprecated(
        "ExecHandleFactory is an internal Gradle API and must be removed to support Gradle 9.0. Please remove usages of this property. Scheduled for removal in Kotlin 2.4.",
        ReplaceWith("TODO(\"ExecHandleFactory is an internal Gradle API and must be removed to support Gradle 9.0. Please remove usages of this property.\")"),
        DeprecationLevel.ERROR,
    )
    @Suppress("unused")
    open val execHandleFactory: Nothing
        get() = injected

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
