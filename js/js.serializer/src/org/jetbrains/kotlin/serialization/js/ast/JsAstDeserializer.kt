/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.serialization.js.ast

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf.*
import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf.Expression.ExpressionCase
import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf.Statement.StatementCase
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import org.jetbrains.kotlin.resolve.inline.InlineStrategy as KotlinInlineStrategy

class JsAstDeserializer(program: JsProgram, private val sourceRoots: Iterable<File>) {
    private val scope = JsRootScope(program)
    private val stringTable = mutableListOf<String>()
    private val nameTable = mutableListOf<Name>()
    private val nameCache = mutableListOf<JsName?>()
    private val fileStack: Deque<String> = ArrayDeque()

    fun deserialize(input: InputStream): JsProgramFragment {
        return deserialize(Chunk.parseFrom(CodedInputStream.newInstance(input).apply { setRecursionLimit(4096) }))
    }

    fun deserialize(proto: Chunk): JsProgramFragment {
        stringTable += proto.stringTable.entryList
        nameTable += proto.nameTable.entryList
        nameCache += nameTable.map { null }
        try {
            return deserialize(proto.fragment)
        }
        finally {
            stringTable.clear()
            nameTable.clear()
            nameCache.clear()
        }
    }

    private fun deserialize(proto: Fragment): JsProgramFragment {
        val fragment = JsProgramFragment(scope, proto.packageFqn)

        fragment.importedModules += proto.importedModuleList.map { importedModuleProto ->
             JsImportedModule(
                    deserializeString(importedModuleProto.externalNameId),
                    deserializeName(importedModuleProto.internalNameId),
                    if (importedModuleProto.hasPlainReference()) deserialize(importedModuleProto.plainReference) else null
            )
        }

        fragment.imports += proto.importEntryList.associate { importProto ->
            deserializeString(importProto.signatureId) to deserialize(importProto.expression)
        }

        if (proto.hasDeclarationBlock()) {
            fragment.declarationBlock.statements += deserializeGlobalBlock(proto.declarationBlock).statements
        }
        if (proto.hasInitializerBlock()) {
            fragment.initializerBlock.statements += deserializeGlobalBlock(proto.initializerBlock).statements
        }
        if (proto.hasExportBlock()) {
            fragment.exportBlock.statements += deserializeGlobalBlock(proto.exportBlock).statements
        }

        fragment.nameBindings += proto.nameBindingList.map { nameBindingProto ->
            JsNameBinding(deserializeString(nameBindingProto.signatureId), deserializeName(nameBindingProto.nameId))
        }

        fragment.classes += proto.classModelList.associate { clsProto -> deserialize(clsProto).let { it.name to it } }

        val moduleExpressions = proto.moduleExpressionList.map { deserialize(it) }
        fragment.inlineModuleMap += proto.inlineModuleList.associate { inlineModuleProto ->
            deserializeString(inlineModuleProto.signatureId) to moduleExpressions[inlineModuleProto.expressionId]
        }

        for (nameBinding in fragment.nameBindings) {
            if (nameBinding.key in fragment.imports) {
                nameBinding.name.imported = true
            }
        }

        if (proto.hasTestsInvocation()) {
            fragment.tests = deserialize(proto.testsInvocation)
        }

        if (proto.hasMainInvocation()) {
            fragment.mainFunction = deserialize(proto.mainInvocation)
        }

        return fragment
    }

    private fun deserialize(proto: ClassModel): JsClassModel {
        val superName = if (proto.hasSuperNameId()) deserializeName(proto.superNameId) else null
        return JsClassModel(deserializeName(proto.nameId), superName).apply {
            proto.interfaceNameIdList.mapTo(interfaces) { deserializeName(it) }
            if (proto.hasPostDeclarationBlock()) {
                postDeclarationBlock.statements += deserializeGlobalBlock(proto.postDeclarationBlock).statements
            }
        }
    }

