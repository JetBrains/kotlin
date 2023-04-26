@kotlin.SinceKotlin(version = "1.2")
public val kotlin.String.Companion.CASE_INSENSITIVE_ORDER: kotlin.Comparator<kotlin.String> { get; }

@kotlin.SinceKotlin(version = "1.5")
public val kotlin.Char.category: kotlin.text.CharCategory { get; }

public val kotlin.CharSequence.indices: kotlin.ranges.IntRange { get; }

public val kotlin.CharSequence.lastIndex: kotlin.Int { get; }

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
@kotlin.internal.InlineOnly
public inline fun HexFormat(builderAction: kotlin.text.HexFormat.Builder.() -> kotlin.Unit): kotlin.text.HexFormat

@kotlin.SinceKotlin(version = "1.2")
@kotlin.Deprecated(message = "Use CharArray.concatToString() instead", replaceWith = kotlin.ReplaceWith(expression = "chars.concatToString()", imports = {}))
@kotlin.DeprecatedSinceKotlin(errorSince = "1.5", warningSince = "1.4")
public fun String(chars: kotlin.CharArray): kotlin.String

@kotlin.SinceKotlin(version = "1.2")
@kotlin.Deprecated(message = "Use CharArray.concatToString(startIndex, endIndex) instead", replaceWith = kotlin.ReplaceWith(expression = "chars.concatToString(offset, offset + length)", imports = {}))
@kotlin.DeprecatedSinceKotlin(errorSince = "1.5", warningSince = "1.4")
public fun String(chars: kotlin.CharArray, offset: kotlin.Int, length: kotlin.Int): kotlin.String

@kotlin.SinceKotlin(version = "1.1")
@kotlin.internal.InlineOnly
public inline fun buildString(capacity: kotlin.Int, builderAction: kotlin.text.StringBuilder.() -> kotlin.Unit): kotlin.String

@kotlin.internal.InlineOnly
public inline fun buildString(builderAction: kotlin.text.StringBuilder.() -> kotlin.Unit): kotlin.String

