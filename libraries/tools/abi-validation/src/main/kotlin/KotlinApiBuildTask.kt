/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import java.io.*
import javax.inject.Inject

open class KotlinApiBuildTask @Inject constructor(
    private val extension: ApiValidationExtension
) : DefaultTask() {

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputClassesDirs: FileCollection

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputDependencies: FileCollection

    @OutputDirectory
    lateinit var outputApiDir: File

    @get:Input
    val ignoredPackages : Set<String> get() = extension.ignoredPackages

    @get:Input
    val nonPublicMarkers : Set<String> get() = extension.nonPublicMarkers

    @get:Input
    val ignoredClasses : Set<String> get() = extension.ignoredClasses

    @TaskAction
    fun generate() {
        cleanup(outputApiDir)
        outputApiDir.mkdirs()

        val signatures = inputClassesDirs.asFileTree.asSequence()
            .filter {
                !it.isDirectory && it.name.endsWith(".class") && !it.name.startsWith("META-INF/")
            }
            .map { it.inputStream() }
            .loadApiFromJvmClasses()
            .filterOutNonPublic(ignoredPackages, ignoredClasses)
            .filterOutAnnotated(nonPublicMarkers.map { it.replace(".", "/") }.toSet())

        outputApiDir.resolve("${project.name}.api").bufferedWriter().use { writer ->
            signatures
                .sortedBy { it.name }
                .forEach { api ->
                    writer.append(api.signature).appendln(" {")
                    api.memberSignatures
                        .sortedWith(MEMBER_SORT_ORDER)
                        .forEach { writer.append("\t").appendln(it.signature) }
                    writer.appendln("}\n")
                }
        }
    }

    private fun cleanup(file: File) {
        if (file.exists()) {
            val listing = file.listFiles()
            if (listing != null) {
                for (sub in listing) {
                    cleanup(sub)
                }
            }
            file.delete()
        }
    }
}

