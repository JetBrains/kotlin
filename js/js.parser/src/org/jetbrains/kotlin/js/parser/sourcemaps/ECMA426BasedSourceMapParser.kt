/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(kotlin.contracts.ExperimentalContracts::class)
package org.jetbrains.kotlin.js.parser.sourcemaps

import kotlin.contracts.contract
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.ParsingResult.Failure
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.ParsingResult.Success

/**
 * Parse and validates source map files against the ECMA-426 specification.
 *
 * The used version is a draft on March 2, 2026
 *
 * **Note:** The compiler doesn't generate a source map containing "sections", so the validation doesn't include a case with the ["Section 10: Index source map"](https://tc39.es/ecma426/#sec-index-source-map)
 * 
 * @see <a href="https://tc39.es/ecma426/">ECMA-426: Source Map Format Specification</a>
 */
object ECMA426BasedSourceMapParser {
    /**
     * @see <a href="https://tc39.es/ecma426/#sec-VLQSignedValue">Section 6.1: VLQSignedValue</a>
     */
    context(parser: MappingsParser)
    private fun vlqSignedValue(): ParsingResult<Int> {
        // 1. Let unsigned be the VLQUnsignedValue of VlqDigitList.
        val unsigned = vlqUnsignedValue().ifFailure { return it }
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
        if (value >= 2147483648u) return Failure("VLQ value exceeds maximum signed integer limit at position ${parser.currentPosition}")
        // 7. Return sign × value.
        return Success(sign * value.toInt())
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-VLQUnsignedValue">Section 6.2: VLQUnsignedValue</a>
     */
    context(parser: MappingsParser)
    private fun vlqUnsignedValue(): ParsingResult<UInt> {
        // 1. Let value be the VLQUnsignedValue of VlqDigitList.
        val value = vlqUnsignedValueForDigitList().ifFailure { return it }
        // 2. If value is ≥ 2**32, throw an error.
        if (value >= 4294967296u) return Failure("VLQ value exceeds maximum unsigned integer limit at position ${parser.currentPosition}")
        // 3. Return value.
        return Success(value)
    }

    context(parser: MappingsParser)
    private fun vlqUnsignedValueForDigitList(): ParsingResult<UInt> {
        val (value, isTerminal) = parser.popVlqDigit().ifFailure { return it }
        return when (isTerminal) {
            false -> {
                // 1. Let left be the VLQUnsignedValue of ContinuationDigit.
                val left = vlqUnsignedValueForContinuationDigit(value).ifFailure { return it }
                // 2. Let right be the VLQUnsignedValue of VlqDigitList.
                val right = vlqUnsignedValue().ifFailure { return it }
                // 3. Return left + right × 2**5.
                Success(left + right * 32u)
            }
            true -> vlqUnsignedValueForTerminalDigit(value)
        }
    }

    private fun vlqUnsignedValueForContinuationDigit(value: UInt): ParsingResult<UInt> {
        // 1. Let digit be the character matched by this production.
        // 2. Let value be the integer corresponding to digit, according to the base64 encoding as defined by IETF RFC 4648.
        // 3. Assert: 32 ≤ value < 64.
        expect(value in 32u..<64u) { return Failure("Invalid continuation digit value: $value") }
        // 4. Return value - 32.
        return Success(value - 32u)
    }

    private fun vlqUnsignedValueForTerminalDigit(value: UInt): ParsingResult<UInt> {
        // 1. Let digit be the character matched by this production.
        // 2. Let value be the integer corresponding to digit, according to the base64 encoding as defined by IETF RFC 4648.
        expect(value < 32u) { return Failure("Invalid terminal digit value: $value") }
        return Success(value)
    }

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

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-position-record-type">Table 8.1: Position Record Fields</a>
     */
    data class PositionRecord(override val line: UInt, override val column: UInt) : PositionWithLineAndColumn

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-original-position-record-type">Table 8.2: Original Position Record Fields</a>
     */
    data class OriginalPositionRecord(val source: DecodedSourceRecord, override val line: UInt, override val column: UInt) :
        PositionWithLineAndColumn

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-ComparePositions">Section 8.3: ComparePositions (first, second)</a>
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

    private enum class ComparisonResult(val value: Int) { LESSER(-1), EQUAL(0), GREATER(1) }

    /**
     * Section 9.1.1 ParseSourceMap(string, baseUrl)
     *
     * @see <a href="https://tc39.es/ecma426/#sec-ParseSourceMap">Section 9.1.1: ParseSourceMap(string, baseUrl)</a>
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
        // 10. Let sources be DecodeSourceMapSources(baseURL, sourceRootField, sourcesField, sourcesContentField, ignoreListField).
        val sources = decodeSourceMapSources(baseUrl, sourceRootField, sourcesField, sourcesContentField, ignoreListField).ifFailure { return it }
        // 11. Let namesField be GetOptionalListOfStrings(json, "names").
        val namesField = getOptionalListOfStrings(json, "names").ifFailure { return it }
        // 12. Let mappings be DecodeMappings(mappingsField, namesField, sources).
        val mappings = decodeMappings(mappingsField, namesField, sources).ifFailure { return it }
        // 13. Sort mappings in ascending order, with a Decoded Mapping Record a being less than a Decoded Mapping Record b if ComparePositions(a.[[GeneratedPosition]], b.[[GeneratedPosition]]) is lesser.
        mappings.sortedWith { record1, record2 -> comparePositions(record1.generatedPosition, record2.generatedPosition).value }
        // 14. Return the Decoded Source Map Record { [[File]]: fileField, [[Sources]]: sources, [[Mappings]]: mappings }.
        return Success(DecodedSourceMapRecord(fileField, sources, mappings))
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
        for ((index, item) in jsonArrayIterate(values).withIndex()) {
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
        for ((index, item) in jsonArrayIterate(values).withIndex()) {
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
     * @see <a href="https://tc39.es/ecma426/#sec-JSONArrayIterate">Section 9.1.2.4: GetOptionalListOfArrayIndexes(object, key)</a>
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
        for ((index, item) in jsonArrayIterate(values).withIndex()) {
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


    /**
     *
     * Parsing based on the specified grammar
     *
     * @see <a href="https://tc39.es/ecma426/#sec-mappings-grammar">Section: 9.2.1 Mappings grammar</a>
     */
    private class MappingsParser(private val input: String) {
        private var pos = 0
        private var isFirstLine = true
        private var isFirstMapping = true
        private var hasAtLeastOneVlqDigit = false

        val currentPosition: Int get() = pos

        fun popLine(): ParsingResult<Boolean?> {
            if (isFirstLine) {
                isFirstLine = false
                return Success(parseLine())
            }

            if (pos < input.length && input[pos] == ';') {
                pos++
                return Success(parseLine())
            }

            expect(pos == input.length) { return Failure("Unexpected remaining file content at position $currentPosition") }

            return Success(null)
        }

        fun parseLine(): Boolean {
            isFirstMapping = true
            // Check if line is empty (next char is ';' or end of input)
            return pos >= input.length || input[pos] == ';'
        }

        fun popMapping(): Boolean {
            // Parse first mapping
            if (isFirstMapping) {
                isFirstMapping = false
                return true
            }

            if (pos < input.length && input[pos] == ',') {
                pos++
                return true
            }

            return false
        }

        fun parseGeneratedColumn() {
            hasAtLeastOneVlqDigit = false
        }

        fun parseOriginalSource() {
            hasAtLeastOneVlqDigit = false
        }

        fun parseOriginalLine() {
            hasAtLeastOneVlqDigit = false
        }

        fun parseOriginalColumn() {
            hasAtLeastOneVlqDigit = false
        }

        fun parseName() {
            hasAtLeastOneVlqDigit = false
        }

        fun popVlqDigit(): ParsingResult<VlqDigit> {
            if (pos < input.length) {
                hasAtLeastOneVlqDigit = true
                return parseVlgDigit()
            }

            expect(hasAtLeastOneVlqDigit) { return Failure("Empty VLQ value at position $currentPosition") }
            error("Attempt to pop digit after a TerminalDigitNode")
        }

        fun hasOnlyGeneratedColumn(): Boolean =
            pos >= input.length || input[pos] == ',' || input[pos] == ';'

        fun hasName(): Boolean =
            pos < input.length && input[pos] != ',' && input[pos] != ';'

        fun parseVlgDigit(): ParsingResult<VlqDigit> {
            val char = input[pos]
            val digitValue = base64ToValue(char)

            expect(digitValue >= 0) { return Failure("Invalid base64 character '$char' at position $currentPosition") }

            pos++

            // Check if this is a continuation digit (bit 5 is set)
            return Success(VlqDigit(digitValue.toUInt(), isTerminal = (digitValue and 0x20) == 0))
        }

        /**
         * Maps base64 character to its numeric value according to the spec
         */
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
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeMappingsField">Section 9.2.1.1: DecodeMappingsField</a>
     */
    context(parser: MappingsParser, state: DecodeMappingStateRecord, mappings: MutableList<DecodedMappingRecord>, names: List<String>, sources: List<DecodedSourceRecord>)
    private tailrec fun decodeMappingsFieldForLineList(): ParsingResult<Unit> {
        val isMappingListEmpty = parser.popLine().ifFailure { return it } ?: return Success(Unit)
        // 1. Perform DecodeMappingsField of Line with arguments state, mappings, names and sources.
        decodeMappingsFieldForLine(isMappingListEmpty).ifFailure { return it }
        // 2. Set state.[[GeneratedLine]] to state.[[GeneratedLine]] + 1.
        state.generatedLine++
        // 3. Set state.[[GeneratedColumn]] to 0.
        state.generatedColumn = 0
        // 4. Perform DecodeMappingsField of LineList with arguments state, mappings, names and sources.
        return decodeMappingsFieldForLineList()
    }

    context(parser: MappingsParser, state: DecodeMappingStateRecord, mappings: MutableList<DecodedMappingRecord>, names: List<String>, sources: List<DecodedSourceRecord>)
    private fun decodeMappingsFieldForLine(isMappingListEmpty: Boolean): ParsingResult<Unit> {
        /** [empty] */
        if (isMappingListEmpty) return Success(Unit)
        /** MappingList */
        return decodeMappingsFieldMappingList()
    }

    context(parser: MappingsParser, state: DecodeMappingStateRecord, mappings: MutableList<DecodedMappingRecord>, names: List<String>, sources: List<DecodedSourceRecord>)
    private tailrec fun decodeMappingsFieldMappingList(): ParsingResult<Unit> {
        if (!parser.popMapping()) return Success(Unit)
        // 1. Perform DecodeMappingsField of Mapping with arguments state, mappings, names and sources.
        decodeMappingsFieldForMapping().ifFailure { return it }
        // 2. Perform DecodeMappingsField of MappingList with arguments state, mappings, names and sources.
        return decodeMappingsFieldMappingList()
    }

    context(parser: MappingsParser, state: DecodeMappingStateRecord, mappings: MutableList<DecodedMappingRecord>, names: List<String>, sources: List<DecodedSourceRecord>)
    private fun decodeMappingsFieldForMapping(): ParsingResult<Unit> {
        // 1. Perform DecodeMappingsField of GeneratedColumn with arguments state, mappings, names and sources.
        decodeMappingsFieldForGeneratedColumn().ifFailure { return it }
        // 2. If state.[[GeneratedColumn]] < 0, then
        if (state.generatedColumn < 0) {
            // a. Optionally report an error.
            // b. Return.
            return Failure("Negative generated column at position ${parser.currentPosition}")
        }
        /**  Mapping :: GeneratedColumn */
        if (parser.hasOnlyGeneratedColumn()) {
            // 3. Let position be a new Position Record { [[Line]]: state.[[GeneratedLine]], [[Column]]: state.[[GeneratedColumn]] }.
            val position = PositionRecord(state.generatedLine, state.generatedColumn.toUInt())
            // 4. Let decodedMapping be a new DecodedMappingRecord { [[GeneratedPosition]]: position, [[OriginalPosition]]: null, [[Name]]: null }.
            val decodedMapping = DecodedMappingRecord(position, null, null)
            // 5. Append decodedMapping to mappings.
            mappings.add(decodedMapping)
        }
        /**  Mapping :: GeneratedColumn OriginalSource OriginalLine OriginalColumn Name?  */
        else {
            // 3. Let generatedPosition be a new Position Record { [[Line]]: state.[[GeneratedLine]], [[Column]]: state.[[GeneratedColumn]] }.
            val generatedPosition = PositionRecord(state.generatedLine, state.generatedColumn.toUInt())
            // 4. Perform DecodeMappingsField of OriginalSource with arguments state, mappings, names and sources.
            decodeMappingsFieldForOriginalSource().ifFailure { return it }
            // 5. Perform DecodeMappingsField of OriginalLine with arguments state, mappings, names and sources.
            decodeMappingsFieldForOriginalLine().ifFailure { return it }
            // 6. Perform DecodeMappingsField of OriginalColumn with arguments state, mappings, names and sources.
            decodeMappingsFieldForOriginalColumn().ifFailure { return it }
            // 7. If state.[[SourceIndex]] < 0 or state.[[SourceIndex]] ≥ the number of elements of sources or state.[[OriginalLine]] < 0 or state.[[OriginalColumn]] < 0, then
            //       a. Optionally report an error.
            //       b. Let originalPosition be null.
            when {
                state.sourceIndex < 0 -> return Failure("Negative source index at position ${parser.currentPosition}")
                state.sourceIndex >= sources.size -> return Failure("Source index out of bounds at position ${parser.currentPosition}")
                state.originalLine < 0 -> return Failure("Negative original line at position ${parser.currentPosition}")
                state.originalColumn < 0 -> return Failure("Negative original column at position ${parser.currentPosition}")
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
            if (parser.hasName()) {
                // a. Perform DecodeMappingsField of Name with arguments state, mappings, names and sources.
                decodeMappingsFieldForName().ifFailure { return it }
                // b. If state.[[NameIndex]] < 0 or state.[[NameIndex]] ≥ the number of elements of names, optionally report an error.
                when {
                    state.nameIndex < 0 -> return Failure("Negative name index at position ${parser.currentPosition}")
                    state.nameIndex >= names.size -> return Failure("Name index out of bounds at position ${parser.currentPosition}")
                }
                // c. Else, set name to names[state.[[NameIndex]]].
                name = names[state.nameIndex]
            }
            // 11. Let decodedMapping be a new DecodedMappingRecord { [[GeneratedPosition]]: generatedPosition, [[OriginalPosition]]: originalPosition, [[Name]]: name }.
            val decodedMapping = DecodedMappingRecord(generatedPosition, originalPosition, name)
            // 12. Append decodedMapping to mappings.
            mappings.add(decodedMapping)
        }

        return Success(Unit)
    }

    context(parser: MappingsParser, state: DecodeMappingStateRecord)
    private fun decodeMappingsFieldForGeneratedColumn(): ParsingResult<Unit> {
        parser.parseGeneratedColumn()
        // 1. Let relativeColumn be the VLQSignedValue of Vlq.
        val relativeColumn = vlqSignedValue().ifFailure { return it }
        // 2. Set state.[[GeneratedColumn]] to state.[[GeneratedColumn]] + relativeColumn.
        state.generatedColumn += relativeColumn
        return Success(Unit)
    }

    context(parser: MappingsParser, state: DecodeMappingStateRecord)
    private fun decodeMappingsFieldForOriginalSource(): ParsingResult<Unit> {
        parser.parseOriginalSource()
        // 1. Let relativeSourceIndex be the VLQSignedValue of Vlq.
        val relativeSourceIndex = vlqSignedValue().ifFailure { return it }
        // 2. Set state.[[SourceIndex]] to state.[[SourceIndex]] + relativeSourceIndex.
        state.sourceIndex += relativeSourceIndex
        return Success(Unit)
    }

    context(parser: MappingsParser, state: DecodeMappingStateRecord)
    private fun decodeMappingsFieldForOriginalLine(): ParsingResult<Unit> {
        parser.parseOriginalLine()
        // 1. Let relativeLine be the VLQSignedValue of Vlq.
        val relativeLine = vlqSignedValue().ifFailure { return it }
        // 2. Set state.[[OriginalLine]] to state.[[OriginalLine]] + relativeLine.
        state.originalLine += relativeLine
        return Success(Unit)
    }

    context(parser: MappingsParser, state: DecodeMappingStateRecord)
    private fun decodeMappingsFieldForOriginalColumn(): ParsingResult<Unit> {
        parser.parseOriginalColumn()
        // 1. Let relativeColumn be the VLQSignedValue of Vlq.
        val relativeColumn = vlqSignedValue().ifFailure { return it }
        // 2. Set state.[[OriginalColumn]] to state.[[OriginalColumn]] + relativeColumn.
        state.originalColumn += relativeColumn
        return Success(Unit)
    }

    context(parser: MappingsParser, state: DecodeMappingStateRecord)
    private fun decodeMappingsFieldForName(): ParsingResult<Unit> {
        parser.parseName()
        // 1. Let relativeName be the VLQSignedValue of Vlq.
        val relativeName = vlqSignedValue().ifFailure { return it }
        // 2. Set state.[[NameIndex]] to state.[[NameIndex]] + relativeName.
        state.nameIndex += relativeName
        return Success(Unit)
    }

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeMappings">Section 9.2.2: DecodeMappings</a>
     */
    private fun decodeMappings(
        rawMappings: String,
        names: List<String>,
        sources: List<DecodedSourceRecord>
    ): ParsingResult<List<DecodedMappingRecord>> {
        // 1. Let mappings be a new empty List.
        val mappings = mutableListOf<DecodedMappingRecord>()
        // 2. Let mappingsNode be the root Parse Node when parsing rawMappings using MappingsField as the goal symbol.
        val mappingsParser = MappingsParser(rawMappings)
        // 3. If parsing failed, then
        //       a. Optionally report an error.
        //       b. Return mappings.
        // (the failure should be reported directly from the parser)

        // 4. Let state be a new Decode Mapping State Record with all fields set to 0.
        val state = DecodeMappingStateRecord()

        // 5. Perform DecodeMappingsField of mappingsNode with arguments state, mappings, names and sources.
        context(mappingsParser, state, mappings, names, sources) {
            decodeMappingsFieldForLineList().ifFailure { return it }
        }

        // 6. Return mappings.
        return Success(mappings)
    }


    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeSourceMapSources">Section 9.3.1: DecodeSourceMapSources</a>
     */
    private fun decodeSourceMapSources(
        baseUrl: String, // URL,
        sourceRoot: String?,
        sources: List<String?>,
        sourcesContent: List<String?>,
        ignoreList: List<UInt>
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
            // b. Let decodedSource be the Decoded Source Record { [[URL]]: null, [[Content]]: null, [[Ignored]]: false }.
            val decodedSource = DecodedSourceRecord(url = null, content = null, ignored = false)
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
            // f. Append decodedSource to decodedSources.
            decodedSources.add(decodedSource)
        }
        // 7. Return decodedSources.
        return Success(decodedSources)
    }


    /**
     * @see <a href="https://tc39.es/ecma426/#decoded-source-map-record">Table 9.3: Fields of Decoded Source Map Records</a>
     */
    data class DecodedSourceMapRecord(
        val file: String?,
        val sources: List<DecodedSourceRecord>,
        val mappings: List<DecodedMappingRecord>,
    )

    /**
     * @see <a href="https://tc39.es/ecma426/#decoded-source-record">Table 9.4: Fields of Decoded Source Records</a>
     */
    class DecodedSourceRecord(var url: String?, var content: String?, var ignored: Boolean)

    /**
     * @see <a href="https://tc39.es/ecma426/#decoded-mapping-record">Table 9.5: Fields of Decoded Mapping Records</a>
     */
    data class DecodedMappingRecord(
        val generatedPosition: PositionRecord,
        val originalPosition: OriginalPositionRecord?,
        val name: String?,
    )

    /**
     * @see <a href="https://tc39.es/ecma426/#decode-mapping-state-record">Table 9.6: Fields of Decode Mapping State Records</a>
     */
    private class DecodeMappingStateRecord(
        var generatedLine: UInt = 0u,
        var generatedColumn: Int = 0,
        var sourceIndex: Int = 0,
        var originalLine: Int = 0,
        var originalColumn: Int = 0,
        var nameIndex: Int = 0
    )

    /**
     * @see <a href="https://tc39.es/ecma426/#sec-DecodeIndexSourceMap">Section 10.1: DecodeIndexSourceMap(json, baseURL)</a>
     */
    private fun decodeIndexSourceMap(json: JsonObject, baseUrl: String): ParsingResult<DecodedSourceMapRecord> {
        TODO("The compiler is not supposed to generate sections, if it started, please implement it based on the Section 10.1")
    }


    // Pack of helper functions and classes not specified in the specification, but just to simplify Kotlin specific implementation
    private data class VlqDigit(val value: UInt, val isTerminal: Boolean)

    private sealed interface PositionWithLineAndColumn {
        val line: UInt
        val column: UInt
    }

    private val JsonNode?.isMissing: Boolean
        inline get() {
            contract {
                returns(true) implies (this@isMissing == null)
                returns(false) implies (this@isMissing != null)
            }
            return this == null
        }

    sealed interface ParsingResult<T> {
        data class Success<T>(val value: T) : ParsingResult<T>
        data class Failure<T>(val message: String, val cause: Throwable? = null) : ParsingResult<T>
    }

    private inline fun expect(value: Boolean, localReturn: () -> Nothing) {
        contract {
            returns() implies value
        }
        if (!value) localReturn()
    }

    private inline fun <reified T : JsonNode> expectType(value: JsonNode, localReturn: (String, String) -> Nothing) {
        contract {
            returns() implies (value is T)
        }
        if (value !is T) localReturn(T::class.simpleName.toString(), value::class.simpleName.toString())
    }

    private inline fun expectToBe(actual: Any, expect: Any, localReturn: (Any, Any) -> Unit) {
        if (actual != expect) localReturn(expect, actual)
    }

    private inline fun <A, B> ParsingResult<A>.ifFailure(localReturn: (ParsingResult<B>) -> Nothing): A {
        when (this) {
            is Success -> return value
            is Failure -> {
                @Suppress("UNCHECKED_CAST")
                localReturn(this as ParsingResult<B>)
            }
        }
    }
}
