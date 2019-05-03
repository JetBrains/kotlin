/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class BuiltinMembersConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
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
        }.let { expression ->
            conversion.actionAfter?.invoke(expression.copyTreeAndDetach()) ?: expression
        }
    }

    private fun JKExpression.getConversion(): Conversion? = when (this) {
        is JKMethodCallExpression ->
            conversions[identifier.deepestFqName()]?.firstOrNull { conversion ->
                if (conversion.from !is Method) return@firstOrNull false
                if (conversion.filter?.invoke(this) == false) return@firstOrNull false
                if (conversion.byArgumentsFilter?.invoke(arguments.arguments.map { it.value }) == false) return@firstOrNull false
                true
            }
        is JKFieldAccessExpression ->
            conversions[identifier.deepestFqName()]?.firstOrNull { conversion ->
                if (conversion.from !is Field) return@firstOrNull false
                if (conversion.filter?.invoke(this) == false) return@firstOrNull false
                true
            }
        else -> null
    }


    private interface ResultBuilder {
        fun build(from: JKExpression): JKExpression
    }

    private inner class MethodBuilder(
        private val fqName: String,
        private val argumentsProvider: (JKArgumentList) -> JKArgumentList
    ) : ResultBuilder {
        override fun build(from: JKExpression): JKExpression =
            when (from) {
                is JKMethodCallExpression ->
                    JKKtCallExpressionImpl(
                        context.symbolProvider.provideByFqName(fqName, true),
                        argumentsProvider(from::arguments.detached()),
                        from::typeArgumentList.detached()
                    ).withNonCodeElementsFrom(from)
                is JKFieldAccessExpression ->
                    JKKtCallExpressionImpl(
                        context.symbolProvider.provideByFqName(fqName, true),
                        JKArgumentListImpl(),
                        JKTypeArgumentListImpl()
                    ).withNonCodeElementsFrom(from)
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
                        context.symbolProvider.provideByFqName(fqName, true)
                    ).withNonCodeElementsFrom(from)
                is JKFieldAccessExpression ->
                    JKFieldAccessExpressionImpl(
                        context.symbolProvider.provideByFqName(fqName, true)
                    ).withNonCodeElementsFrom(from)
                else -> error("Bad conversion")
            }
    }

    private inner class ExtensionMethodBuilder(
        private val fqName: String
    ) : ResultBuilder {
        override fun build(from: JKExpression): JKExpression =
            when (from) {
                is JKMethodCallExpression -> {
                    val arguments = from.arguments::arguments.detached()
                    JKQualifiedExpressionImpl(
                        arguments.first()::value.detached().parenthesizeIfBinaryExpression(),
                        JKKtQualifierImpl.DOT,
                        JKKtCallExpressionImpl(
                            context.symbolProvider.provideByFqName(fqName, true),
                            JKArgumentListImpl(arguments.drop(1)),
                            from::typeArgumentList.detached()
                        )
                    ).withNonCodeElementsFrom(from)
                }
                else -> error("Bad conversion")
            }
    }

    private inner class CustomExpressionBuilder(
        val builder: (JKExpression) -> JKExpression
    ) : ResultBuilder {
        override fun build(from: JKExpression): JKExpression = builder(from)
    }

    private fun Conversion.createBuilder(): ResultBuilder =
        when (to) {
            is Method -> MethodBuilder(to.fqName, argumentsProvider ?: { it })
            is Field -> FieldBuilder(to.fqName)
            is ExtensionMethod -> ExtensionMethodBuilder(to.fqName)
            is CustomExpression -> CustomExpressionBuilder(to.expressionBuilder)
            else -> error("Bad conversion")
        }


    private enum class ReplaceType {
        REPLACE_SELECTOR, FULL_REPLACE
    }


    private interface Info
    private interface SymbolInfo : Info {
        val fqName: String
    }

    private data class Method(override val fqName: String) : SymbolInfo
    private data class Field(override val fqName: String) : SymbolInfo
    private data class ExtensionMethod(override val fqName: String) : SymbolInfo
    private data class CustomExpression(val expressionBuilder: (JKExpression) -> JKExpression) : Info

    private data class Conversion(
        val from: SymbolInfo,
        val to: Info,
        val replaceType: ReplaceType = ReplaceType.REPLACE_SELECTOR,
        val filter: ((JKExpression) -> Boolean)? = null,
        val byArgumentsFilter: ((List<JKExpression>) -> Boolean)? = null,
        val argumentsProvider: ((JKArgumentList) -> JKArgumentList)? = null,
        val actionAfter: ((JKExpression) -> JKExpression)? = null
    )

    private infix fun SymbolInfo.convertTo(to: Info) =
        Conversion(this, to)

    private infix fun Conversion.withReplaceType(replaceType: ReplaceType) =
        copy(replaceType = replaceType)

    private infix fun Conversion.withFilter(filter: (JKExpression) -> Boolean) =
        copy(filter = filter)

    private infix fun Conversion.withByArgumentsFilter(filter: (List<JKExpression>) -> Boolean) =
        copy(byArgumentsFilter = filter)

    private infix fun Conversion.withArgumentsProvider(argumentsProvider: (JKArgumentList) -> JKArgumentList) =
        copy(argumentsProvider = argumentsProvider)

    private infix fun Conversion.andAfter(actionAfter: (JKExpression) -> JKExpression) =
        copy(actionAfter = actionAfter).also { println() }

    private val conversions: Map<String, List<Conversion>> =
        listOf(
            Method("java.lang.Integer.intValue") convertTo Method("kotlin.Int.toInt"),//TODO do not list all variants

            Method("java.io.PrintStream.println") convertTo Method("kotlin.io.println")
                    withReplaceType ReplaceType.FULL_REPLACE
                    withFilter ::isSystemOutCall,

            Method("java.io.PrintStream.print") convertTo Method("kotlin.io.print")
                    withReplaceType ReplaceType.FULL_REPLACE
                    withFilter ::isSystemOutCall,

            Method("java.lang.Object.getClass") convertTo Field("kotlin.jvm.javaClass"),

            Method("java.util.Map.entrySet") convertTo Field("kotlin.collections.Map.entries"),
            Method("java.util.Map.keySet") convertTo Field("kotlin.collections.Map.keys"),
            Method("java.util.Map.size") convertTo Field("kotlin.collections.Map.size"),
            Method("java.util.Map.values") convertTo Field("kotlin.collections.Map.values"),
            Method("java.util.Collection.size") convertTo Field("kotlin.collections.Collection.size"),
            Method("java.util.Collection.remove") convertTo Method("kotlin.collections.MutableCollection.remove"),
            Method("java.util.Collection.toArray")
                    convertTo Method("kotlin.collections.toTypedArray")
                    withByArgumentsFilter { it.isEmpty() },
            Method("java.util.Collection.toArray")
                    convertTo Method("kotlin.collections.toTypedArray")
                    withByArgumentsFilter {
                it.singleOrNull()?.let { parameter ->
                    parameter.safeAs<JKMethodCallExpression>()?.identifier?.fqName == "kotlin.arrayOfNulls"
                } == true
            } withArgumentsProvider { JKArgumentListImpl() },

            Method("java.util.List.remove") convertTo Method("kotlin.collections.MutableCollection.removeAt"),
            Method("java.util.Map.Entry.getKey") convertTo Field("kotlin.collections.Map.Entry.key"),
            Method("java.util.Map.Entry.getValue") convertTo Field("kotlin.collections.Map.Entry.value"),

            Method("java.lang.Enum.name") convertTo Field("kotlin.Enum.name"),
            Method("java.lang.Enum.ordinal") convertTo Field("kotlin.Enum.ordinal"),

            Method("java.lang.Throwable.getCause") convertTo Field("kotlin.Throwable.cause"),
            Method("java.lang.Throwable.getMessage") convertTo Field("kotlin.Throwable.message"),

            Method("java.lang.CharSequence.length") convertTo Field("kotlin.String.length"),
            Method("java.lang.CharSequence.charAt") convertTo Method("kotlin.String.get"),
            Method("java.lang.String.indexOf") convertTo Method("kotlin.text.indexOf"),
            Method("java.lang.String.lastIndexOf") convertTo Method("kotlin.text.lastIndexOf"),
            Method("java.lang.String.getBytes") convertTo Method("kotlin.text.toByteArray"),
            Method("java.lang.String.valueOf")
                    convertTo ExtensionMethod("kotlin.Any.toString")
                    withReplaceType ReplaceType.FULL_REPLACE
                    withByArgumentsFilter { it.isNotEmpty() && it.first().type(context.symbolProvider)?.isArrayType() == false },

            Method("java.lang.String.valueOf")
                    convertTo Method("kotlin.String")
                    withReplaceType ReplaceType.FULL_REPLACE
                    withByArgumentsFilter { it.isNotEmpty() && it.first().type(context.symbolProvider)?.isArrayType() == true },

            Method("java.lang.String.copyValueOf")
                    convertTo Method("kotlin.String")
                    withReplaceType ReplaceType.FULL_REPLACE
                    withByArgumentsFilter { it.isNotEmpty() && it.first().type(context.symbolProvider)?.isArrayType() == true },

            Method("java.lang.String.replaceAll")
                    convertTo Method("kotlin.text.replace")
                    withArgumentsProvider { arguments ->
                val detachedArguments = arguments::arguments.detached()
                val first =
                    detachedArguments.first()::value.detached().callOn(
                        context.symbolProvider.provideByFqName(
                            "kotlin.text.toRegex",
                            multiResolve = true
                        )
                    )
                JKArgumentListImpl(listOf(JKArgumentImpl(first)) + detachedArguments.drop(1))
            },
            Method("java.lang.String.replaceFirst")
                    convertTo Method("kotlin.text.replaceFirst")
                    withArgumentsProvider { arguments ->
                val detachedArguments = arguments::arguments.detached()
                val first =
                    detachedArguments.first()::value.detached().callOn(
                        context.symbolProvider.provideByFqName(
                            "kotlin.text.toRegex",
                            multiResolve = true
                        )
                    )
                JKArgumentListImpl(listOf(JKArgumentImpl(first)) + detachedArguments.drop(1))
            },
            Method("java.lang.String.equalsIgnoreCase")
                    convertTo Method("kotlin.text.equals")
                    withArgumentsProvider { arguments ->
                JKArgumentListImpl(
                    arguments::arguments.detached() + JKNamedArgumentImpl(
                        JKBooleanLiteral(true),
                        JKNameIdentifierImpl("ignoreCase")
                    )
                )
            },

            Method("java.lang.String.compareToIgnoreCase")
                    convertTo Method("kotlin.text.compareTo")
                    withArgumentsProvider { arguments ->
                JKArgumentListImpl(
                    arguments::arguments.detached() + JKNamedArgumentImpl(
                        JKBooleanLiteral(true),
                        JKNameIdentifierImpl("ignoreCase")
                    )
                )
            },


            Method("java.lang.String.split")
                    convertTo Method("kotlin.text.split")
                    withByArgumentsFilter { it.size == 2 }
                    andAfter { expression ->
                val arguments =
                    expression.cast<JKQualifiedExpression>()
                        .selector.cast<JKMethodCallExpression>()
                        .arguments
                val limitArgument = arguments.arguments[1].value
                val limit = limitArgument.asLiteralTextWithPrefix()?.toIntOrNull()
                when {
                    limit != null -> {
                        if (limit > 0) expression
                        else expression
                            .also {
                                arguments.arguments = arguments.arguments.dropLast(1)
                            }.let {
                                it.callOn(
                                    context.symbolProvider.provideByFqName("kotlin.collections.dropLastWhile", multiResolve = true),
                                    JKLambdaExpressionImpl(
                                        JKFieldAccessExpressionImpl(
                                            JKUnresolvedField(//TODO replace with `it` parameter
                                                "it",
                                                context.symbolProvider
                                            )
                                        ).callOn(
                                            context.symbolProvider.provideByFqName(
                                                "kotlin.text.isEmpty",
                                                multiResolve = true
                                            )
                                        ).asStatement(),
                                        emptyList()
                                    )
                                )
                            }
                    }
                    else -> expression.also {
                        val lastArgument = arguments.arguments.last().value.copyTreeAndDetach()
                            .callOn(
                                context.symbolProvider.provideByFqName("kotlin.ranges.coerceAtLeast"),
                                JKKtLiteralExpressionImpl("0", JKLiteralExpression.LiteralType.INT)
                            )
                        arguments.arguments = arguments.arguments.dropLast(1) + JKArgumentImpl(lastArgument)
                    }
                }.castToTypedArray()
            },


            Method("java.lang.String.split")
                    convertTo Method("kotlin.text.split")
                    andAfter { it.castToTypedArray() },

            Method("java.lang.String.trim")
                    convertTo Method("kotlin.text.trim")
                    withArgumentsProvider {
                JKArgumentListImpl(
                    JKLambdaExpressionImpl(
                        JKExpressionStatementImpl(
                            kotlinBinaryExpression(
                                JKFieldAccessExpressionImpl(
                                    JKUnresolvedField(//TODO replace with `it` parameter
                                        "it",
                                        context.symbolProvider
                                    )
                                ),
                                JKKtLiteralExpressionImpl("' '", JKLiteralExpression.LiteralType.CHAR),
                                KtTokens.LTEQ,
                                context.symbolProvider
                            )
                        ),
                        emptyList()
                    )
                )
            },


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


    private fun JKExpression.callOn(symbol: JKMethodSymbol, arguments: List<JKArgument> = emptyList()) =
        JKQualifiedExpressionImpl(
            this,
            JKKtQualifierImpl.DOT,
            JKKtCallExpressionImpl(
                symbol,
                JKArgumentListImpl(arguments),
                JKTypeArgumentListImpl()
            )
        )

    private fun JKExpression.callOn(symbol: JKMethodSymbol, vararg arguments: JKArgument) =
        callOn(symbol, arguments.toList())

    private fun JKExpression.callOn(symbol: JKMethodSymbol, vararg arguments: JKExpression) =
        callOn(symbol, arguments.map { JKArgumentImpl(it) })

    private fun isSystemOutCall(expression: JKExpression): Boolean =
        expression.parent
            ?.safeAs<JKQualifiedExpression>()
            ?.receiver
            ?.let { receiver ->
                when (receiver) {
                    is JKFieldAccessExpression -> receiver
                    is JKQualifiedExpression -> receiver.selector as? JKFieldAccessExpression
                    else -> null
                }
            }?.identifier
            ?.deepestFqName() == "java.lang.System.out"


    private fun JKExpression.castToTypedArray() =
        callOn(context.symbolProvider.provideByFqName("kotlin.collections.toTypedArray", multiResolve = true))
}
