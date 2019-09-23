/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.js.ast

private val NOT_INLINE = 10
private val AFFECTS_STATE = 10

class SimpleJsAstProtoReader(val source: ByteArray) {

    var offset = 0

    var currentEnd = source.size

    val hasData: Boolean
        get() = offset < currentEnd

    inline fun <T> readWithLength(block: () -> T): T {
        val length = readInt32()
        val oldEnd = currentEnd
        currentEnd = offset + length
        try {
            return block()
        } finally {
            currentEnd = oldEnd
        }
    }

    private fun nextByte(): Byte {
        if (!hasData) error("Oops")
        return source[offset++]
    }

    private fun readVarint64(): Long {
        var result = 0L

        var shift = 0
        while (true) {
            val b = nextByte().toInt()

            result = result or ((b and 0x7F).toLong() shl shift)
            shift += 7

            if ((b and 0x80) == 0) break
        }

        if (shift > 70) {
            error("int64 overflow $shift")
        }

        return result
    }

    private fun readVarint32(): Int {
        var result = 0

        var shift = 0
        while (true) {
            val b = nextByte().toInt()

            result = result or ((b and 0x7F) shl shift)
            shift += 7

            if ((b and 0x80) == 0) break
        }

        if (shift > 70) {
            error("int32 overflow $shift")
        }

        return result
    }

    fun readInt32(): Int = readVarint32()

    fun readInt64(): Long = readVarint64()

    fun readBool(): Boolean = readVarint32() != 0

    fun readFloat(): Float {
        var bits = nextByte().toInt()
        bits = (bits shl 8) or nextByte().toInt()
        bits = (bits shl 8) or nextByte().toInt()
        bits = (bits shl 8) or nextByte().toInt()

        return Float.fromBits(bits)
    }

    fun readDouble(): Double {
        var bits = nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()

        return Double.fromBits(bits)
    }

    fun readString(): String {
        val length = readInt32()
        val result = String(source, offset, length)
        offset += length
        return result
    }

    inline fun <T> readField(block: (fieldNumber: Int, type: Int) -> T): T {
        val wire = readInt32()
        val fieldNumber = wire ushr 3
        val wireType = wire and 0x7
        return block(fieldNumber, wireType)
    }

    fun skip(type: Int) {
        when (type) {
            0 -> readInt64()
            1 -> offset += 8
            2 -> {
                val len = readInt32()
                offset += len
            }
            3, 4 -> error("groups")
            5 -> offset += 4
        }
    }

    inline fun <T> delayed(o: Int, block: () -> T): T {
        val oldOffset = offset

        try {
            offset = o
            return block()
        } finally {
            offset = oldOffset
        }
    }

    fun createLocation(startLine : Int, startChar : Int): Any = arrayOf<Any?>(startLine, startChar)

    fun createSideEffects(index: Int): Any = index

    fun createJsImportedModule(externalName : Int, internalName : Int, plainReference : Any?): Any = arrayOf<Any?>(externalName, internalName, plainReference)

