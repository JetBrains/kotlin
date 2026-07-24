/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.junit.jupiter.api.Test

class PropertyCommonizationFromSourcesTest : AbstractCommonizationFromSourcesTest() {

    @Test
    fun testSpecifics() = doTestSuccessfulCommonization()

    @Test
    fun testSetters() = doTestSuccessfulCommonization()

    @Test
    fun testLiftingUpConst() = doTestSuccessfulCommonization()

    @Test
    fun testOverloadingByUpperBounds() = doTestSuccessfulCommonization()
}
