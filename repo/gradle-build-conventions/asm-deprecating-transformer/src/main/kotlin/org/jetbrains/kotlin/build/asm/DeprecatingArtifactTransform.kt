/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.asm

import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.internal.file.archive.ZipEntryConstants.CONSTANT_TIME_FOR_ZIP_ENTRIES
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.util.PatternSet
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassWriter
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@CacheableTransform
abstract class DeprecatingArtifactTransform : TransformAction<DeprecatingArtifactTransform.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val pattern: Property<String>

        @get:Input
        val deprecationMessage: Property<String>

        @get:Input
        val exclusions: ListProperty<String>
    }

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputArtifact: Provider<FileSystemLocation>

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        if (!input.exists()) return

        val output = outputs.file(input.name)
        transformJar(
            input = input,
            output = output,
            pattern = parameters.pattern.get(),
            deprecationMessage = parameters.deprecationMessage.get(),
            exclusions = parameters.exclusions.get(),
        )
    }

    private fun transformJar(
        input: File,
        output: File,
        pattern: String,
        deprecationMessage: String,
        exclusions: List<String>,
    ) {

        val spec = deprecationPattern(pattern, exclusions).asSpec
        ZipOutputStream(output.outputStream()).use { outputStream ->
            archiveOperations.zipTree(input).visit {
                val outputEntry = ZipEntry(if (isDirectory) "$path/" else path).apply {
                    time = CONSTANT_TIME_FOR_ZIP_ENTRIES
                }
                outputStream.putNextEntry(outputEntry)
                if (!isDirectory) {
                    if (name.endsWith(".class") && spec.isSatisfiedBy(this)) {
                        open().use { inputStream ->
                            val classReader = ClassReader(inputStream)
                            val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
                            val classVisitor = DeprecatingClassTransformer(classWriter, deprecationMessage) { className ->
                                require(className.contains('.')) {
                                    "Deprecating classes in the default (unnamed) package is not supported. Tried to deprecate $className"
                                }
                            }
                            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                            outputStream.write(classWriter.toByteArray())
                        }
                    } else {
                        copyTo(outputStream)
                    }
                }
                outputStream.closeEntry()
            }
        }
    }
}


/**
 * Transforms class FQN-like pattern to file names pattern
 * Examples:
 * * `org.example.**` -> `org/example/**/*.class`
 * * `org.example.Class` -> `org/example/Class.class`
 */
internal fun deprecationPattern(pattern: String, exclusions: List<String>): PatternSet = PatternSet().apply {
    include(pattern.toArchivePattern())
    exclude(exclusions.map { it.toArchivePattern() })
}

private fun String.toArchivePattern() = "${replace('.', '/').replace("**", "**/*")}.class"