    private fun deserialize(proto: Statement): JsStatement {
        val statement = withLocation(
                fileId = if (proto.hasFileId()) proto.fileId else null,
                location = if (proto.hasLocation()) proto.location else null,
                action = { deserializeNoMetadata(proto) }
        )
        if (statement is HasMetadata) {
            statement.synthetic = proto.synthetic
        }
        return statement
    }

    private fun deserializeNoMetadata(proto: Statement): JsStatement = when (proto.statementCase) {
        StatementCase.RETURN_STATEMENT -> {
            val returnProto = proto.returnStatement
            JsReturn(if (returnProto.hasValue()) deserialize(returnProto.value) else null)
        }

        StatementCase.THROW_STATEMENT -> {
            val throwProto = proto.throwStatement
            JsThrow(deserialize(throwProto.exception))
        }

        StatementCase.BREAK_STATEMENT -> {
            val breakProto = proto.breakStatement
            JsBreak(if (breakProto.hasLabelId()) JsNameRef(deserializeName(breakProto.labelId)) else null)
        }

        StatementCase.CONTINUE_STATEMENT -> {
            val continueProto = proto.continueStatement
            JsContinue(if (continueProto.hasLabelId()) JsNameRef(deserializeName(continueProto.labelId)) else null)
        }

        StatementCase.DEBUGGER -> {
            JsDebugger()
        }

        StatementCase.EXPRESSION -> {
            val expressionProto = proto.expression
            JsExpressionStatement(deserialize(expressionProto.expression)).also {
                if (expressionProto.hasExportedTagId()) {
                    it.exportedTag = deserializeString(expressionProto.exportedTagId)
                }
            }
        }

        StatementCase.VARS -> {
            deserializeVars(proto.vars)
        }

        StatementCase.BLOCK -> {
            val blockProto = proto.block
            val block = JsBlock()
            block.statements += blockProto.statementList.map { deserialize(it) }
            block
        }

        StatementCase.GLOBAL_BLOCK -> {
            deserializeGlobalBlock(proto.globalBlock)
        }

        StatementCase.LABEL -> {
            val labelProto = proto.label
            JsLabel(deserializeName(labelProto.nameId), deserialize(labelProto.innerStatement))
        }

        StatementCase.IF_STATEMENT -> {
            val ifProto = proto.ifStatement
            JsIf(
                    deserialize(ifProto.condition),
                    deserialize(ifProto.thenStatement),
                    if (ifProto.hasElseStatement()) deserialize(ifProto.elseStatement) else null
            )
        }

        StatementCase.SWITCH_STATEMENT -> {
            val switchProto = proto.switchStatement
            JsSwitch(
                    deserialize(switchProto.expression),
                    switchProto.entryList.map { entryProto ->
                        val member = withLocation(
                                fileId = if (entryProto.hasFileId()) entryProto.fileId else null,
                                location = if (entryProto.hasLocation()) entryProto.location else null
                        ) {
                            if (entryProto.hasLabel()) {
                                JsCase().apply { caseExpression = deserialize(entryProto.label) }
                            }
                            else {
                                JsDefault()
                            }
                        }
                        member.statements += entryProto.statementList.map { deserialize(it) }
                        member
                    }
            )
        }

        StatementCase.WHILE_STATEMENT -> {
            val whileProto = proto.whileStatement
            JsWhile(deserialize(whileProto.condition), deserialize(whileProto.body))
        }

        StatementCase.DO_WHILE_STATEMENT -> {
            val doWhileProto = proto.doWhileStatement
            JsDoWhile(deserialize(doWhileProto.condition), deserialize(doWhileProto.body))
        }

        StatementCase.FOR_STATEMENT -> {
            val forProto = proto.forStatement
            val initVars = if (forProto.hasVariables()) deserialize(forProto.variables) as JsVars else null
            val initExpr = if (forProto.hasExpression()) deserialize(forProto.expression) else null
            val condition = if (forProto.hasCondition()) deserialize(forProto.condition) else null
            val increment = if (forProto.hasIncrement()) deserialize(forProto.increment) else null
            val body = deserialize(forProto.body)
            if (initVars != null) {
                JsFor(initVars, condition, increment, body)
            }
            else {
                JsFor(initExpr, condition, increment, body)
            }
        }

        StatementCase.FOR_IN_STATEMENT -> {
            val forInProto = proto.forInStatement
            val iterName = if (forInProto.hasNameId()) deserializeName(forInProto.nameId) else null
            val iterExpr = if (forInProto.hasExpression()) deserialize(forInProto.expression) else null
            val iterable = deserialize(forInProto.iterable)
            val body = deserialize(forInProto.body)
            JsForIn(iterName, iterExpr, iterable, body)
        }

        StatementCase.TRY_STATEMENT -> {
            val tryProto = proto.tryStatement
            val tryBlock = deserialize(tryProto.tryBlock) as JsBlock
            val catchBlock = if (tryProto.hasCatchBlock()) {
                val catchProto = tryProto.catchBlock
                JsCatch(deserializeName(catchProto.parameter.nameId)).apply {
                    body = deserialize(catchProto.body) as JsBlock
                }
            }
            else {
                null
            }
            val finallyBlock = if (tryProto.hasFinallyBlock()) deserialize(tryProto.finallyBlock) as JsBlock else null
            JsTry(tryBlock, catchBlock, finallyBlock)
        }

        StatementCase.EMPTY -> JsEmpty

        StatementCase.STATEMENT_NOT_SET,
        null -> error("Statement not set")
    }

