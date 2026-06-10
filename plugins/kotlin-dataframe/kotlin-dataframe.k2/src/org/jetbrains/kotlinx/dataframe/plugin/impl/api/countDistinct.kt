/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.*

class GroupByCountDistinct0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by groupBy()
    val Arguments.resultName: String by arg(defaultValue = Present("countDistinct"))
    val Arguments.columns by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.keys.add(resultName, session.builtinTypes.intType.coneType, context = this)
    }
}
