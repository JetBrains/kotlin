/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.org.objectweb.asm.ClassWriter
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.Opcodes.*
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

public class AnnotationProcessingManager(private val task: KotlinCompile) {

    private val project = task.getProject()

    private companion object {
        val JAVA_FQNAME_PATTERN = "^([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*$".toRegex()
        val ANNOTATIONS_FILENAME = "annotations.txt"
    }

    fun getAnnotationFile(outputDirFile: String): File {
        val aptDir = File(outputDirFile, "0apt")
        aptDir.mkdirs()
        return File(aptDir, ANNOTATIONS_FILENAME)
    }

    fun afterKotlinCompile(outputDirFile: File) {
        [suppress("UNCHECKED_CAST")]
        val javaTask = (task.getExtensions().getExtraProperties().get("javaTask") as? WeakReference<JavaCompile>)?.get()
        val aptFiles = task.aptFiles
        if (javaTask == null || aptFiles.isEmpty()) return

        val aptDir = File(outputDirFile, "0apt")
        val annotationDeclarationsFile = File(aptDir, ANNOTATIONS_FILENAME)

        generateJavaHackFile(aptDir, javaTask)

        javaTask.appendClasspath(aptFiles)

        val annotationProcessorFqNames = lookupAnnotationProcessors(aptFiles)
        generateAnnotationProcessorStubs(aptDir, javaTask, annotationProcessorFqNames)
    }

    private fun generateJavaHackFile(aptDir: File, javaTask: JavaCompile) {
        val javaAptSourceDir = File(aptDir, "java_src")
        val javaHackPackageDir = File(javaAptSourceDir, "__gen/annotation")

        javaHackPackageDir.mkdirs()

        val javaHackClFile = File(javaHackPackageDir, "Cl.java")
        javaHackClFile.writeText(
                "package __gen.annotation;" +
                "class Cl { @__gen.KotlinAptAnnotation boolean v; }")

        javaTask.source(javaAptSourceDir)
    }

    private fun generateAnnotationProcessorStubs(aptDir: File, javaTask: JavaCompile, apFqNames: Set<String>) {
        val stubOutputDir = File(aptDir, "wrappers")

        generateKotlinAptAnnotation(stubOutputDir)

        val stubOutputPackageDir = File(stubOutputDir, "__gen")
        stubOutputPackageDir.mkdirs()

        for (processor in apFqNames) {
            generateAnnotationProcessorWrapper(processor, "__gen", stubOutputPackageDir)
        }

        val annotationProcessorWrapperFqNames = apFqNames
                .map { "__gen.AnnotationProcessorWrapper_${it.replace('.', '_')}" }
                .joinToString(",")

        javaTask.appendClasspath(stubOutputDir)
        addWrappersToCompilerArgs(javaTask, annotationProcessorWrapperFqNames)
    }

    private fun JavaCompile.appendClasspath(file: File) = setClasspath(getClasspath() + project.files(file))

    private fun JavaCompile.appendClasspath(files: Iterable<File>) = setClasspath(getClasspath() + project.files(files))

    private fun addWrappersToCompilerArgs(javaTask: JavaCompile, wrapperFqNames: String) {
        val compilerArgs = javaTask.getOptions().getCompilerArgs()
        val processorArgIndex = compilerArgs.indexOfFirst { "-processor" == it }

        // Already has a "-processor" argument (and it is not the last one)
        if (processorArgIndex >= 0 && compilerArgs.size() > (processorArgIndex + 1)) {
            compilerArgs[processorArgIndex + 1] =
                    compilerArgs[processorArgIndex + 1] + "," + wrapperFqNames
        }
        else {
            compilerArgs.add("-processor")
            compilerArgs.add(wrapperFqNames)
        }

        javaTask.getOptions().setCompilerArgs(compilerArgs)
    }

    private fun generateKotlinAptAnnotation(outputDirectory: File) {
        val packageName = "__gen"
        val className = "KotlinAptAnnotation"
        val classFqName = "$packageName/$className"

        val bytes = with (ClassWriter(0)) {
            visit(49, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE + ACC_ANNOTATION, classFqName,
                    null, null, arrayOf("java/lang/annotation/Annotation"))
            visitSource(null, null)
            visitEnd()
            toByteArray()
        }

        val injectPackage = File(outputDirectory, packageName)
        injectPackage.mkdirs()
        File(injectPackage, "$className.class").writeBytes(bytes)
    }

    private fun generateAnnotationProcessorWrapper(processorFqName: String, packageName: String, outputDirectory: File) {
        val className = "AnnotationProcessorWrapper_${processorFqName.replace('.', '_')}"
        val classFqName = "$packageName/$className"

        val bytes = with (ClassWriter(0)) {
            val superClass = "org/kotlin/annotations/AnnotationProcessorWrapper"

            visit(49, ACC_PUBLIC + ACC_SUPER, classFqName, null,
                    superClass, null)

            visitSource(null, null)

            with (visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)) {
                visitVarInsn(ALOAD, 0)
                visitLdcInsn(processorFqName)
                visitMethodInsn(INVOKESPECIAL, superClass, "<init>", "(Ljava/lang/String;)V", false)
                visitInsn(RETURN)
                visitMaxs(2, 1)
                visitEnd()
            }

            visitEnd()
            toByteArray()
        }
        File(outputDirectory, "$className.class").writeBytes(bytes)
    }

    private fun lookupAnnotationProcessors(files: Set<File>): Set<String> {
        fun withZipFile(file: File, job: (ZipFile) -> Unit) {
            var zipFile: ZipFile? = null
            try {
                zipFile = ZipFile(file)
                job(zipFile)
            }
            catch (e: IOException) {
                // Do nothing (do not continue to search for annotation processors on error)
            }
            catch (e: IllegalStateException) {
                // ZipFile was already closed for some reason
            }
            finally {
                try {
                    zipFile?.close()
                }
                catch (e: IOException) {}
            }
        }

        val annotationProcessors = hashSetOf<String>()

        fun processLines(lines: Sequence<String>) {
            for (line in lines) {
                if (line.isBlank() || !JAVA_FQNAME_PATTERN.matcher(line).matches()) continue
                annotationProcessors.add(line)
            }
        }

        for (file in files) {
            withZipFile(file) { zipFile ->
                val entry = zipFile.getEntry("META-INF/services/javax.annotation.processing.Processor")
                if (entry != null) {
                    zipFile.getInputStream(entry).reader().useLines { lines ->
                        processLines(lines)
                    }
                }
            }
        }

        return annotationProcessors
    }

}