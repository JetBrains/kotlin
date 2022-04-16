/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.target.LinkerOutputKind

/**
 * For historical reasons, Android Native executables can be of two types:
 * - [Standalone] is a regular executable, in which the Konan entry point receives command line arguments as usual.
 * - [NativeActivity] is a shared library in which the Konan entry point matches Android NDK's NativeActivity
 *   signature ( https://developer.android.com/ndk/reference/group/native-activity#anativeactivity_createfunc ).
 */
enum class AndroidProgramType(
        val konanMainOverride: String?,
        val linkerOutputKindOverride: LinkerOutputKind?,
        val consolePrintsToLogcat: Boolean
) {

    /** Regular executable. The runtime entry point name is not Konan_main for historical reasons. */
    Standalone("Konan_main_standalone", null, false),

    /** Native activity "executable" - a shared library with a specific entry point. */
    NativeActivity(null, LinkerOutputKind.DYNAMIC_LIBRARY, true);

    companion object {
        val Default = Standalone
    }
}