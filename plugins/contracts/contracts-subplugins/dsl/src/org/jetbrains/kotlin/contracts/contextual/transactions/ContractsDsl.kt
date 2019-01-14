/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.transactions

import org.jetbrains.kotlin.contracts.contextual.ClosesContextDescription
import org.jetbrains.kotlin.contracts.contextual.RequiresContextDescription
import org.jetbrains.kotlin.contracts.contextual.StartsContextDescription
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
class OpenedTransaction(thisReference: Any) : StartsContextDescription, ClosesContextDescription, RequiresContextDescription