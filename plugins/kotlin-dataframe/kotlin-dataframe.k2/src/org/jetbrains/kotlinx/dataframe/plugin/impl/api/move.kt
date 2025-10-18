package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.plugin.impl.*

/** Implementation of `move {}` operation. Returns `MoveClause`.*/
class Move0 : AbstractInterpreter<MoveClauseApproximation>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): MoveClauseApproximation {
        return MoveClauseApproximation(receiver, columns)
    }
}

/** Implementation of `move {}.toTop()` operation. */
class ToTop : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.toTop().toPluginDataFrameSchema()
    }
}

/** Implementation of `move {}.under("colGroup")` operation. */
class MoveUnder0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()
    val Arguments.column: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.under(column).toPluginDataFrameSchema()
    }
}

/** Implementation of `move {}.under { colGroup }` operation. */
class MoveUnder1 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()
    val Arguments.column: SingleColumnApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.under { column.col.path }.toPluginDataFrameSchema()
    }
}

/** Implementation of `move {}.into("newTopLevelCol")` operation. */
class MoveInto0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()
    val Arguments.column: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.into(column).toPluginDataFrameSchema()
    }
}

/** Implementation of `move {}.toStart()` operation. */
class MoveToStart0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.toStart().toPluginDataFrameSchema()
    }
}

/** Implementation of `moveToStart {}` operation. */
class MoveToStart1 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = columns.resolve(receiver).map { it.path }
        return receiver.asDataFrame().moveToStart { columns.toColumnSet() }.toPluginDataFrameSchema()
    }
}

/** Implementation of `move {}.toEnd()` operation. */
class MoveToEnd0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.toEnd().toPluginDataFrameSchema()
    }
}

/** Implementation of `moveToEnd {}` operation. */
class MoveToEnd1 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = columns.resolve(receiver).map { it.path }
        return receiver.asDataFrame().moveToEnd { columns.toColumnSet() }.toPluginDataFrameSchema()
    }
}

/** Implementation of `move {}.before { col }` operation. */
class MoveBefore0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()
    val Arguments.column: SingleColumnApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.before { column.col.path }.toPluginDataFrameSchema()
    }
}

/** Implementation of `move {}.after { col }` operation. */
class MoveAfter0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()
    val Arguments.column: SingleColumnApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.after { column.col.path }.toPluginDataFrameSchema()
    }
}

/** Implementation of `move {}.to(int)` operation. */
class MoveTo : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()
    val Arguments.columnIndex: Int by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.asDataFrame().move { receiver.columns }.to(columnIndex).toPluginDataFrameSchema()
    }
}

/** Implementation of `moveTo(int) {}` operation. */
class MoveTo1 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.newColumnIndex: Int by arg()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = columns.resolve(receiver).map { it.path }
        return receiver.asDataFrame().moveTo(newColumnIndex) { columns.toColumnSet() }.toPluginDataFrameSchema()
    }
}

class MoveClauseApproximation(val df: PluginDataFrameSchema, val columns: ColumnsResolver)
