/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ib

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.ib.InteropBundlePlugin.Companion.konanTargets
import org.jetbrains.kotlin.gradle.utils.`is`
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import kotlin.system.exitProcess

abstract class CreateInteropBundleTask : DefaultTask() {

    internal data class KonanTargetWithConfiguration(
        @get:Input val konanTarget: KonanTarget,
        @get:Classpath val configuration: Configuration
    )

    private val konanTargetConfigurations =
        konanTargets.associateWith { konanTarget -> project.configurations.maybeCreate(konanTarget.name) }

    @OutputDirectory
    val outputDirectory: Property<File> = project.objects.property(File::class.java)
        .convention(project.buildDir.resolve(project.name))

    @get:Nested
    @Suppress("unused")
    internal val targetsWithConfigurations = konanTargetConfigurations.map { (konanTarget, configuration) ->
        KonanTargetWithConfiguration(konanTarget, configuration)
    }

    @TaskAction
    internal fun createInteropBundle() {
        outputDirectory.get().mkdirs()
        val interopBundle = InteropBundleDirectory(outputDirectory.get())
        konanTargetConfigurations.forEach { (konanTarget, configuration) ->
            val libraryFiles = configuration.resolve()
            if (libraryFiles.isEmpty()) return@forEach
            libraryFiles.forEach { libraryFile ->
                require(libraryFile.extension == "klib") { "Only klib files supported" }
                val targetFile = interopBundle.resolve(konanTarget)
                if (libraryFile.extension == "klib") {
                    project.copy { copySpec ->
                        copySpec.from(project.zipTree(libraryFile))
                        copySpec.into(targetFile)
                    }
                }
            }
        }
    }
}
