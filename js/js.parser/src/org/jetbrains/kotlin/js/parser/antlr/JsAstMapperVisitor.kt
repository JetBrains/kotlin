/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import JavaScriptParserVisitor
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.RuleNode
import org.antlr.v4.runtime.tree.TerminalNode
import org.jetbrains.kotlin.js.backend.ast.JsBreak
import org.jetbrains.kotlin.js.backend.ast.JsLocation
import org.jetbrains.kotlin.js.backend.ast.JsNode

class JsAstMapper {
}

class JsAstMapperVisitor(private val offsetLocation: JsLocation) : JavaScriptParserVisitor<JsNode> {
    override fun visitProgram(ctx: JavaScriptParser.ProgramContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitSourceElement(ctx: JavaScriptParser.SourceElementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitStatement(ctx: JavaScriptParser.StatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitBlock(ctx: JavaScriptParser.BlockContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitStatementList(ctx: JavaScriptParser.StatementListContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitImportStatement(ctx: JavaScriptParser.ImportStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitImportFromBlock(ctx: JavaScriptParser.ImportFromBlockContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitImportModuleItems(ctx: JavaScriptParser.ImportModuleItemsContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitImportAliasName(ctx: JavaScriptParser.ImportAliasNameContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitModuleExportName(ctx: JavaScriptParser.ModuleExportNameContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitImportedBinding(ctx: JavaScriptParser.ImportedBindingContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitImportDefault(ctx: JavaScriptParser.ImportDefaultContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitImportNamespace(ctx: JavaScriptParser.ImportNamespaceContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitImportFrom(ctx: JavaScriptParser.ImportFromContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAliasName(ctx: JavaScriptParser.AliasNameContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitExportDeclaration(ctx: JavaScriptParser.ExportDeclarationContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitExportDefaultDeclaration(ctx: JavaScriptParser.ExportDefaultDeclarationContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitExportFromBlock(ctx: JavaScriptParser.ExportFromBlockContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitExportModuleItems(ctx: JavaScriptParser.ExportModuleItemsContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitExportAliasName(ctx: JavaScriptParser.ExportAliasNameContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitDeclaration(ctx: JavaScriptParser.DeclarationContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitVariableStatement(ctx: JavaScriptParser.VariableStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitVariableDeclarationList(ctx: JavaScriptParser.VariableDeclarationListContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitVariableDeclaration(ctx: JavaScriptParser.VariableDeclarationContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitEmptyStatement_(ctx: JavaScriptParser.EmptyStatement_Context?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitExpressionStatement(ctx: JavaScriptParser.ExpressionStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitIfStatement(ctx: JavaScriptParser.IfStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitDoStatement(ctx: JavaScriptParser.DoStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitWhileStatement(ctx: JavaScriptParser.WhileStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitForStatement(ctx: JavaScriptParser.ForStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitForInStatement(ctx: JavaScriptParser.ForInStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitForOfStatement(ctx: JavaScriptParser.ForOfStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitVarModifier(ctx: JavaScriptParser.VarModifierContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitContinueStatement(ctx: JavaScriptParser.ContinueStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitBreakStatement(ctx: JavaScriptParser.BreakStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitReturnStatement(ctx: JavaScriptParser.ReturnStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitYieldStatement(ctx: JavaScriptParser.YieldStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitWithStatement(ctx: JavaScriptParser.WithStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitSwitchStatement(ctx: JavaScriptParser.SwitchStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitCaseBlock(ctx: JavaScriptParser.CaseBlockContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitCaseClauses(ctx: JavaScriptParser.CaseClausesContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitCaseClause(ctx: JavaScriptParser.CaseClauseContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitDefaultClause(ctx: JavaScriptParser.DefaultClauseContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitLabelledStatement(ctx: JavaScriptParser.LabelledStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitThrowStatement(ctx: JavaScriptParser.ThrowStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitTryStatement(ctx: JavaScriptParser.TryStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitCatchProduction(ctx: JavaScriptParser.CatchProductionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitFinallyProduction(ctx: JavaScriptParser.FinallyProductionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitDebuggerStatement(ctx: JavaScriptParser.DebuggerStatementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitFunctionDeclaration(ctx: JavaScriptParser.FunctionDeclarationContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitClassDeclaration(ctx: JavaScriptParser.ClassDeclarationContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitClassTail(ctx: JavaScriptParser.ClassTailContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitClassElement(ctx: JavaScriptParser.ClassElementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitMethodDefinition(ctx: JavaScriptParser.MethodDefinitionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitFieldDefinition(ctx: JavaScriptParser.FieldDefinitionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitClassElementName(ctx: JavaScriptParser.ClassElementNameContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPrivateIdentifier(ctx: JavaScriptParser.PrivateIdentifierContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitFormalParameterList(ctx: JavaScriptParser.FormalParameterListContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitFormalParameterArg(ctx: JavaScriptParser.FormalParameterArgContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitLastFormalParameterArg(ctx: JavaScriptParser.LastFormalParameterArgContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitFunctionBody(ctx: JavaScriptParser.FunctionBodyContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitSourceElements(ctx: JavaScriptParser.SourceElementsContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitArrayLiteral(ctx: JavaScriptParser.ArrayLiteralContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitElementList(ctx: JavaScriptParser.ElementListContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitArrayElement(ctx: JavaScriptParser.ArrayElementContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPropertyExpressionAssignment(ctx: JavaScriptParser.PropertyExpressionAssignmentContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitComputedPropertyExpressionAssignment(ctx: JavaScriptParser.ComputedPropertyExpressionAssignmentContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitFunctionProperty(ctx: JavaScriptParser.FunctionPropertyContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPropertyGetter(ctx: JavaScriptParser.PropertyGetterContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPropertySetter(ctx: JavaScriptParser.PropertySetterContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPropertyShorthand(ctx: JavaScriptParser.PropertyShorthandContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPropertyName(ctx: JavaScriptParser.PropertyNameContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitArguments(ctx: JavaScriptParser.ArgumentsContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitArgument(ctx: JavaScriptParser.ArgumentContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitExpressionSequence(ctx: JavaScriptParser.ExpressionSequenceContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitTemplateStringExpression(ctx: JavaScriptParser.TemplateStringExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitTernaryExpression(ctx: JavaScriptParser.TernaryExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitLogicalAndExpression(ctx: JavaScriptParser.LogicalAndExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPowerExpression(ctx: JavaScriptParser.PowerExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPreIncrementExpression(ctx: JavaScriptParser.PreIncrementExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitObjectLiteralExpression(ctx: JavaScriptParser.ObjectLiteralExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitMetaExpression(ctx: JavaScriptParser.MetaExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitInExpression(ctx: JavaScriptParser.InExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitLogicalOrExpression(ctx: JavaScriptParser.LogicalOrExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitOptionalChainExpression(ctx: JavaScriptParser.OptionalChainExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitNotExpression(ctx: JavaScriptParser.NotExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPreDecreaseExpression(ctx: JavaScriptParser.PreDecreaseExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitArgumentsExpression(ctx: JavaScriptParser.ArgumentsExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAwaitExpression(ctx: JavaScriptParser.AwaitExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitThisExpression(ctx: JavaScriptParser.ThisExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitFunctionExpression(ctx: JavaScriptParser.FunctionExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitUnaryMinusExpression(ctx: JavaScriptParser.UnaryMinusExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAssignmentExpression(ctx: JavaScriptParser.AssignmentExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPostDecreaseExpression(ctx: JavaScriptParser.PostDecreaseExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitTypeofExpression(ctx: JavaScriptParser.TypeofExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitInstanceofExpression(ctx: JavaScriptParser.InstanceofExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitUnaryPlusExpression(ctx: JavaScriptParser.UnaryPlusExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitDeleteExpression(ctx: JavaScriptParser.DeleteExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitImportExpression(ctx: JavaScriptParser.ImportExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitEqualityExpression(ctx: JavaScriptParser.EqualityExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitBitXOrExpression(ctx: JavaScriptParser.BitXOrExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitSuperExpression(ctx: JavaScriptParser.SuperExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitMultiplicativeExpression(ctx: JavaScriptParser.MultiplicativeExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitBitShiftExpression(ctx: JavaScriptParser.BitShiftExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitParenthesizedExpression(ctx: JavaScriptParser.ParenthesizedExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAdditiveExpression(ctx: JavaScriptParser.AdditiveExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitRelationalExpression(ctx: JavaScriptParser.RelationalExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitPostIncrementExpression(ctx: JavaScriptParser.PostIncrementExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitYieldExpression(ctx: JavaScriptParser.YieldExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitBitNotExpression(ctx: JavaScriptParser.BitNotExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitNewExpression(ctx: JavaScriptParser.NewExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitLiteralExpression(ctx: JavaScriptParser.LiteralExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitArrayLiteralExpression(ctx: JavaScriptParser.ArrayLiteralExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitMemberDotExpression(ctx: JavaScriptParser.MemberDotExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitClassExpression(ctx: JavaScriptParser.ClassExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitMemberIndexExpression(ctx: JavaScriptParser.MemberIndexExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitIdentifierExpression(ctx: JavaScriptParser.IdentifierExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitBitAndExpression(ctx: JavaScriptParser.BitAndExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitBitOrExpression(ctx: JavaScriptParser.BitOrExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAssignmentOperatorExpression(ctx: JavaScriptParser.AssignmentOperatorExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitVoidExpression(ctx: JavaScriptParser.VoidExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitCoalesceExpression(ctx: JavaScriptParser.CoalesceExpressionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitInitializer(ctx: JavaScriptParser.InitializerContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAssignable(ctx: JavaScriptParser.AssignableContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitObjectLiteral(ctx: JavaScriptParser.ObjectLiteralContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitNamedFunction(ctx: JavaScriptParser.NamedFunctionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAnonymousFunctionDecl(ctx: JavaScriptParser.AnonymousFunctionDeclContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitArrowFunction(ctx: JavaScriptParser.ArrowFunctionContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitArrowFunctionParameters(ctx: JavaScriptParser.ArrowFunctionParametersContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitArrowFunctionBody(ctx: JavaScriptParser.ArrowFunctionBodyContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitAssignmentOperator(ctx: JavaScriptParser.AssignmentOperatorContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitLiteral(ctx: JavaScriptParser.LiteralContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitTemplateStringLiteral(ctx: JavaScriptParser.TemplateStringLiteralContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitTemplateStringAtom(ctx: JavaScriptParser.TemplateStringAtomContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitNumericLiteral(ctx: JavaScriptParser.NumericLiteralContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitBigintLiteral(ctx: JavaScriptParser.BigintLiteralContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitGetter(ctx: JavaScriptParser.GetterContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitSetter(ctx: JavaScriptParser.SetterContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitIdentifierName(ctx: JavaScriptParser.IdentifierNameContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitIdentifier(ctx: JavaScriptParser.IdentifierContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitReservedWord(ctx: JavaScriptParser.ReservedWordContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitKeyword(ctx: JavaScriptParser.KeywordContext?): JsNode? {
        when (ctx?.start?.type) {
            JavaScriptParser.Break -> return JsBreak()
        }
    }

    override fun visitLet_(ctx: JavaScriptParser.Let_Context?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visitEos(ctx: JavaScriptParser.EosContext?): JsNode? {
        TODO("Not yet implemented")
    }

    override fun visit(tree: ParseTree?): JsNode? {
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

    private fun ParserRuleContext.getLocation(): JsLocation {
        val startLine = start.line
        val startColumn = start.charPositionInLine

        offsetLocation.copy(startLine = startLine, )
        return JsLocation(offsetLocation.file, offsetLocation.startLine + startLine, startColumn)
    }
}