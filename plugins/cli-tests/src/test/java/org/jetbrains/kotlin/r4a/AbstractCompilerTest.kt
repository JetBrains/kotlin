package org.jetbrains.kotlin.r4a

import junit.framework.TestCase
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtPlatformTestUtil
import org.jetbrains.kotlin.codegen.CodegenTestFiles
import org.jetbrains.kotlin.script.ScriptDependenciesProvider
import org.jetbrains.kotlin.utils.rethrow
import org.jetbrains.kotlin.TestsCompilerError
import org.jetbrains.kotlin.TestsCompiletimeError
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.junit.After
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
        System.setProperty("user.dir", homeDir)
        val environment = createEnvironment()
        setupEnvironment(environment)
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

    @After
    fun after() {
        tearDown()
    }

    protected val defaultClassPath by lazy {
        listOf(
            assertExists(kotlinRuntimeJar("kotlin-stdlib")),
            assertExists(
                File(
                    projectRoot,
                    "out/support/compose-runtime/build/" +
                            "intermediates/intermediate-jars/debug/classes.jar"
                ).normalize()
            ),
            assertExists(
                File(
                    projectRoot,
                    "out/support/ui-android-view-non-ir/build/" +
                            "intermediates/intermediate-jars/debug/classes.jar"
                ).normalize()
            ),
            assertExists(
                File(projectRoot, "prebuilts/fullsdk-linux/platforms/android-28/android.jar")
            )
        )
    }

    protected fun createClasspath() = defaultClassPath

    val myTestRootDisposable = TestDisposable()

    protected fun createEnvironment(): KotlinCoreEnvironment {
        val classPath = createClasspath()

        val configuration = KotlinTestUtils.newConfiguration()
        configuration.addJvmClasspathRoots(classPath)

        return KotlinCoreEnvironment.createForTests(
            myTestRootDisposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }

    protected fun setupEnvironment(environment: KotlinCoreEnvironment) {
        R4AComponentRegistrar().registerProjectComponents(
            environment.project as MockProject,
            environment.configuration
        )
    }

    protected fun createClassLoader(): GeneratedClassLoader {
        val uris = arrayOf(kotlinRuntimeJar("kotlin-stdlib").toURI().toURL())
        val classLoader = URLClassLoader(uris, null)
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

        val environment = myEnvironment ?: error("Environment not initialized")
        val externalImportsProvider = ScriptDependenciesProvider.getInstance(environment.project)
        if (externalImportsProvider != null) {
            environment.getSourceFiles().forEach { file ->
                externalImportsProvider.getScriptDependencies(file)?.let {
                    files.addAll(it.classpath)
                }
            }
        }

        try {
            return files.map { it.toURI().toURL() }.toTypedArray()
        } catch (e: MalformedURLException) {
            throw rethrow(e)
        }
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
            } catch (e: TestsCompiletimeError) {
                if (reportProblems) {
                    e.original.printStackTrace()
                    System.err.println("Generating instructions as text...")
                    try {
                        System.err.println(classFileFactory?.createText()
                            ?: "Cannot generate text: exception was thrown during generation")
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
                } else {
                    System.err.println("Compilation failure")
                }
                throw e
            } catch (e: Throwable) {
                throw TestsCompilerError(e)
            }
        }
    }

    protected fun getTestName(lowercaseFirstLetter: Boolean): String =
        getTestName(this.name, lowercaseFirstLetter)
    protected fun getTestName(name: String?, lowercaseFirstLetter: Boolean): String =
        name ?: KtPlatformTestUtil.getTestName(name!!, lowercaseFirstLetter)

    inner class TestDisposable : Disposable {

        override fun dispose() {}

        override fun toString(): String {
            val testName = this@AbstractCompilerTest.getTestName(false)
            return this@AbstractCompilerTest.javaClass.name +
                    if (StringUtil.isEmpty(testName)) "" else ".test$testName"
        }
    }

    companion object {
        val homeDir by lazy { File(KotlinTestUtils.getHomeDirectory()).absolutePath }
        val projectRoot by lazy { File(homeDir, "../../../../..").absolutePath }

        fun kotlinRuntimeJar(module: String) = File(
                projectRoot,
                "prebuilts/androidx/external/org/jetbrains/kotlin/$module/" +
                        "$KOTLIN_RUNTIME_VERSION/$module-$KOTLIN_RUNTIME_VERSION.jar")

        init {
            System.setProperty("idea.home", homeDir)
        }
    }
}
