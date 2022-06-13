/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.js.ast

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import java.util.*

abstract class JsAstSerializerBase {

    protected val nameTableBuilder = JsAstProtoBuf.NameTable.newBuilder()
    protected val stringTableBuilder = JsAstProtoBuf.StringTable.newBuilder()
    protected val nameMap = mutableMapOf<JsName, Int>()
    protected val stringMap = mutableMapOf<String, Int>()
    protected val fileStack: Deque<String> = ArrayDeque()
    protected val importedNames = mutableSetOf<JsName>()

    protected fun serialize(statement: JsStatement): JsAstProtoBuf.Statement {
        val visitor = object : JsVisitor() {
            val builder = JsAstProtoBuf.Statement.newBuilder()

            override fun visitReturn(x: JsReturn) {
                val returnBuilder = JsAstProtoBuf.Return.newBuilder()
                x.expression?.let { returnBuilder.value = serialize(it) }
                builder.returnStatement = returnBuilder.build()
            }

            override fun visitThrow(x: JsThrow) {
                val throwBuilder = JsAstProtoBuf.Throw.newBuilder()
                throwBuilder.exception = serialize(x.expression)
                builder.throwStatement = throwBuilder.build()
            }

            override fun visitBreak(x: JsBreak) {
                val breakBuilder = JsAstProtoBuf.Break.newBuilder()
                x.label?.let { breakBuilder.labelId = serialize(it.name!!) }
                builder.breakStatement = breakBuilder.build()
            }

            override fun visitContinue(x: JsContinue) {
                val continueBuilder = JsAstProtoBuf.Continue.newBuilder()
                x.label?.let { continueBuilder.labelId = serialize(it.name!!) }
                builder.continueStatement = continueBuilder.build()
            }

            override fun visitDebugger(x: JsDebugger) {
                builder.debugger = JsAstProtoBuf.Debugger.newBuilder().build()
            }

            override fun visitExpressionStatement(x: JsExpressionStatement) {
                val statementBuilder = JsAstProtoBuf.ExpressionStatement.newBuilder()
                statementBuilder.expression = serialize(x.expression)
                val tag = x.exportedTag
                if (tag != null) {
                    statementBuilder.exportedTagId = serialize(tag)
                }

                builder.expression = statementBuilder.build()
            }

            override fun visitVars(x: JsVars) {
                builder.vars = serializeVars(x)
            }

            override fun visitBlock(x: JsBlock) {
                when (x) {
                    is JsCompositeBlock -> { builder.compositeBlock = serializeBlock(x) }
                    else -> {
                        val blockBuilder = JsAstProtoBuf.Block.newBuilder()
                        for (part in x.statements) {
                            blockBuilder.addStatement(serialize(part))
                        }
                        builder.block = blockBuilder.build()
                    }
                }
            }

            override fun visitLabel(x: JsLabel) {
                val labelBuilder = JsAstProtoBuf.Label.newBuilder()
                labelBuilder.nameId = serialize(x.name)
                labelBuilder.innerStatement = serialize(x.statement)
                builder.label = labelBuilder.build()
            }

            override fun visitIf(x: JsIf) {
                val ifBuilder = JsAstProtoBuf.If.newBuilder()
                ifBuilder.condition = serialize(x.ifExpression)
                ifBuilder.thenStatement = serialize(x.thenStatement)
                x.elseStatement?.let { ifBuilder.elseStatement = serialize(it) }
                builder.ifStatement = ifBuilder.build()
            }

            override fun visit(x: JsSwitch) {
                val switchBuilder = JsAstProtoBuf.Switch.newBuilder()
                switchBuilder.expression = serialize(x.expression)
                for (case in x.cases) {
                    val entryBuilder = JsAstProtoBuf.SwitchEntry.newBuilder()
                    withLocation(case, { entryBuilder.fileId = it }, { entryBuilder.location = it }) {}
                    if (case is JsCase) {
                        entryBuilder.label = serialize(case.caseExpression)
                    }
                    for (part in case.statements) {
                        entryBuilder.addStatement(serialize(part))
                    }
                    switchBuilder.addEntry(entryBuilder)
                }
                builder.switchStatement = switchBuilder.build()
            }

            override fun visitWhile(x: JsWhile) {
                val whileBuilder = JsAstProtoBuf.While.newBuilder()
                whileBuilder.condition = serialize(x.condition)
                whileBuilder.body = serialize(x.body)
                builder.whileStatement = whileBuilder.build()
            }

            override fun visitDoWhile(x: JsDoWhile) {
                val doWhileBuilder = JsAstProtoBuf.DoWhile.newBuilder()
                doWhileBuilder.condition = serialize(x.condition)
                doWhileBuilder.body = serialize(x.body)
                builder.doWhileStatement = doWhileBuilder.build()
            }

            override fun visitFor(x: JsFor) {
                val forBuilder = JsAstProtoBuf.For.newBuilder()
                when {
                    x.initVars != null -> forBuilder.variables = serialize(x.initVars)
                    x.initExpression != null -> forBuilder.expression = serialize(x.initExpression)
                    else -> forBuilder.empty = JsAstProtoBuf.EmptyInit.newBuilder().build()
                }
                x.condition?.let { forBuilder.condition = serialize(it) }
                x.incrementExpression?.let { forBuilder.increment = serialize(it) }
                forBuilder.body = serialize(x.body ?: JsEmpty)
                builder.forStatement = forBuilder.build()
            }

            override fun visitForIn(x: JsForIn) {
                val forInBuilder = JsAstProtoBuf.ForIn.newBuilder()
                when {
                    x.iterVarName != null -> forInBuilder.nameId = serialize(x.iterVarName)
                    x.iterExpression != null -> forInBuilder.expression = serialize(x.iterExpression)
                }
                forInBuilder.iterable = serialize(x.objectExpression)
                forInBuilder.body = serialize(x.body)
                builder.forInStatement = forInBuilder.build()
            }

            override fun visitTry(x: JsTry) {
                val tryBuilder = JsAstProtoBuf.Try.newBuilder()
                tryBuilder.tryBlock = serialize(x.tryBlock)
                x.catches.firstOrNull()?.let { c ->
                    val catchBuilder = JsAstProtoBuf.Catch.newBuilder()
                    catchBuilder.parameter = serializeParameter(c.parameter)
                    catchBuilder.body = serialize(c.body)
                    tryBuilder.catchBlock = catchBuilder.build()
                }
                x.finallyBlock?.let { tryBuilder.finallyBlock = serialize(it) }
                builder.tryStatement = tryBuilder.build()
            }

            override fun visitEmpty(x: JsEmpty) {
                builder.empty = JsAstProtoBuf.Empty.newBuilder().build()
            }

            override fun visitSingleLineComment(comment: JsSingleLineComment) {
                builder.singleLineComment = JsAstProtoBuf.SingleLineComment.newBuilder().setMessage(comment.text).build()
            }

            override fun visitMultiLineComment(comment: JsMultiLineComment) {
                builder.multiLineComment = JsAstProtoBuf.MultiLineComment.newBuilder().setMessage(comment.text).build()
            }
        }

        withComments(statement, { visitor.builder.addBeforeComments(it) }, { visitor.builder.addAfterComments(it) }) {
            withLocation(statement, { visitor.builder.fileId = it }, { visitor.builder.location = it }) {
                statement.accept(visitor)
            }
        }

        if (statement is HasMetadata && statement.synthetic) {
            visitor.builder.synthetic = true
        }

        if (visitor.builder.statementCase == JsAstProtoBuf.Statement.StatementCase.STATEMENT_NOT_SET) {
            error("Unknown statement type: ${statement::class.qualifiedName}")
        }

        return visitor.builder.build()
    }

