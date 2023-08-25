/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.resolve.maven.test

import org.jetbrains.kotlin.script.examples.jvm.resolve.maven.host.evalFile
import org.junit.Assert
import org.junit.Test
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics

class ResolveTest {

    @Test
    fun testResolveJunit() {
        val res = evalFile(File("testData/hello-maven-resolve-junit.scriptwithdeps.kts"))

        Assert.assertTrue(
            "test failed:\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}",
            res is ResultWithDiagnostics.Success
        )
    }

    @Test
    fun testUnresolvedJunit() {
        val res = evalFile(File("testData/hello-unresolved-junit.scriptwithdeps.kts"))

        Assert.assertTrue(
            "test failed - expecting a failure with the message \"Unresolved reference 'junit'\" but received " +
                    (if (res is ResultWithDiagnostics.Failure) "failure" else "success") +
                    ":\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}",
            res is ResultWithDiagnostics.Failure && res.reports.any { it.message.contains("Unresolved reference 'junit'.") })
    }

    @Test
    fun testResolveError() {
        val res = evalFile(File("testData/hello-maven-resolve-error.scriptwithdeps.kts"))

        Assert.assertTrue(
            "test failed - expecting a failure with the message \"Unknown set of arguments to maven resolver: abracadabra\" but received " +
                    (if (res is ResultWithDiagnostics.Failure) "failure" else "success") +
                    ":\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}",
            res is ResultWithDiagnostics.Failure && res.reports.any { it.message.contains("File 'abracadabra' not found") })
    }
}