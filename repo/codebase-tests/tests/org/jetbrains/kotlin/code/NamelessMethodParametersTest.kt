/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

/**
 * Scans everything this repository compiles for a `MethodParameters` entry that has no name, and fails on any.
 *
 * A modern `javac` writes such an entry — `name_index = 0`, legal per JVMS §4.7.24 — for parameters that carry
 * a flag worth recording but no name of their own: the mandated outer instance of an inner class constructor,
 * the parameters of a bridge method. It does so even under `--release 8`, and even though nothing in this build
 * asks for parameter names. `javac` 8 wrote nothing at all.
 *
 * Two tools this repository still depends on cannot read those entries:
 *
 * * the D8 that `dex-member-list` brings along for `dexMethodCount` fails with a bare `NullPointerException`;
 * * JDK 8 before 8u4xx turns the missing name into `""` rather than `null`, so `Executable.getParameters()`
 *   throws `MalformedParametersException`. JUnit calls it while resolving the constructors of `@Nested`
 *   classes, so every test task still running on JDK 8 breaks — and only on a JDK 8 old enough, which is why
 *   this reproduced on CI and not locally.
 *
 * `stripMethodParameters` removes the attribute from all `javac` output; this test is what keeps it wired up.
 * Both failures above were long investigations whose diagnostics pointed nowhere near the cause.
 *
 * Only *nameless* entries are rejected. A named one is well-formed and harmless — third-party jars we shade
 * carry plenty of both kinds, which is why this checks what we compile rather than what we ship.
 */
class NamelessMethodParametersTest {

    @Test
    fun `no compiled class has a nameless MethodParameters entry`() {
        val violations = runBlocking(Dispatchers.Default) {
            channelFlow {
                forEachCompiledClass { file, node ->
                    val methods = node.methods.orEmpty()
                        .filter { method -> method.parameters.orEmpty().any { it.name == null } }
                        .map { it.name }
                    if (methods.isNotEmpty()) {
                        send("$file: ${methods.joinToString()}")
                    }
                }
            }.toList().sorted()
        }

        if (violations.isNotEmpty()) {
            fail(
                "${violations.size} compiled class(es) carry a `MethodParameters` entry without a name. That breaks " +
                        "`dexMethodCount` and `getParameters()` on older JDK 8 builds — see the KDoc of this test. " +
                        "`stripMethodParameters` should have removed it from every `JavaCompile` output, so either the " +
                        "producing task is not a `JavaCompile`, or the stripping is no longer wired up in " +
                        "`configureJavaCompile`.\n\nThis test walks every `build/classes` directory in the worktree, " +
                        "so output left behind by an earlier revision trips it too: if the paths below are stale, " +
                        "rebuild or delete them.\n\n" +
                        violations.joinToString("\n").prependIndent("  ")
            )
        }
    }
}
