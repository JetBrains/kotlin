/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import com.google.gwt.dev.js.ScopeContext
import com.intellij.util.containers.addIfNotNull
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.RuleNode
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
        return visit<JsStatement?>(ctx.statement())
    }

    // ENTRY POINT
    override fun visitStatement(ctx: JavaScriptParser.StatementContext): JsStatement? {
        ctx.functionDeclaration()?.run {
            return visitFunctionDeclaration(this).makeStmt()
        }

        return super.visitStatement(ctx).expect<JsStatement?>()
    }

    override fun visitBlock(ctx: JavaScriptParser.BlockContext): JsBlock {
        return visit<JsBlock>(ctx.statementList())
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
        return visit<JsVars>(ctx.variableDeclarationList())
    }

    override fun visitVariableDeclarationList(ctx: JavaScriptParser.VariableDeclarationListContext): JsVars {
        return JsVars().apply {
            ctx.variableDeclaration().forEach {
                add(visit<JsVars.JsVar>(it))
            }
        }
    }

    override fun visitSingleVariableDeclaration(ctx: JavaScriptParser.SingleVariableDeclarationContext): JsVars.JsVar {
        return visit<JsVars.JsVar>(ctx.variableDeclaration())
    }

    override fun visitVariableDeclaration(ctx: JavaScriptParser.VariableDeclarationContext): JsVars.JsVar {
        val originalId = ctx.assignable().identifier()?.text
            ?: TODO("Only identifier parameters are supported yet")
        val id = scopeContext.localNameFor(originalId)
        val initialization = ctx.singleExpression()?.let { visit<JsExpression>(it) }

        return JsVars.JsVar(id, initialization).applyLocation(fileName, ctx)
    }

    override fun visitEmptyStatement_(ctx: JavaScriptParser.EmptyStatement_Context): JsStatement? {
        return null
    }

    override fun visitExpressionStatement(ctx: JavaScriptParser.ExpressionStatementContext): JsStatement? {
        return visit<JsExpression>(ctx.expressionSequence()).makeStmt()
    }

    override fun visitIfStatement(ctx: JavaScriptParser.IfStatementContext): JsIf {
        val ifCondition = visit<JsExpression>(ctx.expressionSequence())
        // Empty statements are not supported in both 'if' branches, so always expect non-nullable statements.
        val allStatements = visitAll<JsStatement>(ctx.statement())

        return JsIf(
            ifCondition,
            allStatements[0],
            allStatements.getOrNull(1)
        )
    }

    override fun visitDoStatement(ctx: JavaScriptParser.DoStatementContext): JsDoWhile {
        val body = visit<JsStatement?>(ctx.statement()) ?: JsEmpty
        val condition = visit<JsExpression>(ctx.expressionSequence())

        return JsDoWhile(condition, body)
    }

    override fun visitWhileStatement(ctx: JavaScriptParser.WhileStatementContext): JsWhile {
        val condition = visit<JsExpression>(ctx.expressionSequence())
        val body = visit<JsStatement?>(ctx.statement()) ?: JsEmpty

        return JsWhile(condition, body)
    }

    override fun visitForStatement(ctx: JavaScriptParser.ForStatementContext): JsFor {
        val initSequence = ctx.expressionSequence(0)?.let { visit<JsExpression>(it) }
        val initDeclaration = ctx.variableDeclarationList()?.let { visit<JsVars>(it) }

        val condition = ctx.expressionSequence(1)?.let { visit<JsExpression>(it) }
        val increment = ctx.expressionSequence(2)?.let { visit<JsExpression>(it) }
        val body = visit<JsStatement?>(ctx.statement()) ?: JsEmpty

        return when {
            initSequence != null -> JsFor(initSequence, condition, increment, body)
            initDeclaration != null -> JsFor(initDeclaration, condition, increment, body)
            else -> TODO("Invalid 'for' statement: ${ctx.text}")
        }
    }

    override fun visitForInStatement(ctx: JavaScriptParser.ForInStatementContext): JsNode? {
        val unnamedExpression = ctx.singleExpression()?.let { visit<JsExpression>(it) }
        val namedDeclaration = ctx.singleVariableDeclaration()?.let { visit<JsVars.JsVar>(it) }
        val inTargetExpression = ctx.expressionSequence()?.let { visit<JsExpression>(it) }

        val bodyStatement = visit<JsStatement?>(ctx.statement()) ?: JsEmpty

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
                expression = visit<JsExpression>(it)
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
        val jsSwitchExpr = visit<JsExpression>(ctx.expressionSequence())

        val jsCases = ctx.caseBlock().caseClauses()?.let { visitAll<JsDefault>(it) } ?: emptyList()
        val jsDefault = ctx.caseBlock().defaultClause()?.let { visit<JsDefault>(it) }

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
        val jsExpression = ctx.expressionSequence()?.let { visit<JsExpression>(it) }
        val jsStatements = ctx.statementList()?.let { visit<JsStatement?>(it) }

        return JsCase().apply {
            caseExpression = jsExpression
            statements.addAll(listOfNotNull(jsStatements))
        }
    }

    override fun visitDefaultClause(ctx: JavaScriptParser.DefaultClauseContext): JsDefault {
        val jsStatements = ctx.statementList()?.let { visit<JsStatement?>(it) }

        return JsDefault().apply {
            statements.addAll(listOfNotNull(jsStatements))
        }
    }

    override fun visitLabelledStatement(ctx: JavaScriptParser.LabelledStatementContext): JsLabel {
        val jsLabelIdentifier = ctx.identifier().text
        val jsName = scopeContext.enterLabel(jsLabelIdentifier, jsLabelIdentifier)
        val jsLabel = JsLabel(jsName).apply {
            statement = visit<JsStatement>(ctx.statement())
        }
        scopeContext.exitLabel()

        return jsLabel
    }

    override fun visitThrowStatement(ctx: JavaScriptParser.ThrowStatementContext): JsNode? {
        val jsThrowExpr = visit<JsExpression>(ctx.expressionSequence())

        return JsThrow(jsThrowExpr)
    }

    override fun visitTryStatement(ctx: JavaScriptParser.TryStatementContext): JsTry {
        return JsTry().apply {
            tryBlock = visit<JsBlock>(ctx.block())

            val jsCatchProduction = ctx.catchProduction()?.let { visit<JsCatch>(it) }
            if (jsCatchProduction != null) {
                catches.add(jsCatchProduction)
            }

            val jsFinallyProduction = ctx.finallyProduction()?.let { visit<JsBlock>(it) }
            if (jsFinallyProduction != null) {
                finallyBlock = jsFinallyProduction
            }
        }
    }

    override fun visitCatchProduction(ctx: JavaScriptParser.CatchProductionContext): JsCatch {
        val jsCatchIdentifier = ctx.assignable().identifier()?.text
            ?: TODO("Only identifier catch variables are supported yet")

        return scopeContext.enterCatch(jsCatchIdentifier).apply {
            body = visit<JsBlock>(ctx.block())
            // TODO: Decide what to do with "catch conditions":
            //   https://lia.disi.unibo.it/materiale/JS/developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/try...html#Conditional_catch_clauses
            condition = null
            scopeContext.exitCatch()
        }
    }

    override fun visitFinallyProduction(ctx: JavaScriptParser.FinallyProductionContext): JsBlock {
        return visit<JsBlock>(ctx.block())
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
        return visit<JsBlock>(ctx.sourceElements())
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

        return visit<JsExpression>(ctx.singleExpression())
    }

    override fun visitPropertyExpressionAssignment(ctx: JavaScriptParser.PropertyExpressionAssignmentContext): JsPropertyInitializer {
        val jsLabelExpr = visit<JsExpression>(ctx.propertyName())
        val jsValue = visit<JsExpression>(ctx.singleExpression())

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
            return visit<JsNumberLiteral>(it)
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
            return visit<JsExpression>(it)
        }

        ctx.identifier()?.let {
            return makeRefNode(it.text)
        }

        TODO("Invalid argument: ${ctx.text}")
    }

    override fun visitExpressionSequence(ctx: JavaScriptParser.ExpressionSequenceContext): JsExpression {
        if (ctx.singleExpression().size == 1) {
            return visit<JsExpression>(ctx.singleExpression()[0])
        }

        val exprs = visitAll<JsExpression>(ctx.singleExpression())
        return mapComma(exprs)
    }

    override fun visitSingleExpression(ctx: JavaScriptParser.SingleExpressionContext?): JsExpression {
        return super.visit(ctx).expect<JsExpression>()
    }

    override fun visitTemplateStringExpression(ctx: JavaScriptParser.TemplateStringExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitTernaryExpression(ctx: JavaScriptParser.TernaryExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitLogicalAndExpression(ctx: JavaScriptParser.LogicalAndExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPowerExpression(ctx: JavaScriptParser.PowerExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPreIncrementExpression(ctx: JavaScriptParser.PreIncrementExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitObjectLiteralExpression(ctx: JavaScriptParser.ObjectLiteralExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitMetaExpression(ctx: JavaScriptParser.MetaExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitInExpression(ctx: JavaScriptParser.InExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitLogicalOrExpression(ctx: JavaScriptParser.LogicalOrExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitOptionalChainExpression(ctx: JavaScriptParser.OptionalChainExpressionContext): JsNode? {
        TODO("Optional chain expressions are not supported yet")
    }

    override fun visitNotExpression(ctx: JavaScriptParser.NotExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPreDecreaseExpression(ctx: JavaScriptParser.PreDecreaseExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitArgumentsExpression(ctx: JavaScriptParser.ArgumentsExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAwaitExpression(ctx: JavaScriptParser.AwaitExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitThisExpression(ctx: JavaScriptParser.ThisExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitFunctionExpression(ctx: JavaScriptParser.FunctionExpressionContext): JsNode? {
        return super.visitFunctionExpression(ctx)
    }

    override fun visitUnaryMinusExpression(ctx: JavaScriptParser.UnaryMinusExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAssignmentExpression(ctx: JavaScriptParser.AssignmentExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPostDecreaseExpression(ctx: JavaScriptParser.PostDecreaseExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitTypeofExpression(ctx: JavaScriptParser.TypeofExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitInstanceofExpression(ctx: JavaScriptParser.InstanceofExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitUnaryPlusExpression(ctx: JavaScriptParser.UnaryPlusExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitDeleteExpression(ctx: JavaScriptParser.DeleteExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitImportExpression(ctx: JavaScriptParser.ImportExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitEqualityExpression(ctx: JavaScriptParser.EqualityExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitBitXOrExpression(ctx: JavaScriptParser.BitXOrExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitSuperExpression(ctx: JavaScriptParser.SuperExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitMultiplicativeExpression(ctx: JavaScriptParser.MultiplicativeExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitBitShiftExpression(ctx: JavaScriptParser.BitShiftExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitParenthesizedExpression(ctx: JavaScriptParser.ParenthesizedExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAdditiveExpression(ctx: JavaScriptParser.AdditiveExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitRelationalExpression(ctx: JavaScriptParser.RelationalExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPostIncrementExpression(ctx: JavaScriptParser.PostIncrementExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitYieldExpression(ctx: JavaScriptParser.YieldExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitBitNotExpression(ctx: JavaScriptParser.BitNotExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitNewExpression(ctx: JavaScriptParser.NewExpressionContext): JsNew {
        val jsNewPlainIdentifier = ctx.identifier()?.let { makeRefNode(it.text) }
        val jsNewSingleExpression = ctx.singleExpressionImpl()?.let { visit<JsExpression>(it) }
        val jsNewExpression = when {
            jsNewPlainIdentifier != null -> jsNewPlainIdentifier
            else -> jsNewSingleExpression
        }

        val jsArguments = visitAll<JsExpression>(ctx.arguments().argument())
        return JsNew(jsNewExpression).apply {
            arguments.addAll(jsArguments)
        }
    }

    override fun visitLiteralExpression(ctx: JavaScriptParser.LiteralExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitArrayLiteralExpression(ctx: JavaScriptParser.ArrayLiteralExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitMemberDotExpression(ctx: JavaScriptParser.MemberDotExpressionContext): JsNode? {
        assert(ctx.QuestionMark() == null) { "Optional chain expressions are not supported yet" }
        assert(ctx.Hashtag() == null) { "Private member access expressions are not supported yet" }

        val jsLeft = visit<JsExpression>(ctx.singleExpressionImpl())
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
        val jsObjectExpr = visit<JsExpression>(ctx.singleExpressionImpl())
        val jsMemberExpr = visit<JsExpression>(ctx.expressionSequence())

        return JsArrayAccess(jsObjectExpr, jsMemberExpr)
    }

    override fun visitIdentifierExpression(ctx: JavaScriptParser.IdentifierExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitBitAndExpression(ctx: JavaScriptParser.BitAndExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitBitOrExpression(ctx: JavaScriptParser.BitOrExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAssignmentOperatorExpression(ctx: JavaScriptParser.AssignmentOperatorExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitVoidExpression(ctx: JavaScriptParser.VoidExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitCoalesceExpression(ctx: JavaScriptParser.CoalesceExpressionContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitInitializer(ctx: JavaScriptParser.InitializerContext): JsNode? {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun visitArrowFunctionBody(ctx: JavaScriptParser.ArrowFunctionBodyContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAssignmentOperator(ctx: JavaScriptParser.AssignmentOperatorContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitLiteral(ctx: JavaScriptParser.LiteralContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitTemplateStringLiteral(ctx: JavaScriptParser.TemplateStringLiteralContext): JsNode? {
        TODO("Template strings are not supported yet")
    }

    override fun visitTemplateStringAtom(ctx: JavaScriptParser.TemplateStringAtomContext): JsNode? {
        TODO("Template strings are not supported yet")
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
        TODO("Not yet implemented")
    }

    override fun visitSetter(ctx: JavaScriptParser.SetterContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitIdentifierName(ctx: JavaScriptParser.IdentifierNameContext): JsNode? {
        // There is no JS node that represents identifier name.
        return null
    }

    override fun visitIdentifier(ctx: JavaScriptParser.IdentifierContext): JsNode? {
        // There is no JS node that represents identifier.
        return null
    }

    override fun visitReservedWord(ctx: JavaScriptParser.ReservedWordContext): JsNode? {
        // There is no JS node that represents reserved word.
        return null
    }

    override fun visitKeyword(ctx: JavaScriptParser.KeywordContext): JsNode? {
        TODO()
//        when (ctx?.start?.type) {
//            JavaScriptParser.Break -> return JsBreak()
//            JavaScriptParser.Do -> return JsBreak()
//            JavaScriptParser.Instanceof -> return JsBreak()
//            JavaScriptParser.Typeof -> return JsBreak()
//            JavaScriptParser.Case -> return JsBreak()
//            JavaScriptParser.Else -> return JsBreak()
//            JavaScriptParser.New -> return JsBreak()
//            JavaScriptParser.Var -> return JsBreak()
//            JavaScriptParser.Catch -> return JsBreak()
//            JavaScriptParser.Finally -> return JsBreak()
//            JavaScriptParser.Return -> return JsBreak()
//            JavaScriptParser.Void -> return JsBreak()
//            JavaScriptParser.Continue -> return JsBreak()
//            JavaScriptParser.For -> return JsBreak()
//            JavaScriptParser.Switch -> return JsBreak()
//            JavaScriptParser.While -> return JsBreak()
//            JavaScriptParser.Debugger -> return JsBreak()
//            JavaScriptParser.Function_ -> return JsBreak()
//            JavaScriptParser.This -> return JsBreak()
//            JavaScriptParser.With -> return JsBreak()
//            JavaScriptParser.Default -> return JsBreak()
//            JavaScriptParser.If -> return JsBreak()
//            JavaScriptParser.Throw -> return JsBreak()
//            JavaScriptParser.Delete -> return JsBreak()
//            JavaScriptParser.In -> return JsBreak()
//            JavaScriptParser.Try -> return JsBreak()
//            JavaScriptParser.Class -> return JsBreak()
//            JavaScriptParser.Enum -> return JsBreak()
//            JavaScriptParser.Extends -> return JsBreak()
//            JavaScriptParser.Super -> return JsBreak()
//            JavaScriptParser.Const -> return JsBreak()
//            JavaScriptParser.Export -> return JsBreak()
//            JavaScriptParser.Import -> return JsBreak()
//            JavaScriptParser.Implements -> return JsBreak()
//            JavaScriptParser.NonStrictLet -> return visitLet_(ctx.let_())
//            JavaScriptParser.StrictLet -> return visitLet_(ctx.let_())
//            JavaScriptParser.Private -> return JsBreak()
//            JavaScriptParser.Public -> return JsBreak()
//            JavaScriptParser.Interface -> return JsBreak()
//            JavaScriptParser.Package -> return JsBreak()
//            JavaScriptParser.Protected -> return JsBreak()
//            JavaScriptParser.Static -> return JsBreak()
//            JavaScriptParser.Yield -> return JsBreak()
//            JavaScriptParser.YieldStar -> return JsBreak()
//            JavaScriptParser.Async -> return JsBreak()
//            JavaScriptParser.Await -> return JsBreak()
//            JavaScriptParser.From -> return JsBreak()
//            JavaScriptParser.As -> return JsBreak()
//            JavaScriptParser.Of -> return JsBreak()
//            // For complex nodes like 'let'
//            else -> return visitChildren(ctx)
//        }
    }

    override fun visitLet_(ctx: JavaScriptParser.Let_Context): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitEos(ctx: JavaScriptParser.EosContext): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitChildren(node: RuleNode?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitTerminal(node: TerminalNode?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitErrorNode(node: ErrorNode?): JsNode? {
        TODO("Not yet implemented")
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
                val jsParam = visit<JsParameter>(it)
                parameters.add(jsParam.applyLocation(fileName, it))
            }

            body = visit<JsBlock>(functionBody)

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

    private inline fun <reified T> visit(node: ParseTree): T =
        visit(node).expect<T>()

    private inline fun <reified T> visitAll(nodes: List<ParseTree>): List<T> =
        nodes.map { visit(it).expect<T>() }

    private inline fun <reified T> JsNode?.expect(): T {
        if (this !is T) throw AssertionError("Expected ${T::class}, got ${this?.javaClass}")
        return this
    }
}