    protected fun serialize(expression: JsExpression): JsAstProtoBuf.Expression {
        val visitor = object : JsVisitor() {
            val builder = JsAstProtoBuf.Expression.newBuilder()

            override fun visitThis(x: JsThisRef) {
                builder.thisLiteral = JsAstProtoBuf.ThisLiteral.newBuilder().build()
            }

            override fun visitNull(x: JsNullLiteral) {
                builder.nullLiteral = JsAstProtoBuf.NullLiteral.newBuilder().build()
            }

            override fun visitBoolean(x: JsBooleanLiteral) {
                if (x.value) {
                    builder.trueLiteral = JsAstProtoBuf.TrueLiteral.newBuilder().build()
                } else {
                    builder.falseLiteral = JsAstProtoBuf.FalseLiteral.newBuilder().build()
                }
            }

            override fun visitString(x: JsStringLiteral) {
                builder.stringLiteral = serialize(x.value)
            }

            override fun visitRegExp(x: JsRegExp) {
                val regExpBuilder = JsAstProtoBuf.RegExpLiteral.newBuilder()
                regExpBuilder.patternStringId = serialize(x.pattern)
                x.flags?.let { regExpBuilder.flagsStringId = serialize(it) }
                builder.regExpLiteral = regExpBuilder.build()
            }

            override fun visitInt(x: JsIntLiteral) {
                builder.intLiteral = x.value
            }

            override fun visitDouble(x: JsDoubleLiteral) {
                builder.doubleLiteral = x.value
            }

            override fun visitArray(x: JsArrayLiteral) {
                val arrayBuilder = JsAstProtoBuf.ArrayLiteral.newBuilder()
                x.expressions.forEach { arrayBuilder.addElement(serialize(it)) }
                builder.arrayLiteral = arrayBuilder.build()
            }

            override fun visitObjectLiteral(x: JsObjectLiteral) {
                val objectBuilder = JsAstProtoBuf.ObjectLiteral.newBuilder()
                for (initializer in x.propertyInitializers) {
                    val entryBuilder = JsAstProtoBuf.ObjectLiteralEntry.newBuilder()
                    entryBuilder.key = serialize(initializer.labelExpr)
                    entryBuilder.value = serialize(initializer.valueExpr)
                    objectBuilder.addEntry(entryBuilder)
                }
                objectBuilder.multiline = x.isMultiline
                builder.objectLiteral = objectBuilder.build()
            }

            override fun visitFunction(x: JsFunction) {
                val functionBuilder = JsAstProtoBuf.Function.newBuilder()
                x.parameters.forEach { functionBuilder.addParameter(serializeParameter(it)) }
                x.name?.let { functionBuilder.nameId = serialize(it) }
                functionBuilder.body = serialize(x.body)
                if (x.isLocal) {
                    functionBuilder.local = true
                }
                builder.function = functionBuilder.build()
            }

            override fun visitDocComment(comment: JsDocComment) {
                val commentBuilder = JsAstProtoBuf.DocComment.newBuilder()
                for ((name, value) in comment.tags) {
                    val tagBuilder = JsAstProtoBuf.DocCommentTag.newBuilder()
                    tagBuilder.nameId = serialize(name)
                    when (value) {
                        is JsNameRef -> tagBuilder.expression = serialize(value)
                        is String -> tagBuilder.valueStringId = serialize(value)
                    }
                    commentBuilder.addTag(tagBuilder)
                }
                builder.docComment = commentBuilder.build()
            }

            override fun visitBinaryExpression(x: JsBinaryOperation) {
                val binaryBuilder = JsAstProtoBuf.BinaryOperation.newBuilder()
                binaryBuilder.left = serialize(x.arg1)
                binaryBuilder.right = serialize(x.arg2)
                binaryBuilder.type = map(x.operator)
                builder.binary = binaryBuilder.build()
            }

            override fun visitPrefixOperation(x: JsPrefixOperation) {
                builder.unary = serializeUnary(x, postfix = false)
            }

            override fun visitPostfixOperation(x: JsPostfixOperation) {
                builder.unary = serializeUnary(x, postfix = true)
            }

            override fun visitConditional(x: JsConditional) {
                val conditionalBuilder = JsAstProtoBuf.Conditional.newBuilder()
                conditionalBuilder.testExpression = serialize(x.testExpression)
                conditionalBuilder.thenExpression = serialize(x.thenExpression)
                conditionalBuilder.elseExpression = serialize(x.elseExpression)
                builder.conditional = conditionalBuilder.build()
            }

            override fun visitArrayAccess(x: JsArrayAccess) {
                val arrayAccessBuilder = JsAstProtoBuf.ArrayAccess.newBuilder()
                arrayAccessBuilder.array = serialize(x.arrayExpression)
                arrayAccessBuilder.index = serialize(x.indexExpression)
                builder.arrayAccess = arrayAccessBuilder.build()
            }

            override fun visitNameRef(nameRef: JsNameRef) {
                val name = nameRef.name
                val qualifier = nameRef.qualifier
                if (name != null) {
                    if (qualifier != null || nameRef.isInline == true) {
                        val nameRefBuilder = JsAstProtoBuf.NameReference.newBuilder()
                        nameRefBuilder.nameId = serialize(name)
                        if (qualifier != null) {
                            nameRefBuilder.qualifier = serialize(qualifier)
                        }
                        nameRef.isInline?.let {
                            nameRefBuilder.inlineStrategy = if (it) JsAstProtoBuf.InlineStrategy.IN_PLACE else JsAstProtoBuf.InlineStrategy.NOT_INLINE
                        }
                        builder.nameReference = nameRefBuilder.build()
                    } else {
                        builder.simpleNameReference = serialize(name)
                    }
                } else {
                    val propertyRefBuilder = JsAstProtoBuf.PropertyReference.newBuilder()
                    propertyRefBuilder.stringId = serialize(nameRef.ident)
                    qualifier?.let { propertyRefBuilder.qualifier = serialize(it) }
                    nameRef.isInline?.let {
                        propertyRefBuilder.inlineStrategy = if (it) JsAstProtoBuf.InlineStrategy.IN_PLACE else JsAstProtoBuf.InlineStrategy.NOT_INLINE
                    }
                    builder.propertyReference = propertyRefBuilder.build()
                }
            }

            override fun visitInvocation(invocation: JsInvocation) {
                val invocationBuilder = JsAstProtoBuf.Invocation.newBuilder()
                invocationBuilder.qualifier = serialize(invocation.qualifier)
                invocation.arguments.forEach { invocationBuilder.addArgument(serialize(it)) }
                if (invocation.isInline == true) {
                    invocationBuilder.inlineStrategy = JsAstProtoBuf.InlineStrategy.IN_PLACE
                }
                builder.invocation = invocationBuilder.build()
            }

            override fun visitNew(x: JsNew) {
                val instantiationBuilder = JsAstProtoBuf.Instantiation.newBuilder()
                instantiationBuilder.qualifier = serialize(x.constructorExpression)
                x.arguments.forEach { instantiationBuilder.addArgument(serialize(it)) }
                builder.instantiation = instantiationBuilder.build()
            }
        }

        withComments(expression, { visitor.builder.addBeforeComments(it) }, { visitor.builder.addAfterComments(it) }) {
            withLocation(expression, { visitor.builder.fileId = it }, { visitor.builder.location = it }) {
                expression.accept(visitor)
            }
        }

        with(visitor.builder) {
            synthetic = expression.synthetic
            sideEffects = map(expression.sideEffects)
            expression.localAlias?.let { localAlias = serialize(it) }
        }

        return visitor.builder.build()
    }