    fun createExpression_simpleNameReference(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfSimpleNameReference : Int): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfSimpleNameReference)
    fun createExpression_thisLiteral(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfThisLiteral : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfThisLiteral)
    fun createExpression_nullLiteral(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfNullLiteral : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfNullLiteral)
    fun createExpression_trueLiteral(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfTrueLiteral : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfTrueLiteral)
    fun createExpression_falseLiteral(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfFalseLiteral : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfFalseLiteral)
    fun createExpression_stringLiteral(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfStringLiteral : Int): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfStringLiteral)
    fun createExpression_regExpLiteral(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfRegExpLiteral : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfRegExpLiteral)
    fun createExpression_intLiteral(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfIntLiteral : Int): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfIntLiteral)
    fun createExpression_doubleLiteral(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfDoubleLiteral : Double): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfDoubleLiteral)
    fun createExpression_arrayLiteral(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfArrayLiteral : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfArrayLiteral)
    fun createExpression_objectLiteral(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfObjectLiteral : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfObjectLiteral)
    fun createExpression_function(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfFunction : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfFunction)
    fun createExpression_docComment(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfDocComment : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfDocComment)
    fun createExpression_binary(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfBinary : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfBinary)
    fun createExpression_unary(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfUnary : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfUnary)
    fun createExpression_conditional(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfConditional : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfConditional)
    fun createExpression_arrayAccess(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfArrayAccess : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfArrayAccess)
    fun createExpression_nameReference(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfNameReference : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfNameReference)
    fun createExpression_propertyReference(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfPropertyReference : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfPropertyReference)
    fun createExpression_invocation(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfInvocation : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfInvocation)
    fun createExpression_instantiation(fileId : Int?, location : Any?, synthetic : Boolean?, sideEffects : Any?, localAlias : Any?, oneOfInstantiation : Any): Any = arrayOf<Any?>(fileId, location, synthetic, sideEffects, localAlias, oneOfInstantiation)

    fun createThisLiteral(): Any = arrayOf<Any?>()

    fun createNullLiteral(): Any = arrayOf<Any?>()

    fun createTrueLiteral(): Any = arrayOf<Any?>()

    fun createFalseLiteral(): Any = arrayOf<Any?>()

    fun createRegExpLiteral(patternStringId : Int, flagsStringId : Int?): Any = arrayOf<Any?>(patternStringId, flagsStringId)

    fun createArrayLiteral(element : List<Any>): Any = arrayOf<Any?>(element)

    fun createObjectLiteral(entry : List<Any>, multiline : Boolean?): Any = arrayOf<Any?>(entry, multiline)

    fun createObjectLiteralEntry(key : Any, value : Any): Any = arrayOf<Any?>(key, value)

    fun createFunction(parameter : List<Any>, nameId : Int?, body : Any, local : Boolean?): Any = arrayOf<Any?>(parameter, nameId, body, local)

    fun createParameter(nameId : Int, hasDefaultValue : Boolean?): Any = arrayOf<Any?>(nameId, hasDefaultValue)

    fun createDocComment(tag : List<Any>): Any = arrayOf<Any?>(tag)

    fun createDocCommentTag_valueStringId(nameId : Int, oneOfValueStringId : Int): Any = arrayOf<Any?>(nameId, oneOfValueStringId)
    fun createDocCommentTag_expression(nameId : Int, oneOfExpression : Any): Any = arrayOf<Any?>(nameId, oneOfExpression)

    fun createBinaryOperationType(index: Int): Any = index

    fun createBinaryOperation(left : Any, right : Any, type_ : Any): Any = arrayOf<Any?>(left, right, type_)

    fun createUnaryOperationType(index: Int): Any = index

    fun createUnaryOperation(operand : Any, type_ : Any, postfix : Boolean): Any = arrayOf<Any?>(operand, type_, postfix)

    fun createConditional(testExpression : Any, thenExpression : Any, elseExpression : Any): Any = arrayOf<Any?>(testExpression, thenExpression, elseExpression)

    fun createArrayAccess(array : Any, index : Any): Any = arrayOf<Any?>(array, index)

    fun createNameReference(nameId : Int, qualifier : Any?, inlineStrategy : Any?): Any = arrayOf<Any?>(nameId, qualifier, inlineStrategy)

    fun createPropertyReference(stringId : Int, qualifier : Any?, inlineStrategy : Any?): Any = arrayOf<Any?>(stringId, qualifier, inlineStrategy)

    fun createInvocation(qualifier : Any, argument : List<Any>, inlineStrategy : Any?): Any = arrayOf<Any?>(qualifier, argument, inlineStrategy)

    fun createInstantiation(qualifier : Any, argument : List<Any>): Any = arrayOf<Any?>(qualifier, argument)

    fun createStatement_returnStatement(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfReturnStatement : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfReturnStatement)
    fun createStatement_throwStatement(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfThrowStatement : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfThrowStatement)
    fun createStatement_breakStatement(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfBreakStatement : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfBreakStatement)
    fun createStatement_continueStatement(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfContinueStatement : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfContinueStatement)
    fun createStatement_debugger(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfDebugger : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfDebugger)
    fun createStatement_expression(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfExpression : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfExpression)
    fun createStatement_vars(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfVars : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfVars)
    fun createStatement_block(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfBlock : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfBlock)
    fun createStatement_globalBlock(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfGlobalBlock : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfGlobalBlock)
    fun createStatement_label(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfLabel : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfLabel)
    fun createStatement_ifStatement(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfIfStatement : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfIfStatement)
    fun createStatement_switchStatement(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfSwitchStatement : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfSwitchStatement)
    fun createStatement_whileStatement(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfWhileStatement : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfWhileStatement)
    fun createStatement_doWhileStatement(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfDoWhileStatement : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfDoWhileStatement)
    fun createStatement_forStatement(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfForStatement : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfForStatement)
    fun createStatement_forInStatement(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfForInStatement : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfForInStatement)
    fun createStatement_tryStatement(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfTryStatement : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfTryStatement)
    fun createStatement_empty(fileId : Int?, location : Any?, synthetic : Boolean?, oneOfEmpty : Any): Any = arrayOf<Any?>(fileId, location, synthetic, oneOfEmpty)

    fun createReturn(value : Any?): Any = arrayOf<Any?>(value)

    fun createThrow(exception : Any): Any = arrayOf<Any?>(exception)

    fun createBreak(labelId : Int?): Any = arrayOf<Any?>(labelId)

    fun createContinue(labelId : Int?): Any = arrayOf<Any?>(labelId)

    fun createDebugger(): Any = arrayOf<Any?>()

    fun createExpressionStatement(expression : Any, exportedTagId : Int?): Any = arrayOf<Any?>(expression, exportedTagId)

    fun createVars(declaration : List<Any>, multiline : Boolean?, exportedPackageId : Int?): Any = arrayOf<Any?>(declaration, multiline, exportedPackageId)

    fun createVarDeclaration(nameId : Int, initialValue : Any?, fileId : Int?, location : Any?): Any = arrayOf<Any?>(nameId, initialValue, fileId, location)

    fun createBlock(statement : List<Any>): Any = arrayOf<Any?>(statement)

    fun createGlobalBlock(statement : List<Any>): Any = arrayOf<Any?>(statement)

    fun createLabel(nameId : Int, innerStatement : Any): Any = arrayOf<Any?>(nameId, innerStatement)

    fun createIf(condition : Any, thenStatement : Any, elseStatement : Any?): Any = arrayOf<Any?>(condition, thenStatement, elseStatement)

    fun createSwitch(expression : Any, entry : List<Any>): Any = arrayOf<Any?>(expression, entry)

    fun createSwitchEntry(label : Any?, statement : List<Any>, fileId : Int?, location : Any?): Any = arrayOf<Any?>(label, statement, fileId, location)

    fun createWhile(condition : Any, body : Any): Any = arrayOf<Any?>(condition, body)

    fun createDoWhile(condition : Any, body : Any): Any = arrayOf<Any?>(condition, body)

    fun createFor_variables(oneOfVariables : Any, condition : Any?, increment : Any?, body : Any): Any = arrayOf<Any?>(oneOfVariables, condition, increment, body)
    fun createFor_expression(oneOfExpression : Any, condition : Any?, increment : Any?, body : Any): Any = arrayOf<Any?>(oneOfExpression, condition, increment, body)
    fun createFor_empty(oneOfEmpty : Any, condition : Any?, increment : Any?, body : Any): Any = arrayOf<Any?>(oneOfEmpty, condition, increment, body)

    fun createEmptyInit(): Any = arrayOf<Any?>()

    fun createForIn_nameId(oneOfNameId : Int, iterable : Any, body : Any): Any = arrayOf<Any?>(oneOfNameId, iterable, body)
    fun createForIn_expression(oneOfExpression : Any, iterable : Any, body : Any): Any = arrayOf<Any?>(oneOfExpression, iterable, body)

    fun createTry(tryBlock : Any, catchBlock : Any?, finallyBlock : Any?): Any = arrayOf<Any?>(tryBlock, catchBlock, finallyBlock)

    fun createCatch(parameter : Any, body : Any): Any = arrayOf<Any?>(parameter, body)

    fun createEmpty(): Any = arrayOf<Any?>()

    fun createInlineStrategy(index: Int): Any = index

    fun createFragment(importedModule : List<Any>, importEntry : List<Any>, declarationBlock : Any?, exportBlock : Any?, initializerBlock : Any?, nameBinding : List<Any>, classModel : List<Any>, moduleExpression : List<Any>, inlineModule : List<Any>, packageFqn : String?, testsInvocation : Any?, mainInvocation : Any?, inlinedLocalDeclarations : List<Any>): Any = arrayOf<Any?>(importedModule, importEntry, declarationBlock, exportBlock, initializerBlock, nameBinding, classModel, moduleExpression, inlineModule, packageFqn, testsInvocation, mainInvocation, inlinedLocalDeclarations)

    fun createInlinedLocalDeclarations(tag : Int, block : Any): Any = arrayOf<Any?>(tag, block)

    fun createImportedModule(externalNameId : Int, internalNameId : Int, plainReference : Any?): Any = arrayOf<Any?>(externalNameId, internalNameId, plainReference)

    fun createImport(signatureId : Int, expression : Any): Any = arrayOf<Any?>(signatureId, expression)

    fun createNameBinding(signatureId : Int, nameId : Int): Any = arrayOf<Any?>(signatureId, nameId)

    fun createClassModel(nameId : Int, superNameId : Int?, interfaceNameId : List<Int>, postDeclarationBlock : Any?): Any = arrayOf<Any?>(nameId, superNameId, interfaceNameId, postDeclarationBlock)

    fun createInlineModule(signatureId : Int, expressionId : Int): Any = arrayOf<Any?>(signatureId, expressionId)

    fun createStringTable(entry : List<String>): Any = arrayOf<Any?>(entry)

    fun createNameTable(entry : List<Any>): Any = arrayOf<Any?>(entry)

    fun createName(temporary : Boolean, identifier : Int?, localNameId : Any?, imported : Boolean?, specialFunction : Any?): Any = arrayOf<Any?>(temporary, identifier, localNameId, imported, specialFunction)

    fun createLocalAlias(localNameId : Int, tag : Int?): Any = arrayOf<Any?>(localNameId, tag)

    fun createSpecialFunction(index: Int): Any = index

    fun createChunk(stringTable : Any, nameTable : Any, fragment : Any): Any = arrayOf<Any?>(stringTable, nameTable, fragment)

    fun createInlineData(inlineFunctionTags : List<String>): Any = arrayOf<Any?>(inlineFunctionTags)

    open fun readLocation(): Any {
        var startLine: Int = 0
        var startChar: Int = 0
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> startLine = readInt32()
                    2 -> startChar = readInt32()
                    else -> skip(type)
                }
            }
        }
        return createLocation(startLine, startChar)
    }

    open fun readJsImportedModule(): Any {
        var externalName: Int = 0
        var internalName: Int = 0
        var plainReference: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> externalName = readInt32()
                    2 -> internalName = readInt32()
                    3 -> plainReference = readWithLength { readExpression() }
                    else -> skip(type)
                }
            }
        }
        return createJsImportedModule(externalName, internalName, plainReference)
    }

    open fun readExpression(): Any {
        var fileId: Int? = null
        var location: Any? = null
        var synthetic: Boolean = false
        var sideEffects: Any = AFFECTS_STATE
        var localAlias: Any? = null
        var oneOfSimpleNameReference: Int? = null
        var oneOfThisLiteral: Any? = null
        var oneOfNullLiteral: Any? = null
        var oneOfTrueLiteral: Any? = null
        var oneOfFalseLiteral: Any? = null
        var oneOfStringLiteral: Int? = null
        var oneOfRegExpLiteral: Any? = null
        var oneOfIntLiteral: Int? = null
        var oneOfDoubleLiteral: Double? = null
        var oneOfArrayLiteral: Any? = null
        var oneOfObjectLiteral: Any? = null
        var oneOfFunction: Any? = null
        var oneOfDocComment: Any? = null
        var oneOfBinary: Any? = null
        var oneOfUnary: Any? = null
        var oneOfConditional: Any? = null
        var oneOfArrayAccess: Any? = null
        var oneOfNameReference: Any? = null
        var oneOfPropertyReference: Any? = null
        var oneOfInvocation: Any? = null
        var oneOfInstantiation: Any? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> fileId = readInt32()
                    2 -> location = readWithLength { readLocation() }
                    3 -> synthetic = readBool()
                    4 -> sideEffects = createSideEffects(readInt32())
                    5 -> localAlias = readWithLength { readJsImportedModule() }
                    22 -> {
                        oneOfSimpleNameReference = readInt32()
                        oneOfIndex = 22
                    }
                    23 -> {
                        oneOfThisLiteral = readWithLength { readThisLiteral() }
                        oneOfIndex = 23
                    }
                    24 -> {
                        oneOfNullLiteral = readWithLength { readNullLiteral() }
                        oneOfIndex = 24
                    }
                    25 -> {
                        oneOfTrueLiteral = readWithLength { readTrueLiteral() }
                        oneOfIndex = 25
                    }
                    26 -> {
                        oneOfFalseLiteral = readWithLength { readFalseLiteral() }
                        oneOfIndex = 26
                    }
                    27 -> {
                        oneOfStringLiteral = readInt32()
                        oneOfIndex = 27
                    }
                    28 -> {
                        oneOfRegExpLiteral = readWithLength { readRegExpLiteral() }
                        oneOfIndex = 28
                    }
                    29 -> {
                        oneOfIntLiteral = readInt32()
                        oneOfIndex = 29
                    }
                    30 -> {
                        oneOfDoubleLiteral = readDouble()
                        oneOfIndex = 30
                    }
                    31 -> {
                        oneOfArrayLiteral = readWithLength { readArrayLiteral() }
                        oneOfIndex = 31
                    }
                    32 -> {
                        oneOfObjectLiteral = readWithLength { readObjectLiteral() }
                        oneOfIndex = 32
                    }
                    33 -> {
                        oneOfFunction = readWithLength { readFunction() }
                        oneOfIndex = 33
                    }
                    34 -> {
                        oneOfDocComment = readWithLength { readDocComment() }
                        oneOfIndex = 34
                    }
                    35 -> {
                        oneOfBinary = readWithLength { readBinaryOperation() }
                        oneOfIndex = 35
                    }
                    36 -> {
                        oneOfUnary = readWithLength { readUnaryOperation() }
                        oneOfIndex = 36
                    }
                    37 -> {
                        oneOfConditional = readWithLength { readConditional() }
                        oneOfIndex = 37
                    }
                    38 -> {
                        oneOfArrayAccess = readWithLength { readArrayAccess() }
                        oneOfIndex = 38
                    }
                    39 -> {
                        oneOfNameReference = readWithLength { readNameReference() }
                        oneOfIndex = 39
                    }
                    40 -> {
                        oneOfPropertyReference = readWithLength { readPropertyReference() }
                        oneOfIndex = 40
                    }
                    41 -> {
                        oneOfInvocation = readWithLength { readInvocation() }
                        oneOfIndex = 41
                    }
                    42 -> {
                        oneOfInstantiation = readWithLength { readInstantiation() }
                        oneOfIndex = 42
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            22 -> return createExpression_simpleNameReference(fileId, location, synthetic, sideEffects, localAlias, oneOfSimpleNameReference!!)
            23 -> return createExpression_thisLiteral(fileId, location, synthetic, sideEffects, localAlias, oneOfThisLiteral!!)
            24 -> return createExpression_nullLiteral(fileId, location, synthetic, sideEffects, localAlias, oneOfNullLiteral!!)
            25 -> return createExpression_trueLiteral(fileId, location, synthetic, sideEffects, localAlias, oneOfTrueLiteral!!)
            26 -> return createExpression_falseLiteral(fileId, location, synthetic, sideEffects, localAlias, oneOfFalseLiteral!!)
            27 -> return createExpression_stringLiteral(fileId, location, synthetic, sideEffects, localAlias, oneOfStringLiteral!!)
            28 -> return createExpression_regExpLiteral(fileId, location, synthetic, sideEffects, localAlias, oneOfRegExpLiteral!!)
            29 -> return createExpression_intLiteral(fileId, location, synthetic, sideEffects, localAlias, oneOfIntLiteral!!)
            30 -> return createExpression_doubleLiteral(fileId, location, synthetic, sideEffects, localAlias, oneOfDoubleLiteral!!)
            31 -> return createExpression_arrayLiteral(fileId, location, synthetic, sideEffects, localAlias, oneOfArrayLiteral!!)
            32 -> return createExpression_objectLiteral(fileId, location, synthetic, sideEffects, localAlias, oneOfObjectLiteral!!)
            33 -> return createExpression_function(fileId, location, synthetic, sideEffects, localAlias, oneOfFunction!!)
            34 -> return createExpression_docComment(fileId, location, synthetic, sideEffects, localAlias, oneOfDocComment!!)
            35 -> return createExpression_binary(fileId, location, synthetic, sideEffects, localAlias, oneOfBinary!!)
            36 -> return createExpression_unary(fileId, location, synthetic, sideEffects, localAlias, oneOfUnary!!)
            37 -> return createExpression_conditional(fileId, location, synthetic, sideEffects, localAlias, oneOfConditional!!)
            38 -> return createExpression_arrayAccess(fileId, location, synthetic, sideEffects, localAlias, oneOfArrayAccess!!)
            39 -> return createExpression_nameReference(fileId, location, synthetic, sideEffects, localAlias, oneOfNameReference!!)
            40 -> return createExpression_propertyReference(fileId, location, synthetic, sideEffects, localAlias, oneOfPropertyReference!!)
            41 -> return createExpression_invocation(fileId, location, synthetic, sideEffects, localAlias, oneOfInvocation!!)
            42 -> return createExpression_instantiation(fileId, location, synthetic, sideEffects, localAlias, oneOfInstantiation!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readThisLiteral(): Any {
        return createThisLiteral()
    }
    open fun readNullLiteral(): Any {
        return createNullLiteral()
    }
    open fun readTrueLiteral(): Any {
        return createTrueLiteral()
    }
    open fun readFalseLiteral(): Any {
        return createFalseLiteral()
    }
    open fun readRegExpLiteral(): Any {
        var patternStringId: Int = 0
        var flagsStringId: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> patternStringId = readInt32()
                    2 -> flagsStringId = readInt32()
                    else -> skip(type)
                }
            }
        }
        return createRegExpLiteral(patternStringId, flagsStringId)
    }

    open fun readArrayLiteral(): Any {
        var element: MutableList<Any> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> element.add(readWithLength { readExpression() })
                    else -> skip(type)
                }
            }
        }
        return createArrayLiteral(element)
    }

    open fun readObjectLiteral(): Any {
        var entry: MutableList<Any> = mutableListOf()
        var multiline: Boolean = true
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> entry.add(readWithLength { readObjectLiteralEntry() })
                    2 -> multiline = readBool()
                    else -> skip(type)
                }
            }
        }
        return createObjectLiteral(entry, multiline)
    }

    open fun readObjectLiteralEntry(): Any {
        var key: Any? = null
        var value: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> key = readWithLength { readExpression() }
                    2 -> value = readWithLength { readExpression() }
                    else -> skip(type)
                }
            }
        }
        return createObjectLiteralEntry(key!!, value!!)
    }

    open fun readFunction(): Any {
        var parameter: MutableList<Any> = mutableListOf()
        var nameId: Int? = null
        var body: Any? = null
        var local: Boolean = false
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> parameter.add(readWithLength { readParameter() })
                    2 -> nameId = readInt32()
                    3 -> body = readWithLength { readStatement() }
                    4 -> local = readBool()
                    else -> skip(type)
                }
            }
        }
        return createFunction(parameter, nameId, body!!, local)
    }

    open fun readParameter(): Any {
        var nameId: Int = 0
        var hasDefaultValue: Boolean = false
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> nameId = readInt32()
                    2 -> hasDefaultValue = readBool()
                    else -> skip(type)
                }
            }
        }
        return createParameter(nameId, hasDefaultValue)
    }

    open fun readDocComment(): Any {
        var tag: MutableList<Any> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> tag.add(readWithLength { readDocCommentTag() })
                    else -> skip(type)
                }
            }
        }
        return createDocComment(tag)
    }

    open fun readDocCommentTag(): Any {
        var nameId: Int = 0
        var oneOfValueStringId: Int? = null
        var oneOfExpression: Any? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> nameId = readInt32()
                    2 -> {
                        oneOfValueStringId = readInt32()
                        oneOfIndex = 2
                    }
                    3 -> {
                        oneOfExpression = readWithLength { readExpression() }
                        oneOfIndex = 3
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            2 -> return createDocCommentTag_valueStringId(nameId, oneOfValueStringId!!)
            3 -> return createDocCommentTag_expression(nameId, oneOfExpression!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readBinaryOperation(): Any {
        var left: Any? = null
        var right: Any? = null
        var type_: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> left = readWithLength { readExpression() }
                    2 -> right = readWithLength { readExpression() }
                    3 -> type_ = createBinaryOperationType(readInt32())
                    else -> skip(type)
                }
            }
        }
        return createBinaryOperation(left!!, right!!, type_!!)
    }

    open fun readUnaryOperation(): Any {
        var operand: Any? = null
        var type_: Any? = null
        var postfix: Boolean = false
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> operand = readWithLength { readExpression() }
                    2 -> type_ = createUnaryOperationType(readInt32())
                    3 -> postfix = readBool()
                    else -> skip(type)
                }
            }
        }
        return createUnaryOperation(operand!!, type_!!, postfix)
    }

    open fun readConditional(): Any {
        var testExpression: Any? = null
        var thenExpression: Any? = null
        var elseExpression: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> testExpression = readWithLength { readExpression() }
                    2 -> thenExpression = readWithLength { readExpression() }
                    3 -> elseExpression = readWithLength { readExpression() }
                    else -> skip(type)
                }
            }
        }
        return createConditional(testExpression!!, thenExpression!!, elseExpression!!)
    }

    open fun readArrayAccess(): Any {
        var array: Any? = null
        var index: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> array = readWithLength { readExpression() }
                    2 -> index = readWithLength { readExpression() }
                    else -> skip(type)
                }
            }
        }
        return createArrayAccess(array!!, index!!)
    }

    open fun readNameReference(): Any {
        var nameId: Int = 0
        var qualifier: Any? = null
        var inlineStrategy: Any = NOT_INLINE
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> nameId = readInt32()
                    2 -> qualifier = readWithLength { readExpression() }
                    3 -> inlineStrategy = createInlineStrategy(readInt32())
                    else -> skip(type)
                }
            }
        }
        return createNameReference(nameId, qualifier, inlineStrategy)
    }

    open fun readPropertyReference(): Any {
        var stringId: Int = 0
        var qualifier: Any? = null
        var inlineStrategy: Any = NOT_INLINE
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> stringId = readInt32()
                    2 -> qualifier = readWithLength { readExpression() }
                    3 -> inlineStrategy = createInlineStrategy(readInt32())
                    else -> skip(type)
                }
            }
        }
        return createPropertyReference(stringId, qualifier, inlineStrategy)
    }

    open fun readInvocation(): Any {
        var qualifier: Any? = null
        var argument: MutableList<Any> = mutableListOf()
        var inlineStrategy: Any = NOT_INLINE
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> qualifier = readWithLength { readExpression() }
                    2 -> argument.add(readWithLength { readExpression() })
                    3 -> inlineStrategy = createInlineStrategy(readInt32())
                    else -> skip(type)
                }
            }
        }
        return createInvocation(qualifier!!, argument, inlineStrategy)
    }

    open fun readInstantiation(): Any {
        var qualifier: Any? = null
        var argument: MutableList<Any> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> qualifier = readWithLength { readExpression() }
                    2 -> argument.add(readWithLength { readExpression() })
                    else -> skip(type)
                }
            }
        }
        return createInstantiation(qualifier!!, argument)
    }

    open fun readStatement(): Any {
        var fileId: Int? = null
        var location: Any? = null
        var synthetic: Boolean = false
        var oneOfReturnStatement: Any? = null
        var oneOfThrowStatement: Any? = null
        var oneOfBreakStatement: Any? = null
        var oneOfContinueStatement: Any? = null
        var oneOfDebugger: Any? = null
        var oneOfExpression: Any? = null
        var oneOfVars: Any? = null
        var oneOfBlock: Any? = null
        var oneOfGlobalBlock: Any? = null
        var oneOfLabel: Any? = null
        var oneOfIfStatement: Any? = null
        var oneOfSwitchStatement: Any? = null
        var oneOfWhileStatement: Any? = null
        var oneOfDoWhileStatement: Any? = null
        var oneOfForStatement: Any? = null
        var oneOfForInStatement: Any? = null
        var oneOfTryStatement: Any? = null
        var oneOfEmpty: Any? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> fileId = readInt32()
                    2 -> location = readWithLength { readLocation() }
                    3 -> synthetic = readBool()
                    21 -> {
                        oneOfReturnStatement = readWithLength { readReturn() }
                        oneOfIndex = 21
                    }
                    22 -> {
                        oneOfThrowStatement = readWithLength { readThrow() }
                        oneOfIndex = 22
                    }
                    23 -> {
                        oneOfBreakStatement = readWithLength { readBreak() }
                        oneOfIndex = 23
                    }
                    24 -> {
                        oneOfContinueStatement = readWithLength { readContinue() }
                        oneOfIndex = 24
                    }
                    25 -> {
                        oneOfDebugger = readWithLength { readDebugger() }
                        oneOfIndex = 25
                    }
                    26 -> {
                        oneOfExpression = readWithLength { readExpressionStatement() }
                        oneOfIndex = 26
                    }
                    27 -> {
                        oneOfVars = readWithLength { readVars() }
                        oneOfIndex = 27
                    }
                    28 -> {
                        oneOfBlock = readWithLength { readBlock() }
                        oneOfIndex = 28
                    }
                    29 -> {
                        oneOfGlobalBlock = readWithLength { readGlobalBlock() }
                        oneOfIndex = 29
                    }
                    30 -> {
                        oneOfLabel = readWithLength { readLabel() }
                        oneOfIndex = 30
                    }
                    31 -> {
                        oneOfIfStatement = readWithLength { readIf() }
                        oneOfIndex = 31
                    }
                    32 -> {
                        oneOfSwitchStatement = readWithLength { readSwitch() }
                        oneOfIndex = 32
                    }
                    33 -> {
                        oneOfWhileStatement = readWithLength { readWhile() }
                        oneOfIndex = 33
                    }
                    34 -> {
                        oneOfDoWhileStatement = readWithLength { readDoWhile() }
                        oneOfIndex = 34
                    }
                    35 -> {
                        oneOfForStatement = readWithLength { readFor() }
                        oneOfIndex = 35
                    }
                    36 -> {
                        oneOfForInStatement = readWithLength { readForIn() }
                        oneOfIndex = 36
                    }
                    37 -> {
                        oneOfTryStatement = readWithLength { readTry() }
                        oneOfIndex = 37
                    }
                    38 -> {
                        oneOfEmpty = readWithLength { readEmpty() }
                        oneOfIndex = 38
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            21 -> return createStatement_returnStatement(fileId, location, synthetic, oneOfReturnStatement!!)
            22 -> return createStatement_throwStatement(fileId, location, synthetic, oneOfThrowStatement!!)
            23 -> return createStatement_breakStatement(fileId, location, synthetic, oneOfBreakStatement!!)
            24 -> return createStatement_continueStatement(fileId, location, synthetic, oneOfContinueStatement!!)
            25 -> return createStatement_debugger(fileId, location, synthetic, oneOfDebugger!!)
            26 -> return createStatement_expression(fileId, location, synthetic, oneOfExpression!!)
            27 -> return createStatement_vars(fileId, location, synthetic, oneOfVars!!)
            28 -> return createStatement_block(fileId, location, synthetic, oneOfBlock!!)
            29 -> return createStatement_globalBlock(fileId, location, synthetic, oneOfGlobalBlock!!)
            30 -> return createStatement_label(fileId, location, synthetic, oneOfLabel!!)
            31 -> return createStatement_ifStatement(fileId, location, synthetic, oneOfIfStatement!!)
            32 -> return createStatement_switchStatement(fileId, location, synthetic, oneOfSwitchStatement!!)
            33 -> return createStatement_whileStatement(fileId, location, synthetic, oneOfWhileStatement!!)
            34 -> return createStatement_doWhileStatement(fileId, location, synthetic, oneOfDoWhileStatement!!)
            35 -> return createStatement_forStatement(fileId, location, synthetic, oneOfForStatement!!)
            36 -> return createStatement_forInStatement(fileId, location, synthetic, oneOfForInStatement!!)
            37 -> return createStatement_tryStatement(fileId, location, synthetic, oneOfTryStatement!!)
            38 -> return createStatement_empty(fileId, location, synthetic, oneOfEmpty!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readReturn(): Any {
        var value: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> value = readWithLength { readExpression() }
                    else -> skip(type)
                }
            }
        }
        return createReturn(value)
    }

    open fun readThrow(): Any {
        var exception: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> exception = readWithLength { readExpression() }
                    else -> skip(type)
                }
            }
        }
        return createThrow(exception!!)
    }

    open fun readBreak(): Any {
        var labelId: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> labelId = readInt32()
                    else -> skip(type)
                }
            }
        }
        return createBreak(labelId)
    }

    open fun readContinue(): Any {
        var labelId: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> labelId = readInt32()
                    else -> skip(type)
                }
            }
        }
        return createContinue(labelId)
    }

    open fun readDebugger(): Any {
        return createDebugger()
    }
    open fun readExpressionStatement(): Any {
        var expression: Any? = null
        var exportedTagId: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> expression = readWithLength { readExpression() }
                    2 -> exportedTagId = readInt32()
                    else -> skip(type)
                }
            }
        }
        return createExpressionStatement(expression!!, exportedTagId)
    }

    open fun readVars(): Any {
        var declaration: MutableList<Any> = mutableListOf()
        var multiline: Boolean = false
        var exportedPackageId: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> declaration.add(readWithLength { readVarDeclaration() })
                    2 -> multiline = readBool()
                    3 -> exportedPackageId = readInt32()
                    else -> skip(type)
                }
            }
        }
        return createVars(declaration, multiline, exportedPackageId)
    }

    open fun readVarDeclaration(): Any {
        var nameId: Int = 0
        var initialValue: Any? = null
        var fileId: Int? = null
        var location: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> nameId = readInt32()
                    2 -> initialValue = readWithLength { readExpression() }
                    3 -> fileId = readInt32()
                    4 -> location = readWithLength { readLocation() }
                    else -> skip(type)
                }
            }
        }
        return createVarDeclaration(nameId, initialValue, fileId, location)
    }

    open fun readBlock(): Any {
        var statement: MutableList<Any> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> statement.add(readWithLength { readStatement() })
                    else -> skip(type)
                }
            }
        }
        return createBlock(statement)
    }

    open fun readGlobalBlock(): Any {
        var statement: MutableList<Any> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> statement.add(readWithLength { readStatement() })
                    else -> skip(type)
                }
            }
        }
        return createGlobalBlock(statement)
    }

    open fun readLabel(): Any {
        var nameId: Int = 0
        var innerStatement: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> nameId = readInt32()
                    2 -> innerStatement = readWithLength { readStatement() }
                    else -> skip(type)
                }
            }
        }
        return createLabel(nameId, innerStatement!!)
    }

    open fun readIf(): Any {
        var condition: Any? = null
        var thenStatement: Any? = null
        var elseStatement: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> condition = readWithLength { readExpression() }
                    2 -> thenStatement = readWithLength { readStatement() }
                    3 -> elseStatement = readWithLength { readStatement() }
                    else -> skip(type)
                }
            }
        }
        return createIf(condition!!, thenStatement!!, elseStatement)
    }

    open fun readSwitch(): Any {
        var expression: Any? = null
        var entry: MutableList<Any> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> expression = readWithLength { readExpression() }
                    2 -> entry.add(readWithLength { readSwitchEntry() })
                    else -> skip(type)
                }
            }
        }
        return createSwitch(expression!!, entry)
    }

    open fun readSwitchEntry(): Any {
        var label: Any? = null
        var statement: MutableList<Any> = mutableListOf()
        var fileId: Int? = null
        var location: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> label = readWithLength { readExpression() }
                    2 -> statement.add(readWithLength { readStatement() })
                    3 -> fileId = readInt32()
                    4 -> location = readWithLength { readLocation() }
                    else -> skip(type)
                }
            }
        }
        return createSwitchEntry(label, statement, fileId, location)
    }

    open fun readWhile(): Any {
        var condition: Any? = null
        var body: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> condition = readWithLength { readExpression() }
                    2 -> body = readWithLength { readStatement() }
                    else -> skip(type)
                }
            }
        }
        return createWhile(condition!!, body!!)
    }

    open fun readDoWhile(): Any {
        var condition: Any? = null
        var body: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> condition = readWithLength { readExpression() }
                    2 -> body = readWithLength { readStatement() }
                    else -> skip(type)
                }
            }
        }
        return createDoWhile(condition!!, body!!)
    }

    open fun readFor(): Any {
        var oneOfVariables: Any? = null
        var oneOfExpression: Any? = null
        var oneOfEmpty: Any? = null
        var condition: Any? = null
        var increment: Any? = null
        var body: Any? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        oneOfVariables = readWithLength { readStatement() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        oneOfExpression = readWithLength { readExpression() }
                        oneOfIndex = 2
                    }
                    3 -> {
                        oneOfEmpty = readWithLength { readEmptyInit() }
                        oneOfIndex = 3
                    }
                    4 -> condition = readWithLength { readExpression() }
                    5 -> increment = readWithLength { readExpression() }
                    6 -> body = readWithLength { readStatement() }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createFor_variables(oneOfVariables!!, condition, increment, body!!)
            2 -> return createFor_expression(oneOfExpression!!, condition, increment, body!!)
            3 -> return createFor_empty(oneOfEmpty!!, condition, increment, body!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readEmptyInit(): Any {
        return createEmptyInit()
    }
    open fun readForIn(): Any {
        var oneOfNameId: Int? = null
        var oneOfExpression: Any? = null
        var iterable: Any? = null
        var body: Any? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        oneOfNameId = readInt32()
                        oneOfIndex = 1
                    }
                    2 -> {
                        oneOfExpression = readWithLength { readExpression() }
                        oneOfIndex = 2
                    }
                    3 -> iterable = readWithLength { readExpression() }
                    4 -> body = readWithLength { readStatement() }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createForIn_nameId(oneOfNameId!!, iterable!!, body!!)
            2 -> return createForIn_expression(oneOfExpression!!, iterable!!, body!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readTry(): Any {
        var tryBlock: Any? = null
        var catchBlock: Any? = null
        var finallyBlock: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> tryBlock = readWithLength { readStatement() }
                    2 -> catchBlock = readWithLength { readCatch() }
                    3 -> finallyBlock = readWithLength { readStatement() }
                    else -> skip(type)
                }
            }
        }
        return createTry(tryBlock!!, catchBlock, finallyBlock)
    }

    open fun readCatch(): Any {
        var parameter: Any? = null
        var body: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> parameter = readWithLength { readParameter() }
                    2 -> body = readWithLength { readStatement() }
                    else -> skip(type)
                }
            }
        }
        return createCatch(parameter!!, body!!)
    }

    open fun readEmpty(): Any {
        return createEmpty()
    }
    open fun readFragment(): Any {
        var importedModule: MutableList<Any> = mutableListOf()
        var importEntry: MutableList<Any> = mutableListOf()
        var declarationBlock: Any? = null
        var exportBlock: Any? = null
        var initializerBlock: Any? = null
        var nameBinding: MutableList<Any> = mutableListOf()
        var classModel: MutableList<Any> = mutableListOf()
        var moduleExpression: MutableList<Any> = mutableListOf()
        var inlineModule: MutableList<Any> = mutableListOf()
        var packageFqn: String? = null
        var testsInvocation: Any? = null
        var mainInvocation: Any? = null
        var inlinedLocalDeclarations: MutableList<Any> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> importedModule.add(readWithLength { readImportedModule() })
                    2 -> importEntry.add(readWithLength { readImport() })
                    3 -> declarationBlock = readWithLength { readGlobalBlock() }
                    4 -> exportBlock = readWithLength { readGlobalBlock() }
                    5 -> initializerBlock = readWithLength { readGlobalBlock() }
                    6 -> nameBinding.add(readWithLength { readNameBinding() })
                    7 -> classModel.add(readWithLength { readClassModel() })
                    8 -> moduleExpression.add(readWithLength { readExpression() })
                    9 -> inlineModule.add(readWithLength { readInlineModule() })
                    10 -> packageFqn = readString()
                    11 -> testsInvocation = readWithLength { readStatement() }
                    12 -> mainInvocation = readWithLength { readStatement() }
                    13 -> inlinedLocalDeclarations.add(readWithLength { readInlinedLocalDeclarations() })
                    else -> skip(type)
                }
            }
        }
        return createFragment(importedModule, importEntry, declarationBlock, exportBlock, initializerBlock, nameBinding, classModel, moduleExpression, inlineModule, packageFqn, testsInvocation, mainInvocation, inlinedLocalDeclarations)
    }

    open fun readInlinedLocalDeclarations(): Any {
        var tag: Int = 0
        var block: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> tag = readInt32()
                    2 -> block = readWithLength { readGlobalBlock() }
                    else -> skip(type)
                }
            }
        }
        return createInlinedLocalDeclarations(tag, block!!)
    }

    open fun readImportedModule(): Any {
        var externalNameId: Int = 0
        var internalNameId: Int = 0
        var plainReference: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> externalNameId = readInt32()
                    2 -> internalNameId = readInt32()
                    3 -> plainReference = readWithLength { readExpression() }
                    else -> skip(type)
                }
            }
        }
        return createImportedModule(externalNameId, internalNameId, plainReference)
    }

    open fun readImport(): Any {
        var signatureId: Int = 0
        var expression: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> signatureId = readInt32()
                    2 -> expression = readWithLength { readExpression() }
                    else -> skip(type)
                }
            }
        }
        return createImport(signatureId, expression!!)
    }

    open fun readNameBinding(): Any {
        var signatureId: Int = 0
        var nameId: Int = 0
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> signatureId = readInt32()
                    2 -> nameId = readInt32()
                    else -> skip(type)
                }
            }
        }
        return createNameBinding(signatureId, nameId)
    }

    open fun readClassModel(): Any {
        var nameId: Int = 0
        var superNameId: Int? = null
        var interfaceNameId: MutableList<Int> = mutableListOf()
        var postDeclarationBlock: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> nameId = readInt32()
                    2 -> superNameId = readInt32()
                    4 -> interfaceNameId.add(readInt32())
                    3 -> postDeclarationBlock = readWithLength { readGlobalBlock() }
                    else -> skip(type)
                }
            }
        }
        return createClassModel(nameId, superNameId, interfaceNameId, postDeclarationBlock)
    }

    open fun readInlineModule(): Any {
        var signatureId: Int = 0
        var expressionId: Int = 0
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> signatureId = readInt32()
                    2 -> expressionId = readInt32()
                    else -> skip(type)
                }
            }
        }
        return createInlineModule(signatureId, expressionId)
    }

    open fun readStringTable(): Any {
        var entry: MutableList<String> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> entry.add(readString())
                    else -> skip(type)
                }
            }
        }
        return createStringTable(entry)
    }

    open fun readNameTable(): Any {
        var entry: MutableList<Any> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> entry.add(readWithLength { readName() })
                    else -> skip(type)
                }
            }
        }
        return createNameTable(entry)
    }

    open fun readName(): Any {
        var temporary: Boolean = false
        var identifier: Int? = null
        var localNameId: Any? = null
        var imported: Boolean = false
        var specialFunction: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> temporary = readBool()
                    2 -> identifier = readInt32()
                    3 -> localNameId = readWithLength { readLocalAlias() }
                    4 -> imported = readBool()
                    5 -> specialFunction = createSpecialFunction(readInt32())
                    else -> skip(type)
                }
            }
        }
        return createName(temporary, identifier, localNameId, imported, specialFunction)
    }

    open fun readLocalAlias(): Any {
        var localNameId: Int = 0
        var tag: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> localNameId = readInt32()
                    2 -> tag = readInt32()
                    else -> skip(type)
                }
            }
        }
        return createLocalAlias(localNameId, tag)
    }

    open fun readChunk(): Any {
        var stringTable: Any? = null
        var nameTable: Any? = null
        var fragment: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> stringTable = readWithLength { readStringTable() }
                    2 -> nameTable = readWithLength { readNameTable() }
                    3 -> fragment = readWithLength { readFragment() }
                    else -> skip(type)
                }
            }
        }
        return createChunk(stringTable!!, nameTable!!, fragment!!)
    }

    open fun readInlineData(): Any {
        var inlineFunctionTags: MutableList<String> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> inlineFunctionTags.add(readString())
                    else -> skip(type)
                }
            }
        }
        return createInlineData(inlineFunctionTags)
    }
}
