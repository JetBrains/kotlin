/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl

import org.jetbrains.kotlinx.dataframe.api.pathOf
import org.jetbrains.kotlinx.dataframe.impl.columns.ColumnsList
import org.jetbrains.kotlinx.dataframe.plugin.StringApiOverloadData
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColumnsResolver
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.stringApiColumnResolver
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import kotlin.reflect.typeOf

class StringApiOverloadInterpreter(
    private val adaptee: Interpreter<*>,
    private val stringApiOverload: StringApiOverloadData,
) : Interpreter<Any?> {
    override fun expectedArguments(): List<Interpreter.ExpectedArgument> {
        return adaptee.expectedArguments()
            .filterNot { it.name == stringApiOverload.targetArgument }
            .plus(
                Interpreter.ExpectedArgument(
                    stringApiOverload.stringArgument,
                    typeOf<List<String>>(),
                    Interpreter.Value,
                    defaultValue = Absent
                ),
            )
    }

    override fun interpret(
        arguments: Map<String, Interpreter.Success<Any?>>,
        kotlinTypeFacade: KotlinTypeFacade,
    ): Interpreter.InterpretationResult<Any?> {
        val stringApi = stringApiOverload.stringArgument
        val success = arguments[stringApi]
            ?: return Interpreter.Error("Argument with name `${stringApiOverload.stringArgument}` not found")
        val value = success.value
        if (value !is List<*>) {
            return Interpreter.Error("Expected List<String> value of vararg columns argument, got ${value?.javaClass} instead")
        }
        val selectedColumns = value.filterIsInstance<String>()
            .map { name ->
                kotlinTypeFacade.stringApiColumnResolver(
                    pathOf(name),
                    kotlinTypeFacade.session.builtinTypes.nullableAnyType.coneType
                )
            }
        val adaptedSelector: ColumnsResolver = object : ColumnsResolver, ColumnsList<Any?> {
            override val columns = selectedColumns

            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                return selectedColumns.flatMap { it.resolve(df) }
            }
        }
        val targetArguments = arguments.toMutableMap().apply {
            remove(stringApi)
            put(stringApiOverload.targetArgument, Interpreter.Success(adaptedSelector))
        }
        return adaptee.interpret(targetArguments, kotlinTypeFacade)
    }

    override fun startingSchema(
        arguments: Map<String, Interpreter.Success<Any?>>,
        kotlinTypeFacade: KotlinTypeFacade,
    ): PluginDataFrameSchema? {
        return adaptee.startingSchema(arguments, kotlinTypeFacade)
    }
}
