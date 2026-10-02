/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.printer

import org.jetbrains.kotlin.kir.KirModule
import org.jetbrains.kotlin.kir.printer.impl.KirAsKotlinSourcesPrinter

/**
 * Prints KIR as Kotlin sources.
 */
public class KirPrinter {
    public fun print(module: KirModule): String = KirAsKotlinSourcesPrinter.print(module)
}
