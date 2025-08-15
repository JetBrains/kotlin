// Generated from JavaScriptParser.g4 by ANTLR 4.13.2
package org.jetbrains.kotlin.js.parser.antlr.generated;

import org.jetbrains.kotlin.js.parser.antlr.JavaScriptParserBase;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link JavaScriptParser}.
 */
public interface JavaScriptParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(JavaScriptParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(JavaScriptParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#sourceElement}.
	 * @param ctx the parse tree
	 */
	void enterSourceElement(JavaScriptParser.SourceElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#sourceElement}.
	 * @param ctx the parse tree
	 */
	void exitSourceElement(JavaScriptParser.SourceElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(JavaScriptParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(JavaScriptParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(JavaScriptParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(JavaScriptParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#statementList}.
	 * @param ctx the parse tree
	 */
	void enterStatementList(JavaScriptParser.StatementListContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#statementList}.
	 * @param ctx the parse tree
	 */
	void exitStatementList(JavaScriptParser.StatementListContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#importStatement}.
	 * @param ctx the parse tree
	 */
	void enterImportStatement(JavaScriptParser.ImportStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#importStatement}.
	 * @param ctx the parse tree
	 */
	void exitImportStatement(JavaScriptParser.ImportStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#importFromBlock}.
	 * @param ctx the parse tree
	 */
	void enterImportFromBlock(JavaScriptParser.ImportFromBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#importFromBlock}.
	 * @param ctx the parse tree
	 */
	void exitImportFromBlock(JavaScriptParser.ImportFromBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#importModuleItems}.
	 * @param ctx the parse tree
	 */
	void enterImportModuleItems(JavaScriptParser.ImportModuleItemsContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#importModuleItems}.
	 * @param ctx the parse tree
	 */
	void exitImportModuleItems(JavaScriptParser.ImportModuleItemsContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#importAliasName}.
	 * @param ctx the parse tree
	 */
	void enterImportAliasName(JavaScriptParser.ImportAliasNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#importAliasName}.
	 * @param ctx the parse tree
	 */
	void exitImportAliasName(JavaScriptParser.ImportAliasNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#moduleExportName}.
	 * @param ctx the parse tree
	 */
	void enterModuleExportName(JavaScriptParser.ModuleExportNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#moduleExportName}.
	 * @param ctx the parse tree
	 */
	void exitModuleExportName(JavaScriptParser.ModuleExportNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#importedBinding}.
	 * @param ctx the parse tree
	 */
	void enterImportedBinding(JavaScriptParser.ImportedBindingContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#importedBinding}.
	 * @param ctx the parse tree
	 */
	void exitImportedBinding(JavaScriptParser.ImportedBindingContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#importDefault}.
	 * @param ctx the parse tree
	 */
	void enterImportDefault(JavaScriptParser.ImportDefaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#importDefault}.
	 * @param ctx the parse tree
	 */
	void exitImportDefault(JavaScriptParser.ImportDefaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#importNamespace}.
	 * @param ctx the parse tree
	 */
	void enterImportNamespace(JavaScriptParser.ImportNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#importNamespace}.
	 * @param ctx the parse tree
	 */
	void exitImportNamespace(JavaScriptParser.ImportNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#importFrom}.
	 * @param ctx the parse tree
	 */
	void enterImportFrom(JavaScriptParser.ImportFromContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#importFrom}.
	 * @param ctx the parse tree
	 */
	void exitImportFrom(JavaScriptParser.ImportFromContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#aliasName}.
	 * @param ctx the parse tree
	 */
	void enterAliasName(JavaScriptParser.AliasNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#aliasName}.
	 * @param ctx the parse tree
	 */
	void exitAliasName(JavaScriptParser.AliasNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ExportDeclaration}
	 * labeled alternative in {@link JavaScriptParser#exportStatement}.
	 * @param ctx the parse tree
	 */
	void enterExportDeclaration(JavaScriptParser.ExportDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ExportDeclaration}
	 * labeled alternative in {@link JavaScriptParser#exportStatement}.
	 * @param ctx the parse tree
	 */
	void exitExportDeclaration(JavaScriptParser.ExportDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ExportDefaultDeclaration}
	 * labeled alternative in {@link JavaScriptParser#exportStatement}.
	 * @param ctx the parse tree
	 */
	void enterExportDefaultDeclaration(JavaScriptParser.ExportDefaultDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ExportDefaultDeclaration}
	 * labeled alternative in {@link JavaScriptParser#exportStatement}.
	 * @param ctx the parse tree
	 */
	void exitExportDefaultDeclaration(JavaScriptParser.ExportDefaultDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#exportFromBlock}.
	 * @param ctx the parse tree
	 */
	void enterExportFromBlock(JavaScriptParser.ExportFromBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#exportFromBlock}.
	 * @param ctx the parse tree
	 */
	void exitExportFromBlock(JavaScriptParser.ExportFromBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#exportModuleItems}.
	 * @param ctx the parse tree
	 */
	void enterExportModuleItems(JavaScriptParser.ExportModuleItemsContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#exportModuleItems}.
	 * @param ctx the parse tree
	 */
	void exitExportModuleItems(JavaScriptParser.ExportModuleItemsContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#exportAliasName}.
	 * @param ctx the parse tree
	 */
	void enterExportAliasName(JavaScriptParser.ExportAliasNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#exportAliasName}.
	 * @param ctx the parse tree
	 */
	void exitExportAliasName(JavaScriptParser.ExportAliasNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#declaration}.
	 * @param ctx the parse tree
	 */
	void enterDeclaration(JavaScriptParser.DeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#declaration}.
	 * @param ctx the parse tree
	 */
	void exitDeclaration(JavaScriptParser.DeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#variableStatement}.
	 * @param ctx the parse tree
	 */
	void enterVariableStatement(JavaScriptParser.VariableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#variableStatement}.
	 * @param ctx the parse tree
	 */
	void exitVariableStatement(JavaScriptParser.VariableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#variableDeclarationList}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarationList(JavaScriptParser.VariableDeclarationListContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#variableDeclarationList}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarationList(JavaScriptParser.VariableDeclarationListContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#variableDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaration(JavaScriptParser.VariableDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#variableDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaration(JavaScriptParser.VariableDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#emptyStatement_}.
	 * @param ctx the parse tree
	 */
	void enterEmptyStatement_(JavaScriptParser.EmptyStatement_Context ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#emptyStatement_}.
	 * @param ctx the parse tree
	 */
	void exitEmptyStatement_(JavaScriptParser.EmptyStatement_Context ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void enterExpressionStatement(JavaScriptParser.ExpressionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void exitExpressionStatement(JavaScriptParser.ExpressionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(JavaScriptParser.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(JavaScriptParser.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DoStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 */
	void enterDoStatement(JavaScriptParser.DoStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DoStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 */
	void exitDoStatement(JavaScriptParser.DoStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code WhileStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileStatement(JavaScriptParser.WhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WhileStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileStatement(JavaScriptParser.WhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ForStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 */
	void enterForStatement(JavaScriptParser.ForStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ForStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 */
	void exitForStatement(JavaScriptParser.ForStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ForInStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 */
	void enterForInStatement(JavaScriptParser.ForInStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ForInStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 */
	void exitForInStatement(JavaScriptParser.ForInStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ForOfStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 */
	void enterForOfStatement(JavaScriptParser.ForOfStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ForOfStatement}
	 * labeled alternative in {@link JavaScriptParser#iterationStatement}.
	 * @param ctx the parse tree
	 */
	void exitForOfStatement(JavaScriptParser.ForOfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#varModifier}.
	 * @param ctx the parse tree
	 */
	void enterVarModifier(JavaScriptParser.VarModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#varModifier}.
	 * @param ctx the parse tree
	 */
	void exitVarModifier(JavaScriptParser.VarModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#continueStatement}.
	 * @param ctx the parse tree
	 */
	void enterContinueStatement(JavaScriptParser.ContinueStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#continueStatement}.
	 * @param ctx the parse tree
	 */
	void exitContinueStatement(JavaScriptParser.ContinueStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#breakStatement}.
	 * @param ctx the parse tree
	 */
	void enterBreakStatement(JavaScriptParser.BreakStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#breakStatement}.
	 * @param ctx the parse tree
	 */
	void exitBreakStatement(JavaScriptParser.BreakStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStatement(JavaScriptParser.ReturnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStatement(JavaScriptParser.ReturnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#yieldStatement}.
	 * @param ctx the parse tree
	 */
	void enterYieldStatement(JavaScriptParser.YieldStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#yieldStatement}.
	 * @param ctx the parse tree
	 */
	void exitYieldStatement(JavaScriptParser.YieldStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#withStatement}.
	 * @param ctx the parse tree
	 */
	void enterWithStatement(JavaScriptParser.WithStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#withStatement}.
	 * @param ctx the parse tree
	 */
	void exitWithStatement(JavaScriptParser.WithStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#switchStatement}.
	 * @param ctx the parse tree
	 */
	void enterSwitchStatement(JavaScriptParser.SwitchStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#switchStatement}.
	 * @param ctx the parse tree
	 */
	void exitSwitchStatement(JavaScriptParser.SwitchStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#caseBlock}.
	 * @param ctx the parse tree
	 */
	void enterCaseBlock(JavaScriptParser.CaseBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#caseBlock}.
	 * @param ctx the parse tree
	 */
	void exitCaseBlock(JavaScriptParser.CaseBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#caseClauses}.
	 * @param ctx the parse tree
	 */
	void enterCaseClauses(JavaScriptParser.CaseClausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#caseClauses}.
	 * @param ctx the parse tree
	 */
	void exitCaseClauses(JavaScriptParser.CaseClausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#caseClause}.
	 * @param ctx the parse tree
	 */
	void enterCaseClause(JavaScriptParser.CaseClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#caseClause}.
	 * @param ctx the parse tree
	 */
	void exitCaseClause(JavaScriptParser.CaseClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#defaultClause}.
	 * @param ctx the parse tree
	 */
	void enterDefaultClause(JavaScriptParser.DefaultClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#defaultClause}.
	 * @param ctx the parse tree
	 */
	void exitDefaultClause(JavaScriptParser.DefaultClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#labelledStatement}.
	 * @param ctx the parse tree
	 */
	void enterLabelledStatement(JavaScriptParser.LabelledStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#labelledStatement}.
	 * @param ctx the parse tree
	 */
	void exitLabelledStatement(JavaScriptParser.LabelledStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#throwStatement}.
	 * @param ctx the parse tree
	 */
	void enterThrowStatement(JavaScriptParser.ThrowStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#throwStatement}.
	 * @param ctx the parse tree
	 */
	void exitThrowStatement(JavaScriptParser.ThrowStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#tryStatement}.
	 * @param ctx the parse tree
	 */
	void enterTryStatement(JavaScriptParser.TryStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#tryStatement}.
	 * @param ctx the parse tree
	 */
	void exitTryStatement(JavaScriptParser.TryStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#catchProduction}.
	 * @param ctx the parse tree
	 */
	void enterCatchProduction(JavaScriptParser.CatchProductionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#catchProduction}.
	 * @param ctx the parse tree
	 */
	void exitCatchProduction(JavaScriptParser.CatchProductionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#finallyProduction}.
	 * @param ctx the parse tree
	 */
	void enterFinallyProduction(JavaScriptParser.FinallyProductionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#finallyProduction}.
	 * @param ctx the parse tree
	 */
	void exitFinallyProduction(JavaScriptParser.FinallyProductionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#debuggerStatement}.
	 * @param ctx the parse tree
	 */
	void enterDebuggerStatement(JavaScriptParser.DebuggerStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#debuggerStatement}.
	 * @param ctx the parse tree
	 */
	void exitDebuggerStatement(JavaScriptParser.DebuggerStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDeclaration(JavaScriptParser.FunctionDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDeclaration(JavaScriptParser.FunctionDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassDeclaration(JavaScriptParser.ClassDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassDeclaration(JavaScriptParser.ClassDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#classTail}.
	 * @param ctx the parse tree
	 */
	void enterClassTail(JavaScriptParser.ClassTailContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#classTail}.
	 * @param ctx the parse tree
	 */
	void exitClassTail(JavaScriptParser.ClassTailContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#classElement}.
	 * @param ctx the parse tree
	 */
	void enterClassElement(JavaScriptParser.ClassElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#classElement}.
	 * @param ctx the parse tree
	 */
	void exitClassElement(JavaScriptParser.ClassElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#methodDefinition}.
	 * @param ctx the parse tree
	 */
	void enterMethodDefinition(JavaScriptParser.MethodDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#methodDefinition}.
	 * @param ctx the parse tree
	 */
	void exitMethodDefinition(JavaScriptParser.MethodDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#fieldDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFieldDefinition(JavaScriptParser.FieldDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#fieldDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFieldDefinition(JavaScriptParser.FieldDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#classElementName}.
	 * @param ctx the parse tree
	 */
	void enterClassElementName(JavaScriptParser.ClassElementNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#classElementName}.
	 * @param ctx the parse tree
	 */
	void exitClassElementName(JavaScriptParser.ClassElementNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#privateIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterPrivateIdentifier(JavaScriptParser.PrivateIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#privateIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitPrivateIdentifier(JavaScriptParser.PrivateIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameterList(JavaScriptParser.FormalParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameterList(JavaScriptParser.FormalParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#formalParameterArg}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameterArg(JavaScriptParser.FormalParameterArgContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#formalParameterArg}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameterArg(JavaScriptParser.FormalParameterArgContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#restParameterArg}.
	 * @param ctx the parse tree
	 */
	void enterRestParameterArg(JavaScriptParser.RestParameterArgContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#restParameterArg}.
	 * @param ctx the parse tree
	 */
	void exitRestParameterArg(JavaScriptParser.RestParameterArgContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#functionBody}.
	 * @param ctx the parse tree
	 */
	void enterFunctionBody(JavaScriptParser.FunctionBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#functionBody}.
	 * @param ctx the parse tree
	 */
	void exitFunctionBody(JavaScriptParser.FunctionBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#sourceElements}.
	 * @param ctx the parse tree
	 */
	void enterSourceElements(JavaScriptParser.SourceElementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#sourceElements}.
	 * @param ctx the parse tree
	 */
	void exitSourceElements(JavaScriptParser.SourceElementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#arrayLiteral}.
	 * @param ctx the parse tree
	 */
	void enterArrayLiteral(JavaScriptParser.ArrayLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#arrayLiteral}.
	 * @param ctx the parse tree
	 */
	void exitArrayLiteral(JavaScriptParser.ArrayLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#elementList}.
	 * @param ctx the parse tree
	 */
	void enterElementList(JavaScriptParser.ElementListContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#elementList}.
	 * @param ctx the parse tree
	 */
	void exitElementList(JavaScriptParser.ElementListContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#arrayElement}.
	 * @param ctx the parse tree
	 */
	void enterArrayElement(JavaScriptParser.ArrayElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#arrayElement}.
	 * @param ctx the parse tree
	 */
	void exitArrayElement(JavaScriptParser.ArrayElementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PropertyExpressionAssignment}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 */
	void enterPropertyExpressionAssignment(JavaScriptParser.PropertyExpressionAssignmentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PropertyExpressionAssignment}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 */
	void exitPropertyExpressionAssignment(JavaScriptParser.PropertyExpressionAssignmentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ComputedPropertyExpressionAssignment}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 */
	void enterComputedPropertyExpressionAssignment(JavaScriptParser.ComputedPropertyExpressionAssignmentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ComputedPropertyExpressionAssignment}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 */
	void exitComputedPropertyExpressionAssignment(JavaScriptParser.ComputedPropertyExpressionAssignmentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FunctionProperty}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 */
	void enterFunctionProperty(JavaScriptParser.FunctionPropertyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FunctionProperty}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 */
	void exitFunctionProperty(JavaScriptParser.FunctionPropertyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PropertyGetter}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 */
	void enterPropertyGetter(JavaScriptParser.PropertyGetterContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PropertyGetter}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 */
	void exitPropertyGetter(JavaScriptParser.PropertyGetterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PropertySetter}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 */
	void enterPropertySetter(JavaScriptParser.PropertySetterContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PropertySetter}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 */
	void exitPropertySetter(JavaScriptParser.PropertySetterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PropertyShorthand}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 */
	void enterPropertyShorthand(JavaScriptParser.PropertyShorthandContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PropertyShorthand}
	 * labeled alternative in {@link JavaScriptParser#propertyAssignment}.
	 * @param ctx the parse tree
	 */
	void exitPropertyShorthand(JavaScriptParser.PropertyShorthandContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#propertyName}.
	 * @param ctx the parse tree
	 */
	void enterPropertyName(JavaScriptParser.PropertyNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#propertyName}.
	 * @param ctx the parse tree
	 */
	void exitPropertyName(JavaScriptParser.PropertyNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(JavaScriptParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(JavaScriptParser.ArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(JavaScriptParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(JavaScriptParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#expressionSequence}.
	 * @param ctx the parse tree
	 */
	void enterExpressionSequence(JavaScriptParser.ExpressionSequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#expressionSequence}.
	 * @param ctx the parse tree
	 */
	void exitExpressionSequence(JavaScriptParser.ExpressionSequenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TemplateStringExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterTemplateStringExpression(JavaScriptParser.TemplateStringExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TemplateStringExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitTemplateStringExpression(JavaScriptParser.TemplateStringExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TernaryExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterTernaryExpression(JavaScriptParser.TernaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TernaryExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitTernaryExpression(JavaScriptParser.TernaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalAndExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalAndExpression(JavaScriptParser.LogicalAndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalAndExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalAndExpression(JavaScriptParser.LogicalAndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PowerExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterPowerExpression(JavaScriptParser.PowerExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PowerExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitPowerExpression(JavaScriptParser.PowerExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PreIncrementExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterPreIncrementExpression(JavaScriptParser.PreIncrementExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PreIncrementExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitPreIncrementExpression(JavaScriptParser.PreIncrementExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ObjectLiteralExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterObjectLiteralExpression(JavaScriptParser.ObjectLiteralExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ObjectLiteralExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitObjectLiteralExpression(JavaScriptParser.ObjectLiteralExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MetaExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterMetaExpression(JavaScriptParser.MetaExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MetaExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitMetaExpression(JavaScriptParser.MetaExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code InExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterInExpression(JavaScriptParser.InExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code InExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitInExpression(JavaScriptParser.InExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalOrExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalOrExpression(JavaScriptParser.LogicalOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalOrExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalOrExpression(JavaScriptParser.LogicalOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OptionalChainExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterOptionalChainExpression(JavaScriptParser.OptionalChainExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OptionalChainExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitOptionalChainExpression(JavaScriptParser.OptionalChainExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterNotExpression(JavaScriptParser.NotExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitNotExpression(JavaScriptParser.NotExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PreDecreaseExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterPreDecreaseExpression(JavaScriptParser.PreDecreaseExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PreDecreaseExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitPreDecreaseExpression(JavaScriptParser.PreDecreaseExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArgumentsExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterArgumentsExpression(JavaScriptParser.ArgumentsExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArgumentsExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitArgumentsExpression(JavaScriptParser.ArgumentsExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AwaitExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterAwaitExpression(JavaScriptParser.AwaitExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AwaitExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitAwaitExpression(JavaScriptParser.AwaitExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ThisExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterThisExpression(JavaScriptParser.ThisExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ThisExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitThisExpression(JavaScriptParser.ThisExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FunctionExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionExpression(JavaScriptParser.FunctionExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FunctionExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionExpression(JavaScriptParser.FunctionExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code UnaryMinusExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryMinusExpression(JavaScriptParser.UnaryMinusExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code UnaryMinusExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryMinusExpression(JavaScriptParser.UnaryMinusExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AssignmentExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentExpression(JavaScriptParser.AssignmentExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AssignmentExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentExpression(JavaScriptParser.AssignmentExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PostDecreaseExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostDecreaseExpression(JavaScriptParser.PostDecreaseExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PostDecreaseExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostDecreaseExpression(JavaScriptParser.PostDecreaseExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TypeofExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterTypeofExpression(JavaScriptParser.TypeofExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TypeofExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitTypeofExpression(JavaScriptParser.TypeofExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code InstanceofExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterInstanceofExpression(JavaScriptParser.InstanceofExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code InstanceofExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitInstanceofExpression(JavaScriptParser.InstanceofExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code UnaryPlusExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryPlusExpression(JavaScriptParser.UnaryPlusExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code UnaryPlusExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryPlusExpression(JavaScriptParser.UnaryPlusExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DeleteExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterDeleteExpression(JavaScriptParser.DeleteExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DeleteExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitDeleteExpression(JavaScriptParser.DeleteExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ImportExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterImportExpression(JavaScriptParser.ImportExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ImportExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitImportExpression(JavaScriptParser.ImportExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code EqualityExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpression(JavaScriptParser.EqualityExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code EqualityExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpression(JavaScriptParser.EqualityExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BitXOrExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterBitXOrExpression(JavaScriptParser.BitXOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BitXOrExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitBitXOrExpression(JavaScriptParser.BitXOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SuperExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterSuperExpression(JavaScriptParser.SuperExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SuperExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitSuperExpression(JavaScriptParser.SuperExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MultiplicativeExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpression(JavaScriptParser.MultiplicativeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MultiplicativeExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpression(JavaScriptParser.MultiplicativeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BitShiftExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterBitShiftExpression(JavaScriptParser.BitShiftExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BitShiftExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitBitShiftExpression(JavaScriptParser.BitShiftExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ParenthesizedExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedExpression(JavaScriptParser.ParenthesizedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ParenthesizedExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedExpression(JavaScriptParser.ParenthesizedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AdditiveExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpression(JavaScriptParser.AdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AdditiveExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpression(JavaScriptParser.AdditiveExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RelationalExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpression(JavaScriptParser.RelationalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RelationalExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpression(JavaScriptParser.RelationalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PostIncrementExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostIncrementExpression(JavaScriptParser.PostIncrementExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PostIncrementExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostIncrementExpression(JavaScriptParser.PostIncrementExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code YieldExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterYieldExpression(JavaScriptParser.YieldExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code YieldExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitYieldExpression(JavaScriptParser.YieldExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BitNotExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterBitNotExpression(JavaScriptParser.BitNotExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BitNotExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitBitNotExpression(JavaScriptParser.BitNotExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NewExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterNewExpression(JavaScriptParser.NewExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NewExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitNewExpression(JavaScriptParser.NewExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LiteralExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterLiteralExpression(JavaScriptParser.LiteralExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LiteralExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitLiteralExpression(JavaScriptParser.LiteralExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArrayLiteralExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterArrayLiteralExpression(JavaScriptParser.ArrayLiteralExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArrayLiteralExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitArrayLiteralExpression(JavaScriptParser.ArrayLiteralExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MemberDotExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterMemberDotExpression(JavaScriptParser.MemberDotExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MemberDotExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitMemberDotExpression(JavaScriptParser.MemberDotExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ClassExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterClassExpression(JavaScriptParser.ClassExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ClassExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitClassExpression(JavaScriptParser.ClassExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MemberIndexExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterMemberIndexExpression(JavaScriptParser.MemberIndexExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MemberIndexExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitMemberIndexExpression(JavaScriptParser.MemberIndexExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IdentifierExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierExpression(JavaScriptParser.IdentifierExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IdentifierExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierExpression(JavaScriptParser.IdentifierExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BitAndExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterBitAndExpression(JavaScriptParser.BitAndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BitAndExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitBitAndExpression(JavaScriptParser.BitAndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BitOrExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterBitOrExpression(JavaScriptParser.BitOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BitOrExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitBitOrExpression(JavaScriptParser.BitOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AssignmentOperatorExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentOperatorExpression(JavaScriptParser.AssignmentOperatorExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AssignmentOperatorExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentOperatorExpression(JavaScriptParser.AssignmentOperatorExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code VoidExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterVoidExpression(JavaScriptParser.VoidExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code VoidExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitVoidExpression(JavaScriptParser.VoidExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CoalesceExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoalesceExpression(JavaScriptParser.CoalesceExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CoalesceExpression}
	 * labeled alternative in {@link JavaScriptParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoalesceExpression(JavaScriptParser.CoalesceExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#initializer}.
	 * @param ctx the parse tree
	 */
	void enterInitializer(JavaScriptParser.InitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#initializer}.
	 * @param ctx the parse tree
	 */
	void exitInitializer(JavaScriptParser.InitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#assignable}.
	 * @param ctx the parse tree
	 */
	void enterAssignable(JavaScriptParser.AssignableContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#assignable}.
	 * @param ctx the parse tree
	 */
	void exitAssignable(JavaScriptParser.AssignableContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#objectLiteral}.
	 * @param ctx the parse tree
	 */
	void enterObjectLiteral(JavaScriptParser.ObjectLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#objectLiteral}.
	 * @param ctx the parse tree
	 */
	void exitObjectLiteral(JavaScriptParser.ObjectLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NamedFunction}
	 * labeled alternative in {@link JavaScriptParser#anonymousFunction}.
	 * @param ctx the parse tree
	 */
	void enterNamedFunction(JavaScriptParser.NamedFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NamedFunction}
	 * labeled alternative in {@link JavaScriptParser#anonymousFunction}.
	 * @param ctx the parse tree
	 */
	void exitNamedFunction(JavaScriptParser.NamedFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AnonymousFunctionDecl}
	 * labeled alternative in {@link JavaScriptParser#anonymousFunction}.
	 * @param ctx the parse tree
	 */
	void enterAnonymousFunctionDecl(JavaScriptParser.AnonymousFunctionDeclContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AnonymousFunctionDecl}
	 * labeled alternative in {@link JavaScriptParser#anonymousFunction}.
	 * @param ctx the parse tree
	 */
	void exitAnonymousFunctionDecl(JavaScriptParser.AnonymousFunctionDeclContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArrowFunction}
	 * labeled alternative in {@link JavaScriptParser#anonymousFunction}.
	 * @param ctx the parse tree
	 */
	void enterArrowFunction(JavaScriptParser.ArrowFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArrowFunction}
	 * labeled alternative in {@link JavaScriptParser#anonymousFunction}.
	 * @param ctx the parse tree
	 */
	void exitArrowFunction(JavaScriptParser.ArrowFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#arrowFunctionParameters}.
	 * @param ctx the parse tree
	 */
	void enterArrowFunctionParameters(JavaScriptParser.ArrowFunctionParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#arrowFunctionParameters}.
	 * @param ctx the parse tree
	 */
	void exitArrowFunctionParameters(JavaScriptParser.ArrowFunctionParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#arrowFunctionBody}.
	 * @param ctx the parse tree
	 */
	void enterArrowFunctionBody(JavaScriptParser.ArrowFunctionBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#arrowFunctionBody}.
	 * @param ctx the parse tree
	 */
	void exitArrowFunctionBody(JavaScriptParser.ArrowFunctionBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#assignmentOperator}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentOperator(JavaScriptParser.AssignmentOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#assignmentOperator}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentOperator(JavaScriptParser.AssignmentOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(JavaScriptParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(JavaScriptParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#templateStringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterTemplateStringLiteral(JavaScriptParser.TemplateStringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#templateStringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitTemplateStringLiteral(JavaScriptParser.TemplateStringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#templateStringAtom}.
	 * @param ctx the parse tree
	 */
	void enterTemplateStringAtom(JavaScriptParser.TemplateStringAtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#templateStringAtom}.
	 * @param ctx the parse tree
	 */
	void exitTemplateStringAtom(JavaScriptParser.TemplateStringAtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#numericLiteral}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(JavaScriptParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#numericLiteral}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(JavaScriptParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#bigintLiteral}.
	 * @param ctx the parse tree
	 */
	void enterBigintLiteral(JavaScriptParser.BigintLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#bigintLiteral}.
	 * @param ctx the parse tree
	 */
	void exitBigintLiteral(JavaScriptParser.BigintLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#getter}.
	 * @param ctx the parse tree
	 */
	void enterGetter(JavaScriptParser.GetterContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#getter}.
	 * @param ctx the parse tree
	 */
	void exitGetter(JavaScriptParser.GetterContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#setter}.
	 * @param ctx the parse tree
	 */
	void enterSetter(JavaScriptParser.SetterContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#setter}.
	 * @param ctx the parse tree
	 */
	void exitSetter(JavaScriptParser.SetterContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#identifierName}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierName(JavaScriptParser.IdentifierNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#identifierName}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierName(JavaScriptParser.IdentifierNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(JavaScriptParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(JavaScriptParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#reservedWord}.
	 * @param ctx the parse tree
	 */
	void enterReservedWord(JavaScriptParser.ReservedWordContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#reservedWord}.
	 * @param ctx the parse tree
	 */
	void exitReservedWord(JavaScriptParser.ReservedWordContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#keyword}.
	 * @param ctx the parse tree
	 */
	void enterKeyword(JavaScriptParser.KeywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#keyword}.
	 * @param ctx the parse tree
	 */
	void exitKeyword(JavaScriptParser.KeywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#let_}.
	 * @param ctx the parse tree
	 */
	void enterLet_(JavaScriptParser.Let_Context ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#let_}.
	 * @param ctx the parse tree
	 */
	void exitLet_(JavaScriptParser.Let_Context ctx);
	/**
	 * Enter a parse tree produced by {@link JavaScriptParser#eos}.
	 * @param ctx the parse tree
	 */
	void enterEos(JavaScriptParser.EosContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaScriptParser#eos}.
	 * @param ctx the parse tree
	 */
	void exitEos(JavaScriptParser.EosContext ctx);
}