    private fun deserialize(proto: Expression): JsExpression {
        val expression = withLocation(
                fileId = if (proto.hasFileId()) proto.fileId else null,
                location = if (proto.hasLocation()) proto.location else null,
                action = { deserializeNoMetadata(proto) }
        )
        expression.synthetic = proto.synthetic
        expression.sideEffects = map(proto.sideEffects)
        if (proto.hasLocalAlias()) {
            expression.localAlias = deserializeName(proto.localAlias)
        }
        return expression
    }

    private fun deserializeNoMetadata(proto: Expression): JsExpression = when (proto.expressionCase) {
        ExpressionCase.THIS_LITERAL -> JsThisRef()
        ExpressionCase.NULL_LITERAL -> JsNullLiteral()
        ExpressionCase.TRUE_LITERAL -> JsBooleanLiteral(true)
        ExpressionCase.FALSE_LITERAL -> JsBooleanLiteral(false)
        ExpressionCase.STRING_LITERAL -> JsStringLiteral(deserializeString(proto.stringLiteral))
        ExpressionCase.INT_LITERAL -> JsIntLiteral(proto.intLiteral)
        ExpressionCase.DOUBLE_LITERAL -> JsDoubleLiteral(proto.doubleLiteral)
        ExpressionCase.SIMPLE_NAME_REFERENCE -> JsNameRef(deserializeName(proto.simpleNameReference))

        ExpressionCase.REG_EXP_LITERAL -> {
            val regExpProto = proto.regExpLiteral
            JsRegExp().apply {
                pattern = deserializeString(regExpProto.patternStringId)
                if (regExpProto.hasFlagsStringId()) {
                    flags = deserializeString(regExpProto.flagsStringId)
                }
            }
        }

        ExpressionCase.ARRAY_LITERAL -> {
            val arrayProto = proto.arrayLiteral
            JsArrayLiteral(arrayProto.elementList.map { deserialize(it) })
        }

        ExpressionCase.OBJECT_LITERAL -> {
            val objectProto = proto.objectLiteral
            JsObjectLiteral(
                    objectProto.entryList.map { entryProto ->
                        JsPropertyInitializer(deserialize(entryProto.key), deserialize(entryProto.value))
                    },
                    objectProto.multiline
            )
        }

        ExpressionCase.FUNCTION -> {
            val functionProto = proto.function
            JsFunction(scope, deserialize(functionProto.body) as JsBlock, "").apply {
                parameters += functionProto.parameterList.map { deserializeParameter(it) }
                if (functionProto.hasNameId()) {
                    name = deserializeName(functionProto.nameId)
                }
                isLocal = functionProto.local
            }
        }

        ExpressionCase.DOC_COMMENT -> {
            val docCommentProto = proto.docComment
            JsDocComment(docCommentProto.tagList.associate { tagProto ->
                val name = deserializeString(tagProto.nameId)
                val value: Any = if (tagProto.hasExpression()) {
                    deserialize(tagProto.expression)
                }
                else {
                    deserializeString(tagProto.valueStringId)
                }
                name to value
            })
        }

        ExpressionCase.BINARY -> {
            val binaryProto = proto.binary
            JsBinaryOperation(map(binaryProto.type), deserialize(binaryProto.left), deserialize(binaryProto.right))
        }

        ExpressionCase.UNARY -> {
            val unaryProto = proto.unary
            val type = map(unaryProto.type)
            val operand = deserialize(unaryProto.operand)
            if (unaryProto.postfix) JsPostfixOperation(type, operand) else JsPrefixOperation(type, operand)
        }

        ExpressionCase.CONDITIONAL -> {
            val conditionalProto = proto.conditional
            JsConditional(
                    deserialize(conditionalProto.testExpression),
                    deserialize(conditionalProto.thenExpression),
                    deserialize(conditionalProto.elseExpression)
            )
        }

        ExpressionCase.ARRAY_ACCESS -> {
            val arrayAccessProto = proto.arrayAccess
            JsArrayAccess(deserialize(arrayAccessProto.array), deserialize(arrayAccessProto.index))
        }

        ExpressionCase.NAME_REFERENCE -> {
            val nameRefProto = proto.nameReference
            val qualifier = if (nameRefProto.hasQualifier()) deserialize(nameRefProto.qualifier) else null
            JsNameRef(deserializeName(nameRefProto.nameId), qualifier).apply {
                if (nameRefProto.hasInlineStrategy()) {
                    inlineStrategy = map(nameRefProto.inlineStrategy)
                }
            }
        }

        ExpressionCase.PROPERTY_REFERENCE -> {
            val propertyRefProto = proto.propertyReference
            val qualifier = if (propertyRefProto.hasQualifier()) deserialize(propertyRefProto.qualifier) else null
            JsNameRef(deserializeString(propertyRefProto.stringId), qualifier).apply {
                if (propertyRefProto.hasInlineStrategy()) {
                    inlineStrategy = map(propertyRefProto.inlineStrategy)
                }
            }
        }

        ExpressionCase.INVOCATION -> {
            val invocationProto = proto.invocation
            JsInvocation(
                    deserialize(invocationProto.qualifier),
                    invocationProto.argumentList.map { deserialize(it) }
            ).apply {
                if (invocationProto.hasInlineStrategy()) {
                    inlineStrategy = map(invocationProto.inlineStrategy)
                }
            }
        }

        ExpressionCase.INSTANTIATION -> {
            val instantiationProto = proto.instantiation
            JsNew(
                    deserialize(instantiationProto.qualifier),
                    instantiationProto.argumentList.map { deserialize(it) }
            )
        }

        null,
        ExpressionCase.EXPRESSION_NOT_SET -> error("Unknown expression")
    }

