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

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.zip.ZipFile

public class AnnotationProcessingManager(
        private val task: AbstractCompile,
        val javaTask: JavaCompile,
        val taskQualifier: String,
        val aptFiles: Set<File>,
        val aptOutputDir: File,
        val aptWorkingDir: File) {

    private val project = task.getProject()

    private companion object {
        val JAVA_FQNAME_PATTERN = "^([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*$".toRegex()
        val WRAPPERS_DIRECTORY = "wrappers"
        val GEN_ANNOTATION = "__gen/annotation"
    }

    fun getAnnotationFile(): File {
        aptWorkingDir.mkdirs()
        return File(aptWorkingDir, "$WRAPPERS_DIRECTORY/annotations.$taskQualifier.txt")
    }

    fun setupKapt() {
        if (aptFiles.isEmpty()) return

        generateJavaHackFile(aptWorkingDir, javaTask)

        javaTask.appendClasspath(aptFiles)

        val annotationProcessorFqNames = lookupAnnotationProcessors(aptFiles)
        generateAnnotationProcessorStubs(aptWorkingDir, javaTask, annotationProcessorFqNames)

        addGeneratedSourcesOutputToCompilerArgs(javaTask, aptOutputDir)
    }

    fun afterJavaCompile() {
        val generatedFile = File(javaTask.getDestinationDir(), "$GEN_ANNOTATION/Cl.class")
        if (generatedFile.exists()) generatedFile.delete()
    }

    private fun generateJavaHackFile(aptDir: File, javaTask: JavaCompile) {
        val javaAptSourceDir = File(aptDir, "java_src")
        val javaHackPackageDir = File(javaAptSourceDir, GEN_ANNOTATION)

        javaHackPackageDir.mkdirs()

        val javaHackClFile = File(javaHackPackageDir, "Cl.java")
        javaHackClFile.writeText(
                "package __gen.annotation;" +
                "class Cl { @__gen.KotlinAptAnnotation boolean v; }")

        javaTask.source(javaAptSourceDir)
    }

    private fun generateAnnotationProcessorStubs(aptDir: File, javaTask: JavaCompile, processorFqNames: Set<String>) {
        val stubOutputDir = File(aptDir, "$WRAPPERS_DIRECTORY")

        generateKotlinAptAnnotation(stubOutputDir)

        val stubOutputPackageDir = File(stubOutputDir, "__gen")
        stubOutputPackageDir.mkdirs()

        for (processor in processorFqNames) {
            generateAnnotationProcessorWrapper(processor, "__gen", stubOutputPackageDir)
        }

        val annotationProcessorWrapperFqNames = processorFqNames
                .map { fqName -> "__gen." + getProcessorStubClassName(fqName) }
                .joinToString(",")

        javaTask.appendClasspath(stubOutputDir)
        addWrappersToCompilerArgs(javaTask, annotationProcessorWrapperFqNames)
    }

    private fun JavaCompile.appendClasspath(file: File) = setClasspath(getClasspath() + project.files(file))

    private fun JavaCompile.appendClasspath(files: Iterable<File>) = setClasspath(getClasspath() + project.files(files))

    private fun addWrappersToCompilerArgs(javaTask: JavaCompile, wrapperFqNames: String) {
        val compilerArgs = javaTask.getOptions().getCompilerArgs()
        val argIndex = compilerArgs.indexOfFirst { "-processor" == it }

        // Already has a "-processor" argument (and it is not the last one)
        if (argIndex >= 0 && compilerArgs.size() > (argIndex + 1)) {
            compilerArgs[argIndex + 1] =
                    compilerArgs[argIndex + 1] + "," + wrapperFqNames
        }
        else {
            compilerArgs.add("-processor")
            compilerArgs.add(wrapperFqNames)
        }

        javaTask.getOptions().setCompilerArgs(compilerArgs)
    }

    private fun getProcessorStubClassName(processorFqName: String): String {
        return "AnnotationProcessorWrapper_${taskQualifier}_${processorFqName.replace('.', '_')}"
    }

    private fun addGeneratedSourcesOutputToCompilerArgs(javaTask: JavaCompile, outputDir: File) {
        outputDir.mkdirs()

        val compilerArgs = javaTask.getOptions().getCompilerArgs().toArrayList()

        val argIndex = compilerArgs.indexOfFirst { "-s" == it }

        if (argIndex >= 0 && compilerArgs.size() > (argIndex + 1)) {
            task.getLogger().warn("Destination for generated sources was modified by kapt. " +
                    "Previous value = " + compilerArgs[argIndex + 1])

            compilerArgs[argIndex + 1] = outputDir.getAbsolutePath()
        }
        else {
            compilerArgs.add("-s")
            compilerArgs.add(outputDir.getAbsolutePath())
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
        val className = getProcessorStubClassName(processorFqName)
        val classFqName = "$packageName/$className"

        val bytes = with (ClassWriter(0)) {
            val superClass = "org/jetbrains/kotlin/annotation/AnnotationProcessorWrapper"

            visit(49, ACC_PUBLIC + ACC_SUPER, classFqName, null,
                    superClass, null)

            visitSource(null, null)

            with (visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)) {
                visitVarInsn(ALOAD, 0)
                visitLdcInsn(processorFqName)
                visitLdcInsn(taskQualifier)
                visitMethodInsn(INVOKESPECIAL, superClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V", false)
                visitInsn(RETURN)
                visitMaxs(3 /*max stack*/, 1 /*max locals*/)
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