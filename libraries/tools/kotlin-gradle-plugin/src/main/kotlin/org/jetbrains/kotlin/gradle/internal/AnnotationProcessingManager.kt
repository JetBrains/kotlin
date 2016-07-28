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

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.kapt.generateAnnotationProcessorWrapper
import org.jetbrains.kotlin.gradle.tasks.kapt.generateKotlinAptAnnotation
import java.io.File
import java.io.IOException
import java.util.*
import java.util.zip.ZipFile

fun Project.initKapt(
        kotlinTask: KotlinCompile,
        javaTask: AbstractCompile,
        kaptManager: AnnotationProcessingManager,
        variantName: String,
        kotlinOptions: Any?,
        subpluginEnvironment: SubpluginEnvironment,
        taskFactory: (suffix: String) -> AbstractCompile
): AbstractCompile? {
    val kaptExtension = extensions.getByType(KaptExtension::class.java)
    val kotlinAfterJavaTask: AbstractCompile?

    if (kaptExtension.generateStubs) {
        kotlinAfterJavaTask = createKotlinAfterJavaTask(javaTask, kotlinTask, kotlinOptions, taskFactory)
        mapKotlinTaskProperties(this, kotlinAfterJavaTask)

        kotlinTask.logger.kotlinDebug("kapt: Using class file stubs")

        val stubsDir = File(buildDir, "tmp/kapt/$variantName/classFileStubs")
        stubsDir.mkdirs()
        kotlinTask.extensions.extraProperties.set("kaptStubsDir", stubsDir)
        javaTask.appendClasspathDynamically(stubsDir)
        kotlinTask.appendClasspathDynamically(stubsDir)

        val javaDestinationDir = project.files(javaTask.destinationDir)
        javaTask.doLast {
            kotlinAfterJavaTask.source(kotlinTask.source)
            // we don't want kotlinAfterJavaTask to track modifications in generated class
            kotlinAfterJavaTask.classpath -= javaDestinationDir
        }
        kotlinAfterJavaTask.doFirst {
            kotlinAfterJavaTask.classpath += javaDestinationDir
        }
        kotlinAfterJavaTask.doLast {
            kotlinAfterJavaTask.classpath -= javaDestinationDir
        }

        subpluginEnvironment.addSubpluginArguments(this, kotlinAfterJavaTask)
    } else {
        kotlinAfterJavaTask = null
        kotlinTask.logger.kotlinDebug("kapt: Class file stubs are not used")
    }

    javaTask.appendClasspathDynamically(kaptManager.wrappersDirectory)
    javaTask.source(kaptManager.hackAnnotationDir)

    if (kaptExtension.inheritedAnnotations) {
        kotlinTask.extensions.extraProperties.set("kaptInheritedAnnotations", true)
    }

    kotlinTask.doFirst {
        kaptManager.generateJavaHackFile()
        kotlinAfterJavaTask?.source(kaptManager.getGeneratedKotlinSourceDir())
    }

    var originalJavaCompilerArgs: List<String>? = null
    javaTask.doFirst {
        originalJavaCompilerArgs = (javaTask as JavaCompile).options.compilerArgs
        kaptManager.setupKapt()
        kaptManager.generateJavaHackFile()
        kotlinAfterJavaTask?.source(kaptManager.getGeneratedKotlinSourceDir())
    }

    javaTask.doLast {
        (javaTask as JavaCompile).options.compilerArgs = originalJavaCompilerArgs
        kaptManager.afterJavaCompile()
    }

    kotlinTask.storeKaptAnnotationsFile(kaptManager)
    return kotlinAfterJavaTask
}

