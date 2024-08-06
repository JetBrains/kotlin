/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package app

import app.AppClass
import org.junit.Assert
import org.junit.Test

class AppClassTest {

    @Test
    fun testApp() {
        val appClass = AppClass()
        Assert.assertEquals(appClass.testVal, "text")
    }
}