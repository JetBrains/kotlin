/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.formver.viper.ast.Label

class CatchBlockData(val entryLabel: Label, val firCatch: FirCatch)
class CatchBlockListData(val exitLabel: Label, val blocks: List<CatchBlockData>)