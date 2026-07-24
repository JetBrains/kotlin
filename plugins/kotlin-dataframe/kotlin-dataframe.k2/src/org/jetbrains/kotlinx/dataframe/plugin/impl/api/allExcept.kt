/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.asColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.*

// region ColumnSet except

/** `df.select { colsOf<Number>() except { "age" and height } }` */
internal class ColumnSetExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver { receiver except { selector } }
}

/** `df.select { cols(name, age) except ("age" and height) }` */
internal class ColumnSetExceptColumnsResolver : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.other: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver { receiver except other }
}

/** `df.select { colsOf<Number>().except(age, userData.height) }` */
internal class ColumnSetExceptColumnsResolvers : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.others: List<Interpreter.Success<ColumnsResolver>> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.except(*others.map { it.value }.toTypedArray())
        }
}

/** `df.select { colsOf<Number>() except "age" }` */
internal class ColumnSetExceptString : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.other: String by arg()

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver { receiver except other }
}

/** `df.select { colsOf<Number>().except("age", "height") }` */
internal class ColumnSetExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.others: List<String> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.except(*others.toTypedArray())
        }
}

/** `df.select { colsOf<Number>() except "userdata"["age"] }` */
internal class ColumnSetExceptColumnPath : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.other: ColumnPathApproximation by arg()

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver { receiver except other.path }
}

/** `df.select { colsOf<Number>().except(pathOf("age"), "userdata"["height"]) }` */
internal class ColumnSetExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.others: List<Interpreter.Success<ColumnPathApproximation>> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.except(*others.map { it.value.path }.toTypedArray())
        }
}

// endregion

// region ColumnsSelectionDsl allExcept

/** `df.select { allExcept { "age" and height } }` */
internal class CSDslAllExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver { allExcept { selector } }
}

/** `df.select { allExcept(age, height) }` */
internal class CSDslAllExceptColumnsResolvers : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.others: List<Interpreter.Success<ColumnsResolver>> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            allExcept(*others.map { it.value }.toTypedArray())
        }
}

/** `df.select { allExcept("age", "height") }` */
internal class CSDslAllExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.others: List<String> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            allExcept(*others.toTypedArray())
        }
}

/** `df.select { allExcept(pathOf("age"), "userdata"["height"]) }` */
internal class CSDslAllExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.others: List<Interpreter.Success<ColumnPathApproximation>> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            allExcept(*others.map { it.value.path }.toTypedArray())
        }
}

// endregion

// region SingleColumn allColsExcept

/** `df.select { userData.allColsExcept { "age" and height } }` */
internal class ColumnGroupAllColsExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.asColumnGroup().allColsExcept { selector }
        }
}

/** `df.select { userData.allColsExcept("age", "height") }` */
internal class ColumnGroupAllColsExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.others: List<String> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.asColumnGroup().allColsExcept(*others.toTypedArray())
        }
}

/** `df.select { userData.allColsExcept(pathOf("age"), "extraData"["item1"]) }` */
internal class ColumnGroupAllColsExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.others: List<Interpreter.Success<ColumnPathApproximation>> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.asColumnGroup().allColsExcept(*others.map { it.value.path }.toTypedArray())
        }
}

// endregion

// region String allColsExcept

/** `df.select { "userData".allColsExcept { "age" and height } }` */
internal class StringAllColsExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: String by arg()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver { receiver.allColsExcept { selector } }
}

/** `df.select { "userData".allColsExcept("age", "height") }` */
internal class StringAllColsExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: String by arg()
    val Arguments.others: List<String> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.allColsExcept(*others.toTypedArray())
        }
}

/** `df.select { "userData".allColsExcept(pathOf("age"), "extraData"["item1"]) }` */
internal class StringAllColsExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: String by arg()
    val Arguments.others: List<Interpreter.Success<ColumnPathApproximation>> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.allColsExcept(*others.map { it.value.path }.toTypedArray())
        }
}

// endregion

// region ColumnPath allColsExcept

/** `df.select { pathOf("userData").allColsExcept { "age" and height } }` */
internal class ColumnPathAllColsExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnPathApproximation by arg()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver { receiver.path.allColsExcept { selector } }
}

/** `df.select { pathOf("userData").allColsExcept("age", "height") }` */
internal class ColumnPathAllColsExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnPathApproximation by arg()
    val Arguments.others: List<String> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.path.allColsExcept(*others.toTypedArray())
        }
}

/** `df.select { pathOf("userData").allColsExcept(pathOf("age"), "extraData"["item1"]) }` */
internal class ColumnPathAllColsExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnPathApproximation by arg()
    val Arguments.others: List<Interpreter.Success<ColumnPathApproximation>> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.path.allColsExcept(*others.map { it.value.path }.toTypedArray())
        }
}

// endregion

// region SingleColumn except

/** `df.select { userData.except { "age" and height } }` */
internal class ColumnGroupExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.asColumnGroup().except { selector }
        }
}

/** `df.select { userData.except("age", "height") }` */
internal class ColumnGroupExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.others: List<String> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.asColumnGroup().except(*others.toTypedArray())
        }
}

/** `df.select { userData.except(pathOf("age"), "extraData"["item1"]) }` */
internal class ColumnGroupExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.others: List<Interpreter.Success<ColumnPathApproximation>> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.asColumnGroup().except(*others.map { it.value.path }.toTypedArray())
        }
}

// endregion

// region String except

/** `df.select { "userData".except { "age" and height } }` */
internal class StringExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: String by arg()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver { receiver.except { selector } }
}

/** `df.select { "userData".except("age", "height") }` */
internal class StringExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: String by arg()
    val Arguments.others: List<String> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.except(*others.toTypedArray())
        }
}

/** `df.select { "userData".except(pathOf("age"), "extraData"["item1"]) }` */
internal class StringExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: String by arg()
    val Arguments.others: List<Interpreter.Success<ColumnPathApproximation>> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.except(*others.map { it.value.path }.toTypedArray())
        }
}

// endregion

// region ColumnPath except

/** `df.select { pathOf("userData").except { "age" and height } }` */
internal class ColumnPathExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnPathApproximation by arg()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver { receiver.path.except { selector } }
}

/** `df.select { pathOf("userData").except("age", "height") }` */
internal class ColumnPathExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnPathApproximation by arg()
    val Arguments.others: List<String> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.path.except(*others.toTypedArray())
        }
}

/** `df.select { pathOf("userData").except(pathOf("age"), "extraData"["item1"]) }` */
internal class ColumnPathExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnPathApproximation by arg()
    val Arguments.others: List<Interpreter.Success<ColumnPathApproximation>> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ColumnsResolver =
        columnsResolver {
            receiver.path.except(*others.map { it.value.path }.toTypedArray())
        }
}

// endregion
