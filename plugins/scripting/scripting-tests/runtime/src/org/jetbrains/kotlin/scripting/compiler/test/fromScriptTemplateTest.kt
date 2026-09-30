/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.net.URISyntaxException
import java.net.URL
import java.net.URLClassLoader
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.dependencies.*
import kotlin.script.templates.AcceptedAnnotations
import kotlin.script.templates.ScriptTemplateDefinition

@Suppress("DEPRECATION")
open class TestKotlinScriptDummyDependenciesResolver : DependenciesResolver {

    @AcceptedAnnotations(DependsOn::class, DependsOnTwo::class)
    override fun resolve(
        scriptContents: ScriptContents,
        environment: Environment,
    ): DependenciesResolver.ResolveResult {
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


@Suppress("DEPRECATION")
open class TestKotlinScriptDependenciesResolver : TestKotlinScriptDummyDependenciesResolver() {

    private val kotlinPaths by lazy { PathUtil.kotlinPathsForCompiler }

    @AcceptedAnnotations(DependsOn::class, DependsOnTwo::class)
    override fun resolve(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult {
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

@Suppress("DEPRECATION")
class ErrorReportingResolver : TestKotlinScriptDependenciesResolver() {
    override fun resolve(
        scriptContents: ScriptContents,
        environment: Environment,
    ): DependenciesResolver.ResolveResult {
        return DependenciesResolver.ResolveResult.Failure(
            listOf(
                ScriptReport("error", ScriptReport.Severity.ERROR, null),
                ScriptReport("warning", ScriptReport.Severity.WARNING, ScriptReport.Position(1, 0)),
                ScriptReport("info", ScriptReport.Severity.INFO, ScriptReport.Position(2, 0)),
                ScriptReport("debug", ScriptReport.Severity.DEBUG, ScriptReport.Position(3, 0))
            )
        )
    }
}

@Suppress("DEPRECATION")
class TestAsyncResolver : TestKotlinScriptDependenciesResolver(), AsyncDependenciesResolver {
    override suspend fun resolveAsync(
        scriptContents: ScriptContents,
        environment: Environment,
    ): DependenciesResolver.ResolveResult = super<TestKotlinScriptDependenciesResolver>.resolve(scriptContents, environment)

    override fun resolve(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult =
        super<AsyncDependenciesResolver>.resolve(scriptContents, environment)
}

@Target(AnnotationTarget.FILE)
annotation class TestAnno1

@Target(AnnotationTarget.FILE)
annotation class TestAnno2

@Target(AnnotationTarget.FILE)
annotation class TestAnno3

private val annotationFqNames = listOf(TestAnno1::class, TestAnno2::class, TestAnno3::class).map { it.qualifiedName!! }

@Suppress("DEPRECATION")
interface AcceptedAnnotationsCheck {
    fun checkHasAnno1Annotation(scriptContents: ScriptContents): DependenciesResolver.ResolveResult.Success {
        val actualAnnotations = scriptContents.annotations
        assert(actualAnnotations.singleOrNull()?.annotationClass?.qualifiedName == TestAnno1::class.qualifiedName) {
            "Loaded annotation: $actualAnnotations"
        }

        return ScriptDependencies(
            classpath = classpathFromClassloader(),
            imports = annotationFqNames
        ).asSuccess()
    }
}

@Suppress("DEPRECATION")
class TestAcceptedAnnotationsSyncResolver : DependenciesResolver, AcceptedAnnotationsCheck {
    @AcceptedAnnotations(TestAnno1::class, TestAnno3::class)
    override fun resolve(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult {
        return checkHasAnno1Annotation(scriptContents)
    }
}

@Suppress("DEPRECATION")
class TestAcceptedAnnotationsAsyncResolver : AsyncDependenciesResolver, AcceptedAnnotationsCheck {
    @AcceptedAnnotations(TestAnno1::class, TestAnno3::class)
    override suspend fun resolveAsync(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult {
        return checkHasAnno1Annotation(scriptContents)
    }
}

class SeveralConstructorsResolver(val c: Int) : TestKotlinScriptDependenciesResolver() {
    constructor() : this(0)

}

class DefaultArgsConstructorResolver(val c: Int = 0) : TestKotlinScriptDependenciesResolver()

@Suppress("DEPRECATION")
class ThrowingResolver : DependenciesResolver {
    override fun resolve(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult {
        throw IllegalStateException("Exception from resolver")
    }

    override fun toString(): String {
        return "ThrowingResolver()"
    }
}

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.kts",
    resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptWithIntParam(val num: Int)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.kts",
    resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptWithClassParam(val param: TestParamClass)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.kts",
    resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptWithBaseClass(val num: Int, passthrough: Int) : TestDSLClassWithParam(passthrough)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.kts",
    resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptWithoutParams(@Suppress("UNUSED_PARAMETER") num: Int)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.kts",
    resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptBaseClassWithOverriddenProperty(override val num: Int) : TestClassWithOverridableProperty(num)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(
    scriptFilePattern = ".*\\.custom\\.kts",
    resolver = TestKotlinScriptDependenciesResolver::class
)
abstract class ScriptWithDifferentFileNamePattern

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithArrayParam(val myArgs: Array<String>)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithNullableParam(val param: Int?)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptVarianceParams(val param1: Array<in Number>, val param2: Array<out Number>)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithNullableProjection(val param: Array<String?>)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(resolver = TestKotlinScriptDependenciesResolver::class)
abstract class ScriptWithArray2DParam(val param: Array<Array<in String>>)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(resolver = ErrorReportingResolver::class)
abstract class ScriptReportingErrors(val num: Int)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(resolver = TestAsyncResolver::class)
abstract class ScriptWithAsyncResolver(val num: Int)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(resolver = TestAcceptedAnnotationsSyncResolver::class)
abstract class ScriptWithAcceptedAnnotationsSyncResolver

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(resolver = TestAcceptedAnnotationsAsyncResolver::class)
abstract class ScriptWithAcceptedAnnotationsAsyncResolver

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(resolver = SeveralConstructorsResolver::class)
abstract class ScriptWithSeveralConstructorsResolver(val num: Int)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(resolver = DefaultArgsConstructorResolver::class)
abstract class ScriptWithDefaultArgsResolver(val num: Int)

@Suppress("DEPRECATION")
@ScriptTemplateDefinition(resolver = ThrowingResolver::class)
abstract class ScriptWithThrowingResolver(val num: Int)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class DependsOn(val path: String)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class DependsOnTwo(val unused: String = "", val path1: String = "", val path2: String = "")

internal fun URL.toFile() =
    try {
        File(toURI().schemeSpecificPart)
    } catch (_: URISyntaxException) {
        if (protocol != "file") null
        else File(file)
    }
