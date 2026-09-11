/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.StringValue
import org.jetbrains.dokka.plugability.DokkaContext

/**
 *  Filter out methods and synthetic properties that come from [mapping to Java](https://kotlinlang.org/docs/java-interop.html#mapped-types)
 *  since they have no available doc
 *
 *  Currently, a list of such methods is supported manually based on the compiler source
 *  [here](https://github.com/JetBrains/kotlin/blob/0ef41d75b5901afea68a09cc3e762b52d8ce1e3c/core/compiler.common.jvm/src/org/jetbrains/kotlin/builtins/jvm/JvmBuiltInsSignatures.kt#L15).
 *
 * There are the 4 statuses (JDKMemberStatus) of mapped methods:
 * - HIDDEN
 * - VISIBLE
 * - DROP
 * - NOT_CONSIDERED in K1 / HIDDEN_IN_DECLARING_CLASS_ONLY (KT-65438 WEAKLY_HIDDEN) in K2. See [DFunction.hasDeprecated]
 * We want co cover the HIDDEN, VISIBLE, HIDDEN_IN_DECLARING_CLASS_ONLY statuses here.
 *
 *  TODO https://youtrack.jetbrains.com/issue/KT-69796/Analysis-API-Provide-a-way-to-detect-mapped-methods-from-JVM
 */
