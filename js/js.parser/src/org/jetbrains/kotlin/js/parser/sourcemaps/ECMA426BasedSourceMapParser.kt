/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(kotlin.contracts.ExperimentalContracts::class)

package org.jetbrains.kotlin.js.parser.sourcemaps

import kotlin.contracts.contract
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.ParsingResult.Failure
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.ParsingResult.NoMatch
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.ParsingResult.Success

/**
 * Parses and validates source map files against the ECMA-426 specification.
 *
 * Used spec versions:
 * - Main ECMA-426 specification draft, as of March 2, 2026: https://tc39.es/ecma426/
 * - The scopes proposal draft specification, as of July 20, 2026: https://tc39.es/ecma426/branch/proposal-scopes/
 *
 * **Note:** The compiler doesn't generate a source map containing "sections", so the validation doesn't include a case with the ["Section 10: Index source map"](https://tc39.es/ecma426/#sec-index-source-map)
 *
 * @see <a href="https://tc39.es/ecma426/">ECMA-426: Source Map Format Specification</a>
 */
object ECMA426BasedSourceMapParser {
    //
    // 6. Base64 VLQ decoding and parsing

    /**
     * ```text
     * Vlq :
     *     VlqDigitList
     * ```
     */
    private data class Vlq(val digitList: VlqDigitList, val endPosition: Int)

    /**
     * ```text
     * VlqDigitList :
     *     TerminalDigit
     *     ContinuationDigit VlqDigitList
     * ```
     */
    @JvmInline
    private value class VlqDigitList(val digits: List<VlqDigit>)

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-base64-vlq">Section 6: Base64 VLQ</a>
     */
    @JvmInline
    private value class TerminalDigit(override val value: UInt) : VlqDigit

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-base64-vlq">Section 6: Base64 VLQ</a>
     */
    @JvmInline
    private value class ContinuationDigit(override val value: UInt) : VlqDigit

    /**
     * ```text
     * Vlq :
     *     VlqDigitList
     * ```
     */
    context(stream: ParserStream)
    private fun parseVlq(): ParsingResult<Vlq> {
        val digitList = parseVlqDigitList().ifFailure { return it }
        return Success(Vlq(digitList, stream.position))
    }

