package kotlin.text

@kotlin.SinceKotlin(version = "1.2") public val kotlin.String.Companion.CASE_INSENSITIVE_ORDER: kotlin.Comparator<kotlin.String>
    public fun kotlin.String.Companion.<get-CASE_INSENSITIVE_ORDER>(): kotlin.Comparator<kotlin.String>
public val kotlin.CharSequence.indices: kotlin.ranges.IntRange
    public fun kotlin.CharSequence.<get-indices>(): kotlin.ranges.IntRange
public val kotlin.CharSequence.lastIndex: kotlin.Int
    public fun kotlin.CharSequence.<get-lastIndex>(): kotlin.Int
@kotlin.Deprecated(level = DeprecationLevel.HIDDEN, message = "Provided for binary compatibility") @kotlin.js.JsName(name = "Regex_sb3q2$") public fun Regex_0(/*0*/ pattern: kotlin.String, /*1*/ option: kotlin.text.RegexOption): kotlin.text.Regex
@kotlin.Deprecated(level = DeprecationLevel.HIDDEN, message = "Provided for binary compatibility") @kotlin.js.JsName(name = "Regex_61zpoe$") public fun Regex_1(/*0*/ pattern: kotlin.String): kotlin.text.Regex
@kotlin.SinceKotlin(version = "1.2") public fun String(/*0*/ chars: kotlin.CharArray): kotlin.String
@kotlin.SinceKotlin(version = "1.2") public fun String(/*0*/ chars: kotlin.CharArray, /*1*/ offset: kotlin.Int, /*2*/ length: kotlin.Int): kotlin.String
@kotlin.SinceKotlin(version = "1.1") @kotlin.internal.InlineOnly public inline fun buildString(/*0*/ capacity: kotlin.Int, /*1*/ builderAction: kotlin.text.StringBuilder.() -> kotlin.Unit): kotlin.String
@kotlin.internal.InlineOnly public inline fun buildString(/*0*/ builderAction: kotlin.text.StringBuilder.() -> kotlin.Unit): kotlin.String
public inline fun kotlin.CharSequence.all(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Boolean
public fun kotlin.CharSequence.any(): kotlin.Boolean
public inline fun kotlin.CharSequence.any(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Boolean
public fun </*0*/ T : kotlin.text.Appendable> T.append(/*0*/ vararg value: kotlin.CharSequence? /*kotlin.Array<out kotlin.CharSequence?>*/): T
@kotlin.Deprecated(level = DeprecationLevel.WARNING, message = "Use append(value: Any?) instead", replaceWith = kotlin.ReplaceWith(expression = "append(value = obj)", imports = {})) @kotlin.internal.InlineOnly public inline fun kotlin.text.StringBuilder.append(/*0*/ obj: kotlin.Any?): kotlin.text.StringBuilder
public fun kotlin.text.StringBuilder.append(/*0*/ vararg value: kotlin.Any? /*kotlin.Array<out kotlin.Any?>*/): kotlin.text.StringBuilder
public fun kotlin.text.StringBuilder.append(/*0*/ vararg value: kotlin.String? /*kotlin.Array<out kotlin.String?>*/): kotlin.text.StringBuilder
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.text.Appendable.appendLine(): kotlin.text.Appendable
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.text.Appendable.appendLine(/*0*/ value: kotlin.Char): kotlin.text.Appendable
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.text.Appendable.appendLine(/*0*/ value: kotlin.CharSequence?): kotlin.text.Appendable
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.text.StringBuilder.appendLine(): kotlin.text.StringBuilder
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.text.StringBuilder.appendLine(/*0*/ value: kotlin.Any?): kotlin.text.StringBuilder
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.text.StringBuilder.appendLine(/*0*/ value: kotlin.Boolean): kotlin.text.StringBuilder
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.text.StringBuilder.appendLine(/*0*/ value: kotlin.Char): kotlin.text.StringBuilder
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.text.StringBuilder.appendLine(/*0*/ value: kotlin.CharArray): kotlin.text.StringBuilder
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.text.StringBuilder.appendLine(/*0*/ value: kotlin.CharSequence?): kotlin.text.StringBuilder
@kotlin.SinceKotlin(version = "1.4") @kotlin.internal.InlineOnly public inline fun kotlin.text.StringBuilder.appendLine(/*0*/ value: kotlin.String?): kotlin.text.StringBuilder
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public fun </*0*/ T : kotlin.text.Appendable> T.appendRange(/*0*/ value: kotlin.CharSequence, /*1*/ startIndex: kotlin.Int, /*2*/ endIndex: kotlin.Int): T
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public inline fun kotlin.text.StringBuilder.appendRange(/*0*/ value: kotlin.CharArray, /*1*/ startIndex: kotlin.Int, /*2*/ endIndex: kotlin.Int): kotlin.text.StringBuilder
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public inline fun kotlin.text.StringBuilder.appendRange(/*0*/ value: kotlin.CharSequence, /*1*/ startIndex: kotlin.Int, /*2*/ endIndex: kotlin.Int): kotlin.text.StringBuilder
public fun kotlin.CharSequence.asIterable(): kotlin.collections.Iterable<kotlin.Char>
public fun kotlin.CharSequence.asSequence(): kotlin.sequences.Sequence<kotlin.Char>
public inline fun </*0*/ K, /*1*/ V> kotlin.CharSequence.associate(/*0*/ transform: (kotlin.Char) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>
public inline fun </*0*/ K> kotlin.CharSequence.associateBy(/*0*/ keySelector: (kotlin.Char) -> K): kotlin.collections.Map<K, kotlin.Char>
public inline fun </*0*/ K, /*1*/ V> kotlin.CharSequence.associateBy(/*0*/ keySelector: (kotlin.Char) -> K, /*1*/ valueTransform: (kotlin.Char) -> V): kotlin.collections.Map<K, V>
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, in kotlin.Char>> kotlin.CharSequence.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Char) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.CharSequence.associateByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Char) -> K, /*2*/ valueTransform: (kotlin.Char) -> V): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, in V>> kotlin.CharSequence.associateTo(/*0*/ destination: M, /*1*/ transform: (kotlin.Char) -> kotlin.Pair<K, V>): M
@kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ V> kotlin.CharSequence.associateWith(/*0*/ valueSelector: (kotlin.Char) -> V): kotlin.collections.Map<kotlin.Char, V>
@kotlin.SinceKotlin(version = "1.3") public inline fun </*0*/ V, /*1*/ M : kotlin.collections.MutableMap<in kotlin.Char, in V>> kotlin.CharSequence.associateWithTo(/*0*/ destination: M, /*1*/ valueSelector: (kotlin.Char) -> V): M
public fun kotlin.String.capitalize(): kotlin.String
@kotlin.SinceKotlin(version = "1.2") public fun kotlin.CharSequence.chunked(/*0*/ size: kotlin.Int): kotlin.collections.List<kotlin.String>
@kotlin.SinceKotlin(version = "1.2") public fun </*0*/ R> kotlin.CharSequence.chunked(/*0*/ size: kotlin.Int, /*1*/ transform: (kotlin.CharSequence) -> R): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.2") public fun kotlin.CharSequence.chunkedSequence(/*0*/ size: kotlin.Int): kotlin.sequences.Sequence<kotlin.String>
@kotlin.SinceKotlin(version = "1.2") public fun </*0*/ R> kotlin.CharSequence.chunkedSequence(/*0*/ size: kotlin.Int, /*1*/ transform: (kotlin.CharSequence) -> R): kotlin.sequences.Sequence<R>
@kotlin.SinceKotlin(version = "1.3") public inline fun kotlin.text.StringBuilder.clear(): kotlin.text.StringBuilder
public fun kotlin.CharSequence.commonPrefixWith(/*0*/ other: kotlin.CharSequence, /*1*/ ignoreCase: kotlin.Boolean = ...): kotlin.String
public fun kotlin.CharSequence.commonSuffixWith(/*0*/ other: kotlin.CharSequence, /*1*/ ignoreCase: kotlin.Boolean = ...): kotlin.String
@kotlin.SinceKotlin(version = "1.2") public fun kotlin.String.compareTo(/*0*/ other: kotlin.String, /*1*/ ignoreCase: kotlin.Boolean = ...): kotlin.Int
@kotlin.internal.InlineOnly public inline fun kotlin.String.concat(/*0*/ str: kotlin.String): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.CharArray.concatToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.CharArray.concatToString(/*0*/ startIndex: kotlin.Int = ..., /*1*/ endIndex: kotlin.Int = ...): kotlin.String
public operator fun kotlin.CharSequence.contains(/*0*/ char: kotlin.Char, /*1*/ ignoreCase: kotlin.Boolean = ...): kotlin.Boolean
public operator fun kotlin.CharSequence.contains(/*0*/ other: kotlin.CharSequence, /*1*/ ignoreCase: kotlin.Boolean = ...): kotlin.Boolean
@kotlin.internal.InlineOnly public inline operator fun kotlin.CharSequence.contains(/*0*/ regex: kotlin.text.Regex): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.count(): kotlin.Int
public inline fun kotlin.CharSequence.count(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Int
public fun kotlin.String.decapitalize(): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.ByteArray.decodeToString(): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.ByteArray.decodeToString(/*0*/ startIndex: kotlin.Int = ..., /*1*/ endIndex: kotlin.Int = ..., /*2*/ throwOnInvalidSequence: kotlin.Boolean = ...): kotlin.String
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public inline fun kotlin.text.StringBuilder.deleteAt(/*0*/ index: kotlin.Int): kotlin.text.StringBuilder
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public inline fun kotlin.text.StringBuilder.deleteRange(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int): kotlin.text.StringBuilder
public fun kotlin.CharSequence.drop(/*0*/ n: kotlin.Int): kotlin.CharSequence
public fun kotlin.String.drop(/*0*/ n: kotlin.Int): kotlin.String
public fun kotlin.CharSequence.dropLast(/*0*/ n: kotlin.Int): kotlin.CharSequence
public fun kotlin.String.dropLast(/*0*/ n: kotlin.Int): kotlin.String
public inline fun kotlin.CharSequence.dropLastWhile(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence
public inline fun kotlin.String.dropLastWhile(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String
public inline fun kotlin.CharSequence.dropWhile(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence
public inline fun kotlin.String.dropWhile(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String
public fun kotlin.CharSequence.elementAt(/*0*/ index: kotlin.Int): kotlin.Char
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.elementAtOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Char): kotlin.Char
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.elementAtOrNull(/*0*/ index: kotlin.Int): kotlin.Char?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.String.encodeToByteArray(): kotlin.ByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.String.encodeToByteArray(/*0*/ startIndex: kotlin.Int = ..., /*1*/ endIndex: kotlin.Int = ..., /*2*/ throwOnInvalidSequence: kotlin.Boolean = ...): kotlin.ByteArray
public fun kotlin.CharSequence.endsWith(/*0*/ char: kotlin.Char, /*1*/ ignoreCase: kotlin.Boolean = ...): kotlin.Boolean
public fun kotlin.CharSequence.endsWith(/*0*/ suffix: kotlin.CharSequence, /*1*/ ignoreCase: kotlin.Boolean = ...): kotlin.Boolean
public fun kotlin.String.endsWith(/*0*/ suffix: kotlin.String, /*1*/ ignoreCase: kotlin.Boolean = ...): kotlin.Boolean
public fun kotlin.Char.equals(/*0*/ other: kotlin.Char, /*1*/ ignoreCase: kotlin.Boolean = ...): kotlin.Boolean
public fun kotlin.String?.equals(/*0*/ other: kotlin.String?, /*1*/ ignoreCase: kotlin.Boolean = ...): kotlin.Boolean
public inline fun kotlin.CharSequence.filter(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence
public inline fun kotlin.String.filter(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String
public inline fun kotlin.CharSequence.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence
public inline fun kotlin.String.filterIndexed(/*0*/ predicate: (index: kotlin.Int, kotlin.Char) -> kotlin.Boolean): kotlin.String
public inline fun </*0*/ C : kotlin.text.Appendable> kotlin.CharSequence.filterIndexedTo(/*0*/ destination: C, /*1*/ predicate: (index: kotlin.Int, kotlin.Char) -> kotlin.Boolean): C
public inline fun kotlin.CharSequence.filterNot(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence
public inline fun kotlin.String.filterNot(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String
public inline fun </*0*/ C : kotlin.text.Appendable> kotlin.CharSequence.filterNotTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Char) -> kotlin.Boolean): C
public inline fun </*0*/ C : kotlin.text.Appendable> kotlin.CharSequence.filterTo(/*0*/ destination: C, /*1*/ predicate: (kotlin.Char) -> kotlin.Boolean): C
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.find(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?
public fun kotlin.CharSequence.findAnyOf(/*0*/ strings: kotlin.collections.Collection<kotlin.String>, /*1*/ startIndex: kotlin.Int = ..., /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.Pair<kotlin.Int, kotlin.String>?
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.findLast(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?
public fun kotlin.CharSequence.findLastAnyOf(/*0*/ strings: kotlin.collections.Collection<kotlin.String>, /*1*/ startIndex: kotlin.Int = ..., /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.Pair<kotlin.Int, kotlin.String>?
public fun kotlin.CharSequence.first(): kotlin.Char
public inline fun kotlin.CharSequence.first(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char
public fun kotlin.CharSequence.firstOrNull(): kotlin.Char?
public inline fun kotlin.CharSequence.firstOrNull(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?
public inline fun </*0*/ R> kotlin.CharSequence.flatMap(/*0*/ transform: (kotlin.Char) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.CharSequence.flatMapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Char) -> kotlin.collections.Iterable<R>): C
public inline fun </*0*/ R> kotlin.CharSequence.fold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.Char) -> R): R
public inline fun </*0*/ R> kotlin.CharSequence.foldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.Char) -> R): R
public inline fun </*0*/ R> kotlin.CharSequence.foldRight(/*0*/ initial: R, /*1*/ operation: (kotlin.Char, acc: R) -> R): R
public inline fun </*0*/ R> kotlin.CharSequence.foldRightIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, kotlin.Char, acc: R) -> R): R
public inline fun kotlin.CharSequence.forEach(/*0*/ action: (kotlin.Char) -> kotlin.Unit): kotlin.Unit
public inline fun kotlin.CharSequence.forEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Char) -> kotlin.Unit): kotlin.Unit
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.getOrElse(/*0*/ index: kotlin.Int, /*1*/ defaultValue: (kotlin.Int) -> kotlin.Char): kotlin.Char
public fun kotlin.CharSequence.getOrNull(/*0*/ index: kotlin.Int): kotlin.Char?
public inline fun </*0*/ K> kotlin.CharSequence.groupBy(/*0*/ keySelector: (kotlin.Char) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Char>>
public inline fun </*0*/ K, /*1*/ V> kotlin.CharSequence.groupBy(/*0*/ keySelector: (kotlin.Char) -> K, /*1*/ valueTransform: (kotlin.Char) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>
public inline fun </*0*/ K, /*1*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Char>>> kotlin.CharSequence.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Char) -> K): M
public inline fun </*0*/ K, /*1*/ V, /*2*/ M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.CharSequence.groupByTo(/*0*/ destination: M, /*1*/ keySelector: (kotlin.Char) -> K, /*2*/ valueTransform: (kotlin.Char) -> V): M
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ K> kotlin.CharSequence.groupingBy(/*0*/ crossinline keySelector: (kotlin.Char) -> K): kotlin.collections.Grouping<kotlin.Char, K>
public fun kotlin.CharSequence.hasSurrogatePairAt(/*0*/ index: kotlin.Int): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.CharSequence, /*1*/ R> C.ifBlank(/*0*/ defaultValue: () -> R): R where C : R
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ C : kotlin.CharSequence, /*1*/ R> C.ifEmpty(/*0*/ defaultValue: () -> R): R where C : R
public fun kotlin.CharSequence.indexOf(/*0*/ char: kotlin.Char, /*1*/ startIndex: kotlin.Int = ..., /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.Int
public fun kotlin.CharSequence.indexOf(/*0*/ string: kotlin.String, /*1*/ startIndex: kotlin.Int = ..., /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.Int
public fun kotlin.CharSequence.indexOfAny(/*0*/ chars: kotlin.CharArray, /*1*/ startIndex: kotlin.Int = ..., /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.Int
public fun kotlin.CharSequence.indexOfAny(/*0*/ strings: kotlin.collections.Collection<kotlin.String>, /*1*/ startIndex: kotlin.Int = ..., /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.Int
public inline fun kotlin.CharSequence.indexOfFirst(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Int
public inline fun kotlin.CharSequence.indexOfLast(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public inline fun kotlin.text.StringBuilder.insertRange(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.CharArray, /*2*/ startIndex: kotlin.Int, /*3*/ endIndex: kotlin.Int): kotlin.text.StringBuilder
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public inline fun kotlin.text.StringBuilder.insertRange(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.CharSequence, /*2*/ startIndex: kotlin.Int, /*3*/ endIndex: kotlin.Int): kotlin.text.StringBuilder
public fun kotlin.CharSequence.isBlank(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.isEmpty(): kotlin.Boolean
public fun kotlin.Char.isHighSurrogate(): kotlin.Boolean
public fun kotlin.Char.isLowSurrogate(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.isNotBlank(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.isNotEmpty(): kotlin.Boolean
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence?.isNullOrBlank(): kotlin.Boolean
    Returns(FALSE) -> <this> != null

@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence?.isNullOrEmpty(): kotlin.Boolean
    Returns(FALSE) -> <this> != null

public fun kotlin.Char.isSurrogate(): kotlin.Boolean
public fun kotlin.Char.isWhitespace(): kotlin.Boolean
public operator fun kotlin.CharSequence.iterator(): kotlin.collections.CharIterator
public fun kotlin.CharSequence.last(): kotlin.Char
public inline fun kotlin.CharSequence.last(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char
public fun kotlin.CharSequence.lastIndexOf(/*0*/ char: kotlin.Char, /*1*/ startIndex: kotlin.Int = ..., /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.Int
public fun kotlin.CharSequence.lastIndexOf(/*0*/ string: kotlin.String, /*1*/ startIndex: kotlin.Int = ..., /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.Int
public fun kotlin.CharSequence.lastIndexOfAny(/*0*/ chars: kotlin.CharArray, /*1*/ startIndex: kotlin.Int = ..., /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.Int
public fun kotlin.CharSequence.lastIndexOfAny(/*0*/ strings: kotlin.collections.Collection<kotlin.String>, /*1*/ startIndex: kotlin.Int = ..., /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.Int
public fun kotlin.CharSequence.lastOrNull(): kotlin.Char?
public inline fun kotlin.CharSequence.lastOrNull(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?
public fun kotlin.CharSequence.lineSequence(): kotlin.sequences.Sequence<kotlin.String>
public fun kotlin.CharSequence.lines(): kotlin.collections.List<kotlin.String>
public inline fun </*0*/ R> kotlin.CharSequence.map(/*0*/ transform: (kotlin.Char) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R> kotlin.CharSequence.mapIndexed(/*0*/ transform: (index: kotlin.Int, kotlin.Char) -> R): kotlin.collections.List<R>
public inline fun </*0*/ R : kotlin.Any> kotlin.CharSequence.mapIndexedNotNull(/*0*/ transform: (index: kotlin.Int, kotlin.Char) -> R?): kotlin.collections.List<R>
public inline fun </*0*/ R : kotlin.Any, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.CharSequence.mapIndexedNotNullTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.Char) -> R?): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.CharSequence.mapIndexedTo(/*0*/ destination: C, /*1*/ transform: (index: kotlin.Int, kotlin.Char) -> R): C
public inline fun </*0*/ R : kotlin.Any> kotlin.CharSequence.mapNotNull(/*0*/ transform: (kotlin.Char) -> R?): kotlin.collections.List<R>
public inline fun </*0*/ R : kotlin.Any, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.CharSequence.mapNotNullTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Char) -> R?): C
public inline fun </*0*/ R, /*1*/ C : kotlin.collections.MutableCollection<in R>> kotlin.CharSequence.mapTo(/*0*/ destination: C, /*1*/ transform: (kotlin.Char) -> R): C
@kotlin.internal.InlineOnly public inline fun kotlin.String.match(/*0*/ regex: kotlin.String): kotlin.Array<kotlin.String>?
@kotlin.internal.InlineOnly public inline infix fun kotlin.CharSequence.matches(/*0*/ regex: kotlin.text.Regex): kotlin.Boolean
public fun kotlin.String.matches(/*0*/ regex: kotlin.String): kotlin.Boolean
public fun kotlin.CharSequence.max(): kotlin.Char?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.CharSequence.maxBy(/*0*/ selector: (kotlin.Char) -> R): kotlin.Char?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.CharSequence.maxOf(/*0*/ selector: (kotlin.Char) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.maxOf(/*0*/ selector: (kotlin.Char) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.maxOf(/*0*/ selector: (kotlin.Char) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.CharSequence.maxOfOrNull(/*0*/ selector: (kotlin.Char) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.maxOfOrNull(/*0*/ selector: (kotlin.Char) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.maxOfOrNull(/*0*/ selector: (kotlin.Char) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.CharSequence.maxOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Char) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.CharSequence.maxOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Char) -> R): R?
public fun kotlin.CharSequence.maxWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Char>): kotlin.Char?
public fun kotlin.CharSequence.min(): kotlin.Char?
public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.CharSequence.minBy(/*0*/ selector: (kotlin.Char) -> R): kotlin.Char?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.CharSequence.minOf(/*0*/ selector: (kotlin.Char) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.minOf(/*0*/ selector: (kotlin.Char) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.minOf(/*0*/ selector: (kotlin.Char) -> kotlin.Float): kotlin.Float
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R : kotlin.Comparable<R>> kotlin.CharSequence.minOfOrNull(/*0*/ selector: (kotlin.Char) -> R): R?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.minOfOrNull(/*0*/ selector: (kotlin.Char) -> kotlin.Double): kotlin.Double?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.minOfOrNull(/*0*/ selector: (kotlin.Char) -> kotlin.Float): kotlin.Float?
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.CharSequence.minOfWith(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Char) -> R): R
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.internal.InlineOnly public inline fun </*0*/ R> kotlin.CharSequence.minOfWithOrNull(/*0*/ comparator: kotlin.Comparator<in R>, /*1*/ selector: (kotlin.Char) -> R): R?
public fun kotlin.CharSequence.minWith(/*0*/ comparator: kotlin.Comparator<in kotlin.Char>): kotlin.Char?
public fun kotlin.CharSequence.none(): kotlin.Boolean
public inline fun kotlin.CharSequence.none(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.1") public inline fun </*0*/ S : kotlin.CharSequence> S.onEach(/*0*/ action: (kotlin.Char) -> kotlin.Unit): S
@kotlin.SinceKotlin(version = "1.4") public inline fun </*0*/ S : kotlin.CharSequence> S.onEachIndexed(/*0*/ action: (index: kotlin.Int, kotlin.Char) -> kotlin.Unit): S
@kotlin.internal.InlineOnly public inline fun kotlin.String?.orEmpty(): kotlin.String
public fun kotlin.CharSequence.padEnd(/*0*/ length: kotlin.Int, /*1*/ padChar: kotlin.Char = ...): kotlin.CharSequence
public fun kotlin.String.padEnd(/*0*/ length: kotlin.Int, /*1*/ padChar: kotlin.Char = ...): kotlin.String
public fun kotlin.CharSequence.padStart(/*0*/ length: kotlin.Int, /*1*/ padChar: kotlin.Char = ...): kotlin.CharSequence
public fun kotlin.String.padStart(/*0*/ length: kotlin.Int, /*1*/ padChar: kotlin.Char = ...): kotlin.String
public inline fun kotlin.CharSequence.partition(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Pair<kotlin.CharSequence, kotlin.CharSequence>
public inline fun kotlin.String.partition(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Pair<kotlin.String, kotlin.String>
@kotlin.internal.InlineOnly public inline operator fun kotlin.Char.plus(/*0*/ other: kotlin.String): kotlin.String
public fun kotlin.String.prependIndent(/*0*/ indent: kotlin.String = ...): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.random(): kotlin.Char
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.CharSequence.random(/*0*/ random: kotlin.random.Random): kotlin.Char
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.randomOrNull(): kotlin.Char?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.CharSequence.randomOrNull(/*0*/ random: kotlin.random.Random): kotlin.Char?
public inline fun kotlin.CharSequence.reduce(/*0*/ operation: (acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char
public inline fun kotlin.CharSequence.reduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char
@kotlin.SinceKotlin(version = "1.4") public inline fun kotlin.CharSequence.reduceIndexedOrNull(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun kotlin.CharSequence.reduceOrNull(/*0*/ operation: (acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char?
public inline fun kotlin.CharSequence.reduceRight(/*0*/ operation: (kotlin.Char, acc: kotlin.Char) -> kotlin.Char): kotlin.Char
public inline fun kotlin.CharSequence.reduceRightIndexed(/*0*/ operation: (index: kotlin.Int, kotlin.Char, acc: kotlin.Char) -> kotlin.Char): kotlin.Char
@kotlin.SinceKotlin(version = "1.4") public inline fun kotlin.CharSequence.reduceRightIndexedOrNull(/*0*/ operation: (index: kotlin.Int, kotlin.Char, acc: kotlin.Char) -> kotlin.Char): kotlin.Char?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun kotlin.CharSequence.reduceRightOrNull(/*0*/ operation: (kotlin.Char, acc: kotlin.Char) -> kotlin.Char): kotlin.Char?
public fun kotlin.CharSequence.regionMatches(/*0*/ thisOffset: kotlin.Int, /*1*/ other: kotlin.CharSequence, /*2*/ otherOffset: kotlin.Int, /*3*/ length: kotlin.Int, /*4*/ ignoreCase: kotlin.Boolean = ...): kotlin.Boolean
public fun kotlin.CharSequence.removePrefix(/*0*/ prefix: kotlin.CharSequence): kotlin.CharSequence
public fun kotlin.String.removePrefix(/*0*/ prefix: kotlin.CharSequence): kotlin.String
public fun kotlin.CharSequence.removeRange(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int): kotlin.CharSequence
public fun kotlin.CharSequence.removeRange(/*0*/ range: kotlin.ranges.IntRange): kotlin.CharSequence
@kotlin.internal.InlineOnly public inline fun kotlin.String.removeRange(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int): kotlin.String
@kotlin.internal.InlineOnly public inline fun kotlin.String.removeRange(/*0*/ range: kotlin.ranges.IntRange): kotlin.String
public fun kotlin.CharSequence.removeSuffix(/*0*/ suffix: kotlin.CharSequence): kotlin.CharSequence
public fun kotlin.String.removeSuffix(/*0*/ suffix: kotlin.CharSequence): kotlin.String
public fun kotlin.CharSequence.removeSurrounding(/*0*/ delimiter: kotlin.CharSequence): kotlin.CharSequence
public fun kotlin.CharSequence.removeSurrounding(/*0*/ prefix: kotlin.CharSequence, /*1*/ suffix: kotlin.CharSequence): kotlin.CharSequence
public fun kotlin.String.removeSurrounding(/*0*/ delimiter: kotlin.CharSequence): kotlin.String
public fun kotlin.String.removeSurrounding(/*0*/ prefix: kotlin.CharSequence, /*1*/ suffix: kotlin.CharSequence): kotlin.String
public fun kotlin.CharSequence.repeat(/*0*/ n: kotlin.Int): kotlin.String
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.replace(/*0*/ regex: kotlin.text.Regex, /*1*/ noinline transform: (kotlin.text.MatchResult) -> kotlin.CharSequence): kotlin.String
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.replace(/*0*/ regex: kotlin.text.Regex, /*1*/ replacement: kotlin.String): kotlin.String
public fun kotlin.String.replace(/*0*/ oldChar: kotlin.Char, /*1*/ newChar: kotlin.Char, /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.String
public fun kotlin.String.replace(/*0*/ oldValue: kotlin.String, /*1*/ newValue: kotlin.String, /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.String
public fun kotlin.String.replaceAfter(/*0*/ delimiter: kotlin.Char, /*1*/ replacement: kotlin.String, /*2*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.replaceAfter(/*0*/ delimiter: kotlin.String, /*1*/ replacement: kotlin.String, /*2*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.replaceAfterLast(/*0*/ delimiter: kotlin.Char, /*1*/ replacement: kotlin.String, /*2*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.replaceAfterLast(/*0*/ delimiter: kotlin.String, /*1*/ replacement: kotlin.String, /*2*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.replaceBefore(/*0*/ delimiter: kotlin.Char, /*1*/ replacement: kotlin.String, /*2*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.replaceBefore(/*0*/ delimiter: kotlin.String, /*1*/ replacement: kotlin.String, /*2*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.replaceBeforeLast(/*0*/ delimiter: kotlin.Char, /*1*/ replacement: kotlin.String, /*2*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.replaceBeforeLast(/*0*/ delimiter: kotlin.String, /*1*/ replacement: kotlin.String, /*2*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.replaceFirst(/*0*/ regex: kotlin.text.Regex, /*1*/ replacement: kotlin.String): kotlin.String
public fun kotlin.String.replaceFirst(/*0*/ oldChar: kotlin.Char, /*1*/ newChar: kotlin.Char, /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.String
public fun kotlin.String.replaceFirst(/*0*/ oldValue: kotlin.String, /*1*/ newValue: kotlin.String, /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.String
public fun kotlin.String.replaceIndent(/*0*/ newIndent: kotlin.String = ...): kotlin.String
public fun kotlin.String.replaceIndentByMargin(/*0*/ newIndent: kotlin.String = ..., /*1*/ marginPrefix: kotlin.String = ...): kotlin.String
public fun kotlin.CharSequence.replaceRange(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int, /*2*/ replacement: kotlin.CharSequence): kotlin.CharSequence
public fun kotlin.CharSequence.replaceRange(/*0*/ range: kotlin.ranges.IntRange, /*1*/ replacement: kotlin.CharSequence): kotlin.CharSequence
@kotlin.internal.InlineOnly public inline fun kotlin.String.replaceRange(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int, /*2*/ replacement: kotlin.CharSequence): kotlin.String
@kotlin.internal.InlineOnly public inline fun kotlin.String.replaceRange(/*0*/ range: kotlin.ranges.IntRange, /*1*/ replacement: kotlin.CharSequence): kotlin.String
public fun kotlin.CharSequence.reversed(): kotlin.CharSequence
@kotlin.internal.InlineOnly public inline fun kotlin.String.reversed(): kotlin.String
@kotlin.SinceKotlin(version = "1.4") public inline fun </*0*/ R> kotlin.CharSequence.runningFold(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.Char) -> R): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.4") public inline fun </*0*/ R> kotlin.CharSequence.runningFoldIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.Char) -> R): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.4") public inline fun kotlin.CharSequence.runningReduce(/*0*/ operation: (acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.collections.List<kotlin.Char>
@kotlin.SinceKotlin(version = "1.4") public inline fun kotlin.CharSequence.runningReduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.collections.List<kotlin.Char>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun </*0*/ R> kotlin.CharSequence.scan(/*0*/ initial: R, /*1*/ operation: (acc: R, kotlin.Char) -> R): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun </*0*/ R> kotlin.CharSequence.scanIndexed(/*0*/ initial: R, /*1*/ operation: (index: kotlin.Int, acc: R, kotlin.Char) -> R): kotlin.collections.List<R>
@kotlin.Deprecated(message = "Use runningReduce instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduce(operation)", imports = {})) @kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun kotlin.CharSequence.scanReduce(/*0*/ operation: (acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.collections.List<kotlin.Char>
@kotlin.Deprecated(message = "Use runningReduceIndexed instead.", replaceWith = kotlin.ReplaceWith(expression = "runningReduceIndexed(operation)", imports = {})) @kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public inline fun kotlin.CharSequence.scanReduceIndexed(/*0*/ operation: (index: kotlin.Int, acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.collections.List<kotlin.Char>
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public inline operator fun kotlin.text.StringBuilder.set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Char): kotlin.Unit
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public inline fun kotlin.text.StringBuilder.setRange(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int, /*2*/ value: kotlin.String): kotlin.text.StringBuilder
public fun kotlin.CharSequence.single(): kotlin.Char
public inline fun kotlin.CharSequence.single(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char
public fun kotlin.CharSequence.singleOrNull(): kotlin.Char?
public inline fun kotlin.CharSequence.singleOrNull(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?
public fun kotlin.CharSequence.slice(/*0*/ indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.CharSequence
public fun kotlin.CharSequence.slice(/*0*/ indices: kotlin.ranges.IntRange): kotlin.CharSequence
@kotlin.internal.InlineOnly public inline fun kotlin.String.slice(/*0*/ indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.String
public fun kotlin.String.slice(/*0*/ indices: kotlin.ranges.IntRange): kotlin.String
public fun kotlin.CharSequence.split(/*0*/ vararg delimiters: kotlin.String /*kotlin.Array<out kotlin.String>*/, /*1*/ ignoreCase: kotlin.Boolean = ..., /*2*/ limit: kotlin.Int = ...): kotlin.collections.List<kotlin.String>
public fun kotlin.CharSequence.split(/*0*/ vararg delimiters: kotlin.Char /*kotlin.CharArray*/, /*1*/ ignoreCase: kotlin.Boolean = ..., /*2*/ limit: kotlin.Int = ...): kotlin.collections.List<kotlin.String>
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.split(/*0*/ regex: kotlin.text.Regex, /*1*/ limit: kotlin.Int = ...): kotlin.collections.List<kotlin.String>
public fun kotlin.CharSequence.splitToSequence(/*0*/ vararg delimiters: kotlin.String /*kotlin.Array<out kotlin.String>*/, /*1*/ ignoreCase: kotlin.Boolean = ..., /*2*/ limit: kotlin.Int = ...): kotlin.sequences.Sequence<kotlin.String>
public fun kotlin.CharSequence.splitToSequence(/*0*/ vararg delimiters: kotlin.Char /*kotlin.CharArray*/, /*1*/ ignoreCase: kotlin.Boolean = ..., /*2*/ limit: kotlin.Int = ...): kotlin.sequences.Sequence<kotlin.String>
public fun kotlin.CharSequence.startsWith(/*0*/ char: kotlin.Char, /*1*/ ignoreCase: kotlin.Boolean = ...): kotlin.Boolean
public fun kotlin.CharSequence.startsWith(/*0*/ prefix: kotlin.CharSequence, /*1*/ ignoreCase: kotlin.Boolean = ...): kotlin.Boolean
public fun kotlin.CharSequence.startsWith(/*0*/ prefix: kotlin.CharSequence, /*1*/ startIndex: kotlin.Int, /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.Boolean
public fun kotlin.String.startsWith(/*0*/ prefix: kotlin.String, /*1*/ ignoreCase: kotlin.Boolean = ...): kotlin.Boolean
public fun kotlin.String.startsWith(/*0*/ prefix: kotlin.String, /*1*/ startIndex: kotlin.Int, /*2*/ ignoreCase: kotlin.Boolean = ...): kotlin.Boolean
public fun kotlin.CharSequence.subSequence(/*0*/ range: kotlin.ranges.IntRange): kotlin.CharSequence
@kotlin.internal.InlineOnly @kotlin.Deprecated(message = "Use parameters named startIndex and endIndex.", replaceWith = kotlin.ReplaceWith(expression = "subSequence(startIndex = start, endIndex = end)", imports = {})) public inline fun kotlin.String.subSequence(/*0*/ start: kotlin.Int, /*1*/ end: kotlin.Int): kotlin.CharSequence
@kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.substring(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int = ...): kotlin.String
public fun kotlin.CharSequence.substring(/*0*/ range: kotlin.ranges.IntRange): kotlin.String
@kotlin.internal.InlineOnly public inline fun kotlin.String.substring(/*0*/ startIndex: kotlin.Int): kotlin.String
@kotlin.internal.InlineOnly public inline fun kotlin.String.substring(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int): kotlin.String
public fun kotlin.String.substring(/*0*/ range: kotlin.ranges.IntRange): kotlin.String
public fun kotlin.String.substringAfter(/*0*/ delimiter: kotlin.Char, /*1*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.substringAfter(/*0*/ delimiter: kotlin.String, /*1*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.substringAfterLast(/*0*/ delimiter: kotlin.Char, /*1*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.substringAfterLast(/*0*/ delimiter: kotlin.String, /*1*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.substringBefore(/*0*/ delimiter: kotlin.Char, /*1*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.substringBefore(/*0*/ delimiter: kotlin.String, /*1*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.substringBeforeLast(/*0*/ delimiter: kotlin.Char, /*1*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public fun kotlin.String.substringBeforeLast(/*0*/ delimiter: kotlin.String, /*1*/ missingDelimiterValue: kotlin.String = ...): kotlin.String
public inline fun kotlin.CharSequence.sumBy(/*0*/ selector: (kotlin.Char) -> kotlin.Int): kotlin.Int
public inline fun kotlin.CharSequence.sumByDouble(/*0*/ selector: (kotlin.Char) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "sumOfDouble") @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.sumOf(/*0*/ selector: (kotlin.Char) -> kotlin.Double): kotlin.Double
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "sumOfInt") @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.sumOf(/*0*/ selector: (kotlin.Char) -> kotlin.Int): kotlin.Int
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "sumOfLong") @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.sumOf(/*0*/ selector: (kotlin.Char) -> kotlin.Long): kotlin.Long
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "sumOfUInt") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.sumOf(/*0*/ selector: (kotlin.Char) -> kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.4") @kotlin.OverloadResolutionByLambdaReturnType @kotlin.jvm.JvmName(name = "sumOfULong") @kotlin.ExperimentalUnsignedTypes @kotlin.internal.InlineOnly public inline fun kotlin.CharSequence.sumOf(/*0*/ selector: (kotlin.Char) -> kotlin.ULong): kotlin.ULong
public fun kotlin.CharSequence.take(/*0*/ n: kotlin.Int): kotlin.CharSequence
public fun kotlin.String.take(/*0*/ n: kotlin.Int): kotlin.String
public fun kotlin.CharSequence.takeLast(/*0*/ n: kotlin.Int): kotlin.CharSequence
public fun kotlin.String.takeLast(/*0*/ n: kotlin.Int): kotlin.String
public inline fun kotlin.CharSequence.takeLastWhile(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence
public inline fun kotlin.String.takeLastWhile(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String
public inline fun kotlin.CharSequence.takeWhile(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence
public inline fun kotlin.String.takeWhile(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String
@kotlin.internal.LowPriorityInOverloadResolution @kotlin.internal.InlineOnly public inline fun kotlin.String.toBoolean(): kotlin.Boolean
@kotlin.SinceKotlin(version = "1.4") public fun kotlin.String?.toBoolean(): kotlin.Boolean
public fun kotlin.String.toByte(): kotlin.Byte
public fun kotlin.String.toByte(/*0*/ radix: kotlin.Int): kotlin.Byte
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.String.toByteOrNull(): kotlin.Byte?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.String.toByteOrNull(/*0*/ radix: kotlin.Int): kotlin.Byte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.String.toCharArray(): kotlin.CharArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.String.toCharArray(/*0*/ startIndex: kotlin.Int = ..., /*1*/ endIndex: kotlin.Int = ...): kotlin.CharArray
@kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public inline fun kotlin.text.StringBuilder.toCharArray(/*0*/ destination: kotlin.CharArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.Unit
public fun </*0*/ C : kotlin.collections.MutableCollection<in kotlin.Char>> kotlin.CharSequence.toCollection(/*0*/ destination: C): C
public fun kotlin.String.toDouble(): kotlin.Double
public fun kotlin.String.toDoubleOrNull(): kotlin.Double?
@kotlin.internal.InlineOnly public inline fun kotlin.String.toFloat(): kotlin.Float
@kotlin.internal.InlineOnly public inline fun kotlin.String.toFloatOrNull(): kotlin.Float?
public fun kotlin.CharSequence.toHashSet(): kotlin.collections.HashSet<kotlin.Char>
public fun kotlin.String.toInt(): kotlin.Int
public fun kotlin.String.toInt(/*0*/ radix: kotlin.Int): kotlin.Int
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.String.toIntOrNull(): kotlin.Int?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.String.toIntOrNull(/*0*/ radix: kotlin.Int): kotlin.Int?
public fun kotlin.CharSequence.toList(): kotlin.collections.List<kotlin.Char>
public fun kotlin.String.toLong(): kotlin.Long
public fun kotlin.String.toLong(/*0*/ radix: kotlin.Int): kotlin.Long
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.String.toLongOrNull(): kotlin.Long?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.String.toLongOrNull(/*0*/ radix: kotlin.Int): kotlin.Long?
@kotlin.internal.InlineOnly public inline fun kotlin.Char.toLowerCase(): kotlin.Char
@kotlin.internal.InlineOnly public inline fun kotlin.String.toLowerCase(): kotlin.String
public fun kotlin.CharSequence.toMutableList(): kotlin.collections.MutableList<kotlin.Char>
@kotlin.internal.InlineOnly public inline fun kotlin.String.toRegex(): kotlin.text.Regex
@kotlin.internal.InlineOnly public inline fun kotlin.String.toRegex(/*0*/ options: kotlin.collections.Set<kotlin.text.RegexOption>): kotlin.text.Regex
@kotlin.internal.InlineOnly public inline fun kotlin.String.toRegex(/*0*/ option: kotlin.text.RegexOption): kotlin.text.Regex
public fun kotlin.CharSequence.toSet(): kotlin.collections.Set<kotlin.Char>
public fun kotlin.String.toShort(): kotlin.Short
public fun kotlin.String.toShort(/*0*/ radix: kotlin.Int): kotlin.Short
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.String.toShortOrNull(): kotlin.Short?
@kotlin.SinceKotlin(version = "1.1") public fun kotlin.String.toShortOrNull(/*0*/ radix: kotlin.Int): kotlin.Short?
@kotlin.SinceKotlin(version = "1.2") @kotlin.internal.InlineOnly public inline fun kotlin.Byte.toString(/*0*/ radix: kotlin.Int): kotlin.String
@kotlin.SinceKotlin(version = "1.2") public fun kotlin.Int.toString(/*0*/ radix: kotlin.Int): kotlin.String
@kotlin.SinceKotlin(version = "1.2") public fun kotlin.Long.toString(/*0*/ radix: kotlin.Int): kotlin.String
@kotlin.SinceKotlin(version = "1.2") @kotlin.internal.InlineOnly public inline fun kotlin.Short.toString(/*0*/ radix: kotlin.Int): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UByte.toString(/*0*/ radix: kotlin.Int): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UInt.toString(/*0*/ radix: kotlin.Int): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.ULong.toString(/*0*/ radix: kotlin.Int): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.UShort.toString(/*0*/ radix: kotlin.Int): kotlin.String
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toUByte(): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toUByte(/*0*/ radix: kotlin.Int): kotlin.UByte
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toUByteOrNull(): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toUByteOrNull(/*0*/ radix: kotlin.Int): kotlin.UByte?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toUInt(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toUInt(/*0*/ radix: kotlin.Int): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toUIntOrNull(): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toUIntOrNull(/*0*/ radix: kotlin.Int): kotlin.UInt?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toULong(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toULong(/*0*/ radix: kotlin.Int): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toULongOrNull(): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toULongOrNull(/*0*/ radix: kotlin.Int): kotlin.ULong?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toUShort(): kotlin.UShort
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toUShort(/*0*/ radix: kotlin.Int): kotlin.UShort
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toUShortOrNull(): kotlin.UShort?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.String.toUShortOrNull(/*0*/ radix: kotlin.Int): kotlin.UShort?
@kotlin.internal.InlineOnly public inline fun kotlin.Char.toUpperCase(): kotlin.Char
@kotlin.internal.InlineOnly public inline fun kotlin.String.toUpperCase(): kotlin.String
public fun kotlin.CharSequence.trim(): kotlin.CharSequence
public inline fun kotlin.CharSequence.trim(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence
public fun kotlin.CharSequence.trim(/*0*/ vararg chars: kotlin.Char /*kotlin.CharArray*/): kotlin.CharSequence
@kotlin.internal.InlineOnly public inline fun kotlin.String.trim(): kotlin.String
public inline fun kotlin.String.trim(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String
public fun kotlin.String.trim(/*0*/ vararg chars: kotlin.Char /*kotlin.CharArray*/): kotlin.String
public fun kotlin.CharSequence.trimEnd(): kotlin.CharSequence
public inline fun kotlin.CharSequence.trimEnd(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence
public fun kotlin.CharSequence.trimEnd(/*0*/ vararg chars: kotlin.Char /*kotlin.CharArray*/): kotlin.CharSequence
@kotlin.internal.InlineOnly public inline fun kotlin.String.trimEnd(): kotlin.String
public inline fun kotlin.String.trimEnd(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String
public fun kotlin.String.trimEnd(/*0*/ vararg chars: kotlin.Char /*kotlin.CharArray*/): kotlin.String
public fun kotlin.String.trimIndent(): kotlin.String
public fun kotlin.String.trimMargin(/*0*/ marginPrefix: kotlin.String = ...): kotlin.String
public fun kotlin.CharSequence.trimStart(): kotlin.CharSequence
public inline fun kotlin.CharSequence.trimStart(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence
public fun kotlin.CharSequence.trimStart(/*0*/ vararg chars: kotlin.Char /*kotlin.CharArray*/): kotlin.CharSequence
@kotlin.internal.InlineOnly public inline fun kotlin.String.trimStart(): kotlin.String
public inline fun kotlin.String.trimStart(/*0*/ predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String
public fun kotlin.String.trimStart(/*0*/ vararg chars: kotlin.Char /*kotlin.CharArray*/): kotlin.String
@kotlin.SinceKotlin(version = "1.2") public fun kotlin.CharSequence.windowed(/*0*/ size: kotlin.Int, /*1*/ step: kotlin.Int = ..., /*2*/ partialWindows: kotlin.Boolean = ...): kotlin.collections.List<kotlin.String>
@kotlin.SinceKotlin(version = "1.2") public fun </*0*/ R> kotlin.CharSequence.windowed(/*0*/ size: kotlin.Int, /*1*/ step: kotlin.Int = ..., /*2*/ partialWindows: kotlin.Boolean = ..., /*3*/ transform: (kotlin.CharSequence) -> R): kotlin.collections.List<R>
@kotlin.SinceKotlin(version = "1.2") public fun kotlin.CharSequence.windowedSequence(/*0*/ size: kotlin.Int, /*1*/ step: kotlin.Int = ..., /*2*/ partialWindows: kotlin.Boolean = ...): kotlin.sequences.Sequence<kotlin.String>
@kotlin.SinceKotlin(version = "1.2") public fun </*0*/ R> kotlin.CharSequence.windowedSequence(/*0*/ size: kotlin.Int, /*1*/ step: kotlin.Int = ..., /*2*/ partialWindows: kotlin.Boolean = ..., /*3*/ transform: (kotlin.CharSequence) -> R): kotlin.sequences.Sequence<R>
public fun kotlin.CharSequence.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.Char>>
public infix fun kotlin.CharSequence.zip(/*0*/ other: kotlin.CharSequence): kotlin.collections.List<kotlin.Pair<kotlin.Char, kotlin.Char>>
public inline fun </*0*/ V> kotlin.CharSequence.zip(/*0*/ other: kotlin.CharSequence, /*1*/ transform: (a: kotlin.Char, b: kotlin.Char) -> V): kotlin.collections.List<V>
@kotlin.SinceKotlin(version = "1.2") public fun kotlin.CharSequence.zipWithNext(): kotlin.collections.List<kotlin.Pair<kotlin.Char, kotlin.Char>>
@kotlin.SinceKotlin(version = "1.2") public inline fun </*0*/ R> kotlin.CharSequence.zipWithNext(/*0*/ transform: (a: kotlin.Char, b: kotlin.Char) -> R): kotlin.collections.List<R>

public interface Appendable {
    public abstract fun append(/*0*/ value: kotlin.Char): kotlin.text.Appendable
    public abstract fun append(/*0*/ value: kotlin.CharSequence?): kotlin.text.Appendable
    public abstract fun append(/*0*/ value: kotlin.CharSequence?, /*1*/ startIndex: kotlin.Int, /*2*/ endIndex: kotlin.Int): kotlin.text.Appendable
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public open class CharacterCodingException : kotlin.Exception {
    public constructor CharacterCodingException()
    /*primary*/ public constructor CharacterCodingException(/*0*/ message: kotlin.String?)
}

public final data class MatchGroup {
    /*primary*/ public constructor MatchGroup(/*0*/ value: kotlin.String)
    public final val value: kotlin.String
        public final fun <get-value>(): kotlin.String
    public final operator /*synthesized*/ fun component1(): kotlin.String
    public final /*synthesized*/ fun copy(/*0*/ value: kotlin.String = ...): kotlin.text.MatchGroup
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    public open override /*1*/ /*synthesized*/ fun toString(): kotlin.String
}

public interface MatchGroupCollection : kotlin.collections.Collection<kotlin.text.MatchGroup?> {
    public abstract operator fun get(/*0*/ index: kotlin.Int): kotlin.text.MatchGroup?
}

@kotlin.SinceKotlin(version = "1.1") public interface MatchNamedGroupCollection : kotlin.text.MatchGroupCollection {
    public abstract operator fun get(/*0*/ name: kotlin.String): kotlin.text.MatchGroup?
}

public interface MatchResult {
    public open val destructured: kotlin.text.MatchResult.Destructured
        public open fun <get-destructured>(): kotlin.text.MatchResult.Destructured
    public abstract val groupValues: kotlin.collections.List<kotlin.String>
        public abstract fun <get-groupValues>(): kotlin.collections.List<kotlin.String>
    public abstract val groups: kotlin.text.MatchGroupCollection
        public abstract fun <get-groups>(): kotlin.text.MatchGroupCollection
    public abstract val range: kotlin.ranges.IntRange
        public abstract fun <get-range>(): kotlin.ranges.IntRange
    public abstract val value: kotlin.String
        public abstract fun <get-value>(): kotlin.String
    public abstract fun next(): kotlin.text.MatchResult?

    public final class Destructured {
        public final val match: kotlin.text.MatchResult
            public final fun <get-match>(): kotlin.text.MatchResult
        @kotlin.internal.InlineOnly public final inline operator fun component1(): kotlin.String
        @kotlin.internal.InlineOnly public final inline operator fun component10(): kotlin.String
        @kotlin.internal.InlineOnly public final inline operator fun component2(): kotlin.String
        @kotlin.internal.InlineOnly public final inline operator fun component3(): kotlin.String
        @kotlin.internal.InlineOnly public final inline operator fun component4(): kotlin.String
        @kotlin.internal.InlineOnly public final inline operator fun component5(): kotlin.String
        @kotlin.internal.InlineOnly public final inline operator fun component6(): kotlin.String
        @kotlin.internal.InlineOnly public final inline operator fun component7(): kotlin.String
        @kotlin.internal.InlineOnly public final inline operator fun component8(): kotlin.String
        @kotlin.internal.InlineOnly public final inline operator fun component9(): kotlin.String
        public final fun toList(): kotlin.collections.List<kotlin.String>
    }
}

public final class Regex {
    public constructor Regex(/*0*/ pattern: kotlin.String)
    /*primary*/ public constructor Regex(/*0*/ pattern: kotlin.String, /*1*/ options: kotlin.collections.Set<kotlin.text.RegexOption>)
    public constructor Regex(/*0*/ pattern: kotlin.String, /*1*/ option: kotlin.text.RegexOption)
    public final val options: kotlin.collections.Set<kotlin.text.RegexOption>
        public final fun <get-options>(): kotlin.collections.Set<kotlin.text.RegexOption>
    public final val pattern: kotlin.String
        public final fun <get-pattern>(): kotlin.String
    public final fun containsMatchIn(/*0*/ input: kotlin.CharSequence): kotlin.Boolean
    public final fun find(/*0*/ input: kotlin.CharSequence, /*1*/ startIndex: kotlin.Int = ...): kotlin.text.MatchResult?
    public final fun findAll(/*0*/ input: kotlin.CharSequence, /*1*/ startIndex: kotlin.Int = ...): kotlin.sequences.Sequence<kotlin.text.MatchResult>
    public final fun matchEntire(/*0*/ input: kotlin.CharSequence): kotlin.text.MatchResult?
    public final infix fun matches(/*0*/ input: kotlin.CharSequence): kotlin.Boolean
    public final inline fun replace(/*0*/ input: kotlin.CharSequence, /*1*/ transform: (kotlin.text.MatchResult) -> kotlin.CharSequence): kotlin.String
    public final fun replace(/*0*/ input: kotlin.CharSequence, /*1*/ replacement: kotlin.String): kotlin.String
    public final fun replaceFirst(/*0*/ input: kotlin.CharSequence, /*1*/ replacement: kotlin.String): kotlin.String
    public final fun split(/*0*/ input: kotlin.CharSequence, /*1*/ limit: kotlin.Int = ...): kotlin.collections.List<kotlin.String>
    public open override /*1*/ fun toString(): kotlin.String

    public companion object Companion {
        public final fun escape(/*0*/ literal: kotlin.String): kotlin.String
        public final fun escapeReplacement(/*0*/ literal: kotlin.String): kotlin.String
        public final fun fromLiteral(/*0*/ literal: kotlin.String): kotlin.text.Regex
    }
}

public final enum class RegexOption : kotlin.Enum<kotlin.text.RegexOption> {
    enum entry IGNORE_CASE

    enum entry MULTILINE

    public final val value: kotlin.String
        public final fun <get-value>(): kotlin.String

    // Static members
    public final /*synthesized*/ fun valueOf(/*0*/ value: kotlin.String): kotlin.text.RegexOption
    public final /*synthesized*/ fun values(): kotlin.Array<kotlin.text.RegexOption>
}

public final class StringBuilder : kotlin.text.Appendable, kotlin.CharSequence {
    public constructor StringBuilder()
    public constructor StringBuilder(/*0*/ content: kotlin.CharSequence)
    public constructor StringBuilder(/*0*/ capacity: kotlin.Int)
    /*primary*/ public constructor StringBuilder(/*0*/ content: kotlin.String)
    public open override /*1*/ val length: kotlin.Int
        public open override /*1*/ fun <get-length>(): kotlin.Int
    public final fun append(/*0*/ value: kotlin.Any?): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.3") public final fun append(/*0*/ value: kotlin.Boolean): kotlin.text.StringBuilder
    public open override /*1*/ fun append(/*0*/ value: kotlin.Char): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun append(/*0*/ value: kotlin.CharArray): kotlin.text.StringBuilder
    public open override /*1*/ fun append(/*0*/ value: kotlin.CharSequence?): kotlin.text.StringBuilder
    public open override /*1*/ fun append(/*0*/ value: kotlin.CharSequence?, /*1*/ startIndex: kotlin.Int, /*2*/ endIndex: kotlin.Int): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.3") public final fun append(/*0*/ value: kotlin.String?): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun appendRange(/*0*/ value: kotlin.CharArray, /*1*/ startIndex: kotlin.Int, /*2*/ endIndex: kotlin.Int): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun appendRange(/*0*/ value: kotlin.CharSequence, /*1*/ startIndex: kotlin.Int, /*2*/ endIndex: kotlin.Int): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun capacity(): kotlin.Int
    @kotlin.SinceKotlin(version = "1.3") public final fun clear(): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun deleteAt(/*0*/ index: kotlin.Int): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun deleteRange(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun ensureCapacity(/*0*/ minimumCapacity: kotlin.Int): kotlin.Unit
    public open override /*1*/ fun get(/*0*/ index: kotlin.Int): kotlin.Char
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun indexOf(/*0*/ string: kotlin.String): kotlin.Int
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun indexOf(/*0*/ string: kotlin.String, /*1*/ startIndex: kotlin.Int): kotlin.Int
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun insert(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Any?): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun insert(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Boolean): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun insert(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Char): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun insert(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.CharArray): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun insert(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.CharSequence?): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun insert(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.String?): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun insertRange(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.CharArray, /*2*/ startIndex: kotlin.Int, /*3*/ endIndex: kotlin.Int): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun insertRange(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.CharSequence, /*2*/ startIndex: kotlin.Int, /*3*/ endIndex: kotlin.Int): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun lastIndexOf(/*0*/ string: kotlin.String): kotlin.Int
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun lastIndexOf(/*0*/ string: kotlin.String, /*1*/ startIndex: kotlin.Int): kotlin.Int
    public final fun reverse(): kotlin.text.StringBuilder
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Char): kotlin.Unit
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun setLength(/*0*/ newLength: kotlin.Int): kotlin.Unit
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun setRange(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int, /*2*/ value: kotlin.String): kotlin.text.StringBuilder
    public open override /*1*/ fun subSequence(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int): kotlin.CharSequence
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun substring(/*0*/ startIndex: kotlin.Int): kotlin.String
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun substring(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int): kotlin.String
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun toCharArray(/*0*/ destination: kotlin.CharArray, /*1*/ destinationOffset: kotlin.Int = ..., /*2*/ startIndex: kotlin.Int = ..., /*3*/ endIndex: kotlin.Int = ...): kotlin.Unit
    public open override /*2*/ fun toString(): kotlin.String
    @kotlin.SinceKotlin(version = "1.4") @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class}) public final fun trimToSize(): kotlin.Unit
}

public object Typography {
    public const final val almostEqual: kotlin.Char = \u2248 ('≈')
        public final fun <get-almostEqual>(): kotlin.Char
    public const final val amp: kotlin.Char = \u0026 ('&')
        public final fun <get-amp>(): kotlin.Char
    public const final val bullet: kotlin.Char = \u2022 ('•')
        public final fun <get-bullet>(): kotlin.Char
    public const final val cent: kotlin.Char = \u00A2 ('¢')
        public final fun <get-cent>(): kotlin.Char
    public const final val copyright: kotlin.Char = \u00A9 ('©')
        public final fun <get-copyright>(): kotlin.Char
    public const final val dagger: kotlin.Char = \u2020 ('†')
        public final fun <get-dagger>(): kotlin.Char
    public const final val degree: kotlin.Char = \u00B0 ('°')
        public final fun <get-degree>(): kotlin.Char
    public const final val dollar: kotlin.Char = \u0024 ('$')
        public final fun <get-dollar>(): kotlin.Char
    public const final val doubleDagger: kotlin.Char = \u2021 ('‡')
        public final fun <get-doubleDagger>(): kotlin.Char
    public const final val doublePrime: kotlin.Char = \u2033 ('″')
        public final fun <get-doublePrime>(): kotlin.Char
    public const final val ellipsis: kotlin.Char = \u2026 ('…')
        public final fun <get-ellipsis>(): kotlin.Char
    public const final val euro: kotlin.Char = \u20AC ('€')
        public final fun <get-euro>(): kotlin.Char
    public const final val greater: kotlin.Char = \u003E ('>')
        public final fun <get-greater>(): kotlin.Char
    public const final val greaterOrEqual: kotlin.Char = \u2265 ('≥')
        public final fun <get-greaterOrEqual>(): kotlin.Char
    public const final val half: kotlin.Char = \u00BD ('½')
        public final fun <get-half>(): kotlin.Char
    public const final val leftDoubleQuote: kotlin.Char = \u201C ('“')
        public final fun <get-leftDoubleQuote>(): kotlin.Char
    public const final val leftGuillemete: kotlin.Char = \u00AB ('«')
        public final fun <get-leftGuillemete>(): kotlin.Char
    public const final val leftSingleQuote: kotlin.Char = \u2018 ('‘')
        public final fun <get-leftSingleQuote>(): kotlin.Char
    public const final val less: kotlin.Char = \u003C ('<')
        public final fun <get-less>(): kotlin.Char
    public const final val lessOrEqual: kotlin.Char = \u2264 ('≤')
        public final fun <get-lessOrEqual>(): kotlin.Char
    public const final val lowDoubleQuote: kotlin.Char = \u201E ('„')
        public final fun <get-lowDoubleQuote>(): kotlin.Char
    public const final val lowSingleQuote: kotlin.Char = \u201A ('‚')
        public final fun <get-lowSingleQuote>(): kotlin.Char
    public const final val mdash: kotlin.Char = \u2014 ('—')
        public final fun <get-mdash>(): kotlin.Char
    public const final val middleDot: kotlin.Char = \u00B7 ('·')
        public final fun <get-middleDot>(): kotlin.Char
    public const final val nbsp: kotlin.Char = \u00A0 (' ')
        public final fun <get-nbsp>(): kotlin.Char
    public const final val ndash: kotlin.Char = \u2013 ('–')
        public final fun <get-ndash>(): kotlin.Char
    public const final val notEqual: kotlin.Char = \u2260 ('≠')
        public final fun <get-notEqual>(): kotlin.Char
    public const final val paragraph: kotlin.Char = \u00B6 ('¶')
        public final fun <get-paragraph>(): kotlin.Char
    public const final val plusMinus: kotlin.Char = \u00B1 ('±')
        public final fun <get-plusMinus>(): kotlin.Char
    public const final val pound: kotlin.Char = \u00A3 ('£')
        public final fun <get-pound>(): kotlin.Char
    public const final val prime: kotlin.Char = \u2032 ('′')
        public final fun <get-prime>(): kotlin.Char
    public const final val quote: kotlin.Char = \u0022 ('"')
        public final fun <get-quote>(): kotlin.Char
    public const final val registered: kotlin.Char = \u00AE ('®')
        public final fun <get-registered>(): kotlin.Char
    public const final val rightDoubleQuote: kotlin.Char = \u201D ('”')
        public final fun <get-rightDoubleQuote>(): kotlin.Char
    public const final val rightGuillemete: kotlin.Char = \u00BB ('»')
        public final fun <get-rightGuillemete>(): kotlin.Char
    public const final val rightSingleQuote: kotlin.Char = \u2019 ('’')
        public final fun <get-rightSingleQuote>(): kotlin.Char
    public const final val section: kotlin.Char = \u00A7 ('§')
        public final fun <get-section>(): kotlin.Char
    public const final val times: kotlin.Char = \u00D7 ('×')
        public final fun <get-times>(): kotlin.Char
    public const final val tm: kotlin.Char = \u2122 ('™')
        public final fun <get-tm>(): kotlin.Char
}