    protected fun serialize(module: JsImportedModule): JsAstProtoBuf.JsImportedModule {
        val moduleBuilder = JsAstProtoBuf.JsImportedModule.newBuilder()
        moduleBuilder.externalName = serialize(module.externalName)
        moduleBuilder.internalName = serialize(module.internalName)
        module.plainReference?.let {
            moduleBuilder.plainReference = serialize(it)
        }
        return moduleBuilder.build()
    }

    protected fun serializeParameter(parameter: JsParameter): JsAstProtoBuf.Parameter {
        val parameterBuilder = JsAstProtoBuf.Parameter.newBuilder()
        parameterBuilder.nameId = serialize(parameter.name)
        if (parameter.hasDefaultValue) {
            parameterBuilder.hasDefaultValue = true
        }
        return parameterBuilder.build()
    }

    protected fun serializeBlock(block: JsCompositeBlock): JsAstProtoBuf.CompositeBlock {
        val blockBuilder = JsAstProtoBuf.CompositeBlock.newBuilder()
        for (part in block.statements) {
            blockBuilder.addStatement(serialize(part))
        }
        return blockBuilder.build()
    }

    protected fun serializeVars(vars: JsVars): JsAstProtoBuf.Vars {
        val varsBuilder = JsAstProtoBuf.Vars.newBuilder()
        for (varDecl in vars.vars) {
            val declBuilder = JsAstProtoBuf.VarDeclaration.newBuilder()
            withLocation(varDecl, { declBuilder.fileId = it }, { declBuilder.location = it }) {
                declBuilder.nameId = serialize(varDecl.name)
                varDecl.initExpression?.let { declBuilder.initialValue = serialize(it) }
            }
            varsBuilder.addDeclaration(declBuilder)
        }

        if (vars.isMultiline) {
            varsBuilder.multiline = true
        }
        vars.exportedPackage?.let { varsBuilder.exportedPackageId = serialize(it) }

        return varsBuilder.build()
    }

