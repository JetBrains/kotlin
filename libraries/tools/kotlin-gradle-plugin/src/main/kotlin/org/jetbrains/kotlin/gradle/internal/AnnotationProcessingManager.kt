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
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

fun Project.initKapt(
        kotlinTask: AbstractCompile,
        javaTask: AbstractCompile,
        kaptManager: AnnotationProcessingManager,
        variantName: String,
        kotlinOutputDir: File,
        subpluginEnvironment: SubpluginEnvironment,
        taskFactory: (suffix: String) -> AbstractCompile
): AbstractCompile? {
    val kaptExtension = extensions.getByType(javaClass<KaptExtension>())
    val kotlinAfterJavaTask: AbstractCompile?

    if (kaptExtension.generateStubs) {
        kotlinAfterJavaTask = createKotlinAfterJavaTask(javaTask, kotlinOutputDir, taskFactory)

        kotlinTask.logger.kotlinDebug("kapt: Using class file stubs")

        val stubsDir = File(getBuildDir(), "tmp/kapt/$variantName/classFileStubs")
        kotlinTask.extensions.extraProperties.set("kaptStubsDir", stubsDir)

        javaTask.classpath += files(stubsDir)

        kotlinTask.doFirst {
            kotlinAfterJavaTask.source(kotlinTask.source)
        }

        subpluginEnvironment.addSubpluginArguments(this, kotlinAfterJavaTask)
    } else {
        kotlinAfterJavaTask = null
        kotlinTask.logger.kotlinDebug("kapt: Class file stubs are not used")
    }

    if (kaptExtension.inheritedAnnotations) {
        kotlinTask.extensions.extraProperties.set("kaptInheritedAnnotations", true)
    }

    kotlinTask.doFirst {
        kaptManager.generateJavaHackFile()
        kotlinAfterJavaTask?.source(kaptManager.getGeneratedKotlinSourceDir())
    }

    javaTask.doFirst {
        kaptManager.setupKapt()
        kaptManager.generateJavaHackFile()
        kotlinAfterJavaTask?.source(kaptManager.getGeneratedKotlinSourceDir())
    }

    javaTask.doLast {
        kaptManager.afterJavaCompile()
    }

    kotlinTask.storeKaptAnnotationsFile(kaptManager)
    return kotlinAfterJavaTask
}

private fun Project.createKotlinAfterJavaTask(
        javaTask: AbstractCompile,
        kotlinOutputDir: File,
        taskFactory: (suffix: String) -> AbstractCompile
): AbstractCompile {
    val kotlinAfterJavaTask = with (taskFactory(KOTLIN_AFTER_JAVA_TASK_SUFFIX)) {
        setProperty("kotlinDestinationDir", kotlinOutputDir)
        destinationDir = javaTask.destinationDir
        classpath = javaTask.classpath
        this
    }

    getAllTasks(false)
            .flatMap { it.getValue() }
            .filter { javaTask in it.taskDependencies.getDependencies(it) }
            .forEach { it.dependsOn(kotlinAfterJavaTask) }

    kotlinAfterJavaTask.dependsOn(javaTask)

    return kotlinAfterJavaTask
}