internal class JvmMappedMethodsDocumentableFilterTransformer(context: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(context) {

    private fun inJavaLang(baseName: String, vararg signatures: String): Set<String> {
        val packageAndClassNames = when (baseName) {
            "CharSequence" -> "kotlin.CharSequence"
            "Throwable" -> "kotlin.Throwable"
            "Iterable" -> "kotlin.collections.Iterable"
            "String" -> "kotlin.String"
            "Enum" -> "kotlin.Enum"
            "Double" -> "kotlin.Double"
            "Float" -> "kotlin.Float"
            "List" -> "kotlin.collections.list"
            else -> throw IllegalStateException()
        }
        return signatures.map { "$packageAndClassNames.${it.substring(0, it.indexOf("("))}" }.toSet()
    }

    private fun inJavaUtil(baseName: String, vararg signatures: String): Set<String> {
        val packageAndClassNames = when (baseName) {
            "List" -> "kotlin.collections.List"
            "MutableList" -> "kotlin.collections.MutableList"
            "Iterator" -> "kotlin.collections.Iterator"
            "Collection" -> "kotlin.collections.Collection"
            "MutableCollection" -> "kotlin.collections.MutableCollection"
            "Map" -> "kotlin.collections.Map"
            "MutableMap" -> "kotlin.collections.MutableMap"
            else -> throw IllegalStateException()
        }
        return if (packageAndClassNames == "kotlin.collections.Collection") { // hack for K1 since it can has  kotlin.collections.Set.spliterator instead of kotlin.collections.Collection.spliterator
            signatures.map { "kotlin.collections.Collection.${it.substring(0, it.indexOf("("))}" }
                .toSet() + signatures.map { "kotlin.collections.Set.${it.substring(0, it.indexOf("("))}" }
                .toSet() + signatures.map { "kotlin.collections.List.${it.substring(0, it.indexOf("("))}" }.toSet()

        } else signatures.map { "$packageAndClassNames.${it.substring(0, it.indexOf("("))}" }.toSet()
    }

    // this "grey" list does not exist in the compiler explicitly
    // this was made manually
    private val NOT_CONSIDER_METHOD_SIGNATURES: Set<String> = inJavaUtil(
        "Collection", "toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object;"
    ) + inJavaLang("Enum", "describeConstable()Ljava/util/Optional")


    // copy-pasted from [here](https://github.com/JetBrains/kotlin/blob/0ef41d75b5901afea68a09cc3e762b52d8ce1e3c/core/compiler.common.jvm/src/org/jetbrains/kotlin/builtins/jvm/JvmBuiltInsSignatures.kt#L15)
    private val VISIBLE_METHOD_SIGNATURES: Set<String> = inJavaLang(
        "CharSequence",
        "codePoints()Ljava/util/stream/IntStream;",
        "chars()Ljava/util/stream/IntStream;",
        // note: this method is not handled by the compiler (https://youtrack.jetbrains.com/issue/KT-80102)
        "getChars(II[CI)V"
    ) +

            inJavaUtil(
                "Iterator", "forEachRemaining(Ljava/util/function/Consumer;)V"
            ) +

            inJavaLang(
                "Iterable", "forEach(Ljava/util/function/Consumer;)V", "spliterator()Ljava/util/Spliterator;"
            ) +

            inJavaLang(
                "Throwable",
                "setStackTrace([Ljava/lang/StackTraceElement;)V",
                "fillInStackTrace()Ljava/lang/Throwable;",
                "getLocalizedMessage()Ljava/lang/String;",
                "printStackTrace()V",
                "printStackTrace(Ljava/io/PrintStream;)V",
                "printStackTrace(Ljava/io/PrintWriter;)V",
                "getStackTrace()[Ljava/lang/StackTraceElement;",
                "initCause(Ljava/lang/Throwable;)Ljava/lang/Throwable;",
                "getSuppressed()[Ljava/lang/Throwable;",
                "addSuppressed(Ljava/lang/Throwable;)V"
            ) +

            inJavaUtil(
                "Collection",
                "spliterator()Ljava/util/Spliterator;",
                "parallelStream()Ljava/util/stream/Stream;",
                "stream()Ljava/util/stream/Stream;",
                "removeIf(Ljava/util/function/Predicate;)Z"
            ) +

            inJavaUtil(
                "List",
                "replaceAll(Ljava/util/function/UnaryOperator;)V",
                // From JDK 21
                "addFirst(Ljava/lang/Object;)V",
                "addLast(Ljava/lang/Object;)V",
                "removeFirst()Ljava/lang/Object;",
                "removeLast()Ljava/lang/Object;",
            ) +

            inJavaUtil(
                "Map",
                "getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                "forEach(Ljava/util/function/BiConsumer;)V",
                "replaceAll(Ljava/util/function/BiFunction;)V",
                "merge(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                "computeIfPresent(Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                "putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                "replace(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z",
                "replace(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                "computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;",
                "compute(Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;"
            )

    // for mutable collections
    private val MUTABLE_METHOD_SIGNATURES: Set<String> =
        inJavaUtil("MutableCollection", "removeIf(Ljava/util/function/Predicate;)Z") +

                inJavaUtil(
                    "MutableList",
                    "replaceAll(Ljava/util/function/UnaryOperator;)V",
                    "sort(Ljava/util/Comparator;)V",
                    "addFirst(Ljava/lang/Object;)V",
                    "addLast(Ljava/lang/Object;)V",
                    "removeFirst()Ljava/lang/Object;",
                    "removeLast()Ljava/lang/Object;",
                ) +

                inJavaUtil(
                    "MutableMap",
                    "computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;",
                    "computeIfPresent(Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                    "compute(Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                    "merge(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                    "putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    "remove(Ljava/lang/Object;Ljava/lang/Object;)Z",
                    "replaceAll(Ljava/util/function/BiFunction;)V",
                    "replace(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    "replace(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z"
                ) +
                // In K1, [kotlin.CharSequence.codePoints] can be [kotlin.String.codePoints]
                inJavaLang(
                    "String", "codePoints()Ljava/util/stream/IntStream;", "chars()Ljava/util/stream/IntStream;"
                )

    private val DEPRECATED_LIST_METHODS: Set<String> = inJavaUtil(
        "List",
        "getFirst()Ljava/lang/Object;",
        "getLast()Ljava/lang/Object;",
    )


    private val HIDDEN_METHOD_SIGNATURES: Set<String> =
        inJavaUtil(
            "List",
            "sort(Ljava/util/Comparator;)V",
            // From JDK 21
            "reversed()Ljava/util/List;",
        ) +

                inJavaLang(
                    "String",
                    "codePointAt(I)I",
                    "codePointBefore(I)I",
                    "codePointCount(II)I",
                    "compareToIgnoreCase(Ljava/lang/String;)I",
                    "concat(Ljava/lang/String;)Ljava/lang/String;",
                    "contains(Ljava/lang/CharSequence;)Z",
                    "contentEquals(Ljava/lang/CharSequence;)Z",
                    "contentEquals(Ljava/lang/StringBuffer;)Z",
                    "endsWith(Ljava/lang/String;)Z",
                    "equalsIgnoreCase(Ljava/lang/String;)Z",
                    "getBytes()[B",
                    "getBytes(II[BI)V",
                    "getBytes(Ljava/lang/String;)[B",
                    "getBytes(Ljava/nio/charset/Charset;)[B",
                    "getChars(II[CI)V",
                    "indexOf(I)I",
                    "indexOf(II)I",
                    "indexOf(Ljava/lang/String;)I",
                    "indexOf(Ljava/lang/String;I)I",
                    "intern()Ljava/lang/String;",
                    "isEmpty()Z",
                    "lastIndexOf(I)I",
                    "lastIndexOf(II)I",
                    "lastIndexOf(Ljava/lang/String;)I",
                    "lastIndexOf(Ljava/lang/String;I)I",
                    "matches(Ljava/lang/String;)Z",
                    "offsetByCodePoints(II)I",
                    "regionMatches(ILjava/lang/String;II)Z",
                    "regionMatches(ZILjava/lang/String;II)Z",
                    "replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                    "replace(CC)Ljava/lang/String;",
                    "replaceFirst(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                    "replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;",
                    "split(Ljava/lang/String;I)[Ljava/lang/String;",
                    "split(Ljava/lang/String;)[Ljava/lang/String;",
                    "startsWith(Ljava/lang/String;I)Z",
                    "startsWith(Ljava/lang/String;)Z",
                    "substring(II)Ljava/lang/String;",
                    "substring(I)Ljava/lang/String;",
                    "toCharArray()[C",
                    "toLowerCase()Ljava/lang/String;",
                    "toLowerCase(Ljava/util/Locale;)Ljava/lang/String;",
                    "toUpperCase()Ljava/lang/String;",
                    "toUpperCase(Ljava/util/Locale;)Ljava/lang/String;",
                    "trim()Ljava/lang/String;",
                    "isBlank()Z",
                    "lines()Ljava/util/stream/Stream;",
                    "repeat(I)Ljava/lang/String;"
                ) +

                inJavaLang("Double", "isInfinite()Z", "isNaN()Z") + inJavaLang("Float", "isInfinite()Z", "isNaN()Z") +

                inJavaLang("Enum", "getDeclaringClass()Ljava/lang/Class;", "finalize()V") + inJavaLang(
            "CharSequence",
            "isEmpty()Z"
        )


    private val deprecationAnnotationDRI = DRI("kotlin", "Deprecated")

    /**
     * KT-65438: In K1, such methods (e.g. translateEscapes, formatted... in java.lang.String) have the NOT_CONSIDERED status,
     * that means they are available and have a deprecation annotation.
     * In K2, they are unavailable for final classes (KT-65438) + [NOT_CONSIDER_METHOD_SIGNATURES]
     * @see NOT_CONSIDER_METHOD_SIGNATURES
     */
    private fun DFunction.hasDeprecated(): Boolean = this.extra[Annotations]?.directAnnotations?.any {
        it.value.any {
            it.dri == deprecationAnnotationDRI && it.params["message"] == StringValue("This member is not fully supported by Kotlin compiler, so it may be absent or have different signature in next major version")
        }
    } ?: false

    // user case: stdlib
    private fun Documentable.isOnlyInJVM() = sourceSets.all { it.analysisPlatform == Platform.jvm }

    private fun DFunction.shouldBeSuppressed() = if (this.hasDeprecated())
        true
    else {
        val fqn = this.dri.let { it.packageName + "." + it.classNames + "." + it.callable?.name }
        fqn in VISIBLE_METHOD_SIGNATURES || fqn in MUTABLE_METHOD_SIGNATURES || fqn in DEPRECATED_LIST_METHODS || fqn in HIDDEN_METHOD_SIGNATURES || fqn in NOT_CONSIDER_METHOD_SIGNATURES
    }

    override fun shouldBeSuppressed(d: Documentable): Boolean =
        if (d.isOnlyInJVM()) {
            when (d) {
                is DFunction -> d.shouldBeSuppressed()
                // Note that, if the Java class only has a setter, it isn't visible as a property in Kotlin because Kotlin doesn't support set-only properties
                is DProperty -> d.getter?.shouldBeSuppressed() ?: false
                else -> false
            }
        } else false
}