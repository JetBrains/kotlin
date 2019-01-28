/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*

class BuiltinMembersConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKExpression) return recurse(element)
        return recurse(element.convert() ?: element)
    }

    private fun JKExpression.convert(): JKExpression? {
        val selector = when (this) {
            is JKQualifiedExpression -> selector
            else -> this
        }

        val conversion = selector.getConversion() ?: return null
        val newSelector = conversion.createBuilder().build(selector)

        return when (conversion.replaceType) {
            ReplaceType.REPLACE_SELECTOR -> {
                if (this is JKQualifiedExpression) {
                    this.selector = newSelector
                    this
                } else newSelector
            }
            ReplaceType.FULL_REPLACE -> newSelector
        }
    }

    private fun JKExpression.getConversion(): Conversion? = when (this) {
        is JKMethodCallExpression ->
            conversions[identifier.deepestFqName()]?.firstOrNull() { conversion ->
                conversion.from is Method && conversion.byArgumentsFilter?.invoke(arguments.expressions) ?: true
            }
        is JKFieldAccessExpression ->
            conversions[identifier.deepestFqName()]?.firstOrNull { conversion -> conversion.from is Field }
        else -> null
    }


    private interface ResultBuilder {
        fun build(from: JKExpression): JKExpression
    }

    private inner class MethodBuilder(
        private val fqName: String,
        private val argumentsProvider: (JKExpressionList) -> JKExpressionList
    ) : ResultBuilder {
        override fun build(from: JKExpression): JKExpression =
            when (from) {
                is JKMethodCallExpression ->
                    JKKtCallExpressionImpl(
                        context.symbolProvider.provideByFqNameMulti(fqName),
                        argumentsProvider(from::arguments.detached()),
                        from::typeArgumentList.detached()
                    )
                is JKFieldAccessExpression ->
                    JKKtCallExpressionImpl(
                        context.symbolProvider.provideByFqNameMulti(fqName),
                        JKExpressionListImpl(),
                        JKTypeArgumentListImpl()
                    )
                else -> error("Bad conversion")
            }
    }

    private inner class FieldBuilder(
        private val fqName: String
    ) : ResultBuilder {
        override fun build(from: JKExpression): JKExpression =
            when (from) {
                is JKMethodCallExpression ->
                    JKFieldAccessExpressionImpl(
                        context.symbolProvider.provideByFqNameMulti(fqName)
                    )
                is JKFieldAccessExpression ->
                    JKFieldAccessExpressionImpl(
                        context.symbolProvider.provideByFqNameMulti(fqName)
                    )
                else -> error("Bad conversion")
            }
    }

    private inner class ExtensionMethodBuilder(
        private val fqName: String
    ) : ResultBuilder {
        override fun build(from: JKExpression): JKExpression =
            when (from) {
                is JKMethodCallExpression -> {
                    val arguments = from.arguments::expressions.detached()
                    JKQualifiedExpressionImpl(
                        arguments.first(),
                        JKKtQualifierImpl.DOT,
                        JKKtCallExpressionImpl(
                            context.symbolProvider.provideByFqNameMulti(fqName),
                            JKExpressionListImpl(arguments.drop(1)),
                            from::typeArgumentList.detached()
                        )
                    )
                }
                else -> error("Bad conversion")
            }
    }

    private fun Conversion.createBuilder(): ResultBuilder =
        when (to) {
            is Method -> MethodBuilder(to.fqName, argumentsProvider ?: { it })
            is Field -> FieldBuilder(to.fqName)
            is ExtensionMethod -> ExtensionMethodBuilder(to.fqName)
            else -> error("Bad conversion")
        }


    private enum class ReplaceType {
        REPLACE_SELECTOR, FULL_REPLACE
    }

    private interface Info {
        val fqName: String
    }

    private data class Method(override val fqName: String) : Info
    private data class Field(override val fqName: String) : Info
    private data class ExtensionMethod(override val fqName: String) : Info

    private data class Conversion(
        val from: Info,
        val to: Info,
        val replaceType: ReplaceType = ReplaceType.REPLACE_SELECTOR,
        val byArgumentsFilter: ((List<JKExpression>) -> Boolean)? = null,
        val argumentsProvider: ((JKExpressionList) -> JKExpressionList)? = null
    )

    private infix fun Info.convertTo(to: Info) =
        Conversion(this, to)

    private infix fun Conversion.withReplaceType(replaceType: ReplaceType) =
        copy(replaceType = replaceType)

    private infix fun Conversion.withByArgumentsFilter(filter: (List<JKExpression>) -> Boolean) =
        copy(byArgumentsFilter = filter)

    private infix fun Conversion.withArgumentsProvider(argumentsProvider: (JKExpressionList) -> JKExpressionList) =
        copy(argumentsProvider = argumentsProvider)

    private val conversions: Map<String, List<Conversion>> =
        listOf(
            Method("java.lang.Integer.intValue") convertTo Method("kotlin.Int.toInt"),//TODO do not list all variants

            Method("java.lang.Object.getClass") convertTo Field("kotlin.jvm.javaClass"),

            Method("java.util.Map.entrySet") convertTo Field("kotlin.collections.Map.entries"),
            Method("java.util.Map.keySet") convertTo Field("kotlin.collections.Map.keys"),
            Method("java.util.Map.size") convertTo Field("kotlin.collections.Map.size"),
            Method("java.util.Map.values") convertTo Field("kotlin.collections.Map.values"),
            Method("java.util.Collection.size") convertTo Field("kotlin.collections.Collection.size"),
            Method("java.util.Collection.remove") convertTo Method("kotlin.collections.MutableCollection.remove"),
            Method("java.util.List.remove") convertTo Method("kotlin.collections.MutableCollection.removeAt"),
            Method("java.util.Map.Entry.getKey") convertTo Field("kotlin.collections.Map.Entry.key"),
            Method("java.util.Map.Entry.getValue") convertTo Field("kotlin.collections.Map.Entry.value"),

            Method("java.lang.Enum.name") convertTo Field("kotlin.Enum.name"),
            Method("java.lang.Enum.ordinal") convertTo Field("kotlin.Enum.ordinal"),

            Method("java.lang.Throwable.getCause") convertTo Field("kotlin.Throwable.cause"),
            Method("java.lang.Throwable.getMessage") convertTo Field("kotlin.Throwable.message"),

            Method("java.lang.CharSequence.length") convertTo Field("kotlin.String.length"),
            Method("java.lang.CharSequence.charAt") convertTo Method("kotlin.String.get"),
            Method("java.lang.String.valueOf")
                    convertTo ExtensionMethod("kotlin.Any.toString")
                    withReplaceType ReplaceType.FULL_REPLACE
                    withByArgumentsFilter { it.isNotEmpty() && it.first().type(context.symbolProvider)?.isArrayType() == false },


            Method("java.util.Collections.singletonList") convertTo Method("kotlin.collections.listOf")
                    withReplaceType ReplaceType.FULL_REPLACE,
            Method("java.util.Collections.singleton") convertTo Method("kotlin.collections.setOf")
                    withReplaceType ReplaceType.FULL_REPLACE,
            Method("java.util.Collections.emptyList")
                    convertTo Method("kotlin.collections.emptyList") withReplaceType ReplaceType.FULL_REPLACE,
            Method("java.util.Collections.emptySet")
                    convertTo Method("kotlin.collections.emptySet") withReplaceType ReplaceType.FULL_REPLACE,
            Method("java.util.Collections.emptyMap")
                    convertTo Method("kotlin.collections.emptyMap") withReplaceType ReplaceType.FULL_REPLACE
        ).groupBy { it.from.fqName }
}
