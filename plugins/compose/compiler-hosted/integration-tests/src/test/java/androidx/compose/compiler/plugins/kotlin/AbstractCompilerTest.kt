/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.annotations.Contract
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.utils.rethrow
import org.junit.After
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader

private const val KOTLIN_RUNTIME_VERSION = "1.3.11"

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractCompilerTest : TestCase() {
    protected var myEnvironment: KotlinCoreEnvironment? = null
    protected var myFiles: CodegenTestFiles? = null
    protected var classFileFactory: ClassFileFactory? = null
    protected var javaClassesOutputDirectory: File? = null
    protected var additionalDependencies: List<File>? = null

    override fun setUp() {
        // Setup the environment for the analysis
        System.setProperty(
            "user.dir",
            homeDir
        )
        myEnvironment = createEnvironment()
        setupEnvironment(myEnvironment!!)
        super.setUp()
    }

    override fun tearDown() {
        myFiles = null
        myEnvironment = null
        javaClassesOutputDirectory = null
        additionalDependencies = null
        classFileFactory = null
        Disposer.dispose(myTestRootDisposable)
        super.tearDown()
    }

    fun ensureSetup(block: () -> Unit) {
        setUp()
        block()
    }

    @After
    fun after() {
        tearDown()
    }

    protected val defaultClassPath by lazy { systemClassLoaderJars() }

    protected fun createClasspath() = defaultClassPath.filter {
        !it.path.contains("robolectric") && it.extension != "xml"
    }.toList()

    val myTestRootDisposable = TestDisposable()

    protected fun createEnvironment(): KotlinCoreEnvironment {
        val classPath = createClasspath()

        val configuration = newConfiguration()
        configuration.addJvmClasspathRoots(classPath)

        System.setProperty("idea.ignore.disabled.plugins", "true")
        return KotlinCoreEnvironment.createForTests(
            myTestRootDisposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }

    protected open fun setupEnvironment(environment: KotlinCoreEnvironment) {
        ComposeComponentRegistrar.registerProjectExtensions(
            environment.project as MockProject,
            environment.configuration
        )
    }

    protected fun createClassLoader(): GeneratedClassLoader {
        val classLoader = URLClassLoader(
            defaultClassPath.map {
                it.toURI().toURL()
            }.toTypedArray(),
            null
        )
        return GeneratedClassLoader(
            generateClassesInFile(),
            classLoader,
            *getClassPathURLs()
        )
    }

    protected fun getClassPathURLs(): Array<URL> {
        val files = mutableListOf<File>()
        javaClassesOutputDirectory?.let { files.add(it) }
        additionalDependencies?.let { files.addAll(it) }

        try {
            return files.map { it.toURI().toURL() }.toTypedArray()
        } catch (e: MalformedURLException) {
            throw rethrow(e)
        }
    }

    private fun reportProblem(e: Throwable) {
        e.printStackTrace()
        System.err.println("Generating instructions as text...")
        try {
            System.err.println(
                classFileFactory?.createText()
                    ?: "Cannot generate text: exception was thrown during generation"
            )
        } catch (e1: Throwable) {
            System.err.println(
                "Exception thrown while trying to generate text, " +
                    "the actual exception follows:"
            )
            e1.printStackTrace()
            System.err.println(
                "------------------------------------------------------------------" +
                    "-----------"
            )
        }

        System.err.println("See exceptions above")
    }

    protected fun generateClassesInFile(reportProblems: Boolean = true): ClassFileFactory {
        return classFileFactory ?: run {
            try {
                val environment = myEnvironment ?: error("Environment not initialized")
                val files = myFiles ?: error("Files not initialized")
                val generationState = GenerationUtils.compileFiles(
                    files.psiFiles, environment, ClassBuilderFactories.TEST,
                    NoScopeRecordCliBindingTrace()
                )
                generationState.factory.also { classFileFactory = it }
            } catch (e: TestsCompilerError) {
                if (reportProblems) {
                    reportProblem(e.original)
                } else {
                    System.err.println("Compilation failure")
                }
                throw e
            } catch (e: Throwable) {
                if (reportProblems) reportProblem(e)
                throw TestsCompilerError(e)
            }
        }
    }

    protected fun getTestName(lowercaseFirstLetter: Boolean): String =
        getTestName(this.name ?: "", lowercaseFirstLetter)
    protected fun getTestName(name: String, lowercaseFirstLetter: Boolean): String {
        val trimmedName = trimStart(name, "test")
        return if (StringUtil.isEmpty(trimmedName)) "" else lowercaseFirstLetter(
            trimmedName,
            lowercaseFirstLetter
        )
    }

    protected fun lowercaseFirstLetter(name: String, lowercaseFirstLetter: Boolean): String =
        if (lowercaseFirstLetter && !isMostlyUppercase(name))
            Character.toLowerCase(name[0]) + name.substring(1)
        else name

    protected fun isMostlyUppercase(name: String): Boolean {
        var uppercaseChars = 0
        for (i in 0 until name.length) {
            if (Character.isLowerCase(name[i])) {
                return false
            }
            if (Character.isUpperCase(name[i])) {
                uppercaseChars++
                if (uppercaseChars >= 3) return true
            }
        }
        return false
    }

    inner class TestDisposable : Disposable {

        override fun dispose() {}

        override fun toString(): String {
            val testName = this@AbstractCompilerTest.getTestName(false)
            return this@AbstractCompilerTest.javaClass.name +
                if (StringUtil.isEmpty(testName)) "" else ".test$testName"
        }
    }

    companion object {

        private fun File.applyExistenceCheck(): File = this.apply {
            if (!exists()) throw NoSuchFileException(this)
        }

        val homeDir by lazy { File(computeHomeDirectory()).applyExistenceCheck().absolutePath }
        val projectRoot by lazy {
            File(homeDir, "../../../../../..").applyExistenceCheck().absolutePath
        }
        val kotlinHome by lazy {
            File(projectRoot, "prebuilts/androidx/external/org/jetbrains/kotlin/")
                .applyExistenceCheck()
        }
        val outDir by lazy {
            File(System.getenv("OUT_DIR") ?: File(projectRoot, "out").absolutePath)
                .applyExistenceCheck()
        }
        val composePluginJar by lazy {
            File(outDir, "androidx/compose/compiler/compiler/build/repackaged/embedded.jar")
                .applyExistenceCheck()
        }

        fun kotlinRuntimeJar(module: String) = File(
            kotlinHome, "$module/$KOTLIN_RUNTIME_VERSION/$module-$KOTLIN_RUNTIME_VERSION.jar"
        ).applyExistenceCheck()

        init {
            System.setProperty(
                "idea.home",
                homeDir
            )
        }
    }
}

private fun systemClassLoaderJars(): List<File> {
    val classpath = System.getProperty("java.class.path")!!.split(
        System.getProperty("path.separator")!!
    )
    val urls = classpath.map { URL("file://$it") }
    val result = URLClassLoader(urls.toTypedArray()).urLs?.filter {
        it.protocol == "file"
    }?.map {
        File(it.path)
    }?.toList() ?: emptyList()
    return result
}

private fun computeHomeDirectory(): String {
    val userDir = System.getProperty("user.dir")
    val dir = File(userDir ?: ".")
    return FileUtil.toCanonicalPath(dir.absolutePath)
}

private const val TEST_MODULE_NAME = "test-module"

fun newConfiguration(): CompilerConfiguration {
    val configuration = CompilerConfiguration()
    configuration.put(
        CommonConfigurationKeys.MODULE_NAME,
        TEST_MODULE_NAME
    )

    configuration.put(
        CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
        object : MessageCollector {
            override fun clear() {}

            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?
            ) {
                if (severity === CompilerMessageSeverity.ERROR) {
                    val prefix = if (location == null)
                        ""
                    else
                        "(" + location.path + ":" + location.line + ":" + location.column + ") "
                    throw AssertionError(prefix + message)
                }
            }

            override fun hasErrors(): Boolean {
                return false
            }
        }
    )

    return configuration
}

@Contract(pure = true)
fun trimStart(s: String, prefix: String): String {
    return if (s.startsWith(prefix)) {
        s.substring(prefix.length)
    } else s
}
