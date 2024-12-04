/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.render

internal fun IrProperty.atomicfuRender(): String =
    (if (isVar) "var" else "val") + " " + name.asString() + ": " + backingField?.type?.render()