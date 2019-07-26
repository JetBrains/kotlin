/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import org.jetbrains.kotlin.idea.fir.FirResolution

abstract class AbstractFirReferenceResolveTest : AbstractReferenceResolveTest() {
    override fun setUp() {
        super.setUp()
        FirResolution.enabled = true
    }

    override fun tearDown() {
        FirResolution.enabled = false
        super.tearDown()
    }
}