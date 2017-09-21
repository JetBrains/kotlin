/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
//import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
//import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
//import com.intellij.lang.Language
//import com.intellij.openapi.util.Disposer
//import com.intellij.psi.PsiFileFactory
//import com.intellij.psi.PsiJavaFile
//import com.intellij.psi.impl.PsiFileFactoryImpl
//import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.kapt.generateAnnotationProcessorWrapper
import org.jetbrains.kotlin.gradle.tasks.kapt.generateKotlinAptAnnotation
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

internal fun Project.initKapt(
        kotlinTask: KotlinCompile,
        javaTask: JavaCompile,
        kaptManager: AnnotationProcessingManager,
        variantName: String,
        kotlinOptions: KotlinJvmOptionsImpl?,
        subpluginEnvironment: SubpluginEnvironment,
        tasksProvider: KotlinTasksProvider,
        androidProjectHandler: AbstractAndroidProjectHandler<*>?
): KotlinCompile? {
    val kaptExtension = extensions.getByType(KaptExtension::class.java)
    val kotlinAfterJavaTask: KotlinCompile?

    fun warnUnsupportedKapt1Option(optionName: String) {
        kotlinTask.logger.kotlinWarn("'$optionName' option is not supported by this kapt implementation. " +
                "Please add the \"apply plugin: 'kotlin-kapt\" line to your build script to enable it.")
    }

    if (kaptExtension.processors.isNotEmpty()) warnUnsupportedKapt1Option("processors")
    if (kaptExtension.correctErrorTypes) warnUnsupportedKapt1Option("correctErrorTypes")

    if (kaptExtension.generateStubs) {
        kotlinAfterJavaTask = createKotlinAfterJavaTask(javaTask, kotlinTask, kotlinOptions, tasksProvider)

        kotlinTask.logger.kotlinDebug("kapt: Using class file stubs")

        val stubsDir = File(buildDir, "tmp/kapt/$variantName/classFileStubs").apply { mkdirs() }
        kotlinAfterJavaTask.destinationDir = kotlinTask.destinationDir
        kotlinTask.destinationDir = stubsDir
        kotlinTask.kaptOptions.generateStubs = true

        kotlinAfterJavaTask.source(kaptManager.generatedKotlinSourceDir)
        kotlinAfterJavaTask.source(kaptManager.aptOutputDir)
        subpluginEnvironment.addSubpluginOptions(this, kotlinAfterJavaTask, javaTask, null, androidProjectHandler, null)

//        javaTask.doLast {
//            moveGeneratedJavaFilesToCorrespondingDirectories(kaptManager.aptOutputDir)
//        }
    } else {
        kotlinAfterJavaTask = null
        kotlinTask.logger.kotlinDebug("kapt: Class file stubs are not used")
    }

    javaTask.appendClasspathDynamically(kaptManager.wrappersDirectory)
    javaTask.source(kaptManager.hackAnnotationDir)

    kotlinTask.kaptOptions.supportInheritedAnnotations = kaptExtension.inheritedAnnotations

    kotlinTask.doFirst {
        kaptManager.generateJavaHackFile()
    }

    javaTask.doFirst {
        kaptManager.setupKapt()
        kaptManager.generateJavaHackFile()
        kotlinAfterJavaTask?.source(kaptManager.generatedKotlinSourceDir)
    }

    javaTask.doLast {
        kaptManager.afterJavaCompile()
    }

    kotlinTask.kaptOptions.annotationsFile = kaptManager.getAnnotationFile()
    return kotlinAfterJavaTask
}

private fun Project.createKotlinAfterJavaTask(
        javaTask: AbstractCompile,
        kotlinTask: KotlinCompile,
        kotlinOptions: KotlinJvmOptionsImpl?,
        tasksProvider: KotlinTasksProvider
): KotlinCompile {
    val kotlinAfterJavaTask = with (tasksProvider.createKotlinJVMTask(this, kotlinTask.name + KOTLIN_AFTER_JAVA_TASK_SUFFIX, kotlinTask.sourceSetName)) {
        mapClasspath { kotlinTask.classpath }
        this
    }

    kotlinAfterJavaTask.dependsOn(javaTask)
    javaTask.finalizedByIfNotFailed(kotlinAfterJavaTask)
    kotlinAfterJavaTask.parentKotlinOptionsImpl = kotlinOptions
    return kotlinAfterJavaTask
}

