/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.exceptions

import org.jetbrains.kotlin.contracts.contextual.CallsBlockInContextDescription
import org.jetbrains.kotlin.contracts.contextual.RequiresContextDescription
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@SinceKotlin("1.3")
class CatchesException<T : Throwable> : CallsBlockInContextDescription, RequiresContextDescription