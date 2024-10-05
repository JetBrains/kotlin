/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.UIApplication
import kotlin.test.*

@Test
fun ensureUIApplication() {
    // This test should be run with a test host application.
    // Hence, it should NOT be null
    assertNotNull(UIApplication.sharedApplication)
}