private fun Project.createKotlinAfterJavaTask(
        javaTask: AbstractCompile,
        kotlinTask: AbstractCompile,
        kotlinOptions: Any?,
        taskFactory: (suffix: String) -> AbstractCompile
): AbstractCompile {
    val kotlinAfterJavaTask = with (taskFactory(KOTLIN_AFTER_JAVA_TASK_SUFFIX)) {
        destinationDir = kotlinTask.destinationDir
        classpath = kotlinTask.classpath - project.files(javaTask.destinationDir)
        this
    }

    kotlinAfterJavaTask.dependsOn(javaTask)
    javaTask.finalizedByIfNotFailed(kotlinAfterJavaTask)

    kotlinAfterJavaTask.extensions.extraProperties.set("defaultModuleName", "${project.name}-${kotlinTask.name}")
    if (kotlinOptions != null) {
        kotlinAfterJavaTask.setProperty("kotlinOptions", kotlinOptions)
    }

    return kotlinAfterJavaTask
}

public class AnnotationProcessingManager(
        private val task: AbstractCompile,
        private val javaTask: JavaCompile,
        private val taskQualifier: String,
        private val aptFiles: Set<File>,
        val aptOutputDir: File,
        private val aptWorkingDir: File,
        private val androidVariant: Any? = null) {

    private val project = task.project
    private val random = Random()
    val wrappersDirectory = File(aptWorkingDir, "wrappers")
    val hackAnnotationDir = File(aptWorkingDir, "java_src")

    private companion object {
        val JAVA_FQNAME_PATTERN = "^([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*$".toRegex()
        val GEN_ANNOTATION = "__gen/annotation"

        private val ANDROID_APT_PLUGIN_ID = "com.neenbedankt.android-apt"
    }

    fun getAnnotationFile(): File {
        if (!aptWorkingDir.exists()) aptWorkingDir.mkdirs()
        return File(wrappersDirectory, "annotations.$taskQualifier.txt")
    }

    fun getGeneratedKotlinSourceDir(): File {
        val kotlinGeneratedDir = File(aptWorkingDir, "kotlinGenerated")
        if (!kotlinGeneratedDir.exists()) kotlinGeneratedDir.mkdirs()
        return kotlinGeneratedDir
    }

    fun setupKapt() {
        if (aptFiles.isEmpty()) return

        if (project.plugins.findPlugin(ANDROID_APT_PLUGIN_ID) != null) {
            project.logger.warn("Please do not use `$ANDROID_APT_PLUGIN_ID` with kapt.")
        }

        val annotationProcessorFqNames = lookupAnnotationProcessors(aptFiles)

        generateAnnotationProcessorStubs(javaTask, annotationProcessorFqNames, wrappersDirectory)

        val processorPath = setOf(wrappersDirectory) + aptFiles
        setProcessorPath(javaTask, (processorPath + javaTask.classpath).joinToString(File.pathSeparator))

        if (aptOutputDir.exists()) {
            aptOutputDir.deleteRecursively()
        }
        addGeneratedSourcesOutputToCompilerArgs(javaTask, aptOutputDir)

        appendAnnotationsArguments()
        appendAdditionalComplerArgs()
    }

    fun afterJavaCompile() {
        val generatedFile = File(javaTask.destinationDir, "$GEN_ANNOTATION/Cl.class")
        if (generatedFile.exists()) {
            generatedFile.delete()
        } else {
            project.logger.kotlinDebug("kapt: Java file stub was not found at $generatedFile")
        }
    }

    fun generateJavaHackFile() {
        val javaHackPackageDir = File(hackAnnotationDir, GEN_ANNOTATION)

        if (!javaHackPackageDir.exists()) javaHackPackageDir.mkdirs()

        val javaHackClFile = File(javaHackPackageDir, "Cl.java")
        val previouslyExisted = javaHackClFile.exists()
        val comment = System.currentTimeMillis().toString() + "-" + random.nextInt()

        javaHackClFile.writeText(
                "// $comment\n" +
                        "package __gen.annotation;\n" +
                        "class Cl { @__gen.KotlinAptAnnotation boolean v; }")

        project.logger.kotlinDebug("kapt: Java file stub generated: $javaHackClFile " +
                "(previously existed: $previouslyExisted)")
    }

    private fun appendAnnotationsArguments() {
        javaTask.modifyCompilerArguments { list ->
            list.add("-Akapt.annotations=" + getAnnotationFile())
            list.add("-Akapt.kotlin.generated=" + getGeneratedKotlinSourceDir())
        }
    }

    private fun appendAdditionalComplerArgs() {
        val kaptExtension = project.extensions.getByType(KaptExtension::class.java)
        val args = kaptExtension.getAdditionalArguments(project, androidVariant, getAndroidExtension())
        if (args.isEmpty()) return

        javaTask.modifyCompilerArguments { list ->
            list.addAll(args)
        }
    }

    private fun generateAnnotationProcessorStubs(javaTask: JavaCompile, processorFqNames: Set<String>, outputDir: File) {
        val aptAnnotationFile = generateKotlinAptAnnotation(outputDir)
        project.logger.kotlinDebug("kapt: Stub annotation generated: $aptAnnotationFile")

        val stubOutputPackageDir = File(outputDir, "__gen")
        stubOutputPackageDir.mkdirs()

        for (processorFqName in processorFqNames) {
            val wrapperFile = generateAnnotationProcessorWrapper(
                    processorFqName,
                    "__gen",
                    stubOutputPackageDir,
                    getProcessorStubClassName(processorFqName),
                    taskQualifier)

            project.logger.kotlinDebug("kapt: Wrapper for $processorFqName generated: $wrapperFile")
        }

        val annotationProcessorWrapperFqNames = processorFqNames
                .map { fqName -> "__gen." + getProcessorStubClassName(fqName) }
                .joinToString(",")

        addWrappersToCompilerArgs(javaTask, annotationProcessorWrapperFqNames)
    }

    private fun addWrappersToCompilerArgs(javaTask: JavaCompile, wrapperFqNames: String) {
        javaTask.addCompilerArgument("-processor") { prevValue ->
            if (prevValue != null) "$prevValue,$wrapperFqNames" else wrapperFqNames
        }
    }

    private fun getAndroidExtension(): BaseExtension? {
        try {
            return project.extensions.getByName("android") as BaseExtension
        } catch (e: UnknownDomainObjectException) {
            return null
        }
    }

    private fun addGeneratedSourcesOutputToCompilerArgs(javaTask: JavaCompile, outputDir: File) {
        outputDir.mkdirs()

        javaTask.addCompilerArgument("-s") { prevValue ->
            if (prevValue != null)
                javaTask.logger.warn("Destination for generated sources was modified by kapt. Previous value = $prevValue")
            outputDir.absolutePath
        }
    }

    private fun setProcessorPath(javaTask: JavaCompile, path: String) {
        javaTask.addCompilerArgument("-processorpath") { prevValue ->
            if (prevValue != null)
                javaTask.logger.warn("Processor path was modified by kapt. Previous value = $prevValue")
            path
        }
    }

    private fun getProcessorStubClassName(processorFqName: String): String {
        return "AnnotationProcessorWrapper_${taskQualifier}_${processorFqName.replace('.', '_')}"
    }

    private inline fun JavaCompile.addCompilerArgument(name: String, value: (String?) -> String) {
        modifyCompilerArguments { args ->
            val argIndex = args.indexOfFirst { name == it }

            if (argIndex >= 0 && args.size > (argIndex + 1)) {
                args[argIndex + 1] = value(args[argIndex + 1])
            }
            else {
                args.add(name)
                args.add(value(null))
            }
        }
    }

    private inline fun JavaCompile.modifyCompilerArguments(modifier: (MutableList<String>) -> Unit) {
        val compilerArgs: List<Any> = this.options.compilerArgs
        val newCompilerArgs = compilerArgs.mapTo(arrayListOf<String>()) { it.toString() }
        modifier(newCompilerArgs)
        options.compilerArgs = newCompilerArgs
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
                if (line.isBlank() || !JAVA_FQNAME_PATTERN.matches(line)) continue
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

        project.logger.kotlinDebug("kapt: Discovered annotation processors: ${annotationProcessors.joinToString()}")
        return annotationProcessors
    }
}