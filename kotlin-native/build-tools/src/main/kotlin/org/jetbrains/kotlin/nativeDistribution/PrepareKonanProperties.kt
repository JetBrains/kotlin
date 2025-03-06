/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nativeDistribution

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

/**
 * Task to update `konan.properties` for placing into distribution.
 */
open class PrepareKonanProperties @Inject constructor(
        objectFactory: ObjectFactory,
        private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
    /**
     * Template `konan.properties`
     */
    @get:InputFile
    val input: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Where to put updated `konan.properties`
     */
    @get:OutputFile
    val output: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Compiler version to update in `konan.properties`
     */
    @get:Input
    val compilerVersion: Property<String> = objectFactory.property(String::class.java)

    /**
     * Hostnames for which to update the default llvm variant.
     */
    @get:Input
    val llvmVariants: MapProperty<KonanTarget, LLVMDistributionKind> = objectFactory.mapProperty(KonanTarget::class.java, LLVMDistributionKind::class.java)

    /**
     * LLVM version to update in `konan.properties`
     */
    @get:Input
    val llvmProperties: MapProperty<String, String> = objectFactory.mapProperty(String::class.java, String::class.java)

    @TaskAction
    fun prepare() {
        val output = output.get()
        fileSystemOperations.copy {
            from(input)
            into(output.asFile.parentFile)
            filter(mapOf("tokens" to mapOf("compilerVersion" to compilerVersion.get())), ReplaceTokens::class.java)
            llvmVariants.get().forEach { (host, llvmVariant) ->
                filter(mapOf(
                        "beginToken" to "$",
                        "endToken" to "",
                        "tokens" to mapOf("llvm.${host}.dev" to "\$llvm.${host}.${llvmVariant.nameForProperties}")
                ), ReplaceTokens::class.java)
            }
        }
        val llvmPropertiesString = llvmProperties.get().entries.sortedBy { it.key }.joinToString(separator = "\n") {
            "${it.key}=${it.value}"
        }
        output.asFile.appendText("""
            |# LLVM Version configuration:
            |$llvmPropertiesString
        """.trimMargin())
    }
}