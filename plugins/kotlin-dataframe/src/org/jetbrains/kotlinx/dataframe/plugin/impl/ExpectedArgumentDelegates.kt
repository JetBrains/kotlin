package org.jetbrains.kotlinx.dataframe.plugin.impl

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter.*
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupBy
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.TypeApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.DataFrameCallableId
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

typealias ExpectedArgumentProvider<T> = PropertyDelegateProvider<Any?, ReadOnlyProperty<Arguments, T>>

fun <T> AbstractInterpreter<T>.dataFrame(
    name: ArgumentName? = null
): ExpectedArgumentProvider<PluginDataFrameSchema> = arg(name, lens = Interpreter.Schema)

fun <T> AbstractInterpreter<T>.type(
    name: ArgumentName? = null
): ExpectedArgumentProvider<TypeApproximation> = arg(name, lens = Interpreter.ReturnType)

fun <T, E : Enum<E>> AbstractInterpreter<T>.enum(
    name: ArgumentName? = null,
    defaultValue: DefaultValue<E> = Absent
): ExpectedArgumentProvider<E> = argConvert(name = name, defaultValue = defaultValue) { it: DataFrameCallableId ->
    val forName: Class<*> = Class.forName("${it.packageName}.${it.className}")
    @Suppress("UNCHECKED_CAST")
    java.lang.Enum.valueOf(forName as Class<out Enum<*>>, it.callableName) as E
}

internal fun <T> AbstractInterpreter<T>.dsl(
    name: ArgumentName? = null
): ExpectedArgumentProvider<(Any, Map<String, Interpreter.Success<Any?>>) -> Unit> =
    arg(name, lens = Interpreter.Dsl, defaultValue = Present(value = {_, _ -> }))

internal fun <T> AbstractInterpreter<T>.ignore(
    name: ArgumentName? = null
): ExpectedArgumentProvider<Nothing?> =
    arg(name, lens = Interpreter.Id, defaultValue = Present(null))

internal fun <T> AbstractInterpreter<T>.groupBy(
    name: ArgumentName? = null
): ExpectedArgumentProvider<GroupBy> = arg(name, lens = Interpreter.GroupBy)

