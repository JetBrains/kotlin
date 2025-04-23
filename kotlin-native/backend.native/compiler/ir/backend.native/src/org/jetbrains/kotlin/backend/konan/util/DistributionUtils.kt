/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.util

import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal val Distribution.compilerFingerprint: String
    get() = File(konanHome).resolve("konan/compiler.fingerprint").readText().trim()

internal fun Distribution.runtimeFingerprint(target: KonanTarget): String = File(konanHome).resolve("konan/targets/$target/runtime.fingerprint").readText().trim()