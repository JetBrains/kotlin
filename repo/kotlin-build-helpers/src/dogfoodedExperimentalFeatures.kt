/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Features that must be enabled in the whole repo, including stdlib which deliberately overrides
 * settings from the `common-configuration`.
 *
 * Arguments here must be synchronized with `DogfoodedExperimentalFeatures` in `StdlibTest.kt`.
 */
val dogfoodedExperimentalFeatures = listOf(
    // Enabled by default in 2.4, but we have modules compiled with low LVs
    // (and bootstrap compiler), which still use this feature, so we cannot
    // just remove this argument.
    "-Xexplicit-backing-fields",

    "-Xname-based-destructuring=complete",
    "-Xcollection-literals",
    "-Xcontext-sensitive-resolution",
    "-Xexplicit-context-arguments",
)

/**
 * Between making a language feature stable and the next bootstrap, we
 * need to keep providing the compiler argument. But this produces a
 * warning "The argument ... is redundant for the current language
 * version ..." in the bootstrap test and fails because of -Werror.
 * To work around it, we suppress the warning when needed.
 */
const val redundantCliArgWarningSuppression = "-Xwarning-level=REDUNDANT_CLI_ARG:disabled"
