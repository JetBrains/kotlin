/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.symbols.JKMethodSymbol
import org.jetbrains.kotlin.nj2k.symbols.JKUnresolvedField
import org.jetbrains.kotlin.nj2k.symbols.deepestFqName
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

        if (this is JKQualifiedExpression && conversion.replaceType == ReplaceType.REPLACE_WITH_QUALIFIER) {
            newSelector.rightNonCodeElements =
                (receiver.leftNonCodeElements +
                        receiver.rightNonCodeElements +
                        selector.leftNonCodeElements +
                        selector.rightNonCodeElements).dropSpacesAtBegining()
        }

        return when (conversion.replaceType) {
            ReplaceType.REPLACE_SELECTOR -> {
                if (this is JKQualifiedExpression) {
                    this.selector = newSelector
                    this
                } else newSelector
            }
            ReplaceType.REPLACE_WITH_QUALIFIER -> newSelector
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

        is JKJavaNewExpression ->
            conversions[classSymbol.deepestFqName()]?.firstOrNull { conversion ->
                if (conversion.from !is NewExpression) return@firstOrNull false
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
                        context.symbolProvider.provideMethodSymbol(fqName),
                        argumentsProvider(from::arguments.detached()),
                        from::typeArgumentList.detached()
                    ).withNonCodeElementsFrom(from)
                is JKFieldAccessExpression ->
                    JKKtCallExpressionImpl(
                        context.symbolProvider.provideMethodSymbol(fqName),
                        JKArgumentListImpl(),
                        JKTypeArgumentListImpl()
                    ).withNonCodeElementsFrom(from)
                is JKJavaNewExpression ->
                    JKKtCallExpressionImpl(
                        context.symbolProvider.provideMethodSymbol(fqName),
                        argumentsProvider(from::arguments.detached()),
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
                        context.symbolProvider.provideFieldSymbol(fqName)
                    ).withNonCodeElementsFrom(from)
                is JKFieldAccessExpression ->
                    JKFieldAccessExpressionImpl(
                        context.symbolProvider.provideFieldSymbol(fqName)
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
                            context.symbolProvider.provideMethodSymbol(fqName),
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
        REPLACE_SELECTOR, REPLACE_WITH_QUALIFIER
    }


    private interface Info
    private interface SymbolInfo : Info {
        val fqName: String
    }

    private data class Method(override val fqName: String) : SymbolInfo
    private data class NewExpression(override val fqName: String) : SymbolInfo
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
        copy(actionAfter = actionAfter)

    private val conversions: Map<String, List<Conversion>> =
        listOf(
            Method("java.lang.Short.valueOf") convertTo ExtensionMethod("kotlin.Short.toShort")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER,

            Method("java.lang.Byte.parseByte") convertTo ExtensionMethod("kotlin.text.toByte")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER,
            Method("java.lang.Short.parseShort") convertTo ExtensionMethod("kotlin.text.toShort")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER,
            Method("java.lang.Integer.parseInt") convertTo ExtensionMethod("kotlin.text.toInt")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER,
            Method("java.lang.Long.parseLong") convertTo ExtensionMethod("kotlin.text.toLong")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER,
            Method("java.lang.Float.parseFloat") convertTo ExtensionMethod("kotlin.text.toFloat")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER,
            Method("java.lang.Double.parseDouble") convertTo ExtensionMethod("kotlin.text.toDouble")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER,

            Method("java.io.PrintStream.println") convertTo Method("kotlin.io.println")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER
                    withFilter ::isSystemOutCall,

            Method("java.io.PrintStream.print") convertTo Method("kotlin.io.print")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER
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
            Method("java.lang.String.getBytes") convertTo Method("kotlin.text.toByteArray")
                    withByArgumentsFilter { it.singleOrNull()?.type(context.symbolProvider)?.isStringType() == true }
                    withArgumentsProvider { arguments ->
                val argument = arguments.arguments.single()::value.detached()
                val call = JKKtCallExpressionImpl(
                    context.symbolProvider.provideMethodSymbol("kotlin.text.charset"),
                    JKArgumentListImpl(argument)
                )
                JKArgumentListImpl(call)
            },
            Method("java.lang.String.getBytes") convertTo Method("kotlin.text.toByteArray"),
            Method("java.lang.String.valueOf")
                    convertTo ExtensionMethod("kotlin.Any.toString")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER
                    withByArgumentsFilter { it.isNotEmpty() && it.first().type(context.symbolProvider)?.isArrayType() == false },

            Method("java.lang.String.getChars")
                    convertTo Method("kotlin.text.toCharArray")
                    withByArgumentsFilter { it.size == 4 }
                    withArgumentsProvider { argumentList ->
                val srcBeginArgument = argumentList.arguments[0]::value.detached()
                val srcEndArgument = argumentList.arguments[1]::value.detached()
                val dstArgument = argumentList.arguments[2]::value.detached()
                val dstBeginArgument = argumentList.arguments[3]::value.detached()
                JKArgumentListImpl(dstArgument, dstBeginArgument, srcBeginArgument, srcEndArgument)
            },

            Method("java.lang.String.valueOf")
                    convertTo Method("kotlin.String")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER
                    withByArgumentsFilter { it.isNotEmpty() && it.first().type(context.symbolProvider)?.isArrayType() == true },

            Method("java.lang.String.copyValueOf")
                    convertTo Method("kotlin.String")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER
                    withByArgumentsFilter { it.isNotEmpty() && it.first().type(context.symbolProvider)?.isArrayType() == true },

            Method("java.lang.String.replaceAll")
                    convertTo Method("kotlin.text.replace")
                    withArgumentsProvider { arguments ->
                val detachedArguments = arguments::arguments.detached()
                val first =
                    detachedArguments.first()::value.detached().callOn(
                        context.symbolProvider.provideMethodSymbol("kotlin.text.toRegex")
                    )
                JKArgumentListImpl(listOf(JKArgumentImpl(first)) + detachedArguments.drop(1))
            },
            Method("java.lang.String.replaceFirst")
                    convertTo Method("kotlin.text.replaceFirst")
                    withArgumentsProvider { arguments ->
                val detachedArguments = arguments::arguments.detached()
                val first =
                    detachedArguments.first()::value.detached().callOn(
                        context.symbolProvider.provideMethodSymbol("kotlin.text.toRegex")

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
            Method("java.lang.String.matches") convertTo Method("kotlin.text.matches"),
            Method("java.lang.String.regionMatches")
                    convertTo Method("kotlin.text.regionMatches")
                    withByArgumentsFilter { it.size == 5 }
                    withArgumentsProvider { arguments ->
                val detachedArguments = arguments::arguments.detached()
                JKArgumentListImpl(
                    detachedArguments.drop(1) + JKNamedArgumentImpl(
                        detachedArguments.first()::value.detached().also {
                            it.clearNonCodeElements()
                        },
                        JKNameIdentifierImpl("ignoreCase")
                    )
                )
            },

            Method("java.lang.String.concat") convertTo
                    CustomExpression { expression ->
                        if (expression !is JKMethodCallExpression) error("Expression should be JKMethodCallExpression")
                        val firstArgument = expression.parent.cast<JKQualifiedExpression>()::receiver.detached()
                        val secondArgument = expression.arguments.arguments.first()::value.detached()
                        kotlinBinaryExpression(
                            firstArgument,
                            secondArgument,
                            KtTokens.PLUS,
                            context.symbolProvider
                        )
                    } withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER,

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
                                    context.symbolProvider.provideMethodSymbol("kotlin.collections.dropLastWhile"),
                                    JKLambdaExpressionImpl(
                                        JKFieldAccessExpressionImpl(
                                            JKUnresolvedField(//TODO replace with `it` parameter
                                                "it",
                                                context.symbolProvider
                                            )
                                        ).callOn(context.symbolProvider.provideMethodSymbol("kotlin.text.isEmpty"))
                                            .asStatement(),
                                        emptyList()
                                    )
                                )
                            }
                    }
                    else -> expression.also {
                        val lastArgument = arguments.arguments.last().value.copyTreeAndDetach()
                            .callOn(
                                context.symbolProvider.provideMethodSymbol("kotlin.ranges.coerceAtLeast"),
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
            Method("java.lang.String.format") convertTo CustomExpression { expression ->
                JKClassAccessExpressionImpl(
                    context.symbolProvider.provideClassSymbol(KotlinBuiltIns.FQ_NAMES.string)
                ).callOn(
                    context.symbolProvider.provideMethodSymbol("kotlin.text.String.format"),
                    (expression as JKMethodCallExpression).arguments::arguments.detached()
                )
            } withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER,

            NewExpression("java.lang.String") convertTo Method("kotlin.text.String"),
            NewExpression("kotlin.String") convertTo Method("kotlin.text.String"),

            Method("java.util.Collections.singletonList") convertTo Method("kotlin.collections.listOf")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER,
            Method("java.util.Collections.singleton") convertTo Method("kotlin.collections.setOf")
                    withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER,
            Method("java.util.Collections.emptyList")
                    convertTo Method("kotlin.collections.emptyList") withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER,
            Method("java.util.Collections.emptySet")
                    convertTo Method("kotlin.collections.emptySet") withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER,
            Method("java.util.Collections.emptyMap")
                    convertTo Method("kotlin.collections.emptyMap") withReplaceType ReplaceType.REPLACE_WITH_QUALIFIER
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
        callOn(context.symbolProvider.provideMethodSymbol("kotlin.collections.toTypedArray"))
}
