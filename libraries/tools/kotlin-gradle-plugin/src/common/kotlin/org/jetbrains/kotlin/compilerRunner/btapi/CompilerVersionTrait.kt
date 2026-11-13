/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

// Compiler behavior that depends on the BTA compiler version. [since] inclusive, [before] exclusive.
internal enum class CompilerVersionTrait(
    private val since: KotlinReleaseVersion? = null,
    private val before: KotlinReleaseVersion? = null,
) {
    // Compiler puts level into message text: "w: file:///A.kt:1:5 ...", so KGP must not add another "w: ".
    // Newer compilers send "file:///A.kt:1:5 ..." and KGP adds the level; changed in 38a7bf0be95e (KT-73012).
    COMPILER_RENDERS_LEVEL_PREFIX(before = KotlinReleaseVersion(2, 2, 0)),

    // Compiler logs bad flags via logger.error("Invalid argument: -foo") and returns COMPILATION_ERROR, skipping renderer.
    // Older compilers throw CompilerArgumentsParseException instead; changed in f196595a7dbf (KT-85504).
    COMPILER_LOGS_ARGUMENT_ERRORS(since = KotlinReleaseVersion(2, 4, 20)),
    ;

    fun appliesTo(compilerVersion: KotlinReleaseVersion): Boolean =
        (since == null || compilerVersion >= since) && (before == null || compilerVersion < before)
}

internal fun KotlinToolchains.has(trait: CompilerVersionTrait): Boolean = trait.appliesTo(compilerReleaseVersion())

internal fun KotlinToolchains.compilerReleaseVersion(): KotlinReleaseVersion =
    KotlinToolingVersion(getCompilerVersion()).let { KotlinReleaseVersion(it.major, it.minor, it.patch) }
