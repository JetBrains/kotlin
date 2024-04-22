/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.domains.FunctionBuilder
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.viper.ast.*


object SpecialFunctions {
    val duplicableFunction = FunctionBuilder.build("duplicable") {
        argument { Type.Ref }
        returns { Type.Bool }
    }
    val all = listOf(duplicableFunction) + RuntimeTypeDomain.accompanyingFunctions
}