    private fun deserializeVars(proto: Vars): JsVars {
        val vars = JsVars(proto.multiline)
        for (declProto in proto.declarationList) {
            vars.vars += withLocation(
                    fileId = if (declProto.hasFileId()) declProto.fileId else null,
                    location = if (declProto.hasLocation()) declProto.location else null
            ) {
                val initialValue = if (declProto.hasInitialValue()) deserialize(declProto.initialValue) else null
                JsVars.JsVar(deserializeName(declProto.nameId), initialValue)
            }
        }
        if (proto.hasExportedPackageId()) {
            vars.exportedPackage = deserializeString(proto.exportedPackageId)
        }
        return vars
    }

    private fun deserializeGlobalBlock(proto: GlobalBlock): JsGlobalBlock {
        return JsGlobalBlock().apply { statements += proto.statementList.map { deserialize(it) } }
    }

    private fun deserializeParameter(proto: Parameter): JsParameter {
        return JsParameter(deserializeName(proto.nameId)).apply {
            hasDefaultValue = proto.hasDefaultValue
        }
    }

    private fun deserializeName(id: Int): JsName {
        return nameCache[id] ?: let {
            val nameProto = nameTable[id]
            val identifier = deserializeString(nameProto.identifier)
            val name = if (nameProto.temporary) {
                JsScope.declareTemporaryName(identifier)
            }
            else {
                JsDynamicScope.declareName(identifier)
            }
            if (nameProto.hasLocalNameId()) {
                name.localAlias = deserializeName(nameProto.localNameId)
            }
            if (nameProto.hasImported()) {
                name.imported = nameProto.imported
            }
            if (nameProto.hasSpecialFunction()) {
                name.specialFunction = map(nameProto.specialFunction)
            }
            nameCache[id] = name
            name
        }
    }

