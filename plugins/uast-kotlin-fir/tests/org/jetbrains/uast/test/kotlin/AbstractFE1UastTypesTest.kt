/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import org.jetbrains.uast.UFile
import org.jetbrains.uast.test.common.kotlin.FirUastTypesTestBase
import org.jetbrains.uast.test.env.kotlin.AbstractFE1UastTest

abstract class AbstractFE1UastTypesTest : AbstractFE1UastTest(), FirUastTypesTestBase {
    override val isFirUastPlugin: Boolean = false

    override fun check(filePath: String, file: UFile) {
        super<FirUastTypesTestBase>.check(filePath, file)
    }
}