public inline fun kotlin.CharSequence.all(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Boolean

public fun kotlin.CharSequence.any(): kotlin.Boolean

public inline fun kotlin.CharSequence.any(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Boolean

public fun <T : kotlin.text.Appendable> T.append(vararg value: kotlin.CharSequence?): T

@kotlin.Deprecated(level = DeprecationLevel.WARNING, message = "Use append(value: Any?) instead", replaceWith = kotlin.ReplaceWith(expression = "append(value = obj)", imports = {}))
@kotlin.internal.InlineOnly
public inline fun kotlin.text.StringBuilder.append(obj: kotlin.Any?): kotlin.text.StringBuilder

public fun kotlin.text.StringBuilder.append(vararg value: kotlin.Any?): kotlin.text.StringBuilder

public fun kotlin.text.StringBuilder.append(vararg value: kotlin.String?): kotlin.text.StringBuilder

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use appendRange instead.", replaceWith = kotlin.ReplaceWith(expression = "this.appendRange(str, offset, offset + len)", imports = {}))
@kotlin.internal.InlineOnly
public inline fun kotlin.text.StringBuilder.append(str: kotlin.CharArray, offset: kotlin.Int, len: kotlin.Int): kotlin.text.StringBuilder

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.text.Appendable.appendLine(): kotlin.text.Appendable

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.text.Appendable.appendLine(value: kotlin.Char): kotlin.text.Appendable

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.text.Appendable.appendLine(value: kotlin.CharSequence?): kotlin.text.Appendable

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.text.StringBuilder.appendLine(): kotlin.text.StringBuilder

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.text.StringBuilder.appendLine(value: kotlin.Any?): kotlin.text.StringBuilder

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.text.StringBuilder.appendLine(value: kotlin.Boolean): kotlin.text.StringBuilder

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.text.StringBuilder.appendLine(value: kotlin.Char): kotlin.text.StringBuilder

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.text.StringBuilder.appendLine(value: kotlin.CharArray): kotlin.text.StringBuilder

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.text.StringBuilder.appendLine(value: kotlin.CharSequence?): kotlin.text.StringBuilder

@kotlin.SinceKotlin(version = "1.4")
@kotlin.internal.InlineOnly
public inline fun kotlin.text.StringBuilder.appendLine(value: kotlin.String?): kotlin.text.StringBuilder

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun <T : kotlin.text.Appendable> T.appendRange(value: kotlin.CharSequence, startIndex: kotlin.Int, endIndex: kotlin.Int): T

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.text.StringBuilder.appendRange(value: kotlin.CharArray, startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.text.StringBuilder

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.text.StringBuilder.appendRange(value: kotlin.CharSequence, startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.text.StringBuilder

public fun kotlin.CharSequence.asIterable(): kotlin.collections.Iterable<kotlin.Char>

public fun kotlin.CharSequence.asSequence(): kotlin.sequences.Sequence<kotlin.Char>

public inline fun <K, V> kotlin.CharSequence.associate(transform: (kotlin.Char) -> kotlin.Pair<K, V>): kotlin.collections.Map<K, V>

public inline fun <K> kotlin.CharSequence.associateBy(keySelector: (kotlin.Char) -> K): kotlin.collections.Map<K, kotlin.Char>

public inline fun <K, V> kotlin.CharSequence.associateBy(keySelector: (kotlin.Char) -> K, valueTransform: (kotlin.Char) -> V): kotlin.collections.Map<K, V>

public inline fun <K, M : kotlin.collections.MutableMap<in K, in kotlin.Char>> kotlin.CharSequence.associateByTo(destination: M, keySelector: (kotlin.Char) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.CharSequence.associateByTo(destination: M, keySelector: (kotlin.Char) -> K, valueTransform: (kotlin.Char) -> V): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, in V>> kotlin.CharSequence.associateTo(destination: M, transform: (kotlin.Char) -> kotlin.Pair<K, V>): M

@kotlin.SinceKotlin(version = "1.3")
public inline fun <V> kotlin.CharSequence.associateWith(valueSelector: (kotlin.Char) -> V): kotlin.collections.Map<kotlin.Char, V>

@kotlin.SinceKotlin(version = "1.3")
public inline fun <V, M : kotlin.collections.MutableMap<in kotlin.Char, in V>> kotlin.CharSequence.associateWithTo(destination: M, valueSelector: (kotlin.Char) -> V): M

@kotlin.Deprecated(message = "Use replaceFirstChar instead.", replaceWith = kotlin.ReplaceWith(expression = "replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
public fun kotlin.String.capitalize(): kotlin.String

@kotlin.SinceKotlin(version = "1.2")
public fun kotlin.CharSequence.chunked(size: kotlin.Int): kotlin.collections.List<kotlin.String>

@kotlin.SinceKotlin(version = "1.2")
public fun <R> kotlin.CharSequence.chunked(size: kotlin.Int, transform: (kotlin.CharSequence) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.2")
public fun kotlin.CharSequence.chunkedSequence(size: kotlin.Int): kotlin.sequences.Sequence<kotlin.String>

@kotlin.SinceKotlin(version = "1.2")
public fun <R> kotlin.CharSequence.chunkedSequence(size: kotlin.Int, transform: (kotlin.CharSequence) -> R): kotlin.sequences.Sequence<R>

@kotlin.SinceKotlin(version = "1.3")
public inline fun kotlin.text.StringBuilder.clear(): kotlin.text.StringBuilder

public fun kotlin.CharSequence.commonPrefixWith(other: kotlin.CharSequence, ignoreCase: kotlin.Boolean = ...): kotlin.String

public fun kotlin.CharSequence.commonSuffixWith(other: kotlin.CharSequence, ignoreCase: kotlin.Boolean = ...): kotlin.String

@kotlin.SinceKotlin(version = "1.2")
public fun kotlin.String.compareTo(other: kotlin.String, ignoreCase: kotlin.Boolean = ...): kotlin.Int

@kotlin.Deprecated(message = "Use String.plus() instead", replaceWith = kotlin.ReplaceWith(expression = "this + str", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.6")
@kotlin.internal.InlineOnly
public inline fun kotlin.String.concat(str: kotlin.String): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.CharArray.concatToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.CharArray.concatToString(startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.String

public operator fun kotlin.CharSequence.contains(char: kotlin.Char, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

public operator fun kotlin.CharSequence.contains(other: kotlin.CharSequence, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline operator fun kotlin.CharSequence.contains(regex: kotlin.text.Regex): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
public infix fun kotlin.CharSequence?.contentEquals(other: kotlin.CharSequence?): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
public fun kotlin.CharSequence?.contentEquals(other: kotlin.CharSequence?, ignoreCase: kotlin.Boolean): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.count(): kotlin.Int

public inline fun kotlin.CharSequence.count(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Int

@kotlin.Deprecated(message = "Use replaceFirstChar instead.", replaceWith = kotlin.ReplaceWith(expression = "replaceFirstChar { it.lowercase() }", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
public fun kotlin.String.decapitalize(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.ByteArray.decodeToString(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.ByteArray.decodeToString(startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ..., throwOnInvalidSequence: kotlin.Boolean = ...): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.text.StringBuilder.deleteAt(index: kotlin.Int): kotlin.text.StringBuilder

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.text.StringBuilder.deleteRange(startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.text.StringBuilder

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Int.digitToChar(): kotlin.Char

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Int.digitToChar(radix: kotlin.Int): kotlin.Char

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Char.digitToInt(): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Char.digitToInt(radix: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Char.digitToIntOrNull(): kotlin.Int?

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Char.digitToIntOrNull(radix: kotlin.Int): kotlin.Int?

public fun kotlin.CharSequence.drop(n: kotlin.Int): kotlin.CharSequence

public fun kotlin.String.drop(n: kotlin.Int): kotlin.String

public fun kotlin.CharSequence.dropLast(n: kotlin.Int): kotlin.CharSequence

public fun kotlin.String.dropLast(n: kotlin.Int): kotlin.String

public inline fun kotlin.CharSequence.dropLastWhile(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence

public inline fun kotlin.String.dropLastWhile(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String

public inline fun kotlin.CharSequence.dropWhile(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence

public inline fun kotlin.String.dropWhile(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String

public fun kotlin.CharSequence.elementAt(index: kotlin.Int): kotlin.Char

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.elementAtOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Char): kotlin.Char

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.elementAtOrNull(index: kotlin.Int): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.String.encodeToByteArray(): kotlin.ByteArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.String.encodeToByteArray(startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ..., throwOnInvalidSequence: kotlin.Boolean = ...): kotlin.ByteArray

public fun kotlin.CharSequence.endsWith(char: kotlin.Char, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

public fun kotlin.CharSequence.endsWith(suffix: kotlin.CharSequence, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

public fun kotlin.String.endsWith(suffix: kotlin.String, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

public fun kotlin.Char.equals(other: kotlin.Char, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

public fun kotlin.String?.equals(other: kotlin.String?, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

public inline fun kotlin.CharSequence.filter(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence

public inline fun kotlin.String.filter(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String

public inline fun kotlin.CharSequence.filterIndexed(predicate: (index: kotlin.Int, kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence

public inline fun kotlin.String.filterIndexed(predicate: (index: kotlin.Int, kotlin.Char) -> kotlin.Boolean): kotlin.String

public inline fun <C : kotlin.text.Appendable> kotlin.CharSequence.filterIndexedTo(destination: C, predicate: (index: kotlin.Int, kotlin.Char) -> kotlin.Boolean): C

public inline fun kotlin.CharSequence.filterNot(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence

public inline fun kotlin.String.filterNot(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String

public inline fun <C : kotlin.text.Appendable> kotlin.CharSequence.filterNotTo(destination: C, predicate: (kotlin.Char) -> kotlin.Boolean): C

public inline fun <C : kotlin.text.Appendable> kotlin.CharSequence.filterTo(destination: C, predicate: (kotlin.Char) -> kotlin.Boolean): C

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.find(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?

public fun kotlin.CharSequence.findAnyOf(strings: kotlin.collections.Collection<kotlin.String>, startIndex: kotlin.Int = ..., ignoreCase: kotlin.Boolean = ...): kotlin.Pair<kotlin.Int, kotlin.String>?

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.findLast(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?

public fun kotlin.CharSequence.findLastAnyOf(strings: kotlin.collections.Collection<kotlin.String>, startIndex: kotlin.Int = ..., ignoreCase: kotlin.Boolean = ...): kotlin.Pair<kotlin.Int, kotlin.String>?

public fun kotlin.CharSequence.first(): kotlin.Char

public inline fun kotlin.CharSequence.first(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Any> kotlin.CharSequence.firstNotNullOf(transform: (kotlin.Char) -> R?): R

@kotlin.SinceKotlin(version = "1.5")
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Any> kotlin.CharSequence.firstNotNullOfOrNull(transform: (kotlin.Char) -> R?): R?

public fun kotlin.CharSequence.firstOrNull(): kotlin.Char?

public inline fun kotlin.CharSequence.firstOrNull(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?

public inline fun <R> kotlin.CharSequence.flatMap(transform: (kotlin.Char) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterable")
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharSequence.flatMapIndexed(transform: (index: kotlin.Int, kotlin.Char) -> kotlin.collections.Iterable<R>): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "flatMapIndexedIterableTo")
@kotlin.internal.InlineOnly
public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.CharSequence.flatMapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Char) -> kotlin.collections.Iterable<R>): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.CharSequence.flatMapTo(destination: C, transform: (kotlin.Char) -> kotlin.collections.Iterable<R>): C

public inline fun <R> kotlin.CharSequence.fold(initial: R, operation: (acc: R, kotlin.Char) -> R): R

public inline fun <R> kotlin.CharSequence.foldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Char) -> R): R

public inline fun <R> kotlin.CharSequence.foldRight(initial: R, operation: (kotlin.Char, acc: R) -> R): R

public inline fun <R> kotlin.CharSequence.foldRightIndexed(initial: R, operation: (index: kotlin.Int, kotlin.Char, acc: R) -> R): R

public inline fun kotlin.CharSequence.forEach(action: (kotlin.Char) -> kotlin.Unit): kotlin.Unit

public inline fun kotlin.CharSequence.forEachIndexed(action: (index: kotlin.Int, kotlin.Char) -> kotlin.Unit): kotlin.Unit

@kotlin.SinceKotlin(version = "1.7")
public operator fun kotlin.text.MatchGroupCollection.get(name: kotlin.String): kotlin.text.MatchGroup?

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.getOrElse(index: kotlin.Int, defaultValue: (kotlin.Int) -> kotlin.Char): kotlin.Char

public fun kotlin.CharSequence.getOrNull(index: kotlin.Int): kotlin.Char?

public inline fun <K> kotlin.CharSequence.groupBy(keySelector: (kotlin.Char) -> K): kotlin.collections.Map<K, kotlin.collections.List<kotlin.Char>>

public inline fun <K, V> kotlin.CharSequence.groupBy(keySelector: (kotlin.Char) -> K, valueTransform: (kotlin.Char) -> V): kotlin.collections.Map<K, kotlin.collections.List<V>>

public inline fun <K, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<kotlin.Char>>> kotlin.CharSequence.groupByTo(destination: M, keySelector: (kotlin.Char) -> K): M

public inline fun <K, V, M : kotlin.collections.MutableMap<in K, kotlin.collections.MutableList<V>>> kotlin.CharSequence.groupByTo(destination: M, keySelector: (kotlin.Char) -> K, valueTransform: (kotlin.Char) -> V): M

@kotlin.SinceKotlin(version = "1.1")
public inline fun <K> kotlin.CharSequence.groupingBy(crossinline keySelector: (kotlin.Char) -> K): kotlin.collections.Grouping<kotlin.Char, K>

public fun kotlin.CharSequence.hasSurrogatePairAt(index: kotlin.Int): kotlin.Boolean

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
public fun kotlin.String.hexToByte(format: kotlin.text.HexFormat = ...): kotlin.Byte

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
public fun kotlin.String.hexToByteArray(format: kotlin.text.HexFormat = ...): kotlin.ByteArray

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
public fun kotlin.String.hexToInt(format: kotlin.text.HexFormat = ...): kotlin.Int

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
public fun kotlin.String.hexToLong(format: kotlin.text.HexFormat = ...): kotlin.Long

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
public fun kotlin.String.hexToShort(format: kotlin.text.HexFormat = ...): kotlin.Short

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
@kotlin.internal.InlineOnly
public inline fun kotlin.String.hexToUByte(format: kotlin.text.HexFormat = ...): kotlin.UByte

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.String.hexToUByteArray(format: kotlin.text.HexFormat = ...): kotlin.UByteArray

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
@kotlin.internal.InlineOnly
public inline fun kotlin.String.hexToUInt(format: kotlin.text.HexFormat = ...): kotlin.UInt

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
@kotlin.internal.InlineOnly
public inline fun kotlin.String.hexToULong(format: kotlin.text.HexFormat = ...): kotlin.ULong

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
@kotlin.internal.InlineOnly
public inline fun kotlin.String.hexToUShort(format: kotlin.text.HexFormat = ...): kotlin.UShort

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.CharSequence, R> C.ifBlank(defaultValue: () -> R): R where C : R

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <C : kotlin.CharSequence, R> C.ifEmpty(defaultValue: () -> R): R where C : R

public fun kotlin.CharSequence.indexOf(char: kotlin.Char, startIndex: kotlin.Int = ..., ignoreCase: kotlin.Boolean = ...): kotlin.Int

public fun kotlin.CharSequence.indexOf(string: kotlin.String, startIndex: kotlin.Int = ..., ignoreCase: kotlin.Boolean = ...): kotlin.Int

public fun kotlin.CharSequence.indexOfAny(chars: kotlin.CharArray, startIndex: kotlin.Int = ..., ignoreCase: kotlin.Boolean = ...): kotlin.Int

public fun kotlin.CharSequence.indexOfAny(strings: kotlin.collections.Collection<kotlin.String>, startIndex: kotlin.Int = ..., ignoreCase: kotlin.Boolean = ...): kotlin.Int

public inline fun kotlin.CharSequence.indexOfFirst(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Int

public inline fun kotlin.CharSequence.indexOfLast(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.text.StringBuilder.insertRange(index: kotlin.Int, value: kotlin.CharArray, startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.text.StringBuilder

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.text.StringBuilder.insertRange(index: kotlin.Int, value: kotlin.CharSequence, startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.text.StringBuilder

public fun kotlin.CharSequence.isBlank(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
public fun kotlin.Char.isDefined(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
public fun kotlin.Char.isDigit(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.isEmpty(): kotlin.Boolean

public fun kotlin.Char.isHighSurrogate(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
public fun kotlin.Char.isISOControl(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
public fun kotlin.Char.isLetter(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
public fun kotlin.Char.isLetterOrDigit(): kotlin.Boolean

public fun kotlin.Char.isLowSurrogate(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
public fun kotlin.Char.isLowerCase(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.isNotBlank(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.isNotEmpty(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence?.isNullOrBlank(): kotlin.Boolean

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence?.isNullOrEmpty(): kotlin.Boolean

public fun kotlin.Char.isSurrogate(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
public fun kotlin.Char.isTitleCase(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
public fun kotlin.Char.isUpperCase(): kotlin.Boolean

public fun kotlin.Char.isWhitespace(): kotlin.Boolean

public operator fun kotlin.CharSequence.iterator(): kotlin.collections.CharIterator

public fun kotlin.CharSequence.last(): kotlin.Char

public inline fun kotlin.CharSequence.last(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char

public fun kotlin.CharSequence.lastIndexOf(char: kotlin.Char, startIndex: kotlin.Int = ..., ignoreCase: kotlin.Boolean = ...): kotlin.Int

public fun kotlin.CharSequence.lastIndexOf(string: kotlin.String, startIndex: kotlin.Int = ..., ignoreCase: kotlin.Boolean = ...): kotlin.Int

public fun kotlin.CharSequence.lastIndexOfAny(chars: kotlin.CharArray, startIndex: kotlin.Int = ..., ignoreCase: kotlin.Boolean = ...): kotlin.Int

public fun kotlin.CharSequence.lastIndexOfAny(strings: kotlin.collections.Collection<kotlin.String>, startIndex: kotlin.Int = ..., ignoreCase: kotlin.Boolean = ...): kotlin.Int

public fun kotlin.CharSequence.lastOrNull(): kotlin.Char?

public inline fun kotlin.CharSequence.lastOrNull(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?

public fun kotlin.CharSequence.lineSequence(): kotlin.sequences.Sequence<kotlin.String>

public fun kotlin.CharSequence.lines(): kotlin.collections.List<kotlin.String>

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Char.lowercase(): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.String.lowercase(): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Char.lowercaseChar(): kotlin.Char

public inline fun <R> kotlin.CharSequence.map(transform: (kotlin.Char) -> R): kotlin.collections.List<R>

public inline fun <R> kotlin.CharSequence.mapIndexed(transform: (index: kotlin.Int, kotlin.Char) -> R): kotlin.collections.List<R>

public inline fun <R : kotlin.Any> kotlin.CharSequence.mapIndexedNotNull(transform: (index: kotlin.Int, kotlin.Char) -> R?): kotlin.collections.List<R>

public inline fun <R : kotlin.Any, C : kotlin.collections.MutableCollection<in R>> kotlin.CharSequence.mapIndexedNotNullTo(destination: C, transform: (index: kotlin.Int, kotlin.Char) -> R?): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.CharSequence.mapIndexedTo(destination: C, transform: (index: kotlin.Int, kotlin.Char) -> R): C

public inline fun <R : kotlin.Any> kotlin.CharSequence.mapNotNull(transform: (kotlin.Char) -> R?): kotlin.collections.List<R>

public inline fun <R : kotlin.Any, C : kotlin.collections.MutableCollection<in R>> kotlin.CharSequence.mapNotNullTo(destination: C, transform: (kotlin.Char) -> R?): C

public inline fun <R, C : kotlin.collections.MutableCollection<in R>> kotlin.CharSequence.mapTo(destination: C, transform: (kotlin.Char) -> R): C

@kotlin.Deprecated(message = "Use Regex.findAll() instead or invoke matches() on String dynamically: this.asDynamic().match(regex)")
@kotlin.DeprecatedSinceKotlin(warningSince = "1.6")
@kotlin.internal.InlineOnly
public inline fun kotlin.String.match(regex: kotlin.String): kotlin.Array<kotlin.String>?

@kotlin.internal.InlineOnly
public inline infix fun kotlin.CharSequence.matches(regex: kotlin.text.Regex): kotlin.Boolean

@kotlin.Deprecated(message = "Use Regex.matches() instead", replaceWith = kotlin.ReplaceWith(expression = "regex.toRegex().matches(this)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.6")
public fun kotlin.String.matches(regex: kotlin.String): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "maxOrThrow")
public fun kotlin.CharSequence.max(): kotlin.Char

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "maxByOrThrow")
public inline fun <R : kotlin.Comparable<R>> kotlin.CharSequence.maxBy(selector: (kotlin.Char) -> R): kotlin.Char

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.CharSequence.maxByOrNull(selector: (kotlin.Char) -> R): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.CharSequence.maxOf(selector: (kotlin.Char) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.maxOf(selector: (kotlin.Char) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.maxOf(selector: (kotlin.Char) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.CharSequence.maxOfOrNull(selector: (kotlin.Char) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.maxOfOrNull(selector: (kotlin.Char) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.maxOfOrNull(selector: (kotlin.Char) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharSequence.maxOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Char) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharSequence.maxOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Char) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharSequence.maxOrNull(): kotlin.Char?

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "maxWithOrThrow")
public fun kotlin.CharSequence.maxWith(comparator: kotlin.Comparator<in kotlin.Char>): kotlin.Char

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharSequence.maxWithOrNull(comparator: kotlin.Comparator<in kotlin.Char>): kotlin.Char?

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "minOrThrow")
public fun kotlin.CharSequence.min(): kotlin.Char

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "minByOrThrow")
public inline fun <R : kotlin.Comparable<R>> kotlin.CharSequence.minBy(selector: (kotlin.Char) -> R): kotlin.Char

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R : kotlin.Comparable<R>> kotlin.CharSequence.minByOrNull(selector: (kotlin.Char) -> R): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.CharSequence.minOf(selector: (kotlin.Char) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.minOf(selector: (kotlin.Char) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.minOf(selector: (kotlin.Char) -> kotlin.Float): kotlin.Float

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R : kotlin.Comparable<R>> kotlin.CharSequence.minOfOrNull(selector: (kotlin.Char) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.minOfOrNull(selector: (kotlin.Char) -> kotlin.Double): kotlin.Double?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.minOfOrNull(selector: (kotlin.Char) -> kotlin.Float): kotlin.Float?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharSequence.minOfWith(comparator: kotlin.Comparator<in R>, selector: (kotlin.Char) -> R): R

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.internal.InlineOnly
public inline fun <R> kotlin.CharSequence.minOfWithOrNull(comparator: kotlin.Comparator<in R>, selector: (kotlin.Char) -> R): R?

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharSequence.minOrNull(): kotlin.Char?

@kotlin.SinceKotlin(version = "1.7")
@kotlin.jvm.JvmName(name = "minWithOrThrow")
public fun kotlin.CharSequence.minWith(comparator: kotlin.Comparator<in kotlin.Char>): kotlin.Char

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.CharSequence.minWithOrNull(comparator: kotlin.Comparator<in kotlin.Char>): kotlin.Char?

public fun kotlin.CharSequence.none(): kotlin.Boolean

public inline fun kotlin.CharSequence.none(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.1")
public inline fun <S : kotlin.CharSequence> S.onEach(action: (kotlin.Char) -> kotlin.Unit): S

@kotlin.SinceKotlin(version = "1.4")
public inline fun <S : kotlin.CharSequence> S.onEachIndexed(action: (index: kotlin.Int, kotlin.Char) -> kotlin.Unit): S

@kotlin.internal.InlineOnly
public inline fun kotlin.String?.orEmpty(): kotlin.String

public fun kotlin.CharSequence.padEnd(length: kotlin.Int, padChar: kotlin.Char = ...): kotlin.CharSequence

public fun kotlin.String.padEnd(length: kotlin.Int, padChar: kotlin.Char = ...): kotlin.String

public fun kotlin.CharSequence.padStart(length: kotlin.Int, padChar: kotlin.Char = ...): kotlin.CharSequence

public fun kotlin.String.padStart(length: kotlin.Int, padChar: kotlin.Char = ...): kotlin.String

public inline fun kotlin.CharSequence.partition(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Pair<kotlin.CharSequence, kotlin.CharSequence>

public inline fun kotlin.String.partition(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Pair<kotlin.String, kotlin.String>

@kotlin.internal.InlineOnly
public inline operator fun kotlin.Char.plus(other: kotlin.String): kotlin.String

public fun kotlin.String.prependIndent(indent: kotlin.String = ...): kotlin.String

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.random(): kotlin.Char

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.CharSequence.random(random: kotlin.random.Random): kotlin.Char

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.randomOrNull(): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.CharSequence.randomOrNull(random: kotlin.random.Random): kotlin.Char?

public inline fun kotlin.CharSequence.reduce(operation: (acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char

public inline fun kotlin.CharSequence.reduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.CharSequence.reduceIndexedOrNull(operation: (index: kotlin.Int, acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.CharSequence.reduceOrNull(operation: (acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.Char?

public inline fun kotlin.CharSequence.reduceRight(operation: (kotlin.Char, acc: kotlin.Char) -> kotlin.Char): kotlin.Char

public inline fun kotlin.CharSequence.reduceRightIndexed(operation: (index: kotlin.Int, kotlin.Char, acc: kotlin.Char) -> kotlin.Char): kotlin.Char

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.CharSequence.reduceRightIndexedOrNull(operation: (index: kotlin.Int, kotlin.Char, acc: kotlin.Char) -> kotlin.Char): kotlin.Char?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.CharSequence.reduceRightOrNull(operation: (kotlin.Char, acc: kotlin.Char) -> kotlin.Char): kotlin.Char?

public fun kotlin.CharSequence.regionMatches(thisOffset: kotlin.Int, other: kotlin.CharSequence, otherOffset: kotlin.Int, length: kotlin.Int, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.9")
public fun kotlin.String.regionMatches(thisOffset: kotlin.Int, other: kotlin.String, otherOffset: kotlin.Int, length: kotlin.Int, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

public fun kotlin.CharSequence.removePrefix(prefix: kotlin.CharSequence): kotlin.CharSequence

public fun kotlin.String.removePrefix(prefix: kotlin.CharSequence): kotlin.String

public fun kotlin.CharSequence.removeRange(startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.CharSequence

public fun kotlin.CharSequence.removeRange(range: kotlin.ranges.IntRange): kotlin.CharSequence

@kotlin.internal.InlineOnly
public inline fun kotlin.String.removeRange(startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.String

@kotlin.internal.InlineOnly
public inline fun kotlin.String.removeRange(range: kotlin.ranges.IntRange): kotlin.String

public fun kotlin.CharSequence.removeSuffix(suffix: kotlin.CharSequence): kotlin.CharSequence

public fun kotlin.String.removeSuffix(suffix: kotlin.CharSequence): kotlin.String

public fun kotlin.CharSequence.removeSurrounding(delimiter: kotlin.CharSequence): kotlin.CharSequence

public fun kotlin.CharSequence.removeSurrounding(prefix: kotlin.CharSequence, suffix: kotlin.CharSequence): kotlin.CharSequence

public fun kotlin.String.removeSurrounding(delimiter: kotlin.CharSequence): kotlin.String

public fun kotlin.String.removeSurrounding(prefix: kotlin.CharSequence, suffix: kotlin.CharSequence): kotlin.String

public fun kotlin.CharSequence.repeat(n: kotlin.Int): kotlin.String

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.replace(regex: kotlin.text.Regex, noinline transform: (kotlin.text.MatchResult) -> kotlin.CharSequence): kotlin.String

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.replace(regex: kotlin.text.Regex, replacement: kotlin.String): kotlin.String

public fun kotlin.String.replace(oldChar: kotlin.Char, newChar: kotlin.Char, ignoreCase: kotlin.Boolean = ...): kotlin.String

public fun kotlin.String.replace(oldValue: kotlin.String, newValue: kotlin.String, ignoreCase: kotlin.Boolean = ...): kotlin.String

public fun kotlin.String.replaceAfter(delimiter: kotlin.Char, replacement: kotlin.String, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.replaceAfter(delimiter: kotlin.String, replacement: kotlin.String, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.replaceAfterLast(delimiter: kotlin.Char, replacement: kotlin.String, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.replaceAfterLast(delimiter: kotlin.String, replacement: kotlin.String, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.replaceBefore(delimiter: kotlin.Char, replacement: kotlin.String, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.replaceBefore(delimiter: kotlin.String, replacement: kotlin.String, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.replaceBeforeLast(delimiter: kotlin.Char, replacement: kotlin.String, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.replaceBeforeLast(delimiter: kotlin.String, replacement: kotlin.String, missingDelimiterValue: kotlin.String = ...): kotlin.String

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.replaceFirst(regex: kotlin.text.Regex, replacement: kotlin.String): kotlin.String

public fun kotlin.String.replaceFirst(oldChar: kotlin.Char, newChar: kotlin.Char, ignoreCase: kotlin.Boolean = ...): kotlin.String

public fun kotlin.String.replaceFirst(oldValue: kotlin.String, newValue: kotlin.String, ignoreCase: kotlin.Boolean = ...): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "replaceFirstCharWithChar")
@kotlin.internal.InlineOnly
public inline fun kotlin.String.replaceFirstChar(transform: (kotlin.Char) -> kotlin.Char): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "replaceFirstCharWithCharSequence")
@kotlin.internal.InlineOnly
public inline fun kotlin.String.replaceFirstChar(transform: (kotlin.Char) -> kotlin.CharSequence): kotlin.String

public fun kotlin.String.replaceIndent(newIndent: kotlin.String = ...): kotlin.String

public fun kotlin.String.replaceIndentByMargin(newIndent: kotlin.String = ..., marginPrefix: kotlin.String = ...): kotlin.String

public fun kotlin.CharSequence.replaceRange(startIndex: kotlin.Int, endIndex: kotlin.Int, replacement: kotlin.CharSequence): kotlin.CharSequence

public fun kotlin.CharSequence.replaceRange(range: kotlin.ranges.IntRange, replacement: kotlin.CharSequence): kotlin.CharSequence

@kotlin.internal.InlineOnly
public inline fun kotlin.String.replaceRange(startIndex: kotlin.Int, endIndex: kotlin.Int, replacement: kotlin.CharSequence): kotlin.String

@kotlin.internal.InlineOnly
public inline fun kotlin.String.replaceRange(range: kotlin.ranges.IntRange, replacement: kotlin.CharSequence): kotlin.String

public fun kotlin.CharSequence.reversed(): kotlin.CharSequence

@kotlin.internal.InlineOnly
public inline fun kotlin.String.reversed(): kotlin.String

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R> kotlin.CharSequence.runningFold(initial: R, operation: (acc: R, kotlin.Char) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
public inline fun <R> kotlin.CharSequence.runningFoldIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Char) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.CharSequence.runningReduce(operation: (acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.collections.List<kotlin.Char>

@kotlin.SinceKotlin(version = "1.4")
public inline fun kotlin.CharSequence.runningReduceIndexed(operation: (index: kotlin.Int, acc: kotlin.Char, kotlin.Char) -> kotlin.Char): kotlin.collections.List<kotlin.Char>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun <R> kotlin.CharSequence.scan(initial: R, operation: (acc: R, kotlin.Char) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun <R> kotlin.CharSequence.scanIndexed(initial: R, operation: (index: kotlin.Int, acc: R, kotlin.Char) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline operator fun kotlin.text.StringBuilder.set(index: kotlin.Int, value: kotlin.Char): kotlin.Unit

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.text.StringBuilder.setRange(startIndex: kotlin.Int, endIndex: kotlin.Int, value: kotlin.String): kotlin.text.StringBuilder

public fun kotlin.CharSequence.single(): kotlin.Char

public inline fun kotlin.CharSequence.single(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char

public fun kotlin.CharSequence.singleOrNull(): kotlin.Char?

public inline fun kotlin.CharSequence.singleOrNull(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.Char?

public fun kotlin.CharSequence.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.CharSequence

public fun kotlin.CharSequence.slice(indices: kotlin.ranges.IntRange): kotlin.CharSequence

@kotlin.internal.InlineOnly
public inline fun kotlin.String.slice(indices: kotlin.collections.Iterable<kotlin.Int>): kotlin.String

public fun kotlin.String.slice(indices: kotlin.ranges.IntRange): kotlin.String

public fun kotlin.CharSequence.split(vararg delimiters: kotlin.String, ignoreCase: kotlin.Boolean = ..., limit: kotlin.Int = ...): kotlin.collections.List<kotlin.String>

public fun kotlin.CharSequence.split(vararg delimiters: kotlin.Char, ignoreCase: kotlin.Boolean = ..., limit: kotlin.Int = ...): kotlin.collections.List<kotlin.String>

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.split(regex: kotlin.text.Regex, limit: kotlin.Int = ...): kotlin.collections.List<kotlin.String>

public fun kotlin.CharSequence.splitToSequence(vararg delimiters: kotlin.String, ignoreCase: kotlin.Boolean = ..., limit: kotlin.Int = ...): kotlin.sequences.Sequence<kotlin.String>

public fun kotlin.CharSequence.splitToSequence(vararg delimiters: kotlin.Char, ignoreCase: kotlin.Boolean = ..., limit: kotlin.Int = ...): kotlin.sequences.Sequence<kotlin.String>

@kotlin.SinceKotlin(version = "1.6")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.splitToSequence(regex: kotlin.text.Regex, limit: kotlin.Int = ...): kotlin.sequences.Sequence<kotlin.String>

public fun kotlin.CharSequence.startsWith(char: kotlin.Char, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

public fun kotlin.CharSequence.startsWith(prefix: kotlin.CharSequence, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

public fun kotlin.CharSequence.startsWith(prefix: kotlin.CharSequence, startIndex: kotlin.Int, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

public fun kotlin.String.startsWith(prefix: kotlin.String, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

public fun kotlin.String.startsWith(prefix: kotlin.String, startIndex: kotlin.Int, ignoreCase: kotlin.Boolean = ...): kotlin.Boolean

public fun kotlin.CharSequence.subSequence(range: kotlin.ranges.IntRange): kotlin.CharSequence

@kotlin.internal.InlineOnly
@kotlin.Deprecated(message = "Use parameters named startIndex and endIndex.", replaceWith = kotlin.ReplaceWith(expression = "subSequence(startIndex = start, endIndex = end)", imports = {}))
public inline fun kotlin.String.subSequence(start: kotlin.Int, end: kotlin.Int): kotlin.CharSequence

@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.substring(startIndex: kotlin.Int, endIndex: kotlin.Int = ...): kotlin.String

public fun kotlin.CharSequence.substring(range: kotlin.ranges.IntRange): kotlin.String

@kotlin.internal.InlineOnly
public inline fun kotlin.String.substring(startIndex: kotlin.Int): kotlin.String

@kotlin.internal.InlineOnly
public inline fun kotlin.String.substring(startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.String

public fun kotlin.String.substring(range: kotlin.ranges.IntRange): kotlin.String

public fun kotlin.String.substringAfter(delimiter: kotlin.Char, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.substringAfter(delimiter: kotlin.String, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.substringAfterLast(delimiter: kotlin.Char, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.substringAfterLast(delimiter: kotlin.String, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.substringBefore(delimiter: kotlin.Char, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.substringBefore(delimiter: kotlin.String, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.substringBeforeLast(delimiter: kotlin.Char, missingDelimiterValue: kotlin.String = ...): kotlin.String

public fun kotlin.String.substringBeforeLast(delimiter: kotlin.String, missingDelimiterValue: kotlin.String = ...): kotlin.String

@kotlin.Deprecated(message = "Use sumOf instead.", replaceWith = kotlin.ReplaceWith(expression = "this.sumOf(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
public inline fun kotlin.CharSequence.sumBy(selector: (kotlin.Char) -> kotlin.Int): kotlin.Int

@kotlin.Deprecated(message = "Use sumOf instead.", replaceWith = kotlin.ReplaceWith(expression = "this.sumOf(selector)", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
public inline fun kotlin.CharSequence.sumByDouble(selector: (kotlin.Char) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfDouble")
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.sumOf(selector: (kotlin.Char) -> kotlin.Double): kotlin.Double

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfInt")
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.sumOf(selector: (kotlin.Char) -> kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.4")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfLong")
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.sumOf(selector: (kotlin.Char) -> kotlin.Long): kotlin.Long

@kotlin.SinceKotlin(version = "1.5")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfUInt")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.sumOf(selector: (kotlin.Char) -> kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName(name = "sumOfULong")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.CharSequence.sumOf(selector: (kotlin.Char) -> kotlin.ULong): kotlin.ULong

public fun kotlin.CharSequence.take(n: kotlin.Int): kotlin.CharSequence

public fun kotlin.String.take(n: kotlin.Int): kotlin.String

public fun kotlin.CharSequence.takeLast(n: kotlin.Int): kotlin.CharSequence

public fun kotlin.String.takeLast(n: kotlin.Int): kotlin.String

public inline fun kotlin.CharSequence.takeLastWhile(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence

public inline fun kotlin.String.takeLastWhile(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String

public inline fun kotlin.CharSequence.takeWhile(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence

public inline fun kotlin.String.takeWhile(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
public fun kotlin.Char.titlecase(): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
public fun kotlin.Char.titlecaseChar(): kotlin.Char

@kotlin.SinceKotlin(version = "1.4")
public fun kotlin.String?.toBoolean(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
public fun kotlin.String.toBooleanStrict(): kotlin.Boolean

@kotlin.SinceKotlin(version = "1.5")
public fun kotlin.String.toBooleanStrictOrNull(): kotlin.Boolean?

public fun kotlin.String.toByte(): kotlin.Byte

public fun kotlin.String.toByte(radix: kotlin.Int): kotlin.Byte

@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.String.toByteOrNull(): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.String.toByteOrNull(radix: kotlin.Int): kotlin.Byte?

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.String.toCharArray(): kotlin.CharArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.String.toCharArray(startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.CharArray

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public inline fun kotlin.text.StringBuilder.toCharArray(destination: kotlin.CharArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.Unit

public fun <C : kotlin.collections.MutableCollection<in kotlin.Char>> kotlin.CharSequence.toCollection(destination: C): C

public fun kotlin.String.toDouble(): kotlin.Double

public fun kotlin.String.toDoubleOrNull(): kotlin.Double?

@kotlin.internal.InlineOnly
public inline fun kotlin.String.toFloat(): kotlin.Float

@kotlin.internal.InlineOnly
public inline fun kotlin.String.toFloatOrNull(): kotlin.Float?

public fun kotlin.CharSequence.toHashSet(): kotlin.collections.HashSet<kotlin.Char>

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
public fun kotlin.Byte.toHexString(format: kotlin.text.HexFormat = ...): kotlin.String

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
public fun kotlin.ByteArray.toHexString(startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ..., format: kotlin.text.HexFormat = ...): kotlin.String

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
public fun kotlin.ByteArray.toHexString(format: kotlin.text.HexFormat = ...): kotlin.String

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
public fun kotlin.Int.toHexString(format: kotlin.text.HexFormat = ...): kotlin.String

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
public fun kotlin.Long.toHexString(format: kotlin.text.HexFormat = ...): kotlin.String

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
public fun kotlin.Short.toHexString(format: kotlin.text.HexFormat = ...): kotlin.String

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
@kotlin.internal.InlineOnly
public inline fun kotlin.UByte.toHexString(format: kotlin.text.HexFormat = ...): kotlin.String

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.toHexString(startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ..., format: kotlin.text.HexFormat = ...): kotlin.String

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
@kotlin.ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun kotlin.UByteArray.toHexString(format: kotlin.text.HexFormat = ...): kotlin.String

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
@kotlin.internal.InlineOnly
public inline fun kotlin.UInt.toHexString(format: kotlin.text.HexFormat = ...): kotlin.String

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
@kotlin.internal.InlineOnly
public inline fun kotlin.ULong.toHexString(format: kotlin.text.HexFormat = ...): kotlin.String

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
@kotlin.internal.InlineOnly
public inline fun kotlin.UShort.toHexString(format: kotlin.text.HexFormat = ...): kotlin.String

public fun kotlin.String.toInt(): kotlin.Int

public fun kotlin.String.toInt(radix: kotlin.Int): kotlin.Int

@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.String.toIntOrNull(): kotlin.Int?

@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.String.toIntOrNull(radix: kotlin.Int): kotlin.Int?

public fun kotlin.CharSequence.toList(): kotlin.collections.List<kotlin.Char>

public fun kotlin.String.toLong(): kotlin.Long

public fun kotlin.String.toLong(radix: kotlin.Int): kotlin.Long

@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.String.toLongOrNull(): kotlin.Long?

@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.String.toLongOrNull(radix: kotlin.Int): kotlin.Long?

@kotlin.Deprecated(message = "Use lowercaseChar() instead.", replaceWith = kotlin.ReplaceWith(expression = "lowercaseChar()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Char.toLowerCase(): kotlin.Char

@kotlin.Deprecated(message = "Use lowercase() instead.", replaceWith = kotlin.ReplaceWith(expression = "lowercase()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.String.toLowerCase(): kotlin.String

public fun kotlin.CharSequence.toMutableList(): kotlin.collections.MutableList<kotlin.Char>

@kotlin.internal.InlineOnly
public inline fun kotlin.String.toRegex(): kotlin.text.Regex

@kotlin.internal.InlineOnly
public inline fun kotlin.String.toRegex(options: kotlin.collections.Set<kotlin.text.RegexOption>): kotlin.text.Regex

@kotlin.internal.InlineOnly
public inline fun kotlin.String.toRegex(option: kotlin.text.RegexOption): kotlin.text.Regex

public fun kotlin.CharSequence.toSet(): kotlin.collections.Set<kotlin.Char>

public fun kotlin.String.toShort(): kotlin.Short

public fun kotlin.String.toShort(radix: kotlin.Int): kotlin.Short

@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.String.toShortOrNull(): kotlin.Short?

@kotlin.SinceKotlin(version = "1.1")
public fun kotlin.String.toShortOrNull(radix: kotlin.Int): kotlin.Short?

@kotlin.SinceKotlin(version = "1.2")
@kotlin.internal.InlineOnly
public inline fun kotlin.Byte.toString(radix: kotlin.Int): kotlin.String

@kotlin.SinceKotlin(version = "1.2")
public fun kotlin.Int.toString(radix: kotlin.Int): kotlin.String

@kotlin.SinceKotlin(version = "1.2")
public fun kotlin.Long.toString(radix: kotlin.Int): kotlin.String

@kotlin.SinceKotlin(version = "1.2")
@kotlin.internal.InlineOnly
public inline fun kotlin.Short.toString(radix: kotlin.Int): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.UByte.toString(radix: kotlin.Int): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.UInt.toString(radix: kotlin.Int): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.ULong.toString(radix: kotlin.Int): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.UShort.toString(radix: kotlin.Int): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toUByte(): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toUByte(radix: kotlin.Int): kotlin.UByte

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toUByteOrNull(): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toUByteOrNull(radix: kotlin.Int): kotlin.UByte?

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toUInt(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toUInt(radix: kotlin.Int): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toUIntOrNull(): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toUIntOrNull(radix: kotlin.Int): kotlin.UInt?

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toULong(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toULong(radix: kotlin.Int): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toULongOrNull(): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toULongOrNull(radix: kotlin.Int): kotlin.ULong?

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toUShort(): kotlin.UShort

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toUShort(radix: kotlin.Int): kotlin.UShort

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toUShortOrNull(): kotlin.UShort?

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.String.toUShortOrNull(radix: kotlin.Int): kotlin.UShort?

@kotlin.Deprecated(message = "Use uppercaseChar() instead.", replaceWith = kotlin.ReplaceWith(expression = "uppercaseChar()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.Char.toUpperCase(): kotlin.Char

@kotlin.Deprecated(message = "Use uppercase() instead.", replaceWith = kotlin.ReplaceWith(expression = "uppercase()", imports = {}))
@kotlin.DeprecatedSinceKotlin(warningSince = "1.5")
@kotlin.internal.InlineOnly
public inline fun kotlin.String.toUpperCase(): kotlin.String

public fun kotlin.CharSequence.trim(): kotlin.CharSequence

public inline fun kotlin.CharSequence.trim(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence

public fun kotlin.CharSequence.trim(vararg chars: kotlin.Char): kotlin.CharSequence

@kotlin.internal.InlineOnly
public inline fun kotlin.String.trim(): kotlin.String

public inline fun kotlin.String.trim(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String

public fun kotlin.String.trim(vararg chars: kotlin.Char): kotlin.String

public fun kotlin.CharSequence.trimEnd(): kotlin.CharSequence

public inline fun kotlin.CharSequence.trimEnd(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence

public fun kotlin.CharSequence.trimEnd(vararg chars: kotlin.Char): kotlin.CharSequence

@kotlin.internal.InlineOnly
public inline fun kotlin.String.trimEnd(): kotlin.String

public inline fun kotlin.String.trimEnd(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String

public fun kotlin.String.trimEnd(vararg chars: kotlin.Char): kotlin.String

@kotlin.internal.IntrinsicConstEvaluation
public fun kotlin.String.trimIndent(): kotlin.String

@kotlin.internal.IntrinsicConstEvaluation
public fun kotlin.String.trimMargin(marginPrefix: kotlin.String = ...): kotlin.String

public fun kotlin.CharSequence.trimStart(): kotlin.CharSequence

public inline fun kotlin.CharSequence.trimStart(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.CharSequence

public fun kotlin.CharSequence.trimStart(vararg chars: kotlin.Char): kotlin.CharSequence

@kotlin.internal.InlineOnly
public inline fun kotlin.String.trimStart(): kotlin.String

public inline fun kotlin.String.trimStart(predicate: (kotlin.Char) -> kotlin.Boolean): kotlin.String

public fun kotlin.String.trimStart(vararg chars: kotlin.Char): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.Char.uppercase(): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
@kotlin.internal.InlineOnly
public inline fun kotlin.String.uppercase(): kotlin.String

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public fun kotlin.Char.uppercaseChar(): kotlin.Char

@kotlin.SinceKotlin(version = "1.2")
public fun kotlin.CharSequence.windowed(size: kotlin.Int, step: kotlin.Int = ..., partialWindows: kotlin.Boolean = ...): kotlin.collections.List<kotlin.String>

@kotlin.SinceKotlin(version = "1.2")
public fun <R> kotlin.CharSequence.windowed(size: kotlin.Int, step: kotlin.Int = ..., partialWindows: kotlin.Boolean = ..., transform: (kotlin.CharSequence) -> R): kotlin.collections.List<R>

@kotlin.SinceKotlin(version = "1.2")
public fun kotlin.CharSequence.windowedSequence(size: kotlin.Int, step: kotlin.Int = ..., partialWindows: kotlin.Boolean = ...): kotlin.sequences.Sequence<kotlin.String>

@kotlin.SinceKotlin(version = "1.2")
public fun <R> kotlin.CharSequence.windowedSequence(size: kotlin.Int, step: kotlin.Int = ..., partialWindows: kotlin.Boolean = ..., transform: (kotlin.CharSequence) -> R): kotlin.sequences.Sequence<R>

public fun kotlin.CharSequence.withIndex(): kotlin.collections.Iterable<kotlin.collections.IndexedValue<kotlin.Char>>

public infix fun kotlin.CharSequence.zip(other: kotlin.CharSequence): kotlin.collections.List<kotlin.Pair<kotlin.Char, kotlin.Char>>

public inline fun <V> kotlin.CharSequence.zip(other: kotlin.CharSequence, transform: (a: kotlin.Char, b: kotlin.Char) -> V): kotlin.collections.List<V>

@kotlin.SinceKotlin(version = "1.2")
public fun kotlin.CharSequence.zipWithNext(): kotlin.collections.List<kotlin.Pair<kotlin.Char, kotlin.Char>>

@kotlin.SinceKotlin(version = "1.2")
public inline fun <R> kotlin.CharSequence.zipWithNext(transform: (a: kotlin.Char, b: kotlin.Char) -> R): kotlin.collections.List<R>

public interface Appendable {
    public abstract fun append(value: kotlin.Char): kotlin.text.Appendable

    public abstract fun append(value: kotlin.CharSequence?): kotlin.text.Appendable

    public abstract fun append(value: kotlin.CharSequence?, startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.text.Appendable
}

@kotlin.SinceKotlin(version = "1.5")
public final enum class CharCategory : kotlin.Enum<kotlin.text.CharCategory> {
    enum entry UNASSIGNED

    enum entry UPPERCASE_LETTER

    enum entry LOWERCASE_LETTER

    enum entry TITLECASE_LETTER

    enum entry MODIFIER_LETTER

    enum entry OTHER_LETTER

    enum entry NON_SPACING_MARK

    enum entry ENCLOSING_MARK

    enum entry COMBINING_SPACING_MARK

    enum entry DECIMAL_DIGIT_NUMBER

    enum entry LETTER_NUMBER

    enum entry OTHER_NUMBER

    enum entry SPACE_SEPARATOR

    enum entry LINE_SEPARATOR

    enum entry PARAGRAPH_SEPARATOR

    enum entry CONTROL

    enum entry FORMAT

    enum entry PRIVATE_USE

    enum entry SURROGATE

    enum entry DASH_PUNCTUATION

    enum entry START_PUNCTUATION

    enum entry END_PUNCTUATION

    enum entry CONNECTOR_PUNCTUATION

    enum entry OTHER_PUNCTUATION

    enum entry MATH_SYMBOL

    enum entry CURRENCY_SYMBOL

    enum entry MODIFIER_SYMBOL

    enum entry OTHER_SYMBOL

    enum entry INITIAL_QUOTE_PUNCTUATION

    enum entry FINAL_QUOTE_PUNCTUATION

    public final val code: kotlin.String { get; }

    public final operator fun contains(char: kotlin.Char): kotlin.Boolean

    public companion object of CharCategory {
    }
}

@kotlin.SinceKotlin(version = "1.4")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
public open class CharacterCodingException : kotlin.Exception {
    public constructor CharacterCodingException()

    public constructor CharacterCodingException(message: kotlin.String?)
}

@kotlin.ExperimentalStdlibApi
@kotlin.SinceKotlin(version = "1.9")
public final class HexFormat {
    public final val bytes: kotlin.text.HexFormat.BytesHexFormat { get; }

    public final val number: kotlin.text.HexFormat.NumberHexFormat { get; }

    public final val upperCase: kotlin.Boolean { get; }

    public open override fun toString(): kotlin.String

    public final class Builder {
        public final val bytes: kotlin.text.HexFormat.BytesHexFormat.Builder { get; }

        public final val number: kotlin.text.HexFormat.NumberHexFormat.Builder { get; }

        public final var upperCase: kotlin.Boolean { get; set; }

        @kotlin.internal.InlineOnly
        public final inline fun bytes(builderAction: kotlin.text.HexFormat.BytesHexFormat.Builder.() -> kotlin.Unit): kotlin.Unit

        @kotlin.internal.InlineOnly
        public final inline fun number(builderAction: kotlin.text.HexFormat.NumberHexFormat.Builder.() -> kotlin.Unit): kotlin.Unit
    }

    public final class BytesHexFormat {
        public final val bytePrefix: kotlin.String { get; }

        public final val byteSeparator: kotlin.String { get; }

        public final val byteSuffix: kotlin.String { get; }

        public final val bytesPerGroup: kotlin.Int { get; }

        public final val bytesPerLine: kotlin.Int { get; }

        public final val groupSeparator: kotlin.String { get; }

        public open override fun toString(): kotlin.String

        public final class Builder {
            public final var bytePrefix: kotlin.String { get; set; }

            public final var byteSeparator: kotlin.String { get; set; }

            public final var byteSuffix: kotlin.String { get; set; }

            public final var bytesPerGroup: kotlin.Int { get; set; }

            public final var bytesPerLine: kotlin.Int { get; set; }

            public final var groupSeparator: kotlin.String { get; set; }
        }
    }

    public companion object of HexFormat {
        public final val Default: kotlin.text.HexFormat { get; }

        public final val UpperCase: kotlin.text.HexFormat { get; }
    }

    public final class NumberHexFormat {
        public final val prefix: kotlin.String { get; }

        public final val removeLeadingZeros: kotlin.Boolean { get; }

        public final val suffix: kotlin.String { get; }

        public open override fun toString(): kotlin.String

        public final class Builder {
            public final var prefix: kotlin.String { get; set; }

            public final var removeLeadingZeros: kotlin.Boolean { get; set; }

            public final var suffix: kotlin.String { get; set; }
        }
    }
}

public final data class MatchGroup {
    public constructor MatchGroup(value: kotlin.String)

    public final val value: kotlin.String { get; }

    public final operator fun component1(): kotlin.String

    public final fun copy(value: kotlin.String = ...): kotlin.text.MatchGroup

    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public open override fun toString(): kotlin.String
}

public interface MatchGroupCollection : kotlin.collections.Collection<kotlin.text.MatchGroup?> {
    public abstract operator fun get(index: kotlin.Int): kotlin.text.MatchGroup?
}

@kotlin.SinceKotlin(version = "1.1")
public interface MatchNamedGroupCollection : kotlin.text.MatchGroupCollection {
    public abstract operator fun get(name: kotlin.String): kotlin.text.MatchGroup?
}

public interface MatchResult {
    public open val destructured: kotlin.text.MatchResult.Destructured { get; }

    public abstract val groupValues: kotlin.collections.List<kotlin.String> { get; }

    public abstract val groups: kotlin.text.MatchGroupCollection { get; }

    public abstract val range: kotlin.ranges.IntRange { get; }

    public abstract val value: kotlin.String { get; }

    public abstract fun next(): kotlin.text.MatchResult?

    public final class Destructured {
        public final val match: kotlin.text.MatchResult { get; }

        @kotlin.internal.InlineOnly
        public final inline operator fun component1(): kotlin.String

        @kotlin.internal.InlineOnly
        public final inline operator fun component10(): kotlin.String

        @kotlin.internal.InlineOnly
        public final inline operator fun component2(): kotlin.String

        @kotlin.internal.InlineOnly
        public final inline operator fun component3(): kotlin.String

        @kotlin.internal.InlineOnly
        public final inline operator fun component4(): kotlin.String

        @kotlin.internal.InlineOnly
        public final inline operator fun component5(): kotlin.String

        @kotlin.internal.InlineOnly
        public final inline operator fun component6(): kotlin.String

        @kotlin.internal.InlineOnly
        public final inline operator fun component7(): kotlin.String

        @kotlin.internal.InlineOnly
        public final inline operator fun component8(): kotlin.String

        @kotlin.internal.InlineOnly
        public final inline operator fun component9(): kotlin.String

        public final fun toList(): kotlin.collections.List<kotlin.String>
    }
}

public final class Regex {
    public constructor Regex(pattern: kotlin.String)

    public constructor Regex(pattern: kotlin.String, options: kotlin.collections.Set<kotlin.text.RegexOption>)

    public constructor Regex(pattern: kotlin.String, option: kotlin.text.RegexOption)

    public final val options: kotlin.collections.Set<kotlin.text.RegexOption> { get; }

    public final val pattern: kotlin.String { get; }

    public final fun containsMatchIn(input: kotlin.CharSequence): kotlin.Boolean

    public final fun find(input: kotlin.CharSequence, startIndex: kotlin.Int = ...): kotlin.text.MatchResult?

    public final fun findAll(input: kotlin.CharSequence, startIndex: kotlin.Int = ...): kotlin.sequences.Sequence<kotlin.text.MatchResult>

    @kotlin.SinceKotlin(version = "1.7")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun matchAt(input: kotlin.CharSequence, index: kotlin.Int): kotlin.text.MatchResult?

    public final fun matchEntire(input: kotlin.CharSequence): kotlin.text.MatchResult?

    public final infix fun matches(input: kotlin.CharSequence): kotlin.Boolean

    @kotlin.SinceKotlin(version = "1.7")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun matchesAt(input: kotlin.CharSequence, index: kotlin.Int): kotlin.Boolean

    public final fun replace(input: kotlin.CharSequence, transform: (kotlin.text.MatchResult) -> kotlin.CharSequence): kotlin.String

    public final fun replace(input: kotlin.CharSequence, replacement: kotlin.String): kotlin.String

    public final fun replaceFirst(input: kotlin.CharSequence, replacement: kotlin.String): kotlin.String

    public final fun split(input: kotlin.CharSequence, limit: kotlin.Int = ...): kotlin.collections.List<kotlin.String>

    @kotlin.SinceKotlin(version = "1.6")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun splitToSequence(input: kotlin.CharSequence, limit: kotlin.Int = ...): kotlin.sequences.Sequence<kotlin.String>

    public open override fun toString(): kotlin.String

    public companion object of Regex {
        public final fun escape(literal: kotlin.String): kotlin.String

        public final fun escapeReplacement(literal: kotlin.String): kotlin.String

        public final fun fromLiteral(literal: kotlin.String): kotlin.text.Regex
    }
}

public final enum class RegexOption : kotlin.Enum<kotlin.text.RegexOption> {
    enum entry IGNORE_CASE

    enum entry MULTILINE

    public final val value: kotlin.String { get; }
}

public final class StringBuilder : kotlin.text.Appendable, kotlin.CharSequence {
    public constructor StringBuilder()

    public constructor StringBuilder(content: kotlin.CharSequence)

    public constructor StringBuilder(capacity: kotlin.Int)

    public constructor StringBuilder(content: kotlin.String)

    public open override val length: kotlin.Int { get; }

    public final fun append(value: kotlin.Any?): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.3")
    public final fun append(value: kotlin.Boolean): kotlin.text.StringBuilder

    public open override fun append(value: kotlin.Char): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun append(value: kotlin.CharArray): kotlin.text.StringBuilder

    public open override fun append(value: kotlin.CharSequence?): kotlin.text.StringBuilder

    public open override fun append(value: kotlin.CharSequence?, startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.3")
    public final fun append(value: kotlin.String?): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun appendRange(value: kotlin.CharArray, startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun appendRange(value: kotlin.CharSequence, startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.3")
    @kotlin.Deprecated(level = DeprecationLevel.WARNING, message = "Obtaining StringBuilder capacity is not supported in JS and common code.")
    public final fun capacity(): kotlin.Int

    @kotlin.SinceKotlin(version = "1.3")
    public final fun clear(): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun deleteAt(index: kotlin.Int): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun deleteRange(startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun ensureCapacity(minimumCapacity: kotlin.Int): kotlin.Unit

    public open override operator fun get(index: kotlin.Int): kotlin.Char

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun indexOf(string: kotlin.String): kotlin.Int

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun indexOf(string: kotlin.String, startIndex: kotlin.Int): kotlin.Int

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun insert(index: kotlin.Int, value: kotlin.Any?): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun insert(index: kotlin.Int, value: kotlin.Boolean): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun insert(index: kotlin.Int, value: kotlin.Char): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun insert(index: kotlin.Int, value: kotlin.CharArray): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun insert(index: kotlin.Int, value: kotlin.CharSequence?): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun insert(index: kotlin.Int, value: kotlin.String?): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun insertRange(index: kotlin.Int, value: kotlin.CharArray, startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun insertRange(index: kotlin.Int, value: kotlin.CharSequence, startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun lastIndexOf(string: kotlin.String): kotlin.Int

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun lastIndexOf(string: kotlin.String, startIndex: kotlin.Int): kotlin.Int

    public final fun reverse(): kotlin.text.StringBuilder

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final operator fun set(index: kotlin.Int, value: kotlin.Char): kotlin.Unit

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun setLength(newLength: kotlin.Int): kotlin.Unit

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun setRange(startIndex: kotlin.Int, endIndex: kotlin.Int, value: kotlin.String): kotlin.text.StringBuilder

    public open override fun subSequence(startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.CharSequence

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun substring(startIndex: kotlin.Int): kotlin.String

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun substring(startIndex: kotlin.Int, endIndex: kotlin.Int): kotlin.String

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun toCharArray(destination: kotlin.CharArray, destinationOffset: kotlin.Int = ..., startIndex: kotlin.Int = ..., endIndex: kotlin.Int = ...): kotlin.Unit

    public open override fun toString(): kotlin.String

    @kotlin.SinceKotlin(version = "1.4")
    @kotlin.WasExperimental(markerClass = {kotlin.ExperimentalStdlibApi::class})
    public final fun trimToSize(): kotlin.Unit
}

public object Typography {
    public const final val almostEqual: kotlin.Char = \u2248 ('≈') { get; }

    public const final val amp: kotlin.Char = \u0026 ('&') { get; }

    public const final val bullet: kotlin.Char = \u2022 ('•') { get; }

    public const final val cent: kotlin.Char = \u00A2 ('¢') { get; }

    public const final val copyright: kotlin.Char = \u00A9 ('©') { get; }

    public const final val dagger: kotlin.Char = \u2020 ('†') { get; }

    public const final val degree: kotlin.Char = \u00B0 ('°') { get; }

    public const final val dollar: kotlin.Char = \u0024 ('$') { get; }

    public const final val doubleDagger: kotlin.Char = \u2021 ('‡') { get; }

    public const final val doublePrime: kotlin.Char = \u2033 ('″') { get; }

    public const final val ellipsis: kotlin.Char = \u2026 ('…') { get; }

    public const final val euro: kotlin.Char = \u20AC ('€') { get; }

    public const final val greater: kotlin.Char = \u003E ('>') { get; }

    public const final val greaterOrEqual: kotlin.Char = \u2265 ('≥') { get; }

    public const final val half: kotlin.Char = \u00BD ('½') { get; }

    public const final val leftDoubleQuote: kotlin.Char = \u201C ('“') { get; }

    @kotlin.SinceKotlin(version = "1.6")
    public const final val leftGuillemet: kotlin.Char = \u00AB ('«') { get; }

    @kotlin.Deprecated(message = "This constant has a typo in the name. Use leftGuillemet instead.", replaceWith = kotlin.ReplaceWith(expression = "Typography.leftGuillemet", imports = {}))
    @kotlin.DeprecatedSinceKotlin(warningSince = "1.6")
    public const final val leftGuillemete: kotlin.Char = \u00AB ('«') { get; }

    public const final val leftSingleQuote: kotlin.Char = \u2018 ('‘') { get; }

    public const final val less: kotlin.Char = \u003C ('<') { get; }

    public const final val lessOrEqual: kotlin.Char = \u2264 ('≤') { get; }

    public const final val lowDoubleQuote: kotlin.Char = \u201E ('„') { get; }

    public const final val lowSingleQuote: kotlin.Char = \u201A ('‚') { get; }

    public const final val mdash: kotlin.Char = \u2014 ('—') { get; }

    public const final val middleDot: kotlin.Char = \u00B7 ('·') { get; }

    public const final val nbsp: kotlin.Char = \u00A0 (' ') { get; }

    public const final val ndash: kotlin.Char = \u2013 ('–') { get; }

    public const final val notEqual: kotlin.Char = \u2260 ('≠') { get; }

    public const final val paragraph: kotlin.Char = \u00B6 ('¶') { get; }

    public const final val plusMinus: kotlin.Char = \u00B1 ('±') { get; }

    public const final val pound: kotlin.Char = \u00A3 ('£') { get; }

    public const final val prime: kotlin.Char = \u2032 ('′') { get; }

    public const final val quote: kotlin.Char = \u0022 ('"') { get; }

    public const final val registered: kotlin.Char = \u00AE ('®') { get; }

    public const final val rightDoubleQuote: kotlin.Char = \u201D ('”') { get; }

    @kotlin.SinceKotlin(version = "1.6")
    public const final val rightGuillemet: kotlin.Char = \u00BB ('»') { get; }

    @kotlin.Deprecated(message = "This constant has a typo in the name. Use rightGuillemet instead.", replaceWith = kotlin.ReplaceWith(expression = "Typography.rightGuillemet", imports = {}))
    @kotlin.DeprecatedSinceKotlin(warningSince = "1.6")
    public const final val rightGuillemete: kotlin.Char = \u00BB ('»') { get; }

    public const final val rightSingleQuote: kotlin.Char = \u2019 ('’') { get; }

    public const final val section: kotlin.Char = \u00A7 ('§') { get; }

    public const final val times: kotlin.Char = \u00D7 ('×') { get; }

    public const final val tm: kotlin.Char = \u2122 ('™') { get; }
}