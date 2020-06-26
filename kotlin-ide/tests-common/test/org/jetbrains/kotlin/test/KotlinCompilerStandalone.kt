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

package org.jetbrains.kotlin.test

import com.intellij.util.lang.UrlClassLoader
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.idea.artifacts.TestKotlinArtifacts
import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.js.JsPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.junit.Assert.assertEquals
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.ref.SoftReference
import java.net.URL
import java.net.URLClassLoader
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.reflect.KClass

class KotlinCompilerStandalone @JvmOverloads constructor(
    private val sources: List<File>,
    private val target: File = defaultTargetJar(),
    private val platform: SimplePlatform = jvmPlatform,
    private val options: List<String> = emptyList(),
    classpath: List<File> = emptyList(),
    includeKotlinStdlib: Boolean = true
) {
    companion object {
        @JvmStatic
        val jsPlatform: SimplePlatform
            get() = JsPlatforms.defaultJsPlatform.componentPlatforms.single()

        @JvmStatic
        val jvmPlatform: SimplePlatform
            get() = JdkPlatform(JvmTarget.DEFAULT)

        @JvmStatic
        fun defaultTargetJar(): File {
            return File.createTempFile("kt-lib", ".jar").also { it.deleteOnExit() }.canonicalFile
        }
    }

    private val classpath: List<File>

    init {
        assert(platform is JdkPlatform || platform is JsPlatform) { "Only JVM and JS targets are supported" }

        if (platform is JdkPlatform && includeKotlinStdlib) {
            this.classpath = classpath + listOf(TestKotlinArtifacts.kotlinStdlib, TestKotlinArtifacts.jetbrainsAnnotations)
        } else {
            this.classpath = classpath
        }
    }

    fun compile(): File {
        val ktFiles = mutableListOf<File>()
        val javaFiles = mutableListOf<File>()

        for (source in sources) {
            for (file in source.walk()) {
                if (file.isFile) {
                    when (file.extension) {
                        "java" -> javaFiles += file
                        "kt", "kts" -> ktFiles += file
                    }
                }
            }
        }

        assert(ktFiles.isNotEmpty() || javaFiles.isNotEmpty()) { "Sources not found" }
        assert(platform is JdkPlatform || javaFiles.isEmpty()) { "Java source compilation is only available in JVM target" }

        val compilerTargets = mutableListOf<File>()

        if (ktFiles.isNotEmpty()) {
            val targetForKotlin = KotlinTestUtils.tmpDirForReusableFolder("compile-kt")
            compileKotlin(ktFiles, javaFiles.isNotEmpty(), targetForKotlin)
            compilerTargets += targetForKotlin
        }

        if (javaFiles.isNotEmpty() && platform is JdkPlatform) {
            val targetForJava = KotlinTestUtils.tmpDirForReusableFolder("java-lib")
            compileJava(javaFiles, compilerTargets, targetForJava, useJava9 = platform.targetVersion >= JvmTarget.JVM_9)
            compilerTargets += targetForJava
        }

        val copyFun = if (target.extension.toLowerCase() == "jar") ::copyToJar else ::copyToDirectory
        copyFun(compilerTargets, target)

        return target
    }

    private fun compileKotlin(files: List<File>, hasJavaFiles: Boolean, target: File) {
        val args = mutableListOf<String>()

        args += files.map { it.absolutePath }
        if (classpath.isNotEmpty()) {
            args += "-classpath"
            args += classpath.joinToString(File.pathSeparator) { it.absolutePath }
        }
        args += options

        if (files.none { it.extension.toLowerCase() == "kts" }) {
            args += "-Xdisable-default-scripting-plugin"
        }

        val kotlincFun = when (platform) {
            is JdkPlatform -> {
                args += listOf("-no-stdlib", "-d", target.absolutePath)
                if (hasJavaFiles) {
                    args += "-Xjava-source-roots=" + sources.joinToString(File.pathSeparator) { it.absolutePath }
                }
                KotlinCliCompilerFacade::runJvmCompiler
            }
            is JsPlatform -> {
                args += listOf("-meta-info", "-output", target.absolutePath)
                KotlinCliCompilerFacade::runJsCompiler
            }
            else -> error("Unexpected platform $platform")
        }
        kotlincFun(args)
    }

    private fun compileJava(files: List<File>, existingCompilerTargets: List<File>, target: File, useJava9: Boolean) {
        val classpath = this.classpath + existingCompilerTargets

        val args = mutableListOf("-d", target.absolutePath)
        if (classpath.isNotEmpty()) {
            args += "-classpath"
            args += classpath.joinToString(File.pathSeparator) { it.absolutePath }
        }

        val compileFun = if (useJava9) KotlinTestUtils::compileJavaFilesExternallyWithJava9 else KotlinTestUtils::compileJavaFiles
        assert(compileFun(files, args)) { "Java files are not compiled successfully" }
    }

    private fun copyToJar(sources: List<File>, target: File) {
        target.outputStream().buffered().use { os ->
            ZipOutputStream(os).use { zos ->
                for (source in sources) {
                    for (file in source.walk()) {
                        if (file.isFile) {
                            val path = file.toRelativeString(source)
                            zos.putNextEntry(ZipEntry(path))
                            zos.write(file.readBytes())
                            zos.closeEntry()
                        }
                    }
                }
            }
        }
    }

    private fun copyToDirectory(sources: List<File>, target: File) {
        target.mkdirs()
        assert(target.isDirectory) { "Can't create target directory" }
        assert(target.listFiles().orEmpty().isEmpty()) { "Target directory is not empty" }
        sources.forEach { it.copyRecursively(target) }
    }
}