    protected fun serializeUnary(x: JsUnaryOperation, postfix: Boolean): JsAstProtoBuf.UnaryOperation {
        val unaryBuilder = JsAstProtoBuf.UnaryOperation.newBuilder()
        unaryBuilder.operand = serialize(x.arg)
        unaryBuilder.type = map(x.operator)
        unaryBuilder.postfix = postfix
        return unaryBuilder.build()
    }

    protected fun map(op: JsBinaryOperator) = when (op) {
        JsBinaryOperator.MUL -> JsAstProtoBuf.BinaryOperation.Type.MUL
        JsBinaryOperator.DIV -> JsAstProtoBuf.BinaryOperation.Type.DIV
        JsBinaryOperator.MOD -> JsAstProtoBuf.BinaryOperation.Type.MOD
        JsBinaryOperator.ADD -> JsAstProtoBuf.BinaryOperation.Type.ADD
        JsBinaryOperator.SUB -> JsAstProtoBuf.BinaryOperation.Type.SUB
        JsBinaryOperator.SHL -> JsAstProtoBuf.BinaryOperation.Type.SHL
        JsBinaryOperator.SHR -> JsAstProtoBuf.BinaryOperation.Type.SHR
        JsBinaryOperator.SHRU -> JsAstProtoBuf.BinaryOperation.Type.SHRU
        JsBinaryOperator.LT -> JsAstProtoBuf.BinaryOperation.Type.LT
        JsBinaryOperator.LTE -> JsAstProtoBuf.BinaryOperation.Type.LTE
        JsBinaryOperator.GT -> JsAstProtoBuf.BinaryOperation.Type.GT
        JsBinaryOperator.GTE -> JsAstProtoBuf.BinaryOperation.Type.GTE
        JsBinaryOperator.INSTANCEOF -> JsAstProtoBuf.BinaryOperation.Type.INSTANCEOF
        JsBinaryOperator.INOP -> JsAstProtoBuf.BinaryOperation.Type.IN
        JsBinaryOperator.EQ -> JsAstProtoBuf.BinaryOperation.Type.EQ
        JsBinaryOperator.NEQ -> JsAstProtoBuf.BinaryOperation.Type.NEQ
        JsBinaryOperator.REF_EQ -> JsAstProtoBuf.BinaryOperation.Type.REF_EQ
        JsBinaryOperator.REF_NEQ -> JsAstProtoBuf.BinaryOperation.Type.REF_NEQ
        JsBinaryOperator.BIT_AND -> JsAstProtoBuf.BinaryOperation.Type.BIT_AND
        JsBinaryOperator.BIT_XOR -> JsAstProtoBuf.BinaryOperation.Type.BIT_XOR
        JsBinaryOperator.BIT_OR -> JsAstProtoBuf.BinaryOperation.Type.BIT_OR
        JsBinaryOperator.AND -> JsAstProtoBuf.BinaryOperation.Type.AND
        JsBinaryOperator.OR -> JsAstProtoBuf.BinaryOperation.Type.OR
        JsBinaryOperator.ASG -> JsAstProtoBuf.BinaryOperation.Type.ASG
        JsBinaryOperator.ASG_ADD -> JsAstProtoBuf.BinaryOperation.Type.ASG_ADD
        JsBinaryOperator.ASG_SUB -> JsAstProtoBuf.BinaryOperation.Type.ASG_SUB
        JsBinaryOperator.ASG_MUL -> JsAstProtoBuf.BinaryOperation.Type.ASG_MUL
        JsBinaryOperator.ASG_DIV -> JsAstProtoBuf.BinaryOperation.Type.ASG_DIV
        JsBinaryOperator.ASG_MOD -> JsAstProtoBuf.BinaryOperation.Type.ASG_MOD
        JsBinaryOperator.ASG_SHL -> JsAstProtoBuf.BinaryOperation.Type.ASG_SHL
        JsBinaryOperator.ASG_SHR -> JsAstProtoBuf.BinaryOperation.Type.ASG_SHR
        JsBinaryOperator.ASG_SHRU -> JsAstProtoBuf.BinaryOperation.Type.ASG_SHRU
        JsBinaryOperator.ASG_BIT_AND -> JsAstProtoBuf.BinaryOperation.Type.ASG_BIT_AND
        JsBinaryOperator.ASG_BIT_OR -> JsAstProtoBuf.BinaryOperation.Type.ASG_BIT_OR
        JsBinaryOperator.ASG_BIT_XOR -> JsAstProtoBuf.BinaryOperation.Type.ASG_BIT_XOR
        JsBinaryOperator.COMMA -> JsAstProtoBuf.BinaryOperation.Type.COMMA
    }