class AnnotationProcessingManager(
        task: AbstractCompile,
        private val javaTask: JavaCompile,
        private val taskQualifier: String,
        private val aptFiles: Set<File>,
        val aptOutputDir: File,
        private val aptWorkingDir: File,
        private val androidVariant: Any? = null) {

    private val project = task.project
    val wrappersDirectory = File(aptWorkingDir, "wrappers")
    val hackAnnotationDir = File(aptWorkingDir, "java_src")

    private var originalJavaCompilerArgs: List<String>? = null
    private var originalProcessorPath: FileCollection? = null

    private companion object {
        val JAVA_FQNAME_PATTERN = "^([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*$".toRegex()
        val GEN_ANNOTATION = "__gen/annotation"

        private val ANDROID_APT_PLUGIN_ID = "com.neenbedankt.android-apt"
    }

    fun getAnnotationFile(): File {
        if (!aptWorkingDir.exists()) aptWorkingDir.mkdirs()
        return File(wrappersDirectory, "annotations.$taskQualifier.txt")
    }

    val generatedKotlinSourceDir: File
        get() {
            val kotlinGeneratedDir = File(aptWorkingDir, "kotlinGenerated")
            if (!kotlinGeneratedDir.exists()) kotlinGeneratedDir.mkdirs()
            return kotlinGeneratedDir
        }

    val kaptProcessorPath get() = setOf(wrappersDirectory) + aptFiles + javaTask.classpath

    fun setupKapt() {
        originalJavaCompilerArgs = javaTask.options.compilerArgs

        if (aptFiles.isEmpty()) return

        project.logger.warn("${project.name}: " +
                "Original kapt is deprecated. Please add \"apply plugin: 'kotlin-kapt'\" to your build.gradle.")

        if (project.plugins.findPlugin(ANDROID_APT_PLUGIN_ID) != null) {
            project.logger.warn("Please do not use `$ANDROID_APT_PLUGIN_ID` with kapt.")
        }

        val annotationProcessorFqNames = lookupAnnotationProcessors(aptFiles)

        generateAnnotationProcessorStubs(javaTask, annotationProcessorFqNames, wrappersDirectory)

        setProcessorPathInJavaTask()

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
        javaTask.options.compilerArgs = originalJavaCompilerArgs
        tryRevertProcessorPathProperty()
    }

    fun generateJavaHackFile() {
        val javaHackPackageDir = File(hackAnnotationDir, GEN_ANNOTATION)
        val javaHackClFile = File(javaHackPackageDir, "Cl.java")

        if (!javaHackClFile.exists()) {
            javaHackClFile.parentFile.mkdirs()
            javaHackClFile.writeText("package __gen.annotation;\n" +
                                     "class Cl { @__gen.KotlinAptAnnotation boolean v; }")
        }
    }

    private fun appendAnnotationsArguments() {
        javaTask.modifyCompilerArguments { list ->
            list.add("-Akapt.annotations=" + getAnnotationFile())
            list.add("-Akapt.kotlin.generated=" + generatedKotlinSourceDir)
        }
    }

    private fun appendAdditionalComplerArgs() {
        val kaptExtension = project.extensions.getByType(KaptExtension::class.java)
        val args = kaptExtension.getAdditionalArgumentsForJavac(project, androidVariant, getAndroidExtension())
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

    private fun setProcessorPathInJavaTask() {
        val path = kaptProcessorPath

        // If processor path property is supported, set it, otherwise set compiler argument:
        val couldSetProperty = tryAppendProcessorPathProperty(path)

        if (!couldSetProperty) {
            javaTask.addCompilerArgument("-processorpath") { prevValue ->
                if (prevValue != null)
                    javaTask.logger.warn("Processor path was modified by kapt. Previous value = $prevValue")
                path.joinToString(postfix = prevValue?.let { File.pathSeparator + it }.orEmpty(),
                                  separator = File.pathSeparator)
            }
        }
    }

    private fun tryAppendProcessorPathProperty(path: Iterable<File>): Boolean = try {
        val optionsClass = javaTask.options.javaClass
        val getPath = optionsClass.getMethod("getAnnotationProcessorPath")
        val setPath = optionsClass.getMethod("setAnnotationProcessorPath", FileCollection::class.java)

        originalProcessorPath = getPath(javaTask.options) as? FileCollection

        if (originalProcessorPath != null)
            javaTask.logger.warn("Processor path was modified by kapt. Previous value = $originalProcessorPath")

        val newPath = javaTask.project.files(path + (originalProcessorPath ?: emptyList()))
        setPath(javaTask.options, newPath)
        true
    } catch (_: NoSuchMethodException) {
        false
    }

    private fun tryRevertProcessorPathProperty() {
        try {
            val optionsClass = javaTask.options.javaClass
            val setPath = optionsClass.getMethod("setAnnotationProcessorPath", FileCollection::class.java)

            setPath(javaTask.options, originalProcessorPath)
        } catch (_: NoSuchMethodException) {
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
        val newCompilerArgs = compilerArgs.mapTo(arrayListOf<String>(), Any::toString)
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
                    zipFile.getInputStream(entry).reader().useLines(::processLines)
                }
            }
        }

        project.logger.kotlinDebug("kapt: Discovered annotation processors: ${annotationProcessors.joinToString()}")
        return annotationProcessors
    }
}

// Java files can be generated anywhere, but
// Kotlin searches for java fq-name only in directory corresponding to package.
// Previously this worked because generated files were added to classpath.
// However in that case incremental compilation worked unreliable with generated files.
// The solution is to post-process generated java files and move them to corresponding packages
//fun moveGeneratedJavaFilesToCorrespondingDirectories(generatedJavaSourceRoot: File) {
//    val javaFiles = generatedJavaSourceRoot.walk().filter { it.extension.equals("java", ignoreCase = true) }.toList()
//
//    if (javaFiles.isEmpty()) return
//
//    val rootDisposable = Disposer.newDisposable()
//    val configuration = CompilerConfiguration()
//    val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
//    val project = environment.project
//    val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
//
//    for (javaFile in javaFiles) {
//        val psiFile = psiFileFactory.createFileFromText(javaFile.nameWithoutExtension, Language.findLanguageByID("JAVA")!!, javaFile.readText())
//        val packageName = (psiFile as? PsiJavaFile)?.packageName ?: continue
//        val expectedDir = File(generatedJavaSourceRoot, packageName.replace('.', '/'))
//        val expectedFile = File(expectedDir, javaFile.name)
//
//        if (javaFile != expectedFile) {
//            expectedFile.parentFile.mkdirs()
//            javaFile.copyTo(expectedFile, overwrite = true)
//            javaFile.delete()
//        }
//    }
//}