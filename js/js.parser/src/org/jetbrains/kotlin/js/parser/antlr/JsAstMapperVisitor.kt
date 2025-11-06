/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.parser.AbortParsingException
import org.jetbrains.kotlin.js.parser.CodePosition
import org.jetbrains.kotlin.js.parser.ErrorReporter
import org.jetbrains.kotlin.js.parser.ScopeContext
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptLexer
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptParser

internal class JsAstMapperVisitor(
    private val fileName: String,
    private val scopeContext: ScopeContext,
    private val reporter: ErrorReporter
) : AntlrJsBaseVisitor<JsNode?>() {
    override fun visitSourceElement(ctx: JavaScriptParser.SourceElementContext): JsStatement? {
        return visitNode<JsStatement?>(ctx.statement())
    }

    // ENTRY POINT
    override fun visitStatement(ctx: JavaScriptParser.StatementContext): JsStatement? {
        ctx.functionDeclaration()?.let {
            return visitNode<JsFunction>(it).makeStmt().applyComments(ctx)
        }

        return super.visitStatement(ctx).expect<JsStatement?>()?.applyComments(ctx)
    }

    override fun visitBlock(ctx: JavaScriptParser.BlockContext): JsBlock {
        ctx.statementList()?.let {
            return visitNode<JsBlock>(it).applyLocation(ctx.OpenBrace())
        }

        return JsBlock().applyLocation(ctx.OpenBrace())
    }

    override fun visitStatementList(ctx: JavaScriptParser.StatementListContext): JsBlock {
        return mapBlock(visitAll<JsStatement?>(ctx.statement())).applyLocation(ctx)
    }

    override fun visitImportStatement(ctx: JavaScriptParser.ImportStatementContext): JsNode? {
        reportError("Import statements not supported yet", ctx)
    }

    override fun visitImportFromBlock(ctx: JavaScriptParser.ImportFromBlockContext): JsNode? {
        reportError("Import statements not supported yet", ctx)
    }

    override fun visitImportModuleItems(ctx: JavaScriptParser.ImportModuleItemsContext): JsNode? {
        reportError("Import statements not supported yet", ctx)
    }

    override fun visitImportAliasName(ctx: JavaScriptParser.ImportAliasNameContext): JsNode? {
        reportError("Import statements not supported yet", ctx)
    }

    override fun visitModuleExportName(ctx: JavaScriptParser.ModuleExportNameContext): JsNode? {
        reportError("Export and import statements not supported yet", ctx)
    }

    override fun visitImportedBinding(ctx: JavaScriptParser.ImportedBindingContext): JsNode? {
        reportError("Import statements not supported yet", ctx)
    }

    override fun visitImportDefault(ctx: JavaScriptParser.ImportDefaultContext): JsNode? {
        reportError("Import statements not supported yet", ctx)
    }

    override fun visitImportNamespace(ctx: JavaScriptParser.ImportNamespaceContext): JsNode? {
        reportError("Import statements not supported yet", ctx)
    }

    override fun visitImportFrom(ctx: JavaScriptParser.ImportFromContext): JsNode? {
        reportError("Import statements not supported yet", ctx)
    }

    override fun visitAliasName(ctx: JavaScriptParser.AliasNameContext): JsNode? {
        reportError("Import statements not supported yet", ctx)
    }

    override fun visitExportDeclaration(ctx: JavaScriptParser.ExportDeclarationContext): JsNode? {
        reportError("Export statements not supported yet", ctx)
    }

    override fun visitExportDefaultDeclaration(ctx: JavaScriptParser.ExportDefaultDeclarationContext): JsNode? {
        reportError("Export statements not supported yet", ctx)
    }

    override fun visitExportFromBlock(ctx: JavaScriptParser.ExportFromBlockContext): JsNode? {
        reportError("Export statements not supported yet", ctx)
    }

    override fun visitExportModuleItems(ctx: JavaScriptParser.ExportModuleItemsContext): JsNode? {
        reportError("Export statements not supported yet", ctx)
    }

    override fun visitExportAliasName(ctx: JavaScriptParser.ExportAliasNameContext): JsNode? {
        reportError("Export statements not supported yet", ctx)
    }

    override fun visitDeclaration(ctx: JavaScriptParser.DeclarationContext): JsNode? {
        reportError("Export statements not supported yet", ctx)
    }

    override fun visitVariableStatement(ctx: JavaScriptParser.VariableStatementContext): JsVars {
        return visitNode<JsVars>(ctx.variableDeclarationList())
    }

    override fun visitVariableDeclarationList(ctx: JavaScriptParser.VariableDeclarationListContext): JsVars {
        return JsVars().apply {
            ctx.variableDeclaration().forEach {
                add(visitNode<JsVars.JsVar>(it))
            }
        }.applyLocation(ctx)
    }

    override fun visitSingleVariableDeclaration(ctx: JavaScriptParser.SingleVariableDeclarationContext): JsVars.JsVar {
        return visitNode<JsVars.JsVar>(ctx.variableDeclaration())
    }

    override fun visitVariableDeclaration(ctx: JavaScriptParser.VariableDeclarationContext): JsVars.JsVar {
        val originalId = ctx.assignable().identifier()?.text
            ?: reportError("Only identifier parameters are supported yet", ctx)
        val id = scopeContext.localNameFor(originalId)
        val initialization = ctx.singleExpression()?.let { visitNode<JsExpression>(it) }

        return JsVars.JsVar(id, initialization).applyLocation(ctx)
    }

    override fun visitEmptyStatement_(ctx: JavaScriptParser.EmptyStatement_Context): JsEmpty {
        return JsEmpty
    }

    override fun visitExpressionStatement(ctx: JavaScriptParser.ExpressionStatementContext): JsStatement {
        return visitNode<JsExpression>(ctx.expressionSequence())
            .makeStmt()
    }

    override fun visitIfStatement(ctx: JavaScriptParser.IfStatementContext): JsIf {
        val ifCondition = visitNode<JsExpression>(ctx.expressionSequence())
        // Empty statements are not supported in both 'if' branches, so always expect non-nullable statements.
        val allStatements = visitAll<JsStatement>(ctx.statement())

        return JsIf(
            ifCondition,
            allStatements[0],
            allStatements.getOrNull(1)
        ).applyLocation(ctx)
    }

    override fun visitDoStatement(ctx: JavaScriptParser.DoStatementContext): JsDoWhile {
        val body = visitNode<JsStatement?>(ctx.statement()) ?: JsEmpty
        val condition = visitNode<JsExpression>(ctx.expressionSequence())

        return JsDoWhile(condition, body).applyLocation(ctx)
    }

    override fun visitWhileStatement(ctx: JavaScriptParser.WhileStatementContext): JsWhile {
        val condition = visitNode<JsExpression>(ctx.expressionSequence())
        val body = visitNode<JsStatement?>(ctx.statement()) ?: JsEmpty

        return JsWhile(condition, body).applyLocation(ctx)
    }

    override fun visitForStatement(ctx: JavaScriptParser.ForStatementContext): JsFor {
        val initSequence = ctx.vars?.let { visitNode<JsExpression>(it) }
        val initDeclaration = ctx.`var`?.let { visitNode<JsVars>(it) }

        val condition = ctx.condition?.let { visitNode<JsExpression>(it) }
        val increment = ctx.increment?.let { visitNode<JsExpression>(it) }
        val body = visitNode<JsStatement?>(ctx.statement()) ?: JsEmpty

        return when {
            initSequence != null -> JsFor(initSequence, condition, increment, body)
            initDeclaration != null -> JsFor(initDeclaration, condition, increment, body)
            else -> JsFor(null as JsVars?, condition, increment, body)
        }.applyLocation(ctx)
    }

    override fun visitForInStatement(ctx: JavaScriptParser.ForInStatementContext): JsForIn {
        val unnamedExpression = ctx.singleExpression()?.let { visitNode<JsExpression>(it) }
        val namedDeclaration = ctx.singleVariableDeclaration()?.let { visitNode<JsVars.JsVar>(it) }
        val inTargetExpression = visitNode<JsExpression>(ctx.expressionSequence())

        val bodyStatement = visitNode<JsStatement?>(ctx.statement()) ?: JsEmpty

        return when {
            unnamedExpression != null -> JsForIn().apply {
                iterExpression = unnamedExpression
                objectExpression = inTargetExpression
                body = bodyStatement
            }
            namedDeclaration != null -> JsForIn(namedDeclaration.name).apply {
                iterExpression = namedDeclaration.initExpression
                objectExpression = inTargetExpression
                body = bodyStatement
            }
            else -> reportError("Invalid 'for .. in' statement: ${ctx.text}", ctx)
        }.applyLocation(ctx)
    }

    override fun visitForOfStatement(ctx: JavaScriptParser.ForOfStatementContext): JsNode {
        reportError("'for .. of' is not supported yet", ctx)
    }

    override fun visitVarModifier(ctx: JavaScriptParser.VarModifierContext): JsNode {
        raiseParserException("There is no JS node that represents 'var' modifier.", ctx)
    }

    override fun visitContinueStatement(ctx: JavaScriptParser.ContinueStatementContext): JsContinue {
        val identifier = ctx.identifier()?.let { scopeContext.labelFor(it.text) }
        return JsContinue(identifier?.makeRef()).applyLocation(ctx)
    }

    override fun visitBreakStatement(ctx: JavaScriptParser.BreakStatementContext): JsBreak {
        val identifier = ctx.identifier()?.let { scopeContext.labelFor(it.text) }
        return JsBreak(identifier?.makeRef()).applyLocation(ctx)
    }

    override fun visitReturnStatement(ctx: JavaScriptParser.ReturnStatementContext): JsReturn {
        return JsReturn().apply {
            ctx.expressionSequence()?.let {
                expression = visitNode<JsExpression>(it)
            }
        }.applyLocation(ctx)
    }

    override fun visitYieldStatement(ctx: JavaScriptParser.YieldStatementContext): JsStatement {
        val expression = ctx.expressionSequence()?.let { visitNode<JsExpression>(it) }
        return JsYield(expression).makeStmt().applyLocation(ctx)
    }

    override fun visitWithStatement(ctx: JavaScriptParser.WithStatementContext): JsNode {
        // The "with" statement is unsupported because it introduces ambiguity
        // related to whether a name is obfuscatable that we cannot resolve
        // statically.
        reportError(
            "'with' statement is not supported",
            ctx
        )
    }

    override fun visitSwitchStatement(ctx: JavaScriptParser.SwitchStatementContext): JsSwitch {
        val jsSwitchExpr = visitNode<JsExpression>(ctx.expressionSequence())

        val jsClauses = buildList {
            ctx.caseBlock().let { cases ->
                cases.beforeDefault?.let {
                    addAll(visitAll<JsCase>(it.caseClause()))
                }
                cases.defaultClause()?.let {
                    add(visitNode<JsDefault>(it))
                }
                cases.afterDefault?.let {
                    addAll(visitAll<JsCase>(it.caseClause()))
                }
            }
        }

        return JsSwitch().apply {
            expression = jsSwitchExpr
            cases.addAll(jsClauses)
        }.applyLocation(ctx)
    }

    override fun visitCaseBlock(ctx: JavaScriptParser.CaseBlockContext): JsNode {
        raiseParserException("There is no JS node that represents 'switch' body.", ctx)
    }

    override fun visitCaseClauses(ctx: JavaScriptParser.CaseClausesContext): JsNode {
        raiseParserException("JS AST doesn't have a node representing case clauses aggregate.", ctx)
    }

    override fun visitCaseClause(ctx: JavaScriptParser.CaseClauseContext): JsCase {
        val jsExpression = ctx.expressionSequence()?.let { visitNode<JsExpression>(it) }
        val jsStatementsList = ctx.statementList()?.let { visitNode<JsBlock>(it) }

        return JsCase().apply {
            caseExpression = jsExpression
            statements.addAll(jsStatementsList?.statements ?: emptyList())
        }.applyLocation(ctx)
    }

    override fun visitDefaultClause(ctx: JavaScriptParser.DefaultClauseContext): JsDefault {
        val jsStatementsList = ctx.statementList()?.let { visitNode<JsBlock>(it) }

        return JsDefault().apply {
            statements.addAll(jsStatementsList?.statements ?: emptyList())
        }.applyLocation(ctx)
    }

    override fun visitLabelledStatement(ctx: JavaScriptParser.LabelledStatementContext): JsLabel {
        val jsLabelIdentifier = ctx.identifier().text
        val jsName = scopeContext.enterLabel(jsLabelIdentifier, jsLabelIdentifier)
        val jsLabel = JsLabel(jsName).apply {
            statement = visitNode<JsStatement>(ctx.statement())
        }
        scopeContext.exitLabel()

        return jsLabel.applyLocation(ctx)
    }

    override fun visitThrowStatement(ctx: JavaScriptParser.ThrowStatementContext): JsThrow {
        val jsThrowExpr = visitNode<JsExpression>(ctx.expressionSequence())

        return JsThrow(jsThrowExpr).applyLocation(ctx)
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
        }.applyLocation(ctx)
    }

    override fun visitCatchProduction(ctx: JavaScriptParser.CatchProductionContext): JsCatch {
        val jsCatchIdentifier = ctx.assignable().identifier()?.text
            ?: reportError("Only identifier catch variables are supported yet", ctx)

        return scopeContext.enterCatch(jsCatchIdentifier).apply {
            body = visitNode<JsBlock>(ctx.block())
            scopeContext.exitCatch()
        }.applyLocation(ctx)
    }

    override fun visitFinallyProduction(ctx: JavaScriptParser.FinallyProductionContext): JsBlock {
        return visitNode<JsBlock>(ctx.block())
    }

    override fun visitDebuggerStatement(ctx: JavaScriptParser.DebuggerStatementContext): JsDebugger {
        return JsDebugger().applyLocation(ctx)
    }

    override fun visitFunctionDeclaration(ctx: JavaScriptParser.FunctionDeclarationContext): JsFunction {
        val name = ctx.identifier()
        val isGenerator = ctx.Multiply() != null
        val paramList = ctx.formalParameterList()
        val restParam = paramList?.restParameterArg()
        val formalParams = paramList?.formalParameterArg() ?: emptyList()
        check(restParam == null) { "Rest parameters are not supported yet" }

        return mapFunction(name?.text, ctx.functionBody(), formalParams, isGenerator)
            .applyLocation(ctx)
    }

    override fun visitClassDeclaration(ctx: JavaScriptParser.ClassDeclarationContext): JsNode? {
        reportError("Classes are not supported yet", ctx)
    }

    override fun visitClassTail(ctx: JavaScriptParser.ClassTailContext): JsNode? {
        reportError("Classes are not supported yet", ctx)
    }

    override fun visitClassElement(ctx: JavaScriptParser.ClassElementContext): JsNode? {
        reportError("Classes are not supported yet", ctx)
    }

    override fun visitMethodDefinition(ctx: JavaScriptParser.MethodDefinitionContext): JsNode? {
        reportError("Classes are not supported yet", ctx)
    }

    override fun visitFieldDefinition(ctx: JavaScriptParser.FieldDefinitionContext): JsNode? {
        reportError("Classes are not supported yet", ctx)
    }

    override fun visitClassElementName(ctx: JavaScriptParser.ClassElementNameContext): JsNode? {
        reportError("Classes are not supported yet", ctx)
    }

    override fun visitPrivateIdentifier(ctx: JavaScriptParser.PrivateIdentifierContext): JsNode? {
        reportError("Private fields are not supported yet", ctx)
    }

    override fun visitFormalParameterList(ctx: JavaScriptParser.FormalParameterListContext): JsNode? {
        raiseParserException("JS AST doesn't have a node representing a formal parameter list.", ctx)
    }

    override fun visitFormalParameterArg(ctx: JavaScriptParser.FormalParameterArgContext): JsParameter {
        val identifier = ctx.assignable().identifier()
            ?: reportError("Only identifier parameters are supported yet", ctx)
        val paramName = scopeContext.localNameFor(identifier.text)

        return JsParameter(paramName).applyLocation(ctx)
    }

    override fun visitRestParameterArg(ctx: JavaScriptParser.RestParameterArgContext?): JsNode? {
        raiseParserException("Rest parameters are not supported yet", ctx)
    }

    override fun visitFunctionBody(ctx: JavaScriptParser.FunctionBodyContext): JsBlock {
        ctx.sourceElements()?.let {
            return visitNode<JsBlock>(it)
        }
        return JsBlock().applyLocation(ctx.OpenBrace())
    }

    override fun visitSourceElements(ctx: JavaScriptParser.SourceElementsContext): JsBlock {
        val statements = visitAll<JsStatement?>(ctx.sourceElement())
        return mapBlock(statements).applyLocation(ctx)
    }

    override fun visitArrayLiteral(ctx: JavaScriptParser.ArrayLiteralContext): JsArrayLiteral {
        return JsArrayLiteral().apply {
            expressions.addAll(visitAll<JsExpression>(ctx.elementList().arrayElement()))
        }.applyLocation(ctx)
    }

    override fun visitElementList(ctx: JavaScriptParser.ElementListContext): JsNode? {
        raiseParserException("JS AST doesn't have a node representing an array elements list.", ctx)
    }

    override fun visitArrayElement(ctx: JavaScriptParser.ArrayElementContext): JsExpression {
        check(ctx.Ellipsis() == null) { "Spread operator is not supported yet" }

        return visitNode<JsExpression>(ctx.singleExpression())
    }

    override fun visitPropertyExpressionAssignment(ctx: JavaScriptParser.PropertyExpressionAssignmentContext): JsPropertyInitializer {
        val jsLabelExpr = visitNode<JsExpression>(ctx.propertyName())
        val jsValue = visitNode<JsExpression>(ctx.singleExpression())

        return JsPropertyInitializer(jsLabelExpr, jsValue).applyLocation(ctx)
    }

    override fun visitComputedPropertyExpressionAssignment(ctx: JavaScriptParser.ComputedPropertyExpressionAssignmentContext): JsNode? {
        reportError("Computed property names are not supported yet", ctx)
    }

    override fun visitFunctionProperty(ctx: JavaScriptParser.FunctionPropertyContext): JsNode? {
        reportError("Function properties are not supported yet", ctx)
    }

    override fun visitPropertyGetter(ctx: JavaScriptParser.PropertyGetterContext): JsNode? {
        reportError("Property getters are not supported yet", ctx)
    }

    override fun visitPropertySetter(ctx: JavaScriptParser.PropertySetterContext): JsNode? {
        reportError("Property setters are not supported yet", ctx)
    }

    override fun visitPropertyShorthand(ctx: JavaScriptParser.PropertyShorthandContext): JsNode? {
        reportError("Property shorthands are not supported yet", ctx)
    }

    override fun visitPropertyName(ctx: JavaScriptParser.PropertyNameContext): JsExpression {
        ctx.identifierName()?.let {
            return JsStringLiteral(it.text).applyLocation(ctx)
        }

        ctx.StringLiteral()?.let {
            return it.text.unescapeString(ctx).toStringLiteral().applyLocation(ctx)
        }

        ctx.numericLiteral()?.let {
            return visitNode<JsNumberLiteral>(it)
        }

        ctx.singleExpression()?.let {
            reportError("Computed property names are not supported yet", ctx)
        }

        raiseParserException("Invalid property name: ${ctx.text}", ctx)
    }

    override fun visitArguments(ctx: JavaScriptParser.ArgumentsContext): JsNode? {
        // JS AST doesn't have a node representing an arguments list.'
        raiseParserException("JS AST doesn't have a node representing an arguments list.", ctx)
    }

    override fun visitArgument(ctx: JavaScriptParser.ArgumentContext): JsExpression {
        check(ctx.Ellipsis() == null) { "Spread operator is not supported yet" }

        ctx.singleExpression()?.let {
            return visitNode<JsExpression>(it)
        }

        ctx.identifier()?.let {
            return visitNode<JsNameRef>(it)
        }

        raiseParserException("Invalid argument: ${ctx.text}", ctx)
    }

    override fun visitExpressionSequence(ctx: JavaScriptParser.ExpressionSequenceContext): JsExpression {
        ctx.singleExpression()?.let {
            return visitNode<JsExpression>(it)
        }

        val left = visitNode<JsExpression>(ctx.lhs)
        val right = visitNode<JsExpression>(ctx.rhs)

        return JsBinaryOperation(JsBinaryOperator.COMMA, left, right)
            .applyLocation(ctx.Comma())
    }

    override fun visitSingleExpression(ctx: JavaScriptParser.SingleExpressionContext): JsExpression {
        return super.visit(ctx.singleExpressionImpl()).expect<JsExpression>().applyComments(ctx)
    }

    override fun visitTemplateStringExpression(ctx: JavaScriptParser.TemplateStringExpressionContext): JsNode? {
        reportError("Template string literals are not supported yet", ctx)
    }

    override fun visitTernaryExpression(ctx: JavaScriptParser.TernaryExpressionContext): JsConditional {
        val conditionExpression = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val thenExpression = visitNode<JsExpression>(ctx.singleExpressionImpl(1))
        val elseCondition = visitNode<JsExpression>(ctx.singleExpressionImpl(2))

        return JsConditional(conditionExpression, thenExpression, elseCondition)
            .applyLocation(ctx.QuestionMark())
    }

    override fun visitLogicalAndExpression(ctx: JavaScriptParser.LogicalAndExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.AND, left, right)
            .applyLocation(ctx.And())
    }

    override fun visitPowerExpression(ctx: JavaScriptParser.PowerExpressionContext): JsNode? {
        reportError("Power expressions are not supported yet", ctx)
    }

    override fun visitPreIncrementExpression(ctx: JavaScriptParser.PreIncrementExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.INC, expression)
            .applyLocation(ctx.PlusPlus())
    }

    override fun visitObjectLiteralExpression(ctx: JavaScriptParser.ObjectLiteralExpressionContext): JsObjectLiteral {
        return visitNode<JsObjectLiteral>(ctx.objectLiteral())
    }

    override fun visitMetaExpression(ctx: JavaScriptParser.MetaExpressionContext): JsNode? {
        reportError("Meta expressions are not supported yet", ctx)
    }

    override fun visitInExpression(ctx: JavaScriptParser.InExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.INOP, left, right)
            .applyLocation(ctx.In())
    }

    override fun visitLogicalOrExpression(ctx: JavaScriptParser.LogicalOrExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.OR, left, right)
            .applyLocation(ctx.Or())
    }

    override fun visitOptionalChainExpression(ctx: JavaScriptParser.OptionalChainExpressionContext): JsNode? {
        reportError("Optional chain expressions are not supported yet", ctx)
    }

    override fun visitNotExpression(ctx: JavaScriptParser.NotExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.NOT, expression)
            .applyLocation(ctx.Not())
    }

    override fun visitPreDecreaseExpression(ctx: JavaScriptParser.PreDecreaseExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.DEC, expression)
            .applyLocation(ctx.MinusMinus())
    }

    override fun visitArgumentsExpression(ctx: JavaScriptParser.ArgumentsExpressionContext): JsInvocation {
        val qualifier = visitNode<JsExpression>(ctx.singleExpressionImpl())
        val arguments = ctx.arguments().argument().map { visitNode<JsExpression>(it) }

        return JsInvocation(qualifier, arguments)
            .applyLocation(ctx.arguments().OpenParen())
    }

    override fun visitAwaitExpression(ctx: JavaScriptParser.AwaitExpressionContext): JsNode? {
        reportError("async/await statements are not supported yet", ctx)
    }

    override fun visitThisExpression(ctx: JavaScriptParser.ThisExpressionContext): JsThisRef {
        return JsThisRef().applyLocation(ctx)
    }

    override fun visitFunctionExpression(ctx: JavaScriptParser.FunctionExpressionContext): JsNode? {
        return super.visitFunctionExpression(ctx)
    }

    override fun visitUnaryMinusExpression(ctx: JavaScriptParser.UnaryMinusExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.NEG, expression).applyLocation(ctx.Minus())
    }

    override fun visitAssignmentExpression(ctx: JavaScriptParser.AssignmentExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))
        return JsBinaryOperation(JsBinaryOperator.ASG, left, right)
            .applyLocation(ctx.Assign())
            .applyComments(ctx)
    }

    override fun visitPostDecreaseExpression(ctx: JavaScriptParser.PostDecreaseExpressionContext): JsPostfixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPostfixOperation(JsUnaryOperator.DEC, expression).applyLocation(ctx.MinusMinus())
    }

    override fun visitTypeofExpression(ctx: JavaScriptParser.TypeofExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.TYPEOF, expression).applyLocation(ctx.Typeof())
    }

    override fun visitInstanceofExpression(ctx: JavaScriptParser.InstanceofExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))
        return JsBinaryOperation(JsBinaryOperator.INSTANCEOF, left, right).applyLocation(ctx.Instanceof())
    }

    override fun visitUnaryPlusExpression(ctx: JavaScriptParser.UnaryPlusExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.POS, expression).applyLocation(ctx.Plus())
    }

    override fun visitDeleteExpression(ctx: JavaScriptParser.DeleteExpressionContext): JsExpression {
        val target = visitNode<JsExpression>(ctx.singleExpressionImpl())
        if (target is JsNameRef || target is JsArrayAccess)
            return JsPrefixOperation(JsUnaryOperator.DELETE, target).applyLocation(ctx.Delete())
        return JsNullLiteral().applyLocation(ctx)
    }

    override fun visitImportExpression(ctx: JavaScriptParser.ImportExpressionContext): JsInvocation {
        val argument = visitNode<JsExpression>(ctx.singleExpressionImpl())
        val jsImportIdentifier = makeRefNode(ctx.Import().text).applyLocation(ctx.Import())
        return JsInvocation(jsImportIdentifier, argument).applyLocation(ctx.Import())
    }

    override fun visitEqualityExpression(ctx: JavaScriptParser.EqualityExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return ctx.run {
            val (operator, token) = when {
                Equals_() != null -> JsBinaryOperator.EQ to Equals_()
                NotEquals() != null -> JsBinaryOperator.NEQ to NotEquals()
                IdentityEquals() != null -> JsBinaryOperator.REF_EQ to IdentityEquals()
                IdentityNotEquals() != null -> JsBinaryOperator.REF_NEQ to IdentityNotEquals()
                else -> raiseParserException("Invalid binary operation: ${ctx.text}", ctx)
            }

            JsBinaryOperation(operator, left, right).applyLocation(token)
        }
    }

    override fun visitBitXOrExpression(ctx: JavaScriptParser.BitXOrExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.BIT_XOR, left, right)
            .applyLocation(ctx.BitXOr())
    }

    override fun visitSuperExpression(ctx: JavaScriptParser.SuperExpressionContext): JsNode? {
        reportError("Super calls are not supported yet", ctx)
    }

    override fun visitImportMetaExpression(ctx: JavaScriptParser.ImportMetaExpressionContext): JsNameRef {
        return makeRefNode(ctx.Meta().text).apply {
            qualifier = makeRefNode(ctx.Import().text).applyLocation(ctx.Import())
        }.applyLocation(ctx.Meta())
    }

    override fun visitMultiplicativeExpression(ctx: JavaScriptParser.MultiplicativeExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return ctx.run {
            val (operator, token) = when {
                Multiply() != null -> JsBinaryOperator.MUL to Multiply()
                Divide() != null -> JsBinaryOperator.DIV to Divide()
                Modulus() != null -> JsBinaryOperator.MOD to Modulus()
                else -> raiseParserException("Invalid binary operation: ${ctx.text}", ctx)
            }
            JsBinaryOperation(operator, left, right).applyLocation(token)
        }
    }

    override fun visitBitShiftExpression(ctx: JavaScriptParser.BitShiftExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return ctx.run {
            val (operator, token) = when {
                RightShiftArithmetic() != null -> JsBinaryOperator.SHR to RightShiftArithmetic()
                LeftShiftArithmetic() != null -> JsBinaryOperator.SHL to LeftShiftArithmetic()
                RightShiftLogical() != null -> JsBinaryOperator.SHRU to RightShiftLogical()
                else -> raiseParserException("Invalid binary operation: ${ctx.text}", ctx)
            }
            JsBinaryOperation(operator, left, right).applyLocation(token)
        }
    }

    override fun visitParenthesizedExpression(ctx: JavaScriptParser.ParenthesizedExpressionContext): JsExpression {
        return visitNode<JsExpression>(ctx.expressionSequence())
    }

    override fun visitAdditiveExpression(ctx: JavaScriptParser.AdditiveExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return ctx.run {
            val (operator, token) = when {
                Plus() != null -> JsBinaryOperator.ADD to Plus()
                Minus() != null -> JsBinaryOperator.SUB to Minus()
                else -> raiseParserException("Invalid binary operation: ${ctx.text}", ctx)
            }
            JsBinaryOperation(operator, left, right).applyLocation(token)
        }
    }

    override fun visitRelationalExpression(ctx: JavaScriptParser.RelationalExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return ctx.run {
            val (operator, token) = when {
                LessThan() != null -> JsBinaryOperator.LT to LessThan()
                MoreThan() != null -> JsBinaryOperator.GT to MoreThan()
                LessThanEquals() != null -> JsBinaryOperator.LTE to LessThanEquals()
                GreaterThanEquals() != null -> JsBinaryOperator.GTE to GreaterThanEquals()
                else -> raiseParserException("Invalid binary operation: ${ctx.text}", ctx)
            }
            JsBinaryOperation(operator, left, right).applyLocation(token)
        }
    }

    override fun visitPostIncrementExpression(ctx: JavaScriptParser.PostIncrementExpressionContext): JsPostfixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPostfixOperation(JsUnaryOperator.INC, expression).applyLocation(ctx.PlusPlus())
    }

    override fun visitYieldExpression(ctx: JavaScriptParser.YieldExpressionContext): JsYield {
        val expression = ctx.expressionSequence()?.let { visitNode<JsExpression>(it) }
        return JsYield(expression).applyLocation(ctx)
    }

    override fun visitBitNotExpression(ctx: JavaScriptParser.BitNotExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.BIT_NOT, expression).applyLocation(ctx.BitNot())
    }

    override fun visitNewExpression(ctx: JavaScriptParser.NewExpressionContext): JsNew {
        // For `new Object` or `new Object(...)`
        val jsNewPlainIdentifier = ctx.identifier()?.let { makeRefNode(it.text).applyLocation(it) }
        // For `new (Object)` or `new (Object)(...)`
        val jsNewSingleExpression = ctx.singleExpressionImpl()?.let { visitNode<JsExpression>(it) }
        val jsNewExpression = when {
            jsNewPlainIdentifier != null -> jsNewPlainIdentifier
            else -> jsNewSingleExpression
        }

        // JS allows calling new-expressions without parens at all
        val jsArguments = ctx.arguments()?.let { visitAll<JsExpression>(it.argument()) } ?: emptyList()
        return JsNew(jsNewExpression).apply {
            arguments.addAll(jsArguments)
        }.applyLocation(ctx)
    }

    override fun visitLiteralExpression(ctx: JavaScriptParser.LiteralExpressionContext): JsLiteral {
        return visitNode<JsLiteral>(ctx.literal())
    }

    override fun visitArrayLiteralExpression(ctx: JavaScriptParser.ArrayLiteralExpressionContext): JsArrayLiteral {
        return visitNode<JsArrayLiteral>(ctx.arrayLiteral())
    }

    override fun visitMemberDotExpression(ctx: JavaScriptParser.MemberDotExpressionContext): JsNameRef {
        check(ctx.QuestionMark() == null) { "Optional chain expressions are not supported yet" }
        check(ctx.Hashtag() == null) { "Private member access expressions are not supported yet" }

        val jsLeft = visitNode<JsExpression>(ctx.singleExpressionImpl())
        val jsRight = scopeContext.referenceFor(ctx.identifierName().text).applyLocation(ctx.identifierName())

        return jsRight.apply {
            qualifier = jsLeft
        }
    }

    override fun visitClassExpression(ctx: JavaScriptParser.ClassExpressionContext): JsNode? {
        reportError("Classes are not supported yet", ctx)
    }

    override fun visitMemberIndexExpression(ctx: JavaScriptParser.MemberIndexExpressionContext): JsArrayAccess {
        check(ctx.QuestionMarkDot() == null) { "Optional chain expressions are not supported yet" }
        val jsObjectExpr = visitNode<JsExpression>(ctx.singleExpressionImpl())
        val jsMemberExpr = visitNode<JsExpression>(ctx.expressionSequence())

        return JsArrayAccess(jsObjectExpr, jsMemberExpr)
            .applyLocation(ctx.OpenBracket())
    }

    override fun visitIdentifierExpression(ctx: JavaScriptParser.IdentifierExpressionContext): JsNameRef {
        return visitNode<JsNameRef>(ctx.identifier())
    }

    override fun visitBitAndExpression(ctx: JavaScriptParser.BitAndExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.BIT_AND, left, right)
            .applyLocation(ctx.BitAnd())
    }

    override fun visitBitOrExpression(ctx: JavaScriptParser.BitOrExpressionContext): JsBinaryOperation {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return JsBinaryOperation(JsBinaryOperator.BIT_OR, left, right)
            .applyLocation(ctx.BitOr())
    }

    override fun visitAssignmentOperatorExpression(ctx: JavaScriptParser.AssignmentOperatorExpressionContext): JsNode? {
        val left = visitNode<JsExpression>(ctx.singleExpressionImpl(0))
        val right = visitNode<JsExpression>(ctx.singleExpressionImpl(1))

        return ctx.assignmentOperator().run {
            val (jsOperator, token) = when {
                MultiplyAssign() != null -> JsBinaryOperator.ASG_MUL to MultiplyAssign()
                DivideAssign() != null -> JsBinaryOperator.ASG_DIV to DivideAssign()
                ModulusAssign() != null -> JsBinaryOperator.ASG_MOD to ModulusAssign()
                PlusAssign() != null -> JsBinaryOperator.ASG_ADD to PlusAssign()
                MinusAssign() != null -> JsBinaryOperator.ASG_SUB to MinusAssign()
                LeftShiftArithmeticAssign() != null -> JsBinaryOperator.ASG_SHL to LeftShiftArithmeticAssign()
                RightShiftArithmeticAssign() != null -> JsBinaryOperator.ASG_SHR to RightShiftArithmeticAssign()
                RightShiftLogicalAssign() != null -> JsBinaryOperator.ASG_SHRU to RightShiftLogicalAssign()
                BitAndAssign() != null -> JsBinaryOperator.ASG_BIT_AND to BitAndAssign()
                BitXorAssign() != null -> JsBinaryOperator.ASG_BIT_XOR to BitXorAssign()
                BitOrAssign() != null -> JsBinaryOperator.ASG_BIT_OR to BitOrAssign()
                PowerAssign() != null -> reportError("Power assignment expressions are not supported yet", ctx)
                NullishCoalescingAssign() != null -> reportError("Null-coalescing assignment expressions are not supported yet", ctx)
                else -> raiseParserException("Invalid binary operation: ${ctx.text}", ctx)
            }

            JsBinaryOperation(jsOperator, left, right)
                .applyLocation(token)
        }
    }

    override fun visitVoidExpression(ctx: JavaScriptParser.VoidExpressionContext): JsPrefixOperation {
        val expression = visitNode<JsExpression>(ctx.singleExpressionImpl())
        return JsPrefixOperation(JsUnaryOperator.VOID, expression)
            .applyLocation(ctx.Void())
    }

    override fun visitCoalesceExpression(ctx: JavaScriptParser.CoalesceExpressionContext): JsNode? {
        reportError("Null-coalescing expressions are not supported yet", ctx)
    }

    override fun visitInitializer(ctx: JavaScriptParser.InitializerContext): JsNode? {
        reportError("Classes are not supported yet", ctx)
    }

    override fun visitAssignable(ctx: JavaScriptParser.AssignableContext): JsNode? {
        raiseParserException("Not yet implemented", ctx)
    }

    override fun visitObjectLiteral(ctx: JavaScriptParser.ObjectLiteralContext): JsObjectLiteral {
        return JsObjectLiteral().apply {
            visitAll<JsPropertyInitializer>(ctx.propertyAssignment()).forEach {
                propertyInitializers.add(it)
            }
        }.applyLocation(ctx)
    }

    override fun visitNamedFunction(ctx: JavaScriptParser.NamedFunctionContext): JsFunction {
        val declaration = ctx.functionDeclaration()
        val name = declaration.identifier()
        check(declaration.Async() == null) { "Async functions are not supported yet"}
        val isGenerator = declaration.Multiply() != null
        val paramList = declaration.formalParameterList()
        val restParam = paramList?.restParameterArg()
        val formalParams = paramList?.formalParameterArg() ?: emptyList()
        check(restParam == null) { "Rest parameters are not supported yet" }

        return mapFunction(name?.text, declaration.functionBody(), formalParams, isGenerator)
            .applyLocation(declaration.OpenParen())
    }

    override fun visitAnonymousFunctionDecl(ctx: JavaScriptParser.AnonymousFunctionDeclContext): JsFunction {
        val isGenerator = ctx.Multiply() != null
        val paramList = ctx.formalParameterList()
        val restParam = paramList?.restParameterArg()
        val formalParams = paramList?.formalParameterArg() ?: emptyList()
        check(restParam == null) { "Rest parameters are not supported yet" }

        return mapFunction(null, ctx.functionBody(), formalParams, isGenerator)
            .applyLocation(ctx.OpenParen())
    }

    override fun visitArrowFunction(ctx: JavaScriptParser.ArrowFunctionContext): JsFunction {
        reportError("Arrow functions are not supported yet", ctx)
    }

    override fun visitArrowFunctionParameters(ctx: JavaScriptParser.ArrowFunctionParametersContext): JsNode? {
        raiseParserException("JS AST doesn't have specific nodes for arrow function parameters", ctx)
    }

    override fun visitArrowFunctionBody(ctx: JavaScriptParser.ArrowFunctionBodyContext): JsBlock {
        reportError("Arrow functions are not supported yet", ctx)
    }

    override fun visitAssignmentOperator(ctx: JavaScriptParser.AssignmentOperatorContext): JsNode? {
        raiseParserException("JS AST doesn't have specific nodes for assignment operators", ctx)
    }

    override fun visitLiteral(ctx: JavaScriptParser.LiteralContext): JsLiteral {
        ctx.NullLiteral()?.run {
            return JsNullLiteral().applyLocation(ctx)
        }

        ctx.BooleanLiteral()?.let { bool ->
            return when (bool.text) {
                "true" -> JsBooleanLiteral(true)
                "false" -> JsBooleanLiteral(false)
                else -> raiseParserException("Invalid boolean literal: ${bool.text}", ctx)
            }.applyLocation(ctx)
        }

        ctx.StringLiteral()?.let {
            return it.text.unescapeString(ctx).toStringLiteral().applyLocation(ctx)
        }

        ctx.RegularExpressionLiteral()?.run {
            // TODO[seclerp]: Improve grammar to have pattern and flags as a separate terminals in regex rule to remove splitting here
            return JsRegExp().apply {
                val lastSlashIndex = text.lastIndexOf('/')
                pattern = text.substring(1, lastSlashIndex)
                if (lastSlashIndex < text.length - 1) {
                    flags = text.substring(lastSlashIndex + 1)
                }
            }.applyLocation(ctx)
        }

        return super.visitLiteral(ctx) as JsLiteral
    }

    override fun visitTemplateStringLiteral(ctx: JavaScriptParser.TemplateStringLiteralContext): JsNode? {
        reportError("Template string literals are not supported yet", ctx)
    }

    override fun visitTemplateStringAtom(ctx: JavaScriptParser.TemplateStringAtomContext): JsNode? {
        reportError("Template string literals are not supported yet", ctx)
    }

    override fun visitNumericLiteral(ctx: JavaScriptParser.NumericLiteralContext): JsNumberLiteral {
        ctx.BinaryIntegerLiteral()?.run {
            reportError("Binary integer literals are not supported yet", ctx)
        }

        ctx.OctalIntegerLiteral()?.let {
            val value = it.text.removePrefix("0")

            // In a non-strict mode invalid old octal literals, such are containing 8 and 9 (like 0888 or 0999)
            // are treated like decimal literals (888 and 999 correspondingly).
            // To embrace compatibility, we emit a warning here like the old GWT parser did.
            value.forEach { digit ->
                if (digit !in '0'..'7') {
                    reportWarning("illegal octal value '$value'; interpreting it as a decimal value", it.startPosition, it.stopPosition)
                    return value.toDecimalLiteral().applyLocation(ctx)
                }
            }

            return value.toOctalLiteral().applyLocation(ctx)
        }

        ctx.OctalIntegerLiteral2()?.run {
            reportError("Octal integer literals are not supported yet", ctx)
        }

        ctx.DecimalLiteral()?.let { decimalTerminal ->
            return decimalTerminal.text.toDecimalLiteral().applyLocation(ctx)
        }

        ctx.HexIntegerLiteral()?.let { hexTerminal ->
            return hexTerminal.text.toHexLiteral().applyLocation(ctx)
        }

        raiseParserException("Invalid numeric literal '${ctx.text}'", ctx)
    }

    override fun visitBigintLiteral(ctx: JavaScriptParser.BigintLiteralContext): JsNode? {
        reportError("Big integers are not supported yet", ctx)
    }

    override fun visitGetter(ctx: JavaScriptParser.GetterContext): JsNode? {
        reportError("Property getters are not supported yet", ctx)
    }

    override fun visitSetter(ctx: JavaScriptParser.SetterContext): JsNode? {
        reportError("Property setters are not supported yet", ctx)
    }

    override fun visitIdentifierName(ctx: JavaScriptParser.IdentifierNameContext): JsNode? {
        raiseParserException("There is no JS node that represents identifier name")
    }

    override fun visitIdentifier(ctx: JavaScriptParser.IdentifierContext): JsNameRef {
        return makeRefNode(ctx.text).applyLocation(ctx)
    }

    override fun visitReservedWord(ctx: JavaScriptParser.ReservedWordContext): JsNode? {
        raiseParserException("There is no JS node that represents reserved word")
    }

    override fun visitKeyword(ctx: JavaScriptParser.KeywordContext): JsNode? {
        raiseParserException("There is no JS node that represents keyword", ctx)
    }

    override fun visitLet_(ctx: JavaScriptParser.Let_Context): JsNode? {
        reportError("Let assignments are not supported yet", ctx)
    }

    override fun visitEos(ctx: JavaScriptParser.EosContext): JsNode? {
        return super.visit(ctx)
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
                parameters.add(jsParam.applyLocation(it))
            }

            body = visitNode<JsBlock>(functionBody)

            scopeContext.exitFunction()
        }
    }

    private fun makeRefNode(identifier: String): JsNameRef {
        return scopeContext.globalNameFor(identifier).makeRef()
    }

    private fun reportError(message: String, ctx: ParserRuleContext): Nothing {
        reporter.error(
            message,
            ctx.startPosition,
            ctx.stopPosition
        )
        throw AbortParsingException()
    }

    private fun reportError(message: String, startPosition: CodePosition? = null, endPosition: CodePosition? = null): Nothing {
        reporter.error(
            message,
            startPosition ?: CodePosition(0, 0),
            endPosition ?: CodePosition(0, 0)
        )
        throw AbortParsingException()
    }

    private fun reportWarning(message: String, startPosition: CodePosition? = null, endPosition: CodePosition? = null) {
        reporter.warning(
            message,
            startPosition ?: CodePosition(0, 0),
            endPosition ?: CodePosition(0, 0)
        )
    }

    private fun check(condition: Boolean, position: CodePosition? = null, messageFactory: () -> String) {
        if (!condition)
            reportError(messageFactory(), position ?: CodePosition(0, 0))
    }

    private fun <T : JsNode> T.applyLocation(terminal: TerminalNode): T =
        this.also { targetNode ->
            val location = terminal.symbol.startPosition
            targetNode.source = JsLocation(fileName, location.line, location.offset, null)
        }

    private fun <T : JsNode> T.applyLocation(sourceNode: ParserRuleContext): T =
        this.also { targetNode ->
            val location = when (sourceNode) {
                is JavaScriptParser.FunctionDeclarationContext ->
                    // For functions, consider their location to be at the opening parenthesis.
                    sourceNode.OpenParen().symbol.startPosition
                is JavaScriptParser.MemberDotExpressionContext ->
                    // For dot-qualified references, consider their position to be at the rightmost name reference.
                    sourceNode.identifierName().startPosition
                else ->
                    sourceNode.startPosition
            }

            val originalName = when (targetNode) {
                is JsFunction, is JsVars.JsVar, is JsParameter -> targetNode.name?.toString()
                else -> null
            }

            val jsLocation = JsLocation(fileName, location.line, location.offset, originalName)

            when (targetNode) {
                is SourceInfoAwareJsNode ->
                    targetNode.source = jsLocation
                is JsExpressionStatement if targetNode.expression.source == null ->
                    targetNode.expression.source = jsLocation
            }
        }

    private fun <T : JsNode> T.applyComments(commentsSource: JavaScriptRuleContext): T {
        fun mapComments(tokens: List<Token>) = when {
            tokens.isNotEmpty() -> tokens.map { token ->
                when (token.type) {
                    JavaScriptLexer.SingleLineComment -> token.text.removePrefix("//").let(::JsSingleLineComment)
                    JavaScriptLexer.MultiLineComment -> token.text.removeSurrounding("/*", "*/").let(::JsMultiLineComment)
                    else -> raiseParserException("Invalid comment token type: ${JavaScriptLexer.VOCABULARY.getDisplayName(token.type)}", commentsSource)
                }
            }
            else -> null
        }

        commentsBeforeNode = mapComments(commentsSource.commentsBefore)
        commentsAfterNode = mapComments(commentsSource.commentsAfter)

        return this
    }
}