    protected fun map(op: JsUnaryOperator) = when (op) {
        JsUnaryOperator.BIT_NOT -> JsAstProtoBuf.UnaryOperation.Type.BIT_NOT
        JsUnaryOperator.DEC -> JsAstProtoBuf.UnaryOperation.Type.DEC
        JsUnaryOperator.DELETE -> JsAstProtoBuf.UnaryOperation.Type.DELETE
        JsUnaryOperator.INC -> JsAstProtoBuf.UnaryOperation.Type.INC
        JsUnaryOperator.NEG -> JsAstProtoBuf.UnaryOperation.Type.NEG
        JsUnaryOperator.POS -> JsAstProtoBuf.UnaryOperation.Type.POS
        JsUnaryOperator.NOT -> JsAstProtoBuf.UnaryOperation.Type.NOT
        JsUnaryOperator.TYPEOF -> JsAstProtoBuf.UnaryOperation.Type.TYPEOF
        JsUnaryOperator.VOID -> JsAstProtoBuf.UnaryOperation.Type.VOID
    }

    protected fun map(sideEffects: SideEffectKind) = when (sideEffects) {
        SideEffectKind.AFFECTS_STATE -> JsAstProtoBuf.SideEffects.AFFECTS_STATE
        SideEffectKind.DEPENDS_ON_STATE -> JsAstProtoBuf.SideEffects.DEPENDS_ON_STATE
        SideEffectKind.PURE -> JsAstProtoBuf.SideEffects.PURE
    }

