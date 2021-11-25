/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.settings

// TODO: in fact, only WITH_MODULES mode is supported now
internal enum class TestMode(val description: String) {
    ONE_STAGE(
        description = "Compile test files altogether without producing intermediate KLIBs."
    ),
    TWO_STAGE(
        description = "Compile test files altogether and produce an intermediate KLIB. Then produce a program from the KLIB using -Xinclude."
    ),
    WITH_MODULES(
        description = "Compile each test file as one or many modules (depending on MODULE directives declared in the file)." +
                " Then link the KLIBs into the single executable file."
    )
}
