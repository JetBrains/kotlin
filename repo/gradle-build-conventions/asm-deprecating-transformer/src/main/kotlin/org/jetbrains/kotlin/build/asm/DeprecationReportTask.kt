/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.asm

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

@CacheableTask
abstract class DeprecationReportTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputJar: RegularFileProperty

    @get:Input
    abstract val pattern: Property<String>

    @get:Input
    abstract val exclusions: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @TaskAction
    fun generateReport() {
        val spec = deprecationPattern(pattern.get(), exclusions.get()).asSpec
        val deprecatedPackages = sortedSetOf<String>()
        archiveOperations.zipTree(inputJar).visit {
            if (name.endsWith(".class") && spec.isSatisfiedBy(this)) {
                val className = path.removeSuffix(".class").replace('/', '.')
                require(className.contains('.')) {
                    "Deprecating classes in the default (unnamed) package is not supported. Tried to deprecate $className"
                }
                logger.info("Deprecated class $className")
                deprecatedPackages.add(className.substringBeforeLast('.'))
            }
        }
        outputFile.get().asFile.bufferedWriter().use { writer ->
            writer.appendLine("The following packages are (non-transitively) deprecated by the `asm-deprecating-transformer` plugin:")
            deprecatedPackages.forEach { packageName ->
                writer.appendLine("package $packageName")
            }
        }
    }
}