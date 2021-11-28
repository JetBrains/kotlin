/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectories
import java.io.File
import kotlin.test.assertFails

abstract class AbstractIrCompareJvmAbiTest : AbstractCompareJvmAbiTest() {
    override val useIrBackend = true
}
