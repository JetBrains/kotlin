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
import java.util.jar.JarFile
import javax.inject.Inject

open class KotlinApiBuildTask @Inject constructor(
) : DefaultTask() {

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    var inputClassesDirs: FileCollection? = null

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    val inputJar: RegularFileProperty = this.project.objects.fileProperty()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputDependencies: FileCollection

    @OutputDirectory
    lateinit var outputApiDir: File

    @get:Input
    var ignoredPackages : Set<String> = emptySet()

    @get:Input
    var nonPublicMarkers : Set<String> = emptySet()

    @get:Input
    var ignoredClasses : Set<String> = emptySet()

    @get:Internal
    internal val projectName = project.name

    @TaskAction
    fun generate() {
        cleanup(outputApiDir)
        outputApiDir.mkdirs()

        val inputClassesDirs = inputClassesDirs
        if (listOfNotNull(inputClassesDirs, inputJar.orNull).size != 1) {
            throw GradleException("KotlinApiBuildTask should have either inputClassesDirs, or inputJar properties set")
        }
        val signatures = when {
            inputClassesDirs != null ->
                inputClassesDirs.asFileTree.asSequence()
                    .filter {
                        !it.isDirectory && it.name.endsWith(".class") && !it.name.startsWith("META-INF/")
                    }
                    .map { it.inputStream() }
                    .loadApiFromJvmClasses()
            inputJar.isPresent ->
                JarFile(inputJar.get().asFile)
                    .loadApiFromJvmClasses()
            else ->
                error("Unreachable")
        }


        val filteredSignatures = signatures
            .filterOutNonPublic(ignoredPackages, ignoredClasses)
            .filterOutAnnotated(nonPublicMarkers.map { it.replace(".", "/") }.toSet())

        outputApiDir.resolve("$projectName.api").bufferedWriter().use { writer ->
            filteredSignatures
                .sortedBy { it.name }
                .forEach { api ->
                    writer.append(api.signature).appendLine(" {")
                    api.memberSignatures
                        .sortedWith(MEMBER_SORT_ORDER)
                        .forEach { writer.append("\t").appendLine(it.signature) }
                    writer.appendLine("}\n")
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

