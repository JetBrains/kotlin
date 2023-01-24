/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan


/*
 * Sometimes psi2ir produces IR with non-trivial variance in super types of SAM conversions (this is a language design issue).
 * Earlier this was solved with just erasing all such variances but this might lead to some other hard to debug problems,
 * so after handling the majority of corner cases correctly in psi2ir it is safe to assume that such cases don't get here and
 * even if they do, then it's better to throw an error right away than to dig out weird crashes down the pipeline or even at runtime.
 * We explicitly check this, also fixing older klibs built with previous compiler versions by applying the same trick as before.
 */
//internal val checkSamSuperTypesPhase = konanUnitPhase(
//        op = {
//            // Handling types in current module not recursively:
//            // psi2ir can produce SAM conversions with variances in type arguments of type arguments.
//            // See https://youtrack.jetbrains.com/issue/KT-49384.
//            // So don't go deeper than top-level arguments to avoid the compiler emitting false-positive errors.
//            // Lowerings can handle this.
//            // Also such variances are allowed in the language for manual implementations of interfaces.
//            irModule!!.files
//                    .forEach { SamSuperTypesChecker(this, it, mode = SamSuperTypesChecker.Mode.THROW, recurse = false).run() }
//            // TODO: This is temporary for handling klibs produced with earlier compiler versions.
//            // Handling types in dependencies recursively, just to be extra safe: don't change something that works.
//            irModules.values
//                    .flatMap { it.files }
//                    .forEach { SamSuperTypesChecker(this, it, mode = SamSuperTypesChecker.Mode.ERASE, recurse = true).run() }
//        },
//        name = "CheckSamSuperTypes",
//        description = "Check SAM conversions super types"
//)

// The original comment around [checkSamSuperTypesPhase] still holds, but in order to be on par with JVM_IR
// (which doesn't report error for these corner cases), we turn off the checker for now (the problem with variances
// is workarounded in [FunctionReferenceLowering] by taking erasure of SAM conversion type).
// Also see https://youtrack.jetbrains.com/issue/KT-50399 for more details.
//        disable(checkSamSuperTypesPhase)