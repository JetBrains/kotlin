// Generated from JavaScriptParser.g4 by ANTLR 4.13.2
package org.jetbrains.kotlin.js.parser.antlr.generated;

import org.jetbrains.kotlin.js.parser.antlr.JavaScriptParserBase;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class JavaScriptParser extends JavaScriptParserBase {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		HashBangLine=1, MultiLineComment=2, SingleLineComment=3, RegularExpressionLiteral=4, 
		OpenBracket=5, CloseBracket=6, OpenParen=7, CloseParen=8, OpenBrace=9, 
		TemplateCloseBrace=10, CloseBrace=11, SemiColon=12, Comma=13, Assign=14, 
		QuestionMark=15, QuestionMarkDot=16, Colon=17, Ellipsis=18, Dot=19, PlusPlus=20, 
		MinusMinus=21, Plus=22, Minus=23, BitNot=24, Not=25, Multiply=26, Divide=27, 
		Modulus=28, Power=29, NullCoalesce=30, Hashtag=31, RightShiftArithmetic=32, 
		LeftShiftArithmetic=33, RightShiftLogical=34, LessThan=35, MoreThan=36, 
		LessThanEquals=37, GreaterThanEquals=38, Equals_=39, NotEquals=40, IdentityEquals=41, 
		IdentityNotEquals=42, BitAnd=43, BitXOr=44, BitOr=45, And=46, Or=47, MultiplyAssign=48, 
		DivideAssign=49, ModulusAssign=50, PlusAssign=51, MinusAssign=52, LeftShiftArithmeticAssign=53, 
		RightShiftArithmeticAssign=54, RightShiftLogicalAssign=55, BitAndAssign=56, 
		BitXorAssign=57, BitOrAssign=58, PowerAssign=59, NullishCoalescingAssign=60, 
		ARROW=61, NullLiteral=62, BooleanLiteral=63, DecimalLiteral=64, HexIntegerLiteral=65, 
		OctalIntegerLiteral=66, OctalIntegerLiteral2=67, BinaryIntegerLiteral=68, 
		BigHexIntegerLiteral=69, BigOctalIntegerLiteral=70, BigBinaryIntegerLiteral=71, 
		BigDecimalIntegerLiteral=72, Break=73, Do=74, Instanceof=75, Typeof=76, 
		Case=77, Else=78, New=79, Var=80, Catch=81, Finally=82, Return=83, Void=84, 
		Continue=85, For=86, Switch=87, While=88, Debugger=89, Function_=90, This=91, 
		With=92, Default=93, If=94, Throw=95, Delete=96, In=97, Try=98, As=99, 
		From=100, Of=101, Yield=102, YieldStar=103, Class=104, Enum=105, Extends=106, 
		Super=107, Const=108, Export=109, Import=110, Async=111, Await=112, Implements=113, 
		StrictLet=114, NonStrictLet=115, Private=116, Public=117, Interface=118, 
		Package=119, Protected=120, Static=121, Identifier=122, StringLiteral=123, 
		BackTick=124, WhiteSpaces=125, LineTerminator=126, HtmlComment=127, CDataComment=128, 
		UnexpectedCharacter=129, TemplateStringStartExpression=130, TemplateStringAtom=131;
	public static final int
		RULE_program = 0, RULE_sourceElement = 1, RULE_statement = 2, RULE_block = 3, 
		RULE_statementList = 4, RULE_importStatement = 5, RULE_importFromBlock = 6, 
		RULE_importModuleItems = 7, RULE_importAliasName = 8, RULE_moduleExportName = 9, 
		RULE_importedBinding = 10, RULE_importDefault = 11, RULE_importNamespace = 12, 
		RULE_importFrom = 13, RULE_aliasName = 14, RULE_exportStatement = 15, 
		RULE_exportFromBlock = 16, RULE_exportModuleItems = 17, RULE_exportAliasName = 18, 
		RULE_declaration = 19, RULE_variableStatement = 20, RULE_variableDeclarationList = 21, 
		RULE_variableDeclaration = 22, RULE_emptyStatement_ = 23, RULE_expressionStatement = 24, 
		RULE_ifStatement = 25, RULE_iterationStatement = 26, RULE_varModifier = 27, 
		RULE_continueStatement = 28, RULE_breakStatement = 29, RULE_returnStatement = 30, 
		RULE_yieldStatement = 31, RULE_withStatement = 32, RULE_switchStatement = 33, 
		RULE_caseBlock = 34, RULE_caseClauses = 35, RULE_caseClause = 36, RULE_defaultClause = 37, 
		RULE_labelledStatement = 38, RULE_throwStatement = 39, RULE_tryStatement = 40, 
		RULE_catchProduction = 41, RULE_finallyProduction = 42, RULE_debuggerStatement = 43, 
		RULE_functionDeclaration = 44, RULE_classDeclaration = 45, RULE_classTail = 46, 
		RULE_classElement = 47, RULE_methodDefinition = 48, RULE_fieldDefinition = 49, 
		RULE_classElementName = 50, RULE_privateIdentifier = 51, RULE_formalParameterList = 52, 
		RULE_formalParameterArg = 53, RULE_restParameterArg = 54, RULE_functionBody = 55, 
		RULE_sourceElements = 56, RULE_arrayLiteral = 57, RULE_elementList = 58, 
		RULE_arrayElement = 59, RULE_propertyAssignment = 60, RULE_propertyName = 61, 
		RULE_arguments = 62, RULE_argument = 63, RULE_expressionSequence = 64, 
		RULE_singleExpression = 65, RULE_singleExpressionImpl = 66, RULE_initializer = 67, 
		RULE_assignable = 68, RULE_objectLiteral = 69, RULE_anonymousFunction = 70, 
		RULE_arrowFunctionParameters = 71, RULE_arrowFunctionBody = 72, RULE_assignmentOperator = 73, 
		RULE_literal = 74, RULE_templateStringLiteral = 75, RULE_templateStringAtom = 76, 
		RULE_numericLiteral = 77, RULE_bigintLiteral = 78, RULE_getter = 79, RULE_setter = 80, 
		RULE_identifierName = 81, RULE_identifier = 82, RULE_reservedWord = 83, 
		RULE_keyword = 84, RULE_let_ = 85, RULE_eos = 86;
	private static String[] makeRuleNames() {
		return new String[] {
			"program", "sourceElement", "statement", "block", "statementList", "importStatement", 
			"importFromBlock", "importModuleItems", "importAliasName", "moduleExportName", 
			"importedBinding", "importDefault", "importNamespace", "importFrom", 
			"aliasName", "exportStatement", "exportFromBlock", "exportModuleItems", 
			"exportAliasName", "declaration", "variableStatement", "variableDeclarationList", 
			"variableDeclaration", "emptyStatement_", "expressionStatement", "ifStatement", 
			"iterationStatement", "varModifier", "continueStatement", "breakStatement", 
			"returnStatement", "yieldStatement", "withStatement", "switchStatement", 
			"caseBlock", "caseClauses", "caseClause", "defaultClause", "labelledStatement", 
			"throwStatement", "tryStatement", "catchProduction", "finallyProduction", 
			"debuggerStatement", "functionDeclaration", "classDeclaration", "classTail", 
			"classElement", "methodDefinition", "fieldDefinition", "classElementName", 
			"privateIdentifier", "formalParameterList", "formalParameterArg", "restParameterArg", 
			"functionBody", "sourceElements", "arrayLiteral", "elementList", "arrayElement", 
			"propertyAssignment", "propertyName", "arguments", "argument", "expressionSequence", 
			"singleExpression", "singleExpressionImpl", "initializer", "assignable", 
			"objectLiteral", "anonymousFunction", "arrowFunctionParameters", "arrowFunctionBody", 
			"assignmentOperator", "literal", "templateStringLiteral", "templateStringAtom", 
			"numericLiteral", "bigintLiteral", "getter", "setter", "identifierName", 
			"identifier", "reservedWord", "keyword", "let_", "eos"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, "'['", "']'", "'('", "')'", "'{'", null, 
			"'}'", "';'", "','", "'='", "'?'", "'?.'", "':'", "'...'", "'.'", "'++'", 
			"'--'", "'+'", "'-'", "'~'", "'!'", "'*'", "'/'", "'%'", "'**'", "'??'", 
			"'#'", "'>>'", "'<<'", "'>>>'", "'<'", "'>'", "'<='", "'>='", "'=='", 
			"'!='", "'==='", "'!=='", "'&'", "'^'", "'|'", "'&&'", "'||'", "'*='", 
			"'/='", "'%='", "'+='", "'-='", "'<<='", "'>>='", "'>>>='", "'&='", "'^='", 
			"'|='", "'**='", "'??='", "'=>'", "'null'", null, null, null, null, null, 
			null, null, null, null, null, "'break'", "'do'", "'instanceof'", "'typeof'", 
			"'case'", "'else'", "'new'", "'var'", "'catch'", "'finally'", "'return'", 
			"'void'", "'continue'", "'for'", "'switch'", "'while'", "'debugger'", 
			"'function'", "'this'", "'with'", "'default'", "'if'", "'throw'", "'delete'", 
			"'in'", "'try'", "'as'", "'from'", "'of'", "'yield'", "'yield*'", "'class'", 
			"'enum'", "'extends'", "'super'", "'const'", "'export'", "'import'", 
			"'async'", "'await'", "'implements'", null, null, "'private'", "'public'", 
			"'interface'", "'package'", "'protected'", "'static'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "HashBangLine", "MultiLineComment", "SingleLineComment", "RegularExpressionLiteral", 
			"OpenBracket", "CloseBracket", "OpenParen", "CloseParen", "OpenBrace", 
			"TemplateCloseBrace", "CloseBrace", "SemiColon", "Comma", "Assign", "QuestionMark", 
			"QuestionMarkDot", "Colon", "Ellipsis", "Dot", "PlusPlus", "MinusMinus", 
			"Plus", "Minus", "BitNot", "Not", "Multiply", "Divide", "Modulus", "Power", 
			"NullCoalesce", "Hashtag", "RightShiftArithmetic", "LeftShiftArithmetic", 
			"RightShiftLogical", "LessThan", "MoreThan", "LessThanEquals", "GreaterThanEquals", 
			"Equals_", "NotEquals", "IdentityEquals", "IdentityNotEquals", "BitAnd", 
			"BitXOr", "BitOr", "And", "Or", "MultiplyAssign", "DivideAssign", "ModulusAssign", 
			"PlusAssign", "MinusAssign", "LeftShiftArithmeticAssign", "RightShiftArithmeticAssign", 
			"RightShiftLogicalAssign", "BitAndAssign", "BitXorAssign", "BitOrAssign", 
			"PowerAssign", "NullishCoalescingAssign", "ARROW", "NullLiteral", "BooleanLiteral", 
			"DecimalLiteral", "HexIntegerLiteral", "OctalIntegerLiteral", "OctalIntegerLiteral2", 
			"BinaryIntegerLiteral", "BigHexIntegerLiteral", "BigOctalIntegerLiteral", 
			"BigBinaryIntegerLiteral", "BigDecimalIntegerLiteral", "Break", "Do", 
			"Instanceof", "Typeof", "Case", "Else", "New", "Var", "Catch", "Finally", 
			"Return", "Void", "Continue", "For", "Switch", "While", "Debugger", "Function_", 
			"This", "With", "Default", "If", "Throw", "Delete", "In", "Try", "As", 
			"From", "Of", "Yield", "YieldStar", "Class", "Enum", "Extends", "Super", 
			"Const", "Export", "Import", "Async", "Await", "Implements", "StrictLet", 
			"NonStrictLet", "Private", "Public", "Interface", "Package", "Protected", 
			"Static", "Identifier", "StringLiteral", "BackTick", "WhiteSpaces", "LineTerminator", 
			"HtmlComment", "CDataComment", "UnexpectedCharacter", "TemplateStringStartExpression", 
			"TemplateStringAtom"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "JavaScriptParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public JavaScriptParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProgramContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(JavaScriptParser.EOF, 0); }
		public TerminalNode HashBangLine() { return getToken(JavaScriptParser.HashBangLine, 0); }
		public SourceElementsContext sourceElements() {
			return getRuleContext(SourceElementsContext.class,0);
		}
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterProgram(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitProgram(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitProgram(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_program);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(175);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				{
				setState(174);
				match(HashBangLine);
				}
				break;
			}
			setState(178);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				{
				setState(177);
				sourceElements();
				}
				break;
			}
			setState(180);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SourceElementContext extends ParserRuleContext {
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public SourceElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sourceElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterSourceElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitSourceElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitSourceElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SourceElementContext sourceElement() throws RecognitionException {
		SourceElementContext _localctx = new SourceElementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_sourceElement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(182);
			statement();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StatementContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public VariableStatementContext variableStatement() {
			return getRuleContext(VariableStatementContext.class,0);
		}
		public ImportStatementContext importStatement() {
			return getRuleContext(ImportStatementContext.class,0);
		}
		public ExportStatementContext exportStatement() {
			return getRuleContext(ExportStatementContext.class,0);
		}
		public EmptyStatement_Context emptyStatement_() {
			return getRuleContext(EmptyStatement_Context.class,0);
		}
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public FunctionDeclarationContext functionDeclaration() {
			return getRuleContext(FunctionDeclarationContext.class,0);
		}
		public ExpressionStatementContext expressionStatement() {
			return getRuleContext(ExpressionStatementContext.class,0);
		}
		public IfStatementContext ifStatement() {
			return getRuleContext(IfStatementContext.class,0);
		}
		public IterationStatementContext iterationStatement() {
			return getRuleContext(IterationStatementContext.class,0);
		}
		public ContinueStatementContext continueStatement() {
			return getRuleContext(ContinueStatementContext.class,0);
		}
		public BreakStatementContext breakStatement() {
			return getRuleContext(BreakStatementContext.class,0);
		}
		public ReturnStatementContext returnStatement() {
			return getRuleContext(ReturnStatementContext.class,0);
		}
		public YieldStatementContext yieldStatement() {
			return getRuleContext(YieldStatementContext.class,0);
		}
		public WithStatementContext withStatement() {
			return getRuleContext(WithStatementContext.class,0);
		}
		public LabelledStatementContext labelledStatement() {
			return getRuleContext(LabelledStatementContext.class,0);
		}
		public SwitchStatementContext switchStatement() {
			return getRuleContext(SwitchStatementContext.class,0);
		}
		public ThrowStatementContext throwStatement() {
			return getRuleContext(ThrowStatementContext.class,0);
		}
		public TryStatementContext tryStatement() {
			return getRuleContext(TryStatementContext.class,0);
		}
		public DebuggerStatementContext debuggerStatement() {
			return getRuleContext(DebuggerStatementContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_statement);
		try {
			setState(204);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(184);
				block();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(185);
				variableStatement();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(186);
				importStatement();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(187);
				exportStatement();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(188);
				emptyStatement_();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(189);
				classDeclaration();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(190);
				functionDeclaration();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(191);
				expressionStatement();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(192);
				ifStatement();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(193);
				iterationStatement();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(194);
				continueStatement();
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(195);
				breakStatement();
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(196);
				returnStatement();
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(197);
				yieldStatement();
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(198);
				withStatement();
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(199);
				labelledStatement();
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(200);
				switchStatement();
				}
				break;
			case 18:
				enterOuterAlt(_localctx, 18);
				{
				setState(201);
				throwStatement();
				}
				break;
			case 19:
				enterOuterAlt(_localctx, 19);
				{
				setState(202);
				tryStatement();
				}
				break;
			case 20:
				enterOuterAlt(_localctx, 20);
				{
				setState(203);
				debuggerStatement();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BlockContext extends ParserRuleContext {
		public TerminalNode OpenBrace() { return getToken(JavaScriptParser.OpenBrace, 0); }
		public TerminalNode CloseBrace() { return getToken(JavaScriptParser.CloseBrace, 0); }
		public StatementListContext statementList() {
			return getRuleContext(StatementListContext.class,0);
		}
		public BlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_block; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlockContext block() throws RecognitionException {
		BlockContext _localctx = new BlockContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_block);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(206);
			match(OpenBrace);
			setState(208);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				{
				setState(207);
				statementList();
				}
				break;
			}
			setState(210);
			match(CloseBrace);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StatementListContext extends ParserRuleContext {
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public StatementListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statementList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterStatementList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitStatementList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitStatementList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementListContext statementList() throws RecognitionException {
		StatementListContext _localctx = new StatementListContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_statementList);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(213); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(212);
					statement();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(215); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ImportStatementContext extends ParserRuleContext {
		public TerminalNode Import() { return getToken(JavaScriptParser.Import, 0); }
		public ImportFromBlockContext importFromBlock() {
			return getRuleContext(ImportFromBlockContext.class,0);
		}
		public ImportStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterImportStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitImportStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitImportStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportStatementContext importStatement() throws RecognitionException {
		ImportStatementContext _localctx = new ImportStatementContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_importStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(217);
			match(Import);
			setState(218);
			importFromBlock();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ImportFromBlockContext extends ParserRuleContext {
		public ImportFromContext importFrom() {
			return getRuleContext(ImportFromContext.class,0);
		}
		public EosContext eos() {
			return getRuleContext(EosContext.class,0);
		}
		public ImportNamespaceContext importNamespace() {
			return getRuleContext(ImportNamespaceContext.class,0);
		}
		public ImportModuleItemsContext importModuleItems() {
			return getRuleContext(ImportModuleItemsContext.class,0);
		}
		public ImportDefaultContext importDefault() {
			return getRuleContext(ImportDefaultContext.class,0);
		}
		public TerminalNode StringLiteral() { return getToken(JavaScriptParser.StringLiteral, 0); }
		public ImportFromBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importFromBlock; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterImportFromBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitImportFromBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitImportFromBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportFromBlockContext importFromBlock() throws RecognitionException {
		ImportFromBlockContext _localctx = new ImportFromBlockContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_importFromBlock);
		try {
			setState(232);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OpenBrace:
			case Multiply:
			case NullLiteral:
			case BooleanLiteral:
			case Break:
			case Do:
			case Instanceof:
			case Typeof:
			case Case:
			case Else:
			case New:
			case Var:
			case Catch:
			case Finally:
			case Return:
			case Void:
			case Continue:
			case For:
			case Switch:
			case While:
			case Debugger:
			case Function_:
			case This:
			case With:
			case Default:
			case If:
			case Throw:
			case Delete:
			case In:
			case Try:
			case As:
			case From:
			case Of:
			case Yield:
			case YieldStar:
			case Class:
			case Enum:
			case Extends:
			case Super:
			case Const:
			case Export:
			case Import:
			case Async:
			case Await:
			case Implements:
			case StrictLet:
			case NonStrictLet:
			case Private:
			case Public:
			case Interface:
			case Package:
			case Protected:
			case Static:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(221);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
				case 1:
					{
					setState(220);
					importDefault();
					}
					break;
				}
				setState(225);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case Multiply:
				case NullLiteral:
				case BooleanLiteral:
				case Break:
				case Do:
				case Instanceof:
				case Typeof:
				case Case:
				case Else:
				case New:
				case Var:
				case Catch:
				case Finally:
				case Return:
				case Void:
				case Continue:
				case For:
				case Switch:
				case While:
				case Debugger:
				case Function_:
				case This:
				case With:
				case Default:
				case If:
				case Throw:
				case Delete:
				case In:
				case Try:
				case As:
				case From:
				case Of:
				case Yield:
				case YieldStar:
				case Class:
				case Enum:
				case Extends:
				case Super:
				case Const:
				case Export:
				case Import:
				case Async:
				case Await:
				case Implements:
				case StrictLet:
				case NonStrictLet:
				case Private:
				case Public:
				case Interface:
				case Package:
				case Protected:
				case Static:
				case Identifier:
					{
					setState(223);
					importNamespace();
					}
					break;
				case OpenBrace:
					{
					setState(224);
					importModuleItems();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(227);
				importFrom();
				setState(228);
				eos();
				}
				break;
			case StringLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(230);
				match(StringLiteral);
				setState(231);
				eos();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ImportModuleItemsContext extends ParserRuleContext {
		public TerminalNode OpenBrace() { return getToken(JavaScriptParser.OpenBrace, 0); }
		public TerminalNode CloseBrace() { return getToken(JavaScriptParser.CloseBrace, 0); }
		public List<ImportAliasNameContext> importAliasName() {
			return getRuleContexts(ImportAliasNameContext.class);
		}
		public ImportAliasNameContext importAliasName(int i) {
			return getRuleContext(ImportAliasNameContext.class,i);
		}
		public List<TerminalNode> Comma() { return getTokens(JavaScriptParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(JavaScriptParser.Comma, i);
		}
		public ImportModuleItemsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importModuleItems; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterImportModuleItems(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitImportModuleItems(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitImportModuleItems(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportModuleItemsContext importModuleItems() throws RecognitionException {
		ImportModuleItemsContext _localctx = new ImportModuleItemsContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_importModuleItems);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(234);
			match(OpenBrace);
			setState(240);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(235);
					importAliasName();
					setState(236);
					match(Comma);
					}
					} 
				}
				setState(242);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
			}
			setState(247);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 62)) & ~0x3f) == 0 && ((1L << (_la - 62)) & 4611686018427385859L) != 0)) {
				{
				setState(243);
				importAliasName();
				setState(245);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(244);
					match(Comma);
					}
				}

				}
			}

			setState(249);
			match(CloseBrace);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ImportAliasNameContext extends ParserRuleContext {
		public ModuleExportNameContext moduleExportName() {
			return getRuleContext(ModuleExportNameContext.class,0);
		}
		public TerminalNode As() { return getToken(JavaScriptParser.As, 0); }
		public ImportedBindingContext importedBinding() {
			return getRuleContext(ImportedBindingContext.class,0);
		}
		public ImportAliasNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importAliasName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterImportAliasName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitImportAliasName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitImportAliasName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportAliasNameContext importAliasName() throws RecognitionException {
		ImportAliasNameContext _localctx = new ImportAliasNameContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_importAliasName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(251);
			moduleExportName();
			setState(254);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==As) {
				{
				setState(252);
				match(As);
				setState(253);
				importedBinding();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ModuleExportNameContext extends ParserRuleContext {
		public IdentifierNameContext identifierName() {
			return getRuleContext(IdentifierNameContext.class,0);
		}
		public TerminalNode StringLiteral() { return getToken(JavaScriptParser.StringLiteral, 0); }
		public ModuleExportNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_moduleExportName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterModuleExportName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitModuleExportName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitModuleExportName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ModuleExportNameContext moduleExportName() throws RecognitionException {
		ModuleExportNameContext _localctx = new ModuleExportNameContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_moduleExportName);
		try {
			setState(258);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NullLiteral:
			case BooleanLiteral:
			case Break:
			case Do:
			case Instanceof:
			case Typeof:
			case Case:
			case Else:
			case New:
			case Var:
			case Catch:
			case Finally:
			case Return:
			case Void:
			case Continue:
			case For:
			case Switch:
			case While:
			case Debugger:
			case Function_:
			case This:
			case With:
			case Default:
			case If:
			case Throw:
			case Delete:
			case In:
			case Try:
			case As:
			case From:
			case Of:
			case Yield:
			case YieldStar:
			case Class:
			case Enum:
			case Extends:
			case Super:
			case Const:
			case Export:
			case Import:
			case Async:
			case Await:
			case Implements:
			case StrictLet:
			case NonStrictLet:
			case Private:
			case Public:
			case Interface:
			case Package:
			case Protected:
			case Static:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(256);
				identifierName();
				}
				break;
			case StringLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(257);
				match(StringLiteral);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ImportedBindingContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(JavaScriptParser.Identifier, 0); }
		public TerminalNode Yield() { return getToken(JavaScriptParser.Yield, 0); }
		public TerminalNode Await() { return getToken(JavaScriptParser.Await, 0); }
		public ImportedBindingContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importedBinding; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterImportedBinding(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitImportedBinding(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitImportedBinding(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportedBindingContext importedBinding() throws RecognitionException {
		ImportedBindingContext _localctx = new ImportedBindingContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_importedBinding);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(260);
			_la = _input.LA(1);
			if ( !(((((_la - 102)) & ~0x3f) == 0 && ((1L << (_la - 102)) & 1049601L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ImportDefaultContext extends ParserRuleContext {
		public AliasNameContext aliasName() {
			return getRuleContext(AliasNameContext.class,0);
		}
		public TerminalNode Comma() { return getToken(JavaScriptParser.Comma, 0); }
		public ImportDefaultContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importDefault; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterImportDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitImportDefault(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitImportDefault(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportDefaultContext importDefault() throws RecognitionException {
		ImportDefaultContext _localctx = new ImportDefaultContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_importDefault);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(262);
			aliasName();
			setState(263);
			match(Comma);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ImportNamespaceContext extends ParserRuleContext {
		public TerminalNode Multiply() { return getToken(JavaScriptParser.Multiply, 0); }
		public List<IdentifierNameContext> identifierName() {
			return getRuleContexts(IdentifierNameContext.class);
		}
		public IdentifierNameContext identifierName(int i) {
			return getRuleContext(IdentifierNameContext.class,i);
		}
		public TerminalNode As() { return getToken(JavaScriptParser.As, 0); }
		public ImportNamespaceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importNamespace; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterImportNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitImportNamespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitImportNamespace(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportNamespaceContext importNamespace() throws RecognitionException {
		ImportNamespaceContext _localctx = new ImportNamespaceContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_importNamespace);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(267);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Multiply:
				{
				setState(265);
				match(Multiply);
				}
				break;
			case NullLiteral:
			case BooleanLiteral:
			case Break:
			case Do:
			case Instanceof:
			case Typeof:
			case Case:
			case Else:
			case New:
			case Var:
			case Catch:
			case Finally:
			case Return:
			case Void:
			case Continue:
			case For:
			case Switch:
			case While:
			case Debugger:
			case Function_:
			case This:
			case With:
			case Default:
			case If:
			case Throw:
			case Delete:
			case In:
			case Try:
			case As:
			case From:
			case Of:
			case Yield:
			case YieldStar:
			case Class:
			case Enum:
			case Extends:
			case Super:
			case Const:
			case Export:
			case Import:
			case Async:
			case Await:
			case Implements:
			case StrictLet:
			case NonStrictLet:
			case Private:
			case Public:
			case Interface:
			case Package:
			case Protected:
			case Static:
			case Identifier:
				{
				setState(266);
				identifierName();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(271);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==As) {
				{
				setState(269);
				match(As);
				setState(270);
				identifierName();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ImportFromContext extends ParserRuleContext {
		public TerminalNode From() { return getToken(JavaScriptParser.From, 0); }
		public TerminalNode StringLiteral() { return getToken(JavaScriptParser.StringLiteral, 0); }
		public ImportFromContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importFrom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterImportFrom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitImportFrom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitImportFrom(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportFromContext importFrom() throws RecognitionException {
		ImportFromContext _localctx = new ImportFromContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_importFrom);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(273);
			match(From);
			setState(274);
			match(StringLiteral);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AliasNameContext extends ParserRuleContext {
		public List<IdentifierNameContext> identifierName() {
			return getRuleContexts(IdentifierNameContext.class);
		}
		public IdentifierNameContext identifierName(int i) {
			return getRuleContext(IdentifierNameContext.class,i);
		}
		public TerminalNode As() { return getToken(JavaScriptParser.As, 0); }
		public AliasNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aliasName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterAliasName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitAliasName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitAliasName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AliasNameContext aliasName() throws RecognitionException {
		AliasNameContext _localctx = new AliasNameContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_aliasName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(276);
			identifierName();
			setState(279);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==As) {
				{
				setState(277);
				match(As);
				setState(278);
				identifierName();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExportStatementContext extends ParserRuleContext {
		public ExportStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exportStatement; }
	 
		public ExportStatementContext() { }
		public void copyFrom(ExportStatementContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExportDefaultDeclarationContext extends ExportStatementContext {
		public TerminalNode Export() { return getToken(JavaScriptParser.Export, 0); }
		public TerminalNode Default() { return getToken(JavaScriptParser.Default, 0); }
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public EosContext eos() {
			return getRuleContext(EosContext.class,0);
		}
		public ExportDefaultDeclarationContext(ExportStatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterExportDefaultDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitExportDefaultDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitExportDefaultDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExportDeclarationContext extends ExportStatementContext {
		public TerminalNode Export() { return getToken(JavaScriptParser.Export, 0); }
		public EosContext eos() {
			return getRuleContext(EosContext.class,0);
		}
		public ExportFromBlockContext exportFromBlock() {
			return getRuleContext(ExportFromBlockContext.class,0);
		}
		public DeclarationContext declaration() {
			return getRuleContext(DeclarationContext.class,0);
		}
		public TerminalNode Default() { return getToken(JavaScriptParser.Default, 0); }
		public ExportDeclarationContext(ExportStatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterExportDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitExportDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitExportDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExportStatementContext exportStatement() throws RecognitionException {
		ExportStatementContext _localctx = new ExportStatementContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_exportStatement);
		try {
			setState(296);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				_localctx = new ExportDeclarationContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(281);
				match(Export);
				setState(283);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
				case 1:
					{
					setState(282);
					match(Default);
					}
					break;
				}
				setState(287);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
				case 1:
					{
					setState(285);
					exportFromBlock();
					}
					break;
				case 2:
					{
					setState(286);
					declaration();
					}
					break;
				}
				setState(289);
				eos();
				}
				break;
			case 2:
				_localctx = new ExportDefaultDeclarationContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(291);
				match(Export);
				setState(292);
				match(Default);
				setState(293);
				singleExpression();
				setState(294);
				eos();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExportFromBlockContext extends ParserRuleContext {
		public ImportNamespaceContext importNamespace() {
			return getRuleContext(ImportNamespaceContext.class,0);
		}
		public ImportFromContext importFrom() {
			return getRuleContext(ImportFromContext.class,0);
		}
		public EosContext eos() {
			return getRuleContext(EosContext.class,0);
		}
		public ExportModuleItemsContext exportModuleItems() {
			return getRuleContext(ExportModuleItemsContext.class,0);
		}
		public ExportFromBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exportFromBlock; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterExportFromBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitExportFromBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitExportFromBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExportFromBlockContext exportFromBlock() throws RecognitionException {
		ExportFromBlockContext _localctx = new ExportFromBlockContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_exportFromBlock);
		try {
			setState(308);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Multiply:
			case NullLiteral:
			case BooleanLiteral:
			case Break:
			case Do:
			case Instanceof:
			case Typeof:
			case Case:
			case Else:
			case New:
			case Var:
			case Catch:
			case Finally:
			case Return:
			case Void:
			case Continue:
			case For:
			case Switch:
			case While:
			case Debugger:
			case Function_:
			case This:
			case With:
			case Default:
			case If:
			case Throw:
			case Delete:
			case In:
			case Try:
			case As:
			case From:
			case Of:
			case Yield:
			case YieldStar:
			case Class:
			case Enum:
			case Extends:
			case Super:
			case Const:
			case Export:
			case Import:
			case Async:
			case Await:
			case Implements:
			case StrictLet:
			case NonStrictLet:
			case Private:
			case Public:
			case Interface:
			case Package:
			case Protected:
			case Static:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(298);
				importNamespace();
				setState(299);
				importFrom();
				setState(300);
				eos();
				}
				break;
			case OpenBrace:
				enterOuterAlt(_localctx, 2);
				{
				setState(302);
				exportModuleItems();
				setState(304);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
				case 1:
					{
					setState(303);
					importFrom();
					}
					break;
				}
				setState(306);
				eos();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExportModuleItemsContext extends ParserRuleContext {
		public TerminalNode OpenBrace() { return getToken(JavaScriptParser.OpenBrace, 0); }
		public TerminalNode CloseBrace() { return getToken(JavaScriptParser.CloseBrace, 0); }
		public List<ExportAliasNameContext> exportAliasName() {
			return getRuleContexts(ExportAliasNameContext.class);
		}
		public ExportAliasNameContext exportAliasName(int i) {
			return getRuleContext(ExportAliasNameContext.class,i);
		}
		public List<TerminalNode> Comma() { return getTokens(JavaScriptParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(JavaScriptParser.Comma, i);
		}
		public ExportModuleItemsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exportModuleItems; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterExportModuleItems(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitExportModuleItems(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitExportModuleItems(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExportModuleItemsContext exportModuleItems() throws RecognitionException {
		ExportModuleItemsContext _localctx = new ExportModuleItemsContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_exportModuleItems);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(310);
			match(OpenBrace);
			setState(316);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(311);
					exportAliasName();
					setState(312);
					match(Comma);
					}
					} 
				}
				setState(318);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
			}
			setState(323);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 62)) & ~0x3f) == 0 && ((1L << (_la - 62)) & 4611686018427385859L) != 0)) {
				{
				setState(319);
				exportAliasName();
				setState(321);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(320);
					match(Comma);
					}
				}

				}
			}

			setState(325);
			match(CloseBrace);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExportAliasNameContext extends ParserRuleContext {
		public List<ModuleExportNameContext> moduleExportName() {
			return getRuleContexts(ModuleExportNameContext.class);
		}
		public ModuleExportNameContext moduleExportName(int i) {
			return getRuleContext(ModuleExportNameContext.class,i);
		}
		public TerminalNode As() { return getToken(JavaScriptParser.As, 0); }
		public ExportAliasNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exportAliasName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterExportAliasName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitExportAliasName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitExportAliasName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExportAliasNameContext exportAliasName() throws RecognitionException {
		ExportAliasNameContext _localctx = new ExportAliasNameContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_exportAliasName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(327);
			moduleExportName();
			setState(330);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==As) {
				{
				setState(328);
				match(As);
				setState(329);
				moduleExportName();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DeclarationContext extends ParserRuleContext {
		public VariableStatementContext variableStatement() {
			return getRuleContext(VariableStatementContext.class,0);
		}
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public FunctionDeclarationContext functionDeclaration() {
			return getRuleContext(FunctionDeclarationContext.class,0);
		}
		public DeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeclarationContext declaration() throws RecognitionException {
		DeclarationContext _localctx = new DeclarationContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_declaration);
		try {
			setState(335);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Var:
			case Const:
			case StrictLet:
			case NonStrictLet:
				enterOuterAlt(_localctx, 1);
				{
				setState(332);
				variableStatement();
				}
				break;
			case Class:
				enterOuterAlt(_localctx, 2);
				{
				setState(333);
				classDeclaration();
				}
				break;
			case Function_:
			case Async:
				enterOuterAlt(_localctx, 3);
				{
				setState(334);
				functionDeclaration();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariableStatementContext extends ParserRuleContext {
		public VariableDeclarationListContext variableDeclarationList() {
			return getRuleContext(VariableDeclarationListContext.class,0);
		}
		public EosContext eos() {
			return getRuleContext(EosContext.class,0);
		}
		public VariableStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterVariableStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitVariableStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitVariableStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableStatementContext variableStatement() throws RecognitionException {
		VariableStatementContext _localctx = new VariableStatementContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_variableStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(337);
			variableDeclarationList();
			setState(338);
			eos();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariableDeclarationListContext extends ParserRuleContext {
		public VarModifierContext varModifier() {
			return getRuleContext(VarModifierContext.class,0);
		}
		public List<VariableDeclarationContext> variableDeclaration() {
			return getRuleContexts(VariableDeclarationContext.class);
		}
		public VariableDeclarationContext variableDeclaration(int i) {
			return getRuleContext(VariableDeclarationContext.class,i);
		}
		public List<TerminalNode> Comma() { return getTokens(JavaScriptParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(JavaScriptParser.Comma, i);
		}
		public VariableDeclarationListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDeclarationList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterVariableDeclarationList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitVariableDeclarationList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitVariableDeclarationList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableDeclarationListContext variableDeclarationList() throws RecognitionException {
		VariableDeclarationListContext _localctx = new VariableDeclarationListContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_variableDeclarationList);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(340);
			varModifier();
			setState(341);
			variableDeclaration();
			setState(346);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(342);
					match(Comma);
					setState(343);
					variableDeclaration();
					}
					} 
				}
				setState(348);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariableDeclarationContext extends ParserRuleContext {
		public AssignableContext assignable() {
			return getRuleContext(AssignableContext.class,0);
		}
		public TerminalNode Assign() { return getToken(JavaScriptParser.Assign, 0); }
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public VariableDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterVariableDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitVariableDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitVariableDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableDeclarationContext variableDeclaration() throws RecognitionException {
		VariableDeclarationContext _localctx = new VariableDeclarationContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_variableDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(349);
			assignable();
			setState(352);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				{
				setState(350);
				match(Assign);
				setState(351);
				singleExpression();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EmptyStatement_Context extends ParserRuleContext {
		public TerminalNode SemiColon() { return getToken(JavaScriptParser.SemiColon, 0); }
		public EmptyStatement_Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_emptyStatement_; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterEmptyStatement_(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitEmptyStatement_(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitEmptyStatement_(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EmptyStatement_Context emptyStatement_() throws RecognitionException {
		EmptyStatement_Context _localctx = new EmptyStatement_Context(_ctx, getState());
		enterRule(_localctx, 46, RULE_emptyStatement_);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(354);
			match(SemiColon);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionStatementContext extends ParserRuleContext {
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public EosContext eos() {
			return getRuleContext(EosContext.class,0);
		}
		public ExpressionStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterExpressionStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitExpressionStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitExpressionStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionStatementContext expressionStatement() throws RecognitionException {
		ExpressionStatementContext _localctx = new ExpressionStatementContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_expressionStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(356);
			if (!(this.notOpenBraceAndNotFunction())) throw new FailedPredicateException(this, "this.notOpenBraceAndNotFunction()");
			setState(357);
			expressionSequence();
			setState(358);
			eos();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IfStatementContext extends ParserRuleContext {
		public TerminalNode If() { return getToken(JavaScriptParser.If, 0); }
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public TerminalNode Else() { return getToken(JavaScriptParser.Else, 0); }
		public IfStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ifStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterIfStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitIfStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitIfStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IfStatementContext ifStatement() throws RecognitionException {
		IfStatementContext _localctx = new IfStatementContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_ifStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(360);
			match(If);
			setState(361);
			match(OpenParen);
			setState(362);
			expressionSequence();
			setState(363);
			match(CloseParen);
			setState(364);
			statement();
			setState(367);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,28,_ctx) ) {
			case 1:
				{
				setState(365);
				match(Else);
				setState(366);
				statement();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IterationStatementContext extends ParserRuleContext {
		public IterationStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_iterationStatement; }
	 
		public IterationStatementContext() { }
		public void copyFrom(IterationStatementContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class DoStatementContext extends IterationStatementContext {
		public TerminalNode Do() { return getToken(JavaScriptParser.Do, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TerminalNode While() { return getToken(JavaScriptParser.While, 0); }
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public EosContext eos() {
			return getRuleContext(EosContext.class,0);
		}
		public DoStatementContext(IterationStatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterDoStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitDoStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitDoStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class WhileStatementContext extends IterationStatementContext {
		public TerminalNode While() { return getToken(JavaScriptParser.While, 0); }
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public WhileStatementContext(IterationStatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterWhileStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitWhileStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitWhileStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ForStatementContext extends IterationStatementContext {
		public TerminalNode For() { return getToken(JavaScriptParser.For, 0); }
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public List<TerminalNode> SemiColon() { return getTokens(JavaScriptParser.SemiColon); }
		public TerminalNode SemiColon(int i) {
			return getToken(JavaScriptParser.SemiColon, i);
		}
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public List<ExpressionSequenceContext> expressionSequence() {
			return getRuleContexts(ExpressionSequenceContext.class);
		}
		public ExpressionSequenceContext expressionSequence(int i) {
			return getRuleContext(ExpressionSequenceContext.class,i);
		}
		public VariableDeclarationListContext variableDeclarationList() {
			return getRuleContext(VariableDeclarationListContext.class,0);
		}
		public ForStatementContext(IterationStatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterForStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitForStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitForStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ForInStatementContext extends IterationStatementContext {
		public TerminalNode For() { return getToken(JavaScriptParser.For, 0); }
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public TerminalNode In() { return getToken(JavaScriptParser.In, 0); }
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public VariableDeclarationListContext variableDeclarationList() {
			return getRuleContext(VariableDeclarationListContext.class,0);
		}
		public ForInStatementContext(IterationStatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterForInStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitForInStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitForInStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ForOfStatementContext extends IterationStatementContext {
		public TerminalNode For() { return getToken(JavaScriptParser.For, 0); }
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public TerminalNode Of() { return getToken(JavaScriptParser.Of, 0); }
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public VariableDeclarationListContext variableDeclarationList() {
			return getRuleContext(VariableDeclarationListContext.class,0);
		}
		public TerminalNode Await() { return getToken(JavaScriptParser.Await, 0); }
		public ForOfStatementContext(IterationStatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterForOfStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitForOfStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitForOfStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IterationStatementContext iterationStatement() throws RecognitionException {
		IterationStatementContext _localctx = new IterationStatementContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_iterationStatement);
		int _la;
		try {
			setState(424);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
			case 1:
				_localctx = new DoStatementContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(369);
				match(Do);
				setState(370);
				statement();
				setState(371);
				match(While);
				setState(372);
				match(OpenParen);
				setState(373);
				expressionSequence();
				setState(374);
				match(CloseParen);
				setState(375);
				eos();
				}
				break;
			case 2:
				_localctx = new WhileStatementContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(377);
				match(While);
				setState(378);
				match(OpenParen);
				setState(379);
				expressionSequence();
				setState(380);
				match(CloseParen);
				setState(381);
				statement();
				}
				break;
			case 3:
				_localctx = new ForStatementContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(383);
				match(For);
				setState(384);
				match(OpenParen);
				setState(387);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
				case 1:
					{
					setState(385);
					expressionSequence();
					}
					break;
				case 2:
					{
					setState(386);
					variableDeclarationList();
					}
					break;
				}
				setState(389);
				match(SemiColon);
				setState(391);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -4611686018361326928L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305843009213693951L) != 0)) {
					{
					setState(390);
					expressionSequence();
					}
				}

				setState(393);
				match(SemiColon);
				setState(395);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -4611686018361326928L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305843009213693951L) != 0)) {
					{
					setState(394);
					expressionSequence();
					}
				}

				setState(397);
				match(CloseParen);
				setState(398);
				statement();
				}
				break;
			case 4:
				_localctx = new ForInStatementContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(399);
				match(For);
				setState(400);
				match(OpenParen);
				setState(403);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
				case 1:
					{
					setState(401);
					singleExpression();
					}
					break;
				case 2:
					{
					setState(402);
					variableDeclarationList();
					}
					break;
				}
				setState(405);
				match(In);
				setState(406);
				expressionSequence();
				setState(407);
				match(CloseParen);
				setState(408);
				statement();
				}
				break;
			case 5:
				_localctx = new ForOfStatementContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(410);
				match(For);
				setState(412);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Await) {
					{
					setState(411);
					match(Await);
					}
				}

				setState(414);
				match(OpenParen);
				setState(417);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,34,_ctx) ) {
				case 1:
					{
					setState(415);
					singleExpression();
					}
					break;
				case 2:
					{
					setState(416);
					variableDeclarationList();
					}
					break;
				}
				setState(419);
				match(Of);
				setState(420);
				expressionSequence();
				setState(421);
				match(CloseParen);
				setState(422);
				statement();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VarModifierContext extends ParserRuleContext {
		public TerminalNode Var() { return getToken(JavaScriptParser.Var, 0); }
		public Let_Context let_() {
			return getRuleContext(Let_Context.class,0);
		}
		public TerminalNode Const() { return getToken(JavaScriptParser.Const, 0); }
		public VarModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_varModifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterVarModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitVarModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitVarModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VarModifierContext varModifier() throws RecognitionException {
		VarModifierContext _localctx = new VarModifierContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_varModifier);
		try {
			setState(429);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Var:
				enterOuterAlt(_localctx, 1);
				{
				setState(426);
				match(Var);
				}
				break;
			case StrictLet:
			case NonStrictLet:
				enterOuterAlt(_localctx, 2);
				{
				setState(427);
				let_();
				}
				break;
			case Const:
				enterOuterAlt(_localctx, 3);
				{
				setState(428);
				match(Const);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ContinueStatementContext extends ParserRuleContext {
		public TerminalNode Continue() { return getToken(JavaScriptParser.Continue, 0); }
		public EosContext eos() {
			return getRuleContext(EosContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ContinueStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_continueStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterContinueStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitContinueStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitContinueStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ContinueStatementContext continueStatement() throws RecognitionException {
		ContinueStatementContext _localctx = new ContinueStatementContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_continueStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(431);
			match(Continue);
			setState(434);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,37,_ctx) ) {
			case 1:
				{
				setState(432);
				if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
				setState(433);
				identifier();
				}
				break;
			}
			setState(436);
			eos();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BreakStatementContext extends ParserRuleContext {
		public TerminalNode Break() { return getToken(JavaScriptParser.Break, 0); }
		public EosContext eos() {
			return getRuleContext(EosContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public BreakStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_breakStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterBreakStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitBreakStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitBreakStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BreakStatementContext breakStatement() throws RecognitionException {
		BreakStatementContext _localctx = new BreakStatementContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_breakStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(438);
			match(Break);
			setState(441);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
			case 1:
				{
				setState(439);
				if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
				setState(440);
				identifier();
				}
				break;
			}
			setState(443);
			eos();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ReturnStatementContext extends ParserRuleContext {
		public TerminalNode Return() { return getToken(JavaScriptParser.Return, 0); }
		public EosContext eos() {
			return getRuleContext(EosContext.class,0);
		}
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public ReturnStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_returnStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterReturnStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitReturnStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitReturnStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReturnStatementContext returnStatement() throws RecognitionException {
		ReturnStatementContext _localctx = new ReturnStatementContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_returnStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(445);
			match(Return);
			setState(448);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,39,_ctx) ) {
			case 1:
				{
				setState(446);
				if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
				setState(447);
				expressionSequence();
				}
				break;
			}
			setState(450);
			eos();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class YieldStatementContext extends ParserRuleContext {
		public EosContext eos() {
			return getRuleContext(EosContext.class,0);
		}
		public TerminalNode Yield() { return getToken(JavaScriptParser.Yield, 0); }
		public TerminalNode YieldStar() { return getToken(JavaScriptParser.YieldStar, 0); }
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public YieldStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_yieldStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterYieldStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitYieldStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitYieldStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final YieldStatementContext yieldStatement() throws RecognitionException {
		YieldStatementContext _localctx = new YieldStatementContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_yieldStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(452);
			_la = _input.LA(1);
			if ( !(_la==Yield || _la==YieldStar) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(455);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,40,_ctx) ) {
			case 1:
				{
				setState(453);
				if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
				setState(454);
				expressionSequence();
				}
				break;
			}
			setState(457);
			eos();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class WithStatementContext extends ParserRuleContext {
		public TerminalNode With() { return getToken(JavaScriptParser.With, 0); }
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public WithStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_withStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterWithStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitWithStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitWithStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WithStatementContext withStatement() throws RecognitionException {
		WithStatementContext _localctx = new WithStatementContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_withStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(459);
			match(With);
			setState(460);
			match(OpenParen);
			setState(461);
			expressionSequence();
			setState(462);
			match(CloseParen);
			setState(463);
			statement();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SwitchStatementContext extends ParserRuleContext {
		public TerminalNode Switch() { return getToken(JavaScriptParser.Switch, 0); }
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public CaseBlockContext caseBlock() {
			return getRuleContext(CaseBlockContext.class,0);
		}
		public SwitchStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switchStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterSwitchStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitSwitchStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitSwitchStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SwitchStatementContext switchStatement() throws RecognitionException {
		SwitchStatementContext _localctx = new SwitchStatementContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_switchStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(465);
			match(Switch);
			setState(466);
			match(OpenParen);
			setState(467);
			expressionSequence();
			setState(468);
			match(CloseParen);
			setState(469);
			caseBlock();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CaseBlockContext extends ParserRuleContext {
		public TerminalNode OpenBrace() { return getToken(JavaScriptParser.OpenBrace, 0); }
		public TerminalNode CloseBrace() { return getToken(JavaScriptParser.CloseBrace, 0); }
		public List<CaseClausesContext> caseClauses() {
			return getRuleContexts(CaseClausesContext.class);
		}
		public CaseClausesContext caseClauses(int i) {
			return getRuleContext(CaseClausesContext.class,i);
		}
		public DefaultClauseContext defaultClause() {
			return getRuleContext(DefaultClauseContext.class,0);
		}
		public CaseBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_caseBlock; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterCaseBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitCaseBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitCaseBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CaseBlockContext caseBlock() throws RecognitionException {
		CaseBlockContext _localctx = new CaseBlockContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_caseBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(471);
			match(OpenBrace);
			setState(473);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Case) {
				{
				setState(472);
				caseClauses();
				}
			}

			setState(479);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Default) {
				{
				setState(475);
				defaultClause();
				setState(477);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Case) {
					{
					setState(476);
					caseClauses();
					}
				}

				}
			}

			setState(481);
			match(CloseBrace);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CaseClausesContext extends ParserRuleContext {
		public List<CaseClauseContext> caseClause() {
			return getRuleContexts(CaseClauseContext.class);
		}
		public CaseClauseContext caseClause(int i) {
			return getRuleContext(CaseClauseContext.class,i);
		}
		public CaseClausesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_caseClauses; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterCaseClauses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitCaseClauses(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitCaseClauses(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CaseClausesContext caseClauses() throws RecognitionException {
		CaseClausesContext _localctx = new CaseClausesContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_caseClauses);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(484); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(483);
				caseClause();
				}
				}
				setState(486); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==Case );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CaseClauseContext extends ParserRuleContext {
		public TerminalNode Case() { return getToken(JavaScriptParser.Case, 0); }
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public TerminalNode Colon() { return getToken(JavaScriptParser.Colon, 0); }
		public StatementListContext statementList() {
			return getRuleContext(StatementListContext.class,0);
		}
		public CaseClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_caseClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterCaseClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitCaseClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitCaseClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CaseClauseContext caseClause() throws RecognitionException {
		CaseClauseContext _localctx = new CaseClauseContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_caseClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(488);
			match(Case);
			setState(489);
			expressionSequence();
			setState(490);
			match(Colon);
			setState(492);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,45,_ctx) ) {
			case 1:
				{
				setState(491);
				statementList();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DefaultClauseContext extends ParserRuleContext {
		public TerminalNode Default() { return getToken(JavaScriptParser.Default, 0); }
		public TerminalNode Colon() { return getToken(JavaScriptParser.Colon, 0); }
		public StatementListContext statementList() {
			return getRuleContext(StatementListContext.class,0);
		}
		public DefaultClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defaultClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterDefaultClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitDefaultClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitDefaultClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DefaultClauseContext defaultClause() throws RecognitionException {
		DefaultClauseContext _localctx = new DefaultClauseContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_defaultClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(494);
			match(Default);
			setState(495);
			match(Colon);
			setState(497);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,46,_ctx) ) {
			case 1:
				{
				setState(496);
				statementList();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LabelledStatementContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode Colon() { return getToken(JavaScriptParser.Colon, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public LabelledStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelledStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterLabelledStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitLabelledStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitLabelledStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LabelledStatementContext labelledStatement() throws RecognitionException {
		LabelledStatementContext _localctx = new LabelledStatementContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_labelledStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(499);
			identifier();
			setState(500);
			match(Colon);
			setState(501);
			statement();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ThrowStatementContext extends ParserRuleContext {
		public TerminalNode Throw() { return getToken(JavaScriptParser.Throw, 0); }
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public EosContext eos() {
			return getRuleContext(EosContext.class,0);
		}
		public ThrowStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_throwStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterThrowStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitThrowStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitThrowStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ThrowStatementContext throwStatement() throws RecognitionException {
		ThrowStatementContext _localctx = new ThrowStatementContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_throwStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(503);
			match(Throw);
			setState(504);
			if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
			setState(505);
			expressionSequence();
			setState(506);
			eos();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TryStatementContext extends ParserRuleContext {
		public TerminalNode Try() { return getToken(JavaScriptParser.Try, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public CatchProductionContext catchProduction() {
			return getRuleContext(CatchProductionContext.class,0);
		}
		public FinallyProductionContext finallyProduction() {
			return getRuleContext(FinallyProductionContext.class,0);
		}
		public TryStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tryStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterTryStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitTryStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitTryStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TryStatementContext tryStatement() throws RecognitionException {
		TryStatementContext _localctx = new TryStatementContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_tryStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(508);
			match(Try);
			setState(509);
			block();
			setState(515);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Catch:
				{
				setState(510);
				catchProduction();
				setState(512);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,47,_ctx) ) {
				case 1:
					{
					setState(511);
					finallyProduction();
					}
					break;
				}
				}
				break;
			case Finally:
				{
				setState(514);
				finallyProduction();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CatchProductionContext extends ParserRuleContext {
		public TerminalNode Catch() { return getToken(JavaScriptParser.Catch, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public AssignableContext assignable() {
			return getRuleContext(AssignableContext.class,0);
		}
		public CatchProductionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_catchProduction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterCatchProduction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitCatchProduction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitCatchProduction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CatchProductionContext catchProduction() throws RecognitionException {
		CatchProductionContext _localctx = new CatchProductionContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_catchProduction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(517);
			match(Catch);
			setState(523);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OpenParen) {
				{
				setState(518);
				match(OpenParen);
				setState(520);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OpenBracket || _la==OpenBrace || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 1125899906842623L) != 0)) {
					{
					setState(519);
					assignable();
					}
				}

				setState(522);
				match(CloseParen);
				}
			}

			setState(525);
			block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FinallyProductionContext extends ParserRuleContext {
		public TerminalNode Finally() { return getToken(JavaScriptParser.Finally, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public FinallyProductionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_finallyProduction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterFinallyProduction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitFinallyProduction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitFinallyProduction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FinallyProductionContext finallyProduction() throws RecognitionException {
		FinallyProductionContext _localctx = new FinallyProductionContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_finallyProduction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(527);
			match(Finally);
			setState(528);
			block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DebuggerStatementContext extends ParserRuleContext {
		public TerminalNode Debugger() { return getToken(JavaScriptParser.Debugger, 0); }
		public EosContext eos() {
			return getRuleContext(EosContext.class,0);
		}
		public DebuggerStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_debuggerStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterDebuggerStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitDebuggerStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitDebuggerStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DebuggerStatementContext debuggerStatement() throws RecognitionException {
		DebuggerStatementContext _localctx = new DebuggerStatementContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_debuggerStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(530);
			match(Debugger);
			setState(531);
			eos();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionDeclarationContext extends ParserRuleContext {
		public TerminalNode Function_() { return getToken(JavaScriptParser.Function_, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public FunctionBodyContext functionBody() {
			return getRuleContext(FunctionBodyContext.class,0);
		}
		public TerminalNode Async() { return getToken(JavaScriptParser.Async, 0); }
		public TerminalNode Multiply() { return getToken(JavaScriptParser.Multiply, 0); }
		public FormalParameterListContext formalParameterList() {
			return getRuleContext(FormalParameterListContext.class,0);
		}
		public FunctionDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterFunctionDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitFunctionDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitFunctionDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionDeclarationContext functionDeclaration() throws RecognitionException {
		FunctionDeclarationContext _localctx = new FunctionDeclarationContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_functionDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(534);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Async) {
				{
				setState(533);
				match(Async);
				}
			}

			setState(536);
			match(Function_);
			setState(538);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Multiply) {
				{
				setState(537);
				match(Multiply);
				}
			}

			setState(540);
			identifier();
			setState(541);
			match(OpenParen);
			setState(543);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 262688L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 1125899906842623L) != 0)) {
				{
				setState(542);
				formalParameterList();
				}
			}

			setState(545);
			match(CloseParen);
			setState(546);
			functionBody();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ClassDeclarationContext extends ParserRuleContext {
		public TerminalNode Class() { return getToken(JavaScriptParser.Class, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ClassTailContext classTail() {
			return getRuleContext(ClassTailContext.class,0);
		}
		public ClassDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterClassDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitClassDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitClassDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassDeclarationContext classDeclaration() throws RecognitionException {
		ClassDeclarationContext _localctx = new ClassDeclarationContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_classDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(548);
			match(Class);
			setState(549);
			identifier();
			setState(550);
			classTail();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ClassTailContext extends ParserRuleContext {
		public TerminalNode OpenBrace() { return getToken(JavaScriptParser.OpenBrace, 0); }
		public TerminalNode CloseBrace() { return getToken(JavaScriptParser.CloseBrace, 0); }
		public TerminalNode Extends() { return getToken(JavaScriptParser.Extends, 0); }
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public List<ClassElementContext> classElement() {
			return getRuleContexts(ClassElementContext.class);
		}
		public ClassElementContext classElement(int i) {
			return getRuleContext(ClassElementContext.class,i);
		}
		public ClassTailContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classTail; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterClassTail(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitClassTail(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitClassTail(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassTailContext classTail() throws RecognitionException {
		ClassTailContext _localctx = new ClassTailContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_classTail);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(554);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Extends) {
				{
				setState(552);
				match(Extends);
				setState(553);
				singleExpression();
				}
			}

			setState(556);
			match(OpenBrace);
			setState(560);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,55,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(557);
					classElement();
					}
					} 
				}
				setState(562);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,55,_ctx);
			}
			setState(563);
			match(CloseBrace);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ClassElementContext extends ParserRuleContext {
		public MethodDefinitionContext methodDefinition() {
			return getRuleContext(MethodDefinitionContext.class,0);
		}
		public TerminalNode Static() { return getToken(JavaScriptParser.Static, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public FieldDefinitionContext fieldDefinition() {
			return getRuleContext(FieldDefinitionContext.class,0);
		}
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public EmptyStatement_Context emptyStatement_() {
			return getRuleContext(EmptyStatement_Context.class,0);
		}
		public ClassElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterClassElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitClassElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitClassElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassElementContext classElement() throws RecognitionException {
		ClassElementContext _localctx = new ClassElementContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_classElement);
		try {
			setState(584);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,59,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(568);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,56,_ctx) ) {
				case 1:
					{
					setState(565);
					match(Static);
					}
					break;
				case 2:
					{
					setState(566);
					if (!(this.n("static"))) throw new FailedPredicateException(this, "this.n(\"static\")");
					setState(567);
					identifier();
					}
					break;
				}
				setState(570);
				methodDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(574);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,57,_ctx) ) {
				case 1:
					{
					setState(571);
					match(Static);
					}
					break;
				case 2:
					{
					setState(572);
					if (!(this.n("static"))) throw new FailedPredicateException(this, "this.n(\"static\")");
					setState(573);
					identifier();
					}
					break;
				}
				setState(576);
				fieldDefinition();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(580);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,58,_ctx) ) {
				case 1:
					{
					setState(577);
					match(Static);
					}
					break;
				case 2:
					{
					setState(578);
					if (!(this.n("static"))) throw new FailedPredicateException(this, "this.n(\"static\")");
					setState(579);
					identifier();
					}
					break;
				}
				setState(582);
				block();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(583);
				emptyStatement_();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MethodDefinitionContext extends ParserRuleContext {
		public ClassElementNameContext classElementName() {
			return getRuleContext(ClassElementNameContext.class,0);
		}
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public FunctionBodyContext functionBody() {
			return getRuleContext(FunctionBodyContext.class,0);
		}
		public TerminalNode Async() { return getToken(JavaScriptParser.Async, 0); }
		public TerminalNode Multiply() { return getToken(JavaScriptParser.Multiply, 0); }
		public FormalParameterListContext formalParameterList() {
			return getRuleContext(FormalParameterListContext.class,0);
		}
		public GetterContext getter() {
			return getRuleContext(GetterContext.class,0);
		}
		public SetterContext setter() {
			return getRuleContext(SetterContext.class,0);
		}
		public MethodDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterMethodDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitMethodDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitMethodDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodDefinitionContext methodDefinition() throws RecognitionException {
		MethodDefinitionContext _localctx = new MethodDefinitionContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_methodDefinition);
		int _la;
		try {
			setState(620);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,66,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(588);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,60,_ctx) ) {
				case 1:
					{
					setState(586);
					match(Async);
					setState(587);
					if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
					}
					break;
				}
				setState(591);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Multiply) {
					{
					setState(590);
					match(Multiply);
					}
				}

				setState(593);
				classElementName();
				setState(594);
				match(OpenParen);
				setState(596);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 262688L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 1125899906842623L) != 0)) {
					{
					setState(595);
					formalParameterList();
					}
				}

				setState(598);
				match(CloseParen);
				setState(599);
				functionBody();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(602);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,63,_ctx) ) {
				case 1:
					{
					setState(601);
					match(Multiply);
					}
					break;
				}
				setState(604);
				getter();
				setState(605);
				match(OpenParen);
				setState(606);
				match(CloseParen);
				setState(607);
				functionBody();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(610);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,64,_ctx) ) {
				case 1:
					{
					setState(609);
					match(Multiply);
					}
					break;
				}
				setState(612);
				setter();
				setState(613);
				match(OpenParen);
				setState(615);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 262688L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 1125899906842623L) != 0)) {
					{
					setState(614);
					formalParameterList();
					}
				}

				setState(617);
				match(CloseParen);
				setState(618);
				functionBody();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FieldDefinitionContext extends ParserRuleContext {
		public ClassElementNameContext classElementName() {
			return getRuleContext(ClassElementNameContext.class,0);
		}
		public InitializerContext initializer() {
			return getRuleContext(InitializerContext.class,0);
		}
		public FieldDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterFieldDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitFieldDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitFieldDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldDefinitionContext fieldDefinition() throws RecognitionException {
		FieldDefinitionContext _localctx = new FieldDefinitionContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_fieldDefinition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(622);
			classElementName();
			setState(624);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,67,_ctx) ) {
			case 1:
				{
				setState(623);
				initializer();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ClassElementNameContext extends ParserRuleContext {
		public PropertyNameContext propertyName() {
			return getRuleContext(PropertyNameContext.class,0);
		}
		public PrivateIdentifierContext privateIdentifier() {
			return getRuleContext(PrivateIdentifierContext.class,0);
		}
		public ClassElementNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classElementName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterClassElementName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitClassElementName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitClassElementName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassElementNameContext classElementName() throws RecognitionException {
		ClassElementNameContext _localctx = new ClassElementNameContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_classElementName);
		try {
			setState(628);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OpenBracket:
			case NullLiteral:
			case BooleanLiteral:
			case DecimalLiteral:
			case HexIntegerLiteral:
			case OctalIntegerLiteral:
			case OctalIntegerLiteral2:
			case BinaryIntegerLiteral:
			case Break:
			case Do:
			case Instanceof:
			case Typeof:
			case Case:
			case Else:
			case New:
			case Var:
			case Catch:
			case Finally:
			case Return:
			case Void:
			case Continue:
			case For:
			case Switch:
			case While:
			case Debugger:
			case Function_:
			case This:
			case With:
			case Default:
			case If:
			case Throw:
			case Delete:
			case In:
			case Try:
			case As:
			case From:
			case Of:
			case Yield:
			case YieldStar:
			case Class:
			case Enum:
			case Extends:
			case Super:
			case Const:
			case Export:
			case Import:
			case Async:
			case Await:
			case Implements:
			case StrictLet:
			case NonStrictLet:
			case Private:
			case Public:
			case Interface:
			case Package:
			case Protected:
			case Static:
			case Identifier:
			case StringLiteral:
				enterOuterAlt(_localctx, 1);
				{
				setState(626);
				propertyName();
				}
				break;
			case Hashtag:
				enterOuterAlt(_localctx, 2);
				{
				setState(627);
				privateIdentifier();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PrivateIdentifierContext extends ParserRuleContext {
		public TerminalNode Hashtag() { return getToken(JavaScriptParser.Hashtag, 0); }
		public IdentifierNameContext identifierName() {
			return getRuleContext(IdentifierNameContext.class,0);
		}
		public PrivateIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_privateIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterPrivateIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitPrivateIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitPrivateIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrivateIdentifierContext privateIdentifier() throws RecognitionException {
		PrivateIdentifierContext _localctx = new PrivateIdentifierContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_privateIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(630);
			match(Hashtag);
			setState(631);
			identifierName();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FormalParameterListContext extends ParserRuleContext {
		public List<FormalParameterArgContext> formalParameterArg() {
			return getRuleContexts(FormalParameterArgContext.class);
		}
		public FormalParameterArgContext formalParameterArg(int i) {
			return getRuleContext(FormalParameterArgContext.class,i);
		}
		public List<TerminalNode> Comma() { return getTokens(JavaScriptParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(JavaScriptParser.Comma, i);
		}
		public RestParameterArgContext restParameterArg() {
			return getRuleContext(RestParameterArgContext.class,0);
		}
		public FormalParameterListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalParameterList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterFormalParameterList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitFormalParameterList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitFormalParameterList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalParameterListContext formalParameterList() throws RecognitionException {
		FormalParameterListContext _localctx = new FormalParameterListContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_formalParameterList);
		int _la;
		try {
			int _alt;
			setState(646);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OpenBracket:
			case OpenBrace:
			case Break:
			case Do:
			case Instanceof:
			case Typeof:
			case Case:
			case Else:
			case New:
			case Var:
			case Catch:
			case Finally:
			case Return:
			case Void:
			case Continue:
			case For:
			case Switch:
			case While:
			case Debugger:
			case Function_:
			case This:
			case With:
			case Default:
			case If:
			case Throw:
			case Delete:
			case In:
			case Try:
			case As:
			case From:
			case Of:
			case Yield:
			case YieldStar:
			case Class:
			case Enum:
			case Extends:
			case Super:
			case Const:
			case Export:
			case Import:
			case Async:
			case Await:
			case Implements:
			case StrictLet:
			case NonStrictLet:
			case Private:
			case Public:
			case Interface:
			case Package:
			case Protected:
			case Static:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(633);
				formalParameterArg();
				setState(638);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,69,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(634);
						match(Comma);
						setState(635);
						formalParameterArg();
						}
						} 
					}
					setState(640);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,69,_ctx);
				}
				setState(643);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(641);
					match(Comma);
					setState(642);
					restParameterArg();
					}
				}

				}
				break;
			case Ellipsis:
				enterOuterAlt(_localctx, 2);
				{
				setState(645);
				restParameterArg();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FormalParameterArgContext extends ParserRuleContext {
		public AssignableContext assignable() {
			return getRuleContext(AssignableContext.class,0);
		}
		public TerminalNode Assign() { return getToken(JavaScriptParser.Assign, 0); }
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public FormalParameterArgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalParameterArg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterFormalParameterArg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitFormalParameterArg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitFormalParameterArg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalParameterArgContext formalParameterArg() throws RecognitionException {
		FormalParameterArgContext _localctx = new FormalParameterArgContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_formalParameterArg);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(648);
			assignable();
			setState(651);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Assign) {
				{
				setState(649);
				match(Assign);
				setState(650);
				singleExpression();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RestParameterArgContext extends ParserRuleContext {
		public TerminalNode Ellipsis() { return getToken(JavaScriptParser.Ellipsis, 0); }
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public RestParameterArgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_restParameterArg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterRestParameterArg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitRestParameterArg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitRestParameterArg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RestParameterArgContext restParameterArg() throws RecognitionException {
		RestParameterArgContext _localctx = new RestParameterArgContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_restParameterArg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(653);
			match(Ellipsis);
			setState(654);
			singleExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionBodyContext extends ParserRuleContext {
		public TerminalNode OpenBrace() { return getToken(JavaScriptParser.OpenBrace, 0); }
		public TerminalNode CloseBrace() { return getToken(JavaScriptParser.CloseBrace, 0); }
		public SourceElementsContext sourceElements() {
			return getRuleContext(SourceElementsContext.class,0);
		}
		public FunctionBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterFunctionBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitFunctionBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitFunctionBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionBodyContext functionBody() throws RecognitionException {
		FunctionBodyContext _localctx = new FunctionBodyContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_functionBody);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(656);
			match(OpenBrace);
			setState(658);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,73,_ctx) ) {
			case 1:
				{
				setState(657);
				sourceElements();
				}
				break;
			}
			setState(660);
			match(CloseBrace);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SourceElementsContext extends ParserRuleContext {
		public List<SourceElementContext> sourceElement() {
			return getRuleContexts(SourceElementContext.class);
		}
		public SourceElementContext sourceElement(int i) {
			return getRuleContext(SourceElementContext.class,i);
		}
		public SourceElementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sourceElements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterSourceElements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitSourceElements(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitSourceElements(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SourceElementsContext sourceElements() throws RecognitionException {
		SourceElementsContext _localctx = new SourceElementsContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_sourceElements);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(663); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(662);
					sourceElement();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(665); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,74,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArrayLiteralContext extends ParserRuleContext {
		public TerminalNode OpenBracket() { return getToken(JavaScriptParser.OpenBracket, 0); }
		public ElementListContext elementList() {
			return getRuleContext(ElementListContext.class,0);
		}
		public TerminalNode CloseBracket() { return getToken(JavaScriptParser.CloseBracket, 0); }
		public ArrayLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterArrayLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitArrayLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitArrayLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayLiteralContext arrayLiteral() throws RecognitionException {
		ArrayLiteralContext _localctx = new ArrayLiteralContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_arrayLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(667);
			match(OpenBracket);
			setState(668);
			elementList();
			setState(669);
			match(CloseBracket);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ElementListContext extends ParserRuleContext {
		public List<TerminalNode> Comma() { return getTokens(JavaScriptParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(JavaScriptParser.Comma, i);
		}
		public List<ArrayElementContext> arrayElement() {
			return getRuleContexts(ArrayElementContext.class);
		}
		public ArrayElementContext arrayElement(int i) {
			return getRuleContext(ArrayElementContext.class,i);
		}
		public ElementListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elementList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterElementList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitElementList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitElementList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElementListContext elementList() throws RecognitionException {
		ElementListContext _localctx = new ElementListContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_elementList);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(674);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,75,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(671);
					match(Comma);
					}
					} 
				}
				setState(676);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,75,_ctx);
			}
			setState(678);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -4611686018361064784L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305843009213693951L) != 0)) {
				{
				setState(677);
				arrayElement();
				}
			}

			setState(688);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,78,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(681); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(680);
						match(Comma);
						}
						}
						setState(683); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==Comma );
					setState(685);
					arrayElement();
					}
					} 
				}
				setState(690);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,78,_ctx);
			}
			setState(694);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==Comma) {
				{
				{
				setState(691);
				match(Comma);
				}
				}
				setState(696);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArrayElementContext extends ParserRuleContext {
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public TerminalNode Ellipsis() { return getToken(JavaScriptParser.Ellipsis, 0); }
		public ArrayElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterArrayElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitArrayElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitArrayElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayElementContext arrayElement() throws RecognitionException {
		ArrayElementContext _localctx = new ArrayElementContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_arrayElement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(698);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Ellipsis) {
				{
				setState(697);
				match(Ellipsis);
				}
			}

			setState(700);
			singleExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PropertyAssignmentContext extends ParserRuleContext {
		public PropertyAssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyAssignment; }
	 
		public PropertyAssignmentContext() { }
		public void copyFrom(PropertyAssignmentContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PropertyExpressionAssignmentContext extends PropertyAssignmentContext {
		public PropertyNameContext propertyName() {
			return getRuleContext(PropertyNameContext.class,0);
		}
		public TerminalNode Colon() { return getToken(JavaScriptParser.Colon, 0); }
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public PropertyExpressionAssignmentContext(PropertyAssignmentContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterPropertyExpressionAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitPropertyExpressionAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitPropertyExpressionAssignment(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ComputedPropertyExpressionAssignmentContext extends PropertyAssignmentContext {
		public TerminalNode OpenBracket() { return getToken(JavaScriptParser.OpenBracket, 0); }
		public List<SingleExpressionContext> singleExpression() {
			return getRuleContexts(SingleExpressionContext.class);
		}
		public SingleExpressionContext singleExpression(int i) {
			return getRuleContext(SingleExpressionContext.class,i);
		}
		public TerminalNode CloseBracket() { return getToken(JavaScriptParser.CloseBracket, 0); }
		public TerminalNode Colon() { return getToken(JavaScriptParser.Colon, 0); }
		public ComputedPropertyExpressionAssignmentContext(PropertyAssignmentContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterComputedPropertyExpressionAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitComputedPropertyExpressionAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitComputedPropertyExpressionAssignment(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PropertyShorthandContext extends PropertyAssignmentContext {
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public TerminalNode Ellipsis() { return getToken(JavaScriptParser.Ellipsis, 0); }
		public PropertyShorthandContext(PropertyAssignmentContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterPropertyShorthand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitPropertyShorthand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitPropertyShorthand(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PropertySetterContext extends PropertyAssignmentContext {
		public SetterContext setter() {
			return getRuleContext(SetterContext.class,0);
		}
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public FormalParameterArgContext formalParameterArg() {
			return getRuleContext(FormalParameterArgContext.class,0);
		}
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public FunctionBodyContext functionBody() {
			return getRuleContext(FunctionBodyContext.class,0);
		}
		public PropertySetterContext(PropertyAssignmentContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterPropertySetter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitPropertySetter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitPropertySetter(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PropertyGetterContext extends PropertyAssignmentContext {
		public GetterContext getter() {
			return getRuleContext(GetterContext.class,0);
		}
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public FunctionBodyContext functionBody() {
			return getRuleContext(FunctionBodyContext.class,0);
		}
		public PropertyGetterContext(PropertyAssignmentContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterPropertyGetter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitPropertyGetter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitPropertyGetter(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FunctionPropertyContext extends PropertyAssignmentContext {
		public PropertyNameContext propertyName() {
			return getRuleContext(PropertyNameContext.class,0);
		}
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public FunctionBodyContext functionBody() {
			return getRuleContext(FunctionBodyContext.class,0);
		}
		public TerminalNode Async() { return getToken(JavaScriptParser.Async, 0); }
		public TerminalNode Multiply() { return getToken(JavaScriptParser.Multiply, 0); }
		public FormalParameterListContext formalParameterList() {
			return getRuleContext(FormalParameterListContext.class,0);
		}
		public FunctionPropertyContext(PropertyAssignmentContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterFunctionProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitFunctionProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitFunctionProperty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyAssignmentContext propertyAssignment() throws RecognitionException {
		PropertyAssignmentContext _localctx = new PropertyAssignmentContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_propertyAssignment);
		int _la;
		try {
			setState(741);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,85,_ctx) ) {
			case 1:
				_localctx = new PropertyExpressionAssignmentContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(702);
				propertyName();
				setState(703);
				match(Colon);
				setState(704);
				singleExpression();
				}
				break;
			case 2:
				_localctx = new ComputedPropertyExpressionAssignmentContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(706);
				match(OpenBracket);
				setState(707);
				singleExpression();
				setState(708);
				match(CloseBracket);
				setState(709);
				match(Colon);
				setState(710);
				singleExpression();
				}
				break;
			case 3:
				_localctx = new FunctionPropertyContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(713);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,81,_ctx) ) {
				case 1:
					{
					setState(712);
					match(Async);
					}
					break;
				}
				setState(716);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Multiply) {
					{
					setState(715);
					match(Multiply);
					}
				}

				setState(718);
				propertyName();
				setState(719);
				match(OpenParen);
				setState(721);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 262688L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 1125899906842623L) != 0)) {
					{
					setState(720);
					formalParameterList();
					}
				}

				setState(723);
				match(CloseParen);
				setState(724);
				functionBody();
				}
				break;
			case 4:
				_localctx = new PropertyGetterContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(726);
				getter();
				setState(727);
				match(OpenParen);
				setState(728);
				match(CloseParen);
				setState(729);
				functionBody();
				}
				break;
			case 5:
				_localctx = new PropertySetterContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(731);
				setter();
				setState(732);
				match(OpenParen);
				setState(733);
				formalParameterArg();
				setState(734);
				match(CloseParen);
				setState(735);
				functionBody();
				}
				break;
			case 6:
				_localctx = new PropertyShorthandContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(738);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Ellipsis) {
					{
					setState(737);
					match(Ellipsis);
					}
				}

				setState(740);
				singleExpression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PropertyNameContext extends ParserRuleContext {
		public IdentifierNameContext identifierName() {
			return getRuleContext(IdentifierNameContext.class,0);
		}
		public TerminalNode StringLiteral() { return getToken(JavaScriptParser.StringLiteral, 0); }
		public NumericLiteralContext numericLiteral() {
			return getRuleContext(NumericLiteralContext.class,0);
		}
		public TerminalNode OpenBracket() { return getToken(JavaScriptParser.OpenBracket, 0); }
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public TerminalNode CloseBracket() { return getToken(JavaScriptParser.CloseBracket, 0); }
		public PropertyNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterPropertyName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitPropertyName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitPropertyName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyNameContext propertyName() throws RecognitionException {
		PropertyNameContext _localctx = new PropertyNameContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_propertyName);
		try {
			setState(750);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NullLiteral:
			case BooleanLiteral:
			case Break:
			case Do:
			case Instanceof:
			case Typeof:
			case Case:
			case Else:
			case New:
			case Var:
			case Catch:
			case Finally:
			case Return:
			case Void:
			case Continue:
			case For:
			case Switch:
			case While:
			case Debugger:
			case Function_:
			case This:
			case With:
			case Default:
			case If:
			case Throw:
			case Delete:
			case In:
			case Try:
			case As:
			case From:
			case Of:
			case Yield:
			case YieldStar:
			case Class:
			case Enum:
			case Extends:
			case Super:
			case Const:
			case Export:
			case Import:
			case Async:
			case Await:
			case Implements:
			case StrictLet:
			case NonStrictLet:
			case Private:
			case Public:
			case Interface:
			case Package:
			case Protected:
			case Static:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(743);
				identifierName();
				}
				break;
			case StringLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(744);
				match(StringLiteral);
				}
				break;
			case DecimalLiteral:
			case HexIntegerLiteral:
			case OctalIntegerLiteral:
			case OctalIntegerLiteral2:
			case BinaryIntegerLiteral:
				enterOuterAlt(_localctx, 3);
				{
				setState(745);
				numericLiteral();
				}
				break;
			case OpenBracket:
				enterOuterAlt(_localctx, 4);
				{
				setState(746);
				match(OpenBracket);
				setState(747);
				singleExpression();
				setState(748);
				match(CloseBracket);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgumentsContext extends ParserRuleContext {
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public List<TerminalNode> Comma() { return getTokens(JavaScriptParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(JavaScriptParser.Comma, i);
		}
		public ArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentsContext arguments() throws RecognitionException {
		ArgumentsContext _localctx = new ArgumentsContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_arguments);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(752);
			match(OpenParen);
			setState(764);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -4611686018361064784L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 2305843009213693951L) != 0)) {
				{
				setState(753);
				argument();
				setState(758);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,87,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(754);
						match(Comma);
						setState(755);
						argument();
						}
						} 
					}
					setState(760);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,87,_ctx);
				}
				setState(762);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(761);
					match(Comma);
					}
				}

				}
			}

			setState(766);
			match(CloseParen);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgumentContext extends ParserRuleContext {
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode Ellipsis() { return getToken(JavaScriptParser.Ellipsis, 0); }
		public ArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentContext argument() throws RecognitionException {
		ArgumentContext _localctx = new ArgumentContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_argument);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(769);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Ellipsis) {
				{
				setState(768);
				match(Ellipsis);
				}
			}

			setState(773);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,91,_ctx) ) {
			case 1:
				{
				setState(771);
				singleExpression();
				}
				break;
			case 2:
				{
				setState(772);
				identifier();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionSequenceContext extends ParserRuleContext {
		public List<SingleExpressionContext> singleExpression() {
			return getRuleContexts(SingleExpressionContext.class);
		}
		public SingleExpressionContext singleExpression(int i) {
			return getRuleContext(SingleExpressionContext.class,i);
		}
		public List<TerminalNode> Comma() { return getTokens(JavaScriptParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(JavaScriptParser.Comma, i);
		}
		public ExpressionSequenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionSequence; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterExpressionSequence(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitExpressionSequence(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitExpressionSequence(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionSequenceContext expressionSequence() throws RecognitionException {
		ExpressionSequenceContext _localctx = new ExpressionSequenceContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_expressionSequence);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(775);
			singleExpression();
			setState(780);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,92,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(776);
					match(Comma);
					setState(777);
					singleExpression();
					}
					} 
				}
				setState(782);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,92,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SingleExpressionContext extends ParserRuleContext {
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public SingleExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterSingleExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitSingleExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitSingleExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleExpressionContext singleExpression() throws RecognitionException {
		SingleExpressionContext _localctx = new SingleExpressionContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_singleExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(783);
			singleExpressionImpl(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SingleExpressionImplContext extends ParserRuleContext {
		public SingleExpressionImplContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleExpressionImpl; }
	 
		public SingleExpressionImplContext() { }
		public void copyFrom(SingleExpressionImplContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TemplateStringExpressionContext extends SingleExpressionImplContext {
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public TemplateStringLiteralContext templateStringLiteral() {
			return getRuleContext(TemplateStringLiteralContext.class,0);
		}
		public TemplateStringExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterTemplateStringExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitTemplateStringExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitTemplateStringExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TernaryExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode QuestionMark() { return getToken(JavaScriptParser.QuestionMark, 0); }
		public TerminalNode Colon() { return getToken(JavaScriptParser.Colon, 0); }
		public TernaryExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterTernaryExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitTernaryExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitTernaryExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LogicalAndExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode And() { return getToken(JavaScriptParser.And, 0); }
		public LogicalAndExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterLogicalAndExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitLogicalAndExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitLogicalAndExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PowerExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode Power() { return getToken(JavaScriptParser.Power, 0); }
		public PowerExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterPowerExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitPowerExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitPowerExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PreIncrementExpressionContext extends SingleExpressionImplContext {
		public TerminalNode PlusPlus() { return getToken(JavaScriptParser.PlusPlus, 0); }
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public PreIncrementExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterPreIncrementExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitPreIncrementExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitPreIncrementExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ObjectLiteralExpressionContext extends SingleExpressionImplContext {
		public ObjectLiteralContext objectLiteral() {
			return getRuleContext(ObjectLiteralContext.class,0);
		}
		public ObjectLiteralExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterObjectLiteralExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitObjectLiteralExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitObjectLiteralExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MetaExpressionContext extends SingleExpressionImplContext {
		public TerminalNode New() { return getToken(JavaScriptParser.New, 0); }
		public TerminalNode Dot() { return getToken(JavaScriptParser.Dot, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public MetaExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterMetaExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitMetaExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitMetaExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class InExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode In() { return getToken(JavaScriptParser.In, 0); }
		public InExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterInExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitInExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitInExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LogicalOrExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode Or() { return getToken(JavaScriptParser.Or, 0); }
		public LogicalOrExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterLogicalOrExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitLogicalOrExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitLogicalOrExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OptionalChainExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode QuestionMarkDot() { return getToken(JavaScriptParser.QuestionMarkDot, 0); }
		public OptionalChainExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterOptionalChainExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitOptionalChainExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitOptionalChainExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NotExpressionContext extends SingleExpressionImplContext {
		public TerminalNode Not() { return getToken(JavaScriptParser.Not, 0); }
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public NotExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterNotExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitNotExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitNotExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PreDecreaseExpressionContext extends SingleExpressionImplContext {
		public TerminalNode MinusMinus() { return getToken(JavaScriptParser.MinusMinus, 0); }
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public PreDecreaseExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterPreDecreaseExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitPreDecreaseExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitPreDecreaseExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ArgumentsExpressionContext extends SingleExpressionImplContext {
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public ArgumentsExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterArgumentsExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitArgumentsExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitArgumentsExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AwaitExpressionContext extends SingleExpressionImplContext {
		public TerminalNode Await() { return getToken(JavaScriptParser.Await, 0); }
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public AwaitExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterAwaitExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitAwaitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitAwaitExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ThisExpressionContext extends SingleExpressionImplContext {
		public TerminalNode This() { return getToken(JavaScriptParser.This, 0); }
		public ThisExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterThisExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitThisExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitThisExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FunctionExpressionContext extends SingleExpressionImplContext {
		public AnonymousFunctionContext anonymousFunction() {
			return getRuleContext(AnonymousFunctionContext.class,0);
		}
		public FunctionExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterFunctionExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitFunctionExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitFunctionExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class UnaryMinusExpressionContext extends SingleExpressionImplContext {
		public TerminalNode Minus() { return getToken(JavaScriptParser.Minus, 0); }
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public UnaryMinusExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterUnaryMinusExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitUnaryMinusExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitUnaryMinusExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AssignmentExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode Assign() { return getToken(JavaScriptParser.Assign, 0); }
		public AssignmentExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterAssignmentExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitAssignmentExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitAssignmentExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PostDecreaseExpressionContext extends SingleExpressionImplContext {
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public TerminalNode MinusMinus() { return getToken(JavaScriptParser.MinusMinus, 0); }
		public PostDecreaseExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterPostDecreaseExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitPostDecreaseExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitPostDecreaseExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TypeofExpressionContext extends SingleExpressionImplContext {
		public TerminalNode Typeof() { return getToken(JavaScriptParser.Typeof, 0); }
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public TypeofExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterTypeofExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitTypeofExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitTypeofExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class InstanceofExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode Instanceof() { return getToken(JavaScriptParser.Instanceof, 0); }
		public InstanceofExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterInstanceofExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitInstanceofExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitInstanceofExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class UnaryPlusExpressionContext extends SingleExpressionImplContext {
		public TerminalNode Plus() { return getToken(JavaScriptParser.Plus, 0); }
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public UnaryPlusExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterUnaryPlusExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitUnaryPlusExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitUnaryPlusExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class DeleteExpressionContext extends SingleExpressionImplContext {
		public TerminalNode Delete() { return getToken(JavaScriptParser.Delete, 0); }
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public DeleteExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterDeleteExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitDeleteExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitDeleteExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ImportExpressionContext extends SingleExpressionImplContext {
		public TerminalNode Import() { return getToken(JavaScriptParser.Import, 0); }
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public ImportExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterImportExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitImportExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitImportExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class EqualityExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode Equals_() { return getToken(JavaScriptParser.Equals_, 0); }
		public TerminalNode NotEquals() { return getToken(JavaScriptParser.NotEquals, 0); }
		public TerminalNode IdentityEquals() { return getToken(JavaScriptParser.IdentityEquals, 0); }
		public TerminalNode IdentityNotEquals() { return getToken(JavaScriptParser.IdentityNotEquals, 0); }
		public EqualityExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterEqualityExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitEqualityExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitEqualityExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BitXOrExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode BitXOr() { return getToken(JavaScriptParser.BitXOr, 0); }
		public BitXOrExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterBitXOrExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitBitXOrExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitBitXOrExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class SuperExpressionContext extends SingleExpressionImplContext {
		public TerminalNode Super() { return getToken(JavaScriptParser.Super, 0); }
		public SuperExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterSuperExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitSuperExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitSuperExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MultiplicativeExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode Multiply() { return getToken(JavaScriptParser.Multiply, 0); }
		public TerminalNode Divide() { return getToken(JavaScriptParser.Divide, 0); }
		public TerminalNode Modulus() { return getToken(JavaScriptParser.Modulus, 0); }
		public MultiplicativeExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterMultiplicativeExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitMultiplicativeExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitMultiplicativeExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BitShiftExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode LeftShiftArithmetic() { return getToken(JavaScriptParser.LeftShiftArithmetic, 0); }
		public TerminalNode RightShiftArithmetic() { return getToken(JavaScriptParser.RightShiftArithmetic, 0); }
		public TerminalNode RightShiftLogical() { return getToken(JavaScriptParser.RightShiftLogical, 0); }
		public BitShiftExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterBitShiftExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitBitShiftExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitBitShiftExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParenthesizedExpressionContext extends SingleExpressionImplContext {
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public ParenthesizedExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterParenthesizedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitParenthesizedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitParenthesizedExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AdditiveExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode Plus() { return getToken(JavaScriptParser.Plus, 0); }
		public TerminalNode Minus() { return getToken(JavaScriptParser.Minus, 0); }
		public AdditiveExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterAdditiveExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitAdditiveExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitAdditiveExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class RelationalExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode LessThan() { return getToken(JavaScriptParser.LessThan, 0); }
		public TerminalNode MoreThan() { return getToken(JavaScriptParser.MoreThan, 0); }
		public TerminalNode LessThanEquals() { return getToken(JavaScriptParser.LessThanEquals, 0); }
		public TerminalNode GreaterThanEquals() { return getToken(JavaScriptParser.GreaterThanEquals, 0); }
		public RelationalExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterRelationalExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitRelationalExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitRelationalExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PostIncrementExpressionContext extends SingleExpressionImplContext {
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public TerminalNode PlusPlus() { return getToken(JavaScriptParser.PlusPlus, 0); }
		public PostIncrementExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterPostIncrementExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitPostIncrementExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitPostIncrementExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class YieldExpressionContext extends SingleExpressionImplContext {
		public YieldStatementContext yieldStatement() {
			return getRuleContext(YieldStatementContext.class,0);
		}
		public YieldExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterYieldExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitYieldExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitYieldExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BitNotExpressionContext extends SingleExpressionImplContext {
		public TerminalNode BitNot() { return getToken(JavaScriptParser.BitNot, 0); }
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public BitNotExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterBitNotExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitBitNotExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitBitNotExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NewExpressionContext extends SingleExpressionImplContext {
		public TerminalNode New() { return getToken(JavaScriptParser.New, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public NewExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterNewExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitNewExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitNewExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LiteralExpressionContext extends SingleExpressionImplContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public LiteralExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterLiteralExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitLiteralExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitLiteralExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ArrayLiteralExpressionContext extends SingleExpressionImplContext {
		public ArrayLiteralContext arrayLiteral() {
			return getRuleContext(ArrayLiteralContext.class,0);
		}
		public ArrayLiteralExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterArrayLiteralExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitArrayLiteralExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitArrayLiteralExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MemberDotExpressionContext extends SingleExpressionImplContext {
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public TerminalNode Dot() { return getToken(JavaScriptParser.Dot, 0); }
		public IdentifierNameContext identifierName() {
			return getRuleContext(IdentifierNameContext.class,0);
		}
		public TerminalNode QuestionMark() { return getToken(JavaScriptParser.QuestionMark, 0); }
		public TerminalNode Hashtag() { return getToken(JavaScriptParser.Hashtag, 0); }
		public MemberDotExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterMemberDotExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitMemberDotExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitMemberDotExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ClassExpressionContext extends SingleExpressionImplContext {
		public TerminalNode Class() { return getToken(JavaScriptParser.Class, 0); }
		public ClassTailContext classTail() {
			return getRuleContext(ClassTailContext.class,0);
		}
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ClassExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterClassExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitClassExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitClassExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MemberIndexExpressionContext extends SingleExpressionImplContext {
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public TerminalNode OpenBracket() { return getToken(JavaScriptParser.OpenBracket, 0); }
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
		}
		public TerminalNode CloseBracket() { return getToken(JavaScriptParser.CloseBracket, 0); }
		public TerminalNode QuestionMarkDot() { return getToken(JavaScriptParser.QuestionMarkDot, 0); }
		public MemberIndexExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterMemberIndexExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitMemberIndexExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitMemberIndexExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IdentifierExpressionContext extends SingleExpressionImplContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public IdentifierExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterIdentifierExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitIdentifierExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitIdentifierExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BitAndExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode BitAnd() { return getToken(JavaScriptParser.BitAnd, 0); }
		public BitAndExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterBitAndExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitBitAndExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitBitAndExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BitOrExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode BitOr() { return getToken(JavaScriptParser.BitOr, 0); }
		public BitOrExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterBitOrExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitBitOrExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitBitOrExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AssignmentOperatorExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public AssignmentOperatorContext assignmentOperator() {
			return getRuleContext(AssignmentOperatorContext.class,0);
		}
		public AssignmentOperatorExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterAssignmentOperatorExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitAssignmentOperatorExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitAssignmentOperatorExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class VoidExpressionContext extends SingleExpressionImplContext {
		public TerminalNode Void() { return getToken(JavaScriptParser.Void, 0); }
		public SingleExpressionImplContext singleExpressionImpl() {
			return getRuleContext(SingleExpressionImplContext.class,0);
		}
		public VoidExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterVoidExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitVoidExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitVoidExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class CoalesceExpressionContext extends SingleExpressionImplContext {
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
		public TerminalNode NullCoalesce() { return getToken(JavaScriptParser.NullCoalesce, 0); }
		public CoalesceExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterCoalesceExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitCoalesceExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitCoalesceExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleExpressionImplContext singleExpressionImpl() throws RecognitionException {
		return singleExpressionImpl(0);
	}

	private SingleExpressionImplContext singleExpressionImpl(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		SingleExpressionImplContext _localctx = new SingleExpressionImplContext(_ctx, _parentState);
		SingleExpressionImplContext _prevctx = _localctx;
		int _startState = 132;
		enterRecursionRule(_localctx, 132, RULE_singleExpressionImpl, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(841);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,94,_ctx) ) {
			case 1:
				{
				_localctx = new FunctionExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(786);
				anonymousFunction();
				}
				break;
			case 2:
				{
				_localctx = new ClassExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(787);
				match(Class);
				setState(789);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 99)) & ~0x3f) == 0 && ((1L << (_la - 99)) & 8458255L) != 0)) {
					{
					setState(788);
					identifier();
					}
				}

				setState(791);
				classTail();
				}
				break;
			case 3:
				{
				_localctx = new NewExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(792);
				match(New);
				setState(793);
				identifier();
				setState(794);
				arguments();
				}
				break;
			case 4:
				{
				_localctx = new NewExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(796);
				match(New);
				setState(797);
				singleExpressionImpl(0);
				setState(798);
				arguments();
				}
				break;
			case 5:
				{
				_localctx = new NewExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(800);
				match(New);
				setState(801);
				singleExpressionImpl(42);
				}
				break;
			case 6:
				{
				_localctx = new MetaExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(802);
				match(New);
				setState(803);
				match(Dot);
				setState(804);
				identifier();
				}
				break;
			case 7:
				{
				_localctx = new DeleteExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(805);
				match(Delete);
				setState(806);
				singleExpressionImpl(37);
				}
				break;
			case 8:
				{
				_localctx = new VoidExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(807);
				match(Void);
				setState(808);
				singleExpressionImpl(36);
				}
				break;
			case 9:
				{
				_localctx = new TypeofExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(809);
				match(Typeof);
				setState(810);
				singleExpressionImpl(35);
				}
				break;
			case 10:
				{
				_localctx = new PreIncrementExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(811);
				match(PlusPlus);
				setState(812);
				singleExpressionImpl(34);
				}
				break;
			case 11:
				{
				_localctx = new PreDecreaseExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(813);
				match(MinusMinus);
				setState(814);
				singleExpressionImpl(33);
				}
				break;
			case 12:
				{
				_localctx = new UnaryPlusExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(815);
				match(Plus);
				setState(816);
				singleExpressionImpl(32);
				}
				break;
			case 13:
				{
				_localctx = new UnaryMinusExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(817);
				match(Minus);
				setState(818);
				singleExpressionImpl(31);
				}
				break;
			case 14:
				{
				_localctx = new BitNotExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(819);
				match(BitNot);
				setState(820);
				singleExpressionImpl(30);
				}
				break;
			case 15:
				{
				_localctx = new NotExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(821);
				match(Not);
				setState(822);
				singleExpressionImpl(29);
				}
				break;
			case 16:
				{
				_localctx = new AwaitExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(823);
				match(Await);
				setState(824);
				singleExpressionImpl(28);
				}
				break;
			case 17:
				{
				_localctx = new ImportExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(825);
				match(Import);
				setState(826);
				match(OpenParen);
				setState(827);
				singleExpressionImpl(0);
				setState(828);
				match(CloseParen);
				}
				break;
			case 18:
				{
				_localctx = new YieldExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(830);
				yieldStatement();
				}
				break;
			case 19:
				{
				_localctx = new ThisExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(831);
				match(This);
				}
				break;
			case 20:
				{
				_localctx = new IdentifierExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(832);
				identifier();
				}
				break;
			case 21:
				{
				_localctx = new SuperExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(833);
				match(Super);
				}
				break;
			case 22:
				{
				_localctx = new LiteralExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(834);
				literal();
				}
				break;
			case 23:
				{
				_localctx = new ArrayLiteralExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(835);
				arrayLiteral();
				}
				break;
			case 24:
				{
				_localctx = new ObjectLiteralExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(836);
				objectLiteral();
				}
				break;
			case 25:
				{
				_localctx = new ParenthesizedExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(837);
				match(OpenParen);
				setState(838);
				expressionSequence();
				setState(839);
				match(CloseParen);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(930);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,99,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(928);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,98,_ctx) ) {
					case 1:
						{
						_localctx = new OptionalChainExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(843);
						if (!(precpred(_ctx, 47))) throw new FailedPredicateException(this, "precpred(_ctx, 47)");
						setState(844);
						match(QuestionMarkDot);
						setState(845);
						singleExpressionImpl(48);
						}
						break;
					case 2:
						{
						_localctx = new PowerExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(846);
						if (!(precpred(_ctx, 27))) throw new FailedPredicateException(this, "precpred(_ctx, 27)");
						setState(847);
						match(Power);
						setState(848);
						singleExpressionImpl(27);
						}
						break;
					case 3:
						{
						_localctx = new MultiplicativeExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(849);
						if (!(precpred(_ctx, 26))) throw new FailedPredicateException(this, "precpred(_ctx, 26)");
						setState(850);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 469762048L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(851);
						singleExpressionImpl(27);
						}
						break;
					case 4:
						{
						_localctx = new AdditiveExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(852);
						if (!(precpred(_ctx, 25))) throw new FailedPredicateException(this, "precpred(_ctx, 25)");
						setState(853);
						_la = _input.LA(1);
						if ( !(_la==Plus || _la==Minus) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(854);
						singleExpressionImpl(26);
						}
						break;
					case 5:
						{
						_localctx = new CoalesceExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(855);
						if (!(precpred(_ctx, 24))) throw new FailedPredicateException(this, "precpred(_ctx, 24)");
						setState(856);
						match(NullCoalesce);
						setState(857);
						singleExpressionImpl(25);
						}
						break;
					case 6:
						{
						_localctx = new BitShiftExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(858);
						if (!(precpred(_ctx, 23))) throw new FailedPredicateException(this, "precpred(_ctx, 23)");
						setState(859);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 30064771072L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(860);
						singleExpressionImpl(24);
						}
						break;
					case 7:
						{
						_localctx = new RelationalExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(861);
						if (!(precpred(_ctx, 22))) throw new FailedPredicateException(this, "precpred(_ctx, 22)");
						setState(862);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 515396075520L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(863);
						singleExpressionImpl(23);
						}
						break;
					case 8:
						{
						_localctx = new InstanceofExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(864);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(865);
						match(Instanceof);
						setState(866);
						singleExpressionImpl(22);
						}
						break;
					case 9:
						{
						_localctx = new InExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(867);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(868);
						match(In);
						setState(869);
						singleExpressionImpl(21);
						}
						break;
					case 10:
						{
						_localctx = new EqualityExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(870);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(871);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 8246337208320L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(872);
						singleExpressionImpl(20);
						}
						break;
					case 11:
						{
						_localctx = new BitAndExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(873);
						if (!(precpred(_ctx, 18))) throw new FailedPredicateException(this, "precpred(_ctx, 18)");
						setState(874);
						match(BitAnd);
						setState(875);
						singleExpressionImpl(19);
						}
						break;
					case 12:
						{
						_localctx = new BitXOrExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(876);
						if (!(precpred(_ctx, 17))) throw new FailedPredicateException(this, "precpred(_ctx, 17)");
						setState(877);
						match(BitXOr);
						setState(878);
						singleExpressionImpl(18);
						}
						break;
					case 13:
						{
						_localctx = new BitOrExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(879);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(880);
						match(BitOr);
						setState(881);
						singleExpressionImpl(17);
						}
						break;
					case 14:
						{
						_localctx = new LogicalAndExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(882);
						if (!(precpred(_ctx, 15))) throw new FailedPredicateException(this, "precpred(_ctx, 15)");
						setState(883);
						match(And);
						setState(884);
						singleExpressionImpl(16);
						}
						break;
					case 15:
						{
						_localctx = new LogicalOrExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(885);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(886);
						match(Or);
						setState(887);
						singleExpressionImpl(15);
						}
						break;
					case 16:
						{
						_localctx = new TernaryExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(888);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(889);
						match(QuestionMark);
						setState(890);
						singleExpressionImpl(0);
						setState(891);
						match(Colon);
						setState(892);
						singleExpressionImpl(14);
						}
						break;
					case 17:
						{
						_localctx = new AssignmentExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(894);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(895);
						match(Assign);
						setState(896);
						singleExpressionImpl(12);
						}
						break;
					case 18:
						{
						_localctx = new AssignmentOperatorExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(897);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(898);
						assignmentOperator();
						setState(899);
						singleExpressionImpl(11);
						}
						break;
					case 19:
						{
						_localctx = new MemberIndexExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(901);
						if (!(precpred(_ctx, 46))) throw new FailedPredicateException(this, "precpred(_ctx, 46)");
						setState(903);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==QuestionMarkDot) {
							{
							setState(902);
							match(QuestionMarkDot);
							}
						}

						setState(905);
						match(OpenBracket);
						setState(906);
						expressionSequence();
						setState(907);
						match(CloseBracket);
						}
						break;
					case 20:
						{
						_localctx = new MemberDotExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(909);
						if (!(precpred(_ctx, 45))) throw new FailedPredicateException(this, "precpred(_ctx, 45)");
						setState(911);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==QuestionMark) {
							{
							setState(910);
							match(QuestionMark);
							}
						}

						setState(913);
						match(Dot);
						setState(915);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==Hashtag) {
							{
							setState(914);
							match(Hashtag);
							}
						}

						setState(917);
						identifierName();
						}
						break;
					case 21:
						{
						_localctx = new ArgumentsExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(918);
						if (!(precpred(_ctx, 41))) throw new FailedPredicateException(this, "precpred(_ctx, 41)");
						setState(919);
						arguments();
						}
						break;
					case 22:
						{
						_localctx = new PostIncrementExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(920);
						if (!(precpred(_ctx, 39))) throw new FailedPredicateException(this, "precpred(_ctx, 39)");
						setState(921);
						if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
						setState(922);
						match(PlusPlus);
						}
						break;
					case 23:
						{
						_localctx = new PostDecreaseExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(923);
						if (!(precpred(_ctx, 38))) throw new FailedPredicateException(this, "precpred(_ctx, 38)");
						setState(924);
						if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
						setState(925);
						match(MinusMinus);
						}
						break;
					case 24:
						{
						_localctx = new TemplateStringExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(926);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(927);
						templateStringLiteral();
						}
						break;
					}
					} 
				}
				setState(932);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,99,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InitializerContext extends ParserRuleContext {
		public TerminalNode Assign() { return getToken(JavaScriptParser.Assign, 0); }
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public InitializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterInitializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitInitializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitInitializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InitializerContext initializer() throws RecognitionException {
		InitializerContext _localctx = new InitializerContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_initializer);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(933);
			match(Assign);
			setState(934);
			singleExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AssignableContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public KeywordContext keyword() {
			return getRuleContext(KeywordContext.class,0);
		}
		public ArrayLiteralContext arrayLiteral() {
			return getRuleContext(ArrayLiteralContext.class,0);
		}
		public ObjectLiteralContext objectLiteral() {
			return getRuleContext(ObjectLiteralContext.class,0);
		}
		public AssignableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterAssignable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitAssignable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitAssignable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignableContext assignable() throws RecognitionException {
		AssignableContext _localctx = new AssignableContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_assignable);
		try {
			setState(940);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,100,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(936);
				identifier();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(937);
				keyword();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(938);
				arrayLiteral();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(939);
				objectLiteral();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ObjectLiteralContext extends ParserRuleContext {
		public TerminalNode OpenBrace() { return getToken(JavaScriptParser.OpenBrace, 0); }
		public TerminalNode CloseBrace() { return getToken(JavaScriptParser.CloseBrace, 0); }
		public List<PropertyAssignmentContext> propertyAssignment() {
			return getRuleContexts(PropertyAssignmentContext.class);
		}
		public PropertyAssignmentContext propertyAssignment(int i) {
			return getRuleContext(PropertyAssignmentContext.class,i);
		}
		public List<TerminalNode> Comma() { return getTokens(JavaScriptParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(JavaScriptParser.Comma, i);
		}
		public ObjectLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterObjectLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitObjectLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitObjectLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectLiteralContext objectLiteral() throws RecognitionException {
		ObjectLiteralContext _localctx = new ObjectLiteralContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_objectLiteral);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(942);
			match(OpenBrace);
			setState(954);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,103,_ctx) ) {
			case 1:
				{
				setState(943);
				propertyAssignment();
				setState(948);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,101,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(944);
						match(Comma);
						setState(945);
						propertyAssignment();
						}
						} 
					}
					setState(950);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,101,_ctx);
				}
				setState(952);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(951);
					match(Comma);
					}
				}

				}
				break;
			}
			setState(956);
			match(CloseBrace);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AnonymousFunctionContext extends ParserRuleContext {
		public AnonymousFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_anonymousFunction; }
	 
		public AnonymousFunctionContext() { }
		public void copyFrom(AnonymousFunctionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AnonymousFunctionDeclContext extends AnonymousFunctionContext {
		public TerminalNode Function_() { return getToken(JavaScriptParser.Function_, 0); }
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public FunctionBodyContext functionBody() {
			return getRuleContext(FunctionBodyContext.class,0);
		}
		public TerminalNode Async() { return getToken(JavaScriptParser.Async, 0); }
		public TerminalNode Multiply() { return getToken(JavaScriptParser.Multiply, 0); }
		public FormalParameterListContext formalParameterList() {
			return getRuleContext(FormalParameterListContext.class,0);
		}
		public AnonymousFunctionDeclContext(AnonymousFunctionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterAnonymousFunctionDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitAnonymousFunctionDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitAnonymousFunctionDecl(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ArrowFunctionContext extends AnonymousFunctionContext {
		public ArrowFunctionParametersContext arrowFunctionParameters() {
			return getRuleContext(ArrowFunctionParametersContext.class,0);
		}
		public TerminalNode ARROW() { return getToken(JavaScriptParser.ARROW, 0); }
		public ArrowFunctionBodyContext arrowFunctionBody() {
			return getRuleContext(ArrowFunctionBodyContext.class,0);
		}
		public TerminalNode Async() { return getToken(JavaScriptParser.Async, 0); }
		public ArrowFunctionContext(AnonymousFunctionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterArrowFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitArrowFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitArrowFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NamedFunctionContext extends AnonymousFunctionContext {
		public FunctionDeclarationContext functionDeclaration() {
			return getRuleContext(FunctionDeclarationContext.class,0);
		}
		public NamedFunctionContext(AnonymousFunctionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterNamedFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitNamedFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitNamedFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnonymousFunctionContext anonymousFunction() throws RecognitionException {
		AnonymousFunctionContext _localctx = new AnonymousFunctionContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_anonymousFunction);
		int _la;
		try {
			setState(979);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,108,_ctx) ) {
			case 1:
				_localctx = new NamedFunctionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(958);
				functionDeclaration();
				}
				break;
			case 2:
				_localctx = new AnonymousFunctionDeclContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(960);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Async) {
					{
					setState(959);
					match(Async);
					}
				}

				setState(962);
				match(Function_);
				setState(964);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Multiply) {
					{
					setState(963);
					match(Multiply);
					}
				}

				setState(966);
				match(OpenParen);
				setState(968);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 262688L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 1125899906842623L) != 0)) {
					{
					setState(967);
					formalParameterList();
					}
				}

				setState(970);
				match(CloseParen);
				setState(971);
				functionBody();
				}
				break;
			case 3:
				_localctx = new ArrowFunctionContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(973);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,107,_ctx) ) {
				case 1:
					{
					setState(972);
					match(Async);
					}
					break;
				}
				setState(975);
				arrowFunctionParameters();
				setState(976);
				match(ARROW);
				setState(977);
				arrowFunctionBody();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArrowFunctionParametersContext extends ParserRuleContext {
		public PropertyNameContext propertyName() {
			return getRuleContext(PropertyNameContext.class,0);
		}
		public TerminalNode OpenParen() { return getToken(JavaScriptParser.OpenParen, 0); }
		public TerminalNode CloseParen() { return getToken(JavaScriptParser.CloseParen, 0); }
		public FormalParameterListContext formalParameterList() {
			return getRuleContext(FormalParameterListContext.class,0);
		}
		public ArrowFunctionParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrowFunctionParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterArrowFunctionParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitArrowFunctionParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitArrowFunctionParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrowFunctionParametersContext arrowFunctionParameters() throws RecognitionException {
		ArrowFunctionParametersContext _localctx = new ArrowFunctionParametersContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_arrowFunctionParameters);
		int _la;
		try {
			setState(987);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OpenBracket:
			case NullLiteral:
			case BooleanLiteral:
			case DecimalLiteral:
			case HexIntegerLiteral:
			case OctalIntegerLiteral:
			case OctalIntegerLiteral2:
			case BinaryIntegerLiteral:
			case Break:
			case Do:
			case Instanceof:
			case Typeof:
			case Case:
			case Else:
			case New:
			case Var:
			case Catch:
			case Finally:
			case Return:
			case Void:
			case Continue:
			case For:
			case Switch:
			case While:
			case Debugger:
			case Function_:
			case This:
			case With:
			case Default:
			case If:
			case Throw:
			case Delete:
			case In:
			case Try:
			case As:
			case From:
			case Of:
			case Yield:
			case YieldStar:
			case Class:
			case Enum:
			case Extends:
			case Super:
			case Const:
			case Export:
			case Import:
			case Async:
			case Await:
			case Implements:
			case StrictLet:
			case NonStrictLet:
			case Private:
			case Public:
			case Interface:
			case Package:
			case Protected:
			case Static:
			case Identifier:
			case StringLiteral:
				enterOuterAlt(_localctx, 1);
				{
				setState(981);
				propertyName();
				}
				break;
			case OpenParen:
				enterOuterAlt(_localctx, 2);
				{
				setState(982);
				match(OpenParen);
				setState(984);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 262688L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 1125899906842623L) != 0)) {
					{
					setState(983);
					formalParameterList();
					}
				}

				setState(986);
				match(CloseParen);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArrowFunctionBodyContext extends ParserRuleContext {
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public FunctionBodyContext functionBody() {
			return getRuleContext(FunctionBodyContext.class,0);
		}
		public ArrowFunctionBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrowFunctionBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterArrowFunctionBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitArrowFunctionBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitArrowFunctionBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrowFunctionBodyContext arrowFunctionBody() throws RecognitionException {
		ArrowFunctionBodyContext _localctx = new ArrowFunctionBodyContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_arrowFunctionBody);
		try {
			setState(991);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,111,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(989);
				singleExpression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(990);
				functionBody();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AssignmentOperatorContext extends ParserRuleContext {
		public TerminalNode MultiplyAssign() { return getToken(JavaScriptParser.MultiplyAssign, 0); }
		public TerminalNode DivideAssign() { return getToken(JavaScriptParser.DivideAssign, 0); }
		public TerminalNode ModulusAssign() { return getToken(JavaScriptParser.ModulusAssign, 0); }
		public TerminalNode PlusAssign() { return getToken(JavaScriptParser.PlusAssign, 0); }
		public TerminalNode MinusAssign() { return getToken(JavaScriptParser.MinusAssign, 0); }
		public TerminalNode LeftShiftArithmeticAssign() { return getToken(JavaScriptParser.LeftShiftArithmeticAssign, 0); }
		public TerminalNode RightShiftArithmeticAssign() { return getToken(JavaScriptParser.RightShiftArithmeticAssign, 0); }
		public TerminalNode RightShiftLogicalAssign() { return getToken(JavaScriptParser.RightShiftLogicalAssign, 0); }
		public TerminalNode BitAndAssign() { return getToken(JavaScriptParser.BitAndAssign, 0); }
		public TerminalNode BitXorAssign() { return getToken(JavaScriptParser.BitXorAssign, 0); }
		public TerminalNode BitOrAssign() { return getToken(JavaScriptParser.BitOrAssign, 0); }
		public TerminalNode PowerAssign() { return getToken(JavaScriptParser.PowerAssign, 0); }
		public TerminalNode NullishCoalescingAssign() { return getToken(JavaScriptParser.NullishCoalescingAssign, 0); }
		public AssignmentOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignmentOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterAssignmentOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitAssignmentOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitAssignmentOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentOperatorContext assignmentOperator() throws RecognitionException {
		AssignmentOperatorContext _localctx = new AssignmentOperatorContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_assignmentOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(993);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2305561534236983296L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode NullLiteral() { return getToken(JavaScriptParser.NullLiteral, 0); }
		public TerminalNode BooleanLiteral() { return getToken(JavaScriptParser.BooleanLiteral, 0); }
		public TerminalNode StringLiteral() { return getToken(JavaScriptParser.StringLiteral, 0); }
		public TemplateStringLiteralContext templateStringLiteral() {
			return getRuleContext(TemplateStringLiteralContext.class,0);
		}
		public TerminalNode RegularExpressionLiteral() { return getToken(JavaScriptParser.RegularExpressionLiteral, 0); }
		public NumericLiteralContext numericLiteral() {
			return getRuleContext(NumericLiteralContext.class,0);
		}
		public BigintLiteralContext bigintLiteral() {
			return getRuleContext(BigintLiteralContext.class,0);
		}
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_literal);
		try {
			setState(1002);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NullLiteral:
				enterOuterAlt(_localctx, 1);
				{
				setState(995);
				match(NullLiteral);
				}
				break;
			case BooleanLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(996);
				match(BooleanLiteral);
				}
				break;
			case StringLiteral:
				enterOuterAlt(_localctx, 3);
				{
				setState(997);
				match(StringLiteral);
				}
				break;
			case BackTick:
				enterOuterAlt(_localctx, 4);
				{
				setState(998);
				templateStringLiteral();
				}
				break;
			case RegularExpressionLiteral:
				enterOuterAlt(_localctx, 5);
				{
				setState(999);
				match(RegularExpressionLiteral);
				}
				break;
			case DecimalLiteral:
			case HexIntegerLiteral:
			case OctalIntegerLiteral:
			case OctalIntegerLiteral2:
			case BinaryIntegerLiteral:
				enterOuterAlt(_localctx, 6);
				{
				setState(1000);
				numericLiteral();
				}
				break;
			case BigHexIntegerLiteral:
			case BigOctalIntegerLiteral:
			case BigBinaryIntegerLiteral:
			case BigDecimalIntegerLiteral:
				enterOuterAlt(_localctx, 7);
				{
				setState(1001);
				bigintLiteral();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TemplateStringLiteralContext extends ParserRuleContext {
		public List<TerminalNode> BackTick() { return getTokens(JavaScriptParser.BackTick); }
		public TerminalNode BackTick(int i) {
			return getToken(JavaScriptParser.BackTick, i);
		}
		public List<TemplateStringAtomContext> templateStringAtom() {
			return getRuleContexts(TemplateStringAtomContext.class);
		}
		public TemplateStringAtomContext templateStringAtom(int i) {
			return getRuleContext(TemplateStringAtomContext.class,i);
		}
		public TemplateStringLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_templateStringLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterTemplateStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitTemplateStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitTemplateStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TemplateStringLiteralContext templateStringLiteral() throws RecognitionException {
		TemplateStringLiteralContext _localctx = new TemplateStringLiteralContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_templateStringLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1004);
			match(BackTick);
			setState(1008);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==TemplateStringStartExpression || _la==TemplateStringAtom) {
				{
				{
				setState(1005);
				templateStringAtom();
				}
				}
				setState(1010);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1011);
			match(BackTick);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TemplateStringAtomContext extends ParserRuleContext {
		public TerminalNode TemplateStringAtom() { return getToken(JavaScriptParser.TemplateStringAtom, 0); }
		public TerminalNode TemplateStringStartExpression() { return getToken(JavaScriptParser.TemplateStringStartExpression, 0); }
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public TerminalNode TemplateCloseBrace() { return getToken(JavaScriptParser.TemplateCloseBrace, 0); }
		public TemplateStringAtomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_templateStringAtom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterTemplateStringAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitTemplateStringAtom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitTemplateStringAtom(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TemplateStringAtomContext templateStringAtom() throws RecognitionException {
		TemplateStringAtomContext _localctx = new TemplateStringAtomContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_templateStringAtom);
		try {
			setState(1018);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TemplateStringAtom:
				enterOuterAlt(_localctx, 1);
				{
				setState(1013);
				match(TemplateStringAtom);
				}
				break;
			case TemplateStringStartExpression:
				enterOuterAlt(_localctx, 2);
				{
				setState(1014);
				match(TemplateStringStartExpression);
				setState(1015);
				singleExpression();
				setState(1016);
				match(TemplateCloseBrace);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NumericLiteralContext extends ParserRuleContext {
		public TerminalNode DecimalLiteral() { return getToken(JavaScriptParser.DecimalLiteral, 0); }
		public TerminalNode HexIntegerLiteral() { return getToken(JavaScriptParser.HexIntegerLiteral, 0); }
		public TerminalNode OctalIntegerLiteral() { return getToken(JavaScriptParser.OctalIntegerLiteral, 0); }
		public TerminalNode OctalIntegerLiteral2() { return getToken(JavaScriptParser.OctalIntegerLiteral2, 0); }
		public TerminalNode BinaryIntegerLiteral() { return getToken(JavaScriptParser.BinaryIntegerLiteral, 0); }
		public NumericLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_numericLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterNumericLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitNumericLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitNumericLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumericLiteralContext numericLiteral() throws RecognitionException {
		NumericLiteralContext _localctx = new NumericLiteralContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_numericLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1020);
			_la = _input.LA(1);
			if ( !(((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 31L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BigintLiteralContext extends ParserRuleContext {
		public TerminalNode BigDecimalIntegerLiteral() { return getToken(JavaScriptParser.BigDecimalIntegerLiteral, 0); }
		public TerminalNode BigHexIntegerLiteral() { return getToken(JavaScriptParser.BigHexIntegerLiteral, 0); }
		public TerminalNode BigOctalIntegerLiteral() { return getToken(JavaScriptParser.BigOctalIntegerLiteral, 0); }
		public TerminalNode BigBinaryIntegerLiteral() { return getToken(JavaScriptParser.BigBinaryIntegerLiteral, 0); }
		public BigintLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bigintLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterBigintLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitBigintLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitBigintLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BigintLiteralContext bigintLiteral() throws RecognitionException {
		BigintLiteralContext _localctx = new BigintLiteralContext(_ctx, getState());
		enterRule(_localctx, 156, RULE_bigintLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1022);
			_la = _input.LA(1);
			if ( !(((((_la - 69)) & ~0x3f) == 0 && ((1L << (_la - 69)) & 15L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GetterContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ClassElementNameContext classElementName() {
			return getRuleContext(ClassElementNameContext.class,0);
		}
		public GetterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_getter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterGetter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitGetter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitGetter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GetterContext getter() throws RecognitionException {
		GetterContext _localctx = new GetterContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_getter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1024);
			if (!(this.n("get"))) throw new FailedPredicateException(this, "this.n(\"get\")");
			setState(1025);
			identifier();
			setState(1026);
			classElementName();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SetterContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ClassElementNameContext classElementName() {
			return getRuleContext(ClassElementNameContext.class,0);
		}
		public SetterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterSetter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitSetter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitSetter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetterContext setter() throws RecognitionException {
		SetterContext _localctx = new SetterContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_setter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1028);
			if (!(this.n("set"))) throw new FailedPredicateException(this, "this.n(\"set\")");
			setState(1029);
			identifier();
			setState(1030);
			classElementName();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IdentifierNameContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ReservedWordContext reservedWord() {
			return getRuleContext(ReservedWordContext.class,0);
		}
		public IdentifierNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifierName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterIdentifierName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitIdentifierName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitIdentifierName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierNameContext identifierName() throws RecognitionException {
		IdentifierNameContext _localctx = new IdentifierNameContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_identifierName);
		try {
			setState(1034);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,115,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1032);
				identifier();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1033);
				reservedWord();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IdentifierContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(JavaScriptParser.Identifier, 0); }
		public TerminalNode NonStrictLet() { return getToken(JavaScriptParser.NonStrictLet, 0); }
		public TerminalNode Async() { return getToken(JavaScriptParser.Async, 0); }
		public TerminalNode As() { return getToken(JavaScriptParser.As, 0); }
		public TerminalNode From() { return getToken(JavaScriptParser.From, 0); }
		public TerminalNode Yield() { return getToken(JavaScriptParser.Yield, 0); }
		public TerminalNode Of() { return getToken(JavaScriptParser.Of, 0); }
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_identifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1036);
			_la = _input.LA(1);
			if ( !(((((_la - 99)) & ~0x3f) == 0 && ((1L << (_la - 99)) & 8458255L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ReservedWordContext extends ParserRuleContext {
		public KeywordContext keyword() {
			return getRuleContext(KeywordContext.class,0);
		}
		public TerminalNode NullLiteral() { return getToken(JavaScriptParser.NullLiteral, 0); }
		public TerminalNode BooleanLiteral() { return getToken(JavaScriptParser.BooleanLiteral, 0); }
		public ReservedWordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_reservedWord; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterReservedWord(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitReservedWord(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitReservedWord(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReservedWordContext reservedWord() throws RecognitionException {
		ReservedWordContext _localctx = new ReservedWordContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_reservedWord);
		try {
			setState(1041);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Break:
			case Do:
			case Instanceof:
			case Typeof:
			case Case:
			case Else:
			case New:
			case Var:
			case Catch:
			case Finally:
			case Return:
			case Void:
			case Continue:
			case For:
			case Switch:
			case While:
			case Debugger:
			case Function_:
			case This:
			case With:
			case Default:
			case If:
			case Throw:
			case Delete:
			case In:
			case Try:
			case As:
			case From:
			case Of:
			case Yield:
			case YieldStar:
			case Class:
			case Enum:
			case Extends:
			case Super:
			case Const:
			case Export:
			case Import:
			case Async:
			case Await:
			case Implements:
			case StrictLet:
			case NonStrictLet:
			case Private:
			case Public:
			case Interface:
			case Package:
			case Protected:
			case Static:
				enterOuterAlt(_localctx, 1);
				{
				setState(1038);
				keyword();
				}
				break;
			case NullLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(1039);
				match(NullLiteral);
				}
				break;
			case BooleanLiteral:
				enterOuterAlt(_localctx, 3);
				{
				setState(1040);
				match(BooleanLiteral);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class KeywordContext extends ParserRuleContext {
		public TerminalNode Break() { return getToken(JavaScriptParser.Break, 0); }
		public TerminalNode Do() { return getToken(JavaScriptParser.Do, 0); }
		public TerminalNode Instanceof() { return getToken(JavaScriptParser.Instanceof, 0); }
		public TerminalNode Typeof() { return getToken(JavaScriptParser.Typeof, 0); }
		public TerminalNode Case() { return getToken(JavaScriptParser.Case, 0); }
		public TerminalNode Else() { return getToken(JavaScriptParser.Else, 0); }
		public TerminalNode New() { return getToken(JavaScriptParser.New, 0); }
		public TerminalNode Var() { return getToken(JavaScriptParser.Var, 0); }
		public TerminalNode Catch() { return getToken(JavaScriptParser.Catch, 0); }
		public TerminalNode Finally() { return getToken(JavaScriptParser.Finally, 0); }
		public TerminalNode Return() { return getToken(JavaScriptParser.Return, 0); }
		public TerminalNode Void() { return getToken(JavaScriptParser.Void, 0); }
		public TerminalNode Continue() { return getToken(JavaScriptParser.Continue, 0); }
		public TerminalNode For() { return getToken(JavaScriptParser.For, 0); }
		public TerminalNode Switch() { return getToken(JavaScriptParser.Switch, 0); }
		public TerminalNode While() { return getToken(JavaScriptParser.While, 0); }
		public TerminalNode Debugger() { return getToken(JavaScriptParser.Debugger, 0); }
		public TerminalNode Function_() { return getToken(JavaScriptParser.Function_, 0); }
		public TerminalNode This() { return getToken(JavaScriptParser.This, 0); }
		public TerminalNode With() { return getToken(JavaScriptParser.With, 0); }
		public TerminalNode Default() { return getToken(JavaScriptParser.Default, 0); }
		public TerminalNode If() { return getToken(JavaScriptParser.If, 0); }
		public TerminalNode Throw() { return getToken(JavaScriptParser.Throw, 0); }
		public TerminalNode Delete() { return getToken(JavaScriptParser.Delete, 0); }
		public TerminalNode In() { return getToken(JavaScriptParser.In, 0); }
		public TerminalNode Try() { return getToken(JavaScriptParser.Try, 0); }
		public TerminalNode Class() { return getToken(JavaScriptParser.Class, 0); }
		public TerminalNode Enum() { return getToken(JavaScriptParser.Enum, 0); }
		public TerminalNode Extends() { return getToken(JavaScriptParser.Extends, 0); }
		public TerminalNode Super() { return getToken(JavaScriptParser.Super, 0); }
		public TerminalNode Const() { return getToken(JavaScriptParser.Const, 0); }
		public TerminalNode Export() { return getToken(JavaScriptParser.Export, 0); }
		public TerminalNode Import() { return getToken(JavaScriptParser.Import, 0); }
		public TerminalNode Implements() { return getToken(JavaScriptParser.Implements, 0); }
		public Let_Context let_() {
			return getRuleContext(Let_Context.class,0);
		}
		public TerminalNode Private() { return getToken(JavaScriptParser.Private, 0); }
		public TerminalNode Public() { return getToken(JavaScriptParser.Public, 0); }
		public TerminalNode Interface() { return getToken(JavaScriptParser.Interface, 0); }
		public TerminalNode Package() { return getToken(JavaScriptParser.Package, 0); }
		public TerminalNode Protected() { return getToken(JavaScriptParser.Protected, 0); }
		public TerminalNode Static() { return getToken(JavaScriptParser.Static, 0); }
		public TerminalNode Yield() { return getToken(JavaScriptParser.Yield, 0); }
		public TerminalNode YieldStar() { return getToken(JavaScriptParser.YieldStar, 0); }
		public TerminalNode Async() { return getToken(JavaScriptParser.Async, 0); }
		public TerminalNode Await() { return getToken(JavaScriptParser.Await, 0); }
		public TerminalNode From() { return getToken(JavaScriptParser.From, 0); }
		public TerminalNode As() { return getToken(JavaScriptParser.As, 0); }
		public TerminalNode Of() { return getToken(JavaScriptParser.Of, 0); }
		public KeywordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keyword; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterKeyword(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitKeyword(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitKeyword(this);
			else return visitor.visitChildren(this);
		}
	}

	public final KeywordContext keyword() throws RecognitionException {
		KeywordContext _localctx = new KeywordContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_keyword);
		try {
			setState(1091);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Break:
				enterOuterAlt(_localctx, 1);
				{
				setState(1043);
				match(Break);
				}
				break;
			case Do:
				enterOuterAlt(_localctx, 2);
				{
				setState(1044);
				match(Do);
				}
				break;
			case Instanceof:
				enterOuterAlt(_localctx, 3);
				{
				setState(1045);
				match(Instanceof);
				}
				break;
			case Typeof:
				enterOuterAlt(_localctx, 4);
				{
				setState(1046);
				match(Typeof);
				}
				break;
			case Case:
				enterOuterAlt(_localctx, 5);
				{
				setState(1047);
				match(Case);
				}
				break;
			case Else:
				enterOuterAlt(_localctx, 6);
				{
				setState(1048);
				match(Else);
				}
				break;
			case New:
				enterOuterAlt(_localctx, 7);
				{
				setState(1049);
				match(New);
				}
				break;
			case Var:
				enterOuterAlt(_localctx, 8);
				{
				setState(1050);
				match(Var);
				}
				break;
			case Catch:
				enterOuterAlt(_localctx, 9);
				{
				setState(1051);
				match(Catch);
				}
				break;
			case Finally:
				enterOuterAlt(_localctx, 10);
				{
				setState(1052);
				match(Finally);
				}
				break;
			case Return:
				enterOuterAlt(_localctx, 11);
				{
				setState(1053);
				match(Return);
				}
				break;
			case Void:
				enterOuterAlt(_localctx, 12);
				{
				setState(1054);
				match(Void);
				}
				break;
			case Continue:
				enterOuterAlt(_localctx, 13);
				{
				setState(1055);
				match(Continue);
				}
				break;
			case For:
				enterOuterAlt(_localctx, 14);
				{
				setState(1056);
				match(For);
				}
				break;
			case Switch:
				enterOuterAlt(_localctx, 15);
				{
				setState(1057);
				match(Switch);
				}
				break;
			case While:
				enterOuterAlt(_localctx, 16);
				{
				setState(1058);
				match(While);
				}
				break;
			case Debugger:
				enterOuterAlt(_localctx, 17);
				{
				setState(1059);
				match(Debugger);
				}
				break;
			case Function_:
				enterOuterAlt(_localctx, 18);
				{
				setState(1060);
				match(Function_);
				}
				break;
			case This:
				enterOuterAlt(_localctx, 19);
				{
				setState(1061);
				match(This);
				}
				break;
			case With:
				enterOuterAlt(_localctx, 20);
				{
				setState(1062);
				match(With);
				}
				break;
			case Default:
				enterOuterAlt(_localctx, 21);
				{
				setState(1063);
				match(Default);
				}
				break;
			case If:
				enterOuterAlt(_localctx, 22);
				{
				setState(1064);
				match(If);
				}
				break;
			case Throw:
				enterOuterAlt(_localctx, 23);
				{
				setState(1065);
				match(Throw);
				}
				break;
			case Delete:
				enterOuterAlt(_localctx, 24);
				{
				setState(1066);
				match(Delete);
				}
				break;
			case In:
				enterOuterAlt(_localctx, 25);
				{
				setState(1067);
				match(In);
				}
				break;
			case Try:
				enterOuterAlt(_localctx, 26);
				{
				setState(1068);
				match(Try);
				}
				break;
			case Class:
				enterOuterAlt(_localctx, 27);
				{
				setState(1069);
				match(Class);
				}
				break;
			case Enum:
				enterOuterAlt(_localctx, 28);
				{
				setState(1070);
				match(Enum);
				}
				break;
			case Extends:
				enterOuterAlt(_localctx, 29);
				{
				setState(1071);
				match(Extends);
				}
				break;
			case Super:
				enterOuterAlt(_localctx, 30);
				{
				setState(1072);
				match(Super);
				}
				break;
			case Const:
				enterOuterAlt(_localctx, 31);
				{
				setState(1073);
				match(Const);
				}
				break;
			case Export:
				enterOuterAlt(_localctx, 32);
				{
				setState(1074);
				match(Export);
				}
				break;
			case Import:
				enterOuterAlt(_localctx, 33);
				{
				setState(1075);
				match(Import);
				}
				break;
			case Implements:
				enterOuterAlt(_localctx, 34);
				{
				setState(1076);
				match(Implements);
				}
				break;
			case StrictLet:
			case NonStrictLet:
				enterOuterAlt(_localctx, 35);
				{
				setState(1077);
				let_();
				}
				break;
			case Private:
				enterOuterAlt(_localctx, 36);
				{
				setState(1078);
				match(Private);
				}
				break;
			case Public:
				enterOuterAlt(_localctx, 37);
				{
				setState(1079);
				match(Public);
				}
				break;
			case Interface:
				enterOuterAlt(_localctx, 38);
				{
				setState(1080);
				match(Interface);
				}
				break;
			case Package:
				enterOuterAlt(_localctx, 39);
				{
				setState(1081);
				match(Package);
				}
				break;
			case Protected:
				enterOuterAlt(_localctx, 40);
				{
				setState(1082);
				match(Protected);
				}
				break;
			case Static:
				enterOuterAlt(_localctx, 41);
				{
				setState(1083);
				match(Static);
				}
				break;
			case Yield:
				enterOuterAlt(_localctx, 42);
				{
				setState(1084);
				match(Yield);
				}
				break;
			case YieldStar:
				enterOuterAlt(_localctx, 43);
				{
				setState(1085);
				match(YieldStar);
				}
				break;
			case Async:
				enterOuterAlt(_localctx, 44);
				{
				setState(1086);
				match(Async);
				}
				break;
			case Await:
				enterOuterAlt(_localctx, 45);
				{
				setState(1087);
				match(Await);
				}
				break;
			case From:
				enterOuterAlt(_localctx, 46);
				{
				setState(1088);
				match(From);
				}
				break;
			case As:
				enterOuterAlt(_localctx, 47);
				{
				setState(1089);
				match(As);
				}
				break;
			case Of:
				enterOuterAlt(_localctx, 48);
				{
				setState(1090);
				match(Of);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Let_Context extends ParserRuleContext {
		public TerminalNode NonStrictLet() { return getToken(JavaScriptParser.NonStrictLet, 0); }
		public TerminalNode StrictLet() { return getToken(JavaScriptParser.StrictLet, 0); }
		public Let_Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_let_; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterLet_(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitLet_(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitLet_(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Let_Context let_() throws RecognitionException {
		Let_Context _localctx = new Let_Context(_ctx, getState());
		enterRule(_localctx, 170, RULE_let_);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1093);
			_la = _input.LA(1);
			if ( !(_la==StrictLet || _la==NonStrictLet) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EosContext extends ParserRuleContext {
		public TerminalNode SemiColon() { return getToken(JavaScriptParser.SemiColon, 0); }
		public TerminalNode EOF() { return getToken(JavaScriptParser.EOF, 0); }
		public EosContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_eos; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterEos(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitEos(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitEos(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EosContext eos() throws RecognitionException {
		EosContext _localctx = new EosContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_eos);
		try {
			setState(1099);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,118,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1095);
				match(SemiColon);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1096);
				match(EOF);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1097);
				if (!(this.lineTerminatorAhead())) throw new FailedPredicateException(this, "this.lineTerminatorAhead()");
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1098);
				if (!(this.closeBrace())) throw new FailedPredicateException(this, "this.closeBrace()");
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 24:
			return expressionStatement_sempred((ExpressionStatementContext)_localctx, predIndex);
		case 28:
			return continueStatement_sempred((ContinueStatementContext)_localctx, predIndex);
		case 29:
			return breakStatement_sempred((BreakStatementContext)_localctx, predIndex);
		case 30:
			return returnStatement_sempred((ReturnStatementContext)_localctx, predIndex);
		case 31:
			return yieldStatement_sempred((YieldStatementContext)_localctx, predIndex);
		case 39:
			return throwStatement_sempred((ThrowStatementContext)_localctx, predIndex);
		case 47:
			return classElement_sempred((ClassElementContext)_localctx, predIndex);
		case 48:
			return methodDefinition_sempred((MethodDefinitionContext)_localctx, predIndex);
		case 66:
			return singleExpressionImpl_sempred((SingleExpressionImplContext)_localctx, predIndex);
		case 79:
			return getter_sempred((GetterContext)_localctx, predIndex);
		case 80:
			return setter_sempred((SetterContext)_localctx, predIndex);
		case 86:
			return eos_sempred((EosContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expressionStatement_sempred(ExpressionStatementContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return this.notOpenBraceAndNotFunction();
		}
		return true;
	}
	private boolean continueStatement_sempred(ContinueStatementContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return this.notLineTerminator();
		}
		return true;
	}
	private boolean breakStatement_sempred(BreakStatementContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return this.notLineTerminator();
		}
		return true;
	}
	private boolean returnStatement_sempred(ReturnStatementContext _localctx, int predIndex) {
		switch (predIndex) {
		case 3:
			return this.notLineTerminator();
		}
		return true;
	}
	private boolean yieldStatement_sempred(YieldStatementContext _localctx, int predIndex) {
		switch (predIndex) {
		case 4:
			return this.notLineTerminator();
		}
		return true;
	}
	private boolean throwStatement_sempred(ThrowStatementContext _localctx, int predIndex) {
		switch (predIndex) {
		case 5:
			return this.notLineTerminator();
		}
		return true;
	}
	private boolean classElement_sempred(ClassElementContext _localctx, int predIndex) {
		switch (predIndex) {
		case 6:
			return this.n("static");
		case 7:
			return this.n("static");
		case 8:
			return this.n("static");
		}
		return true;
	}
	private boolean methodDefinition_sempred(MethodDefinitionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 9:
			return this.notLineTerminator();
		}
		return true;
	}
	private boolean singleExpressionImpl_sempred(SingleExpressionImplContext _localctx, int predIndex) {
		switch (predIndex) {
		case 10:
			return precpred(_ctx, 47);
		case 11:
			return precpred(_ctx, 27);
		case 12:
			return precpred(_ctx, 26);
		case 13:
			return precpred(_ctx, 25);
		case 14:
			return precpred(_ctx, 24);
		case 15:
			return precpred(_ctx, 23);
		case 16:
			return precpred(_ctx, 22);
		case 17:
			return precpred(_ctx, 21);
		case 18:
			return precpred(_ctx, 20);
		case 19:
			return precpred(_ctx, 19);
		case 20:
			return precpred(_ctx, 18);
		case 21:
			return precpred(_ctx, 17);
		case 22:
			return precpred(_ctx, 16);
		case 23:
			return precpred(_ctx, 15);
		case 24:
			return precpred(_ctx, 14);
		case 25:
			return precpred(_ctx, 13);
		case 26:
			return precpred(_ctx, 12);
		case 27:
			return precpred(_ctx, 11);
		case 28:
			return precpred(_ctx, 46);
		case 29:
			return precpred(_ctx, 45);
		case 30:
			return precpred(_ctx, 41);
		case 31:
			return precpred(_ctx, 39);
		case 32:
			return this.notLineTerminator();
		case 33:
			return precpred(_ctx, 38);
		case 34:
			return this.notLineTerminator();
		case 35:
			return precpred(_ctx, 9);
		}
		return true;
	}
	private boolean getter_sempred(GetterContext _localctx, int predIndex) {
		switch (predIndex) {
		case 36:
			return this.n("get");
		}
		return true;
	}
	private boolean setter_sempred(SetterContext _localctx, int predIndex) {
		switch (predIndex) {
		case 37:
			return this.n("set");
		}
		return true;
	}
	private boolean eos_sempred(EosContext _localctx, int predIndex) {
		switch (predIndex) {
		case 38:
			return this.lineTerminatorAhead();
		case 39:
			return this.closeBrace();
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001\u0083\u044e\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007"+
		"\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007"+
		"\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007"+
		"\u001b\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007"+
		"\u001e\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007"+
		"\"\u0002#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007"+
		"\'\u0002(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007"+
		",\u0002-\u0007-\u0002.\u0007.\u0002/\u0007/\u00020\u00070\u00021\u0007"+
		"1\u00022\u00072\u00023\u00073\u00024\u00074\u00025\u00075\u00026\u0007"+
		"6\u00027\u00077\u00028\u00078\u00029\u00079\u0002:\u0007:\u0002;\u0007"+
		";\u0002<\u0007<\u0002=\u0007=\u0002>\u0007>\u0002?\u0007?\u0002@\u0007"+
		"@\u0002A\u0007A\u0002B\u0007B\u0002C\u0007C\u0002D\u0007D\u0002E\u0007"+
		"E\u0002F\u0007F\u0002G\u0007G\u0002H\u0007H\u0002I\u0007I\u0002J\u0007"+
		"J\u0002K\u0007K\u0002L\u0007L\u0002M\u0007M\u0002N\u0007N\u0002O\u0007"+
		"O\u0002P\u0007P\u0002Q\u0007Q\u0002R\u0007R\u0002S\u0007S\u0002T\u0007"+
		"T\u0002U\u0007U\u0002V\u0007V\u0001\u0000\u0003\u0000\u00b0\b\u0000\u0001"+
		"\u0000\u0003\u0000\u00b3\b\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0003\u0002\u00cd\b\u0002\u0001\u0003\u0001"+
		"\u0003\u0003\u0003\u00d1\b\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0004"+
		"\u0004\u00d6\b\u0004\u000b\u0004\f\u0004\u00d7\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0006\u0003\u0006\u00de\b\u0006\u0001\u0006\u0001\u0006"+
		"\u0003\u0006\u00e2\b\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0003\u0006\u00e9\b\u0006\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0005\u0007\u00ef\b\u0007\n\u0007\f\u0007\u00f2\t\u0007\u0001"+
		"\u0007\u0001\u0007\u0003\u0007\u00f6\b\u0007\u0003\u0007\u00f8\b\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\b\u0003\b\u00ff\b\b\u0001"+
		"\t\u0001\t\u0003\t\u0103\b\t\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\f\u0001\f\u0003\f\u010c\b\f\u0001\f\u0001\f\u0003\f\u0110"+
		"\b\f\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0003"+
		"\u000e\u0118\b\u000e\u0001\u000f\u0001\u000f\u0003\u000f\u011c\b\u000f"+
		"\u0001\u000f\u0001\u000f\u0003\u000f\u0120\b\u000f\u0001\u000f\u0001\u000f"+
		"\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0003\u000f"+
		"\u0129\b\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0003\u0010\u0131\b\u0010\u0001\u0010\u0001\u0010\u0003\u0010"+
		"\u0135\b\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0005\u0011"+
		"\u013b\b\u0011\n\u0011\f\u0011\u013e\t\u0011\u0001\u0011\u0001\u0011\u0003"+
		"\u0011\u0142\b\u0011\u0003\u0011\u0144\b\u0011\u0001\u0011\u0001\u0011"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0003\u0012\u014b\b\u0012\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0003\u0013\u0150\b\u0013\u0001\u0014\u0001\u0014"+
		"\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0005\u0015"+
		"\u0159\b\u0015\n\u0015\f\u0015\u015c\t\u0015\u0001\u0016\u0001\u0016\u0001"+
		"\u0016\u0003\u0016\u0161\b\u0016\u0001\u0017\u0001\u0017\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0019\u0001\u0019\u0001\u0019\u0001"+
		"\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0003\u0019\u0170\b\u0019\u0001"+
		"\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001"+
		"\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001"+
		"\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0003"+
		"\u001a\u0184\b\u001a\u0001\u001a\u0001\u001a\u0003\u001a\u0188\b\u001a"+
		"\u0001\u001a\u0001\u001a\u0003\u001a\u018c\b\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0003\u001a\u0194\b\u001a"+
		"\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0003\u001a\u019d\b\u001a\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0003\u001a\u01a2\b\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0003\u001a\u01a9\b\u001a\u0001\u001b\u0001\u001b\u0001\u001b"+
		"\u0003\u001b\u01ae\b\u001b\u0001\u001c\u0001\u001c\u0001\u001c\u0003\u001c"+
		"\u01b3\b\u001c\u0001\u001c\u0001\u001c\u0001\u001d\u0001\u001d\u0001\u001d"+
		"\u0003\u001d\u01ba\b\u001d\u0001\u001d\u0001\u001d\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0003\u001e\u01c1\b\u001e\u0001\u001e\u0001\u001e\u0001\u001f"+
		"\u0001\u001f\u0001\u001f\u0003\u001f\u01c8\b\u001f\u0001\u001f\u0001\u001f"+
		"\u0001 \u0001 \u0001 \u0001 \u0001 \u0001 \u0001!\u0001!\u0001!\u0001"+
		"!\u0001!\u0001!\u0001\"\u0001\"\u0003\"\u01da\b\"\u0001\"\u0001\"\u0003"+
		"\"\u01de\b\"\u0003\"\u01e0\b\"\u0001\"\u0001\"\u0001#\u0004#\u01e5\b#"+
		"\u000b#\f#\u01e6\u0001$\u0001$\u0001$\u0001$\u0003$\u01ed\b$\u0001%\u0001"+
		"%\u0001%\u0003%\u01f2\b%\u0001&\u0001&\u0001&\u0001&\u0001\'\u0001\'\u0001"+
		"\'\u0001\'\u0001\'\u0001(\u0001(\u0001(\u0001(\u0003(\u0201\b(\u0001("+
		"\u0003(\u0204\b(\u0001)\u0001)\u0001)\u0003)\u0209\b)\u0001)\u0003)\u020c"+
		"\b)\u0001)\u0001)\u0001*\u0001*\u0001*\u0001+\u0001+\u0001+\u0001,\u0003"+
		",\u0217\b,\u0001,\u0001,\u0003,\u021b\b,\u0001,\u0001,\u0001,\u0003,\u0220"+
		"\b,\u0001,\u0001,\u0001,\u0001-\u0001-\u0001-\u0001-\u0001.\u0001.\u0003"+
		".\u022b\b.\u0001.\u0001.\u0005.\u022f\b.\n.\f.\u0232\t.\u0001.\u0001."+
		"\u0001/\u0001/\u0001/\u0003/\u0239\b/\u0001/\u0001/\u0001/\u0001/\u0003"+
		"/\u023f\b/\u0001/\u0001/\u0001/\u0001/\u0003/\u0245\b/\u0001/\u0001/\u0003"+
		"/\u0249\b/\u00010\u00010\u00030\u024d\b0\u00010\u00030\u0250\b0\u0001"+
		"0\u00010\u00010\u00030\u0255\b0\u00010\u00010\u00010\u00010\u00030\u025b"+
		"\b0\u00010\u00010\u00010\u00010\u00010\u00010\u00030\u0263\b0\u00010\u0001"+
		"0\u00010\u00030\u0268\b0\u00010\u00010\u00010\u00030\u026d\b0\u00011\u0001"+
		"1\u00031\u0271\b1\u00012\u00012\u00032\u0275\b2\u00013\u00013\u00013\u0001"+
		"4\u00014\u00014\u00054\u027d\b4\n4\f4\u0280\t4\u00014\u00014\u00034\u0284"+
		"\b4\u00014\u00034\u0287\b4\u00015\u00015\u00015\u00035\u028c\b5\u0001"+
		"6\u00016\u00016\u00017\u00017\u00037\u0293\b7\u00017\u00017\u00018\u0004"+
		"8\u0298\b8\u000b8\f8\u0299\u00019\u00019\u00019\u00019\u0001:\u0005:\u02a1"+
		"\b:\n:\f:\u02a4\t:\u0001:\u0003:\u02a7\b:\u0001:\u0004:\u02aa\b:\u000b"+
		":\f:\u02ab\u0001:\u0005:\u02af\b:\n:\f:\u02b2\t:\u0001:\u0005:\u02b5\b"+
		":\n:\f:\u02b8\t:\u0001;\u0003;\u02bb\b;\u0001;\u0001;\u0001<\u0001<\u0001"+
		"<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0003<\u02ca"+
		"\b<\u0001<\u0003<\u02cd\b<\u0001<\u0001<\u0001<\u0003<\u02d2\b<\u0001"+
		"<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001<\u0001"+
		"<\u0001<\u0001<\u0001<\u0001<\u0003<\u02e3\b<\u0001<\u0003<\u02e6\b<\u0001"+
		"=\u0001=\u0001=\u0001=\u0001=\u0001=\u0001=\u0003=\u02ef\b=\u0001>\u0001"+
		">\u0001>\u0001>\u0005>\u02f5\b>\n>\f>\u02f8\t>\u0001>\u0003>\u02fb\b>"+
		"\u0003>\u02fd\b>\u0001>\u0001>\u0001?\u0003?\u0302\b?\u0001?\u0001?\u0003"+
		"?\u0306\b?\u0001@\u0001@\u0001@\u0005@\u030b\b@\n@\f@\u030e\t@\u0001A"+
		"\u0001A\u0001B\u0001B\u0001B\u0001B\u0003B\u0316\bB\u0001B\u0001B\u0001"+
		"B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001"+
		"B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001"+
		"B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001"+
		"B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001"+
		"B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0003B\u034a\bB\u0001"+
		"B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001"+
		"B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001"+
		"B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001"+
		"B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001"+
		"B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001"+
		"B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0003"+
		"B\u0388\bB\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0003B\u0390\bB\u0001"+
		"B\u0001B\u0003B\u0394\bB\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0001"+
		"B\u0001B\u0001B\u0001B\u0001B\u0005B\u03a1\bB\nB\fB\u03a4\tB\u0001C\u0001"+
		"C\u0001C\u0001D\u0001D\u0001D\u0001D\u0003D\u03ad\bD\u0001E\u0001E\u0001"+
		"E\u0001E\u0005E\u03b3\bE\nE\fE\u03b6\tE\u0001E\u0003E\u03b9\bE\u0003E"+
		"\u03bb\bE\u0001E\u0001E\u0001F\u0001F\u0003F\u03c1\bF\u0001F\u0001F\u0003"+
		"F\u03c5\bF\u0001F\u0001F\u0003F\u03c9\bF\u0001F\u0001F\u0001F\u0003F\u03ce"+
		"\bF\u0001F\u0001F\u0001F\u0001F\u0003F\u03d4\bF\u0001G\u0001G\u0001G\u0003"+
		"G\u03d9\bG\u0001G\u0003G\u03dc\bG\u0001H\u0001H\u0003H\u03e0\bH\u0001"+
		"I\u0001I\u0001J\u0001J\u0001J\u0001J\u0001J\u0001J\u0001J\u0003J\u03eb"+
		"\bJ\u0001K\u0001K\u0005K\u03ef\bK\nK\fK\u03f2\tK\u0001K\u0001K\u0001L"+
		"\u0001L\u0001L\u0001L\u0001L\u0003L\u03fb\bL\u0001M\u0001M\u0001N\u0001"+
		"N\u0001O\u0001O\u0001O\u0001O\u0001P\u0001P\u0001P\u0001P\u0001Q\u0001"+
		"Q\u0003Q\u040b\bQ\u0001R\u0001R\u0001S\u0001S\u0001S\u0003S\u0412\bS\u0001"+
		"T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001"+
		"T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001"+
		"T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001"+
		"T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001"+
		"T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0003T\u0444\bT\u0001"+
		"U\u0001U\u0001V\u0001V\u0001V\u0001V\u0003V\u044c\bV\u0001V\u0000\u0001"+
		"\u0084W\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018"+
		"\u001a\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080"+
		"\u0082\u0084\u0086\u0088\u008a\u008c\u008e\u0090\u0092\u0094\u0096\u0098"+
		"\u009a\u009c\u009e\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa\u00ac\u0000\f\u0003"+
		"\u0000ffppzz\u0001\u0000fg\u0001\u0000\u001a\u001c\u0001\u0000\u0016\u0017"+
		"\u0001\u0000 \"\u0001\u0000#&\u0001\u0000\'*\u0001\u00000<\u0001\u0000"+
		"@D\u0001\u0000EH\u0004\u0000cfoosszz\u0001\u0000rs\u04f6\u0000\u00af\u0001"+
		"\u0000\u0000\u0000\u0002\u00b6\u0001\u0000\u0000\u0000\u0004\u00cc\u0001"+
		"\u0000\u0000\u0000\u0006\u00ce\u0001\u0000\u0000\u0000\b\u00d5\u0001\u0000"+
		"\u0000\u0000\n\u00d9\u0001\u0000\u0000\u0000\f\u00e8\u0001\u0000\u0000"+
		"\u0000\u000e\u00ea\u0001\u0000\u0000\u0000\u0010\u00fb\u0001\u0000\u0000"+
		"\u0000\u0012\u0102\u0001\u0000\u0000\u0000\u0014\u0104\u0001\u0000\u0000"+
		"\u0000\u0016\u0106\u0001\u0000\u0000\u0000\u0018\u010b\u0001\u0000\u0000"+
		"\u0000\u001a\u0111\u0001\u0000\u0000\u0000\u001c\u0114\u0001\u0000\u0000"+
		"\u0000\u001e\u0128\u0001\u0000\u0000\u0000 \u0134\u0001\u0000\u0000\u0000"+
		"\"\u0136\u0001\u0000\u0000\u0000$\u0147\u0001\u0000\u0000\u0000&\u014f"+
		"\u0001\u0000\u0000\u0000(\u0151\u0001\u0000\u0000\u0000*\u0154\u0001\u0000"+
		"\u0000\u0000,\u015d\u0001\u0000\u0000\u0000.\u0162\u0001\u0000\u0000\u0000"+
		"0\u0164\u0001\u0000\u0000\u00002\u0168\u0001\u0000\u0000\u00004\u01a8"+
		"\u0001\u0000\u0000\u00006\u01ad\u0001\u0000\u0000\u00008\u01af\u0001\u0000"+
		"\u0000\u0000:\u01b6\u0001\u0000\u0000\u0000<\u01bd\u0001\u0000\u0000\u0000"+
		">\u01c4\u0001\u0000\u0000\u0000@\u01cb\u0001\u0000\u0000\u0000B\u01d1"+
		"\u0001\u0000\u0000\u0000D\u01d7\u0001\u0000\u0000\u0000F\u01e4\u0001\u0000"+
		"\u0000\u0000H\u01e8\u0001\u0000\u0000\u0000J\u01ee\u0001\u0000\u0000\u0000"+
		"L\u01f3\u0001\u0000\u0000\u0000N\u01f7\u0001\u0000\u0000\u0000P\u01fc"+
		"\u0001\u0000\u0000\u0000R\u0205\u0001\u0000\u0000\u0000T\u020f\u0001\u0000"+
		"\u0000\u0000V\u0212\u0001\u0000\u0000\u0000X\u0216\u0001\u0000\u0000\u0000"+
		"Z\u0224\u0001\u0000\u0000\u0000\\\u022a\u0001\u0000\u0000\u0000^\u0248"+
		"\u0001\u0000\u0000\u0000`\u026c\u0001\u0000\u0000\u0000b\u026e\u0001\u0000"+
		"\u0000\u0000d\u0274\u0001\u0000\u0000\u0000f\u0276\u0001\u0000\u0000\u0000"+
		"h\u0286\u0001\u0000\u0000\u0000j\u0288\u0001\u0000\u0000\u0000l\u028d"+
		"\u0001\u0000\u0000\u0000n\u0290\u0001\u0000\u0000\u0000p\u0297\u0001\u0000"+
		"\u0000\u0000r\u029b\u0001\u0000\u0000\u0000t\u02a2\u0001\u0000\u0000\u0000"+
		"v\u02ba\u0001\u0000\u0000\u0000x\u02e5\u0001\u0000\u0000\u0000z\u02ee"+
		"\u0001\u0000\u0000\u0000|\u02f0\u0001\u0000\u0000\u0000~\u0301\u0001\u0000"+
		"\u0000\u0000\u0080\u0307\u0001\u0000\u0000\u0000\u0082\u030f\u0001\u0000"+
		"\u0000\u0000\u0084\u0349\u0001\u0000\u0000\u0000\u0086\u03a5\u0001\u0000"+
		"\u0000\u0000\u0088\u03ac\u0001\u0000\u0000\u0000\u008a\u03ae\u0001\u0000"+
		"\u0000\u0000\u008c\u03d3\u0001\u0000\u0000\u0000\u008e\u03db\u0001\u0000"+
		"\u0000\u0000\u0090\u03df\u0001\u0000\u0000\u0000\u0092\u03e1\u0001\u0000"+
		"\u0000\u0000\u0094\u03ea\u0001\u0000\u0000\u0000\u0096\u03ec\u0001\u0000"+
		"\u0000\u0000\u0098\u03fa\u0001\u0000\u0000\u0000\u009a\u03fc\u0001\u0000"+
		"\u0000\u0000\u009c\u03fe\u0001\u0000\u0000\u0000\u009e\u0400\u0001\u0000"+
		"\u0000\u0000\u00a0\u0404\u0001\u0000\u0000\u0000\u00a2\u040a\u0001\u0000"+
		"\u0000\u0000\u00a4\u040c\u0001\u0000\u0000\u0000\u00a6\u0411\u0001\u0000"+
		"\u0000\u0000\u00a8\u0443\u0001\u0000\u0000\u0000\u00aa\u0445\u0001\u0000"+
		"\u0000\u0000\u00ac\u044b\u0001\u0000\u0000\u0000\u00ae\u00b0\u0005\u0001"+
		"\u0000\u0000\u00af\u00ae\u0001\u0000\u0000\u0000\u00af\u00b0\u0001\u0000"+
		"\u0000\u0000\u00b0\u00b2\u0001\u0000\u0000\u0000\u00b1\u00b3\u0003p8\u0000"+
		"\u00b2\u00b1\u0001\u0000\u0000\u0000\u00b2\u00b3\u0001\u0000\u0000\u0000"+
		"\u00b3\u00b4\u0001\u0000\u0000\u0000\u00b4\u00b5\u0005\u0000\u0000\u0001"+
		"\u00b5\u0001\u0001\u0000\u0000\u0000\u00b6\u00b7\u0003\u0004\u0002\u0000"+
		"\u00b7\u0003\u0001\u0000\u0000\u0000\u00b8\u00cd\u0003\u0006\u0003\u0000"+
		"\u00b9\u00cd\u0003(\u0014\u0000\u00ba\u00cd\u0003\n\u0005\u0000\u00bb"+
		"\u00cd\u0003\u001e\u000f\u0000\u00bc\u00cd\u0003.\u0017\u0000\u00bd\u00cd"+
		"\u0003Z-\u0000\u00be\u00cd\u0003X,\u0000\u00bf\u00cd\u00030\u0018\u0000"+
		"\u00c0\u00cd\u00032\u0019\u0000\u00c1\u00cd\u00034\u001a\u0000\u00c2\u00cd"+
		"\u00038\u001c\u0000\u00c3\u00cd\u0003:\u001d\u0000\u00c4\u00cd\u0003<"+
		"\u001e\u0000\u00c5\u00cd\u0003>\u001f\u0000\u00c6\u00cd\u0003@ \u0000"+
		"\u00c7\u00cd\u0003L&\u0000\u00c8\u00cd\u0003B!\u0000\u00c9\u00cd\u0003"+
		"N\'\u0000\u00ca\u00cd\u0003P(\u0000\u00cb\u00cd\u0003V+\u0000\u00cc\u00b8"+
		"\u0001\u0000\u0000\u0000\u00cc\u00b9\u0001\u0000\u0000\u0000\u00cc\u00ba"+
		"\u0001\u0000\u0000\u0000\u00cc\u00bb\u0001\u0000\u0000\u0000\u00cc\u00bc"+
		"\u0001\u0000\u0000\u0000\u00cc\u00bd\u0001\u0000\u0000\u0000\u00cc\u00be"+
		"\u0001\u0000\u0000\u0000\u00cc\u00bf\u0001\u0000\u0000\u0000\u00cc\u00c0"+
		"\u0001\u0000\u0000\u0000\u00cc\u00c1\u0001\u0000\u0000\u0000\u00cc\u00c2"+
		"\u0001\u0000\u0000\u0000\u00cc\u00c3\u0001\u0000\u0000\u0000\u00cc\u00c4"+
		"\u0001\u0000\u0000\u0000\u00cc\u00c5\u0001\u0000\u0000\u0000\u00cc\u00c6"+
		"\u0001\u0000\u0000\u0000\u00cc\u00c7\u0001\u0000\u0000\u0000\u00cc\u00c8"+
		"\u0001\u0000\u0000\u0000\u00cc\u00c9\u0001\u0000\u0000\u0000\u00cc\u00ca"+
		"\u0001\u0000\u0000\u0000\u00cc\u00cb\u0001\u0000\u0000\u0000\u00cd\u0005"+
		"\u0001\u0000\u0000\u0000\u00ce\u00d0\u0005\t\u0000\u0000\u00cf\u00d1\u0003"+
		"\b\u0004\u0000\u00d0\u00cf\u0001\u0000\u0000\u0000\u00d0\u00d1\u0001\u0000"+
		"\u0000\u0000\u00d1\u00d2\u0001\u0000\u0000\u0000\u00d2\u00d3\u0005\u000b"+
		"\u0000\u0000\u00d3\u0007\u0001\u0000\u0000\u0000\u00d4\u00d6\u0003\u0004"+
		"\u0002\u0000\u00d5\u00d4\u0001\u0000\u0000\u0000\u00d6\u00d7\u0001\u0000"+
		"\u0000\u0000\u00d7\u00d5\u0001\u0000\u0000\u0000\u00d7\u00d8\u0001\u0000"+
		"\u0000\u0000\u00d8\t\u0001\u0000\u0000\u0000\u00d9\u00da\u0005n\u0000"+
		"\u0000\u00da\u00db\u0003\f\u0006\u0000\u00db\u000b\u0001\u0000\u0000\u0000"+
		"\u00dc\u00de\u0003\u0016\u000b\u0000\u00dd\u00dc\u0001\u0000\u0000\u0000"+
		"\u00dd\u00de\u0001\u0000\u0000\u0000\u00de\u00e1\u0001\u0000\u0000\u0000"+
		"\u00df\u00e2\u0003\u0018\f\u0000\u00e0\u00e2\u0003\u000e\u0007\u0000\u00e1"+
		"\u00df\u0001\u0000\u0000\u0000\u00e1\u00e0\u0001\u0000\u0000\u0000\u00e2"+
		"\u00e3\u0001\u0000\u0000\u0000\u00e3\u00e4\u0003\u001a\r\u0000\u00e4\u00e5"+
		"\u0003\u00acV\u0000\u00e5\u00e9\u0001\u0000\u0000\u0000\u00e6\u00e7\u0005"+
		"{\u0000\u0000\u00e7\u00e9\u0003\u00acV\u0000\u00e8\u00dd\u0001\u0000\u0000"+
		"\u0000\u00e8\u00e6\u0001\u0000\u0000\u0000\u00e9\r\u0001\u0000\u0000\u0000"+
		"\u00ea\u00f0\u0005\t\u0000\u0000\u00eb\u00ec\u0003\u0010\b\u0000\u00ec"+
		"\u00ed\u0005\r\u0000\u0000\u00ed\u00ef\u0001\u0000\u0000\u0000\u00ee\u00eb"+
		"\u0001\u0000\u0000\u0000\u00ef\u00f2\u0001\u0000\u0000\u0000\u00f0\u00ee"+
		"\u0001\u0000\u0000\u0000\u00f0\u00f1\u0001\u0000\u0000\u0000\u00f1\u00f7"+
		"\u0001\u0000\u0000\u0000\u00f2\u00f0\u0001\u0000\u0000\u0000\u00f3\u00f5"+
		"\u0003\u0010\b\u0000\u00f4\u00f6\u0005\r\u0000\u0000\u00f5\u00f4\u0001"+
		"\u0000\u0000\u0000\u00f5\u00f6\u0001\u0000\u0000\u0000\u00f6\u00f8\u0001"+
		"\u0000\u0000\u0000\u00f7\u00f3\u0001\u0000\u0000\u0000\u00f7\u00f8\u0001"+
		"\u0000\u0000\u0000\u00f8\u00f9\u0001\u0000\u0000\u0000\u00f9\u00fa\u0005"+
		"\u000b\u0000\u0000\u00fa\u000f\u0001\u0000\u0000\u0000\u00fb\u00fe\u0003"+
		"\u0012\t\u0000\u00fc\u00fd\u0005c\u0000\u0000\u00fd\u00ff\u0003\u0014"+
		"\n\u0000\u00fe\u00fc\u0001\u0000\u0000\u0000\u00fe\u00ff\u0001\u0000\u0000"+
		"\u0000\u00ff\u0011\u0001\u0000\u0000\u0000\u0100\u0103\u0003\u00a2Q\u0000"+
		"\u0101\u0103\u0005{\u0000\u0000\u0102\u0100\u0001\u0000\u0000\u0000\u0102"+
		"\u0101\u0001\u0000\u0000\u0000\u0103\u0013\u0001\u0000\u0000\u0000\u0104"+
		"\u0105\u0007\u0000\u0000\u0000\u0105\u0015\u0001\u0000\u0000\u0000\u0106"+
		"\u0107\u0003\u001c\u000e\u0000\u0107\u0108\u0005\r\u0000\u0000\u0108\u0017"+
		"\u0001\u0000\u0000\u0000\u0109\u010c\u0005\u001a\u0000\u0000\u010a\u010c"+
		"\u0003\u00a2Q\u0000\u010b\u0109\u0001\u0000\u0000\u0000\u010b\u010a\u0001"+
		"\u0000\u0000\u0000\u010c\u010f\u0001\u0000\u0000\u0000\u010d\u010e\u0005"+
		"c\u0000\u0000\u010e\u0110\u0003\u00a2Q\u0000\u010f\u010d\u0001\u0000\u0000"+
		"\u0000\u010f\u0110\u0001\u0000\u0000\u0000\u0110\u0019\u0001\u0000\u0000"+
		"\u0000\u0111\u0112\u0005d\u0000\u0000\u0112\u0113\u0005{\u0000\u0000\u0113"+
		"\u001b\u0001\u0000\u0000\u0000\u0114\u0117\u0003\u00a2Q\u0000\u0115\u0116"+
		"\u0005c\u0000\u0000\u0116\u0118\u0003\u00a2Q\u0000\u0117\u0115\u0001\u0000"+
		"\u0000\u0000\u0117\u0118\u0001\u0000\u0000\u0000\u0118\u001d\u0001\u0000"+
		"\u0000\u0000\u0119\u011b\u0005m\u0000\u0000\u011a\u011c\u0005]\u0000\u0000"+
		"\u011b\u011a\u0001\u0000\u0000\u0000\u011b\u011c\u0001\u0000\u0000\u0000"+
		"\u011c\u011f\u0001\u0000\u0000\u0000\u011d\u0120\u0003 \u0010\u0000\u011e"+
		"\u0120\u0003&\u0013\u0000\u011f\u011d\u0001\u0000\u0000\u0000\u011f\u011e"+
		"\u0001\u0000\u0000\u0000\u0120\u0121\u0001\u0000\u0000\u0000\u0121\u0122"+
		"\u0003\u00acV\u0000\u0122\u0129\u0001\u0000\u0000\u0000\u0123\u0124\u0005"+
		"m\u0000\u0000\u0124\u0125\u0005]\u0000\u0000\u0125\u0126\u0003\u0082A"+
		"\u0000\u0126\u0127\u0003\u00acV\u0000\u0127\u0129\u0001\u0000\u0000\u0000"+
		"\u0128\u0119\u0001\u0000\u0000\u0000\u0128\u0123\u0001\u0000\u0000\u0000"+
		"\u0129\u001f\u0001\u0000\u0000\u0000\u012a\u012b\u0003\u0018\f\u0000\u012b"+
		"\u012c\u0003\u001a\r\u0000\u012c\u012d\u0003\u00acV\u0000\u012d\u0135"+
		"\u0001\u0000\u0000\u0000\u012e\u0130\u0003\"\u0011\u0000\u012f\u0131\u0003"+
		"\u001a\r\u0000\u0130\u012f\u0001\u0000\u0000\u0000\u0130\u0131\u0001\u0000"+
		"\u0000\u0000\u0131\u0132\u0001\u0000\u0000\u0000\u0132\u0133\u0003\u00ac"+
		"V\u0000\u0133\u0135\u0001\u0000\u0000\u0000\u0134\u012a\u0001\u0000\u0000"+
		"\u0000\u0134\u012e\u0001\u0000\u0000\u0000\u0135!\u0001\u0000\u0000\u0000"+
		"\u0136\u013c\u0005\t\u0000\u0000\u0137\u0138\u0003$\u0012\u0000\u0138"+
		"\u0139\u0005\r\u0000\u0000\u0139\u013b\u0001\u0000\u0000\u0000\u013a\u0137"+
		"\u0001\u0000\u0000\u0000\u013b\u013e\u0001\u0000\u0000\u0000\u013c\u013a"+
		"\u0001\u0000\u0000\u0000\u013c\u013d\u0001\u0000\u0000\u0000\u013d\u0143"+
		"\u0001\u0000\u0000\u0000\u013e\u013c\u0001\u0000\u0000\u0000\u013f\u0141"+
		"\u0003$\u0012\u0000\u0140\u0142\u0005\r\u0000\u0000\u0141\u0140\u0001"+
		"\u0000\u0000\u0000\u0141\u0142\u0001\u0000\u0000\u0000\u0142\u0144\u0001"+
		"\u0000\u0000\u0000\u0143\u013f\u0001\u0000\u0000\u0000\u0143\u0144\u0001"+
		"\u0000\u0000\u0000\u0144\u0145\u0001\u0000\u0000\u0000\u0145\u0146\u0005"+
		"\u000b\u0000\u0000\u0146#\u0001\u0000\u0000\u0000\u0147\u014a\u0003\u0012"+
		"\t\u0000\u0148\u0149\u0005c\u0000\u0000\u0149\u014b\u0003\u0012\t\u0000"+
		"\u014a\u0148\u0001\u0000\u0000\u0000\u014a\u014b\u0001\u0000\u0000\u0000"+
		"\u014b%\u0001\u0000\u0000\u0000\u014c\u0150\u0003(\u0014\u0000\u014d\u0150"+
		"\u0003Z-\u0000\u014e\u0150\u0003X,\u0000\u014f\u014c\u0001\u0000\u0000"+
		"\u0000\u014f\u014d\u0001\u0000\u0000\u0000\u014f\u014e\u0001\u0000\u0000"+
		"\u0000\u0150\'\u0001\u0000\u0000\u0000\u0151\u0152\u0003*\u0015\u0000"+
		"\u0152\u0153\u0003\u00acV\u0000\u0153)\u0001\u0000\u0000\u0000\u0154\u0155"+
		"\u00036\u001b\u0000\u0155\u015a\u0003,\u0016\u0000\u0156\u0157\u0005\r"+
		"\u0000\u0000\u0157\u0159\u0003,\u0016\u0000\u0158\u0156\u0001\u0000\u0000"+
		"\u0000\u0159\u015c\u0001\u0000\u0000\u0000\u015a\u0158\u0001\u0000\u0000"+
		"\u0000\u015a\u015b\u0001\u0000\u0000\u0000\u015b+\u0001\u0000\u0000\u0000"+
		"\u015c\u015a\u0001\u0000\u0000\u0000\u015d\u0160\u0003\u0088D\u0000\u015e"+
		"\u015f\u0005\u000e\u0000\u0000\u015f\u0161\u0003\u0082A\u0000\u0160\u015e"+
		"\u0001\u0000\u0000\u0000\u0160\u0161\u0001\u0000\u0000\u0000\u0161-\u0001"+
		"\u0000\u0000\u0000\u0162\u0163\u0005\f\u0000\u0000\u0163/\u0001\u0000"+
		"\u0000\u0000\u0164\u0165\u0004\u0018\u0000\u0000\u0165\u0166\u0003\u0080"+
		"@\u0000\u0166\u0167\u0003\u00acV\u0000\u01671\u0001\u0000\u0000\u0000"+
		"\u0168\u0169\u0005^\u0000\u0000\u0169\u016a\u0005\u0007\u0000\u0000\u016a"+
		"\u016b\u0003\u0080@\u0000\u016b\u016c\u0005\b\u0000\u0000\u016c\u016f"+
		"\u0003\u0004\u0002\u0000\u016d\u016e\u0005N\u0000\u0000\u016e\u0170\u0003"+
		"\u0004\u0002\u0000\u016f\u016d\u0001\u0000\u0000\u0000\u016f\u0170\u0001"+
		"\u0000\u0000\u0000\u01703\u0001\u0000\u0000\u0000\u0171\u0172\u0005J\u0000"+
		"\u0000\u0172\u0173\u0003\u0004\u0002\u0000\u0173\u0174\u0005X\u0000\u0000"+
		"\u0174\u0175\u0005\u0007\u0000\u0000\u0175\u0176\u0003\u0080@\u0000\u0176"+
		"\u0177\u0005\b\u0000\u0000\u0177\u0178\u0003\u00acV\u0000\u0178\u01a9"+
		"\u0001\u0000\u0000\u0000\u0179\u017a\u0005X\u0000\u0000\u017a\u017b\u0005"+
		"\u0007\u0000\u0000\u017b\u017c\u0003\u0080@\u0000\u017c\u017d\u0005\b"+
		"\u0000\u0000\u017d\u017e\u0003\u0004\u0002\u0000\u017e\u01a9\u0001\u0000"+
		"\u0000\u0000\u017f\u0180\u0005V\u0000\u0000\u0180\u0183\u0005\u0007\u0000"+
		"\u0000\u0181\u0184\u0003\u0080@\u0000\u0182\u0184\u0003*\u0015\u0000\u0183"+
		"\u0181\u0001\u0000\u0000\u0000\u0183\u0182\u0001\u0000\u0000\u0000\u0183"+
		"\u0184\u0001\u0000\u0000\u0000\u0184\u0185\u0001\u0000\u0000\u0000\u0185"+
		"\u0187\u0005\f\u0000\u0000\u0186\u0188\u0003\u0080@\u0000\u0187\u0186"+
		"\u0001\u0000\u0000\u0000\u0187\u0188\u0001\u0000\u0000\u0000\u0188\u0189"+
		"\u0001\u0000\u0000\u0000\u0189\u018b\u0005\f\u0000\u0000\u018a\u018c\u0003"+
		"\u0080@\u0000\u018b\u018a\u0001\u0000\u0000\u0000\u018b\u018c\u0001\u0000"+
		"\u0000\u0000\u018c\u018d\u0001\u0000\u0000\u0000\u018d\u018e\u0005\b\u0000"+
		"\u0000\u018e\u01a9\u0003\u0004\u0002\u0000\u018f\u0190\u0005V\u0000\u0000"+
		"\u0190\u0193\u0005\u0007\u0000\u0000\u0191\u0194\u0003\u0082A\u0000\u0192"+
		"\u0194\u0003*\u0015\u0000\u0193\u0191\u0001\u0000\u0000\u0000\u0193\u0192"+
		"\u0001\u0000\u0000\u0000\u0194\u0195\u0001\u0000\u0000\u0000\u0195\u0196"+
		"\u0005a\u0000\u0000\u0196\u0197\u0003\u0080@\u0000\u0197\u0198\u0005\b"+
		"\u0000\u0000\u0198\u0199\u0003\u0004\u0002\u0000\u0199\u01a9\u0001\u0000"+
		"\u0000\u0000\u019a\u019c\u0005V\u0000\u0000\u019b\u019d\u0005p\u0000\u0000"+
		"\u019c\u019b\u0001\u0000\u0000\u0000\u019c\u019d\u0001\u0000\u0000\u0000"+
		"\u019d\u019e\u0001\u0000\u0000\u0000\u019e\u01a1\u0005\u0007\u0000\u0000"+
		"\u019f\u01a2\u0003\u0082A\u0000\u01a0\u01a2\u0003*\u0015\u0000\u01a1\u019f"+
		"\u0001\u0000\u0000\u0000\u01a1\u01a0\u0001\u0000\u0000\u0000\u01a2\u01a3"+
		"\u0001\u0000\u0000\u0000\u01a3\u01a4\u0005e\u0000\u0000\u01a4\u01a5\u0003"+
		"\u0080@\u0000\u01a5\u01a6\u0005\b\u0000\u0000\u01a6\u01a7\u0003\u0004"+
		"\u0002\u0000\u01a7\u01a9\u0001\u0000\u0000\u0000\u01a8\u0171\u0001\u0000"+
		"\u0000\u0000\u01a8\u0179\u0001\u0000\u0000\u0000\u01a8\u017f\u0001\u0000"+
		"\u0000\u0000\u01a8\u018f\u0001\u0000\u0000\u0000\u01a8\u019a\u0001\u0000"+
		"\u0000\u0000\u01a95\u0001\u0000\u0000\u0000\u01aa\u01ae\u0005P\u0000\u0000"+
		"\u01ab\u01ae\u0003\u00aaU\u0000\u01ac\u01ae\u0005l\u0000\u0000\u01ad\u01aa"+
		"\u0001\u0000\u0000\u0000\u01ad\u01ab\u0001\u0000\u0000\u0000\u01ad\u01ac"+
		"\u0001\u0000\u0000\u0000\u01ae7\u0001\u0000\u0000\u0000\u01af\u01b2\u0005"+
		"U\u0000\u0000\u01b0\u01b1\u0004\u001c\u0001\u0000\u01b1\u01b3\u0003\u00a4"+
		"R\u0000\u01b2\u01b0\u0001\u0000\u0000\u0000\u01b2\u01b3\u0001\u0000\u0000"+
		"\u0000\u01b3\u01b4\u0001\u0000\u0000\u0000\u01b4\u01b5\u0003\u00acV\u0000"+
		"\u01b59\u0001\u0000\u0000\u0000\u01b6\u01b9\u0005I\u0000\u0000\u01b7\u01b8"+
		"\u0004\u001d\u0002\u0000\u01b8\u01ba\u0003\u00a4R\u0000\u01b9\u01b7\u0001"+
		"\u0000\u0000\u0000\u01b9\u01ba\u0001\u0000\u0000\u0000\u01ba\u01bb\u0001"+
		"\u0000\u0000\u0000\u01bb\u01bc\u0003\u00acV\u0000\u01bc;\u0001\u0000\u0000"+
		"\u0000\u01bd\u01c0\u0005S\u0000\u0000\u01be\u01bf\u0004\u001e\u0003\u0000"+
		"\u01bf\u01c1\u0003\u0080@\u0000\u01c0\u01be\u0001\u0000\u0000\u0000\u01c0"+
		"\u01c1\u0001\u0000\u0000\u0000\u01c1\u01c2\u0001\u0000\u0000\u0000\u01c2"+
		"\u01c3\u0003\u00acV\u0000\u01c3=\u0001\u0000\u0000\u0000\u01c4\u01c7\u0007"+
		"\u0001\u0000\u0000\u01c5\u01c6\u0004\u001f\u0004\u0000\u01c6\u01c8\u0003"+
		"\u0080@\u0000\u01c7\u01c5\u0001\u0000\u0000\u0000\u01c7\u01c8\u0001\u0000"+
		"\u0000\u0000\u01c8\u01c9\u0001\u0000\u0000\u0000\u01c9\u01ca\u0003\u00ac"+
		"V\u0000\u01ca?\u0001\u0000\u0000\u0000\u01cb\u01cc\u0005\\\u0000\u0000"+
		"\u01cc\u01cd\u0005\u0007\u0000\u0000\u01cd\u01ce\u0003\u0080@\u0000\u01ce"+
		"\u01cf\u0005\b\u0000\u0000\u01cf\u01d0\u0003\u0004\u0002\u0000\u01d0A"+
		"\u0001\u0000\u0000\u0000\u01d1\u01d2\u0005W\u0000\u0000\u01d2\u01d3\u0005"+
		"\u0007\u0000\u0000\u01d3\u01d4\u0003\u0080@\u0000\u01d4\u01d5\u0005\b"+
		"\u0000\u0000\u01d5\u01d6\u0003D\"\u0000\u01d6C\u0001\u0000\u0000\u0000"+
		"\u01d7\u01d9\u0005\t\u0000\u0000\u01d8\u01da\u0003F#\u0000\u01d9\u01d8"+
		"\u0001\u0000\u0000\u0000\u01d9\u01da\u0001\u0000\u0000\u0000\u01da\u01df"+
		"\u0001\u0000\u0000\u0000\u01db\u01dd\u0003J%\u0000\u01dc\u01de\u0003F"+
		"#\u0000\u01dd\u01dc\u0001\u0000\u0000\u0000\u01dd\u01de\u0001\u0000\u0000"+
		"\u0000\u01de\u01e0\u0001\u0000\u0000\u0000\u01df\u01db\u0001\u0000\u0000"+
		"\u0000\u01df\u01e0\u0001\u0000\u0000\u0000\u01e0\u01e1\u0001\u0000\u0000"+
		"\u0000\u01e1\u01e2\u0005\u000b\u0000\u0000\u01e2E\u0001\u0000\u0000\u0000"+
		"\u01e3\u01e5\u0003H$\u0000\u01e4\u01e3\u0001\u0000\u0000\u0000\u01e5\u01e6"+
		"\u0001\u0000\u0000\u0000\u01e6\u01e4\u0001\u0000\u0000\u0000\u01e6\u01e7"+
		"\u0001\u0000\u0000\u0000\u01e7G\u0001\u0000\u0000\u0000\u01e8\u01e9\u0005"+
		"M\u0000\u0000\u01e9\u01ea\u0003\u0080@\u0000\u01ea\u01ec\u0005\u0011\u0000"+
		"\u0000\u01eb\u01ed\u0003\b\u0004\u0000\u01ec\u01eb\u0001\u0000\u0000\u0000"+
		"\u01ec\u01ed\u0001\u0000\u0000\u0000\u01edI\u0001\u0000\u0000\u0000\u01ee"+
		"\u01ef\u0005]\u0000\u0000\u01ef\u01f1\u0005\u0011\u0000\u0000\u01f0\u01f2"+
		"\u0003\b\u0004\u0000\u01f1\u01f0\u0001\u0000\u0000\u0000\u01f1\u01f2\u0001"+
		"\u0000\u0000\u0000\u01f2K\u0001\u0000\u0000\u0000\u01f3\u01f4\u0003\u00a4"+
		"R\u0000\u01f4\u01f5\u0005\u0011\u0000\u0000\u01f5\u01f6\u0003\u0004\u0002"+
		"\u0000\u01f6M\u0001\u0000\u0000\u0000\u01f7\u01f8\u0005_\u0000\u0000\u01f8"+
		"\u01f9\u0004\'\u0005\u0000\u01f9\u01fa\u0003\u0080@\u0000\u01fa\u01fb"+
		"\u0003\u00acV\u0000\u01fbO\u0001\u0000\u0000\u0000\u01fc\u01fd\u0005b"+
		"\u0000\u0000\u01fd\u0203\u0003\u0006\u0003\u0000\u01fe\u0200\u0003R)\u0000"+
		"\u01ff\u0201\u0003T*\u0000\u0200\u01ff\u0001\u0000\u0000\u0000\u0200\u0201"+
		"\u0001\u0000\u0000\u0000\u0201\u0204\u0001\u0000\u0000\u0000\u0202\u0204"+
		"\u0003T*\u0000\u0203\u01fe\u0001\u0000\u0000\u0000\u0203\u0202\u0001\u0000"+
		"\u0000\u0000\u0204Q\u0001\u0000\u0000\u0000\u0205\u020b\u0005Q\u0000\u0000"+
		"\u0206\u0208\u0005\u0007\u0000\u0000\u0207\u0209\u0003\u0088D\u0000\u0208"+
		"\u0207\u0001\u0000\u0000\u0000\u0208\u0209\u0001\u0000\u0000\u0000\u0209"+
		"\u020a\u0001\u0000\u0000\u0000\u020a\u020c\u0005\b\u0000\u0000\u020b\u0206"+
		"\u0001\u0000\u0000\u0000\u020b\u020c\u0001\u0000\u0000\u0000\u020c\u020d"+
		"\u0001\u0000\u0000\u0000\u020d\u020e\u0003\u0006\u0003\u0000\u020eS\u0001"+
		"\u0000\u0000\u0000\u020f\u0210\u0005R\u0000\u0000\u0210\u0211\u0003\u0006"+
		"\u0003\u0000\u0211U\u0001\u0000\u0000\u0000\u0212\u0213\u0005Y\u0000\u0000"+
		"\u0213\u0214\u0003\u00acV\u0000\u0214W\u0001\u0000\u0000\u0000\u0215\u0217"+
		"\u0005o\u0000\u0000\u0216\u0215\u0001\u0000\u0000\u0000\u0216\u0217\u0001"+
		"\u0000\u0000\u0000\u0217\u0218\u0001\u0000\u0000\u0000\u0218\u021a\u0005"+
		"Z\u0000\u0000\u0219\u021b\u0005\u001a\u0000\u0000\u021a\u0219\u0001\u0000"+
		"\u0000\u0000\u021a\u021b\u0001\u0000\u0000\u0000\u021b\u021c\u0001\u0000"+
		"\u0000\u0000\u021c\u021d\u0003\u00a4R\u0000\u021d\u021f\u0005\u0007\u0000"+
		"\u0000\u021e\u0220\u0003h4\u0000\u021f\u021e\u0001\u0000\u0000\u0000\u021f"+
		"\u0220\u0001\u0000\u0000\u0000\u0220\u0221\u0001\u0000\u0000\u0000\u0221"+
		"\u0222\u0005\b\u0000\u0000\u0222\u0223\u0003n7\u0000\u0223Y\u0001\u0000"+
		"\u0000\u0000\u0224\u0225\u0005h\u0000\u0000\u0225\u0226\u0003\u00a4R\u0000"+
		"\u0226\u0227\u0003\\.\u0000\u0227[\u0001\u0000\u0000\u0000\u0228\u0229"+
		"\u0005j\u0000\u0000\u0229\u022b\u0003\u0082A\u0000\u022a\u0228\u0001\u0000"+
		"\u0000\u0000\u022a\u022b\u0001\u0000\u0000\u0000\u022b\u022c\u0001\u0000"+
		"\u0000\u0000\u022c\u0230\u0005\t\u0000\u0000\u022d\u022f\u0003^/\u0000"+
		"\u022e\u022d\u0001\u0000\u0000\u0000\u022f\u0232\u0001\u0000\u0000\u0000"+
		"\u0230\u022e\u0001\u0000\u0000\u0000\u0230\u0231\u0001\u0000\u0000\u0000"+
		"\u0231\u0233\u0001\u0000\u0000\u0000\u0232\u0230\u0001\u0000\u0000\u0000"+
		"\u0233\u0234\u0005\u000b\u0000\u0000\u0234]\u0001\u0000\u0000\u0000\u0235"+
		"\u0239\u0005y\u0000\u0000\u0236\u0237\u0004/\u0006\u0000\u0237\u0239\u0003"+
		"\u00a4R\u0000\u0238\u0235\u0001\u0000\u0000\u0000\u0238\u0236\u0001\u0000"+
		"\u0000\u0000\u0238\u0239\u0001\u0000\u0000\u0000\u0239\u023a\u0001\u0000"+
		"\u0000\u0000\u023a\u0249\u0003`0\u0000\u023b\u023f\u0005y\u0000\u0000"+
		"\u023c\u023d\u0004/\u0007\u0000\u023d\u023f\u0003\u00a4R\u0000\u023e\u023b"+
		"\u0001\u0000\u0000\u0000\u023e\u023c\u0001\u0000\u0000\u0000\u023e\u023f"+
		"\u0001\u0000\u0000\u0000\u023f\u0240\u0001\u0000\u0000\u0000\u0240\u0249"+
		"\u0003b1\u0000\u0241\u0245\u0005y\u0000\u0000\u0242\u0243\u0004/\b\u0000"+
		"\u0243\u0245\u0003\u00a4R\u0000\u0244\u0241\u0001\u0000\u0000\u0000\u0244"+
		"\u0242\u0001\u0000\u0000\u0000\u0245\u0246\u0001\u0000\u0000\u0000\u0246"+
		"\u0249\u0003\u0006\u0003\u0000\u0247\u0249\u0003.\u0017\u0000\u0248\u0238"+
		"\u0001\u0000\u0000\u0000\u0248\u023e\u0001\u0000\u0000\u0000\u0248\u0244"+
		"\u0001\u0000\u0000\u0000\u0248\u0247\u0001\u0000\u0000\u0000\u0249_\u0001"+
		"\u0000\u0000\u0000\u024a\u024b\u0005o\u0000\u0000\u024b\u024d\u00040\t"+
		"\u0000\u024c\u024a\u0001\u0000\u0000\u0000\u024c\u024d\u0001\u0000\u0000"+
		"\u0000\u024d\u024f\u0001\u0000\u0000\u0000\u024e\u0250\u0005\u001a\u0000"+
		"\u0000\u024f\u024e\u0001\u0000\u0000\u0000\u024f\u0250\u0001\u0000\u0000"+
		"\u0000\u0250\u0251\u0001\u0000\u0000\u0000\u0251\u0252\u0003d2\u0000\u0252"+
		"\u0254\u0005\u0007\u0000\u0000\u0253\u0255\u0003h4\u0000\u0254\u0253\u0001"+
		"\u0000\u0000\u0000\u0254\u0255\u0001\u0000\u0000\u0000\u0255\u0256\u0001"+
		"\u0000\u0000\u0000\u0256\u0257\u0005\b\u0000\u0000\u0257\u0258\u0003n"+
		"7\u0000\u0258\u026d\u0001\u0000\u0000\u0000\u0259\u025b\u0005\u001a\u0000"+
		"\u0000\u025a\u0259\u0001\u0000\u0000\u0000\u025a\u025b\u0001\u0000\u0000"+
		"\u0000\u025b\u025c\u0001\u0000\u0000\u0000\u025c\u025d\u0003\u009eO\u0000"+
		"\u025d\u025e\u0005\u0007\u0000\u0000\u025e\u025f\u0005\b\u0000\u0000\u025f"+
		"\u0260\u0003n7\u0000\u0260\u026d\u0001\u0000\u0000\u0000\u0261\u0263\u0005"+
		"\u001a\u0000\u0000\u0262\u0261\u0001\u0000\u0000\u0000\u0262\u0263\u0001"+
		"\u0000\u0000\u0000\u0263\u0264\u0001\u0000\u0000\u0000\u0264\u0265\u0003"+
		"\u00a0P\u0000\u0265\u0267\u0005\u0007\u0000\u0000\u0266\u0268\u0003h4"+
		"\u0000\u0267\u0266\u0001\u0000\u0000\u0000\u0267\u0268\u0001\u0000\u0000"+
		"\u0000\u0268\u0269\u0001\u0000\u0000\u0000\u0269\u026a\u0005\b\u0000\u0000"+
		"\u026a\u026b\u0003n7\u0000\u026b\u026d\u0001\u0000\u0000\u0000\u026c\u024c"+
		"\u0001\u0000\u0000\u0000\u026c\u025a\u0001\u0000\u0000\u0000\u026c\u0262"+
		"\u0001\u0000\u0000\u0000\u026da\u0001\u0000\u0000\u0000\u026e\u0270\u0003"+
		"d2\u0000\u026f\u0271\u0003\u0086C\u0000\u0270\u026f\u0001\u0000\u0000"+
		"\u0000\u0270\u0271\u0001\u0000\u0000\u0000\u0271c\u0001\u0000\u0000\u0000"+
		"\u0272\u0275\u0003z=\u0000\u0273\u0275\u0003f3\u0000\u0274\u0272\u0001"+
		"\u0000\u0000\u0000\u0274\u0273\u0001\u0000\u0000\u0000\u0275e\u0001\u0000"+
		"\u0000\u0000\u0276\u0277\u0005\u001f\u0000\u0000\u0277\u0278\u0003\u00a2"+
		"Q\u0000\u0278g\u0001\u0000\u0000\u0000\u0279\u027e\u0003j5\u0000\u027a"+
		"\u027b\u0005\r\u0000\u0000\u027b\u027d\u0003j5\u0000\u027c\u027a\u0001"+
		"\u0000\u0000\u0000\u027d\u0280\u0001\u0000\u0000\u0000\u027e\u027c\u0001"+
		"\u0000\u0000\u0000\u027e\u027f\u0001\u0000\u0000\u0000\u027f\u0283\u0001"+
		"\u0000\u0000\u0000\u0280\u027e\u0001\u0000\u0000\u0000\u0281\u0282\u0005"+
		"\r\u0000\u0000\u0282\u0284\u0003l6\u0000\u0283\u0281\u0001\u0000\u0000"+
		"\u0000\u0283\u0284\u0001\u0000\u0000\u0000\u0284\u0287\u0001\u0000\u0000"+
		"\u0000\u0285\u0287\u0003l6\u0000\u0286\u0279\u0001\u0000\u0000\u0000\u0286"+
		"\u0285\u0001\u0000\u0000\u0000\u0287i\u0001\u0000\u0000\u0000\u0288\u028b"+
		"\u0003\u0088D\u0000\u0289\u028a\u0005\u000e\u0000\u0000\u028a\u028c\u0003"+
		"\u0082A\u0000\u028b\u0289\u0001\u0000\u0000\u0000\u028b\u028c\u0001\u0000"+
		"\u0000\u0000\u028ck\u0001\u0000\u0000\u0000\u028d\u028e\u0005\u0012\u0000"+
		"\u0000\u028e\u028f\u0003\u0082A\u0000\u028fm\u0001\u0000\u0000\u0000\u0290"+
		"\u0292\u0005\t\u0000\u0000\u0291\u0293\u0003p8\u0000\u0292\u0291\u0001"+
		"\u0000\u0000\u0000\u0292\u0293\u0001\u0000\u0000\u0000\u0293\u0294\u0001"+
		"\u0000\u0000\u0000\u0294\u0295\u0005\u000b\u0000\u0000\u0295o\u0001\u0000"+
		"\u0000\u0000\u0296\u0298\u0003\u0002\u0001\u0000\u0297\u0296\u0001\u0000"+
		"\u0000\u0000\u0298\u0299\u0001\u0000\u0000\u0000\u0299\u0297\u0001\u0000"+
		"\u0000\u0000\u0299\u029a\u0001\u0000\u0000\u0000\u029aq\u0001\u0000\u0000"+
		"\u0000\u029b\u029c\u0005\u0005\u0000\u0000\u029c\u029d\u0003t:\u0000\u029d"+
		"\u029e\u0005\u0006\u0000\u0000\u029es\u0001\u0000\u0000\u0000\u029f\u02a1"+
		"\u0005\r\u0000\u0000\u02a0\u029f\u0001\u0000\u0000\u0000\u02a1\u02a4\u0001"+
		"\u0000\u0000\u0000\u02a2\u02a0\u0001\u0000\u0000\u0000\u02a2\u02a3\u0001"+
		"\u0000\u0000\u0000\u02a3\u02a6\u0001\u0000\u0000\u0000\u02a4\u02a2\u0001"+
		"\u0000\u0000\u0000\u02a5\u02a7\u0003v;\u0000\u02a6\u02a5\u0001\u0000\u0000"+
		"\u0000\u02a6\u02a7\u0001\u0000\u0000\u0000\u02a7\u02b0\u0001\u0000\u0000"+
		"\u0000\u02a8\u02aa\u0005\r\u0000\u0000\u02a9\u02a8\u0001\u0000\u0000\u0000"+
		"\u02aa\u02ab\u0001\u0000\u0000\u0000\u02ab\u02a9\u0001\u0000\u0000\u0000"+
		"\u02ab\u02ac\u0001\u0000\u0000\u0000\u02ac\u02ad\u0001\u0000\u0000\u0000"+
		"\u02ad\u02af\u0003v;\u0000\u02ae\u02a9\u0001\u0000\u0000\u0000\u02af\u02b2"+
		"\u0001\u0000\u0000\u0000\u02b0\u02ae\u0001\u0000\u0000\u0000\u02b0\u02b1"+
		"\u0001\u0000\u0000\u0000\u02b1\u02b6\u0001\u0000\u0000\u0000\u02b2\u02b0"+
		"\u0001\u0000\u0000\u0000\u02b3\u02b5\u0005\r\u0000\u0000\u02b4\u02b3\u0001"+
		"\u0000\u0000\u0000\u02b5\u02b8\u0001\u0000\u0000\u0000\u02b6\u02b4\u0001"+
		"\u0000\u0000\u0000\u02b6\u02b7\u0001\u0000\u0000\u0000\u02b7u\u0001\u0000"+
		"\u0000\u0000\u02b8\u02b6\u0001\u0000\u0000\u0000\u02b9\u02bb\u0005\u0012"+
		"\u0000\u0000\u02ba\u02b9\u0001\u0000\u0000\u0000\u02ba\u02bb\u0001\u0000"+
		"\u0000\u0000\u02bb\u02bc\u0001\u0000\u0000\u0000\u02bc\u02bd\u0003\u0082"+
		"A\u0000\u02bdw\u0001\u0000\u0000\u0000\u02be\u02bf\u0003z=\u0000\u02bf"+
		"\u02c0\u0005\u0011\u0000\u0000\u02c0\u02c1\u0003\u0082A\u0000\u02c1\u02e6"+
		"\u0001\u0000\u0000\u0000\u02c2\u02c3\u0005\u0005\u0000\u0000\u02c3\u02c4"+
		"\u0003\u0082A\u0000\u02c4\u02c5\u0005\u0006\u0000\u0000\u02c5\u02c6\u0005"+
		"\u0011\u0000\u0000\u02c6\u02c7\u0003\u0082A\u0000\u02c7\u02e6\u0001\u0000"+
		"\u0000\u0000\u02c8\u02ca\u0005o\u0000\u0000\u02c9\u02c8\u0001\u0000\u0000"+
		"\u0000\u02c9\u02ca\u0001\u0000\u0000\u0000\u02ca\u02cc\u0001\u0000\u0000"+
		"\u0000\u02cb\u02cd\u0005\u001a\u0000\u0000\u02cc\u02cb\u0001\u0000\u0000"+
		"\u0000\u02cc\u02cd\u0001\u0000\u0000\u0000\u02cd\u02ce\u0001\u0000\u0000"+
		"\u0000\u02ce\u02cf\u0003z=\u0000\u02cf\u02d1\u0005\u0007\u0000\u0000\u02d0"+
		"\u02d2\u0003h4\u0000\u02d1\u02d0\u0001\u0000\u0000\u0000\u02d1\u02d2\u0001"+
		"\u0000\u0000\u0000\u02d2\u02d3\u0001\u0000\u0000\u0000\u02d3\u02d4\u0005"+
		"\b\u0000\u0000\u02d4\u02d5\u0003n7\u0000\u02d5\u02e6\u0001\u0000\u0000"+
		"\u0000\u02d6\u02d7\u0003\u009eO\u0000\u02d7\u02d8\u0005\u0007\u0000\u0000"+
		"\u02d8\u02d9\u0005\b\u0000\u0000\u02d9\u02da\u0003n7\u0000\u02da\u02e6"+
		"\u0001\u0000\u0000\u0000\u02db\u02dc\u0003\u00a0P\u0000\u02dc\u02dd\u0005"+
		"\u0007\u0000\u0000\u02dd\u02de\u0003j5\u0000\u02de\u02df\u0005\b\u0000"+
		"\u0000\u02df\u02e0\u0003n7\u0000\u02e0\u02e6\u0001\u0000\u0000\u0000\u02e1"+
		"\u02e3\u0005\u0012\u0000\u0000\u02e2\u02e1\u0001\u0000\u0000\u0000\u02e2"+
		"\u02e3\u0001\u0000\u0000\u0000\u02e3\u02e4\u0001\u0000\u0000\u0000\u02e4"+
		"\u02e6\u0003\u0082A\u0000\u02e5\u02be\u0001\u0000\u0000\u0000\u02e5\u02c2"+
		"\u0001\u0000\u0000\u0000\u02e5\u02c9\u0001\u0000\u0000\u0000\u02e5\u02d6"+
		"\u0001\u0000\u0000\u0000\u02e5\u02db\u0001\u0000\u0000\u0000\u02e5\u02e2"+
		"\u0001\u0000\u0000\u0000\u02e6y\u0001\u0000\u0000\u0000\u02e7\u02ef\u0003"+
		"\u00a2Q\u0000\u02e8\u02ef\u0005{\u0000\u0000\u02e9\u02ef\u0003\u009aM"+
		"\u0000\u02ea\u02eb\u0005\u0005\u0000\u0000\u02eb\u02ec\u0003\u0082A\u0000"+
		"\u02ec\u02ed\u0005\u0006\u0000\u0000\u02ed\u02ef\u0001\u0000\u0000\u0000"+
		"\u02ee\u02e7\u0001\u0000\u0000\u0000\u02ee\u02e8\u0001\u0000\u0000\u0000"+
		"\u02ee\u02e9\u0001\u0000\u0000\u0000\u02ee\u02ea\u0001\u0000\u0000\u0000"+
		"\u02ef{\u0001\u0000\u0000\u0000\u02f0\u02fc\u0005\u0007\u0000\u0000\u02f1"+
		"\u02f6\u0003~?\u0000\u02f2\u02f3\u0005\r\u0000\u0000\u02f3\u02f5\u0003"+
		"~?\u0000\u02f4\u02f2\u0001\u0000\u0000\u0000\u02f5\u02f8\u0001\u0000\u0000"+
		"\u0000\u02f6\u02f4\u0001\u0000\u0000\u0000\u02f6\u02f7\u0001\u0000\u0000"+
		"\u0000\u02f7\u02fa\u0001\u0000\u0000\u0000\u02f8\u02f6\u0001\u0000\u0000"+
		"\u0000\u02f9\u02fb\u0005\r\u0000\u0000\u02fa\u02f9\u0001\u0000\u0000\u0000"+
		"\u02fa\u02fb\u0001\u0000\u0000\u0000\u02fb\u02fd\u0001\u0000\u0000\u0000"+
		"\u02fc\u02f1\u0001\u0000\u0000\u0000\u02fc\u02fd\u0001\u0000\u0000\u0000"+
		"\u02fd\u02fe\u0001\u0000\u0000\u0000\u02fe\u02ff\u0005\b\u0000\u0000\u02ff"+
		"}\u0001\u0000\u0000\u0000\u0300\u0302\u0005\u0012\u0000\u0000\u0301\u0300"+
		"\u0001\u0000\u0000\u0000\u0301\u0302\u0001\u0000\u0000\u0000\u0302\u0305"+
		"\u0001\u0000\u0000\u0000\u0303\u0306\u0003\u0082A\u0000\u0304\u0306\u0003"+
		"\u00a4R\u0000\u0305\u0303\u0001\u0000\u0000\u0000\u0305\u0304\u0001\u0000"+
		"\u0000\u0000\u0306\u007f\u0001\u0000\u0000\u0000\u0307\u030c\u0003\u0082"+
		"A\u0000\u0308\u0309\u0005\r\u0000\u0000\u0309\u030b\u0003\u0082A\u0000"+
		"\u030a\u0308\u0001\u0000\u0000\u0000\u030b\u030e\u0001\u0000\u0000\u0000"+
		"\u030c\u030a\u0001\u0000\u0000\u0000\u030c\u030d\u0001\u0000\u0000\u0000"+
		"\u030d\u0081\u0001\u0000\u0000\u0000\u030e\u030c\u0001\u0000\u0000\u0000"+
		"\u030f\u0310\u0003\u0084B\u0000\u0310\u0083\u0001\u0000\u0000\u0000\u0311"+
		"\u0312\u0006B\uffff\uffff\u0000\u0312\u034a\u0003\u008cF\u0000\u0313\u0315"+
		"\u0005h\u0000\u0000\u0314\u0316\u0003\u00a4R\u0000\u0315\u0314\u0001\u0000"+
		"\u0000\u0000\u0315\u0316\u0001\u0000\u0000\u0000\u0316\u0317\u0001\u0000"+
		"\u0000\u0000\u0317\u034a\u0003\\.\u0000\u0318\u0319\u0005O\u0000\u0000"+
		"\u0319\u031a\u0003\u00a4R\u0000\u031a\u031b\u0003|>\u0000\u031b\u034a"+
		"\u0001\u0000\u0000\u0000\u031c\u031d\u0005O\u0000\u0000\u031d\u031e\u0003"+
		"\u0084B\u0000\u031e\u031f\u0003|>\u0000\u031f\u034a\u0001\u0000\u0000"+
		"\u0000\u0320\u0321\u0005O\u0000\u0000\u0321\u034a\u0003\u0084B*\u0322"+
		"\u0323\u0005O\u0000\u0000\u0323\u0324\u0005\u0013\u0000\u0000\u0324\u034a"+
		"\u0003\u00a4R\u0000\u0325\u0326\u0005`\u0000\u0000\u0326\u034a\u0003\u0084"+
		"B%\u0327\u0328\u0005T\u0000\u0000\u0328\u034a\u0003\u0084B$\u0329\u032a"+
		"\u0005L\u0000\u0000\u032a\u034a\u0003\u0084B#\u032b\u032c\u0005\u0014"+
		"\u0000\u0000\u032c\u034a\u0003\u0084B\"\u032d\u032e\u0005\u0015\u0000"+
		"\u0000\u032e\u034a\u0003\u0084B!\u032f\u0330\u0005\u0016\u0000\u0000\u0330"+
		"\u034a\u0003\u0084B \u0331\u0332\u0005\u0017\u0000\u0000\u0332\u034a\u0003"+
		"\u0084B\u001f\u0333\u0334\u0005\u0018\u0000\u0000\u0334\u034a\u0003\u0084"+
		"B\u001e\u0335\u0336\u0005\u0019\u0000\u0000\u0336\u034a\u0003\u0084B\u001d"+
		"\u0337\u0338\u0005p\u0000\u0000\u0338\u034a\u0003\u0084B\u001c\u0339\u033a"+
		"\u0005n\u0000\u0000\u033a\u033b\u0005\u0007\u0000\u0000\u033b\u033c\u0003"+
		"\u0084B\u0000\u033c\u033d\u0005\b\u0000\u0000\u033d\u034a\u0001\u0000"+
		"\u0000\u0000\u033e\u034a\u0003>\u001f\u0000\u033f\u034a\u0005[\u0000\u0000"+
		"\u0340\u034a\u0003\u00a4R\u0000\u0341\u034a\u0005k\u0000\u0000\u0342\u034a"+
		"\u0003\u0094J\u0000\u0343\u034a\u0003r9\u0000\u0344\u034a\u0003\u008a"+
		"E\u0000\u0345\u0346\u0005\u0007\u0000\u0000\u0346\u0347\u0003\u0080@\u0000"+
		"\u0347\u0348\u0005\b\u0000\u0000\u0348\u034a\u0001\u0000\u0000\u0000\u0349"+
		"\u0311\u0001\u0000\u0000\u0000\u0349\u0313\u0001\u0000\u0000\u0000\u0349"+
		"\u0318\u0001\u0000\u0000\u0000\u0349\u031c\u0001\u0000\u0000\u0000\u0349"+
		"\u0320\u0001\u0000\u0000\u0000\u0349\u0322\u0001\u0000\u0000\u0000\u0349"+
		"\u0325\u0001\u0000\u0000\u0000\u0349\u0327\u0001\u0000\u0000\u0000\u0349"+
		"\u0329\u0001\u0000\u0000\u0000\u0349\u032b\u0001\u0000\u0000\u0000\u0349"+
		"\u032d\u0001\u0000\u0000\u0000\u0349\u032f\u0001\u0000\u0000\u0000\u0349"+
		"\u0331\u0001\u0000\u0000\u0000\u0349\u0333\u0001\u0000\u0000\u0000\u0349"+
		"\u0335\u0001\u0000\u0000\u0000\u0349\u0337\u0001\u0000\u0000\u0000\u0349"+
		"\u0339\u0001\u0000\u0000\u0000\u0349\u033e\u0001\u0000\u0000\u0000\u0349"+
		"\u033f\u0001\u0000\u0000\u0000\u0349\u0340\u0001\u0000\u0000\u0000\u0349"+
		"\u0341\u0001\u0000\u0000\u0000\u0349\u0342\u0001\u0000\u0000\u0000\u0349"+
		"\u0343\u0001\u0000\u0000\u0000\u0349\u0344\u0001\u0000\u0000\u0000\u0349"+
		"\u0345\u0001\u0000\u0000\u0000\u034a\u03a2\u0001\u0000\u0000\u0000\u034b"+
		"\u034c\n/\u0000\u0000\u034c\u034d\u0005\u0010\u0000\u0000\u034d\u03a1"+
		"\u0003\u0084B0\u034e\u034f\n\u001b\u0000\u0000\u034f\u0350\u0005\u001d"+
		"\u0000\u0000\u0350\u03a1\u0003\u0084B\u001b\u0351\u0352\n\u001a\u0000"+
		"\u0000\u0352\u0353\u0007\u0002\u0000\u0000\u0353\u03a1\u0003\u0084B\u001b"+
		"\u0354\u0355\n\u0019\u0000\u0000\u0355\u0356\u0007\u0003\u0000\u0000\u0356"+
		"\u03a1\u0003\u0084B\u001a\u0357\u0358\n\u0018\u0000\u0000\u0358\u0359"+
		"\u0005\u001e\u0000\u0000\u0359\u03a1\u0003\u0084B\u0019\u035a\u035b\n"+
		"\u0017\u0000\u0000\u035b\u035c\u0007\u0004\u0000\u0000\u035c\u03a1\u0003"+
		"\u0084B\u0018\u035d\u035e\n\u0016\u0000\u0000\u035e\u035f\u0007\u0005"+
		"\u0000\u0000\u035f\u03a1\u0003\u0084B\u0017\u0360\u0361\n\u0015\u0000"+
		"\u0000\u0361\u0362\u0005K\u0000\u0000\u0362\u03a1\u0003\u0084B\u0016\u0363"+
		"\u0364\n\u0014\u0000\u0000\u0364\u0365\u0005a\u0000\u0000\u0365\u03a1"+
		"\u0003\u0084B\u0015\u0366\u0367\n\u0013\u0000\u0000\u0367\u0368\u0007"+
		"\u0006\u0000\u0000\u0368\u03a1\u0003\u0084B\u0014\u0369\u036a\n\u0012"+
		"\u0000\u0000\u036a\u036b\u0005+\u0000\u0000\u036b\u03a1\u0003\u0084B\u0013"+
		"\u036c\u036d\n\u0011\u0000\u0000\u036d\u036e\u0005,\u0000\u0000\u036e"+
		"\u03a1\u0003\u0084B\u0012\u036f\u0370\n\u0010\u0000\u0000\u0370\u0371"+
		"\u0005-\u0000\u0000\u0371\u03a1\u0003\u0084B\u0011\u0372\u0373\n\u000f"+
		"\u0000\u0000\u0373\u0374\u0005.\u0000\u0000\u0374\u03a1\u0003\u0084B\u0010"+
		"\u0375\u0376\n\u000e\u0000\u0000\u0376\u0377\u0005/\u0000\u0000\u0377"+
		"\u03a1\u0003\u0084B\u000f\u0378\u0379\n\r\u0000\u0000\u0379\u037a\u0005"+
		"\u000f\u0000\u0000\u037a\u037b\u0003\u0084B\u0000\u037b\u037c\u0005\u0011"+
		"\u0000\u0000\u037c\u037d\u0003\u0084B\u000e\u037d\u03a1\u0001\u0000\u0000"+
		"\u0000\u037e\u037f\n\f\u0000\u0000\u037f\u0380\u0005\u000e\u0000\u0000"+
		"\u0380\u03a1\u0003\u0084B\f\u0381\u0382\n\u000b\u0000\u0000\u0382\u0383"+
		"\u0003\u0092I\u0000\u0383\u0384\u0003\u0084B\u000b\u0384\u03a1\u0001\u0000"+
		"\u0000\u0000\u0385\u0387\n.\u0000\u0000\u0386\u0388\u0005\u0010\u0000"+
		"\u0000\u0387\u0386\u0001\u0000\u0000\u0000\u0387\u0388\u0001\u0000\u0000"+
		"\u0000\u0388\u0389\u0001\u0000\u0000\u0000\u0389\u038a\u0005\u0005\u0000"+
		"\u0000\u038a\u038b\u0003\u0080@\u0000\u038b\u038c\u0005\u0006\u0000\u0000"+
		"\u038c\u03a1\u0001\u0000\u0000\u0000\u038d\u038f\n-\u0000\u0000\u038e"+
		"\u0390\u0005\u000f\u0000\u0000\u038f\u038e\u0001\u0000\u0000\u0000\u038f"+
		"\u0390\u0001\u0000\u0000\u0000\u0390\u0391\u0001\u0000\u0000\u0000\u0391"+
		"\u0393\u0005\u0013\u0000\u0000\u0392\u0394\u0005\u001f\u0000\u0000\u0393"+
		"\u0392\u0001\u0000\u0000\u0000\u0393\u0394\u0001\u0000\u0000\u0000\u0394"+
		"\u0395\u0001\u0000\u0000\u0000\u0395\u03a1\u0003\u00a2Q\u0000\u0396\u0397"+
		"\n)\u0000\u0000\u0397\u03a1\u0003|>\u0000\u0398\u0399\n\'\u0000\u0000"+
		"\u0399\u039a\u0004B \u0000\u039a\u03a1\u0005\u0014\u0000\u0000\u039b\u039c"+
		"\n&\u0000\u0000\u039c\u039d\u0004B\"\u0000\u039d\u03a1\u0005\u0015\u0000"+
		"\u0000\u039e\u039f\n\t\u0000\u0000\u039f\u03a1\u0003\u0096K\u0000\u03a0"+
		"\u034b\u0001\u0000\u0000\u0000\u03a0\u034e\u0001\u0000\u0000\u0000\u03a0"+
		"\u0351\u0001\u0000\u0000\u0000\u03a0\u0354\u0001\u0000\u0000\u0000\u03a0"+
		"\u0357\u0001\u0000\u0000\u0000\u03a0\u035a\u0001\u0000\u0000\u0000\u03a0"+
		"\u035d\u0001\u0000\u0000\u0000\u03a0\u0360\u0001\u0000\u0000\u0000\u03a0"+
		"\u0363\u0001\u0000\u0000\u0000\u03a0\u0366\u0001\u0000\u0000\u0000\u03a0"+
		"\u0369\u0001\u0000\u0000\u0000\u03a0\u036c\u0001\u0000\u0000\u0000\u03a0"+
		"\u036f\u0001\u0000\u0000\u0000\u03a0\u0372\u0001\u0000\u0000\u0000\u03a0"+
		"\u0375\u0001\u0000\u0000\u0000\u03a0\u0378\u0001\u0000\u0000\u0000\u03a0"+
		"\u037e\u0001\u0000\u0000\u0000\u03a0\u0381\u0001\u0000\u0000\u0000\u03a0"+
		"\u0385\u0001\u0000\u0000\u0000\u03a0\u038d\u0001\u0000\u0000\u0000\u03a0"+
		"\u0396\u0001\u0000\u0000\u0000\u03a0\u0398\u0001\u0000\u0000\u0000\u03a0"+
		"\u039b\u0001\u0000\u0000\u0000\u03a0\u039e\u0001\u0000\u0000\u0000\u03a1"+
		"\u03a4\u0001\u0000\u0000\u0000\u03a2\u03a0\u0001\u0000\u0000\u0000\u03a2"+
		"\u03a3\u0001\u0000\u0000\u0000\u03a3\u0085\u0001\u0000\u0000\u0000\u03a4"+
		"\u03a2\u0001\u0000\u0000\u0000\u03a5\u03a6\u0005\u000e\u0000\u0000\u03a6"+
		"\u03a7\u0003\u0082A\u0000\u03a7\u0087\u0001\u0000\u0000\u0000\u03a8\u03ad"+
		"\u0003\u00a4R\u0000\u03a9\u03ad\u0003\u00a8T\u0000\u03aa\u03ad\u0003r"+
		"9\u0000\u03ab\u03ad\u0003\u008aE\u0000\u03ac\u03a8\u0001\u0000\u0000\u0000"+
		"\u03ac\u03a9\u0001\u0000\u0000\u0000\u03ac\u03aa\u0001\u0000\u0000\u0000"+
		"\u03ac\u03ab\u0001\u0000\u0000\u0000\u03ad\u0089\u0001\u0000\u0000\u0000"+
		"\u03ae\u03ba\u0005\t\u0000\u0000\u03af\u03b4\u0003x<\u0000\u03b0\u03b1"+
		"\u0005\r\u0000\u0000\u03b1\u03b3\u0003x<\u0000\u03b2\u03b0\u0001\u0000"+
		"\u0000\u0000\u03b3\u03b6\u0001\u0000\u0000\u0000\u03b4\u03b2\u0001\u0000"+
		"\u0000\u0000\u03b4\u03b5\u0001\u0000\u0000\u0000\u03b5\u03b8\u0001\u0000"+
		"\u0000\u0000\u03b6\u03b4\u0001\u0000\u0000\u0000\u03b7\u03b9\u0005\r\u0000"+
		"\u0000\u03b8\u03b7\u0001\u0000\u0000\u0000\u03b8\u03b9\u0001\u0000\u0000"+
		"\u0000\u03b9\u03bb\u0001\u0000\u0000\u0000\u03ba\u03af\u0001\u0000\u0000"+
		"\u0000\u03ba\u03bb\u0001\u0000\u0000\u0000\u03bb\u03bc\u0001\u0000\u0000"+
		"\u0000\u03bc\u03bd\u0005\u000b\u0000\u0000\u03bd\u008b\u0001\u0000\u0000"+
		"\u0000\u03be\u03d4\u0003X,\u0000\u03bf\u03c1\u0005o\u0000\u0000\u03c0"+
		"\u03bf\u0001\u0000\u0000\u0000\u03c0\u03c1\u0001\u0000\u0000\u0000\u03c1"+
		"\u03c2\u0001\u0000\u0000\u0000\u03c2\u03c4\u0005Z\u0000\u0000\u03c3\u03c5"+
		"\u0005\u001a\u0000\u0000\u03c4\u03c3\u0001\u0000\u0000\u0000\u03c4\u03c5"+
		"\u0001\u0000\u0000\u0000\u03c5\u03c6\u0001\u0000\u0000\u0000\u03c6\u03c8"+
		"\u0005\u0007\u0000\u0000\u03c7\u03c9\u0003h4\u0000\u03c8\u03c7\u0001\u0000"+
		"\u0000\u0000\u03c8\u03c9\u0001\u0000\u0000\u0000\u03c9\u03ca\u0001\u0000"+
		"\u0000\u0000\u03ca\u03cb\u0005\b\u0000\u0000\u03cb\u03d4\u0003n7\u0000"+
		"\u03cc\u03ce\u0005o\u0000\u0000\u03cd\u03cc\u0001\u0000\u0000\u0000\u03cd"+
		"\u03ce\u0001\u0000\u0000\u0000\u03ce\u03cf\u0001\u0000\u0000\u0000\u03cf"+
		"\u03d0\u0003\u008eG\u0000\u03d0\u03d1\u0005=\u0000\u0000\u03d1\u03d2\u0003"+
		"\u0090H\u0000\u03d2\u03d4\u0001\u0000\u0000\u0000\u03d3\u03be\u0001\u0000"+
		"\u0000\u0000\u03d3\u03c0\u0001\u0000\u0000\u0000\u03d3\u03cd\u0001\u0000"+
		"\u0000\u0000\u03d4\u008d\u0001\u0000\u0000\u0000\u03d5\u03dc\u0003z=\u0000"+
		"\u03d6\u03d8\u0005\u0007\u0000\u0000\u03d7\u03d9\u0003h4\u0000\u03d8\u03d7"+
		"\u0001\u0000\u0000\u0000\u03d8\u03d9\u0001\u0000\u0000\u0000\u03d9\u03da"+
		"\u0001\u0000\u0000\u0000\u03da\u03dc\u0005\b\u0000\u0000\u03db\u03d5\u0001"+
		"\u0000\u0000\u0000\u03db\u03d6\u0001\u0000\u0000\u0000\u03dc\u008f\u0001"+
		"\u0000\u0000\u0000\u03dd\u03e0\u0003\u0082A\u0000\u03de\u03e0\u0003n7"+
		"\u0000\u03df\u03dd\u0001\u0000\u0000\u0000\u03df\u03de\u0001\u0000\u0000"+
		"\u0000\u03e0\u0091\u0001\u0000\u0000\u0000\u03e1\u03e2\u0007\u0007\u0000"+
		"\u0000\u03e2\u0093\u0001\u0000\u0000\u0000\u03e3\u03eb\u0005>\u0000\u0000"+
		"\u03e4\u03eb\u0005?\u0000\u0000\u03e5\u03eb\u0005{\u0000\u0000\u03e6\u03eb"+
		"\u0003\u0096K\u0000\u03e7\u03eb\u0005\u0004\u0000\u0000\u03e8\u03eb\u0003"+
		"\u009aM\u0000\u03e9\u03eb\u0003\u009cN\u0000\u03ea\u03e3\u0001\u0000\u0000"+
		"\u0000\u03ea\u03e4\u0001\u0000\u0000\u0000\u03ea\u03e5\u0001\u0000\u0000"+
		"\u0000\u03ea\u03e6\u0001\u0000\u0000\u0000\u03ea\u03e7\u0001\u0000\u0000"+
		"\u0000\u03ea\u03e8\u0001\u0000\u0000\u0000\u03ea\u03e9\u0001\u0000\u0000"+
		"\u0000\u03eb\u0095\u0001\u0000\u0000\u0000\u03ec\u03f0\u0005|\u0000\u0000"+
		"\u03ed\u03ef\u0003\u0098L\u0000\u03ee\u03ed\u0001\u0000\u0000\u0000\u03ef"+
		"\u03f2\u0001\u0000\u0000\u0000\u03f0\u03ee\u0001\u0000\u0000\u0000\u03f0"+
		"\u03f1\u0001\u0000\u0000\u0000\u03f1\u03f3\u0001\u0000\u0000\u0000\u03f2"+
		"\u03f0\u0001\u0000\u0000\u0000\u03f3\u03f4\u0005|\u0000\u0000\u03f4\u0097"+
		"\u0001\u0000\u0000\u0000\u03f5\u03fb\u0005\u0083\u0000\u0000\u03f6\u03f7"+
		"\u0005\u0082\u0000\u0000\u03f7\u03f8\u0003\u0082A\u0000\u03f8\u03f9\u0005"+
		"\n\u0000\u0000\u03f9\u03fb\u0001\u0000\u0000\u0000\u03fa\u03f5\u0001\u0000"+
		"\u0000\u0000\u03fa\u03f6\u0001\u0000\u0000\u0000\u03fb\u0099\u0001\u0000"+
		"\u0000\u0000\u03fc\u03fd\u0007\b\u0000\u0000\u03fd\u009b\u0001\u0000\u0000"+
		"\u0000\u03fe\u03ff\u0007\t\u0000\u0000\u03ff\u009d\u0001\u0000\u0000\u0000"+
		"\u0400\u0401\u0004O$\u0000\u0401\u0402\u0003\u00a4R\u0000\u0402\u0403"+
		"\u0003d2\u0000\u0403\u009f\u0001\u0000\u0000\u0000\u0404\u0405\u0004P"+
		"%\u0000\u0405\u0406\u0003\u00a4R\u0000\u0406\u0407\u0003d2\u0000\u0407"+
		"\u00a1\u0001\u0000\u0000\u0000\u0408\u040b\u0003\u00a4R\u0000\u0409\u040b"+
		"\u0003\u00a6S\u0000\u040a\u0408\u0001\u0000\u0000\u0000\u040a\u0409\u0001"+
		"\u0000\u0000\u0000\u040b\u00a3\u0001\u0000\u0000\u0000\u040c\u040d\u0007"+
		"\n\u0000\u0000\u040d\u00a5\u0001\u0000\u0000\u0000\u040e\u0412\u0003\u00a8"+
		"T\u0000\u040f\u0412\u0005>\u0000\u0000\u0410\u0412\u0005?\u0000\u0000"+
		"\u0411\u040e\u0001\u0000\u0000\u0000\u0411\u040f\u0001\u0000\u0000\u0000"+
		"\u0411\u0410\u0001\u0000\u0000\u0000\u0412\u00a7\u0001\u0000\u0000\u0000"+
		"\u0413\u0444\u0005I\u0000\u0000\u0414\u0444\u0005J\u0000\u0000\u0415\u0444"+
		"\u0005K\u0000\u0000\u0416\u0444\u0005L\u0000\u0000\u0417\u0444\u0005M"+
		"\u0000\u0000\u0418\u0444\u0005N\u0000\u0000\u0419\u0444\u0005O\u0000\u0000"+
		"\u041a\u0444\u0005P\u0000\u0000\u041b\u0444\u0005Q\u0000\u0000\u041c\u0444"+
		"\u0005R\u0000\u0000\u041d\u0444\u0005S\u0000\u0000\u041e\u0444\u0005T"+
		"\u0000\u0000\u041f\u0444\u0005U\u0000\u0000\u0420\u0444\u0005V\u0000\u0000"+
		"\u0421\u0444\u0005W\u0000\u0000\u0422\u0444\u0005X\u0000\u0000\u0423\u0444"+
		"\u0005Y\u0000\u0000\u0424\u0444\u0005Z\u0000\u0000\u0425\u0444\u0005["+
		"\u0000\u0000\u0426\u0444\u0005\\\u0000\u0000\u0427\u0444\u0005]\u0000"+
		"\u0000\u0428\u0444\u0005^\u0000\u0000\u0429\u0444\u0005_\u0000\u0000\u042a"+
		"\u0444\u0005`\u0000\u0000\u042b\u0444\u0005a\u0000\u0000\u042c\u0444\u0005"+
		"b\u0000\u0000\u042d\u0444\u0005h\u0000\u0000\u042e\u0444\u0005i\u0000"+
		"\u0000\u042f\u0444\u0005j\u0000\u0000\u0430\u0444\u0005k\u0000\u0000\u0431"+
		"\u0444\u0005l\u0000\u0000\u0432\u0444\u0005m\u0000\u0000\u0433\u0444\u0005"+
		"n\u0000\u0000\u0434\u0444\u0005q\u0000\u0000\u0435\u0444\u0003\u00aaU"+
		"\u0000\u0436\u0444\u0005t\u0000\u0000\u0437\u0444\u0005u\u0000\u0000\u0438"+
		"\u0444\u0005v\u0000\u0000\u0439\u0444\u0005w\u0000\u0000\u043a\u0444\u0005"+
		"x\u0000\u0000\u043b\u0444\u0005y\u0000\u0000\u043c\u0444\u0005f\u0000"+
		"\u0000\u043d\u0444\u0005g\u0000\u0000\u043e\u0444\u0005o\u0000\u0000\u043f"+
		"\u0444\u0005p\u0000\u0000\u0440\u0444\u0005d\u0000\u0000\u0441\u0444\u0005"+
		"c\u0000\u0000\u0442\u0444\u0005e\u0000\u0000\u0443\u0413\u0001\u0000\u0000"+
		"\u0000\u0443\u0414\u0001\u0000\u0000\u0000\u0443\u0415\u0001\u0000\u0000"+
		"\u0000\u0443\u0416\u0001\u0000\u0000\u0000\u0443\u0417\u0001\u0000\u0000"+
		"\u0000\u0443\u0418\u0001\u0000\u0000\u0000\u0443\u0419\u0001\u0000\u0000"+
		"\u0000\u0443\u041a\u0001\u0000\u0000\u0000\u0443\u041b\u0001\u0000\u0000"+
		"\u0000\u0443\u041c\u0001\u0000\u0000\u0000\u0443\u041d\u0001\u0000\u0000"+
		"\u0000\u0443\u041e\u0001\u0000\u0000\u0000\u0443\u041f\u0001\u0000\u0000"+
		"\u0000\u0443\u0420\u0001\u0000\u0000\u0000\u0443\u0421\u0001\u0000\u0000"+
		"\u0000\u0443\u0422\u0001\u0000\u0000\u0000\u0443\u0423\u0001\u0000\u0000"+
		"\u0000\u0443\u0424\u0001\u0000\u0000\u0000\u0443\u0425\u0001\u0000\u0000"+
		"\u0000\u0443\u0426\u0001\u0000\u0000\u0000\u0443\u0427\u0001\u0000\u0000"+
		"\u0000\u0443\u0428\u0001\u0000\u0000\u0000\u0443\u0429\u0001\u0000\u0000"+
		"\u0000\u0443\u042a\u0001\u0000\u0000\u0000\u0443\u042b\u0001\u0000\u0000"+
		"\u0000\u0443\u042c\u0001\u0000\u0000\u0000\u0443\u042d\u0001\u0000\u0000"+
		"\u0000\u0443\u042e\u0001\u0000\u0000\u0000\u0443\u042f\u0001\u0000\u0000"+
		"\u0000\u0443\u0430\u0001\u0000\u0000\u0000\u0443\u0431\u0001\u0000\u0000"+
		"\u0000\u0443\u0432\u0001\u0000\u0000\u0000\u0443\u0433\u0001\u0000\u0000"+
		"\u0000\u0443\u0434\u0001\u0000\u0000\u0000\u0443\u0435\u0001\u0000\u0000"+
		"\u0000\u0443\u0436\u0001\u0000\u0000\u0000\u0443\u0437\u0001\u0000\u0000"+
		"\u0000\u0443\u0438\u0001\u0000\u0000\u0000\u0443\u0439\u0001\u0000\u0000"+
		"\u0000\u0443\u043a\u0001\u0000\u0000\u0000\u0443\u043b\u0001\u0000\u0000"+
		"\u0000\u0443\u043c\u0001\u0000\u0000\u0000\u0443\u043d\u0001\u0000\u0000"+
		"\u0000\u0443\u043e\u0001\u0000\u0000\u0000\u0443\u043f\u0001\u0000\u0000"+
		"\u0000\u0443\u0440\u0001\u0000\u0000\u0000\u0443\u0441\u0001\u0000\u0000"+
		"\u0000\u0443\u0442\u0001\u0000\u0000\u0000\u0444\u00a9\u0001\u0000\u0000"+
		"\u0000\u0445\u0446\u0007\u000b\u0000\u0000\u0446\u00ab\u0001\u0000\u0000"+
		"\u0000\u0447\u044c\u0005\f\u0000\u0000\u0448\u044c\u0005\u0000\u0000\u0001"+
		"\u0449\u044c\u0004V&\u0000\u044a\u044c\u0004V\'\u0000\u044b\u0447\u0001"+
		"\u0000\u0000\u0000\u044b\u0448\u0001\u0000\u0000\u0000\u044b\u0449\u0001"+
		"\u0000\u0000\u0000\u044b\u044a\u0001\u0000\u0000\u0000\u044c\u00ad\u0001"+
		"\u0000\u0000\u0000w\u00af\u00b2\u00cc\u00d0\u00d7\u00dd\u00e1\u00e8\u00f0"+
		"\u00f5\u00f7\u00fe\u0102\u010b\u010f\u0117\u011b\u011f\u0128\u0130\u0134"+
		"\u013c\u0141\u0143\u014a\u014f\u015a\u0160\u016f\u0183\u0187\u018b\u0193"+
		"\u019c\u01a1\u01a8\u01ad\u01b2\u01b9\u01c0\u01c7\u01d9\u01dd\u01df\u01e6"+
		"\u01ec\u01f1\u0200\u0203\u0208\u020b\u0216\u021a\u021f\u022a\u0230\u0238"+
		"\u023e\u0244\u0248\u024c\u024f\u0254\u025a\u0262\u0267\u026c\u0270\u0274"+
		"\u027e\u0283\u0286\u028b\u0292\u0299\u02a2\u02a6\u02ab\u02b0\u02b6\u02ba"+
		"\u02c9\u02cc\u02d1\u02e2\u02e5\u02ee\u02f6\u02fa\u02fc\u0301\u0305\u030c"+
		"\u0315\u0349\u0387\u038f\u0393\u03a0\u03a2\u03ac\u03b4\u03b8\u03ba\u03c0"+
		"\u03c4\u03c8\u03cd\u03d3\u03d8\u03db\u03df\u03ea\u03f0\u03fa\u040a\u0411"+
		"\u0443\u044b";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}