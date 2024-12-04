/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.DocExtensions.collection
import templates.DocExtensions.element
import templates.DocExtensions.mapResult
import templates.DocExtensions.prefixWithArticle
import templates.Family.*
import templates.SequenceClass.*

object Aggregates : TemplateGroupBase() {

    init {
        defaultBuilder {
            if (sequenceClassification.isEmpty()) {
                sequenceClassification(terminal)
            }
            specialFor(ArraysOfUnsigned) {
                sinceAtLeast("1.3")
                annotation("@ExperimentalUnsignedTypes")
            }
        }
    }

    val f_all = fn("all(predicate: (T) -> Boolean)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Returns `true` if all ${f.element.pluralize()} match the given [predicate].
            
            Note that if the ${f.collection} contains no ${f.element.pluralize()}, the function returns `true` 
            because there are no ${f.element.pluralize()} in it that _do not_ match the predicate.
            See a more detailed explanation of this logic concept in ["Vacuous truth"](https://en.wikipedia.org/wiki/Vacuous_truth) article. 
            """
        }
        sample("samples.collections.Collections.Aggregates.all")
        returns("Boolean")
        body {
            """
            ${when (f) {
                Iterables -> "if (this is Collection && isEmpty()) return true"
                Maps -> "if (isEmpty()) return true"
                else -> ""
            }}
            for (element in this) if (!predicate(element)) return false
            return true
            """
        }
    }

    val f_none_predicate = fn("none(predicate: (T) -> Boolean)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Returns `true` if no ${f.element.pluralize()} match the given [predicate].
            """
        }
        sample("samples.collections.Collections.Aggregates.noneWithPredicate")
        returns("Boolean")
        body {
            """
            ${when (f) {
                Iterables -> "if (this is Collection && isEmpty()) return true"
                Maps -> "if (isEmpty()) return true"
                else -> ""
            }}
            for (element in this) if (predicate(element)) return false
            return true
            """
        }
    }

