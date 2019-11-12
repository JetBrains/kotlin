/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.script.experimental.GenericDependenciesResolver
import kotlin.script.experimental.acceptsRepository
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.valueOrThrow

@ExperimentalContracts
fun <T> assertIsFailure(r: ResultWithDiagnostics<T>) {
    contract {
        returns() implies (r is ResultWithDiagnostics.Failure)
    }

    Assert.assertTrue(r is ResultWithDiagnostics.Failure)
}

@ExperimentalContracts
fun <T> assertIsSuccess(r: ResultWithDiagnostics<T>) {
    contract {
        returns() implies (r is ResultWithDiagnostics.Success<T>)
    }

    TestCase.assertTrue(r is ResultWithDiagnostics.Success<T>)
}

@ExperimentalContracts
abstract class ResolversTestBase : TestCase() {
    fun GenericDependenciesResolver.assertNotResolve(expectedReportsCount: Int, path: String) {
        val result = runBlocking { resolve(path) }
        assertIsFailure(result)
        assertEquals(expectedReportsCount, result.reports.count())
    }

    fun GenericDependenciesResolver.assertAcceptsArtifact(path: String) = assertTrue(acceptsArtifact(path))

    fun GenericDependenciesResolver.assertNotAcceptsArtifact(path: String) = assertFalse(acceptsArtifact(path))

    fun GenericDependenciesResolver.assertAcceptsRepository(path: String) = assertTrue(acceptsRepository(path))

    fun GenericDependenciesResolver.assertResolve(expected: File, path: String) {

        assertTrue(acceptsArtifact(path))

        val result = runBlocking { resolve(path) }
        assertIsSuccess(result)

        val value = result.valueOrThrow()
        assertEquals(1, value.count())
        assertEquals(expected.canonicalPath, value.first().canonicalPath)
    }
}