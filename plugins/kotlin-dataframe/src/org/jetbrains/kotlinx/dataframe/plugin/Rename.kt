package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments

public class Rename : AbstractInterpreter<RenameClauseApproximation>() {
    private val Arguments.receiver by dataFrame()
    private val Arguments.columns by columnsSelector()
    override fun Arguments.interpret(): RenameClauseApproximation {
        return RenameClauseApproximation(receiver, columns)
    }
}