    protected fun map(specialFunction: SpecialFunction) = when (specialFunction) {
        SpecialFunction.DEFINE_INLINE_FUNCTION -> JsAstProtoBuf.SpecialFunction.DEFINE_INLINE_FUNCTION
        SpecialFunction.WRAP_FUNCTION -> JsAstProtoBuf.SpecialFunction.WRAP_FUNCTION
        SpecialFunction.TO_BOXED_CHAR -> JsAstProtoBuf.SpecialFunction.TO_BOXED_CHAR
        SpecialFunction.UNBOX_CHAR -> JsAstProtoBuf.SpecialFunction.UNBOX_CHAR
        SpecialFunction.SUSPEND_CALL -> JsAstProtoBuf.SpecialFunction.SUSPEND_CALL
        SpecialFunction.COROUTINE_RESULT -> JsAstProtoBuf.SpecialFunction.COROUTINE_RESULT
        SpecialFunction.COROUTINE_CONTROLLER -> JsAstProtoBuf.SpecialFunction.COROUTINE_CONTROLLER
        SpecialFunction.COROUTINE_RECEIVER -> JsAstProtoBuf.SpecialFunction.COROUTINE_RECEIVER
        SpecialFunction.SET_COROUTINE_RESULT -> JsAstProtoBuf.SpecialFunction.SET_COROUTINE_RESULT
        SpecialFunction.GET_KCLASS -> JsAstProtoBuf.SpecialFunction.GET_KCLASS
        SpecialFunction.GET_REIFIED_TYPE_PARAMETER_KTYPE -> JsAstProtoBuf.SpecialFunction.GET_REIFIED_TYPE_PARAMETER_KTYPE
    }

