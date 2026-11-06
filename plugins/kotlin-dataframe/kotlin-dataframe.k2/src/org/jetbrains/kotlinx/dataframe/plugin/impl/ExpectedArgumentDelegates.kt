package org.jetbrains.kotlinx.dataframe.plugin.impl

import org.jetbrains.kotlinx.dataframe.plugin.extensions.ColumnType
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter.ArgumentName
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupBy
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.DataFrameCallableId
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

typealias ExpectedArgumentProvider<T> = PropertyDelegateProvider<Any?, ReadOnlyProperty<Arguments, T>>

fun <T> AbstractInterpreter<T>.dataFrame(
    name: ArgumentName? = null,
): ExpectedArgumentProvider<PluginDataFrameSchema> = arg(name, lens = Interpreter.Schema)

fun <T> AbstractInterpreter<T>.type(
    name: ArgumentName? = null,
): ExpectedArgumentProvider<ColumnType> = arg(name, lens = Interpreter.ReturnType)

fun <T, E : Enum<E>> AbstractInterpreter<T>.enum(
    name: ArgumentName? = null,
    defaultValue: DefaultValue<E> = Absent,
): ExpectedArgumentProvider<E> = argConvert(name = name, defaultValue = defaultValue) { it: DataFrameCallableId ->
    @Suppress("UNCHECKED_CAST")
    it.toEnumValue() as E
}

/**
 * Resolves an enum entry reference, such as `ColumnKind.Value`, to the actual enum value
 * of the class available in the plugin classpath.
 */
internal fun DataFrameCallableId.toEnumValue(): Enum<*> {
    val forName: Class<*> = Class.forName("$packageName.$className")
    @Suppress("UNCHECKED_CAST")
    return java.lang.Enum.valueOf(forName as Class<out Enum<*>>, callableName)
}

internal fun <T> AbstractInterpreter<T>.dsl(
    name: ArgumentName? = null,
): ExpectedArgumentProvider<(Any, Map<String, Interpreter.Success<Any?>>) -> Unit> =
    arg(name, lens = Interpreter.Dsl, defaultValue = Present(value = { _, _ -> }))

internal fun <T> AbstractInterpreter<T>.ignore(
    name: ArgumentName? = null,
): ExpectedArgumentProvider<Nothing?> =
    arg(name, lens = Interpreter.Id, defaultValue = Present(null))

internal fun <T> AbstractInterpreter<T>.groupBy(
    name: ArgumentName? = null,
): ExpectedArgumentProvider<GroupBy> = arg(name, lens = Interpreter.GroupBy)
