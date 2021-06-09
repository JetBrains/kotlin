/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile

internal fun UFile.findFacade(): UClass? {
    return classes.find { it.sourcePsi is KtLightClassForFacade }
}