object KotlinCliCompilerFacade {
    private var classLoader = SoftReference<ClassLoader>(null)

    private val jvmCompilerClass: Class<*>
        @Synchronized get() = loadCompilerClass(K2JVMCompiler::class)

    private val jsCompilerClass: Class<*>
        @Synchronized get() = loadCompilerClass(K2JSCompiler::class)

    fun runJvmCompiler(args: List<String>) {
        runCompiler(jvmCompilerClass, args)
    }

    fun runJsCompiler(args: List<String>) {
        runCompiler(jsCompilerClass, args)
    }

    // Runs compiler in custom class loader to avoid effects caused by replacing Application with another one created in compiler.
    private fun runCompiler(compilerClass: Class<*>, args: List<String>) {
        val outStream = ByteArrayOutputStream()
        val compiler = compilerClass.newInstance()
        val execMethod = compilerClass.getMethod("exec", PrintStream::class.java, Array<String>::class.java)
        val invocationResult = execMethod.invoke(compiler, PrintStream(outStream), args.toTypedArray()) as Enum<*>
        assertEquals(String(outStream.toByteArray()), ExitCode.OK.name, invocationResult.name)
    }

    @Synchronized
    private fun loadCompilerClass(compilerClass: KClass<out CLICompiler<*>>): Class<*> {
        val classLoader = classLoader.get() ?: createCompilerClassLoader().also { classLoader ->
            this.classLoader = SoftReference(classLoader)
        }
        return classLoader.loadClass(compilerClass.java.name)
    }

    @Synchronized
    private fun createCompilerClassLoader(): ClassLoader {
        val currentLoader = K2JVMCompiler::class.java.classLoader

        val urls = when (currentLoader) {
            is URLClassLoader -> currentLoader.urLs
            is UrlClassLoader -> currentLoader.urls.toTypedArray()
            else -> {
                if (currentLoader.javaClass.name ==  UrlClassLoader::class.java.name) {
                    // It seems UrlClassLoader is loaded from some other class loader on CI, and the type check doesn't work properly
                    @Suppress("UNCHECKED_CAST")
                    (currentLoader.javaClass.getMethod("getUrls").invoke(currentLoader) as List<URL>).toTypedArray()
                } else {
                    error("Unexpected class loader type $currentLoader")
                }
            }
        }

        return URLClassLoader(urls, currentLoader.parent)
    }
}