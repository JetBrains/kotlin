/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import com.google.gwt.dev.js.ScopeContext
import com.intellij.util.containers.addIfNotNull
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.parser.antlr.JsAstMapper.Companion.createParserException
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptParser
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptParserBaseVisitor

class JsAstMapperVisitor(
    private val fileName: String,
    private val scopeContext: ScopeContext,
) : JavaScriptParserBaseVisitor<JsNode?>() {
    override fun visitSourceElement(ctx: JavaScriptParser.SourceElementContext): JsNode? {
        return visitNode<JsStatement?>(ctx.statement())
    }

    // ENTRY POINT
    override fun visitStatement(ctx: JavaScriptParser.StatementContext): JsStatement? {
        ctx.functionDeclaration()?.let {
            return visitNode<JsFunction>(it).makeStmt()
        }

        return super.visitStatement(ctx).expect<JsStatement?>()
    }

    override fun visitBlock(ctx: JavaScriptParser.BlockContext): JsBlock {
        return visitNode<JsBlock>(ctx.statementList())
    }

    override fun visitStatementList(ctx: JavaScriptParser.StatementListContext): JsBlock {
        return mapBlock(visitAll<JsStatement?>(ctx.statement()))
    }

    override fun visitImportStatement(ctx: JavaScriptParser.ImportStatementContext): JsNode? {
        TODO("Import statemements not supported yet")
    }

    override fun visitImportFromBlock(ctx: JavaScriptParser.ImportFromBlockContext): JsNode? {
        TODO("Import statemements not supported yet")
    }

    override fun visitImportModuleItems(ctx: JavaScriptParser.ImportModuleItemsContext): JsNode? {
        TODO("Import statemements not supported yet")
    }

    override fun visitImportAliasName(ctx: JavaScriptParser.ImportAliasNameContext): JsNode? {
        TODO("Import statemements not supported yet")
    }

    override fun visitModuleExportName(ctx: JavaScriptParser.ModuleExportNameContext): JsNode? {
        TODO("Import statemements not supported yet")
    }

    override fun visitImportedBinding(ctx: JavaScriptParser.ImportedBindingContext): JsNode? {
        TODO("Import statemements not supported yet")
    }

    override fun visitImportDefault(ctx: JavaScriptParser.ImportDefaultContext): JsNode? {
        TODO("Import statemements not supported yet")
    }

    override fun visitImportNamespace(ctx: JavaScriptParser.ImportNamespaceContext): JsNode? {
        TODO("Import statemements not supported yet")
    }

    override fun visitImportFrom(ctx: JavaScriptParser.ImportFromContext): JsNode? {
        TODO("Import statemements not supported yet")
    }

    override fun visitAliasName(ctx: JavaScriptParser.AliasNameContext): JsNode? {
        TODO("Import statemements not supported yet")
    }

    override fun visitExportDeclaration(ctx: JavaScriptParser.ExportDeclarationContext): JsNode? {
        TODO("Export statemements not supported yet")
    }

    override fun visitExportDefaultDeclaration(ctx: JavaScriptParser.ExportDefaultDeclarationContext): JsNode? {
        TODO("Export statemements not supported yet")
    }

    override fun visitExportFromBlock(ctx: JavaScriptParser.ExportFromBlockContext): JsNode? {
        TODO("Export statemements not supported yet")
    }

    override fun visitExportModuleItems(ctx: JavaScriptParser.ExportModuleItemsContext): JsNode? {
        TODO("Export statemements not supported yet")
    }

    override fun visitExportAliasName(ctx: JavaScriptParser.ExportAliasNameContext): JsNode? {
        TODO("Export statemements not supported yet")
    }

    override fun visitDeclaration(ctx: JavaScriptParser.DeclarationContext): JsNode? {
        TODO("Export statemements not supported yet")
    }

    override fun visitVariableStatement(ctx: JavaScriptParser.VariableStatementContext): JsVars {
        return visitNode<JsVars>(ctx.variableDeclarationList())
    }

    override fun visitVariableDeclarationList(ctx: JavaScriptParser.VariableDeclarationListContext): JsVars {
        return JsVars().apply {
            ctx.variableDeclaration().forEach {
                add(visitNode<JsVars.JsVar>(it))
            }
        }
    }

    override fun visitSingleVariableDeclaration(ctx: JavaScriptParser.SingleVariableDeclarationContext): JsVars.JsVar {
        return visitNode<JsVars.JsVar>(ctx.variableDeclaration())
    }

    override fun visitVariableDeclaration(ctx: JavaScriptParser.VariableDeclarationContext): JsVars.JsVar {
        val originalId = ctx.assignable().identifier()?.text
            ?: TODO("Only identifier parameters are supported yet")
        val id = scopeContext.localNameFor(originalId)
        val initialization = ctx.singleExpression()?.let { visitNode<JsExpression>(it) }

        return JsVars.JsVar(id, initialization).applyLocation(fileName, ctx)
    }

    override fun visitEmptyStatement_(ctx: JavaScriptParser.EmptyStatement_Context): JsStatement? {
        return null
    }

    override fun visitExpressionStatement(ctx: JavaScriptParser.ExpressionStatementContext): JsStatement? {
        return visitNode<JsExpression>(ctx.expressionSequence()).makeStmt()
    }

    override fun visitIfStatement(ctx: JavaScriptParser.IfStatementContext): JsIf {
        val ifCondition = visitNode<JsExpression>(ctx.expressionSequence())
        // Empty statements are not supported in both 'if' branches, so always expect non-nullable statements.
        val allStatements = visitAll<JsStatement>(ctx.statement())

        return JsIf(
            ifCondition,
            allStatements[0],
            allStatements.getOrNull(1)
        )
    }

    override fun visitDoStatement(ctx: JavaScriptParser.DoStatementContext): JsDoWhile {
        val body = visitNode<JsStatement?>(ctx.statement()) ?: JsEmpty
        val condition = visitNode<JsExpression>(ctx.expressionSequence())

        return JsDoWhile(condition, body)
    }

    override fun visitWhileStatement(ctx: JavaScriptParser.WhileStatementContext): JsWhile {
        val condition = visitNode<JsExpression>(ctx.expressionSequence())
        val body = visitNode<JsStatement?>(ctx.statement()) ?: JsEmpty

        return JsWhile(condition, body)
    }

    override fun visitForStatement(ctx: JavaScriptParser.ForStatementContext): JsFor {
        val initSequence = ctx.expressionSequence(0)?.let { visitNode<JsExpression>(it) }
        val initDeclaration = ctx.variableDeclarationList()?.let { visitNode<JsVars>(it) }

        val condition = ctx.expressionSequence(1)?.let { visitNode<JsExpression>(it) }
        val increment = ctx.expressionSequence(2)?.let { visitNode<JsExpression>(it) }
        val body = visitNode<JsStatement?>(ctx.statement()) ?: JsEmpty

        return when {
            initSequence != null -> JsFor(initSequence, condition, increment, body)
            initDeclaration != null -> JsFor(initDeclaration, condition, increment, body)
            else -> TODO("Invalid 'for' statement: ${ctx.text}")
        }
    }

    override fun visitForInStatement(ctx: JavaScriptParser.ForInStatementContext): JsNode? {
        val unnamedExpression = ctx.singleExpression()?.let { visitNode<JsExpression>(it) }
        val namedDeclaration = ctx.singleVariableDeclaration()?.let { visitNode<JsVars.JsVar>(it) }
        val inTargetExpression = ctx.expressionSequence()?.let { visitNode<JsExpression>(it) }

        val bodyStatement = visitNode<JsStatement?>(ctx.statement()) ?: JsEmpty

        return when {
            unnamedExpression != null -> JsForIn().apply {
                iterExpression = unnamedExpression
                objectExpression = inTargetExpression
                body = bodyStatement
            }
            namedDeclaration != null -> JsForIn(namedDeclaration.name).apply {
                iterExpression = namedDeclaration.initExpression
                body = bodyStatement
            }
            else -> TODO("Invalid 'for .. in' statement: ${ctx.text}")
        }
    }

    override fun visitForOfStatement(ctx: JavaScriptParser.ForOfStatementContext): JsNode? {
        TODO("'for .. of' is not supported yet")
    }

    override fun visitVarModifier(ctx: JavaScriptParser.VarModifierContext): JsNode? {
        // There is no JS node that represents 'var' modifier.
        return null
    }

    override fun visitContinueStatement(ctx: JavaScriptParser.ContinueStatementContext): JsContinue {
        return JsContinue(getTargetLabel(ctx))
    }

    override fun visitBreakStatement(ctx: JavaScriptParser.BreakStatementContext): JsBreak {
        return JsBreak(getTargetLabel(ctx))
    }

    override fun visitReturnStatement(ctx: JavaScriptParser.ReturnStatementContext): JsReturn {
        return JsReturn().apply {
            ctx.expressionSequence()?.let {
                expression = visitNode<JsExpression>(it)
            }
        }
    }

    override fun visitYieldStatement(ctx: JavaScriptParser.YieldStatementContext): JsNode? {
        TODO("yield statement is not supported yet")
    }

    override fun visitWithStatement(ctx: JavaScriptParser.WithStatementContext): JsNode? {
        // The "with" statement is unsupported because it introduces ambiguity
        // related to whether or not a name is obfuscatable that we cannot resolve
        // statically. This is modified in our copy of the Rhino Parser to provide
        // detailed source & line info. So, this method should never actually be
        // called.
        //
        throw createParserException(
            "Internal error: unexpected token 'with'",
            ctx
        )
    }

    override fun visitSwitchStatement(ctx: JavaScriptParser.SwitchStatementContext): JsSwitch {
        val jsSwitchExpr = visitNode<JsExpression>(ctx.expressionSequence())

        val jsCases = ctx.caseBlock().caseClauses()?.let { visitAll<JsDefault>(it) } ?: emptyList()
        val jsDefault = ctx.caseBlock().defaultClause()?.let { visitNode<JsDefault>(it) }

        return JsSwitch().apply {
            expression = jsSwitchExpr
            cases.addAll(jsCases)
            cases.addIfNotNull(jsDefault)
        }
    }

    override fun visitCaseBlock(ctx: JavaScriptParser.CaseBlockContext): JsNode? {
        // JS AST doesn't have a node representing switch body.
        return null
    }

    override fun visitCaseClauses(ctx: JavaScriptParser.CaseClausesContext): JsNode? {
        // JS AST doesn't have a node representing case clauses aggregate.
        return null
    }

    override fun visitCaseClause(ctx: JavaScriptParser.CaseClauseContext): JsCase {
        val jsExpression = ctx.expressionSequence()?.let { visitNode<JsExpression>(it) }
        val jsStatements = ctx.statementList()?.let { visitNode<JsStatement?>(it) }

        return JsCase().apply {
            caseExpression = jsExpression
            statements.addAll(listOfNotNull(jsStatements))
        }
    }

    override fun visitDefaultClause(ctx: JavaScriptParser.DefaultClauseContext): JsDefault {
        val jsStatements = ctx.statementList()?.let { visitNode<JsStatement?>(it) }

        return JsDefault().apply {
            statements.addAll(listOfNotNull(jsStatements))
        }
    }

    override fun visitLabelledStatement(ctx: JavaScriptParser.LabelledStatementContext): JsLabel {
        val jsLabelIdentifier = ctx.identifier().text
        val jsName = scopeContext.enterLabel(jsLabelIdentifier, jsLabelIdentifier)
        val jsLabel = JsLabel(jsName).apply {
            statement = visitNode<JsStatement>(ctx.statement())
        }
        scopeContext.exitLabel()

        return jsLabel
    }

    override fun visitThrowStatement(ctx: JavaScriptParser.ThrowStatementContext): JsNode? {
        val jsThrowExpr = visitNode<JsExpression>(ctx.expressionSequence())

        return JsThrow(jsThrowExpr)
    }

    override fun visitTryStatement(ctx: JavaScriptParser.TryStatementContext): JsTry {
        return JsTry().apply {
            tryBlock = visitNode<JsBlock>(ctx.block())

            val jsCatchProduction = ctx.catchProduction()?.let { visitNode<JsCatch>(it) }
            if (jsCatchProduction != null) {
                catches.add(jsCatchProduction)
            }

            val jsFinallyProduction = ctx.finallyProduction()?.let { visitNode<JsBlock>(it) }
            if (jsFinallyProduction != null) {
                finallyBlock = jsFinallyProduction
            }
        }
    }

    override fun visitCatchProduction(ctx: JavaScriptParser.CatchProductionContext): JsCatch {
        val jsCatchIdentifier = ctx.assignable().identifier()?.text
            ?: TODO("Only identifier catch variables are supported yet")

        return scopeContext.enterCatch(jsCatchIdentifier).apply {
            body = visitNode<JsBlock>(ctx.block())
            // TODO: Decide what to do with "catch conditions":
            //   https://lia.disi.unibo.it/materiale/JS/developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/try...html#Conditional_catch_clauses
            condition = null
            scopeContext.exitCatch()
        }
    }

    override fun visitFinallyProduction(ctx: JavaScriptParser.FinallyProductionContext): JsBlock {
        return visitNode<JsBlock>(ctx.block())
    }

    override fun visitDebuggerStatement(ctx: JavaScriptParser.DebuggerStatementContext): JsDebugger {
        return JsDebugger()
    }

    override fun visitFunctionDeclaration(ctx: JavaScriptParser.FunctionDeclarationContext): JsFunction {
        val name = ctx.identifier()
        val isGenerator = ctx.Multiply() != null
        val paramList = ctx.formalParameterList()
        assert(paramList.restParameterArg() == null) { "Rest parameters are not supported yet" }

        return mapFunction(name?.text, ctx.functionBody(), paramList.formalParameterArg(), isGenerator)
    }

    override fun visitClassDeclaration(ctx: JavaScriptParser.ClassDeclarationContext): JsNode? {
        TODO("Classes are not supported yet")
    }

    override fun visitClassTail(ctx: JavaScriptParser.ClassTailContext): JsNode? {
        TODO("Classes are not supported yet")
    }

    override fun visitClassElement(ctx: JavaScriptParser.ClassElementContext): JsNode? {
        TODO("Classes are not supported yet")
    }

    override fun visitMethodDefinition(ctx: JavaScriptParser.MethodDefinitionContext): JsNode? {
        TODO("Classes are not supported yet")
    }

    override fun visitFieldDefinition(ctx: JavaScriptParser.FieldDefinitionContext): JsNode? {
        TODO("Classes are not supported yet")
    }

    override fun visitClassElementName(ctx: JavaScriptParser.ClassElementNameContext): JsNode? {
        TODO("Classes are not supported yet")
    }

    override fun visitPrivateIdentifier(ctx: JavaScriptParser.PrivateIdentifierContext): JsNode? {
        TODO("Private fields are not supported yet")
    }

    override fun visitFormalParameterList(ctx: JavaScriptParser.FormalParameterListContext): JsNode? {
        // JS AST doesn't have a node representing a formal parameter list.
        return null
    }

    override fun visitFormalParameterArg(ctx: JavaScriptParser.FormalParameterArgContext): JsParameter {
        val identifier = ctx.assignable().identifier()
            ?: TODO("Only identifier parameters are supported yet")
        val paramName = scopeContext.localNameFor(identifier.text)

        return JsParameter(paramName)
    }

    override fun visitRestParameterArg(ctx: JavaScriptParser.RestParameterArgContext?): JsNode? {
        TODO("Rest parameters are not supported yet")
    }

    override fun visitFunctionBody(ctx: JavaScriptParser.FunctionBodyContext): JsBlock {
        return visitNode<JsBlock>(ctx.sourceElements())
    }

    override fun visitSourceElements(ctx: JavaScriptParser.SourceElementsContext): JsBlock {
        val statements = visitAll<JsStatement?>(ctx.sourceElement())
        return mapBlock(statements)
    }

    override fun visitArrayLiteral(ctx: JavaScriptParser.ArrayLiteralContext): JsArrayLiteral {
        return JsArrayLiteral().apply {
            expressions.addAll(visitAll<JsExpression>(ctx.elementList().arrayElement()))
        }
    }

    override fun visitElementList(ctx: JavaScriptParser.ElementListContext): JsNode? {
        // JS AST doesn't have a node representing an array elements list.
        return null
    }

    override fun visitArrayElement(ctx: JavaScriptParser.ArrayElementContext): JsExpression {
        assert(ctx.Ellipsis() == null) { "Spread operator is not supported yet" }

        return visitNode<JsExpression>(ctx.singleExpression())
    }

    override fun visitPropertyExpressionAssignment(ctx: JavaScriptParser.PropertyExpressionAssignmentContext): JsPropertyInitializer {
        val jsLabelExpr = visitNode<JsExpression>(ctx.propertyName())
        val jsValue = visitNode<JsExpression>(ctx.singleExpression())

        return JsPropertyInitializer(jsLabelExpr, jsValue)
    }

    override fun visitComputedPropertyExpressionAssignment(ctx: JavaScriptParser.ComputedPropertyExpressionAssignmentContext): JsNode? {
        TODO("Computed property names are not supported yet")
    }

    override fun visitFunctionProperty(ctx: JavaScriptParser.FunctionPropertyContext): JsNode? {
        TODO("Function properties are not supported yet")
    }

    override fun visitPropertyGetter(ctx: JavaScriptParser.PropertyGetterContext): JsNode? {
        TODO("Property getters are not supported yet")
    }

    override fun visitPropertySetter(ctx: JavaScriptParser.PropertySetterContext): JsNode? {
        TODO("Property setters are not supported yet")
    }

    override fun visitPropertyShorthand(ctx: JavaScriptParser.PropertyShorthandContext): JsNode? {
        TODO("Property shorthands are not supported yet")
    }

    override fun visitPropertyName(ctx: JavaScriptParser.PropertyNameContext): JsExpression {
        ctx.identifierName()?.let {
            return JsStringLiteral(it.text)
        }

        ctx.StringLiteral()?.let {
            return it.toStringLiteral()
        }

        ctx.numericLiteral()?.let {
            return visitNode<JsNumberLiteral>(it)
        }

        ctx.singleExpression()?.let {
            TODO("Computed property names are not supported yet")
        }

        TODO("Invalid property name: ${ctx.text}")
    }

    override fun visitArguments(ctx: JavaScriptParser.ArgumentsContext): JsNode? {
        // JS AST doesn't have a node representing an arguments list.'
        return null
    }

    override fun visitArgument(ctx: JavaScriptParser.ArgumentContext): JsExpression {
        assert(ctx.Ellipsis() == null) { "Spread operator is not supported yet" }

        ctx.singleExpression()?.let {
            return visitNode<JsExpression>(it)
        }

        ctx.identifier()?.let {
            return makeRefNode(it.text)
        }

        TODO("Invalid argument: ${ctx.text}")
    }

    override fun visitExpressionSequence(ctx: JavaScriptParser.ExpressionSequenceContext): JsExpression {
        if (ctx.singleExpression().size == 1) {
            return visitNode<JsExpression>(ctx.singleExpression()[0])
        }

        val exprs = visitAll<JsExpression>(ctx.singleExpression())
        return mapComma(exprs)
    }

    override fun visitSingleExpression(ctx: JavaScriptParser.SingleExpressionContext): JsExpression {
        return super.visit(ctx.singleExpressionImpl()).expect<JsExpression>()
    }

    override fun visitTemplateStringExpression(ctx: JavaScriptParser.TemplateStringExpressionContext): JsNode? {
        TODO("Template string literals are not supported yet")
    }

    override fun visitTernaryExpression(ctx: JavaScriptParser.TernaryExpressionContext): JsConditional {
        val conditionExpression = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val thenExpression = visitNode<JsExpression>(ctx.singleExpressionImpl(1))
        val elseCondition = visitNode<JsExpression>(ctx.singleExpressionImpl(2))

        return JsConditional(conditionExpression, thenExpression, elseCondition)
    }

    override fun visitLogicalAndExpression(ctx: JavaScriptParser.LogicalAndExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.AND, left, right)
    }

    override fun visitPowerExpression(ctx: JavaScriptParser.PowerExpressionContext): JsNode? {
        TODO("Power expressions are not supported yet")
    }

    override fun visitPreIncrementExpression(ctx: JavaScriptParser.PreIncrementExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.INC, expression)
    }

    override fun visitObjectLiteralExpression(ctx: JavaScriptParser.ObjectLiteralExpressionContext): JsObjectLiteral {
        return visitNode<JsObjectLiteral>(ctx.objectLiteral())
    }

    override fun visitMetaExpression(ctx: JavaScriptParser.MetaExpressionContext): JsNode? {
        TODO("Meta expressions are not supported yet")
    }

    override fun visitInExpression(ctx: JavaScriptParser.InExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.INOP, left, right)
    }

    override fun visitLogicalOrExpression(ctx: JavaScriptParser.LogicalOrExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.OR, left, right)
    }

    override fun visitOptionalChainExpression(ctx: JavaScriptParser.OptionalChainExpressionContext): JsNode? {
        TODO("Optional chain expressions are not supported yet")
    }

    override fun visitNotExpression(ctx: JavaScriptParser.NotExpressionContext): JsNode? {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.NOT, expression)
    }

    override fun visitPreDecreaseExpression(ctx: JavaScriptParser.PreDecreaseExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.DEC, expression)
    }

    override fun visitArgumentsExpression(ctx: JavaScriptParser.ArgumentsExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAwaitExpression(ctx: JavaScriptParser.AwaitExpressionContext): JsNode? {
        TODO("async/await statements are not supported yet")
    }

    override fun visitThisExpression(ctx: JavaScriptParser.ThisExpressionContext): JsThisRef {
        return JsThisRef()
    }

    override fun visitFunctionExpression(ctx: JavaScriptParser.FunctionExpressionContext): JsNode? {
        return super.visitFunctionExpression(ctx)
    }

    override fun visitUnaryMinusExpression(ctx: JavaScriptParser.UnaryMinusExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.NEG, expression)
    }

    override fun visitAssignmentExpression(ctx: JavaScriptParser.AssignmentExpressionContext): JsNode? {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.ASG, left, right)
    }

    override fun visitPostDecreaseExpression(ctx: JavaScriptParser.PostDecreaseExpressionContext): JsPostfixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPostfixOperation(JsUnaryOperator.DEC, expression)
    }

    override fun visitTypeofExpression(ctx: JavaScriptParser.TypeofExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.TYPEOF, expression)
    }

    override fun visitInstanceofExpression(ctx: JavaScriptParser.InstanceofExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.INSTANCEOF, left, right)
    }

    override fun visitUnaryPlusExpression(ctx: JavaScriptParser.UnaryPlusExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.POS, expression)
    }

    override fun visitDeleteExpression(ctx: JavaScriptParser.DeleteExpressionContext): JsExpression {
        val target = visitNode<JsExpression>(ctx.singleExpressionImpl())
        if (target is JsNameRef || target is JsArrayAccess)
            return JsPrefixOperation(JsUnaryOperator.DELETE, target)
        return JsNullLiteral()
    }

    override fun visitImportExpression(ctx: JavaScriptParser.ImportExpressionContext): JsNode? {
        TODO("Import expressions are not supported yet")
    }

    override fun visitEqualityExpression(ctx: JavaScriptParser.EqualityExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return ctx.run {
            val operator = when {
                Equals_() != null -> JsBinaryOperator.EQ
                NotEquals() != null -> JsBinaryOperator.NEQ
                IdentityEquals() != null -> JsBinaryOperator.REF_EQ
                IdentityNotEquals() != null -> JsBinaryOperator.REF_NEQ
                else -> TODO("Invalid binary operation: ${ctx.text}")
            }

            JsBinaryOperation(operator, left, right)
        }
    }

    override fun visitBitXOrExpression(ctx: JavaScriptParser.BitXOrExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.BIT_XOR, left, right)
    }

    override fun visitSuperExpression(ctx: JavaScriptParser.SuperExpressionContext): JsNode? {
        TODO("Super calls are not supported yet")
    }

    override fun visitMultiplicativeExpression(ctx: JavaScriptParser.MultiplicativeExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return ctx.run {
            val operator = when {
                Multiply() != null -> JsBinaryOperator.MUL
                Divide() != null -> JsBinaryOperator.DIV
                Modulus() != null -> JsBinaryOperator.MOD
                else -> TODO("Invalid binary operation: ${ctx.text}")
            }
            JsBinaryOperation(operator, left, right)
        }
    }

    override fun visitBitShiftExpression(ctx: JavaScriptParser.BitShiftExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return ctx.run {
            val operator = when {
                RightShiftArithmetic() != null -> JsBinaryOperator.SHR
                LeftShiftArithmetic() != null -> JsBinaryOperator.SHL
                RightShiftLogical() != null -> JsBinaryOperator.SHRU
                else -> TODO("Invalid binary operation: ${ctx.text}")
            }
            JsBinaryOperation(operator, left, right)
        }
    }

    override fun visitParenthesizedExpression(ctx: JavaScriptParser.ParenthesizedExpressionContext): JsExpression {
        return visitNode<JsExpression>(ctx.expressionSequence())
    }

    override fun visitAdditiveExpression(ctx: JavaScriptParser.AdditiveExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return ctx.run {
            val operator = when {
                Plus() != null -> JsBinaryOperator.ADD
                Minus() != null -> JsBinaryOperator.SUB
                else -> TODO("Invalid binary operation: ${ctx.text}")
            }
            JsBinaryOperation(operator, left, right)
        }
    }

    override fun visitRelationalExpression(ctx: JavaScriptParser.RelationalExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return ctx.run {
            val operator = when {
                LessThan() != null -> JsBinaryOperator.LT
                MoreThan() != null -> JsBinaryOperator.GT
                LessThanEquals() != null -> JsBinaryOperator.LTE
                GreaterThanEquals() != null -> JsBinaryOperator.GTE
                else -> TODO("Invalid binary operation: ${ctx.text}")
            }
            JsBinaryOperation(operator, left, right)
        }
    }

    override fun visitPostIncrementExpression(ctx: JavaScriptParser.PostIncrementExpressionContext): JsPostfixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPostfixOperation(JsUnaryOperator.INC, expression)
    }

    override fun visitYieldExpression(ctx: JavaScriptParser.YieldExpressionContext): JsNode? {
        TODO("Yield expressions are not supported yet")
    }

    override fun visitBitNotExpression(ctx: JavaScriptParser.BitNotExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.BIT_NOT, expression)
    }

    override fun visitNewExpression(ctx: JavaScriptParser.NewExpressionContext): JsNew {
        val jsNewPlainIdentifier = ctx.identifier()?.let { makeRefNode(it.text) }
        val jsNewSingleExpression = ctx.singleExpressionImpl()?.let { visitNode<JsExpression>(it) }
        val jsNewExpression = when {
            jsNewPlainIdentifier != null -> jsNewPlainIdentifier
            else -> jsNewSingleExpression
        }

        val jsArguments = visitAll<JsExpression>(ctx.arguments().argument())
        return JsNew(jsNewExpression).apply {
            arguments.addAll(jsArguments)
        }
    }

    override fun visitLiteralExpression(ctx: JavaScriptParser.LiteralExpressionContext): JsLiteral {
        return visitNode<JsLiteral>(ctx.literal())
    }

    override fun visitArrayLiteralExpression(ctx: JavaScriptParser.ArrayLiteralExpressionContext): JsArrayLiteral {
        return visitNode<JsArrayLiteral>(ctx.arrayLiteral())
    }

    override fun visitMemberDotExpression(ctx: JavaScriptParser.MemberDotExpressionContext): JsNode? {
        assert(ctx.QuestionMark() == null) { "Optional chain expressions are not supported yet" }
        assert(ctx.Hashtag() == null) { "Private member access expressions are not supported yet" }

        val jsLeft = visitNode<JsExpression>(ctx.singleExpressionImpl())
        val jsRight = scopeContext.referenceFor(ctx.identifierName().text)

        return jsRight.apply {
            qualifier = jsLeft
        }
    }

    override fun visitClassExpression(ctx: JavaScriptParser.ClassExpressionContext): JsNode? {
        TODO("Classes are not supported yet")
    }

    override fun visitMemberIndexExpression(ctx: JavaScriptParser.MemberIndexExpressionContext): JsArrayAccess {
        assert(ctx.QuestionMarkDot() == null) { "Optional chain expressions are not supported yet" }
        val jsObjectExpr = visitNode<JsExpression>(ctx.singleExpressionImpl())
        val jsMemberExpr = visitNode<JsExpression>(ctx.expressionSequence())

        return JsArrayAccess(jsObjectExpr, jsMemberExpr)
    }

    override fun visitIdentifierExpression(ctx: JavaScriptParser.IdentifierExpressionContext): JsNameRef {
        return visitNode<JsNameRef>(ctx.identifier())
    }

    override fun visitBitAndExpression(ctx: JavaScriptParser.BitAndExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.BIT_AND, left, right)
    }

    override fun visitBitOrExpression(ctx: JavaScriptParser.BitOrExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.BIT_OR, left, right)
    }

    override fun visitAssignmentOperatorExpression(ctx: JavaScriptParser.AssignmentOperatorExpressionContext): JsNode? {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return ctx.assignmentOperator().run {
            val jsOperator = when {
                MultiplyAssign() != null -> JsBinaryOperator.ASG_MUL
                DivideAssign() != null -> JsBinaryOperator.ASG_DIV
                ModulusAssign() != null -> JsBinaryOperator.ASG_MOD
                PlusAssign() != null -> JsBinaryOperator.ASG_ADD
                MinusAssign() != null -> JsBinaryOperator.ASG_SUB
                LeftShiftArithmeticAssign() != null -> JsBinaryOperator.ASG_SHL
                RightShiftArithmeticAssign() != null -> JsBinaryOperator.ASG_SHR
                RightShiftLogicalAssign() != null -> JsBinaryOperator.ASG_SHRU
                BitAndAssign() != null -> JsBinaryOperator.ASG_BIT_AND
                BitXorAssign() != null -> JsBinaryOperator.ASG_BIT_XOR
                BitOrAssign() != null -> JsBinaryOperator.ASG_BIT_OR
                PowerAssign() != null -> TODO("Power assignment expressions are not supported yet")
                NullishCoalescingAssign() != null -> TODO("Null-coalescing assignment expressions are not supported yet")
                else -> TODO("Invalid binary operation: ${ctx.text}")
            }

            JsBinaryOperation(jsOperator, left, right)
        }
    }

    override fun visitVoidExpression(ctx: JavaScriptParser.VoidExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.VOID, expression)
    }

    override fun visitCoalesceExpression(ctx: JavaScriptParser.CoalesceExpressionContext): JsNode? {
        TODO("Null-coalescing expressions are not supported yet")
    }

    override fun visitInitializer(ctx: JavaScriptParser.InitializerContext): JsNode? {
        TODO("Classes are not supported yet")
    }

    override fun visitAssignable(ctx: JavaScriptParser.AssignableContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitObjectLiteral(ctx: JavaScriptParser.ObjectLiteralContext): JsObjectLiteral {
        return JsObjectLiteral().apply {
            visitAll<JsPropertyInitializer>(ctx.propertyAssignment()).forEach {
                propertyInitializers.add(it)
            }
        }
    }

    override fun visitNamedFunction(ctx: JavaScriptParser.NamedFunctionContext): JsFunction {
        val declaration = ctx.functionDeclaration()
        val name = declaration.identifier()
        assert(declaration.Async() == null) { "Async functions are not supported yet"}
        val isGenerator = declaration.Multiply() != null

        return mapFunction(name?.text, declaration.functionBody(), declaration.formalParameterList().formalParameterArg(), isGenerator)
    }

    override fun visitAnonymousFunctionDecl(ctx: JavaScriptParser.AnonymousFunctionDeclContext): JsFunction {
        val isGenerator = ctx.Multiply() != null
        val paramList = ctx.formalParameterList()
        assert(paramList.restParameterArg() == null) { "Rest parameters are not supported yet" }

        return mapFunction(null, ctx.functionBody(), paramList.formalParameterArg(), isGenerator)
    }

    override fun visitArrowFunction(ctx: JavaScriptParser.ArrowFunctionContext): JsFunction {
        assert(ctx.Async() == null) { "Async arrow functions are not supported yet"}
        val parameters = ctx.arrowFunctionParameters()

        return mapFunction(null, ctx.arrowFunctionBody().functionBody(), parameters.formalParameterList().formalParameterArg(), false)
    }

    override fun visitArrowFunctionParameters(ctx: JavaScriptParser.ArrowFunctionParametersContext): JsNode? {
        // JS AST doesn't have specific nodes for arrow function parameters
        return null
    }

    override fun visitArrowFunctionBody(ctx: JavaScriptParser.ArrowFunctionBodyContext): JsBlock {
        ctx.functionBody()?.let { body ->
            return visitNode<JsBlock>(body)
        }

        ctx.singleExpression()?.let { expr ->
            // TODO[seclerp]
            //  The current JS function node implementation doesn't support lambda expressions, so we will transform it
            //  into anonymous function expression. Example: `() => 123` becomes `function () { return 123 }` in generated code.
            //  This is a temporary approach.
            val returnNode = JsReturn(visitNode<JsExpression>(expr))
            return JsBlock(returnNode)
        }

        TODO("Invalid arrow function body: ${ctx.text}")
    }

    override fun visitAssignmentOperator(ctx: JavaScriptParser.AssignmentOperatorContext): JsNode? {
        // JS AST doesn't have specific nodes for assignment operators
        return null
    }

    override fun visitLiteral(ctx: JavaScriptParser.LiteralContext): JsLiteral {
        ctx.NullLiteral()?.run {
            return JsNullLiteral()
        }

        ctx.BooleanLiteral()?.let { bool ->
            return when (bool.text) {
                "true" -> JsBooleanLiteral(true)
                "false" -> JsBooleanLiteral(false)
                else -> TODO("Invalid boolean literal: ${bool.text}")
            }
        }

        ctx.StringLiteral()?.run {
            return JsStringLiteral(text)
        }

        ctx.RegularExpressionLiteral()?.run {
            // TODO[seclerp]: Improve grammar to have pattern and flags as a separate terminals in regex rule to remove splitting here
            return JsRegExp().apply {
                // We perform reversing here to always split on the latest / and prevent reversing on escaped /
                val parts = text
                    .removePrefix("/")
                    .reversed()
                    .split("/", limit = 2)
                    .reversed()
                pattern = parts[0]
                parts.getOrNull(1)?.let {
                    flags = it
                }
            }
        }

        return super.visitLiteral(ctx) as JsLiteral
    }

    override fun visitTemplateStringLiteral(ctx: JavaScriptParser.TemplateStringLiteralContext): JsNode? {
        TODO("Template string literals are not supported yet")
    }

    override fun visitTemplateStringAtom(ctx: JavaScriptParser.TemplateStringAtomContext): JsNode? {
        TODO("Template string literals are not supported yet")
    }

    override fun visitNumericLiteral(ctx: JavaScriptParser.NumericLiteralContext): JsNumberLiteral {
        ctx.BinaryIntegerLiteral()?.run {
            TODO("Binary integer literals are not supported yet")
        }

        ctx.OctalIntegerLiteral()?.run {
            TODO("Octal integer literals are not supported yet")
        }

        ctx.OctalIntegerLiteral2()?.run {
            TODO("Octal integer literals are not supported yet")
        }

        ctx.DecimalLiteral()?.let { decimalTerminal ->
            return decimalTerminal.toDecimalLiteral()
        }

        ctx.HexIntegerLiteral()?.let { hexTerminal ->
            return hexTerminal.toDecimalLiteral()
        }

        TODO("Invalid numeric literal '${ctx.text}'")
    }

    override fun visitBigintLiteral(ctx: JavaScriptParser.BigintLiteralContext): JsNode? {
        TODO("Big integers are not supported yet")
    }

    override fun visitGetter(ctx: JavaScriptParser.GetterContext): JsNode? {
        TODO("Property getters are not supported yet")
    }

    override fun visitSetter(ctx: JavaScriptParser.SetterContext): JsNode? {
        TODO("Property setters are not supported yet")
    }

    override fun visitIdentifierName(ctx: JavaScriptParser.IdentifierNameContext): JsNode? {
        // There is no JS node that represents identifier name.
        return null
    }

    override fun visitIdentifier(ctx: JavaScriptParser.IdentifierContext): JsNameRef {
        return makeRefNode(ctx.text)
    }

    override fun visitReservedWord(ctx: JavaScriptParser.ReservedWordContext): JsNode? {
        // There is no JS node that represents reserved word.
        return null
    }

    override fun visitKeyword(ctx: JavaScriptParser.KeywordContext): JsNode? {
        // There is no JS node that represents keyword.
        return null
    }

    override fun visitLet_(ctx: JavaScriptParser.Let_Context): JsNode? {
        TODO("Let assignments are not supported yet")
    }

    override fun visitEos(ctx: JavaScriptParser.EosContext): JsNode? {
        return super.visit(ctx)
    }

    private fun mapComma(sequence: List<JsExpression>): JsBinaryOperation {
        fun reduce(i: Int, expressions: List<JsExpression>): JsBinaryOperation {
            if (i == expressions.size - 2) {
                val left = expressions[i]
                val right = expressions[i + 1]
                return JsBinaryOperation(JsBinaryOperator.COMMA, left, right)
            }

            return JsBinaryOperation(
                JsBinaryOperator.COMMA,
                expressions[i],
                reduce(i + 1, expressions)
            )
        }

        if (sequence.size < 2)
            TODO("Sequence should contain at least 2 expressions to be used in comma mapping")

        return reduce(0, sequence)
    }

    private fun mapBlock(statements: List<JsStatement?>): JsBlock {
        val block = JsBlock()
        statements
            // visitStatement can return null in some cases, like an empty statement (';') maps to nothing.
            // maybe we need to consider it including into resulting AST as it may be useful for debugging and stepping.
            .filterNotNull()
            .forEach { block.statements.add(it) }

        return block
    }

    private fun mapFunction(
        functionName: String?,
        functionBody: JavaScriptParser.FunctionBodyContext,
        params: List<JavaScriptParser.FormalParameterArgContext>,
        isGenerator: Boolean,
    ): JsFunction {
        return scopeContext.enterFunction().apply {
            name = when {
                functionName.isNullOrEmpty() -> null
                else -> scopeContext.localNameFor(functionName)
            }

            if (isGenerator)
                modifiers.add(JsFunction.Modifier.GENERATOR)

            params.forEach {
                val jsParam = visitNode<JsParameter>(it)
                parameters.add(jsParam.applyLocation(fileName, it))
            }

            body = visitNode<JsBlock>(functionBody)

            scopeContext.exitFunction()
        }
    }

    private fun getTargetLabel(statementWithLabel: ParserRuleContext): JsNameRef? {
        val identifier = when {
            statementWithLabel is JavaScriptParser.ContinueStatementContext ->
                statementWithLabel.identifier()
            statementWithLabel is JavaScriptParser.BreakStatementContext ->
                statementWithLabel.identifier()
            else -> TODO("Unexpected node type: ${statementWithLabel.javaClass.name}")
        }

        val labelName = scopeContext.localNameFor(identifier.text)
        return labelName.makeRef()
    }

    private fun makeRefNode(identifier: String): JsNameRef {
        return scopeContext.referenceFor(identifier)
    }

    private fun unwrapStringLiteral(literal: TerminalNode): String {
        if (literal.text.startsWith("'") && literal.text.endsWith("'"))
            return literal.text.removeSurrounding("'")

        if (literal.text.startsWith("\"") && literal.text.endsWith("\""))
            return literal.text.removeSurrounding("\"")

        return literal.text
    }

    private fun TerminalNode.toStringLiteral(): JsStringLiteral {
        return JsStringLiteral(unwrapStringLiteral(this))
    }

    private fun TerminalNode.toDecimalLiteral(): JsNumberLiteral {
        val intValue = text.toIntOrNull()
        if (intValue != null)
            return JsIntLiteral(intValue)

        return JsDoubleLiteral(text.toDouble())
    }

    private fun TerminalNode.toHexLiteral(): JsIntLiteral {
        return JsIntLiteral(text.removePrefix("0x").toInt(16))
    }

    private inline fun <reified T> visitNode(node: ParseTree): T =
        visit(node).expect<T>()

    private inline fun <reified T> visitAll(nodes: List<ParseTree>): List<T> =
        nodes.map { visitNode<T>(it) }

    private inline fun <reified T> JsNode?.expect(): T {
        if (this !is T) throw AssertionError("Expected ${T::class}, got ${this?.javaClass}")
        return this
    }
}