/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("unused")

package org.jetbrains.kotlin.scripting.compiler.test

import com.intellij.openapi.util.Disposer
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.TestMessageCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.assertHasMessage
import org.jetbrains.kotlin.scripting.compiler.plugin.expectTestToFailOnK2
import org.jetbrains.kotlin.scripting.compiler.plugin.updateWithBaseCompilerArguments
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.InvalidScriptResolverAnnotation
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.tryConstructClassFromStringArgs
import org.junit.Assert
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.Future
import kotlin.reflect.KClass
import kotlin.script.dependencies.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.dependencies.DependenciesResolver.ResolveResult
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.templates.AcceptedAnnotations
import kotlin.script.templates.ScriptTemplateDefinition
import kotlin.script.templates.standard.ScriptTemplateWithArgs

// TODO: the contetnts of this file should go into ScriptTest.kt and replace appropriate xml-based functionality,
// as soon as the the latter is removed from the codebase

class ScriptTemplateTest : TestCase() {
    fun testScriptWithParam() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("fib.kts", ScriptWithIntParam::class, null, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    fun testScriptWithClassParameter() {
        val messageCollector = TestMessageCollector()
        val aClass =
            compileScript("fib_cp.kts", ScriptWithClassParam::class, null, runIsolated = false, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        val out = captureOut {
            aClass!!.getConstructor(TestParamClass::class.java).newInstance(TestParamClass(4))
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    fun testScriptWithBaseClassWithParam() {
        val messageCollector = TestMessageCollector()
        val aClass =
            compileScript("fib_dsl.kts", ScriptWithBaseClass::class, null, runIsolated = false, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE, Integer.TYPE).newInstance(4, 1)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    fun testScriptWithDependsAnn() {
        Assert.assertNull(compileScript("fib_ext_ann.kts", ScriptWithIntParamAndDummyResolver::class, null, includeKotlinRuntime = false))

        val messageCollector = TestMessageCollector()
        val aClass = compileScript("fib_ext_ann.kts", ScriptWithIntParam::class, null, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    fun testScriptWithDependsAnn2() {
        val savedErr = System.err
        try {
            System.setErr(PrintStream(NullOutputStream()))
            Assert.assertNull(
                compileScript(
                    "fib_ext_ann2.kts",
                    ScriptWithIntParamAndDummyResolver::class,
                    null,
                    includeKotlinRuntime = false
                )
            )
        } finally {
            System.setErr(savedErr)
        }

        val messageCollector = TestMessageCollector()
        val aClass = compileScript("fib_ext_ann2.kts", ScriptWithIntParam::class, null, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    // Should fail on K2, see KT-60452
    fun testScriptWithoutParams() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("without_params.kts", ScriptWithoutParams::class, null, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed("10", out)
    }

    fun testScriptWithOverriddenParam() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript(
            "overridden_parameter.kts",
            ScriptBaseClassWithOverriddenProperty::class,
            null,
            messageCollector = messageCollector
        )
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed("14", out)
    }

    fun testScriptWithArrayParam() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("array_parameter.kts", ScriptWithArrayParam::class, null, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        captureOut {
            aClass!!.getConstructor(Array<String>::class.java).newInstance(arrayOf("one", "two"))
        }.let {
            assertEqualsTrimmed("one and two", it)
        }
    }

    fun testScriptWithNullableParam() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("nullable_parameter.kts", ScriptWithNullableParam::class, null, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        captureOut {
            aClass!!.getConstructor(Int::class.javaObjectType).newInstance(null)
        }.let {
            assertEqualsTrimmed("Param is null", it)
        }
    }

    fun testScriptVarianceParams() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("variance_parameters.kts", ScriptVarianceParams::class, null, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        captureOut {
            aClass!!.getConstructor(Array<in Number>::class.java, Array<out Number>::class.java).newInstance(arrayOf("one"), arrayOf(1, 2))
        }.let {
            assertEqualsTrimmed("one and 1", it)
        }
    }

    fun testScriptWithNullableProjection() {
        val messageCollector = TestMessageCollector()
        val aClass =
            compileScript("nullable_projection.kts", ScriptWithNullableProjection::class, null, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        captureOut {
            aClass!!.getConstructor(Array<String>::class.java).newInstance(arrayOf<String?>(null))
        }.let {
            assertEqualsTrimmed("nullable", it)
        }
    }

    fun testScriptWithArray2DParam() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("array2d_param.kts", ScriptWithArray2DParam::class, null, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        captureOut {
            aClass!!.getConstructor(Array<Array<in String>>::class.java).newInstance(arrayOf(arrayOf("one"), arrayOf("two")))
        }.let {
            assertEqualsTrimmed("first: one, size: 1", it)
        }
    }

    fun testScriptWithStandardTemplate() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("fib_std.kts", ScriptTemplateWithArgs::class, runIsolated = false, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        captureOut {
            aClass!!.getConstructor(Array<String>::class.java).newInstance(arrayOf("4", "other"))
        }.let {
            assertEqualsTrimmed("$NUM_4_LINE (other)$FIB_SCRIPT_OUTPUT_TAIL", it)
        }
    }

    fun testScriptWithPackage() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("fib.pkg.kts", ScriptWithIntParam::class, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }.let {
            assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, it)
        }
    }

    fun testScriptWithScriptDefinition() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("fib.kts", ScriptWithIntParam::class, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }.let {
            assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, it)
        }
    }

    fun testScriptWithParamConversion() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("fib.kts", ScriptWithIntParam::class, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        captureOut {
            val anObj = tryConstructClassFromStringArgs(aClass!!, listOf("4"))
            Assert.assertNotNull(anObj)
        }.let {
            assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, it)
        }
    }

    fun testScriptErrorReporting() {
        val messageCollector = TestMessageCollector()
        compileScript("fib.kts", ScriptReportingErrors::class, messageCollector = messageCollector)

        messageCollector.assertHasMessage("error", desiredSeverity = CompilerMessageSeverity.ERROR)
        messageCollector.assertHasMessage("warning", desiredSeverity = CompilerMessageSeverity.WARNING)
        messageCollector.assertHasMessage("info", desiredSeverity = CompilerMessageSeverity.INFO)
        messageCollector.assertHasMessage("debug", desiredSeverity = CompilerMessageSeverity.LOGGING)
    }

    fun testAsyncResolver() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("fib.kts", ScriptWithAsyncResolver::class, null, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        val out = captureOut {
            aClass!!.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    fun testAcceptedAnnotationsSync() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript(
            "acceptedAnnotations.kts",
            ScriptWithAcceptedAnnotationsSyncResolver::class,
            null,
            messageCollector = messageCollector
        )
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
    }

    fun testAcceptedAnnotationsAsync() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript(
            "acceptedAnnotations.kts",
            ScriptWithAcceptedAnnotationsAsyncResolver::class,
            null,
            messageCollector = messageCollector
        )
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
    }

    fun testAcceptedAnnotationsLegacy() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript(
            "acceptedAnnotations.kts",
            ScriptWithAcceptedAnnotationsLegacyResolver::class,
            null,
            messageCollector = messageCollector
        )
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
    }

    fun testSeveralConstructors() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("fib.kts", ScriptWithSeveralConstructorsResolver::class, null, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
    }

    fun testConstructorWithDefaultArgs() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("fib.kts", ScriptWithDefaultArgsResolver::class, null, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
    }

    fun testThrowing() {
        val messageCollector = TestMessageCollector()
        compileScript("fib.kts", ScriptWithThrowingResolver::class, null, messageCollector = messageCollector)

        messageCollector.assertHasMessage("Exception from resolver", desiredSeverity = CompilerMessageSeverity.ERROR)
    }

    fun testSmokeScriptException() {
        val messageCollector = TestMessageCollector()
        val aClass = compileScript("smoke_exception.kts", ScriptWithArrayParam::class, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
        var exceptionThrown = false
        try {
            tryConstructClassFromStringArgs(aClass!!, emptyList())
        } catch (e: InvocationTargetException) {
            Assert.assertTrue(e.cause is IllegalStateException)
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }

    fun testScriptWithNoMatchingTemplate() {
        val messageCollector = TestMessageCollector()
        val aClass =
            compileScript("without_params.kts", ScriptWithDifferentFileNamePattern::class, null, messageCollector = messageCollector)
        Assert.assertNotNull("Compilation failed:\n$messageCollector", aClass)
    }

    private fun compileScript(
        scriptPath: String,
        scriptTemplate: KClass<out Any>,
        environment: Map<String, Any?>? = null,
        runIsolated: Boolean = true,
        messageCollector: MessageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false),
        includeKotlinRuntime: Boolean = true
    ): Class<*>? =
        compileScriptImpl(
            "plugins/scripting/scripting-compiler/testData/compiler/$scriptPath",
            KotlinScriptDefinitionFromAnnotatedTemplate(
                scriptTemplate, environment
            ), runIsolated, messageCollector, includeKotlinRuntime
        )

    private fun compileScriptImpl(
        scriptPath: String,
        scriptDefinition: KotlinScriptDefinition,
        runIsolated: Boolean,
        messageCollector: MessageCollector,
        includeKotlinRuntime: Boolean
    ): Class<*>? {
        val rootDisposable = Disposer.newDisposable()
        try {
            val additionalClasspath = System.getProperty("kotlin.test.script.classpath")?.split(File.pathSeparator)
                ?.mapNotNull { File(it).takeIf { file -> file.exists() } }.orEmpty().toTypedArray()
            val configuration = KotlinTestUtils.newConfiguration(
                if (includeKotlinRuntime) ConfigurationKind.ALL else ConfigurationKind.JDK_ONLY,
                TestJdkKind.FULL_JDK,
                *additionalClasspath
            )
            configuration.updateWithBaseCompilerArguments()
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            configuration.add(
                ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
                ScriptDefinition.FromLegacy(
                    defaultJvmScriptingHostConfiguration,
                    scriptDefinition
                )
            )
            configuration.put(JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION, true)
            configuration.put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

            loadScriptingPlugin(configuration)

            val environment = KotlinCoreEnvironment.createForTests(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            return try {
                val res = compileScript(
                    File(scriptPath).toScriptSource(),
                    environment,
                    this::class.java.classLoader.takeUnless { runIsolated }
                )
                res.first?.java
            } catch (e: CompilationException) {
                messageCollector.report(
                    CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e),
                    MessageUtil.psiElementToMessageLocation(e.element)
                )
                null
            } catch (e: IllegalStateException) {
                messageCollector.report(CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e))
                null
            } catch (t: Throwable) {
                MessageCollectorUtil.reportException(messageCollector, t)
                throw t
            }
        } finally {
            Disposer.dispose(rootDisposable)
        }
    }
}

open class TestKotlinScriptDummyDependenciesResolver : DependenciesResolver {

    @AcceptedAnnotations(DependsOn::class, DependsOnTwo::class)
    override fun resolve(
        scriptContents: ScriptContents,
        environment: Environment
    ): ResolveResult {
        return ScriptDependencies(
            classpath = classpathFromClassloader(),
            imports = listOf(
                "org.jetbrains.kotlin.scripting.compiler.test.DependsOn",
                "org.jetbrains.kotlin.scripting.compiler.test.DependsOnTwo"
            )
        ).asSuccess()
    }
}

private fun classpathFromClassloader(): List<File> =
    (TestKotlinScriptDependenciesResolver::class.java.classLoader as? URLClassLoader)?.urLs
        ?.mapNotNull(URL::toFile)
        ?.filter { it.path.contains("out") && it.path.contains("test") }
        ?: emptyList()


open class TestKotlinScriptDependenciesResolver : TestKotlinScriptDummyDependenciesResolver() {

    private val kotlinPaths by lazy { PathUtil.kotlinPathsForCompiler }

    @AcceptedAnnotations(DependsOn::class, DependsOnTwo::class)
    override fun resolve(scriptContents: ScriptContents, environment: Environment): ResolveResult {
        val cp = scriptContents.annotations.flatMap { annotation ->
            when (annotation) {
                is DependsOn ->
                    if (annotation.path == "@{kotlin-stdlib}") listOf(kotlinPaths.stdlibPath, kotlinPaths.scriptRuntimePath)
                    else listOf(File(annotation.path))
                is DependsOnTwo -> listOf(annotation.path1, annotation.path2).flatMap {
                    when {
                        it.isBlank() -> emptyList()
                        it == "@{kotlin-stdlib}" -> listOf(kotlinPaths.stdlibPath, kotlinPaths.scriptRuntimePath)
                        else -> listOf(File(it))
                    }
                }
                is InvalidScriptResolverAnnotation -> throw Exception("Invalid annotation ${annotation.name}", annotation.error)
                else -> throw Exception("Unknown annotation ${annotation::class.java}")
            }
        }
        return ScriptDependencies(
            classpath = classpathFromClassloader() + cp,
            imports = listOf(
                "org.jetbrains.kotlin.scripting.compiler.test.DependsOn",
                "org.jetbrains.kotlin.scripting.compiler.test.DependsOnTwo"
            )
        ).asSuccess()
    }
}

class TestParamClass(@Suppress("unused") val memberNum: Int)

class ErrorReportingResolver : TestKotlinScriptDependenciesResolver() {
    override fun resolve(
        scriptContents: ScriptContents,
        environment: Environment
    ): ResolveResult {
        return ResolveResult.Success(
            super.resolve(scriptContents, environment).dependencies!!,
            listOf(
                ScriptReport("error", ScriptReport.Severity.ERROR, null),
                ScriptReport("warning", ScriptReport.Severity.WARNING, ScriptReport.Position(1, 0)),
                ScriptReport("info", ScriptReport.Severity.INFO, ScriptReport.Position(2, 0)),
                ScriptReport("debug", ScriptReport.Severity.DEBUG, ScriptReport.Position(3, 0))

            )
        )
    }
}

class TestAsyncResolver : TestKotlinScriptDependenciesResolver(), AsyncDependenciesResolver {
    override suspend fun resolveAsync(
        scriptContents: ScriptContents,
        environment: Environment
    ): ResolveResult = super<TestKotlinScriptDependenciesResolver>.resolve(scriptContents, environment)

    override fun resolve(scriptContents: ScriptContents, environment: Environment): ResolveResult =
        super<AsyncDependenciesResolver>.resolve(scriptContents, environment)
}

@Target(AnnotationTarget.FILE)
annotation class TestAnno1

@Target(AnnotationTarget.FILE)
annotation class TestAnno2

@Target(AnnotationTarget.FILE)
annotation class TestAnno3

private val annotationFqNames = listOf(TestAnno1::class, TestAnno2::class, TestAnno3::class).map { it.qualifiedName!! }

interface AcceptedAnnotationsCheck {
    fun checkHasAnno1Annotation(scriptContents: ScriptContents): ResolveResult.Success {
        val actualAnnotations = scriptContents.annotations
        Assert.assertTrue(
            "Loaded annotation: $actualAnnotations",
            actualAnnotations.singleOrNull()?.annotationClass?.qualifiedName == TestAnno1::class.qualifiedName
        )

        return ScriptDependencies(
            classpath = classpathFromClassloader(),
            imports = annotationFqNames
        ).asSuccess()
    }
}

class TestAcceptedAnnotationsSyncResolver : DependenciesResolver, AcceptedAnnotationsCheck {
    @AcceptedAnnotations(TestAnno1::class, TestAnno3::class)
    override fun resolve(scriptContents: ScriptContents, environment: Environment): ResolveResult {
        return checkHasAnno1Annotation(scriptContents)
    }
}

class TestAcceptedAnnotationsAsyncResolver : AsyncDependenciesResolver, AcceptedAnnotationsCheck {
    @AcceptedAnnotations(TestAnno1::class, TestAnno3::class)
    override suspend fun resolveAsync(scriptContents: ScriptContents, environment: Environment): ResolveResult {
        return checkHasAnno1Annotation(scriptContents)
    }
}

class TestAcceptedAnnotationsLegacyResolver : ScriptDependenciesResolver, AcceptedAnnotationsCheck {
    @AcceptedAnnotations(TestAnno1::class, TestAnno3::class)
    override fun resolve(
        script: ScriptContents,
        environment: Environment?,
        report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit,
        previousDependencies: KotlinScriptExternalDependencies?
    ): Future<KotlinScriptExternalDependencies?> {
        checkHasAnno1Annotation(script)
        return object : KotlinScriptExternalDependencies {
            override val classpath: Iterable<File>
                get() = classpathFromClassloader()

            override val imports: Iterable<String>
                get() = annotationFqNames
        }.asFuture()
    }
}

class SeveralConstructorsResolver(val c: Int) : TestKotlinScriptDependenciesResolver() {
    constructor() : this(0)

}

class DefaultArgsConstructorResolver(val c: Int = 0) : TestKotlinScriptDependenciesResolver()

class ThrowingResolver : DependenciesResolver {
    override fun resolve(scriptContents: ScriptContents, environment: Environment): ResolveResult {
        throw IllegalStateException("Exception from resolver")
    }
}

@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.kts",
    resolver = TestKotlinScriptDummyDependenciesResolver::class
)
abstract class ScriptWithIntParamAndDummyResolver(val num: Int)

@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.kts",
    resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptWithIntParam(val num: Int)

@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.kts",
    resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptWithClassParam(val param: TestParamClass)

@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.kts",
    resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptWithBaseClass(val num: Int, passthrough: Int) : TestDSLClassWithParam(passthrough)

@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.kts",
    resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptWithoutParams(@Suppress("UNUSED_PARAMETER") num: Int)

@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.kts",
    resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptBaseClassWithOverriddenProperty(override val num: Int) : TestClassWithOverridableProperty(num)

@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.custom\\.kts",
    resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptWithDifferentFileNamePattern

@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithArrayParam(val myArgs: Array<String>)

@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithNullableParam(val param: Int?)

@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptVarianceParams(val param1: Array<in Number>, val param2: Array<out Number>)

@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithNullableProjection(val param: Array<String?>)

@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithArray2DParam(val param: Array<Array<in String>>)

@ScriptTemplateDefinition(resolver = ErrorReportingResolver::class)
abstract class ScriptReportingErrors(val num: Int)

@ScriptTemplateDefinition(resolver = TestAsyncResolver::class)
abstract class ScriptWithAsyncResolver(val num: Int)

@ScriptTemplateDefinition(resolver = TestAcceptedAnnotationsSyncResolver::class)
abstract class ScriptWithAcceptedAnnotationsSyncResolver

@ScriptTemplateDefinition(resolver = TestAcceptedAnnotationsAsyncResolver::class)
abstract class ScriptWithAcceptedAnnotationsAsyncResolver

@ScriptTemplateDefinition(resolver = TestAcceptedAnnotationsLegacyResolver::class)
abstract class ScriptWithAcceptedAnnotationsLegacyResolver

@ScriptTemplateDefinition(resolver = SeveralConstructorsResolver::class)
abstract class ScriptWithSeveralConstructorsResolver(val num: Int)

@ScriptTemplateDefinition(resolver = DefaultArgsConstructorResolver::class)
abstract class ScriptWithDefaultArgsResolver(val num: Int)

@ScriptTemplateDefinition(resolver = ThrowingResolver::class)
abstract class ScriptWithThrowingResolver(val num: Int)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class DependsOn(val path: String)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class DependsOnTwo(val unused: String = "", val path1: String = "", val path2: String = "")

private class NullOutputStream : OutputStream() {
    override fun write(b: Int) {}
    override fun write(b: ByteArray) {}
    override fun write(b: ByteArray, off: Int, len: Int) {}
}

internal fun URL.toFile() =
    try {
        File(toURI().schemeSpecificPart)
    } catch (e: java.net.URISyntaxException) {
        if (protocol != "file") null
        else File(file)
    }