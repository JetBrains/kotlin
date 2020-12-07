/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Ignore
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.reflect.full.primaryConstructor
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.dependencies.impl.DependenciesResolverOptionsName
import kotlin.script.experimental.dependencies.impl.makeExternalDependenciesResolverOptions
import kotlin.script.experimental.dependencies.impl.set

@ExperimentalContracts
class MavenResolverTest : ResolversTestBase() {

    fun resolveAndCheck(
        coordinates: String,
        options: ExternalDependenciesResolver.Options = ExternalDependenciesResolver.Options.Empty,
        checkBody: (Iterable<File>) -> Boolean = { true }
    ) {
        val resolver = MavenDependenciesResolver()
        val result = runBlocking { resolver.resolve(coordinates, options) }
        if (result is ResultWithDiagnostics.Failure) {
            Assert.fail(result.reports.joinToString("\n") { it.exception?.toString() ?: it.message })
        }
        val files = result.valueOrThrow()
        if (!checkBody(files)) {
            Assert.fail("Unexpected resolving results:\n  ${files.joinToString("\n  ")}")
        }
        files.forEach { it.delete() }
    }

    fun testResolveSimple() {
        resolveAndCheck("org.jetbrains.kotlin:kotlin-annotations-jvm:1.3.50") { files ->
            files.any { it.name.startsWith("kotlin-annotations-jvm") }
        }
    }

    @ExperimentalStdlibApi
    fun testResolveWithRuntime() {
        val compileOnly = "compile"
        val compileRuntime = "compile,runtime"
        val resolvedFilesCount = mutableMapOf<String, Int>()

        listOf(compileOnly, compileRuntime).forEach { scopes ->
            resolveAndCheck(
                "org.uberfire:uberfire-io:7.39.0.Final",
                makeExternalDependenciesResolverOptions(buildMap {
                    this[DependenciesResolverOptionsName.SCOPE] = scopes
                })
            ) { files ->
                resolvedFilesCount[scopes] = files.count()
                files.any { it.name.startsWith("uberfire-commons") }
            }
        }

        val compileOnlyCount = resolvedFilesCount[compileOnly]!!
        val compileRuntimeCount = resolvedFilesCount[compileRuntime]!!
        assertTrue(
            "Compile only ($compileOnlyCount) dependencies count should be less than compile/runtime ($compileRuntimeCount) one",
            compileOnlyCount < compileRuntimeCount
        )
    }

    fun testResolveVersionsRange() {
        resolveAndCheck("org.jetbrains.kotlin:kotlin-annotations-jvm:(1.3.40,1.3.60)")
    }

    fun testResolveDifferentType() {
        resolveAndCheck("org.javamoney:moneta:pom:1.3") { files ->
            files.any { it.extension == "pom" }
        }
    }

    // Ignored - tests with custom repos often break the CI due to the caching issues
    // TODO: find a way to enable iut back
    @Ignore
    fun ignore_testResolveFromAnnotationsWillResolveTheSameRegardlessOfAnnotationOrder() {
        val dependsOnConstructor = DependsOn::class.primaryConstructor!!
        val repositoryConstructor = Repository::class.primaryConstructor!!

        // @DepensOn("eu.jrie.jetbrains:kotlin-shell-core:0.2")
        val dependsOn = dependsOnConstructor.callBy(
            mapOf(
                dependsOnConstructor.parameters.first() to arrayOf("eu.jrie.jetbrains:kotlin-shell-core:0.2")
            )
        )

        // @Repository( "https://dl.bintray.com/jakubriegel/kotlin-shell")
        val repositories = repositoryConstructor.callBy(
            mapOf(
                repositoryConstructor.parameters.first() to arrayOf(
                    "https://dl.bintray.com/jakubriegel/kotlin-shell"
                )
            )
        )

        val annotationsWithReposFirst = listOf(repositories, dependsOn)
        val annotationsWithDependsOnFirst = listOf(dependsOn, repositories)

        val filesWithReposFirst = runBlocking {
            MavenDependenciesResolver().resolveFromAnnotations(annotationsWithReposFirst)
        }.valueOrThrow()

        val filesWithDependsOnFirst = runBlocking {
            MavenDependenciesResolver().resolveFromAnnotations(annotationsWithDependsOnFirst)
        }.valueOrThrow()

        // Tests that the jar was resolved
        assert(
            filesWithReposFirst.any { it.name.startsWith("kotlin-shell-core-") && it.extension == "jar" }
        )
        assert(
            filesWithDependsOnFirst.any { it.name.startsWith("kotlin-shell-core-") && it.extension == "jar" }
        )

        // Test that the the same files are resolved regardless of annotation order
        assertEquals(filesWithReposFirst.map { it.name }.sorted(), filesWithDependsOnFirst.map { it.name }.sorted())
    }
}
