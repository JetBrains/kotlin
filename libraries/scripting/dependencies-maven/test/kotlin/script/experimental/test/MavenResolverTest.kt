/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import kotlin.contracts.ExperimentalContracts
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.valueOrThrow

@ExperimentalContracts
class MavenResolverTest : ResolversTestBase() {

    fun testResolve() {
        val resolver = MavenDependenciesResolver()
        val result = runBlocking { resolver.resolve("org.jetbrains.kotlin:kotlin-annotations-jvm:1.3.50") }
        Assert.assertTrue(result is ResultWithDiagnostics.Success)
        val files = result.valueOrThrow()
        files.forEach { it.delete() }
    }
}
