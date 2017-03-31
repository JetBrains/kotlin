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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf.*
import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf.BinaryOperation.Type.*
import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf.UnaryOperation.Type.*
import java.io.OutputStream
import java.util.*
import org.jetbrains.kotlin.resolve.inline.InlineStrategy as KotlinInlineStrategy

class JsAstSerializer {
    private val nameTableBuilder = NameTable.newBuilder()
    private val stringTableBuilder = StringTable.newBuilder()
    private val nameMap = mutableMapOf<JsName, Int>()
    private val stringMap = mutableMapOf<String, Int>()
    private val fileStack: Deque<String> = ArrayDeque()

    fun serialize(fragment: JsProgramFragment, output: OutputStream) {
        serialize(fragment).writeTo(output)
    }

    fun serialize(fragment: JsProgramFragment): Chunk {
        try {
            val chunkBuilder = Chunk.newBuilder()
            chunkBuilder.fragment = serializeFragment(fragment)
            chunkBuilder.nameTable = nameTableBuilder.build()
            chunkBuilder.stringTable = stringTableBuilder.build()
            return chunkBuilder.build()
        }
        finally {
            nameTableBuilder.clear()
            stringTableBuilder.clear()
            nameMap.clear()
            stringMap.clear()
        }
    }

    private fun serializeFragment(fragment: JsProgramFragment): Fragment {
        val fragmentBuilder = Fragment.newBuilder()

        for (importedModule in fragment.importedModules) {
            val importedModuleBuilder = ImportedModule.newBuilder()
            importedModuleBuilder.externalNameId = serialize(importedModule.externalName)
            importedModuleBuilder.internalNameId = serialize(importedModule.internalName)
            importedModule.plainReference?.let { importedModuleBuilder.plainReference = serialize(it) }
            fragmentBuilder.addImportedModule(importedModuleBuilder)
        }

        for ((signature, expression) in fragment.imports) {
            val importBuilder = Import.newBuilder()
            importBuilder.signatureId = serialize(signature)
            importBuilder.expression = serialize(expression)
            fragmentBuilder.addImportEntry(importBuilder)
        }

        fragmentBuilder.declarationBlock = serializeBlock(fragment.declarationBlock)
        fragmentBuilder.initializerBlock = serializeBlock(fragment.initializerBlock)
        fragmentBuilder.exportBlock = serializeBlock(fragment.exportBlock)

        for (nameBinding in fragment.nameBindings) {
            val nameBindingBuilder = NameBinding.newBuilder()
            nameBindingBuilder.signatureId = serialize(nameBinding.key)
            nameBindingBuilder.nameId = serialize(nameBinding.name)
            fragmentBuilder.addNameBinding(nameBindingBuilder)
        }

        fragment.classes.values.forEach { fragmentBuilder.addClassModel(serialize(it)) }

        val inlineModuleExprMap = mutableMapOf<JsExpression, Int>()
        for ((signature, expression) in fragment.inlineModuleMap) {
            val inlineModuleBuilder = InlineModule.newBuilder()
            inlineModuleBuilder.signatureId = serialize(signature)
            inlineModuleBuilder.expressionId = inlineModuleExprMap.getOrPut(expression) {
                val result = fragmentBuilder.moduleExpressionCount
                fragmentBuilder.addModuleExpression(serialize(expression))
                result
            }
            fragmentBuilder.addInlineModule(inlineModuleBuilder)
        }

        return fragmentBuilder.build()
    }

    private fun serialize(classModel: JsClassModel): ClassModel {
        val builder = ClassModel.newBuilder()
        builder.nameId = serialize(classModel.name)
        classModel.superName?.let { builder.superNameId = serialize(it) }
        if (classModel.postDeclarationBlock.statements.isNotEmpty()) {
            builder.postDeclarationBlock = serializeBlock(classModel.postDeclarationBlock)
        }
        return builder.build()
    }

