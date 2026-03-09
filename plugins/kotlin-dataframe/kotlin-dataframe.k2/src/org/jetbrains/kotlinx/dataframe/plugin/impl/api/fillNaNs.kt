/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.ignore

class FillNaNs0 : AbstractInterpreter<FillNaNsApproximation>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns by ignore()

    override fun Arguments.interpret(): FillNaNsApproximation {
        return FillNaNsApproximation(receiver)
    }
}

data class FillNaNsApproximation(val schema: PluginDataFrameSchema) : UpdateApproximation {
    override fun withWhere(): UpdateApproximation = this
}