    private fun deserializeString(id: Int): String = stringTable[id]

    private fun map(op: BinaryOperation.Type) = when (op) {
        BinaryOperation.Type.MUL -> JsBinaryOperator.MUL
        BinaryOperation.Type.DIV -> JsBinaryOperator.DIV
        BinaryOperation.Type.MOD -> JsBinaryOperator.MOD
        BinaryOperation.Type.ADD -> JsBinaryOperator.ADD
        BinaryOperation.Type.SUB -> JsBinaryOperator.SUB
        BinaryOperation.Type.SHL -> JsBinaryOperator.SHL
        BinaryOperation.Type.SHR -> JsBinaryOperator.SHR
        BinaryOperation.Type.SHRU -> JsBinaryOperator.SHRU
        BinaryOperation.Type.LT -> JsBinaryOperator.LT
        BinaryOperation.Type.LTE -> JsBinaryOperator.LTE
        BinaryOperation.Type.GT -> JsBinaryOperator.GT
        BinaryOperation.Type.GTE -> JsBinaryOperator.GTE
        BinaryOperation.Type.INSTANCEOF -> JsBinaryOperator.INSTANCEOF
        BinaryOperation.Type.IN -> JsBinaryOperator.INOP
        BinaryOperation.Type.EQ -> JsBinaryOperator.EQ
        BinaryOperation.Type.NEQ -> JsBinaryOperator.NEQ
        BinaryOperation.Type.REF_EQ -> JsBinaryOperator.REF_EQ
        BinaryOperation.Type.REF_NEQ -> JsBinaryOperator.REF_NEQ
        BinaryOperation.Type.BIT_AND -> JsBinaryOperator.BIT_AND
        BinaryOperation.Type.BIT_XOR -> JsBinaryOperator.BIT_XOR
        BinaryOperation.Type.BIT_OR -> JsBinaryOperator.BIT_OR
        BinaryOperation.Type.AND -> JsBinaryOperator.AND
        BinaryOperation.Type.OR -> JsBinaryOperator.OR
        BinaryOperation.Type.ASG -> JsBinaryOperator.ASG
        BinaryOperation.Type.ASG_ADD -> JsBinaryOperator.ASG_ADD
        BinaryOperation.Type.ASG_SUB -> JsBinaryOperator.ASG_SUB
        BinaryOperation.Type.ASG_MUL -> JsBinaryOperator.ASG_MUL
        BinaryOperation.Type.ASG_DIV -> JsBinaryOperator.ASG_DIV
        BinaryOperation.Type.ASG_MOD -> JsBinaryOperator.ASG_MOD
        BinaryOperation.Type.ASG_SHL -> JsBinaryOperator.ASG_SHL
        BinaryOperation.Type.ASG_SHR -> JsBinaryOperator.ASG_SHR
        BinaryOperation.Type.ASG_SHRU -> JsBinaryOperator.ASG_SHRU
        BinaryOperation.Type.ASG_BIT_AND -> JsBinaryOperator.ASG_BIT_AND
        BinaryOperation.Type.ASG_BIT_OR -> JsBinaryOperator.ASG_BIT_OR
        BinaryOperation.Type.ASG_BIT_XOR -> JsBinaryOperator.ASG_BIT_XOR
        BinaryOperation.Type.COMMA -> JsBinaryOperator.COMMA
    }

