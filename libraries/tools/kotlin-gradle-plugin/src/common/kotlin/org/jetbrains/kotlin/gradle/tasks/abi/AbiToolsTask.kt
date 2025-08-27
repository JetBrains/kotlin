/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.abi.tools.api.AbiToolsFactory
import org.jetbrains.kotlin.abi.tools.api.AbiToolsInterface
import org.jetbrains.kotlin.buildtools.api.KotlinToolchain
import org.jetbrains.kotlin.buildtools.api.KotlinToolchain.BuildSession
import org.jetbrains.kotlin.compilerRunner.btapi.SharedApiClassesClassLoaderProvider
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec
import org.jetbrains.kotlin.gradle.internal.UsesClassLoadersCachingBuildService
import java.util.*
import java.util.Locale.getDefault
import kotlin.reflect.KClass

/**
 * A parent class for all tasks that use Application Binary Interace (ABI) tools.
 */
@DisableCachingByDefault(because = "Abstract task")
internal abstract class AbiToolsTask : DefaultTask(), UsesClassLoadersCachingBuildService {
    @get:Classpath
    abstract val toolsClasspath: ConfigurableFileCollection

    @get:Input
    internal abstract val buildToolsApiEnabled: Property<Boolean>

    // TODO I'd rather like to implement some interface from which I can get properties `isBuildToolsApiUsed` and `kotlinToolchain`
    @get:Classpath
    internal abstract val buildToolsClasspath: ConfigurableFileCollection

    @TaskAction
    fun execute() {
        println("exec")

        val classLoadersCachingBuildService = classLoadersCachingService.get()

        val files = toolsClasspath.files.toList()
        // get class loader with the ABI Tools implementation from cache
        val classLoader = classLoadersCachingBuildService.getClassLoader(files, SharedClassLoaderProvider)
        // get implementation of the ABI Tools factory
        val factory = loadImplementation(AbiToolsFactory::class, classLoader)
        // get an instance of the ABI Tools and invoke task logic

        // TODO this to some common code with a global session
        val buildSession: BuildSession? = if (buildToolsApiEnabled.get()) {
            val classLoader = classLoadersCachingBuildService
                .getClassLoader(buildToolsClasspath.toList(), SharedApiClassesClassLoaderProvider)
            val toolchain = KotlinToolchain.loadImplementation(classLoader)
            toolchain.createBuildSession()
        } else {
            null
        }

        try {
            runTools(factory.get(), buildSession)
        } finally {
            // TODO is `close` can throw an Exception ?
            buildSession?.close()
        }
    }

    protected abstract fun runTools(tools: AbiToolsInterface, session: BuildSession?)


    private fun <T : Any> loadImplementation(cls: KClass<T>, classLoader: ClassLoader): T {
        val implementations = ServiceLoader.load(cls.java, classLoader)
        implementations.firstOrNull() ?: error("The classpath contains no implementation for ${cls.qualifiedName}")
        return implementations.singleOrNull()
            ?: error("The classpath contains more than one implementation for ${cls.qualifiedName}")
    }

    companion object {
        /**
         * Gets the task name intended for the specified report variant.
         *
         * For example, the task 'foo' for the main variant has the name "foo", while for the "extra"  variant, it's called "fooExtra".
         */
        internal fun composeTaskName(baseName: String, variantName: String): String {
            val suffix = if (variantName == AbiValidationVariantSpec.MAIN_VARIANT_NAME) {
                ""
            } else {
                variantName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
            }

            return "$baseName$suffix"
        }
    }

}
