package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlin.fir.dataframe.api.RenameClauseApproximation
import org.jetbrains.kotlinx.dataframe.annotations.Absent
import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter.*
import org.jetbrains.kotlinx.dataframe.annotations.Arguments
import org.jetbrains.kotlinx.dataframe.annotations.DefaultValue
import org.jetbrains.kotlinx.dataframe.annotations.Interpreter
import org.jetbrains.kotlinx.dataframe.annotations.TypeApproximation
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

typealias ExpectedArgumentProvider<T> = PropertyDelegateProvider<Any?, ReadOnlyProperty<Arguments, T>>

fun <T> AbstractInterpreter<T>.dataFrame(
    name: ArgumentName? = null
): ExpectedArgumentProvider<PluginDataFrameSchema> = arg(name, lens = Interpreter.Schema)

fun <T> AbstractInterpreter<T>.varargString(
    name: ArgumentName? = null,
    defaultValue: DefaultValue<List<String>> = Absent
): ExpectedArgumentProvider<List<String>> = arg(name, lens = Interpreter.Value, defaultValue = defaultValue)

fun <T> AbstractInterpreter<T>.renameClause(
    name: ArgumentName? = null
): ExpectedArgumentProvider<RenameClauseApproximation> = arg(name, lens = Interpreter.Value)

fun <T> AbstractInterpreter<T>.columnsSelector(
    name: ArgumentName? = null
): ExpectedArgumentProvider<List<String>> = arg(name, lens = Interpreter.Value)

fun <T> AbstractInterpreter<T>.type(
    name: ArgumentName? = null
): ExpectedArgumentProvider<TypeApproximation> = arg(name, lens = Interpreter.ReturnType)

fun <T, E : Enum<E>> AbstractInterpreter<T>.enum(
    name: ArgumentName? = null,
    defaultValue: DefaultValue<E> = Absent
): ExpectedArgumentProvider<E> = argConvert(name = name, lens = Interpreter.Value, defaultValue = defaultValue) { it: DataFrameCallableId ->
    val forName: Class<*> = Class.forName("${it.packageName}.${it.className}")
    java.lang.Enum.valueOf(forName as Class<out Enum<*>>, it.callableName) as E
}

fun <T> AbstractInterpreter<T>.columnAccessor(
    name: ArgumentName? = null
): ExpectedArgumentProvider<ColumnAccessorApproximation> = arg(name, lens = Interpreter.Value)

fun <T> AbstractInterpreter<T>.dataColumn(
    name: ArgumentName? = null
): ExpectedArgumentProvider<SimpleCol> = arg(name, lens = Interpreter.Value)

fun <T> AbstractInterpreter<T>.insertClause(
    name: ArgumentName? = null
): ExpectedArgumentProvider<InsertClauseApproximation> = arg(name, lens = Interpreter.Value)

internal fun <T> AbstractInterpreter<T>.columnPath(
    name: ArgumentName? = null
): ExpectedArgumentProvider<ColumnPathApproximation> = arg(name, lens = Interpreter.Value)

internal fun <T> AbstractInterpreter<T>.columnWithPath(
    name: ArgumentName? = null
): ExpectedArgumentProvider<ColumnWithPathApproximation> = arg(name, lens = Interpreter.Value)

fun <T> AbstractInterpreter<T>.kproperty(
    name: ArgumentName? = null
): ExpectedArgumentProvider<KPropertyApproximation> = arg(name, lens = Interpreter.Value)

internal fun <T> AbstractInterpreter<T>.string(
    name: ArgumentName? = null
): ExpectedArgumentProvider<String
    > = arg(name, lens = Interpreter.Value)