    /**
     * ```text
     * VlqDigitList :
     *     TerminalDigit
     *     ContinuationDigit VlqDigitList
     * ```
     */
    context(stream: ParserStream)
    private fun parseVlqDigitList(): ParsingResult<VlqDigitList> {
        val digits = mutableListOf<VlqDigit>()

        while (true) {
            val char = stream.current ?: return when {
                digits.isEmpty() -> NoMatch()
                else -> Failure("Unterminated VLQ at position ${stream.position}")
            }
            val value = base64ToValue(char)
            if (value < 0) {
                return when {
                    digits.isEmpty() -> NoMatch()
                    else -> Failure("Invalid base64 character '$char' at position ${stream.position}")
                }
            }

            stream.advance()
            val digit = when {
                value < 32 -> TerminalDigit(value.toUInt())
                else -> ContinuationDigit(value.toUInt())
            }
            digits.add(digit)

            if (digit is TerminalDigit) {
                return Success(VlqDigitList(digits))
            }
        }
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-VLQSignedValue">Section 6.1: VLQSignedValue</a>
     */
    private fun vlqSignedValue(vlq: Vlq): ParsingResult<Int> {
        // 1. Let unsigned be the VLQUnsignedValue of VlqDigitList.
        val unsigned = vlqUnsignedValue(vlq).ifFailure { return it }
        // 2. If unsigned modulo 2 = 1, let sign be -1.
        val sign = when {
            unsigned.mod(2u) == 1u -> -1
            // 3. Else, let sign be 1.
            else -> 1
        }
        // 4. Let value be floor(unsigned / 2).
        val value = unsigned / 2u
        // 5. If value is 0 and sign is -1, return -2**31.
        if (value == 0u && sign == -1) return Success(-2147483648)
        // 6. If value is ≥ 2**31, throw an error.
        if (value >= 2147483648u) return Failure("VLQ value exceeds maximum signed integer limit at position ${vlq.endPosition}")
        // 7. Return sign × value.
        return Success(sign * value.toInt())
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-VLQUnsignedValue">Section 6.2: VLQUnsignedValue</a>
     */
    private fun vlqUnsignedValue(vlq: Vlq): ParsingResult<UInt> {
        // 1. Let value be the VLQUnsignedValue of VlqDigitList.
        val value = vlqUnsignedValue(vlq.digitList, vlq.endPosition).ifFailure { return it }
        // 2. If value is ≥ 2**32, throw an error.
        if (value > UInt.MAX_VALUE.toULong()) {
            return Failure("VLQ value exceeds maximum unsigned integer limit at position ${vlq.endPosition}")
        }
        // 3. Return value.
        return Success(value.toUInt())
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-VLQUnsignedValue">Section 6.2: VLQUnsignedValue</a>
     */
    private fun vlqUnsignedValue(digitList: VlqDigitList, endPosition: Int): ParsingResult<ULong> {
        val terminal = digitList.digits.last() as TerminalDigit
        var right = vlqUnsignedValue(terminal)

        for (digit in digitList.digits.asReversed().drop(1)) {
            // 1. Let left be the VLQUnsignedValue of ContinuationDigit.
            val left = vlqUnsignedValue(digit as ContinuationDigit)
            // 2. Let right be the VLQUnsignedValue of VlqDigitList.
            // 3. Return left + right × 2**5.
            if (right > (UInt.MAX_VALUE.toULong() - left) / 32uL) {
                return Failure("VLQ value exceeds maximum unsigned integer limit at position $endPosition")
            }
            right = left + right * 32uL
        }

        return Success(right)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-VLQUnsignedValue">Section 6.2: VLQUnsignedValue</a>
     */
    private fun vlqUnsignedValue(digit: TerminalDigit): ULong {
        // 1. Let digit be the character matched by this production.
        // 2. Let value be the integer corresponding to digit, according to the base64 encoding as defined by IETF RFC 4648.
        // 3. Assert: value < 32.
        require(digit.value < 32u)
        // 4. Return value.
        return digit.value.toULong()
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-VLQUnsignedValue">Section 6.2: VLQUnsignedValue</a>
     */
    private fun vlqUnsignedValue(digit: ContinuationDigit): ULong {
        // 1. Let digit be the character matched by this production.
        // 2. Let value be the integer corresponding to digit, according to the base64 encoding as defined by IETF RFC 4648.
        // 3. Assert: 32 ≤ value < 64.
        require(digit.value in 32u..<64u)
        // 4. Return value - 32.
        return (digit.value - 32u).toULong()
    }

    //
    // 7. JSON utilities

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-JSONObjectGet">Section 7.2: JSONObjectGet(object, key)</a>
     */
    private fun jsonObjectGet(obj: JsonObject, key: String): JsonNode? {
        // 1. If obj does not have an own property with key key, return missing.
        // 2. Let prop be obj's own property whose key is key.
        // 3. Return prop's [[Value]] attribute.
        return obj.properties[key]
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-JSONArrayIterate">Section 7.3: JSONArrayIterate(array)</a>
     */
    private fun jsonArrayIterate(array: JsonArray): List<JsonNode> {
        // 1. Let length be JSONObjectGet(array, "length").
        // 2. Assert: length is a non-negative integral Number.
        // 3. Let list be a new empty List.
        // 4. Let i be 0.
        // 5. Repeat, while i < ℝ(length),
        //       a. Let value be JSONObjectGet(array, ToString(𝔽(i))).
        //       b. Assert: value is not missing.
        //       c. Append value to list.
        //       d. Set i to i + 1.
        //6. Return list.
        return array.elements
    }

    //
    // 8. Positions

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-position-record-type">Table 1: Position Record Fields</a>
     */
    data class PositionRecord(override val line: UInt, override val column: UInt) : PositionWithLineAndColumn

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-original-position-record-type">Table 2: Original Position Record Fields</a>
     */
    data class OriginalPositionRecord(val source: DecodedSourceRecord, override val line: UInt, override val column: UInt) :
        PositionWithLineAndColumn

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-ComparePositions">Section 8.3: ComparePositions(first, second)</a>
     */
    private fun <T : PositionWithLineAndColumn> comparePositions(first: T, second: T): ComparisonResult {
        // 1. If first.[[Line]] < second.[[Line]], return lesser.
        if (first.line < second.line) return ComparisonResult.LESSER
        // 2. If first.[[Line]] > second.[[Line]], return greater.
        if (first.line > second.line) return ComparisonResult.GREATER
        // 3. Assert: first.[[Line]] is equal to second.[[Line]].
        require(first.line == second.line) { "Unexpected result of comparison" }
        // 4. If first.[[Column]] < second.[[Column]], return lesser.
        if (first.column < second.column) return ComparisonResult.LESSER
        // 5. If first.[[Column]] > second.[[Column]], return greater.
        if (first.column > second.column) return ComparisonResult.GREATER
        // 6. Return equal.
        return ComparisonResult.EQUAL
    }

    //
    // 9.1 Source map decoding

    /**
     * @see <a href="https://tc39.es/ecma426/#decoded-source-map-record">Table 3: Fields of Decoded Source Map Records</a>
     */
    data class DecodedSourceMapRecord(
        val file: String?,
        val sources: List<DecodedSourceRecord>,
        val mappings: List<DecodedMappingRecord>,
        val ranges: List<GeneratedRangeRecord>,
    )

    /**
     * @see <a href="https://tc39.es/ecma426/#decoded-source-record">Table 4: Fields of Decoded Source Records</a>
     */
    class DecodedSourceRecord(
        var url: String?,
        var content: String?,
        var ignored: Boolean,
        var rootScopes: List<OriginalScopeRecord>?,
    )

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-ParseSourceMap">Section 9.1.1: ParseSourceMap(string, baseURL)</a>
     */
    fun parseSourceMap(string: String, baseUrl: String): ParsingResult<DecodedSourceMapRecord> {
        // 1. Let json be ParseJSON(string).
        val json = try {
            parseJson(string)
        } catch (e: JsonSyntaxException) {
            return Failure("Invalid JSON", e)
        }

        // 2. If json is not a JSON object, throw an error.
        expectType<JsonObject>(json) { e, a ->
            return Failure("Invalid JSON type of Source map: expected $e, actual $a")
        }

        // 3. If JSONObjectGet(json, "sections") is not missing, then
        if (!jsonObjectGet(json, "sections").isMissing) {
            // a. Return DecodeIndexSourceMap(json, baseURL).
            return decodeIndexSourceMap(json, baseUrl)
        }

        // 4. Return DecodeSourceMap(json, baseURL).
        return decodeSourceMap(json, baseUrl)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeSourceMap">Section 9.1.2: DecodeSourceMap(json, baseURL)</a>
     */
    private fun decodeSourceMap(json: JsonObject, baseUrl: String): ParsingResult<DecodedSourceMapRecord> {
        // 1. If JSONObjectGet(json, "version") is not 3𝔽, optionally report an error.
        jsonObjectGet(json, "version").let {
            expect(!it.isMissing) { return Failure("Missing required field: version") }
            expectType<JsonNumber>(it) { e, a -> return Failure("Invalid JSON type of version: expected $e, actual $a") }
            expectToBe(it.value, 3.0) { e, a -> return Failure("Invalid field value for version: expected $e, actual $a") }
        }

        // 2. Let mappingsField be JSONObjectGet(json, "mappings").
        val mappingsField = jsonObjectGet(json, "mappings").let {
            expect(!it.isMissing) { return Failure("Missing required field: mappings") }
            // 3. If mappingsField is not a String, throw an error.
            expectType<JsonString>(it) { e, a -> return Failure("Invalid JSON type of mappings: expected $e, actual $a") }
            it.value
        }

        // 4. If JSONObjectGet(json, "sources") is not a JSON array, throw an error.
        jsonObjectGet(json, "sources").let {
            expect(!it.isMissing) { return Failure("Missing required field: sources") }
            expectType<JsonArray>(it) { e, a -> return Failure("Invalid JSON type of sources: expected $e, actual $a") }
        }

        // 5. Let fileField be GetOptionalString(json, "file").
        val fileField = getOptionalString(json, "file").ifFailure { return it }
        // 6. Let sourceRootField be GetOptionalString(json, "sourceRoot").
        val sourceRootField = getOptionalString(json, "sourceRoot").ifFailure { return it }
        // 7. Let sourcesField be GetOptionalListOfOptionalStrings(json, "sources").
        val sourcesField = getOptionalListOfOptionalStrings(json, "sources").ifFailure { return it }
        // 8. Let sourcesContentField be GetOptionalListOfOptionalStrings(json, "sourcesContent").
        val sourcesContentField = getOptionalListOfOptionalStrings(json, "sourcesContent").ifFailure { return it }
        // 9. Let ignoreListField be GetOptionalListOfArrayIndexes(json, "ignoreList").
        val ignoreListField = getOptionalListOfArrayIndexes(json, "ignoreList").ifFailure { return it }
        // 10. Let namesField be GetOptionalListOfStrings(json, "names").
        val namesField = getOptionalListOfStrings(json, "names").ifFailure { return it }
        // 11. Let scopesField be GetOptionalListOfOptionalStrings(json, "scopes").
        val scopesField = getOptionalListOfOptionalStrings(json, "scopes").ifFailure { return it }
        // 12. Let rangesField be GetOptionalString(json, "ranges").
        val rangesField = getOptionalString(json, "ranges").ifFailure { return it }
        // 13. Let sources be DecodeSourceMapSources(baseURL, sourceRootField, sourcesField, sourcesContentField, ignoreListField, scopesField, namesField).
        val sources = decodeSourceMapSources(
            baseUrl,
            sourceRootField,
            sourcesField,
            sourcesContentField,
            ignoreListField,
            scopesField,
            namesField
        ).ifFailure { return it }
        // 14. Let ranges be DecodeGeneratedRanges(rangesField, sources, namesField).
        val ranges = decodeGeneratedRanges(rangesField, sources, namesField).ifFailure { return it }
        // 15. Let mappings be DecodeMappings(mappingsField, namesField, sources).
        // 16. Sort mappings in ascending order, with a Decoded Mapping Record a being less than a Decoded Mapping Record b if ComparePositions(a.[[GeneratedPosition]], b.[[GeneratedPosition]]) is lesser.
        val mappings = decodeMappings(mappingsField, namesField, sources)
            .ifFailure { return it }
            .sortedWith { record1, record2 -> comparePositions(record1.generatedPosition, record2.generatedPosition).value }
        // 17. Return the Decoded Source Map Record { [[File]]: fileField, [[Sources]]: sources, [[Mappings]]: mappings, [[Ranges]]: ranges }.
        return Success(DecodedSourceMapRecord(fileField, sources, mappings, ranges))
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-GetOptionalString">Section 9.1.2.1: GetOptionalString(object, key)</a>
     */
    private fun getOptionalString(obj: JsonObject, key: String): ParsingResult<String?> {
        // 1. Let value be JSONObjectGet(object, key).
        val value = jsonObjectGet(obj, key)
        // 2. If value is a String, return value.
        if (value is JsonString) return Success(value.value)
        // 3. If value is not missing, optionally report an error.
        if (!value.isMissing) return Failure("Invalid JSON type of $key: expected JsonString, actual ${value::class.simpleName}")
        // 4. Return null.
        return Success(null)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-GetOptionalListOfStrings">Section 9.1.2.2: GetOptionalListOfStrings(object, key)</a>
     */
    private fun getOptionalListOfStrings(obj: JsonObject, key: String): ParsingResult<List<String>> {
        // 1. Let list be a new empty List.
        val list = mutableListOf<String>()
        // 2. Let values be JSONObjectGet(object, key).
        val values = jsonObjectGet(obj, key)
        // 3. If values is missing, return list.
        if (values.isMissing) return Success(list)
        // 4. If values is not a JSON array, then
        // a. Optionally report an error.
        expectType<JsonArray>(values) { e, a ->
            return Failure("Invalid JSON type of $key: expected $e, actual $a")
        }
        // 5. For each element item of JSONArrayIterate(values), do
        for ([index, item] in jsonArrayIterate(values).withIndex()) {
            // a. If item is a String, then
            if (item is JsonString) {
                // i. Append item to list.
                list.add(item.value)
            }
            // b. Else,
            else {
                // i. Optionally report an error.
                return Failure("Invalid JSON type of array element in $key at index $index: expected JsonString, actual ${item::class.simpleName}")
            }
        }
        // 6. Return list.
        return Success(list)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-GetOptionalListOfOptionalStrings">Section 9.1.2.3: GetOptionalListOfOptionalStrings(object, key)</a>
     */
    private fun getOptionalListOfOptionalStrings(obj: JsonObject, key: String): ParsingResult<List<String?>> {
        // 1. Let list be a new empty List.
        val list = mutableListOf<String?>()
        // 2. Let values be JSONObjectGet(object, key).
        val values = jsonObjectGet(obj, key)
        // 3. If values is missing, return list.
        if (values.isMissing) return Success(list)
        // 4. If values is not a JSON array, then
        expectType<JsonArray>(values) { e, a ->
            // a. Optionally report an error.
            return Failure("Invalid JSON type of $key: expected $e, actual $a")
        }
        // 5. For each element item of JSONArrayIterate(values), do
        for ([index, item] in jsonArrayIterate(values).withIndex()) {
            // a. If item is a String, then
            if (item is JsonString) {
                // i. Append item to list.
                list.add(item.value)
            } else { // b. Else,
                // i. If item ≠ null, optionally report an error.
                if (item !is JsonNull) {
                    return Failure("Invalid JSON type of array element in $key at index $index: expected JsonString or JsonNull, actual ${item::class.simpleName}")
                }
                // ii. Append null to list.
                list.add(null)
            }
        }

        // 6. Return list.
        return Success(list)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-GetOptionalListOfArrayIndexes">Section 9.1.2.4: GetOptionalListOfArrayIndexes(object, key)</a>
     */
    private fun getOptionalListOfArrayIndexes(obj: JsonObject, key: String): ParsingResult<List<UInt>> {
        // 1. Let list be a new empty List.
        val list = mutableListOf<UInt>()
        // 2. Let values be JSONObjectGet(object, key).
        val values = jsonObjectGet(obj, key)
        // 3. If values is missing, return list.
        if (values.isMissing) return Success(list)
        // 4. If values is not a JSON array, then
        expectType<JsonArray>(values) { e, a ->
            // a. Optionally report an error.
            return Failure("Invalid JSON type of $key: expected $e, actual $a")
        }
        // 5. For each element item of JSONArrayIterate(values), do
        for ([index, item] in jsonArrayIterate(values).withIndex()) {
            // a. If item is an integral Number
            expectType<JsonNumber>(item) { e, a ->
                return Failure("Invalid JSON type of array element in $key at index $index: expected $e, actual $a")
            }
            // and item ≥ +0𝔽, then
            expect(item.value >= 0) { return Failure("Invalid negative value index in $key at index $index") }
            // i. Append ℝ(item) to list.
            list.add(item.value.toUInt())
        }

        // 6. Return list.
        return Success(list)
    }

    //
    // 9.2 Mappings grammar and decoding

    /**
     * @see <a href="https://tc39.es/ecma426/#decoded-mapping-record">Table 5: Fields of Decoded Mapping Records</a>
     */
    data class DecodedMappingRecord(
        val generatedPosition: PositionRecord,
        val originalPosition: OriginalPositionRecord?,
        val name: String?,
    )

    /**
     * ```text
     * Line :
     *     MappingList?
     * ```
     *
     * @see <a href="https://tc39.es/ecma426/#sec-mappings-grammar">Section 9.2.1: Mappings grammar</a>
     */
    private sealed interface Line {
        data object Empty : Line
        data object MappingList : Line
    }

    /**
     * ```text
     * Mapping :
     *     GeneratedColumn
     *     GeneratedColumn OriginalSource OriginalLine OriginalColumn Name?
     * ```
     *
     * @see <a href="https://tc39.es/ecma426/#sec-mappings-grammar">Section 9.2.1: Mappings grammar</a>
     */
    private sealed interface Mapping {
        val generatedColumn: Vlq

        data class GeneratedOnly(
            override val generatedColumn: Vlq,
        ) : Mapping

        data class Original(
            override val generatedColumn: Vlq,
            val originalSource: Vlq,
            val originalLine: Vlq,
            val originalColumn: Vlq,
            val name: Vlq?,
        ) : Mapping
    }

    /**
     * ```text
     * Line :
     *     MappingList?
     * ```
     *
     * @see <a href="https://tc39.es/ecma426/#sec-mappings-grammar">Section 9.2.1: Mappings grammar</a>
     */
    context(stream: ParserStream)
    private fun parseMappingsLine(): Line = when (stream.current) {
        null, ';' -> Line.Empty
        else -> Line.MappingList
    }

    /**
     * ```text
     * Mapping :
     *     GeneratedColumn
     *     GeneratedColumn OriginalSource OriginalLine OriginalColumn Name?
     * ```
     *
     * @see <a href="https://tc39.es/ecma426/#sec-mappings-grammar">Section 9.2.1: Mappings grammar</a>
     */
    context(stream: ParserStream)
    private fun parseMapping(): ParsingResult<Mapping> {
        val generatedColumn = parseVlq().ifFailure { return it }
        if (stream.isMappingDelimiter()) {
            return Success(Mapping.GeneratedOnly(generatedColumn))
        }

        val originalSource = parseRequiredVlq("original source").ifFailure { return it }
        val originalLine = parseRequiredVlq("original line").ifFailure { return it }
        val originalColumn = parseRequiredVlq("original column").ifFailure { return it }
        val name = if (stream.isMappingDelimiter()) null else parseRequiredVlq("name").ifFailure { return it }

        expect(stream.isMappingDelimiter()) {
            return Failure("Unexpected remaining mapping content at position ${stream.position}")
        }

        return Success(Mapping.Original(generatedColumn, originalSource, originalLine, originalColumn, name))
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#decode-mapping-state-record">Table 6: Fields of Decode Mapping State Records</a>
     */
    private class DecodeMappingStateRecord(
        var generatedLine: UInt = 0u,
        var generatedColumn: Int = 0,
        var sourceIndex: Int = 0,
        var originalLine: Int = 0,
        var originalColumn: Int = 0,
        var nameIndex: Int = 0,
    )

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeMappingsField">Section 9.2.1.1: DecodeMappingsField</a>
     */
    context(stream: ParserStream, state: DecodeMappingStateRecord, mappings: MutableList<DecodedMappingRecord>, names: List<String>, sources: List<DecodedSourceRecord>)
    private tailrec fun decodeMappingsFieldForLineList(): ParsingResult<Unit> {
        val line = parseMappingsLine()
        // 1. Perform DecodeMappingsField of Line with arguments state, mappings, names and sources.
        decodeMappingsFieldForLine(line).ifFailure { return it }
        // 2. Set state.[[GeneratedLine]] to state.[[GeneratedLine]] + 1.
        state.generatedLine++
        // 3. Set state.[[GeneratedColumn]] to 0.
        state.generatedColumn = 0
        // 4. Perform DecodeMappingsField of LineList with arguments state, mappings, names and sources.
        return when (stream.current) {
            ';' -> {
                stream.advance()
                decodeMappingsFieldForLineList()
            }
            null -> Success(Unit)
            else -> Failure("Unexpected remaining file content at position ${stream.position}")
        }
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeMappingsField">Section 9.2.1.1: DecodeMappingsField</a>
     */
    context(stream: ParserStream, state: DecodeMappingStateRecord, mappings: MutableList<DecodedMappingRecord>, names: List<String>, sources: List<DecodedSourceRecord>)
    private fun decodeMappingsFieldForLine(line: Line): ParsingResult<Unit> {
        return when (line) {
            // Line : [empty]
            Line.Empty -> Success(Unit)
            // Line : MappingList
            Line.MappingList -> decodeMappingsFieldMappingList()
        }
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeMappingsField">Section 9.2.1.1: DecodeMappingsField</a>
     */
    context(stream: ParserStream, state: DecodeMappingStateRecord, mappings: MutableList<DecodedMappingRecord>, names: List<String>, sources: List<DecodedSourceRecord>)
    private tailrec fun decodeMappingsFieldMappingList(): ParsingResult<Unit> {
        val mapping = parseMapping()
            .required("mapping", stream)
            .ifFailure { return it }
        // 1. Perform DecodeMappingsField of Mapping with arguments state, mappings, names and sources.
        decodeMappingsFieldForMapping(mapping).ifFailure { return it }
        // 2. Perform DecodeMappingsField of MappingList with arguments state, mappings, names and sources.
        return if (stream.current == ',') {
            stream.advance()
            decodeMappingsFieldMappingList()
        } else {
            Success(Unit)
        }
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeMappingsField">Section 9.2.1.1: DecodeMappingsField</a>
     */
    context(state: DecodeMappingStateRecord, mappings: MutableList<DecodedMappingRecord>, names: List<String>, sources: List<DecodedSourceRecord>)
    private fun decodeMappingsFieldForMapping(mapping: Mapping): ParsingResult<Unit> {
        return when (mapping) {
            // Mapping : GeneratedColumn
            is Mapping.GeneratedOnly -> {
                // 1. Perform DecodeMappingsField of GeneratedColumn with arguments state, mappings, names and sources.
                decodeMappingsFieldForGeneratedColumn(mapping.generatedColumn).ifFailure { return it }
                // 2. If state.[[GeneratedColumn]] < 0, then
                if (state.generatedColumn < 0) {
                    // a. Optionally report an error.
                    // b. Return.
                    return Failure("Negative generated column at position ${mapping.generatedColumn.endPosition}")
                }
                // 3. Let position be a new Position Record { [[Line]]: state.[[GeneratedLine]], [[Column]]: state.[[GeneratedColumn]] }.
                val position = PositionRecord(state.generatedLine, state.generatedColumn.toUInt())
                // 4. Let decodedMapping be a new DecodedMappingRecord { [[GeneratedPosition]]: position, [[OriginalPosition]]: null, [[Name]]: null }.
                val decodedMapping = DecodedMappingRecord(position, null, null)
                // 5. Append decodedMapping to mappings.
                mappings.add(decodedMapping)
                Success(Unit)
            }

            // Mapping : GeneratedColumn OriginalSource OriginalLine OriginalColumn Name?
            is Mapping.Original -> {
                // 1. Perform DecodeMappingsField of GeneratedColumn with arguments state, mappings, names and sources.
                decodeMappingsFieldForGeneratedColumn(mapping.generatedColumn).ifFailure { return it }
                // 2. If state.[[GeneratedColumn]] < 0, then
                if (state.generatedColumn < 0) {
                    // a. Optionally report an error.
                    // b. Return.
                    return Failure("Negative generated column at position ${mapping.generatedColumn.endPosition}")
                }
                // 3. Let generatedPosition be a new Position Record { [[Line]]: state.[[GeneratedLine]], [[Column]]: state.[[GeneratedColumn]] }.
                val generatedPosition = PositionRecord(state.generatedLine, state.generatedColumn.toUInt())
                // 4. Perform DecodeMappingsField of OriginalSource with arguments state, mappings, names and sources.
                decodeMappingsFieldForOriginalSource(mapping.originalSource).ifFailure { return it }
                // 5. Perform DecodeMappingsField of OriginalLine with arguments state, mappings, names and sources.
                decodeMappingsFieldForOriginalLine(mapping.originalLine).ifFailure { return it }
                // 6. Perform DecodeMappingsField of OriginalColumn with arguments state, mappings, names and sources.
                decodeMappingsFieldForOriginalColumn(mapping.originalColumn).ifFailure { return it }
                // 7. If state.[[SourceIndex]] < 0 or state.[[SourceIndex]] ≥ the number of elements of sources or state.[[OriginalLine]] < 0 or state.[[OriginalColumn]] < 0, then
                //       a. Optionally report an error.
                //       b. Let originalPosition be null.
                when {
                    state.sourceIndex < 0 -> return Failure("Negative source index at position ${mapping.originalColumn.endPosition}")
                    state.sourceIndex >= sources.size -> return Failure("Source index out of bounds at position ${mapping.originalColumn.endPosition}")
                    state.originalLine < 0 -> return Failure("Negative original line at position ${mapping.originalColumn.endPosition}")
                    state.originalColumn < 0 -> return Failure("Negative original column at position ${mapping.originalColumn.endPosition}")
                }
                // 8. Else,
                // a. Let originalPosition be a new Original Position Record { [[Source]]: sources[state.[[SourceIndex]]], [[Line]]: state.[[OriginalLine]], [[Column]]: state.[[OriginalColumn]] }.
                val originalPosition = OriginalPositionRecord(
                    sources[state.sourceIndex],
                    state.originalLine.toUInt(),
                    state.originalColumn.toUInt()
                )
                // 9. Let name be null.
                var name: String? = null
                // 10. If Name is present, then
                if (mapping.name != null) {
                    // a. Perform DecodeMappingsField of Name with arguments state, mappings, names and sources.
                    decodeMappingsFieldForName(mapping.name).ifFailure { return it }
                    // b. If state.[[NameIndex]] < 0 or state.[[NameIndex]] ≥ the number of elements of names, optionally report an error.
                    when {
                        state.nameIndex < 0 -> return Failure("Negative name index at position ${mapping.name.endPosition}")
                        state.nameIndex >= names.size -> return Failure("Name index out of bounds at position ${mapping.name.endPosition}")
                    }
                    // c. Else, set name to names[state.[[NameIndex]]].
                    name = names[state.nameIndex]
                }
                // 11. Let decodedMapping be a new DecodedMappingRecord { [[GeneratedPosition]]: generatedPosition, [[OriginalPosition]]: originalPosition, [[Name]]: name }.
                val decodedMapping = DecodedMappingRecord(generatedPosition, originalPosition, name)
                // 12. Append decodedMapping to mappings.
                mappings.add(decodedMapping)
                Success(Unit)
            }
        }
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeMappingsField">Section 9.2.1.1: DecodeMappingsField</a>
     */
    context(state: DecodeMappingStateRecord)
    private fun decodeMappingsFieldForGeneratedColumn(generatedColumn: Vlq): ParsingResult<Unit> {
        // 1. Let relativeColumn be the VLQSignedValue of Vlq.
        val relativeColumn = vlqSignedValue(generatedColumn).ifFailure { return it }
        // 2. Set state.[[GeneratedColumn]] to state.[[GeneratedColumn]] + relativeColumn.
        state.generatedColumn += relativeColumn
        return Success(Unit)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeMappingsField">Section 9.2.1.1: DecodeMappingsField</a>
     */
    context(state: DecodeMappingStateRecord)
    private fun decodeMappingsFieldForOriginalSource(originalSource: Vlq): ParsingResult<Unit> {
        // 1. Let relativeSourceIndex be the VLQSignedValue of Vlq.
        val relativeSourceIndex = vlqSignedValue(originalSource).ifFailure { return it }
        // 2. Set state.[[SourceIndex]] to state.[[SourceIndex]] + relativeSourceIndex.
        state.sourceIndex += relativeSourceIndex
        return Success(Unit)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeMappingsField">Section 9.2.1.1: DecodeMappingsField</a>
     */
    context(state: DecodeMappingStateRecord)
    private fun decodeMappingsFieldForOriginalLine(originalLine: Vlq): ParsingResult<Unit> {
        // 1. Let relativeLine be the VLQSignedValue of Vlq.
        val relativeLine = vlqSignedValue(originalLine).ifFailure { return it }
        // 2. Set state.[[OriginalLine]] to state.[[OriginalLine]] + relativeLine.
        state.originalLine += relativeLine
        return Success(Unit)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeMappingsField">Section 9.2.1.1: DecodeMappingsField</a>
     */
    context(state: DecodeMappingStateRecord)
    private fun decodeMappingsFieldForOriginalColumn(originalColumn: Vlq): ParsingResult<Unit> {
        // 1. Let relativeColumn be the VLQSignedValue of Vlq.
        val relativeColumn = vlqSignedValue(originalColumn).ifFailure { return it }
        // 2. Set state.[[OriginalColumn]] to state.[[OriginalColumn]] + relativeColumn.
        state.originalColumn += relativeColumn
        return Success(Unit)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeMappingsField">Section 9.2.1.1: DecodeMappingsField</a>
     */
    context(state: DecodeMappingStateRecord)
    private fun decodeMappingsFieldForName(name: Vlq): ParsingResult<Unit> {
        // 1. Let relativeName be the VLQSignedValue of Vlq.
        val relativeName = vlqSignedValue(name).ifFailure { return it }
        // 2. Set state.[[NameIndex]] to state.[[NameIndex]] + relativeName.
        state.nameIndex += relativeName
        return Success(Unit)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeMappings">Section 9.2.2: DecodeMappings(rawMappings, names, sources)</a>
     */
    private fun decodeMappings(
        rawMappings: String,
        names: List<String>,
        sources: List<DecodedSourceRecord>,
    ): ParsingResult<List<DecodedMappingRecord>> {
        // 1. Let mappings be a new empty List.
        val mappings = mutableListOf<DecodedMappingRecord>()
        // 2. Let mappingsNode be the root Parse Node when parsing rawMappings using MappingsField as the goal symbol.
        val stream = ParserStream(rawMappings)
        // 3. If parsing failed, then
        //       a. Optionally report an error.
        //       b. Return mappings.
        // (the failure should be reported directly from the parser)

        // 4. Let state be a new Decode Mapping State Record with all fields set to 0.
        val state = DecodeMappingStateRecord()

        // 5. Perform DecodeMappingsField of mappingsNode with arguments state, mappings, names and sources.
        context(stream, state, mappings, names, sources) {
            decodeMappingsFieldForLineList().ifFailure { return it }
        }

        // 6. Return mappings.
        return Success(mappings)
    }

    //
    // 9.3 Scopes grammar and decoding

    //
    // 9.3.1 Original scope record

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-original-scope-record-type">Section 9.3.1: Original Scope Record</a>
     */
    data class OriginalScopeRecord(
        var start: PositionRecord,
        var end: PositionRecord,
        var name: String?,
        var kind: String?,
        var variables: List<String>,
        var children: List<OriginalScopeRecord>,
        var isStackFrame: Boolean,
    )

    //
    // 9.3.2 Generated range and binding records

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-generated-range-record-type">Section 9.3.2: Generated Range Record</a>
     */
    data class GeneratedRangeRecord(
        val start: PositionRecord,
        val end: PositionRecord,
        val definition: OriginalScopeRecord?,
        val stackFrameType: StackFrameType,
        val bindings: List<List<BindingRecord>>,
        val callSite: GeneratedRangeCallSiteRecord?,
        val children: List<GeneratedRangeRecord>,
    )

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-binding-record-type">Section 9.3.2.1: The Binding Record</a>
     */
    data class BindingRecord(
        /**
         * Use [binding] from this position until the next binding record's [from] position or [GeneratedRangeRecord.end] to retrieve the variable's value.
         */
        val from: PositionRecord,
        /**
         * The JavaScript expression used to retrieve the variable's value, or null when the variable is unavailable.
         */
        val binding: String?,
    )

    /** Identifies how a generated range contributes to stack-frame reconstruction. */
    enum class StackFrameType {
        /** The generated range is not a JavaScript/WASM function. */
        NONE,

        /** The generated range is an original JavaScript/Wasm function. */
        ORIGINAL,

        /** The generated range is a compiler/transpiler-inserted JavaScript/Wasm function even though [GeneratedRangeRecord.definition] is not null. */
        HIDDEN
    }

    //
    // 9.3.3 Scopes grammar


    /**
     * ```text
     * OriginalScopeTreeItem :
     *     OriginalScopeTree
     * ```
     */
    private sealed interface OriginalScopeTreeItem

    /**
     * ```text
     * TopLevelItem :
     *     GeneratedRangeTree
     *     VendorExtensionItem
     *     InvalidRangeItem
     * ```
     */
    private sealed interface TopLevelItem

    /**
     * ```text
     * OriginalScopeTree :
     *     OriginalScopeStart OriginalScopeVariablesItem? OriginalScopeItemList? , OriginalScopeEnd
     * ```
     */
    private data class OriginalScopeTree(
        val start: OriginalScopeStart,
        val variables: List<ScopeVariable>?,
        val items: List<OriginalScopeItem>?,
        val end: OriginalScopeEnd,
    ) : OriginalScopeItem, OriginalScopeTreeItem

    /**
     * ```text
     * OriginalScopeItem :
     *     OriginalScopeTree
     *     VendorExtensionItem
     *     InvalidScopeItem
     * ```
     */
    private sealed interface OriginalScopeItem

    /**
     * ```text
     * OriginalScopeStart :
     *     B ScopeFlags ScopeLine ScopeColumn ScopeNameOrKind? ScopeKind?
     * ```
     */
    private data class OriginalScopeStart(
        val flags: UInt,
        val line: ScopeLine,
        val column: ScopeColumn,
        val nameOrKind: ScopeNameOrKind?,
        val kind: ScopeKind?,
    )

    /**
     * ```text
     * OriginalScopeEnd :
     *     C ScopeLine ScopeColumn
     * ```
     */
    private data class OriginalScopeEnd(
        val line: ScopeLine,
        val column: ScopeColumn,
    )

    /**
     * ```text
     * ScopeFlags :
     *     Vlq
     * ```
     */
    private typealias ScopeFlags = Vlq

    /**
     * ```text
     * ScopeLine :
     *     Vlq
     * ```
     */
    private typealias ScopeLine = Vlq

    /**
     * ```text
     * ScopeColumn :
     *     Vlq
     * ```
     */
    private typealias ScopeColumn = Vlq

    /**
     * ```text
     * ScopeNameOrKind :
     *     Vlq
     * ```
     */
    private typealias ScopeNameOrKind = Vlq

    /**
     * ```text
     * ScopeKind :
     *     Vlq
     * ```
     */
    private typealias ScopeKind = Vlq

    /**
     * ```text
     * ScopeVariable :
     *     Vlq
     * ```
     */
    private typealias ScopeVariable = Vlq

    /**
     * ```text
     * GeneratedRangeTree :
     *     GeneratedRangeStart GeneratedRangeBindingsItem? GeneratedRangeCallSiteItem? GeneratedRangeItemList? , GeneratedRangeEnd
     * ```
     */
    private data class GeneratedRangeTree(
        val start: GeneratedRangeStart,
        val bindings: GeneratedRangeBindingsItem?,
        val callSite: GeneratedRangeCallSiteItem?,
        val items: List<GeneratedRangeItem>?,
        val end: GeneratedRangeEnd,
    ) : TopLevelItem, GeneratedRangeItem

    /**
     * ```text
     * GeneratedRangeBindingsItem :
     *     , GeneratedRangeBindings
     * ```
     */
    private data class GeneratedRangeBindingsItem(val bindings: GeneratedRangeBindings)

    /**
     * ```text
     * GeneratedRangeCallSiteItem :
     *     , GeneratedRangeCallSite
     * ```
     */
    private data class GeneratedRangeCallSiteItem(val callSite: GeneratedRangeCallSite)

    /**
     * ```text
     * GeneratedRangeItem :
     *     GeneratedSubRangeBinding
     *     GeneratedRangeTree
     *     VendorExtensionItem
     *     InvalidRangeItem
     * ```
     */
    private sealed interface GeneratedRangeItem

    /**
     * ```text
     * GeneratedRangeStart :
     *     E RangeFlags RangeLine? RangeColumn RangeDefinition?
     * ```
     */
    private data class GeneratedRangeStart(
        val flags: UInt,
        val line: RangeLine?,
        val column: RangeColumn,
        val definition: RangeDefinition?,
    )

    /**
     * ```text
     * GeneratedRangeEnd :
     *     F RangeLine? RangeColumn
     * ```
     */
    private data class GeneratedRangeEnd(
        val line: RangeLine?,
        val column: RangeColumn,
    )

    /**
     * ```text
     * GeneratedRangeBindings :
     *     G BindingExpressionList
     * ```
     */
    private data class GeneratedRangeBindings(val list: List<BindingExpression>)

    /**
     * ```text
     * GeneratedSubRangeBinding :
     *     H VariableIndex BindingFromList
     * ```
     */
    private data class GeneratedSubRangeBinding(
        val variableIndex: VariableIndex,
        val bindings: List<BindingFrom>,
    ) : GeneratedRangeItem

    /**
     * ```text
     * BindingFrom :
     *     BindingLine BindingColumn BindingExpression
     * ```
     */
    private data class BindingFrom(
        val line: BindingLine,
        val column: BindingColumn,
        val expression: BindingExpression,
    )

    /**
     * ```text
     * GeneratedRangeCallSite :
     *     I CallSiteSourceIdx CallSiteLine CallSiteColumn
     * ```
     */
    private data class GeneratedRangeCallSite(
        val sourceIndex: CallSiteSourceIdx,
        val line: CallSiteLine,
        val column: CallSiteColumn,
    )

    /**
     * ```text
     * RangeFlags :
     *     Vlq
     * ```
     */
    private typealias RangeFlags = Vlq

    /**
     * ```text
     * RangeLine :
     *     Vlq
     * ```
     */
    private typealias RangeLine = Vlq

    /**
     * ```text
     * RangeColumn :
     *     Vlq
     * ```
     */
    private typealias RangeColumn = Vlq

    /**
     * ```text
     * RangeDefinition :
     *     DefinitionSourceIdx DefinitionScopeIdx
     * ```
     */
    private data class RangeDefinition(
        val sourceIdx: DefinitionSourceIdx,
        val scopeIdx: DefinitionScopeIdx,
    )

    /**
     * ```text
     * DefinitionSourceIdx :
     *     Vlq
     * ```
     */
    private typealias DefinitionSourceIdx = Vlq

    /**
     * ```text
     * DefinitionScopeIdx :
     *     Vlq
     * ```
     */
    private typealias DefinitionScopeIdx = Vlq

    /**
     * ```text
     * VariableIndex :
     *     Vlq
     * ```
     */
    private typealias VariableIndex = Vlq

    /**
     * ```text
     * BindingLine :
     *     Vlq
     * ```
     */
    private typealias BindingLine = Vlq

    /**
     * ```text
     * BindingColumn :
     *     Vlq
     * ```
     */
    private typealias BindingColumn = Vlq

    /**
     * ```text
     * BindingExpression :
     *     Vlq
     * ```
     */
    private typealias BindingExpression = Vlq

    /**
     * ```text
     * CallSiteSourceIdx :
     *     Vlq
     * ```
     */
    private typealias CallSiteSourceIdx = Vlq

    /**
     * ```text
     * CallSiteLine :
     *     Vlq
     * ```
     */
    private typealias CallSiteLine = Vlq

    /**
     * ```text
     * CallSiteColumn :
     *     Vlq
     * ```
     */
    private typealias CallSiteColumn = Vlq

    /**
     * ```text
     * VendorExtensionItem :
     *     / VendorExtensionName
     *     / VendorExtensionName VlqList
     * ```
     */
    private data class VendorExtensionItem(
        val name: VendorExtensionName,
        val data: List<Vlq>,
    ) : TopLevelItem, OriginalScopeItem, GeneratedRangeItem

    /**
     * ```text
     * VendorExtensionName :
     *     Vlq
     * ```
     */
    private typealias VendorExtensionName = Vlq

    /**
     * ```text
     * InvalidScopeItem :
     *     InvalidScopeTag
     *     InvalidScopeTag VlqList
     * ```
     */
    private data class InvalidScopeItem(
        val tag: Vlq,
        val data: List<Vlq>,
    ) : OriginalScopeItem

    /**
     * ```text
     * InvalidRangeItem :
     *     InvalidRangeTag
     *     InvalidRangeTag VlqList
     * ```
     */
    private data class InvalidRangeItem(
        val tag: Vlq,
        val data: List<Vlq>,
    ) : TopLevelItem, GeneratedRangeItem

    /**
     * ```text
     * Scopes :
     *     OriginalScopeTreeList?
     * ```
     */
    context(stream: ParserStream)
    private fun parseScopes(): ParsingResult<List<OriginalScopeTreeItem>?> {
        val originalScopeTrees = stream.parseOptional { parseOriginalScopeTreeList() }.ifFailure { return it }
        return Success(originalScopeTrees)
    }

    /**
     * ```text
     * OriginalScopeTreeList :
     *     OriginalScopeTreeItem
     *     OriginalScopeTreeList , OriginalScopeTreeItem
     * ```
     */
    context(stream: ParserStream)
    private fun parseOriginalScopeTreeList(): ParsingResult<List<OriginalScopeTreeItem>> {
        return stream.parseManySeparated(',') { parseOriginalScopeTreeItem() }
    }

    /**
     * ```text
     * OriginalScopeTreeItem :
     *     OriginalScopeTree
     * ```
     */
    context(stream: ParserStream)
    private fun parseOriginalScopeTreeItem(): ParsingResult<OriginalScopeTreeItem> {
        return stream.parseOneOf(
            { parseOriginalScopeTree() }
        )
    }

    /**
     * ```text
     * Ranges :
     *     TopLevelItemList?
     * ```
     */
    context(stream: ParserStream)
    private fun parseRanges(): ParsingResult<List<TopLevelItem>?> {
        val topLevelItems = stream.parseOptional { parseTopLevelItemList() }.ifFailure { return it }
        return Success(topLevelItems)
    }

    /**
     * ```text
     * TopLevelItemList :
     *     TopLevelItem
     *     TopLevelItemList , TopLevelItem
     * ```
     */
    context(stream: ParserStream)
    private fun parseTopLevelItemList(): ParsingResult<List<TopLevelItem>> {
        return stream.parseManySeparated(',') { parseTopLevelItem() }
    }

    /**
     * ```text
     * TopLevelItem :
     *     GeneratedRangeTree
     *     VendorExtensionItem
     *     InvalidRangeItem
     * ```
     */
    context(stream: ParserStream)
    private fun parseTopLevelItem(): ParsingResult<TopLevelItem> {
        return stream.parseOneOf(
            { parseGeneratedRangeTree() },
            { parseVendorExtensionItem() },
            { parseInvalidRangeItem() }
        )
    }

    /**
     * ```text
     * OriginalScopeTree :
     *     OriginalScopeStart OriginalScopeVariablesItem? OriginalScopeItemList? , OriginalScopeEnd
     * ```
     */
    context(stream: ParserStream)
    private fun parseOriginalScopeTree(): ParsingResult<OriginalScopeTree> {
        val start = parseOriginalScopeStart().ifFailure { return it }

        val variables = stream.parseOptional { parseOriginalScopeVariablesItem() }.ifFailure { return it }
        val items = stream.parseOptional { parseOriginalScopeItemList() }.ifFailure { return it }

        stream.expectChar(',').ifFailure { return it }
        val end = parseOriginalScopeEnd()
            .required("original scope end", stream)
            .ifFailure { return it }

        return Success(OriginalScopeTree(start, variables, items, end))
    }

    /**
     * ```text
     * OriginalScopeVariablesItem :
     *     , OriginalScopeVariables
     * ```
     */
    context(stream: ParserStream)
    private fun parseOriginalScopeVariablesItem(): ParsingResult<List<ScopeVariable>> {
        stream.parseChar(',').ifFailure { return it }
        return parseOriginalScopeVariables()
    }

    /**
     * ```text
     * OriginalScopeItemList :
     *     , OriginalScopeItem
     *     OriginalScopeItemList , OriginalScopeItem
     * ```
     */
    context(stream: ParserStream)
    private fun parseOriginalScopeItemList(): ParsingResult<List<OriginalScopeItem>> {
        stream.parseChar(',').ifFailure { return it }
        return stream.parseManySeparated(',') { parseOriginalScopeItem() }
    }

    /**
     * ```text
     * OriginalScopeItem :
     *     OriginalScopeTree
     *     VendorExtensionItem
     *     InvalidScopeItem
     * ```
     */
    context(stream: ParserStream)
    private fun parseOriginalScopeItem(): ParsingResult<OriginalScopeItem> {
        return stream.parseOneOf(
            { parseOriginalScopeTree() },
            { parseVendorExtensionItem() },
            { parseInvalidScopeItem() }
        )
    }

    /**
     * ```text
     * OriginalScopeStart :
     *     B ScopeFlags ScopeLine ScopeColumn ScopeNameOrKind? ScopeKind?
     * ```
     */
    context(stream: ParserStream)
    private fun parseOriginalScopeStart(): ParsingResult<OriginalScopeStart> {
        stream.parseChar(VlqKindTag.ORIGINAL_SCOPE_START).ifFailure { return it }

        val vlqScopeFlags: ScopeFlags = parseRequiredVlq("scope flags").ifFailure { return it }
        val scopeFlags = vlqUnsignedValue(vlqScopeFlags).ifFailure { return it }
        val scopeLine: ScopeLine = parseRequiredVlq("scope line").ifFailure { return it }
        val scopeColumn: ScopeColumn = parseRequiredVlq("scope column").ifFailure { return it }

        var scopeNameOrKind: ScopeNameOrKind? = null
        var scopeKind: ScopeKind? = null

        if ((scopeFlags and 0x1u) != 0u) {
            scopeNameOrKind = parseRequiredVlq("scope name or kind").ifFailure { return it }
            if ((scopeFlags and 0x2u) != 0u) {
                scopeKind = parseRequiredVlq("scope kind").ifFailure { return it }
            }
        } else if ((scopeFlags and 0x2u) != 0u) {
            scopeNameOrKind = parseRequiredVlq("scope name or kind").ifFailure { return it }
        }

        return Success(
            OriginalScopeStart(
                flags = scopeFlags,
                line = scopeLine,
                column = scopeColumn,
                nameOrKind = scopeNameOrKind,
                kind = scopeKind,
            )
        )
    }

    /**
     * ```text
     * OriginalScopeEnd :
     *     C ScopeLine ScopeColumn
     * ```
     */
    context(stream: ParserStream)
    private fun parseOriginalScopeEnd(): ParsingResult<OriginalScopeEnd> {
        stream.parseChar(VlqKindTag.ORIGINAL_SCOPE_END).ifFailure { return it }

        val scopeLine: ScopeLine = parseRequiredVlq("scope line").ifFailure { return it }
        val scopeColumn: ScopeColumn = parseRequiredVlq("scope column").ifFailure { return it }

        return Success(OriginalScopeEnd(scopeLine, scopeColumn))
    }

    /**
     * ```text
     * OriginalScopeVariables :
     *     D ScopeVariableList
     * ```
     */
    context(stream: ParserStream)
    private fun parseOriginalScopeVariables(): ParsingResult<List<ScopeVariable>> {
        stream.parseChar(VlqKindTag.ORIGINAL_SCOPE_VARIABLES).ifFailure { return it }

        val variables = parseScopeVariableList()
            .required("scope variable", stream)
            .ifFailure { return it }

        return Success(variables)
    }

    /**
     * ```text
     * ScopeVariableList :
     *     ScopeVariable
     *     ScopeVariableList ScopeVariable
     * ```
     */
    context(stream: ParserStream)
    private fun parseScopeVariableList(): ParsingResult<List<ScopeVariable>> {
        return stream.parseMany { parseVlq() }
    }

    /**
     * ```text
     * GeneratedRangeTree :
     *     GeneratedRangeStart GeneratedRangeBindingsItem? GeneratedRangeCallSiteItem? GeneratedRangeItemList? , GeneratedRangeEnd
     * ```
     */
    context(stream: ParserStream)
    private fun parseGeneratedRangeTree(): ParsingResult<GeneratedRangeTree> {
        val start = parseGeneratedRangeStart().ifFailure { return it }

        val bindings = stream.parseOptional { parseGeneratedRangeBindingsItem() }.ifFailure { return it }
        val callSite = stream.parseOptional { parseGeneratedRangeCallSiteItem() }.ifFailure { return it }
        val items = stream.parseOptional { parseGeneratedRangeItemList() }.ifFailure { return it }

        stream.expectChar(',').ifFailure { return it }
        val end = parseGeneratedRangeEnd()
            .required("generated range end", stream)
            .ifFailure { return it }

        return Success(GeneratedRangeTree(start, bindings, callSite, items, end))
    }

    /**
     * ```text
     * GeneratedRangeBindingsItem :
     *     , GeneratedRangeBindings
     * ```
     */
    context(stream: ParserStream)
    private fun parseGeneratedRangeBindingsItem(): ParsingResult<GeneratedRangeBindingsItem> {
        stream.parseChar(',').ifFailure { return it }
        return parseGeneratedRangeBindings().map(::GeneratedRangeBindingsItem)
    }

    /**
     * ```text
     * GeneratedRangeCallSiteItem :
     *     , GeneratedRangeCallSite
     * ```
     */
    context(stream: ParserStream)
    private fun parseGeneratedRangeCallSiteItem(): ParsingResult<GeneratedRangeCallSiteItem> {
        stream.parseChar(',').ifFailure { return it }
        return parseGeneratedRangeCallSite().map(::GeneratedRangeCallSiteItem)
    }

    /**
     * ```text
     * GeneratedRangeItemList :
     *     , GeneratedRangeItem
     *     GeneratedRangeItemList , GeneratedRangeItem
     * ```
     */
    context(stream: ParserStream)
    private fun parseGeneratedRangeItemList(): ParsingResult<List<GeneratedRangeItem>> {
        stream.parseChar(',').ifFailure { return it }
        return stream.parseManySeparated(',') { parseGeneratedRangeItem() }
    }

    /**
     * ```text
     * GeneratedRangeItem :
     *     GeneratedSubRangeBinding
     *     GeneratedRangeTree
     *     VendorExtensionItem
     *     InvalidRangeItem
     * ```
     */
    context(stream: ParserStream)
    private fun parseGeneratedRangeItem(): ParsingResult<GeneratedRangeItem> {
        return stream.parseOneOf(
            { parseGeneratedSubRangeBinding() },
            { parseGeneratedRangeTree() },
            { parseVendorExtensionItem() },
            { parseInvalidRangeItem() }
        )
    }

    /**
     * ```text
     * GeneratedRangeStart :
     *     E RangeFlags RangeLine? RangeColumn RangeDefinition?
     * ```
     */
    context(stream: ParserStream)
    private fun parseGeneratedRangeStart(): ParsingResult<GeneratedRangeStart> {
        stream.parseChar(VlqKindTag.GENERATED_RANGE_START).ifFailure { return it }

        val vlqFlags: RangeFlags = parseRequiredVlq("range flags").ifFailure { return it }
        val flags = vlqUnsignedValue(vlqFlags).ifFailure { return it }
        val line: RangeLine? =
            if ((flags and 0x1u) != 0u) parseRequiredVlq("range line").ifFailure { return it } else null
        val column: RangeColumn = parseRequiredVlq("range column").ifFailure { return it }
        val definition: RangeDefinition? = if ((flags and 0x2u) != 0u) {
            val sourceIdx: DefinitionSourceIdx = parseRequiredVlq("range definition source index").ifFailure { return it }
            val scopeIdx: DefinitionScopeIdx = parseRequiredVlq("range definition scope index").ifFailure { return it }
            RangeDefinition(sourceIdx, scopeIdx)
        } else null

        return Success(GeneratedRangeStart(flags, line, column, definition))
    }

    /**
     * ```text
     * GeneratedRangeEnd :
     *     F RangeLine? RangeColumn
     * ```
     */
    context(stream: ParserStream)
    private fun parseGeneratedRangeEnd(): ParsingResult<GeneratedRangeEnd> {
        stream.parseChar(VlqKindTag.GENERATED_RANGE_END).ifFailure { return it }

        val values = stream.parseMany { parseVlq() }
            .required("range column", stream)
            .ifFailure { return it }
        return when (values.size) {
            1 -> Success(GeneratedRangeEnd(line = null, column = values.single()))
            2 -> Success(GeneratedRangeEnd(line = values[0], column = values[1]))
            else -> Failure("Generated range end expects an optional line and a column at position ${stream.position}")
        }
    }

    /**
     * ```text
     * GeneratedRangeBindings :
     *     G BindingExpressionList
     * ```
     */
    context(stream: ParserStream)
    private fun parseGeneratedRangeBindings(): ParsingResult<GeneratedRangeBindings> {
        stream.parseChar(VlqKindTag.GENERATED_RANGE_BINDINGS).ifFailure { return it }
        return parseBindingExpressionList()
            .required("binding expression", stream)
            .map(::GeneratedRangeBindings)
    }

    /**
     * ```text
     * BindingExpressionList :
     *     BindingExpression
     *     BindingExpressionList BindingExpression
     * ```
     */
    context(stream: ParserStream)
    private fun parseBindingExpressionList(): ParsingResult<List<BindingExpression>> {
        return stream.parseMany { parseVlq() }
    }

    /**
     * ```text
     * GeneratedSubRangeBinding :
     *     H VariableIndex BindingFromList
     * ```
     */
    context(stream: ParserStream)
    private fun parseGeneratedSubRangeBinding(): ParsingResult<GeneratedSubRangeBinding> {
        stream.parseChar(VlqKindTag.GENERATED_SUB_RANGE_BINDINGS).ifFailure { return it }

        val variableIndex: VariableIndex = parseRequiredVlq("variable index").ifFailure { return it }
        val bindings = parseBindingFromList()
            .required("binding", stream)
            .ifFailure { return it }

        return Success(GeneratedSubRangeBinding(variableIndex, bindings))
    }

    /**
     * ```text
     * BindingFromList :
     *     BindingFrom
     *     BindingFromList BindingFrom
     * ```
     */
    context(stream: ParserStream)
    private fun parseBindingFromList(): ParsingResult<List<BindingFrom>> {
        return stream.parseMany { parseBindingFrom() }
    }

    /**
     * ```text
     * BindingFrom :
     *     BindingLine BindingColumn BindingExpression
     * ```
     */
    context(stream: ParserStream)
    private fun parseBindingFrom(): ParsingResult<BindingFrom> {
        val line: BindingLine = parseVlq().ifFailure { return it }
        val column: BindingColumn = parseRequiredVlq("binding column").ifFailure { return it }
        val expression: BindingExpression = parseRequiredVlq("binding expression").ifFailure { return it }

        return Success(BindingFrom(line, column, expression))
    }

    /**
     * ```text
     * GeneratedRangeCallSite :
     *     I CallSiteSourceIdx CallSiteLine CallSiteColumn
     * ```
     */
    context(stream: ParserStream)
    private fun parseGeneratedRangeCallSite(): ParsingResult<GeneratedRangeCallSite> {
        stream.parseChar(VlqKindTag.GENERATED_RANGE_CALL_SITE).ifFailure { return it }

        val sourceIndex: CallSiteSourceIdx = parseRequiredVlq("call site source index").ifFailure { return it }
        val line: CallSiteLine = parseRequiredVlq("call site line").ifFailure { return it }
        val column: CallSiteColumn = parseRequiredVlq("call site column").ifFailure { return it }

        return Success(GeneratedRangeCallSite(sourceIndex, line, column))
    }

    /**
     * ```text
     * VendorExtensionItem :
     *     / VendorExtensionName
     *     / VendorExtensionName VlqList
     * ```
     */
    context(stream: ParserStream)
    private fun parseVendorExtensionItem(): ParsingResult<VendorExtensionItem> {
        stream.parseChar(VlqKindTag.VENDOR_EXTENSION).ifFailure { return it }

        val name: VendorExtensionName = parseRequiredVlq("vendor extension name").ifFailure { return it }
        val data: List<Vlq> = parseVlqList()
            .defaultIfNoMatch(::emptyList)
            .ifFailure { return it }

        return Success(VendorExtensionItem(name, data))
    }

    /**
     * ```text
     * VlqList :
     *     Vlq
     *     VlqList Vlq
     * ```
     */
    context(stream: ParserStream)
    private fun parseVlqList(): ParsingResult<List<Vlq>> {
        return stream.parseMany { parseVlq() }
    }

    /**
     * ```text
     * InvalidScopeItem :
     *     InvalidScopeTag
     *     InvalidScopeTag VlqList
     * ```
     */
    context(stream: ParserStream)
    private fun parseInvalidScopeItem(): ParsingResult<InvalidScopeItem> {
        val tag = parseInvalidScopeTag().ifFailure { return it }
        val data: List<Vlq> = parseVlqList()
            .defaultIfNoMatch(::emptyList)
            .ifFailure { return it }

        return Success(InvalidScopeItem(tag, data))
    }

    /**
     * ```text
     * InvalidScopeTag :
     *     Vlq but not one of B C D /
     * ```
     */
    context(stream: ParserStream)
    private fun parseInvalidScopeTag(): ParsingResult<Vlq> {
        if (stream.current in VlqKindTag.knownScopeTags) return NoMatch()

        return parseVlq()
    }

    /**
     * ```text
     * InvalidRangeItem :
     *     InvalidRangeTag
     *     InvalidRangeTag VlqList
     * ```
     */
    context(stream: ParserStream)
    private fun parseInvalidRangeItem(): ParsingResult<InvalidRangeItem> {
        val tag = parseInvalidRangeTag().ifFailure { return it }
        val data: List<Vlq> = parseVlqList()
            .defaultIfNoMatch(::emptyList)
            .ifFailure { return it }

        return Success(InvalidRangeItem(tag, data))
    }

    /**
     * ```text
     * InvalidRangeTag :
     *     Vlq but not one of E F G H I /
     * ```
     */
    context(stream: ParserStream)
    private fun parseInvalidRangeTag(): ParsingResult<Vlq> {
        if (stream.current in VlqKindTag.knownRangeTags) return NoMatch()

        return parseVlq()
    }

    //
    // 9.3.4 Decoding the scopes field

    /**
     * The scopes grammar defined in the scopes and ranges grammar section is context-sensitive and heavily
     * utilizes relative numbers to reduce the bytes required for the encoded scope information. As such, the
     * grammar alone is not enough to describe how a `Scopes` parse node is turned into Original Scope Records,
     * and a `Ranges` parse node is turned into Generated Range Records.
     *
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-decode-scope-state-record-type">Section 9.3.4.1: Decode Scope State Record</a>
     */
    data class DecodeScopeStateRecord(
        val scopePosition: PositionAccumulatorRecord,
        val scopeNameIndex: IndexAccumulatorRecord,
        val scopeKindIndex: IndexAccumulatorRecord,
        val scopeVariableIndex: IndexAccumulatorRecord,
    )

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-decode-scope-state-record-type">Section 9.3.4.1: Decode Range State Record</a>
     */
    data class DecodeRangeStateRecord(
        val rangePosition: PositionAccumulatorRecord,
        val rangeDefinitionSourceIndex: IndexAccumulatorRecord,
        val rangeDefinitionScopeIndex: IndexAccumulatorRecord,
    )

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#index-accumulator-record">Table 14: Index Accumulator Record Fields</a>
     */
    data class IndexAccumulatorRecord(var index: Int)

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#position-accumulator-record">Table 15: Position Accumulator Record Fields</a>
     */
    data class PositionAccumulatorRecord(override var line: UInt, override var column: UInt) : PositionWithLineAndColumn

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-AccumulateIndex">Section 9.3.4.1.1: AccumulateIndex(accumulator, increment)</a>
     */
    private fun accumulateIndex(accumulator: IndexAccumulatorRecord, increment: Int): Int {
        // 1. Set accumulator.[[Index]] to accumulator.[[Index]] + increment.
        val accumulatedIndex = accumulator.index.toLong() + increment
        // 2. If accumulator.[[Index]] < 0, then
        //       a. Optionally report an error.
        //       b. Set accumulator.[[Index]] to 0.
        accumulator.index = accumulatedIndex
            .coerceIn(0L, Int.MAX_VALUE.toLong())
            .toInt()
        // 3. Return accumulator.[[Index]].
        return accumulator.index
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-AccumulatePosition">Section 9.3.4.1.2: AccumulatePosition(accumulator, lineIncrement, columnIncrement)</a>
     */
    private fun accumulatePosition(
        accumulator: PositionAccumulatorRecord,
        lineIncrement: UInt,
        columnIncrement: UInt,
    ): PositionRecord {
        // 1. Set accumulator.[[Line]] to accumulator.[[Line]] + lineIncrement.
        accumulator.line += lineIncrement
        // 2. If lineIncrement = 0, then
        //       a. Set accumulator.[[Column]] to accumulator.[[Column]] + columnIncrement.
        // 3. Else,
        //       a. Set accumulator.[[Column]] to columnIncrement.
        accumulator.column = if (lineIncrement == 0u) accumulator.column + columnIncrement else columnIncrement
        // 4. Return { [[Line]]: accumulator.[[Line]], [[Column]]: accumulator.[[Column]] }.
        return PositionRecord(accumulator.line, accumulator.column)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-DecodeSourceScopes">Section 9.3.4.2: DecodeSourceScopes(scopes, names)</a>
     */
    internal fun decodeSourceScopes(
        scopes: String?,
        names: List<String>,
    ): ParsingResult<List<OriginalScopeRecord>> {
        // 1. If scopes is null, return « ».
        if (scopes == null) return Success([])

        // 2. Let parsedScopes be the root Parse Node when parsing scopes using Scopes as the goal symbol.
        val stream = ParserStream(scopes)
        // 3. If parsing failed, then
        //       a. Optionally report an error.
        //       b. Return « ».
        //       c. If the stream is not fully consumed, optionally report an error and return « ».
        // (the failure should be reported directly from the parser)
        val parsedScopes = context(stream) {
            val result = parseScopes().ifFailure { return it }
            if (!stream.isEnded) {
                return Failure("Unexpected '${stream.current}' at position ${stream.position}")
            }
            result
        }

        // 4. Let scopePosition be a new Position Accumulator Record { [[Line]]: 0, [[Column]]: 0 }.
        // 5. Let scopeNameIndex be a new Index Accumulator Record { [[Index]]: 0 }.
        // 6. Let scopeKindIndex be a new Index Accumulator Record { [[Index]]: 0 }.
        // 7. Let scopeVariableIndex be a new Index Accumulator Record { [[Index]]: 0 }.
        // 8. Let state be a new Decode Scope State Record { [[ScopePosition]]: scopePosition, [[ScopeNameIndex]]: scopeNameIndex, [[ScopeKindIndex]]: scopeKindIndex, [[ScopeVariableIndex]]: scopeVariableIndex }.
        val state = DecodeScopeStateRecord(
            scopePosition = PositionAccumulatorRecord(0u, 0u),
            scopeNameIndex = IndexAccumulatorRecord(0),
            scopeKindIndex = IndexAccumulatorRecord(0),
            scopeVariableIndex = IndexAccumulatorRecord(0),
        )

        // 9. Return DecodedTopLevelOriginalScopeTrees of parsedScopes with arguments state and names.
        return context(state, names) {
            decodedTopLevelOriginalScopeTrees(parsedScopes)
        }
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-DecodedTopLevelOriginalScopeTrees">Section 9.3.4.4: DecodedTopLevelOriginalScopeTrees</a>
     */
    context(state: DecodeScopeStateRecord, names: List<String>)
    private fun decodedTopLevelOriginalScopeTrees(
        originalScopes: List<OriginalScopeTreeItem>?,
    ): ParsingResult<List<OriginalScopeRecord>> {
        // 1. If OriginalScopeTreeList is not present, return « ».
        if (originalScopes == null) return Success([])
        // OriginalScopeTreeList : OriginalScopeTreeList `,` OriginalScopeTreeItem
        return originalScopes.map { originalScope ->
            when (originalScope) {
                // OriginalScopeTreeItem : OriginalScopeTree
                is OriginalScopeTree -> {
                    // 1. Return the DecodedOriginalScopeTrees of OriginalScopeTree with arguments state and names.
                    decodedOriginalScopeTrees(originalScope).ifFailure { failure -> return failure }
                }
            }
        }.let(::Success)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-DecodedOriginalScopeTrees">Section 9.3.4.4.1: DecodedOriginalScopeTrees</a>
     */
    context(state: DecodeScopeStateRecord, names: List<String>)
    private fun decodedOriginalScopeTrees(originalScopeTree: OriginalScopeTree): ParsingResult<OriginalScopeRecord> {
        // OriginalScopeTree : OriginalScopeStart OriginalScopeVariablesItem? OriginalScopeItemList? `,` OriginalScopeEnd
        // 1. Let start be the DecodedPosition of OriginalScopeStart with argument state.[[ScopePosition]].
        val start = context(state.scopePosition) {
            decodedPosition(originalScopeTree.start.line, originalScopeTree.start.column)
        }.ifFailure { return it }

        // 2. Let name be the OriginalScopeName of OriginalScopeStart with arguments state.[[ScopeNameIndex]] and names.
        val name = context(state.scopeNameIndex) {
            originalScopeName(originalScopeTree.start)
        }.ifFailure { return it }

        // 3. Let kind be the OriginalScopeKind of OriginalScopeStart with arguments state.[[ScopeKindIndex]] and names.
        val kind = context(state.scopeKindIndex) {
            originalScopeKind(originalScopeTree.start)
        }.ifFailure { return it }

        // 4. Let flags be the VLQUnsignedValue of OriginalScopeStart's ScopeFlags nonterminal.
        val flags = originalScopeTree.start.flags

        // 5. Let originalScope be the Original Scope Record { [[Start]]: start, [[End]]: start, [[Name]]: name, [[Kind]]: kind, [[Variables]]: « », [[Children]]: « », [[IsStackFrame]]: false }.
        val originalScope = OriginalScopeRecord(
            start = start,
            end = start,
            name = name,
            kind = kind,
            variables = [],
            children = [],
            isStackFrame = false,
        )

        // 6. If flags & 0x4 = 0x4, set originalScope.[[IsStackFrame]] to true.
        if ((flags and 0x4u) == 0x4u)
            originalScope.isStackFrame = true

        // 7. If OriginalScopeVariablesItem is present, then
        if (originalScopeTree.variables != null) {
            // a. Set originalScope.[[Variables]] to the OriginalScopeVariables of OriginalScopeVariablesItem with arguments state.[[ScopeVariableIndex]] and names.
            originalScope.variables = context(state.scopeVariableIndex) {
                originalScopeVariables(originalScopeTree.variables)
            }.ifFailure { return it }
        }

        // 8. If OriginalScopeItemList is present, then
        if (originalScopeTree.items != null) {
            // a. Set originalScope.[[Children]] to the DecodedOriginalScopeTrees of OriginalScopeItemList with arguments state and names.
            // OriginalScopeItemList : OriginalScopeItemList `,` OriginalScopeItem
            originalScope.children = originalScopeTree.items
                .filterIsInstance<OriginalScopeTree>()
                .map { item ->
                    decodedOriginalScopeTrees(item).ifFailure { return it }
                }
        }

        // 9. Set originalScope.[[End]] to the DecodedPosition of OriginalScopeEnd with argument state.[[ScopePosition]].
        originalScope.end = context(state.scopePosition) {
            decodedPosition(originalScopeTree.end.line, originalScopeTree.end.column)
        }.ifFailure { return it }

        // 10. Return « originalScope ».
        return Success(originalScope)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-DecodedPosition">Section 9.3.4.4.2: DecodedPosition</a>
     */
    context(accumulator: PositionAccumulatorRecord)
    private fun decodedPosition(
        line: Vlq?,
        column: Vlq,
    ): ParsingResult<PositionRecord> {
        // OriginalScopeStart : `B` ScopeFlags ScopeLine ScopeColumn ScopeNameOrKind? ScopeKind?
        // OriginalScopeEnd : `C` ScopeLine ScopeColumn
        // GeneratedRangeStart : `E` RangeFlags RangeLine? RangeColumn RangeDefinition?
        // GeneratedRangeEnd : `F` RangeLine? RangeColumn
        // (the scope and range productions are merged here; ScopeLine is always present, RangeLine is optional)
        // 1. If RangeLine is present, let relativeLine be the VLQUnsignedValue of RangeLine, otherwise let relativeLine be 0.
        val relativeLine = line?.let { vlqUnsignedValue(it) }?.ifFailure { return it }
        // 2. Let relativeColumn be the VLQUnsignedValue of RangeColumn.
        val relativeColumn = vlqUnsignedValue(column).ifFailure { return it }

        // 3. Return AccumulatePosition(accumulator, relativeLine, relativeColumn).
        return Success(
            accumulatePosition(
                accumulator,
                relativeLine ?: 0u,
                relativeColumn
            )
        )
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-OriginalScopeName">Section 9.3.4.4.3: OriginalScopeName</a>
     */
    context(accumulator: IndexAccumulatorRecord, names: List<String>)
    private fun originalScopeName(
        scopeStart: OriginalScopeStart,
    ): ParsingResult<String?> {
        // 1. Let flags be the VLQUnsignedValue of ScopeFlags.
        val [flags] = scopeStart
        // 2. If flags & 0x1 ≠ 0x1, return null.
        if ((flags and 0x1u) != 0x1u) return Success(null)
        // 3. Assert: ScopeNameOrKind is present.
        require(scopeStart.nameOrKind != null) { "ScopeNameOrKind expected, got null" }
        // 4. Return RelativeName(ScopeNameOrKind, accumulator, names).
        return relativeName(scopeStart.nameOrKind)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-OriginalScopeKind">Section 9.3.4.4.4: OriginalScopeKind</a>
     */
    context(accumulator: IndexAccumulatorRecord, names: List<String>)
    private fun originalScopeKind(
        scopeStart: OriginalScopeStart,
    ): ParsingResult<String?> {
        // 1. Let flags be the VLQUnsignedValue of ScopeFlags.
        val [flags] = scopeStart
        // 2. If flags & 0x2 ≠ 0x2, return null.
        if ((flags and 0x2u) != 0x2u) return Success(null)
        // 3. Assert: ScopeNameOrKind is present.
        require(scopeStart.nameOrKind != null) { "ScopeNameOrKind expected, got null" }
        // 4. If flags & 0x1 = 0x1, then
        if ((flags and 0x1u) == 0x1u) {
            // a. Assert: ScopeKind is present.
            require(scopeStart.kind != null) { "ScopeKind expected, got null" }
            // b. Return RelativeName(ScopeKind, accumulator, names).
            return relativeName(scopeStart.kind)
        }
        // 5. Return RelativeName(ScopeNameOrKind, accumulator, names).
        return relativeName(scopeStart.nameOrKind)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-OriginalScopeVariables">Section 9.3.4.4.5: OriginalScopeVariables</a>
     */
    context(accumulator: IndexAccumulatorRecord, names: List<String>)
    private fun originalScopeVariables(
        scopeVariablesList: List<ScopeVariable>,
    ): ParsingResult<List<String>> {
        // ScopeVariableList : ScopeVariableList ScopeVariable
        return scopeVariablesList.map { scopeVariable ->
            // ScopeVariable : Vlq
            // 1. Let variable be RelativeName(Vlq, accumulator, names).
            val variable = relativeName(scopeVariable).ifFailure { failure -> return failure }
            // 2. If variable is null, return « "" ».
            // 3. Return « variable ».
            variable ?: ""
        }.let(::Success)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-RelativeName">Section 9.3.4.4.6: RelativeName(vlq, accumulator, names)</a>
     */
    context(accumulator: IndexAccumulatorRecord, names: List<String>)
    private fun relativeName(vlqIndex: Vlq): ParsingResult<String?> {
        // 1. Let relativeIndex be the VLQSignedValue of vlq.
        val relativeIndex = vlqSignedValue(vlqIndex).ifFailure { return it }
        // 2. Let index be AccumulateIndex(accumulator, relativeIndex).
        val index = accumulateIndex(accumulator, relativeIndex)
        // 3. If index >= the length of names, then
        //       a. Optionally report an error.
        //       b. Return null.
        if (index >= names.size) return Failure("Relative index of the name exceeds the size of names field")
        // 4. Return names[index].
        return Success(names[index])
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-DecodeGeneratedRanges">Section 9.3.4.3: DecodeGeneratedRanges(ranges, sources, names)</a>
     */
    internal fun decodeGeneratedRanges(
        ranges: String?,
        sources: List<DecodedSourceRecord>,
        names: List<String>
    ): ParsingResult<List<GeneratedRangeRecord>> {
        // 1. If ranges is null, return « ».
        if (ranges == null) return Success([])

        // 2. Let parsedRanges be the root Parse Node when parsing ranges using Ranges as the goal symbol.
        val stream = ParserStream(ranges)
        // 3. If parsing failed, then
        //       a. Optionally report an error.
        //       b. Return « ».
        //       c. If the stream is not fully consumed, optionally report an error and return « ».
        // (the failure should be reported directly from the parser)
        val parsedRanges = context(stream) {
            val result = parseRanges().ifFailure { return it }
            if (!stream.isEnded) {
                return Failure("Unexpected '${stream.current}' at position ${stream.position}")
            }
            result
        }

        // 4. Let rangePosition be a new Position Accumulator Record { [[Line]]: 0, [[Column]]: 0 }.
        // 5. Let rangeDefinitionSourceIndex be a new Index Accumulator Record { [[Index]]: 0 }.
        // 6. Let rangeDefinitionScopeIndex be a new Index Accumulator Record { [[Index]]: 0 }.
        // 7. Let state be a new Decode Range State Record { [[RangePosition]]: rangePosition, [[RangeDefinitionSourceIndex]]: rangeDefinitionSourceIndex [[RangeDefinitionScopeIndex]]: rangeDefinitionScopeIndex }.
        val state = DecodeRangeStateRecord(
            rangePosition = PositionAccumulatorRecord(0u, 0u),
            rangeDefinitionSourceIndex = IndexAccumulatorRecord(0),
            rangeDefinitionScopeIndex = IndexAccumulatorRecord(0),
        )

        // 8. Let result be the DecodedGeneratedRangeTrees of parsedRanges with arguments state, sources and names.
        return context(state, sources, names) {
            decodedGeneratedRangeTrees(parsedRanges)
        }
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-DecodedGeneratedRangeTrees">Section 9.3.4.5: DecodedGeneratedRangeTrees</a>
     */
    context(state: DecodeRangeStateRecord, sources: List<DecodedSourceRecord>, names: List<String>)
    private fun decodedGeneratedRangeTrees(topLevelItems: List<TopLevelItem>?): ParsingResult<List<GeneratedRangeRecord>> {
        // 1. If TopLevelItemList is not present, return « ».
        if (topLevelItems == null) return Success([])
        // TopLevelItemList : TopLevelItemList `,` TopLevelItem
        return topLevelItems
            .filterIsInstance<GeneratedRangeTree>()
            .map { generatedRangeTree ->
                decodedGeneratedRangeTree(generatedRangeTree).ifFailure { return it }
            }
            .let(::Success)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-DecodedGeneratedRangeTrees">Section 9.3.4.5: DecodedGeneratedRangeTrees</a>
     */
    context(state: DecodeRangeStateRecord, sources: List<DecodedSourceRecord>, names: List<String>)
    private fun decodedGeneratedRangeTree(generatedRangeTree: GeneratedRangeTree): ParsingResult<GeneratedRangeRecord> {
        // GeneratedRangeTree : GeneratedRangeStart GeneratedRangeBindingsItem? GeneratedRangeCallSiteItem? GeneratedRangeItemList? `,` GeneratedRangeEnd
        // 1. Let start be the DecodedPosition of GeneratedRangeStart with argument state.[[RangePosition]].
        val start = context(state.rangePosition) {
            decodedPosition(generatedRangeTree.start.line, generatedRangeTree.start.column)
        }.ifFailure { return it }

        // 2. Let definition be the GeneratedRangeDefinition of GeneratedRangeStart with arguments state and sources.
        val definition = context(state) {
            generatedRangeDefinition(generatedRangeTree.start, sources)
        }.ifFailure { return it }

        // 3. Let flags be the VLQUnsignedValue of GeneratedRangeStart's RangeFlags nonterminal.
        val flags = generatedRangeTree.start.flags
        // 4. If flags & 0xc = 0xc, then
        //       a. Let stackFrameType be ~hidden~.
        // 5. Else if flags & 0x4 = 0x4, then
        //       a. Let stackFrameType be ~original~.
        // 6. Else,
        //       a. If flags & 0x8 = 0x8, optionally report an error.
        //       b. Let stackFrameType be ~none~.
        val stackFrameType = when {
            (flags and 0xcu) == 0xcu -> StackFrameType.HIDDEN
            (flags and 0x4u) == 0x4u -> StackFrameType.ORIGINAL
            (flags and 0x8u) == 0x8u -> return Failure("Generated range has the hidden flag without the stack frame flag")
            else -> StackFrameType.NONE
        }

        // 7. If GeneratedRangeBindingsItem is present, then
        //       a. Let bindings be the GeneratedRangeBindings of GeneratedRangeBindingsItem with arguments start and names.
        // 8. Else,
        //       a. Let bindings be « ».
        val bindings = generatedRangeTree.bindings?.let { bindingsItem ->
            generatedRangeBindings(bindingsItem.bindings.list, start).ifFailure { failure -> return failure }
        } ?: mutableListOf()

        // 9. If GeneratedRangeCallSiteItem is present, then
        //       a. Let callSite be the GeneratedRangeCallSite of GeneratedRangeCallSiteItem.
        // 10. Else,
        //       a. Let callSite be null.
        val callSite = generatedRangeTree.callSite?.let { callSiteItem ->
            generatedRangeCallSite(callSiteItem.callSite, sources).ifFailure { failure -> return failure }
        }

        // 11. If GeneratedRangeItemList is present, then
        //       a. Let children be the DecodedGeneratedRangeTrees of GeneratedRangeItemList with arguments state and names.
        // 12. Else,
        //       a. Let children be « ».
        // GeneratedRangeItemList : GeneratedRangeItemList `,` GeneratedRangeItem
        val children = generatedRangeTree.items
            ?.filterIsInstance<GeneratedRangeTree>()
            ?.map { child ->
                decodedGeneratedRangeTree(child).ifFailure { return it }
            }
            ?: emptyList()

        // 13. If GeneratedRangeItemList is present, then
        if (generatedRangeTree.items != null) {
            // a. Let subRangeBindings be the GeneratedSubRangeBindings of GeneratedRangeItemList with arguments start and names.
            val subRangeBindings = generatedSubRangeBindings(generatedRangeTree.items, start).ifFailure { return it }
            val rangeBindings = bindings
            // b. For each Sub-Range Binding Record subRangeBinding of subRangeBindings, do
            for ((variableIndex, bindings) in subRangeBindings) {
                //       i. Let index be subRangeBinding.[[VariableIndex]].
                //       ii. If index < the length of bindings, then
                //             1. Set bindings[index] to the list-concatenation of bindings[index] and subRangeBinding.[[Bindings]].
                //       iii. Else,
                //             1. Optionally report an error.
                if (variableIndex >= rangeBindings.size.toUInt()) {
                    return Failure("Sub-range binding variable index $variableIndex exceeds the number of generated range bindings")
                }
                rangeBindings[variableIndex.toInt()].addAll(bindings)
            }
        }

        // 14. Let end be the DecodedPosition of GeneratedRangeEnd with argument state.[[RangePosition]].
        val end = context(state.rangePosition) {
            decodedPosition(generatedRangeTree.end.line, generatedRangeTree.end.column)
        }.ifFailure { return it }

        // 15. Return « Generated Range Record { [[Start]]: start, [[End]]: end, [[Definition]]: definition, [[StackFrameType]]: stackFrameType, [[Bindings]]: bindings, [[CallSite]]: callSite, [[Children]]: children } ».
        return Success(
            GeneratedRangeRecord(
                start = start,
                end = end,
                definition = definition,
                stackFrameType = stackFrameType,
                bindings = bindings,
                callSite = callSite,
                children = children,
            )
        )
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-GeneratedRangeDefinition">Section 9.3.4.5.1: GeneratedRangeDefinition</a>
     */
    context(state: DecodeRangeStateRecord)
    private fun generatedRangeDefinition(
        generatedRangeStart: GeneratedRangeStart,
        sources: List<DecodedSourceRecord>,
    ): ParsingResult<OriginalScopeRecord?> {
        // 1. Let flags be the VLQUnsignedValue of RangeFlags.
        val flags = generatedRangeStart.flags
        // 2. If flags & 0x2 ≠ 0x2, return null.
        if ((flags and 0x2u) != 0x2u) return Success(null)
        // 3. Assert: RangeDefinition is present.
        val definition = requireNotNull(generatedRangeStart.definition) { "RangeDefinition expected, got null" }
        // 4. Return GeneratedRangeDefinition of RangeDefinition with arguments state and sources.
        // RangeDefinition : DefinitionSourceIdx DefinitionScopeIdx
        // 1. Let sourceOffset be the VLQSignedValue of DefinitionSourceIdx.
        val sourceOffset = vlqSignedValue(definition.sourceIdx).ifFailure { return it }
        // 2. Let sourceIndex be AccumulateIndex(state.[[RangeDefinitionSourceIndex]], sourceOffset).
        val sourceIndex = accumulateIndex(state.rangeDefinitionSourceIndex, sourceOffset)
        if (sourceIndex >= sources.size) {
            return Failure("Generated range definition source index $sourceIndex exceeds the number of sources")
        }
        // 3. Let source be sources[sourceIndex].
        val source = sources[sourceIndex]
        // 4. If sourceOffset = 0, then
        //       a. Let scopeOffset be the VLQSignedValue of DefinitionScopeIdx.
        // 5. Else,
        //       a. Set state.[[RangeDefinitionScopeIndex]].[[Index]] to 0.
        //       b. Let scopeOffset be the VLQUnsignedValue of DefinitionScopeIdx.
        // (the scope offset is signed and relative within the same source, but unsigned and effectively
        //  absolute when the source changes)
        val scopeOffset = if (sourceOffset == 0) {
            vlqSignedValue(definition.scopeIdx).ifFailure { return it }
        } else {
            state.rangeDefinitionScopeIndex.index = 0
            vlqUnsignedValue(definition.scopeIdx).ifFailure { return it }.toInt()
        }
        // 6. Let scopeIndex be AccumulateIndex(state.[[RangeDefinitionScopeIndex]], scopeOffset).
        val scopeIndex = accumulateIndex(state.rangeDefinitionScopeIndex, scopeOffset)
        // 7. Return ScopeForScopeIndex(source.[[RootScopes]], scopeIndex).
        return scopeForScopeIndex(source.rootScopes, scopeIndex)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-ScopeForScopeIndex">Section 9.3.4.4.7: ScopeForScopeIndex(scopes, index)</a>
     */
    private fun scopeForScopeIndex(scopes: List<OriginalScopeRecord>?, index: Int): ParsingResult<OriginalScopeRecord> {
        // 1. Return FlattenedScopes(scopes)[index].
        val flattened = flattenedScopes(scopes ?: emptyList())
        if (index < 0 || index >= flattened.size) {
            return Failure("Generated range definition scope index $index exceeds the number of original scopes")
        }
        return Success(flattened[index])
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-FlattenedScopes">Section 9.3.4.4.7.1: FlattenedScopes(scopes)</a>
     */
    private fun flattenedScopes(scopes: List<OriginalScopeRecord>): List<OriginalScopeRecord> {
        // 1. Let result be « ».
        val result = mutableListOf<OriginalScopeRecord>()
        // 2. For each Original Scope Record scope of scopes, do
        for (scope in scopes) {
            //       a. Append scope to result.
            result.add(scope)
            //       b. Append each element of FlattenedScopes(scope.[[Children]]) to result.
            result.addAll(flattenedScopes(scope.children))
        }
        // 3. Return result.
        return result
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-GeneratedRangeCallSite">Section 9.3.4.5.2: GeneratedRangeCallSite</a>
     */
    data class GeneratedRangeCallSiteRecord(
        val source: DecodedSourceRecord,
        override val line: UInt,
        override val column: UInt,
    ) : PositionWithLineAndColumn

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-GeneratedRangeCallSite">Section 9.3.4.5.2: GeneratedRangeCallSite</a>
     */
    private fun generatedRangeCallSite(
        generatedRangeCallSite: GeneratedRangeCallSite,
        sources: List<DecodedSourceRecord>,
    ): ParsingResult<GeneratedRangeCallSiteRecord> {
        // 1. Let sourceIndex be the VLQUnsignedValue of CallSiteSourceIdx.
        val sourceIndex = vlqUnsignedValue(generatedRangeCallSite.sourceIndex).ifFailure { return it }
        // 2. Let line be the VLQUnsignedValue of CallSiteLine.
        val line = vlqUnsignedValue(generatedRangeCallSite.line).ifFailure { return it }
        // 3. Let column be the VLQUnsignedValue of CallSiteColumn.
        val column = vlqUnsignedValue(generatedRangeCallSite.column).ifFailure { return it }
        // 4. Return a new Original Position Record { [[Source]]: sources[sourceIndex], [[Line]]: line, [[Column]]: column }.
        if (sourceIndex >= sources.size.toUInt()) {
            return Failure("Generated range call site source index $sourceIndex exceeds the number of sources")
        }
        return Success(GeneratedRangeCallSiteRecord(sources[sourceIndex.toInt()], line, column))
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-GeneratedRangeBindings">Section 9.3.4.5.3: GeneratedRangeBindings</a>
     */
    context(names: List<String>)
    private fun generatedRangeBindings(
        bindingExpressions: List<BindingExpression>,
        start: PositionRecord,
    ): ParsingResult<MutableList<MutableList<BindingRecord>>> {
        // BindingExpressionList : BindingExpressionList BindingExpression
        return bindingExpressions.mapTo(mutableListOf()) { expression ->
            // BindingExpression : Vlq
            // 1. Let binding be the BindingExpression of BindingExpression with argument names.
            val binding = bindingExpression(expression).ifFailure { return it }
            // 2. Return « « { [[From]]: start, [[Binding]]: binding } » ».
            mutableListOf(BindingRecord(start, binding))
        }.let(::Success)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-BindingExpression">Section 9.3.4.5.4: BindingExpression</a>
     */
    context(names: List<String>)
    private fun bindingExpression(bindingExpression: BindingExpression): ParsingResult<String?> {
        // 1. Let unadjustedBindingIndex be the VLQUnsignedValue of Vlq.
        val unadjustedBindingIndex = vlqUnsignedValue(bindingExpression).ifFailure { return it }
        // 2. If unadjustedBindingIndex = 0, return null.
        if (unadjustedBindingIndex == 0u) return Success(null)
        // 3. Let bindingIndex be unadjustedBindingIndex - 1.
        val bindingIndex = unadjustedBindingIndex - 1u
        // 4. If bindingIndex >= the length of names, then
        //       a. Optionally report an error.
        //       b. Return null.
        if (bindingIndex >= names.size.toUInt()) return Success(null)
        // 5. Return names[bindingIndex].
        return Success(names[bindingIndex.toInt()])
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sub-range-binding-records">Table 16: Sub-Range Binding Record Fields</a>
     */
    data class SubRangeBindingRecord(
        val variableIndex: UInt,
        val bindings: List<BindingRecord>,
    )

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-GeneratedSubRangeBindings">Section 9.3.4.5.5: GeneratedSubRangeBindings</a>
     */
    context(names: List<String>)
    private fun generatedSubRangeBindings(
        generatedRangeItems: List<GeneratedRangeItem>,
        start: PositionRecord,
    ): ParsingResult<List<SubRangeBindingRecord>> {
        // GeneratedRangeItemList : GeneratedRangeItemList `,` GeneratedRangeItem
        return generatedRangeItems
            // GeneratedRangeItem : GeneratedRangeTree
            // GeneratedRangeItem : VendorExtensionItem
            // GeneratedRangeItem : InvalidRangeItem
            // 1. Return « ».
            .filterIsInstance<GeneratedSubRangeBinding>()
            .map { generatedSubRangeBinding ->
                // GeneratedSubRangeBinding : `H` VariableIndex BindingFromList
                // 1. Let variableIndex be the VLQUnsignedValue of VariableIndex.
                val variableIndex = vlqUnsignedValue(generatedSubRangeBinding.variableIndex).ifFailure { return it }
                // 2. Let from be a new Position Accumulator Record { [[Line]]: start.[[Line]], [[Column]]: start.[[Column]] }.
                val from = PositionAccumulatorRecord(start.line, start.column)
                // 3. Let bindings be the SubRangeBinding of BindingFromList with arguments from and names.
                val bindings = context(from) {
                    subRangeBinding(generatedSubRangeBinding.bindings).ifFailure { return it }
                }
                // 4. Return « { [[VariableIndex]]: variableIndex, [[Bindings]]: bindings } ».
                SubRangeBindingRecord(variableIndex, bindings)
            }
            .let(::Success)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-SubRangeBinding">Section 9.3.4.5.6: SubRangeBinding</a>
     */
    context(from: PositionAccumulatorRecord, names: List<String>)
    private fun subRangeBinding(bindingsFrom: List<BindingFrom>): ParsingResult<List<BindingRecord>> {
        // BindingFromList : BindingFromList BindingFrom
        return bindingsFrom.map { bindingFrom ->
            // BindingFrom : BindingLine BindingColumn BindingExpression
            // 1. Let relativeLine be the VLQUnsignedValue of BindingLine.
            val relativeLine = vlqUnsignedValue(bindingFrom.line).ifFailure { return it }
            // 2. Let relativeColumn be the VLQUnsignedValue of BindingColumn.
            val relativeColumn = vlqUnsignedValue(bindingFrom.column).ifFailure { return it }
            // 3. Let fromPosition be AccumulatePosition(from, relativeLine, relativeColumn).
            val fromPosition = accumulatePosition(from, relativeLine, relativeColumn)
            // 4. Let binding be the BindingExpression of BindingExpression with argument names.
            val binding = bindingExpression(bindingFrom.expression).ifFailure { return it }
            // 5. Return « { [[From]]: fromPosition, [[Binding]]: binding } ».
            BindingRecord(fromPosition, binding)
        }.let(::Success)
    }

    //
    // 9.4 Resolving sources

    /**
     * @see <a href="https://tc39.es/ecma426/branch/proposal-scopes/#sec-DecodeSourceMapSources">Section 9.4.1: DecodeSourceMapSources(baseURL, sourceRoot, sources, sourcesContent, ignoreList, encodedScopes, names)</a>
     */
    private fun decodeSourceMapSources(
        baseUrl: String, // URL,
        sourceRoot: String?,
        sources: List<String?>,
        sourcesContent: List<String?>,
        ignoreList: List<UInt>,
        encodedScopes: List<String?>,
        names: List<String>,
    ): ParsingResult<List<DecodedSourceRecord>> {
        // 1. Let decodedSources be a new empty List.
        val decodedSources = mutableListOf<DecodedSourceRecord>()
        // 2. Let sourcesContentCount be the number of elements in sourcesContent.
        val sourcesContentCount = sourcesContent.size
        // 3. Let sourceUrlPrefix be "".
        var sourceUrlPrefix = ""
        // 4. If sourceRoot ≠ null, then
        if (sourceRoot != null) {
            // a. If sourceRoot ends with the code point U+002F (SOLIDUS), then
            if (sourceRoot.endsWith('\u002F')) {
                // i. Set sourceUrlPrefix to sourceRoot.
                sourceUrlPrefix = sourceRoot
            } else { // b. Else,
                // i. Set sourceUrlPrefix to the string-concatenation of sourceRoot and "/".
                sourceUrlPrefix = "$sourceRoot/"
            }
        }
        // 5. Let index be 0.
        // 6. Repeat, while index < sources' length,
        for (index in 0 until sources.size) {
            // a. Let source be sources[index].
            var source = sources[index]
            // b. Let decodedSource be the Decoded Source Record { [[URL]]: null, [[Content]]: null, [[Ignored]]: false, [[RootScopes]]: null }.
            val decodedSource = DecodedSourceRecord(url = null, content = null, ignored = false, rootScopes = null)
            // c. If source ≠ null, then
            if (source != null) {
                // i. Set source to the string-concatenation of sourceUrlPrefix and source.
                source = "$sourceUrlPrefix$source"
                // ii. Let sourceURL be the result of URL parsing source with baseURL.
                // iii. If sourceURL is failure, optionally report an error.
                // (Skip URL validation this since the sourceURL is specified by users and not by the compiler)
                // TODO: However, it definitely makes sense to check that the provided source URL exists
                // iv. Else, set decodedSource.[[URL]] to sourceURL.
                decodedSource.url = source
            }
            // d. If ignoreList contains index, set decodedSource.[[Ignored]] to true.
            if (ignoreList.contains(index.toUInt())) {
                decodedSource.ignored = true
            }
            // e. If sourcesContentCount > index, set decodedSource.[[Content]] to sourcesContent[index].
            if (sourcesContentCount > index) {
                decodedSource.content = sourcesContent[index]
            }
            // f. If index < encodedScopes' length and encodedScopes[index] is not null, then
            val encodedSourceScopes = encodedScopes.getOrNull(index)
            if (encodedSourceScopes != null) {
                // i. Let decodedSourceScopes be DecodeSourceScopes(encodedScopes[index], names).
                val decodedSourceScopes = decodeSourceScopes(encodedSourceScopes, names).ifFailure { return it }
                // ii. Set decodedSource.[[RootScopes]] to decodedSourceScopes.
                decodedSource.rootScopes = decodedSourceScopes
            }
            // g. Append decodedSource to decodedSources.
            decodedSources.add(decodedSource)
        }
        // 7. Return decodedSources.
        return Success(decodedSources)
    }

    //
    // 10. Index source maps

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeIndexSourceMap">Section 10.1: DecodeIndexSourceMap(json, baseURL)</a>
     */
    private fun decodeIndexSourceMap(json: JsonObject, baseUrl: String): ParsingResult<DecodedSourceMapRecord> {
        TODO("The compiler is not supposed to generate sections, if it started, please implement it based on the Section 10.1")
    }

    //
    // Utility functions and implementation types

    /** Common representation of [TerminalDigit] and [ContinuationDigit] parse nodes. */
    private sealed interface VlqDigit {
        val value: UInt
    }

    /** Returns whether the current character terminates a mapping segment. */
    private fun ParserStream.isMappingDelimiter(): Boolean =
        isEnded || current == ',' || current == ';'

    /** Converts a Base64 character to its numeric value, or returns -1 when the character is invalid. */
    private fun base64ToValue(char: Char): Int {
        return when (char) {
            in 'A'..'Z' -> char - 'A'
            in 'a'..'z' -> char - 'a' + 26
            in '0'..'9' -> char - '0' + 52
            '+' -> 62
            '/' -> 63
            else -> -1
        }
    }

    /**
     * Parses a required VLQ value and reports a field-specific failure when it is absent.
     */
    context(stream: ParserStream)
    private fun parseRequiredVlq(fieldName: String): ParsingResult<Vlq> {
        return parseVlq().required(fieldName, stream)
    }

    /** The three possible results of comparing two source-map positions. */
    private enum class ComparisonResult(val value: Int) { LESSER(-1), EQUAL(0), GREATER(1) }

    /** Common contract for implementation types that carry a line and column. */
    private sealed interface PositionWithLineAndColumn {
        val line: UInt
        val column: UInt
    }

    /** Returns whether an optional JSON property is missing. */
    private val JsonNode?.isMissing: Boolean
        inline get() {
            contract {
                returns(true) implies (this@isMissing == null)
                returns(false) implies (this@isMissing != null)
            }
            return this == null
        }

    /** Represents success, absence of a grammar match, or a parsing failure. */
    sealed interface ParsingResult<out T> {
        data class Success<T>(val value: T) : ParsingResult<T>
        class NoMatch<T> : ParsingResult<T>
        data class Failure<T>(val message: String, val cause: Throwable? = null) : ParsingResult<T>
    }

    /** Performs a contract-aware parser precondition check. */
    private inline fun expect(value: Boolean, localReturn: () -> Nothing) {
        contract {
            returns() implies value
        }
        if (!value) localReturn()
    }

    /** Checks a JSON node type while preserving smart-cast information. */
    private inline fun <reified T : JsonNode> expectType(value: JsonNode, localReturn: (String, String) -> Nothing) {
        contract {
            returns() implies (value is T)
        }
        if (value !is T) localReturn(T::class.simpleName.toString(), value::class.simpleName.toString())
    }

    /** Reports a parser expectation when two values differ. */
    private inline fun expectToBe(actual: Any, expected: Any, localReturn: (Any, Any) -> Unit) {
        if (actual != expected) localReturn(expected, actual)
    }

    /**
     * Unwraps a successful parsing result or returns the non-success result from the caller.
     */
    private inline fun <A, B> ParsingResult<A>.ifFailure(localReturn: (ParsingResult<B>) -> Nothing): A {
        when (this) {
            is Success -> return value
            is Failure, is NoMatch -> {
                @Suppress("UNCHECKED_CAST")
                localReturn(this as ParsingResult<B>)
            }
        }
    }

    /** Transforms the value of a successful parsing result. */
    private inline fun <A, B> ParsingResult<A>.map(f: (A) -> B): ParsingResult<B> {
        return when (this) {
            is Success -> Success(f(value))
            is Failure, is NoMatch -> {
                @Suppress("UNCHECKED_CAST")
                this as ParsingResult<B>
            }
        }
    }

    /** Replaces a missing grammar match with a default value. */
    private inline fun <T> ParsingResult<T>.defaultIfNoMatch(factory: () -> T): ParsingResult<T> {
        return when (this) {
            is Failure, is Success -> this
            is NoMatch -> Success(factory())
        }
    }

    /** Converts a missing grammar match into a parsing failure. */
    private fun <T> ParsingResult<T>.required(expected: String, stream: ParserStream): ParsingResult<T> {
        return when (this) {
            is Failure, is Success -> this
            is NoMatch -> Failure("$expected expected, got '${stream.current ?: "<EOF>"}' at position ${stream.position}")
        }
    }

    /** Parses and consumes the expected character. */
    private fun ParserStream.parseChar(value: Char): ParsingResult<Char> {
        return when (current) {
            value -> Success(value).also { advance() }
            else -> NoMatch()
        }
    }

    /** Parses a required character. */
    private fun ParserStream.expectChar(value: Char): ParsingResult<Char> {
        return parseChar(value).required("'$value' character", this)
    }

    /** Parses one or more values separated by the specified character. */
    private inline fun <T> ParserStream.parseManySeparated(separator: Char, parser: () -> ParsingResult<T>): ParsingResult<List<T>> {
        buildList {
            val first = withRollback(parser).ifFailure { return it }
            add(first)

            while (true) {
                val checkpoint = position
                when (parseChar(separator)) {
                    is Failure -> error("parseChar cannot fail")
                    is NoMatch -> return Success(this)
                    is Success -> {}
                }

                when (val result = parser()) {
                    is Failure -> return Failure(result.message, result.cause)
                    is NoMatch -> {
                        position = checkpoint
                        return Success(this)
                    }
                    is Success -> add(result.value)
                }
            }
        }
    }

    /** Parses one or more consecutive values. */
    private inline fun <T> ParserStream.parseMany(parser: () -> ParsingResult<T>): ParsingResult<List<T>> {
        buildList {
            val first = withRollback(parser).ifFailure { return it }
            add(first)

            while (true) {
                when (val current = withRollback(parser)) {
                    is Failure -> return Failure(current.message, current.cause)
                    is NoMatch -> return Success(this)
                    is Success -> add(current.value)
                }
            }
        }
    }

    /** Parses the first matching grammar alternative. */
    private fun <T> ParserStream.parseOneOf(vararg parser: () -> ParsingResult<T>): ParsingResult<T> {
        for (candidate in parser) {
            when (val result = withRollback(candidate)) {
                is Success, is Failure -> return result
                is NoMatch -> {}
            }
        }

        return NoMatch()
    }

    /** Runs [parser] and restores the stream position when the grammar does not match. */
    private inline fun <T> ParserStream.withRollback(parser: () -> ParsingResult<T>): ParsingResult<T> {
        val checkpoint = position
        return when (val result = parser()) {
            is NoMatch -> result.also { position = checkpoint }
            is Success, is Failure -> result
        }
    }

    /** Parses an optional grammar production. */
    private inline fun <T> ParserStream.parseOptional(parser: () -> ParsingResult<T>): ParsingResult<T?> {
        return when (val result = withRollback(parser)) {
            is Success, is Failure -> result
            is NoMatch -> Success(null)
        }
    }

    /** A cursor over the source-map field currently being parsed. */
    private class ParserStream(
        private val input: String,
        var position: Int = 0,
    ) {
        val current: Char? get() = input.getOrNull(position)
        val length: Int get() = input.length
        val isEnded: Boolean get() = position >= length

        fun advance(): Char? =
            current?.also {
                position++
            }
    }

    /** Tags used by the scopes grammar to identify item kinds. */
    private object VlqKindTag {
        const val ORIGINAL_SCOPE_START = 'B'
        const val ORIGINAL_SCOPE_END = 'C'
        const val ORIGINAL_SCOPE_VARIABLES = 'D'

        const val GENERATED_RANGE_START = 'E'
        const val GENERATED_RANGE_END = 'F'
        const val GENERATED_RANGE_BINDINGS = 'G'
        const val GENERATED_SUB_RANGE_BINDINGS = 'H'
        const val GENERATED_RANGE_CALL_SITE = 'I'

        const val VENDOR_EXTENSION = '/'

        /** Tags reserved by the `Scopes` grammar — anything else is an [InvalidScopeItem]. */
        val knownScopeTags = setOf(
            ORIGINAL_SCOPE_START, ORIGINAL_SCOPE_END, ORIGINAL_SCOPE_VARIABLES,
            VENDOR_EXTENSION
        )

        /** Tags reserved by the `Ranges` grammar — anything else is an [InvalidRangeItem]. */
        val knownRangeTags = setOf(
            GENERATED_RANGE_START, GENERATED_RANGE_END, GENERATED_RANGE_BINDINGS, GENERATED_SUB_RANGE_BINDINGS, GENERATED_RANGE_CALL_SITE,
            VENDOR_EXTENSION
        )
    }
}
