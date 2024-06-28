/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: packages.def
package package1.package2
---
#define OK_STRING "OK"

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import package1.package2.*

fun box(): String = OK_STRING