    val f_none = fn("none()") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Returns `true` if the ${f.collection} has no ${f.element.pluralize()}.
            """
        }
        sample("samples.collections.Collections.Aggregates.none")
        returns("Boolean")
        body {
            "return !iterator().hasNext()"
        }
        specialFor(Iterables) {
            body {
                """
                if (this is Collection) return isEmpty()
                return !iterator().hasNext()
                """
            }
        }

        body(Maps, CharSequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            "return isEmpty()"
        }
    }

    val f_any_predicate = fn("any(predicate: (T) -> Boolean)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Returns `true` if at least one ${f.element} matches the given [predicate].
            """
        }
        sample("samples.collections.Collections.Aggregates.anyWithPredicate")
        returns("Boolean")
        body {
            """
            ${when (f) {
                Iterables -> "if (this is Collection && isEmpty()) return false"
                Maps -> "if (isEmpty()) return false"
                else -> ""
            }}
            for (element in this) if (predicate(element)) return true
            return false
            """
        }
    }

    val f_any = fn("any()") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        doc {
            """
            Returns `true` if ${f.collection} has at least one ${f.element}.
            """
        }
        sample("samples.collections.Collections.Aggregates.any")
        returns("Boolean")
        body {
            "return iterator().hasNext()"
        }
        body(Iterables) {
            """
            if (this is Collection) return !isEmpty()
            return iterator().hasNext()
            """
        }
        body(Maps, CharSequences, ArraysOfObjects, ArraysOfPrimitives) { "return !isEmpty()" }

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body { "return storage.any()" }
        }
    }


    val f_count_predicate = fn("count(predicate: (T) -> Boolean)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Returns the number of ${f.element.pluralize()} matching the given [predicate]." }
        returns("Int")
        body {
            fun checkOverflow(value: String) = if (f == Sequences || f == Iterables) "checkCountOverflow($value)" else value
            """
            ${when (f) {
                Iterables -> "if (this is Collection && isEmpty()) return 0"
                Maps -> "if (isEmpty()) return 0"
                else -> ""
            }}
            var count = 0
            for (element in this) if (predicate(element)) ${checkOverflow("++count")}
            return count
            """
        }
    }

    val f_count = fn("count()") {
        includeDefault()
        include(Collections, Maps, CharSequences)
    } builder {
        doc { "Returns the number of ${f.element.pluralize()} in this ${f.collection}." }
        returns("Int")
        body {
            fun checkOverflow(value: String) = if (f == Sequences || f == Iterables) "checkCountOverflow($value)" else value
            """
            ${if (f == Iterables) "if (this is Collection) return size" else ""}
            var count = 0
            for (element in this) ${checkOverflow("++count")}
            return count
            """
        }

        specialFor(CharSequences, Maps, Collections, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) { inlineOnly() }

        specialFor(CharSequences) {
            doc { "Returns the length of this char sequence." }
            body { "return length" }
        }
        body(Maps, Collections, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            "return size"
        }
    }

    val f_sumBy = fn("sumBy(selector: (T) -> Int)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        deprecate(Deprecation("Use sumOf instead.", "this.sumOf(selector)", DeprecationLevel.WARNING, warningSince = "1.5"))

        inline()
        doc { "Returns the sum of all values produced by [selector] function applied to each ${f.element} in the ${f.collection}." }
        returns("Int")
        body {
            """
            var sum: Int = 0
            for (element in this) {
                sum += selector(element)
            }
            return sum
            """
        }

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            signature("sumBy(selector: (T) -> UInt)")
            returns("UInt")
            body {
                """
                var sum: UInt = 0u
                for (element in this) {
                    sum += selector(element)
                }
                return sum
                """
            }
        }
    }

    fun f_sumOf() = listOf("Int", "Long", "UInt", "ULong", "Double", "java.math.BigInteger", "java.math.BigDecimal").map { selectorType ->
        fn("sumOf(selector: (T) -> $selectorType)") {
            includeDefault()
            include(CharSequences, ArraysOfUnsigned)
            if (selectorType.startsWith("java")) platforms(Platform.JVM)
        } builder {
            inlineOnly()
            since("1.4")
            val typeShortName = when {
                selectorType.startsWith("java") -> selectorType.substringAfterLast('.')
                else -> selectorType
            }
            annotation("@OptIn(kotlin.experimental.ExperimentalTypeInference::class)")
            annotation("@OverloadResolutionByLambdaReturnType")
            specialFor(ArraysOfUnsigned) {
                annotation("""@Suppress("INAPPLICABLE_JVM_NAME")""")
            }
            annotation("""@kotlin.jvm.JvmName("sumOf$typeShortName")""") // should not be needed if inline return type is mangled
            if (selectorType.startsWith("U")) {
                since("1.5")
                wasExperimental("ExperimentalUnsignedTypes")
            }

            doc { "Returns the sum of all values produced by [selector] function applied to each ${f.element} in the ${f.collection}." }
            returns(selectorType)
            body {
                """
                var sum: $selectorType = 0.to$typeShortName()
                for (element in this) {
                    sum += selector(element)
                }
                return sum
                """
            }
        }
    }

    val f_sumByDouble = fn("sumByDouble(selector: (T) -> Double)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        deprecate(Deprecation("Use sumOf instead.", "this.sumOf(selector)", DeprecationLevel.WARNING, warningSince = "1.5"))

        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Returns the sum of all values produced by [selector] function applied to each ${f.element} in the ${f.collection}." }
        returns("Double")
        body {
            """
            var sum: Double = 0.0
            for (element in this) {
                sum += selector(element)
            }
            return sum
            """
        }
    }


    val f_minMax = sequence {
        val genericSpecializations = PrimitiveType.floatingPointPrimitives + setOf(null)

        fun def(op: String, nullable: Boolean, legacy: Boolean = false, orNull: String = "OrNull".ifOrEmpty(nullable)) =
            fn("$op$orNull()") {
                if (legacy) platforms(Platform.JVM)
                include(Iterables, genericSpecializations)
                include(Sequences, genericSpecializations)
                include(ArraysOfObjects, genericSpecializations)
                include(ArraysOfPrimitives, PrimitiveType.defaultPrimitives - PrimitiveType.Boolean)
                include(ArraysOfUnsigned)
                include(CharSequences)
            } builder {
                typeParam("T : Comparable<T>")
                returns("T" + "?".ifOrEmpty(nullable))

                val isFloat = primitive?.isFloatingPoint() == true
                val isUnsigned = family == ArraysOfUnsigned

                if (!nullable || legacy) suppress("CONFLICTING_OVERLOADS")
                if (legacy) {
                    deprecate(Deprecation("Use ${op}OrNull instead.", "this.${op}OrNull()", warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6"))
                    val isGeneric = f in listOf(Iterables, Sequences, ArraysOfObjects)
                    if (isFloat && isGeneric) {
                        since("1.1")
                    }

                    body { "return ${op}OrNull()" }

                    return@builder
                }

                val doOnEmpty = if (nullable) "return null" else "throw NoSuchElementException()"

                since("1.4")
                if (!nullable) since("1.7")

                doc {
                    "Returns the ${if (op == "max") "largest" else "smallest"} ${f.element}${" or `null` if there are no ${f.element.pluralize()}".ifOrEmpty(nullable)}." +
                    if (isFloat) "\n\n" + "If any of ${f.element.pluralize()} is `NaN` returns `NaN`." else ""
                }
                if (!nullable) {
                    throws("NoSuchElementException", "if the ${f.collection} is empty.")
                    annotation("@kotlin.jvm.JvmName(\"${op}OrThrow${"-U".ifOrEmpty(isUnsigned)}\")")
                }

                val acc = op
                val cmpBlock = if (isFloat)
                    """$acc = ${op}Of($acc, e)"""
                else
                    """if ($acc ${if (op == "max") "<" else ">"} e) $acc = e"""
                body {
                    """
                    val iterator = iterator()
                    if (!iterator.hasNext()) $doOnEmpty
                    var $acc = iterator.next()
                    while (iterator.hasNext()) {
                        val e = iterator.next()
                        $cmpBlock
                    }
                    return $acc
                    """
                }
                body(ArraysOfObjects, ArraysOfPrimitives, CharSequences, ArraysOfUnsigned) {
                    """
                    if (isEmpty()) $doOnEmpty
                    var $acc = this[0]
                    for (i in 1..lastIndex) {
                        val e = this[i]
                        $cmpBlock
                    }
                    return $acc
                    """
                }
            }

        for (op in listOf("min", "max")) {
            for (nullable in listOf(false, true))
                yield(def(op, nullable))
            yield(def(op, nullable = true, legacy = true, orNull = ""))
        }
    }

    val f_minMaxBy = sequence {
        fun def(op: String, nullable: Boolean, legacy: Boolean = false, orNull: String = "OrNull".ifOrEmpty(nullable)) =
            fn("$op$orNull(selector: (T) -> R)") {
                if (legacy) platforms(Platform.JVM)
                includeDefault()
                include(Maps, CharSequences, ArraysOfUnsigned)
            } builder {
                inline()
                specialFor(ArraysOfUnsigned) { inlineOnly() }
                specialFor(Maps) { if (op == "maxBy" || !legacy) inlineOnly() }
                typeParam("R : Comparable<R>")
                returns("T" + "?".ifOrEmpty(nullable))
                val isUnsigned = family == ArraysOfUnsigned

                if (!nullable || legacy) suppress("CONFLICTING_OVERLOADS")
                if (legacy) {
                    deprecate(Deprecation("Use ${op}OrNull instead.", "this.${op}OrNull(selector)", warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6"))
                    body { "return ${op}OrNull(selector)" }
                    return@builder
                }

                val doOnEmpty = if (nullable) "return null" else "throw NoSuchElementException()"

                since("1.4")
                if (!nullable) since("1.7")

                doc { "Returns the first ${f.element} yielding the ${if (op == "maxBy") "largest" else "smallest"} value of the given function${" or `null` if there are no ${f.element.pluralize()}".ifOrEmpty(nullable)}." }
                sample("samples.collections.Collections.Aggregates.$op$orNull")

                if (!nullable) {
                    throws("NoSuchElementException", "if the ${f.collection} is empty.")
                    annotation("@kotlin.jvm.JvmName(\"${op}OrThrow${"-U".ifOrEmpty(isUnsigned)}\")")
                }

                val (elem, value, cmp) = if (op == "minBy") Triple("minElem", "minValue", ">") else Triple("maxElem", "maxValue", "<")
                body {
                    """
                    val iterator = iterator()
                    if (!iterator.hasNext()) $doOnEmpty
        
                    var $elem = iterator.next()
                    if (!iterator.hasNext()) return $elem
                    var $value = selector($elem)
                    do {
                        val e = iterator.next()
                        val v = selector(e)
                        if ($value $cmp v) {
                            $elem = e
                            $value = v
                        }
                    } while (iterator.hasNext())
                    return $elem
                    """
                }
                body(CharSequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
                    """
                    if (isEmpty()) $doOnEmpty
        
                    var $elem = this[0]
                    val lastIndex = this.lastIndex
                    if (lastIndex == 0) return $elem
                    var $value = selector($elem)
                    for (i in 1..lastIndex) {
                        val e = this[i]
                        val v = selector(e)
                        if ($value $cmp v) {
                            $elem = e
                            $value = v
                        }
                    }
                    return $elem
                    """
                }
                body(Maps) { "return entries.$op$orNull(selector)" }
            }

        for (op in listOf("minBy", "maxBy")) {
            for (nullable in listOf(false, true))
                yield(def(op, nullable))
            yield(def(op, nullable = true, legacy = true, orNull = ""))
        }
    }

    val f_minMaxWith = sequence {
        fun def(op: String, nullable: Boolean, legacy: Boolean = false, orNull: String = "OrNull".ifOrEmpty(nullable)) =
            fn("$op$orNull(comparator: Comparator<in T>)") {
                if (legacy) platforms(Platform.JVM)
                includeDefault()
                include(Maps, CharSequences, ArraysOfUnsigned)
            } builder {
                specialFor(Maps) { if (op == "maxWith" || !legacy) inlineOnly() }
                returns("T" + "?".ifOrEmpty(nullable))
                val isUnsigned = family == ArraysOfUnsigned

                if (!nullable || legacy) suppress("CONFLICTING_OVERLOADS")
                if (legacy) {
                    deprecate(Deprecation("Use ${op}OrNull instead.", "this.${op}OrNull(comparator)", warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6"))
                    body { "return ${op}OrNull(comparator)" }
                    return@builder
                }

                val doOnEmpty = if (nullable) "return null" else "throw NoSuchElementException()"

                since("1.4")
                if (!nullable) since("1.7")

                doc { "Returns the first ${f.element} having the ${if (op == "maxWith") "largest" else "smallest"} value according to the provided [comparator]${" or `null` if there are no ${f.element.pluralize()}".ifOrEmpty(nullable)}." }
                if (!nullable) {
                    throws("NoSuchElementException", "if the ${f.collection} is empty.")
                    annotation("@kotlin.jvm.JvmName(\"${op}OrThrow${"-U".ifOrEmpty(isUnsigned)}\")")
                }

                val (acc, cmp) = if (op == "minWith") Pair("min", ">") else Pair("max", "<")
                body {
                    """
                    val iterator = iterator()
                    if (!iterator.hasNext()) $doOnEmpty
        
                    var $acc = iterator.next()
                    while (iterator.hasNext()) {
                        val e = iterator.next()
                        if (comparator.compare($acc, e) $cmp 0) $acc = e
                    }
                    return $acc
                    """
                }
                body(CharSequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
                    """
                    if (isEmpty()) $doOnEmpty
                    var $acc = this[0]
                    for (i in 1..lastIndex) {
                        val e = this[i]
                        if (comparator.compare($acc, e) $cmp 0) $acc = e
                    }
                    return $acc
                    """
                }
                body(Maps) { "return entries.$op$orNull(comparator)" }
            }

        for (op in listOf("minWith", "maxWith")) {
            for (nullable in listOf(false, true))
                yield(def(op, nullable))
            yield(def(op, nullable = true, legacy = true, orNull = ""))
        }
    }

    fun f_minMaxOf() = sequence {
        fun def(op: String, selectorType: String, nullable: Boolean, orNull: String = "OrNull".ifOrEmpty(nullable)) =
            fn("${op}Of$orNull(selector: (T) -> $selectorType)") {
                includeDefault()
                include(Maps, CharSequences, ArraysOfUnsigned)
            } builder {
                inlineOnly()
                since("1.4")
                annotation("@OptIn(kotlin.experimental.ExperimentalTypeInference::class)")
                annotation("@OverloadResolutionByLambdaReturnType")

                val isFloat = selectorType != "R"

                doc {
                    """
                    Returns the ${if (op == "max") "largest" else "smallest"} value among all values produced by [selector] function 
                    applied to each ${f.element} in the ${f.collection}${" or `null` if there are no ${f.element.pluralize()}".ifOrEmpty(nullable)}.
                    """ +
                    """
                    If any of values produced by [selector] function is `NaN`, the returned result is `NaN`.
                    """.ifOrEmpty(isFloat)
                }
                if (!nullable) {
                    throws("NoSuchElementException", "if the ${f.collection} is empty.")
                }

                if (!isFloat) typeParam("R : Comparable<R>")
                returns(selectorType + "?".ifOrEmpty(nullable))
                val doOnEmpty = if (nullable) "return null" else "throw NoSuchElementException()"
                val acc = op + "Value"
                val cmpBlock = if (isFloat)
                    """$acc = ${op}Of($acc, v)"""
                else
                    """if ($acc ${if (op == "max") "<" else ">"} v) {
                            $acc = v
                        }"""
                body {
                    """
                    val iterator = iterator()
                    if (!iterator.hasNext()) $doOnEmpty
                    var $acc = selector(iterator.next())
                    while (iterator.hasNext()) {
                        val v = selector(iterator.next())
                        $cmpBlock
                    }
                    return $acc
                    """
                }
                body(CharSequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
                    """
                    if (isEmpty()) $doOnEmpty
        
                    var $acc = selector(this[0])
                    for (i in 1..lastIndex) {
                        val v = selector(this[i])
                        $cmpBlock
                    }
                    return $acc
                    """
                }
                specialFor(Maps) {
                    inlineOnly()
                    body { "return entries.${op}Of$orNull(selector)" }
                }
            }


        for (op in listOf("min", "max"))
            for (selectorType in listOf("R", "Float", "Double"))
                for (nullable in listOf(false, true))
                    yield(def(op, selectorType, nullable))
    }

    fun f_minMaxOfWith() = sequence {
        val selectorType = "R"
        fun def(op: String, nullable: Boolean, orNull: String = "OrNull".ifOrEmpty(nullable)) =
            fn("${op}OfWith$orNull(comparator: Comparator<in R>, selector: (T) -> $selectorType)") {
                includeDefault()
                include(Maps, CharSequences, ArraysOfUnsigned)
            } builder {
                inlineOnly()
                since("1.4")
                annotation("@OptIn(kotlin.experimental.ExperimentalTypeInference::class)")
                annotation("@OverloadResolutionByLambdaReturnType")

                doc {
                    """
                    Returns the ${if (op == "max") "largest" else "smallest"} value according to the provided [comparator] 
                    among all values produced by [selector] function applied to each ${f.element} in the ${f.collection}${" or `null` if there are no ${f.element.pluralize()}".ifOrEmpty(nullable)}.
                    """ +
                    """
                    @throws NoSuchElementException if the ${f.collection} is empty.
                    """.ifOrEmpty(!nullable)
                }

                typeParam(selectorType)
                returns(selectorType + "?".ifOrEmpty(nullable))
                val doOnEmpty = if (nullable) "return null" else "throw NoSuchElementException()"
                val acc = op + "Value"
                val cmp = if (op == "max") "<" else ">"
                body {
                    """
                    val iterator = iterator()
                    if (!iterator.hasNext()) $doOnEmpty
                    var $acc = selector(iterator.next())
                    while (iterator.hasNext()) {
                        val v = selector(iterator.next())
                        if (comparator.compare($acc, v) $cmp 0) {
                            $acc = v
                        }
                    }
                    return $acc
                    """
                }
                body(CharSequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
                    """
                    if (isEmpty()) $doOnEmpty
        
                    var $acc = selector(this[0])
                    for (i in 1..lastIndex) {
                        val v = selector(this[i])
                        if (comparator.compare($acc, v) $cmp 0) {
                            $acc = v
                        }
                    }
                    return $acc
                    """
                }
                specialFor(Maps) {
                    body { "return entries.${op}OfWith$orNull(comparator, selector)" }
                }
            }

        for (op in listOf("min", "max"))
            for (nullable in listOf(false, true))
                yield(def(op, nullable))
    }


    val f_foldIndexed = fn("foldIndexed(initial: R, operation: (index: Int, acc: R, T) -> R)") {
        includeDefault()
        include(CharSequences)
        include(ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Accumulates value starting with [initial] value and applying [operation] from left to right
            to current accumulator value and each ${f.element} with its index in the original ${f.collection}.
            
            Returns the specified [initial] value if the ${f.collection} is empty.
            
            @param [operation] function that takes the index of ${f.element.prefixWithArticle()}, current accumulator value
            and the ${f.element} itself, and calculates the next accumulator value.
            """
        }
        typeParam("R")
        returns("R")
        body {
            fun checkOverflow(value: String) = if (f == Sequences || f == Iterables) "checkIndexOverflow($value)" else value
            """
            var index = 0
            var accumulator = initial
            for (element in this) accumulator = operation(${checkOverflow("index++")}, accumulator, element)
            return accumulator
            """
        }
    }

    val f_foldRightIndexed = fn("foldRightIndexed(initial: R, operation: (index: Int, T, acc: R) -> R)") {
        include(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Accumulates value starting with [initial] value and applying [operation] from right to left
            to each ${f.element} with its index in the original ${f.collection} and current accumulator value.
            
            Returns the specified [initial] value if the ${f.collection} is empty.
            
            @param [operation] function that takes the index of ${f.element.prefixWithArticle()}, the ${f.element} itself
            and current accumulator value, and calculates the next accumulator value.
            """
        }
        typeParam("R")
        returns("R")
        body {
            """
            var index = lastIndex
            var accumulator = initial
            while (index >= 0) {
                accumulator = operation(index, get(index), accumulator)
                --index
            }
            return accumulator
            """
        }
        body(Lists) {
            """
            var accumulator = initial
            if (!isEmpty()) {
                val iterator = listIterator(size)
                while (iterator.hasPrevious()) {
                    val index = iterator.previousIndex()
                    accumulator = operation(index, iterator.previous(), accumulator)
                }
            }
            return accumulator
            """
        }
    }

    val f_fold = fn("fold(initial: R, operation: (acc: R, T) -> R)") {
        includeDefault()
        include(CharSequences)
        include(ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Accumulates value starting with [initial] value and applying [operation] from left to right 
            to current accumulator value and each ${f.element}.

            Returns the specified [initial] value if the ${f.collection} is empty.

            @param [operation] function that takes current accumulator value and ${f.element.prefixWithArticle()}, and calculates the next accumulator value.
            """
        }
        typeParam("R")
        returns("R")
        body {
            """
            var accumulator = initial
            for (element in this) accumulator = operation(accumulator, element)
            return accumulator
            """
        }
    }

    val f_foldRight = fn("foldRight(initial: R, operation: (T, acc: R) -> R)") {
        include(CharSequences, Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Accumulates value starting with [initial] value and applying [operation] from right to left 
            to each ${f.element} and current accumulator value.

            Returns the specified [initial] value if the ${f.collection} is empty.

            @param [operation] function that takes ${f.element.prefixWithArticle()} and current accumulator value, and calculates the next accumulator value.
            """
        }
        typeParam("R")
        returns("R")
        body {
            """
            var index = lastIndex
            var accumulator = initial
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }
            return accumulator
            """
        }
        body(Lists) {
            """
            var accumulator = initial
            if (!isEmpty()) {
                val iterator = listIterator(size)
                while (iterator.hasPrevious()) {
                    accumulator = operation(iterator.previous(), accumulator)
                }
            }
            return accumulator
            """
        }
    }

    private fun MemberBuilder.reduceDoc(fName: String): String {
        fun summaryDoc(isLeftToRight: Boolean, isIndexed: Boolean): String {
            val acc = "current accumulator value"
            val element = if (isIndexed) "each ${f.element} with its index in the original ${f.collection}" else "each ${f.element}"
            val start = if (isLeftToRight) "first" else "last"
            val iteration = if (isLeftToRight) "left to right\nto $acc and $element" else "right to left\nto $element and $acc"
            return """
                Accumulates value starting with the $start ${f.element} and applying [operation] from $iteration."""
        }

        fun paramDoc(isLeftToRight: Boolean, isIndexed: Boolean): String {
            val acc = "current accumulator value"
            val element = if (isIndexed) "the ${f.element} itself" else f.element.prefixWithArticle()
            val index = if (isIndexed) "the index of ${f.element.prefixWithArticle()}, " else ""
            return """
                @param [operation] function that takes $index${if (isLeftToRight) "$acc and $element" else "$element and $acc"}, 
                and calculates the next accumulator value."""
        }

        fun emptyNote(isThrowing: Boolean): String = if (isThrowing) """
            Throws an exception if this ${f.collection} is empty. If the ${f.collection} can be empty in an expected way, 
            please use [${fName}OrNull] instead. It returns `null` when its receiver is empty."""
        else """
            Returns `null` if the ${f.collection} is empty."""

        val isLeftToRight = fName.contains("Right").not()
        val isIndexed = fName.contains("Indexed")
        val isThrowing = fName.contains("OrNull").not()
        return """
            ${summaryDoc(isLeftToRight, isIndexed)}
            ${emptyNote(isThrowing)}
            ${paramDoc(isLeftToRight, isIndexed)}"""
    }

    val f_reduceIndexed = fn("reduceIndexed(operation: (index: Int, acc: T, T) -> T)") {
        include(ArraysOfPrimitives, ArraysOfUnsigned, CharSequences)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { reduceDoc("reduceIndexed") }
        sample("samples.collections.Collections.Aggregates.reduce")
        returns("T")
        body {
            """
            if (isEmpty())
                throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(index, accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceIndexedSuper = fn("reduceIndexed(operation: (index: Int, acc: S, T) -> S)") {
        include(ArraysOfObjects, Iterables, Sequences)
    } builder {
        inline()

        doc { reduceDoc("reduceIndexed") }
        typeParam("S")
        typeParam("T : S")
        sample("samples.collections.Collections.Aggregates.reduce")
        returns("S")
        body {
            fun checkOverflow(value: String) = if (f == Sequences || f == Iterables) "checkIndexOverflow($value)" else value
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var index = 1
            var accumulator: S = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(${checkOverflow("index++")}, accumulator, iterator.next())
            }
            return accumulator
            """
        }
        body(ArraysOfObjects) {
            """
            if (isEmpty())
                throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator: S = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(index, accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceIndexedOrNull = fn("reduceIndexedOrNull(operation: (index: Int, acc: T, T) -> T)") {
        include(ArraysOfPrimitives, ArraysOfUnsigned, CharSequences)
    } builder {
        since("1.4")
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { reduceDoc("reduceIndexedOrNull") }
        sample("samples.collections.Collections.Aggregates.reduceOrNull")
        returns("T?")
        body {
            """
            if (isEmpty())
                return null

            var accumulator = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(index, accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceIndexedOrNullSuper = fn("reduceIndexedOrNull(operation: (index: Int, acc: S, T) -> S)") {
        include(ArraysOfObjects, Iterables, Sequences)
    } builder {
        since("1.4")
        inline()

        doc { reduceDoc("reduceIndexedOrNull") }
        typeParam("S")
        typeParam("T : S")
        sample("samples.collections.Collections.Aggregates.reduceOrNull")
        returns("S?")
        body {
            fun checkOverflow(value: String) = if (f == Sequences || f == Iterables) "checkIndexOverflow($value)" else value
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) return null

            var index = 1
            var accumulator: S = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(${checkOverflow("index++")}, accumulator, iterator.next())
            }
            return accumulator
            """
        }
        body(ArraysOfObjects) {
            """
            if (isEmpty())
                return null

            var accumulator: S = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(index, accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceRightIndexed = fn("reduceRightIndexed(operation: (index: Int, T, acc: T) -> T)") {
        include(CharSequences, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { reduceDoc("reduceRightIndexed") }
        sample("samples.collections.Collections.Aggregates.reduceRight")
        returns("T")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator = get(index--)
            while (index >= 0) {
                accumulator = operation(index, get(index), accumulator)
                --index
            }

            return accumulator
            """
        }
    }

    val f_reduceRightIndexedSuper = fn("reduceRightIndexed(operation: (index: Int, T, acc: S) -> S)") {
        include(Lists, ArraysOfObjects)
    } builder {
        inline()

        doc { reduceDoc("reduceRightIndexed") }
        sample("samples.collections.Collections.Aggregates.reduceRight")
        typeParam("S")
        typeParam("T : S")
        returns("S")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator: S = get(index--)
            while (index >= 0) {
                accumulator = operation(index, get(index), accumulator)
                --index
            }

            return accumulator
            """
        }
        body(Lists) {
            """
            val iterator = listIterator(size)
            if (!iterator.hasPrevious())
                throw UnsupportedOperationException("Empty list can't be reduced.")

            var accumulator: S = iterator.previous()
            while (iterator.hasPrevious()) {
                val index = iterator.previousIndex()
                accumulator = operation(index, iterator.previous(), accumulator)
            }

            return accumulator
            """
        }
    }

    val f_reduceRightIndexedOrNull = fn("reduceRightIndexedOrNull(operation: (index: Int, T, acc: T) -> T)") {
        include(CharSequences, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.4")
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { reduceDoc("reduceRightIndexedOrNull") }
        sample("samples.collections.Collections.Aggregates.reduceRightOrNull")
        returns("T?")
        body {
            """
            var index = lastIndex
            if (index < 0) return null

            var accumulator = get(index--)
            while (index >= 0) {
                accumulator = operation(index, get(index), accumulator)
                --index
            }

            return accumulator
            """
        }
    }

    val f_reduceRightIndexedOrNullSuper = fn("reduceRightIndexedOrNull(operation: (index: Int, T, acc: S) -> S)") {
        include(Lists, ArraysOfObjects)
    } builder {
        since("1.4")
        inline()

        doc { reduceDoc("reduceRightIndexedOrNull") }
        sample("samples.collections.Collections.Aggregates.reduceRightOrNull")
        typeParam("S")
        typeParam("T : S")
        returns("S?")
        body {
            """
            var index = lastIndex
            if (index < 0) return null

            var accumulator: S = get(index--)
            while (index >= 0) {
                accumulator = operation(index, get(index), accumulator)
                --index
            }

            return accumulator
            """
        }
        body(Lists) {
            """
            val iterator = listIterator(size)
            if (!iterator.hasPrevious())
                return null

            var accumulator: S = iterator.previous()
            while (iterator.hasPrevious()) {
                val index = iterator.previousIndex()
                accumulator = operation(index, iterator.previous(), accumulator)
            }

            return accumulator
            """
        }
    }

    val f_reduce = fn("reduce(operation: (acc: T, T) -> T)") {
        include(ArraysOfPrimitives, ArraysOfUnsigned, CharSequences)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { reduceDoc("reduce") }
        sample("samples.collections.Collections.Aggregates.reduce")
        returns("T")
        body {
            """
            if (isEmpty())
                throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceSuper = fn("reduce(operation: (acc: S, T) -> S)") {
        include(ArraysOfObjects, Iterables, Sequences)
    } builder {
        inline()

        doc { reduceDoc("reduce") }
        sample("samples.collections.Collections.Aggregates.reduce")
        typeParam("S")
        typeParam("T : S")
        returns("S")
        body {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator: S = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(accumulator, iterator.next())
            }
            return accumulator
            """
        }
        body(ArraysOfObjects) {
            """
            if (isEmpty())
                throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator: S = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceOrNull = fn("reduceOrNull(operation: (acc: T, T) -> T)") {
        include(ArraysOfPrimitives, ArraysOfUnsigned, CharSequences)
    } builder {
        since("1.4")
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { reduceDoc("reduceOrNull") }
        sample("samples.collections.Collections.Aggregates.reduceOrNull")
        returns("T?")
        body {
            """
            if (isEmpty())
                return null

            var accumulator = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceOrNullSuper = fn("reduceOrNull(operation: (acc: S, T) -> S)") {
        include(ArraysOfObjects, Iterables, Sequences)
    } builder {
        since("1.4")
        inline()

        doc { reduceDoc("reduceOrNull") }
        sample("samples.collections.Collections.Aggregates.reduceOrNull")
        typeParam("S")
        typeParam("T : S")
        returns("S?")
        body {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) return null

            var accumulator: S = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(accumulator, iterator.next())
            }
            return accumulator
            """
        }
        body(ArraysOfObjects) {
            """
            if (isEmpty())
                return null

            var accumulator: S = this[0]
            for (index in 1..lastIndex) {
                accumulator = operation(accumulator, this[index])
            }
            return accumulator
            """
        }
    }

    val f_reduceRight = fn("reduceRight(operation: (T, acc: T) -> T)") {
        include(CharSequences, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { reduceDoc("reduceRight") }
        sample("samples.collections.Collections.Aggregates.reduceRight")
        returns("T")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator = get(index--)
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }

            return accumulator
            """
        }
    }

    val f_reduceRightSuper = fn("reduceRight(operation: (T, acc: S) -> S)") {
        include(Lists, ArraysOfObjects)
    } builder {
        inline()
        doc { reduceDoc("reduceRight") }
        sample("samples.collections.Collections.Aggregates.reduceRight")
        typeParam("S")
        typeParam("T : S")
        returns("S")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty ${f.doc.collection} can't be reduced.")

            var accumulator: S = get(index--)
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }

            return accumulator
            """
        }
        body(Lists) {
            """
            val iterator = listIterator(size)
            if (!iterator.hasPrevious())
                throw UnsupportedOperationException("Empty list can't be reduced.")

            var accumulator: S = iterator.previous()
            while (iterator.hasPrevious()) {
                accumulator = operation(iterator.previous(), accumulator)
            }

            return accumulator
            """
        }
    }

    val f_reduceRightOrNull = fn("reduceRightOrNull(operation: (T, acc: T) -> T)") {
        include(CharSequences, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.4")
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { reduceDoc("reduceRightOrNull") }
        sample("samples.collections.Collections.Aggregates.reduceRightOrNull")
        returns("T?")
        body {
            """
            var index = lastIndex
            if (index < 0) return null

            var accumulator = get(index--)
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }

            return accumulator
            """
        }
    }

    val f_reduceRightOrNullSuper = fn("reduceRightOrNull(operation: (T, acc: S) -> S)") {
        include(Lists, ArraysOfObjects)
    } builder {
        since("1.4")
        inline()
        doc { reduceDoc("reduceRightOrNull") }
        sample("samples.collections.Collections.Aggregates.reduceRightOrNull")
        typeParam("S")
        typeParam("T : S")
        returns("S?")
        body {
            """
            var index = lastIndex
            if (index < 0) return null

            var accumulator: S = get(index--)
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }

            return accumulator
            """
        }
        body(Lists) {
            """
            val iterator = listIterator(size)
            if (!iterator.hasPrevious())
                return null

            var accumulator: S = iterator.previous()
            while (iterator.hasPrevious()) {
                accumulator = operation(iterator.previous(), accumulator)
            }

            return accumulator
            """
        }
    }

    private fun scanAccMutationNote(hasInitial: Boolean, f: Family): String {
        if (!hasInitial && f.isPrimitiveSpecialization) return ""

        val initialValueRequirement = if (hasInitial && f == Sequences)
            """The [initial] value should also be immutable (or should not be mutated)
            as it may be passed to [operation] function later because of sequence's lazy nature.
            """ else
            ""
        return """
        Note that `acc` value passed to [operation] function should not be mutated;
        otherwise it would affect the previous value in resulting ${f.mapResult}.
        $initialValueRequirement"""
    }

    val f_runningFold = fn("runningFold(initial: R, operation: (acc: R, T) -> R)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        since("1.4")

        specialFor(Iterables, ArraysOfObjects, CharSequences) { inline() }
        specialFor(ArraysOfPrimitives, ArraysOfUnsigned) { inlineOnly() }

        typeParam("R")

        returns("List<R>")
        specialFor(Sequences) { returns("Sequence<R>") }

        doc {
            """
            Returns a ${f.mapResult} containing successive accumulation values generated by applying [operation] from left to right 
            to each ${f.element} and current accumulator value that starts with [initial] value.
            ${scanAccMutationNote(true, f)}
            @param [operation] function that takes current accumulator value and ${f.element.prefixWithArticle()}, and calculates the next accumulator value.
            """
        }
        sample("samples.collections.Collections.Aggregates.runningFold")
        sequenceClassification(intermediate, stateless)

        body(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, CharSequences) {
            """
            if (isEmpty()) return listOf(initial)

            val result = ArrayList<R>(${f.code.size} + 1).apply { add(initial) }
            var accumulator = initial
            for (element in this) {
                accumulator = operation(accumulator, element)
                result.add(accumulator)
            }
            return result
            """
        }
        body(Iterables) {
            """
            val estimatedSize = collectionSizeOrDefault(9)
            if (estimatedSize == 0) return listOf(initial)
            
            val result = ArrayList<R>(estimatedSize + 1).apply { add(initial) }
            var accumulator = initial
            for (element in this) {
                accumulator = operation(accumulator, element)
                result.add(accumulator)
            }
            return result
            """
        }
        body(Sequences) {
            """
            return sequence {
                yield(initial)
                var accumulator = initial
                for (element in this@runningFold) {
                    accumulator = operation(accumulator, element)
                    yield(accumulator)
                }
            }
            """
        }
    }

    val f_scan = fn("scan(initial: R, operation: (acc: R, T) -> R)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        since("1.4")

        specialFor(Iterables, ArraysOfObjects, CharSequences) { inline() }
        specialFor(ArraysOfPrimitives, ArraysOfUnsigned) { inlineOnly() }

        typeParam("R")

        returns("List<R>")
        specialFor(Sequences) { returns("Sequence<R>") }

        doc {
            """
            Returns a ${f.mapResult} containing successive accumulation values generated by applying [operation] from left to right 
            to each ${f.element} and current accumulator value that starts with [initial] value.
            ${scanAccMutationNote(true, f)}
            @param [operation] function that takes current accumulator value and ${f.element.prefixWithArticle()}, and calculates the next accumulator value.
            """
        }
        sample("samples.collections.Collections.Aggregates.scan")
        sequenceClassification(intermediate, stateless)

        body { "return runningFold(initial, operation)" }
    }

    val f_runningFoldIndexed = fn("runningFoldIndexed(initial: R, operation: (index: Int, acc: R, T) -> R)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        since("1.4")

        specialFor(Iterables, ArraysOfObjects, CharSequences) { inline() }
        specialFor(ArraysOfPrimitives, ArraysOfUnsigned) { inlineOnly() }

        typeParam("R")

        returns("List<R>")
        specialFor(Sequences) { returns("Sequence<R>") }

        doc {
            """
            Returns a ${f.mapResult} containing successive accumulation values generated by applying [operation] from left to right
            to each ${f.element}, its index in the original ${f.collection} and current accumulator value that starts with [initial] value.
            ${scanAccMutationNote(true, f)}
            @param [operation] function that takes the index of ${f.element.prefixWithArticle()}, current accumulator value
            and the ${f.element} itself, and calculates the next accumulator value.
            """
        }
        sample("samples.collections.Collections.Aggregates.runningFold")
        sequenceClassification(intermediate, stateless)

        body(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned, CharSequences) {
            """
            if (isEmpty()) return listOf(initial)

            val result = ArrayList<R>(${f.code.size} + 1).apply { add(initial) }
            var accumulator = initial
            for (index in indices) {
                accumulator = operation(index, accumulator, this[index])
                result.add(accumulator)
            }
            return result
            """
        }
        body(Iterables) {
            """
            val estimatedSize = collectionSizeOrDefault(9)
            if (estimatedSize == 0) return listOf(initial)
            
            val result = ArrayList<R>(estimatedSize + 1).apply { add(initial) }
            var index = 0
            var accumulator = initial
            for (element in this) {
                accumulator = operation(index++, accumulator, element)
                result.add(accumulator)
            }
            return result
            """
        }
        body(Sequences) {
            """
            return sequence {
                yield(initial)
                var index = 0
                var accumulator = initial
                for (element in this@runningFoldIndexed) {
                    accumulator = operation(checkIndexOverflow(index++), accumulator, element)
                    yield(accumulator)
                }
            }
            """
        }
    }

    val f_scanIndexed = fn("scanIndexed(initial: R, operation: (index: Int, acc: R, T) -> R)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        since("1.4")

        specialFor(Iterables, ArraysOfObjects, CharSequences) { inline() }
        specialFor(ArraysOfPrimitives, ArraysOfUnsigned) { inlineOnly() }

        typeParam("R")

        returns("List<R>")
        specialFor(Sequences) { returns("Sequence<R>") }

        doc {
            """
            Returns a ${f.mapResult} containing successive accumulation values generated by applying [operation] from left to right
            to each ${f.element}, its index in the original ${f.collection} and current accumulator value that starts with [initial] value.
            ${scanAccMutationNote(true, f)}
            @param [operation] function that takes the index of ${f.element.prefixWithArticle()}, current accumulator value
            and the ${f.element} itself, and calculates the next accumulator value.
            """
        }
        sample("samples.collections.Collections.Aggregates.scan")
        sequenceClassification(intermediate, stateless)

        body { "return runningFoldIndexed(initial, operation)" }
    }

    val f_runningReduce = fn("runningReduce(operation: (acc: T, T) -> T)") {
        include(ArraysOfPrimitives, ArraysOfUnsigned, CharSequences)
    } builder {
        since("1.4")

        specialFor(CharSequences) { inline() }
        specialFor(ArraysOfPrimitives, ArraysOfUnsigned) { inlineOnly() }

        returns("List<T>")

        doc {
            """
            Returns a list containing successive accumulation values generated by applying [operation] from left to right 
            to each ${f.element} and current accumulator value that starts with the first ${f.element} of this ${f.collection}.
            ${scanAccMutationNote(false, f)}
            @param [operation] function that takes current accumulator value and ${f.element.prefixWithArticle()}, and calculates the next accumulator value.
            """
        }
        sample("samples.collections.Collections.Aggregates.runningReduce")

        body {
            """
            if (isEmpty()) return emptyList()
            
            var accumulator = this[0]
            val result = ArrayList<T>(${f.code.size}).apply { add(accumulator) }
            for (index in 1 until ${f.code.size}) {
                accumulator = operation(accumulator, this[index])
                result.add(accumulator)
            }
            return result
            """
        }
    }

    val f_runningReduceIndexed = fn("runningReduceIndexed(operation: (index: Int, acc: T, T) -> T)") {
        include(ArraysOfPrimitives, ArraysOfUnsigned, CharSequences)
    } builder {
        since("1.4")

        specialFor(CharSequences) { inline() }
        specialFor(ArraysOfPrimitives, ArraysOfUnsigned) { inlineOnly() }

        returns("List<T>")

        doc {
            """
            Returns a list containing successive accumulation values generated by applying [operation] from left to right 
            to each ${f.element}, its index in the original ${f.collection} and current accumulator value that starts with the first ${f.element} of this ${f.collection}.
            ${scanAccMutationNote(false, f)}
            @param [operation] function that takes the index of ${f.element.prefixWithArticle()}, current accumulator value
            and the ${f.element} itself, and calculates the next accumulator value.
            """
        }
        sample("samples.collections.Collections.Aggregates.runningReduce")

        body {
            """
            if (isEmpty()) return emptyList()

            var accumulator = this[0]
            val result = ArrayList<T>(${f.code.size}).apply { add(accumulator) }
            for (index in 1 until ${f.code.size}) {
                accumulator = operation(index, accumulator, this[index])
                result.add(accumulator)
            }
            return result
            """
        }
    }

    val f_runningReduceSuper = fn("runningReduce(operation: (acc: S, T) -> S)") {
        include(ArraysOfObjects, Iterables, Sequences)
    } builder {
        since("1.4")

        specialFor(ArraysOfObjects, Iterables) { inline() }

        typeParam("S")
        typeParam("T : S")

        returns("List<S>")
        specialFor(Sequences) { returns("Sequence<S>") }

        doc {
            """
            Returns a ${f.mapResult} containing successive accumulation values generated by applying [operation] from left to right 
            to each ${f.element} and current accumulator value that starts with the first ${f.element} of this ${f.collection}.
            ${scanAccMutationNote(false, f)}
            @param [operation] function that takes current accumulator value and the ${f.element}, and calculates the next accumulator value.
            """
        }
        sample("samples.collections.Collections.Aggregates.runningReduce")
        sequenceClassification(intermediate, stateless)

        body(ArraysOfObjects) {
            """
            if (isEmpty()) return emptyList()

            var accumulator: S = this[0]
            val result = ArrayList<S>(size).apply { add(accumulator) }
            for (index in 1 until size) {
                accumulator = operation(accumulator, this[index])
                result.add(accumulator)
            }
            return result
            """
        }
        body(Iterables) {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) return emptyList()

            var accumulator: S = iterator.next()
            val result = ArrayList<S>(collectionSizeOrDefault(10)).apply { add(accumulator) }
            while (iterator.hasNext()) {
                accumulator = operation(accumulator, iterator.next())
                result.add(accumulator)
            }
            return result
            """
        }
        body(Sequences) {
            """
            return sequence {
                val iterator = iterator()
                if (iterator.hasNext()) {
                    var accumulator: S = iterator.next()
                    yield(accumulator)
                    while (iterator.hasNext()) {
                        accumulator = operation(accumulator, iterator.next())
                        yield(accumulator)
                    }
                }
            }
            """
        }
    }

    val f_runningReduceIndexedSuper = fn("runningReduceIndexed(operation: (index: Int, acc: S, T) -> S)") {
        include(ArraysOfObjects, Iterables, Sequences)
    } builder {
        since("1.4")

        specialFor(ArraysOfObjects, Iterables) { inline() }

        typeParam("S")
        typeParam("T : S")

        returns("List<S>")
        specialFor(Sequences) { returns("Sequence<S>") }

        doc {
            """
            Returns a ${f.mapResult} containing successive accumulation values generated by applying [operation] from left to right 
            to each ${f.element}, its index in the original ${f.collection} and current accumulator value that starts with the first ${f.element} of this ${f.collection}.
            ${scanAccMutationNote(false, f)}
            @param [operation] function that takes the index of ${f.element.prefixWithArticle()}, current accumulator value
            and the ${f.element} itself, and calculates the next accumulator value.
            """
        }
        sample("samples.collections.Collections.Aggregates.runningReduce")
        sequenceClassification(intermediate, stateless)

        body(ArraysOfObjects) {
            """
            if (isEmpty()) return emptyList()

            var accumulator: S = this[0]
            val result = ArrayList<S>(size).apply { add(accumulator) }
            for (index in 1 until size) {
                accumulator = operation(index, accumulator, this[index])
                result.add(accumulator)
            }
            return result
            """
        }
        body(Iterables) {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) return emptyList()

            var accumulator: S = iterator.next()
            val result = ArrayList<S>(collectionSizeOrDefault(10)).apply { add(accumulator) }
            var index = 1
            while (iterator.hasNext()) {
                accumulator = operation(index++, accumulator, iterator.next())
                result.add(accumulator)
            }
            return result
            """
        }
        body(Sequences) {
            """
            return sequence {
                val iterator = iterator()
                if (iterator.hasNext()) {
                    var accumulator: S = iterator.next()
                    yield(accumulator)
                    var index = 1
                    while (iterator.hasNext()) {
                        accumulator = operation(checkIndexOverflow(index++), accumulator, iterator.next())
                        yield(accumulator)
                    }
                }
            }
            """
        }
    }


    val f_onEach = fn("onEach(action: (T) -> Unit)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        since("1.1")
        doc { "Performs the given [action] on each ${f.element} and returns the ${f.collection} itself afterwards." }

        specialFor(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            since("1.4")
            inlineOnly()
            returns("SELF")
            body { "return apply { for (element in this) action(element) }" }
        }

        specialFor(Iterables, Maps, CharSequences) {
            inline()
            val collectionType = when (f) {
                Maps -> "M"
                CharSequences -> "S"
                else -> "C"
            }
            receiver(collectionType)
            returns(collectionType)
            typeParam("$collectionType : SELF")

            body { "return apply { for (element in this) action(element) }" }
        }

        specialFor(Sequences) {
            returns("SELF")
            doc { "Returns a sequence which performs the given [action] on each ${f.element} of the original sequence as they pass through it." }
            sequenceClassification(intermediate, stateless)
            body {
                """
                return map {
                    action(it)
                    it
                }
                """
            }
        }
    }

    val f_onEachIndexed = fn("onEachIndexed(action: (index: Int, T) -> Unit)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        since("1.4")

        doc {
            """
                Performs the given [action] on each ${f.element}, providing sequential index with the ${f.element}, 
                and returns the ${f.collection} itself afterwards.
                @param [action] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
                and performs the action on the ${f.element}.
                """
        }

        specialFor(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned) {
            inlineOnly()
            returns("SELF")
            body { "return apply { forEachIndexed(action) }" }
        }

        specialFor(Maps, Iterables, CharSequences) {
            inline()
            val collectionType = when (f) {
                Maps -> "M"
                CharSequences -> "S"
                else -> "C"
            }
            receiver(collectionType)
            returns(collectionType)
            typeParam("$collectionType : SELF")
            body { "return apply { ${if (f == Maps) "entries." else ""}forEachIndexed(action) }" }
        }

        specialFor(Sequences) {
            returns("SELF")
            doc {
                """
                Returns a sequence which performs the given [action] on each ${f.element} of the original sequence as they pass through it.
                @param [action] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
                and performs the action on the ${f.element}.
                """
            }
            sequenceClassification(intermediate, stateless)
            body {
                """
                return mapIndexed { index, element ->
                    action(index, element)
                    element
                }
                """
            }
        }
    }

    val f_forEach = fn("forEach(action: (T) -> Unit)") {
        includeDefault()
        include(Maps, CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc { "Performs the given [action] on each ${f.element}." }
        specialFor(Iterables, Maps) { annotation("@kotlin.internal.HidesMembers") }
        returns("Unit")
        body {
            """
            for (element in this) action(element)
            """
        }
    }

    val f_forEachIndexed = fn("forEachIndexed(action: (index: Int, T) -> Unit)") {
        includeDefault()
        include(CharSequences, ArraysOfUnsigned)
    } builder {
        inline()
        specialFor(ArraysOfUnsigned) { inlineOnly() }

        doc {
            """
            Performs the given [action] on each ${f.element}, providing sequential index with the ${f.element}.
            @param [action] function that takes the index of ${f.element.prefixWithArticle()} and the ${f.element} itself
            and performs the action on the ${f.element}.
            """ }
        returns("Unit")
        body {
            fun checkOverflow(value: String) = if (f == Sequences || f == Iterables) "checkIndexOverflow($value)" else value
            """
            var index = 0
            for (item in this) action(${checkOverflow("index++")}, item)
            """
        }
    }
}
