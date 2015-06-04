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
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
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

        val annotationProcessorFqNames = lookupAnnotationProcessors(aptFiles)

        val stubOutputDir = File(aptWorkingDir, WRAPPERS_DIRECTORY)
        generateAnnotationProcessorStubs(javaTask, annotationProcessorFqNames, stubOutputDir)

        val processorPath = setOf(stubOutputDir) + aptFiles
        setProcessorPath(javaTask, processorPath.joinToString(File.pathSeparator))
        javaTask.appendClasspath(stubOutputDir)

        addGeneratedSourcesOutputToCompilerArgs(javaTask, aptOutputDir)
    }

    fun afterJavaCompile() {
        val generatedFile = File(javaTask.getDestinationDir(), "$GEN_ANNOTATION/Cl.class")
        if (generatedFile.exists()) {
            generatedFile.delete()
        } else {
            project.getLogger().kotlinDebug("kapt: Java file stub was not found at $generatedFile")
        }
    }

    private fun generateJavaHackFile(aptDir: File, javaTask: JavaCompile) {
        val javaAptSourceDir = File(aptDir, "java_src")
        val javaHackPackageDir = File(javaAptSourceDir, GEN_ANNOTATION)

        javaHackPackageDir.mkdirs()

        val javaHackClFile = File(javaHackPackageDir, "Cl.java")
        javaHackClFile.writeText(
                "package __gen.annotation;" +
                "class Cl { @__gen.KotlinAptAnnotation boolean v; }")

        project.getLogger().kotlinDebug("kapt: Java file stub generated: $javaHackClFile")
        javaTask.source(javaAptSourceDir)
    }

    private fun generateAnnotationProcessorStubs(javaTask: JavaCompile, processorFqNames: Set<String>, outputDir: File) {
        generateKotlinAptAnnotation(outputDir)

        val stubOutputPackageDir = File(outputDir, "__gen")
        stubOutputPackageDir.mkdirs()

        for (processor in processorFqNames) {
            generateAnnotationProcessorWrapper(processor, "__gen", stubOutputPackageDir)
        }

        val annotationProcessorWrapperFqNames = processorFqNames
                .map { fqName -> "__gen." + getProcessorStubClassName(fqName) }
                .joinToString(",")

        addWrappersToCompilerArgs(javaTask, annotationProcessorWrapperFqNames)
    }

    private fun JavaCompile.appendClasspath(file: File) = setClasspath(getClasspath() + project.files(file))

    private fun addWrappersToCompilerArgs(javaTask: JavaCompile, wrapperFqNames: String) {
        javaTask.addCompilerArgument("-processor") { prevValue ->
            if (prevValue != null) "$prevValue,$wrapperFqNames" else wrapperFqNames
        }
    }

    private fun addGeneratedSourcesOutputToCompilerArgs(javaTask: JavaCompile, outputDir: File) {
        outputDir.mkdirs()

        javaTask.addCompilerArgument("-s") { prevValue ->
            if (prevValue != null)
                javaTask.getLogger().warn("Destination for generated sources was modified by kapt. Previous value = $prevValue")
            outputDir.getAbsolutePath()
        }
    }

    private fun setProcessorPath(javaTask: JavaCompile, path: String) {
        javaTask.addCompilerArgument("-processorpath") { prevValue ->
            if (prevValue != null)
                javaTask.getLogger().warn("Processor path was modified by kapt. Previous value = $prevValue")
            path
        }
    }

    private fun getProcessorStubClassName(processorFqName: String): String {
        return "AnnotationProcessorWrapper_${taskQualifier}_${processorFqName.replace('.', '_')}"
    }

    private inline fun JavaCompile.addCompilerArgument(name: String, value: (String?) -> String) {
        modifyCompilerArguments { args ->
            val argIndex = args.indexOfFirst { name == it }

            if (argIndex >= 0 && args.size() > (argIndex + 1)) {
                args[argIndex + 1] = value(args[argIndex + 1])
            }
            else {
                args.add(name)
                args.add(value(null))
            }
        }
    }

    private inline fun JavaCompile.modifyCompilerArguments(modifier: (MutableList<String>) -> Unit) {
        val compilerArgs: List<Any> = getOptions().getCompilerArgs()
        val newCompilerArgs = compilerArgs.mapTo(arrayListOf<String>()) { it.toString() }
        modifier(newCompilerArgs)
        getOptions().setCompilerArgs(newCompilerArgs)
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
        val outputFile = File(injectPackage, "$className.class")
        outputFile.writeBytes(bytes)

        project.getLogger().kotlinDebug("kapt: Stub annotation generated: $outputFile")
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
        val outputFile = File(outputDirectory, "$className.class")
        outputFile.writeBytes(bytes)

        project.getLogger().kotlinDebug("kapt: Wrapper for $processorFqName generated: $outputFile")
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

        project.getLogger().kotlinDebug("kapt: Discovered annotation processors: ${annotationProcessors.joinToString()}")
        return annotationProcessors
    }

}