    private fun serialize(statement: JsStatement): Statement {
        val visitor = object : JsVisitor() {
            val builder = Statement.newBuilder()

            override fun visitReturn(x: JsReturn) {
                val returnBuilder = Return.newBuilder()
                x.expression?.let { returnBuilder.value = serialize(it) }
                builder.returnStatement = returnBuilder.build()
            }

            override fun visitThrow(x: JsThrow) {
                val throwBuilder = Throw.newBuilder()
                throwBuilder.exception = serialize(x.expression)
                builder.throwStatement = throwBuilder.build()
            }

            override fun visitBreak(x: JsBreak) {
                val breakBuilder = Break.newBuilder()
                x.label?.let { breakBuilder.labelId = serialize(it.name!!) }
                builder.breakStatement = breakBuilder.build()
            }

            override fun visitContinue(x: JsContinue) {
                val continueBuilder = Continue.newBuilder()
                x.label?.let { continueBuilder.labelId = serialize(it.name!!) }
                builder.continueStatement = continueBuilder.build()
            }

            override fun visitDebugger(x: JsDebugger) {
                builder.debugger = Debugger.newBuilder().build()
            }

            override fun visitExpressionStatement(x: JsExpressionStatement) {
                val statementBuilder = ExpressionStatement.newBuilder()
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
                if (x is JsGlobalBlock) {
                    builder.globalBlock = serializeBlock(x)
                }
                else {
                    val blockBuilder = Block.newBuilder()
                    for (part in x.statements) {
                        blockBuilder.addStatement(serialize(part))
                    }
                    builder.block = blockBuilder.build()
                }
            }

            override fun visitLabel(x: JsLabel) {
                val labelBuilder = Label.newBuilder()
                labelBuilder.nameId = serialize(x.name)
                labelBuilder.innerStatement = serialize(x.statement)
                builder.label = labelBuilder.build()
            }

            override fun visitIf(x: JsIf) {
                val ifBuilder = If.newBuilder()
                ifBuilder.condition = serialize(x.ifExpression)
                ifBuilder.thenStatement = serialize(x.thenStatement)
                x.elseStatement?.let { ifBuilder.elseStatement = serialize(it) }
                builder.ifStatement = ifBuilder.build()
            }

            override fun visit(x: JsSwitch) {
                val switchBuilder = Switch.newBuilder()
                switchBuilder.expression = serialize(x.expression)
                for (case in x.cases) {
                    val entryBuilder = SwitchEntry.newBuilder()
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
                val whileBuilder = While.newBuilder()
                whileBuilder.condition = serialize(x.condition)
                whileBuilder.body = serialize(x.body)
                builder.whileStatement = whileBuilder.build()
            }

            override fun visitDoWhile(x: JsDoWhile) {
                val doWhileBuilder = DoWhile.newBuilder()
                doWhileBuilder.condition = serialize(x.condition)
                doWhileBuilder.body = serialize(x.body)
                builder.doWhileStatement = doWhileBuilder.build()
            }

            override fun visitFor(x: JsFor) {
                val forBuilder = For.newBuilder()
                when {
                    x.initVars != null -> forBuilder.variables = serializeVars(x.initVars)
                    x.initExpression != null -> forBuilder.expression = serialize(x.initExpression)
                    else -> forBuilder.empty = EmptyInit.newBuilder().build()
                }
                x.condition?.let { forBuilder.condition = serialize(it) }
                x.incrementExpression?.let { forBuilder.increment = serialize(it) }
                forBuilder.body = serialize(x.body ?: JsEmpty)
                builder.forStatement = forBuilder.build()
            }

            override fun visitForIn(x: JsForIn) {
                val forInBuilder = ForIn.newBuilder()
                when {
                    x.iterVarName != null -> forInBuilder.nameId = serialize(x.iterVarName)
                    x.iterExpression != null -> forInBuilder.expression = serialize(x.iterExpression)
                }
                forInBuilder.iterable = serialize(x.objectExpression)
                forInBuilder.body = serialize(x.body)
                builder.forInStatement = forInBuilder.build()
            }

            override fun visitTry(x: JsTry) {
                val tryBuilder = Try.newBuilder()
                tryBuilder.tryBlock = serialize(x.tryBlock)
                x.catches.firstOrNull()?.let { c ->
                    val catchBuilder = Catch.newBuilder()
                    catchBuilder.parameter = serializeParameter(c.parameter)
                    catchBuilder.body = serialize(c.body)
                    tryBuilder.catchBlock = catchBuilder.build()
                }
                x.finallyBlock?.let { tryBuilder.finallyBlock = serialize(it) }
                builder.tryStatement = tryBuilder.build()
            }

            override fun visitEmpty(x: JsEmpty) {
                builder.empty = Empty.newBuilder().build()
            }
        }

        withLocation(statement, { visitor.builder.fileId = it }, {visitor.builder.location = it }) {
            statement.accept(visitor)
        }

        if (statement is HasMetadata && statement.synthetic) {
            visitor.builder.synthetic = true
        }

        return visitor.builder.build()
    }

    private fun serialize(expression: JsExpression): Expression {
        val visitor = object : JsVisitor() {
            val builder = Expression.newBuilder()

            override fun visitThis(x: JsLiteral.JsThisRef) {
                builder.thisLiteral = ThisLiteral.newBuilder().build()
            }

            override fun visitNull(x: JsNullLiteral) {
                builder.nullLiteral = NullLiteral.newBuilder().build()
            }

            override fun visitBoolean(x: JsLiteral.JsBooleanLiteral) {
                if (x.value) {
                    builder.trueLiteral = TrueLiteral.newBuilder().build()
                }
                else {
                    builder.falseLiteral = FalseLiteral.newBuilder().build()
                }
            }

            override fun visitString(x: JsStringLiteral) {
                builder.stringLiteral = serialize(x.value)
            }

            override fun visitRegExp(x: JsRegExp) {
                val regExpBuilder = RegExpLiteral.newBuilder()
                regExpBuilder.patternStringId = serialize(x.pattern)
                x.flags?.let { regExpBuilder.flagsStringId = serialize(it) }
                builder.regExpLiteral = regExpBuilder.build()
            }

            override fun visitInt(x: JsNumberLiteral.JsIntLiteral) {
                builder.intLiteral = x.value
            }

            override fun visitDouble(x: JsNumberLiteral.JsDoubleLiteral) {
                builder.doubleLiteral = x.value
            }

            override fun visitArray(x: JsArrayLiteral) {
                val arrayBuilder = ArrayLiteral.newBuilder()
                x.expressions.forEach { arrayBuilder.addElement(serialize(it)) }
                builder.arrayLiteral = arrayBuilder.build()
            }

            override fun visitObjectLiteral(x: JsObjectLiteral) {
                val objectBuilder = ObjectLiteral.newBuilder()
                for (initializer in x.propertyInitializers) {
                    val entryBuilder = ObjectLiteralEntry.newBuilder()
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
                val commentBuilder = DocComment.newBuilder()
                for ((name, value) in comment.tags) {
                    val tagBuilder = DocCommentTag.newBuilder()
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
                val binaryBuilder = BinaryOperation.newBuilder()
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
                val conditionalBuilder = Conditional.newBuilder()
                conditionalBuilder.testExpression = serialize(x.testExpression)
                conditionalBuilder.thenExpression = serialize(x.thenExpression)
                conditionalBuilder.elseExpression = serialize(x.elseExpression)
                builder.conditional = conditionalBuilder.build()
            }

            override fun visitArrayAccess(x: JsArrayAccess) {
                val arrayAccessBuilder = ArrayAccess.newBuilder()
                arrayAccessBuilder.array = serialize(x.arrayExpression)
                arrayAccessBuilder.index = serialize(x.indexExpression)
                builder.arrayAccess = arrayAccessBuilder.build()
            }

            override fun visitNameRef(nameRef: JsNameRef) {
                val name = nameRef.name
                val qualifier = nameRef.qualifier
                if (name != null) {
                    if (qualifier != null || (nameRef.inlineStrategy?.isInline ?: false)) {
                        val nameRefBuilder = NameReference.newBuilder()
                        nameRefBuilder.nameId = serialize(name)
                        if (qualifier != null) {
                            nameRefBuilder.qualifier = serialize(qualifier)
                        }
                        nameRef.inlineStrategy?.let { nameRefBuilder.inlineStrategy = map(it) }
                        builder.nameReference = nameRefBuilder.build()
                    }
                    else {
                        builder.simpleNameReference = serialize(name)
                    }
                }
                else {
                    val propertyRefBuilder = PropertyReference.newBuilder()
                    propertyRefBuilder.stringId = serialize(nameRef.ident)
                    qualifier?.let { propertyRefBuilder.qualifier = serialize(it) }
                    nameRef.inlineStrategy?.let { propertyRefBuilder.inlineStrategy = map(it) }
                    builder.propertyReference = propertyRefBuilder.build()
                }
            }

            override fun visitInvocation(invocation: JsInvocation) {
                val invocationBuilder = Invocation.newBuilder()
                invocationBuilder.qualifier = serialize(invocation.qualifier)
                invocation.arguments.forEach { invocationBuilder.addArgument(serialize(it)) }
                invocation.inlineStrategy?.let { inlineStrategy ->
                    if (inlineStrategy != KotlinInlineStrategy.NOT_INLINE) {
                        invocationBuilder.inlineStrategy = map(inlineStrategy)
                    }
                }
                builder.invocation = invocationBuilder.build()
            }

            override fun visitNew(x: JsNew) {
                val instantiationBuilder = Instantiation.newBuilder()
                instantiationBuilder.qualifier = serialize(x.constructorExpression)
                x.arguments.forEach { instantiationBuilder.addArgument(serialize(it)) }
                builder.instantiation = instantiationBuilder.build()
            }
        }

        withLocation(expression, { visitor.builder.fileId = it }, {visitor.builder.location = it }) {
            expression.accept(visitor)
        }

        with (visitor.builder) {
            synthetic = expression.synthetic
            sideEffects = map(expression.sideEffects)
        }

        return visitor.builder.build()
    }

    private fun serializeParameter(parameter: JsParameter): Parameter {
        val parameterBuilder = Parameter.newBuilder()
        parameterBuilder.nameId = serialize(parameter.name)
        if (parameter.hasDefaultValue) {
            parameterBuilder.hasDefaultValue = true
        }
        return parameterBuilder.build()
    }

    private fun serializeBlock(block: JsGlobalBlock): GlobalBlock {
        val blockBuilder = GlobalBlock.newBuilder()
        for (part in block.statements) {
            blockBuilder.addStatement(serialize(part))
        }
        return blockBuilder.build()
    }

    private fun serializeVars(vars: JsVars): Vars {
        val varsBuilder = Vars.newBuilder()
        for (varDecl in vars.vars) {
            val declBuilder = VarDeclaration.newBuilder()
            declBuilder.nameId = serialize(varDecl.name)
            varDecl.initExpression?.let { declBuilder.initialValue = serialize(it) }
            varsBuilder.addDeclaration(declBuilder)
        }

        if (vars.isMultiline) {
            varsBuilder.multiline = true
        }
        vars.exportedPackage?.let { varsBuilder.exportedPackageId = serialize(it) }

        return varsBuilder.build()
    }

    private fun serializeUnary(x: JsUnaryOperation, postfix: Boolean): UnaryOperation {
        val unaryBuilder = UnaryOperation.newBuilder()
        unaryBuilder.operand = serialize(x.arg)
        unaryBuilder.type = map(x.operator)
        unaryBuilder.postfix = postfix
        return unaryBuilder.build()
    }

    private fun map(op: JsBinaryOperator) = when (op) {
        JsBinaryOperator.MUL -> MUL
        JsBinaryOperator.DIV -> DIV
        JsBinaryOperator.MOD -> MOD
        JsBinaryOperator.ADD -> ADD
        JsBinaryOperator.SUB -> SUB
        JsBinaryOperator.SHL -> SHL
        JsBinaryOperator.SHR -> SHR
        JsBinaryOperator.SHRU -> SHRU
        JsBinaryOperator.LT -> LT
        JsBinaryOperator.LTE -> LTE
        JsBinaryOperator.GT -> GT
        JsBinaryOperator.GTE -> GTE
        JsBinaryOperator.INSTANCEOF -> INSTANCEOF
        JsBinaryOperator.INOP -> IN
        JsBinaryOperator.EQ -> EQ
        JsBinaryOperator.NEQ -> NEQ
        JsBinaryOperator.REF_EQ -> REF_EQ
        JsBinaryOperator.REF_NEQ -> REF_NEQ
        JsBinaryOperator.BIT_AND -> BIT_AND
        JsBinaryOperator.BIT_XOR -> BIT_XOR
        JsBinaryOperator.BIT_OR -> BIT_OR
        JsBinaryOperator.AND -> AND
        JsBinaryOperator.OR -> OR
        JsBinaryOperator.ASG -> ASG
        JsBinaryOperator.ASG_ADD -> ASG_ADD
        JsBinaryOperator.ASG_SUB -> ASG_SUB
        JsBinaryOperator.ASG_MUL -> ASG_MUL
        JsBinaryOperator.ASG_DIV -> ASG_DIV
        JsBinaryOperator.ASG_MOD -> ASG_MOD
        JsBinaryOperator.ASG_SHL -> ASG_SHL
        JsBinaryOperator.ASG_SHR -> ASG_SHR
        JsBinaryOperator.ASG_SHRU -> ASG_SHRU
        JsBinaryOperator.ASG_BIT_AND -> ASG_BIT_AND
        JsBinaryOperator.ASG_BIT_OR -> ASG_BIT_OR
        JsBinaryOperator.ASG_BIT_XOR -> ASG_BIT_XOR
        JsBinaryOperator.COMMA -> COMMA
    }

    private fun map(op: JsUnaryOperator) = when (op) {
        JsUnaryOperator.BIT_NOT -> BIT_NOT
        JsUnaryOperator.DEC -> DEC
        JsUnaryOperator.DELETE -> DELETE
        JsUnaryOperator.INC -> INC
        JsUnaryOperator.NEG -> NEG
        JsUnaryOperator.POS -> POS
        JsUnaryOperator.NOT -> NOT
        JsUnaryOperator.TYPEOF -> TYPEOF
        JsUnaryOperator.VOID -> VOID
    }

    private fun map(sideEffects: SideEffectKind) = when (sideEffects) {
        SideEffectKind.AFFECTS_STATE -> SideEffects.AFFECTS_STATE
        SideEffectKind.DEPENDS_ON_STATE -> SideEffects.DEPENDS_ON_STATE
        SideEffectKind.PURE -> SideEffects.PURE
    }

    private fun map(inlineStrategy: KotlinInlineStrategy) = when (inlineStrategy) {
        KotlinInlineStrategy.AS_FUNCTION -> InlineStrategy.AS_FUNCTION
        KotlinInlineStrategy.IN_PLACE -> InlineStrategy.IN_PLACE
        KotlinInlineStrategy.NOT_INLINE -> InlineStrategy.NOT_INLINE
    }

    private fun serialize(name: JsName) = nameMap.getOrPut(name) {
        val result = nameTableBuilder.entryCount
        val builder = Name.newBuilder()
        builder.identifier = serialize(name.ident)
        builder.temporary = name.isTemporary
        nameTableBuilder.addEntry(builder)
        result
    }

    private fun serialize(string: String) = stringMap.getOrPut(string) {
        val result = stringTableBuilder.entryCount
        stringTableBuilder.addEntry(string)
        result
    }

    private inline fun withLocation(node: JsNode, fileConsumer: (Int) -> Unit, locationConsumer: (Location) -> Unit, inner: () -> Unit) {
        val location = extractLocation(node)
        var fileChanged = false
        if (location != null) {
            val lastFile = fileStack.peek()
            val newFile = location.file
            fileChanged = lastFile != newFile && newFile != null
            if (fileChanged) {
                fileConsumer(serialize(newFile!!))
                fileStack.push(location.file)
            }
            val locationBuilder = Location.newBuilder()
            locationBuilder.startLine = location.startLine
            locationBuilder.startChar = location.startChar
            locationConsumer(locationBuilder.build())
        }

        inner()

        if (fileChanged) {
            fileStack.pop()
        }
    }

    private fun extractLocation(node: JsNode): JsLocation? {
        val source = node.source
        return when (source) {
            is JsLocation -> source
            is PsiElement -> extractLocation(source)
            else -> null
        }
    }

    private fun extractLocation(element: PsiElement): JsLocation {
        val file = element.containingFile
        val document = file.viewProvider.document!!

        val path = file.viewProvider.virtualFile.path

        val startOffset = element.node.startOffset
        val startLine = document.getLineNumber(startOffset)
        val startChar = startOffset - document.getLineStartOffset(startLine)

        return JsLocation(path, startLine, startChar)
    }
}