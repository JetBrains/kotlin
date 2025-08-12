// Generated from JavaScriptParser.g4 by ANTLR 4.13.2
package org.jetbrains.kotlin.js.parser.antlr.generated;

import org.jetbrains.kotlin.js.parser.antlr.JavaScriptParserBase;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link JavaScriptParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface JavaScriptParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(JavaScriptParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#sourceElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSourceElement(JavaScriptParser.SourceElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(JavaScriptParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(JavaScriptParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#statementList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatementList(JavaScriptParser.StatementListContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#importStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportStatement(JavaScriptParser.ImportStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#importFromBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportFromBlock(JavaScriptParser.ImportFromBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#importModuleItems}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportModuleItems(JavaScriptParser.ImportModuleItemsContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#importAliasName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportAliasName(JavaScriptParser.ImportAliasNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#moduleExportName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModuleExportName(JavaScriptParser.ModuleExportNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#importedBinding}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportedBinding(JavaScriptParser.ImportedBindingContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#importDefault}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportDefault(JavaScriptParser.ImportDefaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#importNamespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportNamespace(JavaScriptParser.ImportNamespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#importFrom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportFrom(JavaScriptParser.ImportFromContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#aliasName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasName(JavaScriptParser.AliasNameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ExportDeclaration}
	 * labeled alternative in {@link JavaScriptParser#exportStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExportDeclaration(JavaScriptParser.ExportDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ExportDefaultDeclaration}
	 * labeled alternative in {@link JavaScriptParser#exportStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExportDefaultDeclaration(JavaScriptParser.ExportDefaultDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#exportFromBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExportFromBlock(JavaScriptParser.ExportFromBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#exportModuleItems}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExportModuleItems(JavaScriptParser.ExportModuleItemsContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#exportAliasName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExportAliasName(JavaScriptParser.ExportAliasNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclaration(JavaScriptParser.DeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#variableStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableStatement(JavaScriptParser.VariableStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#variableDeclarationList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclarationList(JavaScriptParser.VariableDeclarationListContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#variableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaration(JavaScriptParser.VariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#emptyStatement_}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmptyStatement_(JavaScriptParser.EmptyStatement_Context ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#expressionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionStatement(JavaScriptParser.ExpressionStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#ifStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(JavaScriptParser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DoStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoStatement(JavaScriptParser.DoStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code WhileStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStatement(JavaScriptParser.WhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ForStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForStatement(JavaScriptParser.ForStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ForInStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForInStatement(JavaScriptParser.ForInStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ForOfStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForOfStatement(JavaScriptParser.ForOfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#varModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarModifier(JavaScriptParser.VarModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#continueStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinueStatement(JavaScriptParser.ContinueStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#breakStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreakStatement(JavaScriptParser.BreakStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#returnStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStatement(JavaScriptParser.ReturnStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#yieldStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYieldStatement(JavaScriptParser.YieldStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#withStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithStatement(JavaScriptParser.WithStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#switchStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchStatement(JavaScriptParser.SwitchStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#caseBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseBlock(JavaScriptParser.CaseBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#caseClauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseClauses(JavaScriptParser.CaseClausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#caseClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseClause(JavaScriptParser.CaseClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#defaultClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultClause(JavaScriptParser.DefaultClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#labelledStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelledStatement(JavaScriptParser.LabelledStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#throwStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrowStatement(JavaScriptParser.ThrowStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#tryStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTryStatement(JavaScriptParser.TryStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#catchProduction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchProduction(JavaScriptParser.CatchProductionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#finallyProduction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFinallyProduction(JavaScriptParser.FinallyProductionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#debuggerStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDebuggerStatement(JavaScriptParser.DebuggerStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#functionDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDeclaration(JavaScriptParser.FunctionDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#classDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassDeclaration(JavaScriptParser.ClassDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#classTail}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassTail(JavaScriptParser.ClassTailContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#classElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassElement(JavaScriptParser.ClassElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#methodDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodDefinition(JavaScriptParser.MethodDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#fieldDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldDefinition(JavaScriptParser.FieldDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#classElementName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassElementName(JavaScriptParser.ClassElementNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#privateIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrivateIdentifier(JavaScriptParser.PrivateIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#formalParameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameterList(JavaScriptParser.FormalParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#formalParameterArg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameterArg(JavaScriptParser.FormalParameterArgContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#lastFormalParameterArg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLastFormalParameterArg(JavaScriptParser.LastFormalParameterArgContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#functionBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionBody(JavaScriptParser.FunctionBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#sourceElements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSourceElements(JavaScriptParser.SourceElementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#arrayLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayLiteral(JavaScriptParser.ArrayLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#elementList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementList(JavaScriptParser.ElementListContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#arrayElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayElement(JavaScriptParser.ArrayElementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PropertyExpressionAssignment}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyExpressionAssignment(JavaScriptParser.PropertyExpressionAssignmentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ComputedPropertyExpressionAssignment}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComputedPropertyExpressionAssignment(JavaScriptParser.ComputedPropertyExpressionAssignmentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FunctionProperty}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionProperty(JavaScriptParser.FunctionPropertyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PropertyGetter}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyGetter(JavaScriptParser.PropertyGetterContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PropertySetter}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertySetter(JavaScriptParser.PropertySetterContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PropertyShorthand}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyShorthand(JavaScriptParser.PropertyShorthandContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#propertyName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyName(JavaScriptParser.PropertyNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(JavaScriptParser.ArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(JavaScriptParser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#expressionSequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionSequence(JavaScriptParser.ExpressionSequenceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TemplateStringExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplateStringExpression(JavaScriptParser.TemplateStringExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TernaryExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernaryExpression(JavaScriptParser.TernaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LogicalAndExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalAndExpression(JavaScriptParser.LogicalAndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PowerExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPowerExpression(JavaScriptParser.PowerExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PreIncrementExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPreIncrementExpression(JavaScriptParser.PreIncrementExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ObjectLiteralExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectLiteralExpression(JavaScriptParser.ObjectLiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MetaExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMetaExpression(JavaScriptParser.MetaExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code InExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInExpression(JavaScriptParser.InExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LogicalOrExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalOrExpression(JavaScriptParser.LogicalOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OptionalChainExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptionalChainExpression(JavaScriptParser.OptionalChainExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotExpression(JavaScriptParser.NotExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PreDecreaseExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPreDecreaseExpression(JavaScriptParser.PreDecreaseExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ArgumentsExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentsExpression(JavaScriptParser.ArgumentsExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AwaitExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAwaitExpression(JavaScriptParser.AwaitExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ThisExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThisExpression(JavaScriptParser.ThisExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FunctionExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionExpression(JavaScriptParser.FunctionExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UnaryMinusExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryMinusExpression(JavaScriptParser.UnaryMinusExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AssignmentExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentExpression(JavaScriptParser.AssignmentExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PostDecreaseExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostDecreaseExpression(JavaScriptParser.PostDecreaseExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TypeofExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeofExpression(JavaScriptParser.TypeofExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code InstanceofExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstanceofExpression(JavaScriptParser.InstanceofExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UnaryPlusExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryPlusExpression(JavaScriptParser.UnaryPlusExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DeleteExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeleteExpression(JavaScriptParser.DeleteExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ImportExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportExpression(JavaScriptParser.ImportExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code EqualityExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityExpression(JavaScriptParser.EqualityExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BitXOrExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitXOrExpression(JavaScriptParser.BitXOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SuperExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperExpression(JavaScriptParser.SuperExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MultiplicativeExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpression(JavaScriptParser.MultiplicativeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BitShiftExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitShiftExpression(JavaScriptParser.BitShiftExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ParenthesizedExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedExpression(JavaScriptParser.ParenthesizedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AdditiveExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpression(JavaScriptParser.AdditiveExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code RelationalExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExpression(JavaScriptParser.RelationalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PostIncrementExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostIncrementExpression(JavaScriptParser.PostIncrementExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code YieldExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYieldExpression(JavaScriptParser.YieldExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BitNotExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitNotExpression(JavaScriptParser.BitNotExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NewExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNewExpression(JavaScriptParser.NewExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LiteralExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralExpression(JavaScriptParser.LiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ArrayLiteralExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayLiteralExpression(JavaScriptParser.ArrayLiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MemberDotExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberDotExpression(JavaScriptParser.MemberDotExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ClassExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassExpression(JavaScriptParser.ClassExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MemberIndexExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberIndexExpression(JavaScriptParser.MemberIndexExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IdentifierExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierExpression(JavaScriptParser.IdentifierExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BitAndExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitAndExpression(JavaScriptParser.BitAndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BitOrExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitOrExpression(JavaScriptParser.BitOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AssignmentOperatorExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentOperatorExpression(JavaScriptParser.AssignmentOperatorExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code VoidExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVoidExpression(JavaScriptParser.VoidExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CoalesceExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoalesceExpression(JavaScriptParser.CoalesceExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializer(JavaScriptParser.InitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#assignable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignable(JavaScriptParser.AssignableContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#objectLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectLiteral(JavaScriptParser.ObjectLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NamedFunction}
	 * labeled alternative in {@link JavaScriptParser#anonymousFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedFunction(JavaScriptParser.NamedFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AnonymousFunctionDecl}
	 * labeled alternative in {@link JavaScriptParser#anonymousFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnonymousFunctionDecl(JavaScriptParser.AnonymousFunctionDeclContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ArrowFunction}
	 * labeled alternative in {@link JavaScriptParser#anonymousFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrowFunction(JavaScriptParser.ArrowFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#arrowFunctionParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrowFunctionParameters(JavaScriptParser.ArrowFunctionParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#arrowFunctionBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrowFunctionBody(JavaScriptParser.ArrowFunctionBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#assignmentOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentOperator(JavaScriptParser.AssignmentOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(JavaScriptParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#templateStringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplateStringLiteral(JavaScriptParser.TemplateStringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#templateStringAtom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplateStringAtom(JavaScriptParser.TemplateStringAtomContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#numericLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteral(JavaScriptParser.NumericLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#bigintLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBigintLiteral(JavaScriptParser.BigintLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#getter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetter(JavaScriptParser.GetterContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#setter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetter(JavaScriptParser.SetterContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#identifierName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierName(JavaScriptParser.IdentifierNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(JavaScriptParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#reservedWord}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReservedWord(JavaScriptParser.ReservedWordContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyword(JavaScriptParser.KeywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#let_}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLet_(JavaScriptParser.Let_Context ctx);
	/**
	 * Visit a parse tree produced by {@link JavaScriptParser#eos}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEos(JavaScriptParser.EosContext ctx);
}