public class AnnotationProcessingManager(
        private val task: AbstractCompile,
        private val javaTask: JavaCompile,
        private val taskQualifier: String,
        private val aptFiles: Set<File>,
        private val aptOutputDir: File,
        private val aptWorkingDir: File,
        private val coreClassLoader: ClassLoader,
        private val androidVariant: Any? = null) {

    private val project = task.project

    private companion object {
        val JAVA_FQNAME_PATTERN = "^([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*$".toRegex()
        val WRAPPERS_DIRECTORY = "wrappers"
        val GEN_ANNOTATION = "__gen/annotation"

        private val ANDROID_APT_PLUGIN_ID = "com.neenbedankt.android-apt"
    }

    fun getAnnotationFile(): File {
        if (!aptWorkingDir.exists()) aptWorkingDir.mkdirs()
        return File(aptWorkingDir, "$WRAPPERS_DIRECTORY/annotations.$taskQualifier.txt")
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

        val stubOutputDir = File(aptWorkingDir, WRAPPERS_DIRECTORY)
        generateAnnotationProcessorStubs(javaTask, annotationProcessorFqNames, stubOutputDir)

        val processorPath = setOf(stubOutputDir) + aptFiles
        setProcessorPath(javaTask, processorPath.joinToString(File.pathSeparator))
        javaTask.appendClasspath(stubOutputDir)

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
        val javaAptSourceDir = File(aptWorkingDir, "java_src")
        val javaHackPackageDir = File(javaAptSourceDir, GEN_ANNOTATION)

        if (!javaHackPackageDir.exists()) javaHackPackageDir.mkdirs()

        val javaHackClFile = File(javaHackPackageDir, "Cl.java")
        if (!javaHackClFile.exists()) {
            javaHackClFile.writeText("package __gen.annotation; class Cl { @__gen.KotlinAptAnnotation boolean v; }")
            project.logger.kotlinDebug("kapt: Java file stub generated: $javaHackClFile")
        }

        if (!javaTask.source.contains(javaHackClFile)) {
            javaTask.source(javaAptSourceDir)
        }
    }

    private fun appendAnnotationsArguments() {
        javaTask.modifyCompilerArguments { list ->
            list.add("-Akapt.annotations=" + getAnnotationFile())
            list.add("-Akapt.kotlin.generated=" + getGeneratedKotlinSourceDir())
        }
    }

    private fun appendAdditionalComplerArgs() {
        val kaptExtension = project.extensions.getByType(javaClass<KaptExtension>())
        val args = kaptExtension.getAdditionalArguments(project, androidVariant, getAndroidExtension())
        if (args.isEmpty()) return

        javaTask.modifyCompilerArguments { list ->
            list.addAll(args)
        }
    }

    private fun generateAnnotationProcessorStubs(javaTask: JavaCompile, processorFqNames: Set<String>, outputDir: File) {
        val aptAnnotationFile = invokeCoreKaptMethod("generateKotlinAptAnnotation", outputDir) as File
        project.logger.kotlinDebug("kapt: Stub annotation generated: $aptAnnotationFile")

        val stubOutputPackageDir = File(outputDir, "__gen")
        stubOutputPackageDir.mkdirs()

        for (processorFqName in processorFqNames) {
            val wrapperFile = invokeCoreKaptMethod("generateAnnotationProcessorWrapper",
                    processorFqName,
                    "__gen",
                    stubOutputPackageDir,
                    getProcessorStubClassName(processorFqName),
                    taskQualifier) as File

            project.getLogger().kotlinDebug("kapt: Wrapper for $processorFqName generated: $wrapperFile")
        }

        val annotationProcessorWrapperFqNames = processorFqNames
                .map { fqName -> "__gen." + getProcessorStubClassName(fqName) }
                .joinToString(",")

        addWrappersToCompilerArgs(javaTask, annotationProcessorWrapperFqNames)
    }

    private fun JavaCompile.appendClasspath(file: File) {
        classpath += project.files(file)
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
            outputDir.getAbsolutePath()
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

        project.logger.kotlinDebug("kapt: Discovered annotation processors: ${annotationProcessors.joinToString()}")
        return annotationProcessors
    }

    private fun invokeCoreKaptMethod(methodName: String, vararg args: Any): Any {
        val array = arrayOfNulls<Class<*>>(args.size())
        args.forEachIndexed { i, arg -> array[i] = arg.javaClass }
        val method = getCoreKaptPackageClass().getMethod(methodName, *array)
        return method.invoke(null, *args)
    }

    private fun getCoreKaptPackageClass(): Class<*> {
        return Class.forName("org.jetbrains.kotlin.gradle.tasks.kapt.KaptPackage", false, coreClassLoader)
    }

}