    private fun map(op: UnaryOperation.Type) = when (op) {
        UnaryOperation.Type.BIT_NOT -> JsUnaryOperator.BIT_NOT
        UnaryOperation.Type.DEC -> JsUnaryOperator.DEC
        UnaryOperation.Type.DELETE -> JsUnaryOperator.DELETE
        UnaryOperation.Type.INC -> JsUnaryOperator.INC
        UnaryOperation.Type.NEG -> JsUnaryOperator.NEG
        UnaryOperation.Type.POS -> JsUnaryOperator.POS
        UnaryOperation.Type.NOT -> JsUnaryOperator.NOT
        UnaryOperation.Type.TYPEOF -> JsUnaryOperator.TYPEOF
        UnaryOperation.Type.VOID -> JsUnaryOperator.VOID
    }

    private fun map(sideEffects: SideEffects) = when (sideEffects) {
        SideEffects.AFFECTS_STATE -> SideEffectKind.AFFECTS_STATE
        SideEffects.DEPENDS_ON_STATE -> SideEffectKind.DEPENDS_ON_STATE
        SideEffects.PURE -> SideEffectKind.PURE
    }

    private fun map(inlineStrategy: InlineStrategy) = when(inlineStrategy) {
        InlineStrategy.AS_FUNCTION -> KotlinInlineStrategy.AS_FUNCTION
        InlineStrategy.IN_PLACE -> KotlinInlineStrategy.IN_PLACE
        InlineStrategy.NOT_INLINE -> KotlinInlineStrategy.NOT_INLINE
    }

    private fun map(specialFunction: JsAstProtoBuf.SpecialFunction) = when(specialFunction) {
        JsAstProtoBuf.SpecialFunction.DEFINE_INLINE_FUNCTION -> SpecialFunction.DEFINE_INLINE_FUNCTION
        JsAstProtoBuf.SpecialFunction.WRAP_FUNCTION -> SpecialFunction.WRAP_FUNCTION
        JsAstProtoBuf.SpecialFunction.TO_BOXED_CHAR -> SpecialFunction.TO_BOXED_CHAR
        JsAstProtoBuf.SpecialFunction.UNBOX_CHAR -> SpecialFunction.UNBOX_CHAR
        JsAstProtoBuf.SpecialFunction.SUSPEND_CALL -> SpecialFunction.SUSPEND_CALL
        JsAstProtoBuf.SpecialFunction.COROUTINE_RESULT -> SpecialFunction.COROUTINE_RESULT
        JsAstProtoBuf.SpecialFunction.COROUTINE_CONTROLLER -> SpecialFunction.COROUTINE_CONTROLLER
        JsAstProtoBuf.SpecialFunction.COROUTINE_RECEIVER -> SpecialFunction.COROUTINE_RECEIVER
        JsAstProtoBuf.SpecialFunction.SET_COROUTINE_RESULT -> SpecialFunction.SET_COROUTINE_RESULT
    }

    private fun <T : JsNode> withLocation(fileId: Int?, location: Location?, action: () -> T): T {
        val deserializedFile = fileId?.let { deserializeString(it) }
        val file = deserializedFile ?: fileStack.peek()
        val deserializedLocation = if (file != null && location != null) {
            JsLocation(file, location.startLine, location.startChar)
        }
        else {
            null
        }

        val shouldUpdateFile = location != null && deserializedFile != null && deserializedFile != fileStack.peek()
        if (shouldUpdateFile) {
            fileStack.push(deserializedFile)
        }
        val node = action()
        if (deserializedLocation != null) {
            val contentFile = sourceRoots
                    .map { File(it, file) }
                    .firstOrNull { it.exists() }
            node.source = if (contentFile != null) {
                JsLocationWithEmbeddedSource(deserializedLocation, null) { InputStreamReader(FileInputStream(contentFile), "UTF-8") }
            }
            else {
                deserializedLocation
            }
        }
        if (shouldUpdateFile) {
            fileStack.pop()
        }

        return node
    }
}
