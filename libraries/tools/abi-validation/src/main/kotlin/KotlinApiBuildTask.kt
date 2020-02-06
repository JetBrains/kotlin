/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.validation

import kotlinx.validation.api.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.*

open class KotlinApiBuildTask : DefaultTask() {

    private val extension = project.rootProject.extensions.getByType(ApiValidationExtension::class.java)

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputClassesDirs: FileCollection

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputDependencies: FileCollection

    @OutputDirectory
    lateinit var outputApiDir: File

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
            .filterOutNonPublic(extension.ignoredPackages)

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