    protected fun serialize(name: JsName): Int = nameMap.getOrPut(name) {
        val builder = JsAstProtoBuf.Name.newBuilder()
        builder.identifier = serialize(name.ident)
        builder.temporary = name.isTemporary
        name.localAlias?.let {
            builder.localNameId = serialize(it)
        }

        if (name.imported && name !in importedNames) {
            builder.imported = true
        }

        name.specialFunction?.let {
            builder.specialFunction = map(it)
        }

        val result = nameTableBuilder.entryCount
        nameTableBuilder.addEntry(builder)
        result
    }

    protected fun serialize(alias: LocalAlias): JsAstProtoBuf.LocalAlias {
        val builder = JsAstProtoBuf.LocalAlias.newBuilder()
        builder.localNameId = serialize(alias.name)
        alias.tag?.let {
            builder.tag = serialize(it)
        }
        return builder.build()
    }

    protected fun serialize(string: String) = stringMap.getOrPut(string) {
        val result = stringTableBuilder.entryCount
        stringTableBuilder.addEntry(string)
        result
    }

    protected fun serialize(comment: JsComment): JsAstProtoBuf.Comment {
        val builder = JsAstProtoBuf.Comment.newBuilder().apply {
            text = comment.text
            multiline = when (comment) {
                is JsSingleLineComment -> false
                is JsMultiLineComment -> true
                else -> error("Unknown type of comment ${comment.javaClass.name}")
            }
        }
        return builder.build()
    }

    private inline fun withLocation(node: JsNode, fileConsumer: (Int) -> Unit, locationConsumer: (JsAstProtoBuf.Location) -> Unit, inner: () -> Unit) {
        val location = extractLocation(node)
        var fileChanged = false
        if (location != null) {
            val lastFile = fileStack.peek()
            val newFile = location.file
            fileChanged = lastFile != newFile
            if (fileChanged) {
                fileConsumer(serialize(newFile))
                fileStack.push(location.file)
            }
            val locationBuilder = JsAstProtoBuf.Location.newBuilder()
            locationBuilder.startLine = location.startLine
            locationBuilder.startChar = location.startChar
            locationConsumer(locationBuilder.build())
        }

        inner()

        if (fileChanged) {
            fileStack.pop()
        }
    }

    private inline fun withComments(
        node: JsNode,
        beforeCommentsConsumer: (JsAstProtoBuf.Comment) -> Unit,
        afterCommentsConsumer: (JsAstProtoBuf.Comment) -> Unit,
        inner: () -> Unit
    ) {
        node.commentsBeforeNode?.forEach { beforeCommentsConsumer(serialize(it))}
        node.commentsAfterNode?.forEach { afterCommentsConsumer(serialize(it))}
        inner()
    }

    abstract fun extractLocation(node: JsNode): JsLocation?
}