/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.valueOrThrow

@ExperimentalContracts
class MavenResolverTest : ResolversTestBase() {

    fun resolveAndCheck(coordinates: String, checkBody: (Iterable<File>) -> Boolean = { true } ) {
        val resolver = MavenDependenciesResolver()
        val result = runBlocking { resolver.resolve(coordinates) }
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

    fun testResolveVersionsRange() {
        resolveAndCheck("org.jetbrains.kotlin:kotlin-annotations-jvm:(1.3.40,1.3.60)")
    }

    fun testResolveDifferentType() {
        resolveAndCheck("org.javamoney:moneta:pom:1.3") { files ->
            files.any { it.extension == "pom" }
        }
    }
}
