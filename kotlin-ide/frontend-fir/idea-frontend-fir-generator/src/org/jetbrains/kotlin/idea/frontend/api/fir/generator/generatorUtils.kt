/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.generator

import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import kotlin.reflect.KClass
import kotlin.reflect.KType


internal fun SmartPrinter.printTypeWithShortNames(type: KType) {
    print((type.classifier as KClass<*>).simpleName!!)
    if (type.arguments.isNotEmpty()) {
        print("<")
        type.arguments.map {
            when (val typeArgument = it.type) {
                null -> "*"
                else -> printTypeWithShortNames(typeArgument)
            }
        }
        print(">")
    }
}