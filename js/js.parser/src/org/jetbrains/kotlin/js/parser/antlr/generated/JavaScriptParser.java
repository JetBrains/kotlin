// Generated from JavaScriptParser.g4 by ANTLR 4.13.2
package org.jetbrains.kotlin.js.parser.antlr.generated;

import org.jetbrains.kotlin.js.parser.antlr.JavaScriptParserBase;
import org.jetbrains.kotlin.js.parser.antlr.JavaScriptRuleContext;

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
		Package=119, Protected=120, Static=121, Meta=122, Target=123, Identifier=124, 
		StringLiteral=125, BackTick=126, WhiteSpaces=127, LineTerminator=128, 
		HtmlComment=129, CDataComment=130, UnexpectedCharacter=131, TemplateStringStartExpression=132, 
		TemplateStringAtom=133;
	public static final int
		RULE_program = 0, RULE_sourceElement = 1, RULE_optionalStatements = 2, 
		RULE_optionalSingleExpression = 3, RULE_optionalExpressionOrStatement = 4, 
		RULE_optionalFunction = 5, RULE_statement = 6, RULE_block = 7, RULE_statementList = 8, 
		RULE_importStatement = 9, RULE_importFromBlock = 10, RULE_importModuleItems = 11, 
		RULE_importAliasName = 12, RULE_moduleExportName = 13, RULE_importedBinding = 14, 
		RULE_importDefault = 15, RULE_importNamespace = 16, RULE_importFrom = 17, 
		RULE_aliasName = 18, RULE_exportStatement = 19, RULE_exportFromBlock = 20, 
		RULE_exportModuleItems = 21, RULE_exportAliasName = 22, RULE_declaration = 23, 
		RULE_variableStatement = 24, RULE_variableDeclarationList = 25, RULE_singleVariableDeclaration = 26, 
		RULE_variableDeclaration = 27, RULE_emptyStatement_ = 28, RULE_expressionStatement = 29, 
		RULE_ifStatement = 30, RULE_iterationStatement = 31, RULE_varModifier = 32, 
		RULE_continueStatement = 33, RULE_breakStatement = 34, RULE_returnStatement = 35, 
		RULE_yieldStatement = 36, RULE_withStatement = 37, RULE_switchStatement = 38, 
		RULE_caseBlock = 39, RULE_caseClauses = 40, RULE_caseClause = 41, RULE_defaultClause = 42, 
		RULE_labelledStatement = 43, RULE_throwStatement = 44, RULE_tryStatement = 45, 
		RULE_catchProduction = 46, RULE_finallyProduction = 47, RULE_debuggerStatement = 48, 
		RULE_functionDeclaration = 49, RULE_classDeclaration = 50, RULE_classTail = 51, 
		RULE_classElement = 52, RULE_methodDefinition = 53, RULE_fieldDefinition = 54, 
		RULE_classElementName = 55, RULE_privateIdentifier = 56, RULE_formalParameterList = 57, 
		RULE_formalParameterArg = 58, RULE_restParameterArg = 59, RULE_functionBody = 60, 
		RULE_sourceElements = 61, RULE_arrayLiteral = 62, RULE_elementList = 63, 
		RULE_arrayElement = 64, RULE_propertyAssignment = 65, RULE_objectBindingPattern = 66, 
		RULE_propertyBindingPattern = 67, RULE_arrayBindingPattern = 68, RULE_arrayItemList = 69, 
		RULE_arrayItemBinding = 70, RULE_bindingElement = 71, RULE_restBindingElement = 72, 
		RULE_propertyName = 73, RULE_arguments = 74, RULE_argument = 75, RULE_expressionSequence = 76, 
		RULE_singleExpression = 77, RULE_singleExpressionImpl = 78, RULE_initializer = 79, 
		RULE_assignable = 80, RULE_objectLiteral = 81, RULE_anonymousFunction = 82, 
		RULE_arrowFunctionParameters = 83, RULE_arrowFunctionBody = 84, RULE_assignmentOperator = 85, 
		RULE_literal = 86, RULE_templateStringLiteral = 87, RULE_templateStringAtom = 88, 
		RULE_numericLiteral = 89, RULE_bigintLiteral = 90, RULE_getter = 91, RULE_setter = 92, 
		RULE_identifierName = 93, RULE_identifier = 94, RULE_reservedWord = 95, 
		RULE_keyword = 96, RULE_let_ = 97, RULE_eos = 98;
	private static String[] makeRuleNames() {
		return new String[] {
			"program", "sourceElement", "optionalStatements", "optionalSingleExpression", 
			"optionalExpressionOrStatement", "optionalFunction", "statement", "block", 
			"statementList", "importStatement", "importFromBlock", "importModuleItems", 
			"importAliasName", "moduleExportName", "importedBinding", "importDefault", 
			"importNamespace", "importFrom", "aliasName", "exportStatement", "exportFromBlock", 
			"exportModuleItems", "exportAliasName", "declaration", "variableStatement", 
			"variableDeclarationList", "singleVariableDeclaration", "variableDeclaration", 
			"emptyStatement_", "expressionStatement", "ifStatement", "iterationStatement", 
			"varModifier", "continueStatement", "breakStatement", "returnStatement", 
			"yieldStatement", "withStatement", "switchStatement", "caseBlock", "caseClauses", 
			"caseClause", "defaultClause", "labelledStatement", "throwStatement", 
			"tryStatement", "catchProduction", "finallyProduction", "debuggerStatement", 
			"functionDeclaration", "classDeclaration", "classTail", "classElement", 
			"methodDefinition", "fieldDefinition", "classElementName", "privateIdentifier", 
			"formalParameterList", "formalParameterArg", "restParameterArg", "functionBody", 
			"sourceElements", "arrayLiteral", "elementList", "arrayElement", "propertyAssignment", 
			"objectBindingPattern", "propertyBindingPattern", "arrayBindingPattern", 
			"arrayItemList", "arrayItemBinding", "bindingElement", "restBindingElement", 
			"propertyName", "arguments", "argument", "expressionSequence", "singleExpression", 
			"singleExpressionImpl", "initializer", "assignable", "objectLiteral", 
			"anonymousFunction", "arrowFunctionParameters", "arrowFunctionBody", 
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
			"'interface'", "'package'", "'protected'", "'static'", "'meta'", "'target'"
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
			"Static", "Meta", "Target", "Identifier", "StringLiteral", "BackTick", 
			"WhiteSpaces", "LineTerminator", "HtmlComment", "CDataComment", "UnexpectedCharacter", 
			"TemplateStringStartExpression", "TemplateStringAtom"
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
	public static class ProgramContext extends JavaScriptRuleContext {
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
			setState(199);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				{
				setState(198);
				match(HashBangLine);
				}
				break;
			}
			setState(202);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				{
				setState(201);
				sourceElements();
				}
				break;
			}
			setState(204);
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
	public static class SourceElementContext extends JavaScriptRuleContext {
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
			setState(206);
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
	public static class OptionalStatementsContext extends JavaScriptRuleContext {
		public TerminalNode EOF() { return getToken(JavaScriptParser.EOF, 0); }
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public OptionalStatementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optionalStatements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterOptionalStatements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitOptionalStatements(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitOptionalStatements(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionalStatementsContext optionalStatements() throws RecognitionException {
		OptionalStatementsContext _localctx = new OptionalStatementsContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_optionalStatements);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(211);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(208);
					statement();
					}
					} 
				}
				setState(213);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			}
			setState(214);
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
	public static class OptionalSingleExpressionContext extends JavaScriptRuleContext {
		public TerminalNode EOF() { return getToken(JavaScriptParser.EOF, 0); }
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public OptionalSingleExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optionalSingleExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterOptionalSingleExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitOptionalSingleExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitOptionalSingleExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionalSingleExpressionContext optionalSingleExpression() throws RecognitionException {
		OptionalSingleExpressionContext _localctx = new OptionalSingleExpressionContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_optionalSingleExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(217);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -4611686018361326928L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 9223372036854775807L) != 0)) {
				{
				setState(216);
				singleExpression();
				}
			}

			setState(219);
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
	public static class OptionalExpressionOrStatementContext extends JavaScriptRuleContext {
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public TerminalNode EOF() { return getToken(JavaScriptParser.EOF, 0); }
		public StatementListContext statementList() {
			return getRuleContext(StatementListContext.class,0);
		}
		public OptionalExpressionOrStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optionalExpressionOrStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterOptionalExpressionOrStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitOptionalExpressionOrStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitOptionalExpressionOrStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionalExpressionOrStatementContext optionalExpressionOrStatement() throws RecognitionException {
		OptionalExpressionOrStatementContext _localctx = new OptionalExpressionOrStatementContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_optionalExpressionOrStatement);
		try {
			setState(228);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(221);
				singleExpression();
				setState(222);
				match(EOF);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(225);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
				case 1:
					{
					setState(224);
					statementList();
					}
					break;
				}
				setState(227);
				match(EOF);
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
	public static class OptionalFunctionContext extends JavaScriptRuleContext {
		public TerminalNode EOF() { return getToken(JavaScriptParser.EOF, 0); }
		public FunctionDeclarationContext functionDeclaration() {
			return getRuleContext(FunctionDeclarationContext.class,0);
		}
		public OptionalFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optionalFunction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterOptionalFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitOptionalFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitOptionalFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionalFunctionContext optionalFunction() throws RecognitionException {
		OptionalFunctionContext _localctx = new OptionalFunctionContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_optionalFunction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(231);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Function_ || _la==Async) {
				{
				setState(230);
				functionDeclaration();
				}
			}

			setState(233);
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
	public static class StatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 12, RULE_statement);
		try {
			setState(255);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(235);
				block();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(236);
				variableStatement();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(237);
				importStatement();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(238);
				exportStatement();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(239);
				emptyStatement_();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(240);
				classDeclaration();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(241);
				functionDeclaration();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(242);
				expressionStatement();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(243);
				ifStatement();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(244);
				iterationStatement();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(245);
				continueStatement();
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(246);
				breakStatement();
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(247);
				returnStatement();
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(248);
				yieldStatement();
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(249);
				withStatement();
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(250);
				labelledStatement();
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(251);
				switchStatement();
				}
				break;
			case 18:
				enterOuterAlt(_localctx, 18);
				{
				setState(252);
				throwStatement();
				}
				break;
			case 19:
				enterOuterAlt(_localctx, 19);
				{
				setState(253);
				tryStatement();
				}
				break;
			case 20:
				enterOuterAlt(_localctx, 20);
				{
				setState(254);
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
	public static class BlockContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 14, RULE_block);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(257);
			match(OpenBrace);
			setState(259);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				setState(258);
				statementList();
				}
				break;
			}
			setState(261);
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
	public static class StatementListContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 16, RULE_statementList);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(264); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(263);
					statement();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(266); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
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
	public static class ImportStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 18, RULE_importStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(268);
			match(Import);
			setState(269);
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
	public static class ImportFromBlockContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 20, RULE_importFromBlock);
		try {
			setState(283);
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
			case Meta:
			case Target:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(272);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
				case 1:
					{
					setState(271);
					importDefault();
					}
					break;
				}
				setState(276);
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
				case Meta:
				case Target:
				case Identifier:
					{
					setState(274);
					importNamespace();
					}
					break;
				case OpenBrace:
					{
					setState(275);
					importModuleItems();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(278);
				importFrom();
				setState(279);
				eos();
				}
				break;
			case StringLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(281);
				match(StringLiteral);
				setState(282);
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
	public static class ImportModuleItemsContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 22, RULE_importModuleItems);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(285);
			match(OpenBrace);
			setState(291);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,13,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(286);
					importAliasName();
					setState(287);
					match(Comma);
					}
					} 
				}
				setState(293);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,13,_ctx);
			}
			setState(298);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 62)) & ~0x3f) == 0 && ((1L << (_la - 62)) & -2045L) != 0)) {
				{
				setState(294);
				importAliasName();
				setState(296);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(295);
					match(Comma);
					}
				}

				}
			}

			setState(300);
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
	public static class ImportAliasNameContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 24, RULE_importAliasName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(302);
			moduleExportName();
			setState(305);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==As) {
				{
				setState(303);
				match(As);
				setState(304);
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
	public static class ModuleExportNameContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 26, RULE_moduleExportName);
		try {
			setState(309);
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
			case Meta:
			case Target:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(307);
				identifierName();
				}
				break;
			case StringLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(308);
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
	public static class ImportedBindingContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 28, RULE_importedBinding);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(311);
			_la = _input.LA(1);
			if ( !(((((_la - 102)) & ~0x3f) == 0 && ((1L << (_la - 102)) & 4195329L) != 0)) ) {
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
	public static class ImportDefaultContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 30, RULE_importDefault);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(313);
			aliasName();
			setState(314);
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
	public static class ImportNamespaceContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 32, RULE_importNamespace);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(318);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Multiply:
				{
				setState(316);
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
			case Meta:
			case Target:
			case Identifier:
				{
				setState(317);
				identifierName();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(322);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==As) {
				{
				setState(320);
				match(As);
				setState(321);
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
	public static class ImportFromContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 34, RULE_importFrom);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(324);
			match(From);
			setState(325);
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
	public static class AliasNameContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 36, RULE_aliasName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(327);
			identifierName();
			setState(330);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==As) {
				{
				setState(328);
				match(As);
				setState(329);
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
	public static class ExportStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 38, RULE_exportStatement);
		try {
			setState(347);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				_localctx = new ExportDeclarationContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(332);
				match(Export);
				setState(334);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
				case 1:
					{
					setState(333);
					match(Default);
					}
					break;
				}
				setState(338);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
				case 1:
					{
					setState(336);
					exportFromBlock();
					}
					break;
				case 2:
					{
					setState(337);
					declaration();
					}
					break;
				}
				setState(340);
				eos();
				}
				break;
			case 2:
				_localctx = new ExportDefaultDeclarationContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(342);
				match(Export);
				setState(343);
				match(Default);
				setState(344);
				singleExpression();
				setState(345);
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
	public static class ExportFromBlockContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 40, RULE_exportFromBlock);
		try {
			setState(359);
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
			case Meta:
			case Target:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(349);
				importNamespace();
				setState(350);
				importFrom();
				setState(351);
				eos();
				}
				break;
			case OpenBrace:
				enterOuterAlt(_localctx, 2);
				{
				setState(353);
				exportModuleItems();
				setState(355);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
				case 1:
					{
					setState(354);
					importFrom();
					}
					break;
				}
				setState(357);
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
	public static class ExportModuleItemsContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 42, RULE_exportModuleItems);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(361);
			match(OpenBrace);
			setState(367);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(362);
					exportAliasName();
					setState(363);
					match(Comma);
					}
					} 
				}
				setState(369);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			}
			setState(374);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 62)) & ~0x3f) == 0 && ((1L << (_la - 62)) & -2045L) != 0)) {
				{
				setState(370);
				exportAliasName();
				setState(372);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(371);
					match(Comma);
					}
				}

				}
			}

			setState(376);
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
	public static class ExportAliasNameContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 44, RULE_exportAliasName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(378);
			moduleExportName();
			setState(381);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==As) {
				{
				setState(379);
				match(As);
				setState(380);
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
	public static class DeclarationContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 46, RULE_declaration);
		try {
			setState(386);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Var:
			case Const:
			case StrictLet:
			case NonStrictLet:
				enterOuterAlt(_localctx, 1);
				{
				setState(383);
				variableStatement();
				}
				break;
			case Class:
				enterOuterAlt(_localctx, 2);
				{
				setState(384);
				classDeclaration();
				}
				break;
			case Function_:
			case Async:
				enterOuterAlt(_localctx, 3);
				{
				setState(385);
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
	public static class VariableStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 48, RULE_variableStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(388);
			variableDeclarationList();
			setState(389);
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
	public static class VariableDeclarationListContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 50, RULE_variableDeclarationList);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(391);
			varModifier();
			setState(392);
			variableDeclaration();
			setState(397);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(393);
					match(Comma);
					setState(394);
					variableDeclaration();
					}
					} 
				}
				setState(399);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
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
	public static class SingleVariableDeclarationContext extends JavaScriptRuleContext {
		public VarModifierContext varModifier() {
			return getRuleContext(VarModifierContext.class,0);
		}
		public VariableDeclarationContext variableDeclaration() {
			return getRuleContext(VariableDeclarationContext.class,0);
		}
		public SingleVariableDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleVariableDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterSingleVariableDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitSingleVariableDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitSingleVariableDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleVariableDeclarationContext singleVariableDeclaration() throws RecognitionException {
		SingleVariableDeclarationContext _localctx = new SingleVariableDeclarationContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_singleVariableDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(400);
			varModifier();
			setState(401);
			variableDeclaration();
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
	public static class VariableDeclarationContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 54, RULE_variableDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(403);
			assignable();
			setState(406);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
			case 1:
				{
				setState(404);
				match(Assign);
				setState(405);
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
	public static class EmptyStatement_Context extends JavaScriptRuleContext {
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
		enterRule(_localctx, 56, RULE_emptyStatement_);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(408);
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
	public static class ExpressionStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 58, RULE_expressionStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(410);
			if (!(this.notOpenBraceAndNotFunction())) throw new FailedPredicateException(this, "this.notOpenBraceAndNotFunction()");
			setState(411);
			expressionSequence(0);
			setState(412);
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
	public static class IfStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 60, RULE_ifStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(414);
			match(If);
			setState(415);
			match(OpenParen);
			setState(416);
			expressionSequence(0);
			setState(417);
			match(CloseParen);
			setState(418);
			statement();
			setState(421);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,33,_ctx) ) {
			case 1:
				{
				setState(419);
				match(Else);
				setState(420);
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
	public static class IterationStatementContext extends JavaScriptRuleContext {
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
		public ExpressionSequenceContext vars;
		public VariableDeclarationListContext var;
		public ExpressionSequenceContext condition;
		public ExpressionSequenceContext increment;
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
		public SingleVariableDeclarationContext singleVariableDeclaration() {
			return getRuleContext(SingleVariableDeclarationContext.class,0);
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
		public SingleVariableDeclarationContext singleVariableDeclaration() {
			return getRuleContext(SingleVariableDeclarationContext.class,0);
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
		enterRule(_localctx, 62, RULE_iterationStatement);
		int _la;
		try {
			setState(478);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,40,_ctx) ) {
			case 1:
				_localctx = new DoStatementContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(423);
				match(Do);
				setState(424);
				statement();
				setState(425);
				match(While);
				setState(426);
				match(OpenParen);
				setState(427);
				expressionSequence(0);
				setState(428);
				match(CloseParen);
				setState(429);
				eos();
				}
				break;
			case 2:
				_localctx = new WhileStatementContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(431);
				match(While);
				setState(432);
				match(OpenParen);
				setState(433);
				expressionSequence(0);
				setState(434);
				match(CloseParen);
				setState(435);
				statement();
				}
				break;
			case 3:
				_localctx = new ForStatementContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(437);
				match(For);
				setState(438);
				match(OpenParen);
				setState(441);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,34,_ctx) ) {
				case 1:
					{
					setState(439);
					((ForStatementContext)_localctx).vars = expressionSequence(0);
					}
					break;
				case 2:
					{
					setState(440);
					((ForStatementContext)_localctx).var = variableDeclarationList();
					}
					break;
				}
				setState(443);
				match(SemiColon);
				setState(445);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -4611686018361326928L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 9223372036854775807L) != 0)) {
					{
					setState(444);
					((ForStatementContext)_localctx).condition = expressionSequence(0);
					}
				}

				setState(447);
				match(SemiColon);
				setState(449);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -4611686018361326928L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 9223372036854775807L) != 0)) {
					{
					setState(448);
					((ForStatementContext)_localctx).increment = expressionSequence(0);
					}
				}

				setState(451);
				match(CloseParen);
				setState(452);
				statement();
				}
				break;
			case 4:
				_localctx = new ForInStatementContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(453);
				match(For);
				setState(454);
				match(OpenParen);
				setState(457);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,37,_ctx) ) {
				case 1:
					{
					setState(455);
					singleExpression();
					}
					break;
				case 2:
					{
					setState(456);
					singleVariableDeclaration();
					}
					break;
				}
				setState(459);
				match(In);
				setState(460);
				expressionSequence(0);
				setState(461);
				match(CloseParen);
				setState(462);
				statement();
				}
				break;
			case 5:
				_localctx = new ForOfStatementContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(464);
				match(For);
				setState(466);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Await) {
					{
					setState(465);
					match(Await);
					}
				}

				setState(468);
				match(OpenParen);
				setState(471);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,39,_ctx) ) {
				case 1:
					{
					setState(469);
					singleExpression();
					}
					break;
				case 2:
					{
					setState(470);
					singleVariableDeclaration();
					}
					break;
				}
				setState(473);
				match(Of);
				setState(474);
				expressionSequence(0);
				setState(475);
				match(CloseParen);
				setState(476);
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
	public static class VarModifierContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 64, RULE_varModifier);
		try {
			setState(483);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Var:
				enterOuterAlt(_localctx, 1);
				{
				setState(480);
				match(Var);
				}
				break;
			case StrictLet:
			case NonStrictLet:
				enterOuterAlt(_localctx, 2);
				{
				setState(481);
				let_();
				}
				break;
			case Const:
				enterOuterAlt(_localctx, 3);
				{
				setState(482);
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
	public static class ContinueStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 66, RULE_continueStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(485);
			match(Continue);
			setState(488);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,42,_ctx) ) {
			case 1:
				{
				setState(486);
				if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
				setState(487);
				identifier();
				}
				break;
			}
			setState(490);
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
	public static class BreakStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 68, RULE_breakStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(492);
			match(Break);
			setState(495);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,43,_ctx) ) {
			case 1:
				{
				setState(493);
				if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
				setState(494);
				identifier();
				}
				break;
			}
			setState(497);
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
	public static class ReturnStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 70, RULE_returnStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(499);
			match(Return);
			setState(502);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,44,_ctx) ) {
			case 1:
				{
				setState(500);
				if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
				setState(501);
				expressionSequence(0);
				}
				break;
			}
			setState(504);
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
	public static class YieldStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 72, RULE_yieldStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(506);
			_la = _input.LA(1);
			if ( !(_la==Yield || _la==YieldStar) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(509);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,45,_ctx) ) {
			case 1:
				{
				setState(507);
				if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
				setState(508);
				expressionSequence(0);
				}
				break;
			}
			setState(511);
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
	public static class WithStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 74, RULE_withStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(513);
			match(With);
			setState(514);
			match(OpenParen);
			setState(515);
			expressionSequence(0);
			setState(516);
			match(CloseParen);
			setState(517);
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
	public static class SwitchStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 76, RULE_switchStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(519);
			match(Switch);
			setState(520);
			match(OpenParen);
			setState(521);
			expressionSequence(0);
			setState(522);
			match(CloseParen);
			setState(523);
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
	public static class CaseBlockContext extends JavaScriptRuleContext {
		public CaseClausesContext beforeDefault;
		public CaseClausesContext afterDefault;
		public TerminalNode OpenBrace() { return getToken(JavaScriptParser.OpenBrace, 0); }
		public TerminalNode CloseBrace() { return getToken(JavaScriptParser.CloseBrace, 0); }
		public DefaultClauseContext defaultClause() {
			return getRuleContext(DefaultClauseContext.class,0);
		}
		public List<CaseClausesContext> caseClauses() {
			return getRuleContexts(CaseClausesContext.class);
		}
		public CaseClausesContext caseClauses(int i) {
			return getRuleContext(CaseClausesContext.class,i);
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
		enterRule(_localctx, 78, RULE_caseBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(525);
			match(OpenBrace);
			setState(527);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Case) {
				{
				setState(526);
				((CaseBlockContext)_localctx).beforeDefault = caseClauses();
				}
			}

			setState(533);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Default) {
				{
				setState(529);
				defaultClause();
				setState(531);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Case) {
					{
					setState(530);
					((CaseBlockContext)_localctx).afterDefault = caseClauses();
					}
				}

				}
			}

			setState(535);
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
	public static class CaseClausesContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 80, RULE_caseClauses);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(538); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(537);
				caseClause();
				}
				}
				setState(540); 
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
	public static class CaseClauseContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 82, RULE_caseClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(542);
			match(Case);
			setState(543);
			expressionSequence(0);
			setState(544);
			match(Colon);
			setState(546);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,50,_ctx) ) {
			case 1:
				{
				setState(545);
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
	public static class DefaultClauseContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 84, RULE_defaultClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(548);
			match(Default);
			setState(549);
			match(Colon);
			setState(551);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,51,_ctx) ) {
			case 1:
				{
				setState(550);
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
	public static class LabelledStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 86, RULE_labelledStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(553);
			identifier();
			setState(554);
			match(Colon);
			setState(555);
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
	public static class ThrowStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 88, RULE_throwStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(557);
			match(Throw);
			setState(558);
			if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
			setState(559);
			expressionSequence(0);
			setState(560);
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
	public static class TryStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 90, RULE_tryStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(562);
			match(Try);
			setState(563);
			block();
			setState(569);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Catch:
				{
				setState(564);
				catchProduction();
				setState(566);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,52,_ctx) ) {
				case 1:
					{
					setState(565);
					finallyProduction();
					}
					break;
				}
				}
				break;
			case Finally:
				{
				setState(568);
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
	public static class CatchProductionContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 92, RULE_catchProduction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(571);
			match(Catch);
			setState(577);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OpenParen) {
				{
				setState(572);
				match(OpenParen);
				setState(574);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OpenBracket || _la==OpenBrace || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 4503599627370495L) != 0)) {
					{
					setState(573);
					assignable();
					}
				}

				setState(576);
				match(CloseParen);
				}
			}

			setState(579);
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
	public static class FinallyProductionContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 94, RULE_finallyProduction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(581);
			match(Finally);
			setState(582);
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
	public static class DebuggerStatementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 96, RULE_debuggerStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(584);
			match(Debugger);
			setState(585);
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
	public static class FunctionDeclarationContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 98, RULE_functionDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(588);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Async) {
				{
				setState(587);
				match(Async);
				}
			}

			setState(590);
			match(Function_);
			setState(592);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Multiply) {
				{
				setState(591);
				match(Multiply);
				}
			}

			setState(594);
			identifier();
			setState(595);
			match(OpenParen);
			setState(597);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 262688L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 4503599627370495L) != 0)) {
				{
				setState(596);
				formalParameterList();
				}
			}

			setState(599);
			match(CloseParen);
			setState(600);
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
	public static class ClassDeclarationContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 100, RULE_classDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(602);
			match(Class);
			setState(603);
			identifier();
			setState(604);
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
	public static class ClassTailContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 102, RULE_classTail);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(608);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Extends) {
				{
				setState(606);
				match(Extends);
				setState(607);
				singleExpression();
				}
			}

			setState(610);
			match(OpenBrace);
			setState(614);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,60,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(611);
					classElement();
					}
					} 
				}
				setState(616);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,60,_ctx);
			}
			setState(617);
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
	public static class ClassElementContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 104, RULE_classElement);
		try {
			setState(638);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,64,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(622);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,61,_ctx) ) {
				case 1:
					{
					setState(619);
					match(Static);
					}
					break;
				case 2:
					{
					setState(620);
					if (!(this.n("static"))) throw new FailedPredicateException(this, "this.n(\"static\")");
					setState(621);
					identifier();
					}
					break;
				}
				setState(624);
				methodDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(628);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,62,_ctx) ) {
				case 1:
					{
					setState(625);
					match(Static);
					}
					break;
				case 2:
					{
					setState(626);
					if (!(this.n("static"))) throw new FailedPredicateException(this, "this.n(\"static\")");
					setState(627);
					identifier();
					}
					break;
				}
				setState(630);
				fieldDefinition();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(634);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,63,_ctx) ) {
				case 1:
					{
					setState(631);
					match(Static);
					}
					break;
				case 2:
					{
					setState(632);
					if (!(this.n("static"))) throw new FailedPredicateException(this, "this.n(\"static\")");
					setState(633);
					identifier();
					}
					break;
				}
				setState(636);
				block();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(637);
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
	public static class MethodDefinitionContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 106, RULE_methodDefinition);
		int _la;
		try {
			setState(674);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,71,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(642);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,65,_ctx) ) {
				case 1:
					{
					setState(640);
					match(Async);
					setState(641);
					if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
					}
					break;
				}
				setState(645);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Multiply) {
					{
					setState(644);
					match(Multiply);
					}
				}

				setState(647);
				classElementName();
				setState(648);
				match(OpenParen);
				setState(650);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 262688L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 4503599627370495L) != 0)) {
					{
					setState(649);
					formalParameterList();
					}
				}

				setState(652);
				match(CloseParen);
				setState(653);
				functionBody();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(656);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,68,_ctx) ) {
				case 1:
					{
					setState(655);
					match(Multiply);
					}
					break;
				}
				setState(658);
				getter();
				setState(659);
				match(OpenParen);
				setState(660);
				match(CloseParen);
				setState(661);
				functionBody();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(664);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,69,_ctx) ) {
				case 1:
					{
					setState(663);
					match(Multiply);
					}
					break;
				}
				setState(666);
				setter();
				setState(667);
				match(OpenParen);
				setState(669);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 262688L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 4503599627370495L) != 0)) {
					{
					setState(668);
					formalParameterList();
					}
				}

				setState(671);
				match(CloseParen);
				setState(672);
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
	public static class FieldDefinitionContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 108, RULE_fieldDefinition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(676);
			classElementName();
			setState(678);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,72,_ctx) ) {
			case 1:
				{
				setState(677);
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
	public static class ClassElementNameContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 110, RULE_classElementName);
		try {
			setState(682);
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
			case Meta:
			case Target:
			case Identifier:
			case StringLiteral:
				enterOuterAlt(_localctx, 1);
				{
				setState(680);
				propertyName();
				}
				break;
			case Hashtag:
				enterOuterAlt(_localctx, 2);
				{
				setState(681);
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
	public static class PrivateIdentifierContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 112, RULE_privateIdentifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(684);
			match(Hashtag);
			setState(685);
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
	public static class FormalParameterListContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 114, RULE_formalParameterList);
		int _la;
		try {
			int _alt;
			setState(700);
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
			case Meta:
			case Target:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(687);
				formalParameterArg();
				setState(692);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,74,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(688);
						match(Comma);
						setState(689);
						formalParameterArg();
						}
						} 
					}
					setState(694);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,74,_ctx);
				}
				setState(697);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(695);
					match(Comma);
					setState(696);
					restParameterArg();
					}
				}

				}
				break;
			case Ellipsis:
				enterOuterAlt(_localctx, 2);
				{
				setState(699);
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
	public static class FormalParameterArgContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 116, RULE_formalParameterArg);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(702);
			assignable();
			setState(705);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Assign) {
				{
				setState(703);
				match(Assign);
				setState(704);
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
	public static class RestParameterArgContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 118, RULE_restParameterArg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(707);
			match(Ellipsis);
			setState(708);
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
	public static class FunctionBodyContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 120, RULE_functionBody);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(710);
			match(OpenBrace);
			setState(712);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,78,_ctx) ) {
			case 1:
				{
				setState(711);
				sourceElements();
				}
				break;
			}
			setState(714);
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
	public static class SourceElementsContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 122, RULE_sourceElements);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(717); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(716);
					sourceElement();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(719); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,79,_ctx);
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
	public static class ArrayLiteralContext extends JavaScriptRuleContext {
		public TerminalNode OpenBracket() { return getToken(JavaScriptParser.OpenBracket, 0); }
		public TerminalNode CloseBracket() { return getToken(JavaScriptParser.CloseBracket, 0); }
		public ElementListContext elementList() {
			return getRuleContext(ElementListContext.class,0);
		}
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
		enterRule(_localctx, 124, RULE_arrayLiteral);
		try {
			setState(727);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,80,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(721);
				match(OpenBracket);
				setState(722);
				match(CloseBracket);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(723);
				match(OpenBracket);
				setState(724);
				elementList();
				setState(725);
				match(CloseBracket);
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
	public static class ElementListContext extends JavaScriptRuleContext {
		public List<ArrayElementContext> arrayElement() {
			return getRuleContexts(ArrayElementContext.class);
		}
		public ArrayElementContext arrayElement(int i) {
			return getRuleContext(ArrayElementContext.class,i);
		}
		public List<TerminalNode> Comma() { return getTokens(JavaScriptParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(JavaScriptParser.Comma, i);
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
		enterRule(_localctx, 126, RULE_elementList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(729);
			arrayElement();
			setState(734);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==Comma) {
				{
				{
				setState(730);
				match(Comma);
				setState(731);
				arrayElement();
				}
				}
				setState(736);
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
	public static class ArrayElementContext extends JavaScriptRuleContext {
		public TerminalNode Ellipsis() { return getToken(JavaScriptParser.Ellipsis, 0); }
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
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
		enterRule(_localctx, 128, RULE_arrayElement);
		int _la;
		try {
			setState(742);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Ellipsis:
				enterOuterAlt(_localctx, 1);
				{
				setState(737);
				match(Ellipsis);
				setState(738);
				singleExpression();
				}
				break;
			case RegularExpressionLiteral:
			case OpenBracket:
			case CloseBracket:
			case OpenParen:
			case OpenBrace:
			case Comma:
			case PlusPlus:
			case MinusMinus:
			case Plus:
			case Minus:
			case BitNot:
			case Not:
			case NullLiteral:
			case BooleanLiteral:
			case DecimalLiteral:
			case HexIntegerLiteral:
			case OctalIntegerLiteral:
			case OctalIntegerLiteral2:
			case BinaryIntegerLiteral:
			case BigHexIntegerLiteral:
			case BigOctalIntegerLiteral:
			case BigBinaryIntegerLiteral:
			case BigDecimalIntegerLiteral:
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
			case Meta:
			case Target:
			case Identifier:
			case StringLiteral:
			case BackTick:
				enterOuterAlt(_localctx, 2);
				{
				setState(740);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -4611686018361326928L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 9223372036854775807L) != 0)) {
					{
					setState(739);
					singleExpression();
					}
				}

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
	public static class PropertyAssignmentContext extends JavaScriptRuleContext {
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
		public SingleExpressionContext label;
		public SingleExpressionContext value;
		public TerminalNode OpenBracket() { return getToken(JavaScriptParser.OpenBracket, 0); }
		public TerminalNode CloseBracket() { return getToken(JavaScriptParser.CloseBracket, 0); }
		public TerminalNode Colon() { return getToken(JavaScriptParser.Colon, 0); }
		public List<SingleExpressionContext> singleExpression() {
			return getRuleContexts(SingleExpressionContext.class);
		}
		public SingleExpressionContext singleExpression(int i) {
			return getRuleContext(SingleExpressionContext.class,i);
		}
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
	public static class SpreadPropertyContext extends PropertyAssignmentContext {
		public TerminalNode Ellipsis() { return getToken(JavaScriptParser.Ellipsis, 0); }
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public SpreadPropertyContext(PropertyAssignmentContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterSpreadProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitSpreadProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitSpreadProperty(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PropertyShorthandContext extends PropertyAssignmentContext {
		public IdentifierNameContext identifierName() {
			return getRuleContext(IdentifierNameContext.class,0);
		}
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
		enterRule(_localctx, 130, RULE_propertyAssignment);
		int _la;
		try {
			setState(782);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,87,_ctx) ) {
			case 1:
				_localctx = new PropertyExpressionAssignmentContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(744);
				propertyName();
				setState(745);
				match(Colon);
				setState(746);
				singleExpression();
				}
				break;
			case 2:
				_localctx = new ComputedPropertyExpressionAssignmentContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(748);
				match(OpenBracket);
				setState(749);
				((ComputedPropertyExpressionAssignmentContext)_localctx).label = singleExpression();
				setState(750);
				match(CloseBracket);
				setState(751);
				match(Colon);
				setState(752);
				((ComputedPropertyExpressionAssignmentContext)_localctx).value = singleExpression();
				}
				break;
			case 3:
				_localctx = new FunctionPropertyContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(755);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,84,_ctx) ) {
				case 1:
					{
					setState(754);
					match(Async);
					}
					break;
				}
				setState(758);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Multiply) {
					{
					setState(757);
					match(Multiply);
					}
				}

				setState(760);
				propertyName();
				setState(761);
				match(OpenParen);
				setState(763);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 262688L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 4503599627370495L) != 0)) {
					{
					setState(762);
					formalParameterList();
					}
				}

				setState(765);
				match(CloseParen);
				setState(766);
				functionBody();
				}
				break;
			case 4:
				_localctx = new PropertyGetterContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(768);
				getter();
				setState(769);
				match(OpenParen);
				setState(770);
				match(CloseParen);
				setState(771);
				functionBody();
				}
				break;
			case 5:
				_localctx = new PropertySetterContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(773);
				setter();
				setState(774);
				match(OpenParen);
				setState(775);
				formalParameterArg();
				setState(776);
				match(CloseParen);
				setState(777);
				functionBody();
				}
				break;
			case 6:
				_localctx = new SpreadPropertyContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(779);
				match(Ellipsis);
				setState(780);
				singleExpression();
				}
				break;
			case 7:
				_localctx = new PropertyShorthandContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(781);
				identifierName();
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
	public static class ObjectBindingPatternContext extends JavaScriptRuleContext {
		public TerminalNode OpenBrace() { return getToken(JavaScriptParser.OpenBrace, 0); }
		public TerminalNode CloseBrace() { return getToken(JavaScriptParser.CloseBrace, 0); }
		public RestBindingElementContext restBindingElement() {
			return getRuleContext(RestBindingElementContext.class,0);
		}
		public List<TerminalNode> Comma() { return getTokens(JavaScriptParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(JavaScriptParser.Comma, i);
		}
		public List<PropertyBindingPatternContext> propertyBindingPattern() {
			return getRuleContexts(PropertyBindingPatternContext.class);
		}
		public PropertyBindingPatternContext propertyBindingPattern(int i) {
			return getRuleContext(PropertyBindingPatternContext.class,i);
		}
		public ObjectBindingPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectBindingPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterObjectBindingPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitObjectBindingPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitObjectBindingPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectBindingPatternContext objectBindingPattern() throws RecognitionException {
		ObjectBindingPatternContext _localctx = new ObjectBindingPatternContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_objectBindingPattern);
		int _la;
		try {
			int _alt;
			setState(811);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,92,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(784);
				match(OpenBrace);
				setState(785);
				match(CloseBrace);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(786);
				match(OpenBrace);
				setState(787);
				restBindingElement();
				setState(789);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(788);
					match(Comma);
					}
				}

				setState(791);
				match(CloseBrace);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(793);
				match(OpenBrace);
				setState(794);
				propertyBindingPattern();
				setState(799);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,89,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(795);
						match(Comma);
						setState(796);
						propertyBindingPattern();
						}
						} 
					}
					setState(801);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,89,_ctx);
				}
				setState(804);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,90,_ctx) ) {
				case 1:
					{
					setState(802);
					match(Comma);
					setState(803);
					restBindingElement();
					}
					break;
				}
				setState(807);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(806);
					match(Comma);
					}
				}

				setState(809);
				match(CloseBrace);
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
	public static class PropertyBindingPatternContext extends JavaScriptRuleContext {
		public PropertyBindingPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyBindingPattern; }
	 
		public PropertyBindingPatternContext() { }
		public void copyFrom(PropertyBindingPatternContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NamedPropertyBindingPatternContext extends PropertyBindingPatternContext {
		public PropertyNameContext propertyName() {
			return getRuleContext(PropertyNameContext.class,0);
		}
		public TerminalNode Colon() { return getToken(JavaScriptParser.Colon, 0); }
		public BindingElementContext bindingElement() {
			return getRuleContext(BindingElementContext.class,0);
		}
		public NamedPropertyBindingPatternContext(PropertyBindingPatternContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterNamedPropertyBindingPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitNamedPropertyBindingPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitNamedPropertyBindingPattern(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class RegularPropertyBindingPatternContext extends PropertyBindingPatternContext {
		public IdentifierNameContext identifierName() {
			return getRuleContext(IdentifierNameContext.class,0);
		}
		public InitializerContext initializer() {
			return getRuleContext(InitializerContext.class,0);
		}
		public RegularPropertyBindingPatternContext(PropertyBindingPatternContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterRegularPropertyBindingPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitRegularPropertyBindingPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitRegularPropertyBindingPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyBindingPatternContext propertyBindingPattern() throws RecognitionException {
		PropertyBindingPatternContext _localctx = new PropertyBindingPatternContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_propertyBindingPattern);
		int _la;
		try {
			setState(821);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,94,_ctx) ) {
			case 1:
				_localctx = new RegularPropertyBindingPatternContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(813);
				identifierName();
				setState(815);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Assign) {
					{
					setState(814);
					initializer();
					}
				}

				}
				break;
			case 2:
				_localctx = new NamedPropertyBindingPatternContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(817);
				propertyName();
				setState(818);
				match(Colon);
				setState(819);
				bindingElement();
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
	public static class ArrayBindingPatternContext extends JavaScriptRuleContext {
		public TerminalNode OpenBracket() { return getToken(JavaScriptParser.OpenBracket, 0); }
		public TerminalNode CloseBracket() { return getToken(JavaScriptParser.CloseBracket, 0); }
		public RestBindingElementContext restBindingElement() {
			return getRuleContext(RestBindingElementContext.class,0);
		}
		public List<TerminalNode> Comma() { return getTokens(JavaScriptParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(JavaScriptParser.Comma, i);
		}
		public ArrayItemListContext arrayItemList() {
			return getRuleContext(ArrayItemListContext.class,0);
		}
		public ArrayBindingPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayBindingPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterArrayBindingPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitArrayBindingPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitArrayBindingPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayBindingPatternContext arrayBindingPattern() throws RecognitionException {
		ArrayBindingPatternContext _localctx = new ArrayBindingPatternContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_arrayBindingPattern);
		int _la;
		try {
			setState(843);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,98,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(823);
				match(OpenBracket);
				setState(824);
				match(CloseBracket);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(825);
				match(OpenBracket);
				setState(826);
				restBindingElement();
				setState(828);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(827);
					match(Comma);
					}
				}

				setState(830);
				match(CloseBracket);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(832);
				match(OpenBracket);
				setState(833);
				arrayItemList();
				setState(836);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,96,_ctx) ) {
				case 1:
					{
					setState(834);
					match(Comma);
					setState(835);
					restBindingElement();
					}
					break;
				}
				setState(839);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(838);
					match(Comma);
					}
				}

				setState(841);
				match(CloseBracket);
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
	public static class ArrayItemListContext extends JavaScriptRuleContext {
		public BindingElementContext bindingElement() {
			return getRuleContext(BindingElementContext.class,0);
		}
		public List<TerminalNode> Comma() { return getTokens(JavaScriptParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(JavaScriptParser.Comma, i);
		}
		public List<ArrayItemBindingContext> arrayItemBinding() {
			return getRuleContexts(ArrayItemBindingContext.class);
		}
		public ArrayItemBindingContext arrayItemBinding(int i) {
			return getRuleContext(ArrayItemBindingContext.class,i);
		}
		public ArrayItemListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayItemList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterArrayItemList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitArrayItemList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitArrayItemList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayItemListContext arrayItemList() throws RecognitionException {
		ArrayItemListContext _localctx = new ArrayItemListContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_arrayItemList);
		try {
			int _alt;
			setState(862);
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
			case Meta:
			case Target:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(845);
				bindingElement();
				setState(850);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,99,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(846);
						match(Comma);
						setState(847);
						arrayItemBinding();
						}
						} 
					}
					setState(852);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,99,_ctx);
				}
				}
				break;
			case Comma:
				enterOuterAlt(_localctx, 2);
				{
				setState(853);
				match(Comma);
				setState(854);
				arrayItemBinding();
				setState(859);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,100,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(855);
						match(Comma);
						setState(856);
						arrayItemBinding();
						}
						} 
					}
					setState(861);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,100,_ctx);
				}
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
	public static class ArrayItemBindingContext extends JavaScriptRuleContext {
		public BindingElementContext bindingElement() {
			return getRuleContext(BindingElementContext.class,0);
		}
		public ArrayItemBindingContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayItemBinding; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterArrayItemBinding(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitArrayItemBinding(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitArrayItemBinding(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayItemBindingContext arrayItemBinding() throws RecognitionException {
		ArrayItemBindingContext _localctx = new ArrayItemBindingContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_arrayItemBinding);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(865);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OpenBracket || _la==OpenBrace || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 4503599627370495L) != 0)) {
				{
				setState(864);
				bindingElement();
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
	public static class BindingElementContext extends JavaScriptRuleContext {
		public AssignableContext assignable() {
			return getRuleContext(AssignableContext.class,0);
		}
		public InitializerContext initializer() {
			return getRuleContext(InitializerContext.class,0);
		}
		public BindingElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bindingElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterBindingElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitBindingElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitBindingElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BindingElementContext bindingElement() throws RecognitionException {
		BindingElementContext _localctx = new BindingElementContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_bindingElement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(867);
			assignable();
			setState(869);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Assign) {
				{
				setState(868);
				initializer();
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
	public static class RestBindingElementContext extends JavaScriptRuleContext {
		public TerminalNode Ellipsis() { return getToken(JavaScriptParser.Ellipsis, 0); }
		public IdentifierNameContext identifierName() {
			return getRuleContext(IdentifierNameContext.class,0);
		}
		public RestBindingElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_restBindingElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterRestBindingElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitRestBindingElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitRestBindingElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RestBindingElementContext restBindingElement() throws RecognitionException {
		RestBindingElementContext _localctx = new RestBindingElementContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_restBindingElement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(871);
			match(Ellipsis);
			setState(872);
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
	public static class PropertyNameContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 146, RULE_propertyName);
		try {
			setState(881);
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
			case Meta:
			case Target:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(874);
				identifierName();
				}
				break;
			case StringLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(875);
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
				setState(876);
				numericLiteral();
				}
				break;
			case OpenBracket:
				enterOuterAlt(_localctx, 4);
				{
				setState(877);
				match(OpenBracket);
				setState(878);
				singleExpression();
				setState(879);
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
	public static class ArgumentsContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 148, RULE_arguments);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(883);
			match(OpenParen);
			setState(895);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -4611686018361064784L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 9223372036854775807L) != 0)) {
				{
				setState(884);
				argument();
				setState(889);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,105,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(885);
						match(Comma);
						setState(886);
						argument();
						}
						} 
					}
					setState(891);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,105,_ctx);
				}
				setState(893);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(892);
					match(Comma);
					}
				}

				}
			}

			setState(897);
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
	public static class ArgumentContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 150, RULE_argument);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(900);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Ellipsis) {
				{
				setState(899);
				match(Ellipsis);
				}
			}

			setState(904);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,109,_ctx) ) {
			case 1:
				{
				setState(902);
				singleExpression();
				}
				break;
			case 2:
				{
				setState(903);
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
	public static class ExpressionSequenceContext extends JavaScriptRuleContext {
		public ExpressionSequenceContext lhs;
		public ExpressionSequenceContext rhs;
		public SingleExpressionContext singleExpression() {
			return getRuleContext(SingleExpressionContext.class,0);
		}
		public TerminalNode Comma() { return getToken(JavaScriptParser.Comma, 0); }
		public List<ExpressionSequenceContext> expressionSequence() {
			return getRuleContexts(ExpressionSequenceContext.class);
		}
		public ExpressionSequenceContext expressionSequence(int i) {
			return getRuleContext(ExpressionSequenceContext.class,i);
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
		return expressionSequence(0);
	}

	private ExpressionSequenceContext expressionSequence(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpressionSequenceContext _localctx = new ExpressionSequenceContext(_ctx, _parentState);
		ExpressionSequenceContext _prevctx = _localctx;
		int _startState = 152;
		enterRecursionRule(_localctx, 152, RULE_expressionSequence, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(907);
			singleExpression();
			}
			_ctx.stop = _input.LT(-1);
			setState(914);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,110,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new ExpressionSequenceContext(_parentctx, _parentState);
					_localctx.lhs = _prevctx;
					pushNewRecursionContext(_localctx, _startState, RULE_expressionSequence);
					setState(909);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(910);
					match(Comma);
					setState(911);
					((ExpressionSequenceContext)_localctx).rhs = expressionSequence(3);
					}
					} 
				}
				setState(916);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,110,_ctx);
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
	public static class SingleExpressionContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 154, RULE_singleExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(917);
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
	public static class SingleExpressionImplContext extends JavaScriptRuleContext {
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
		public TerminalNode Target() { return getToken(JavaScriptParser.Target, 0); }
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
	public static class ImportMetaExpressionContext extends SingleExpressionImplContext {
		public TerminalNode Import() { return getToken(JavaScriptParser.Import, 0); }
		public TerminalNode Dot() { return getToken(JavaScriptParser.Dot, 0); }
		public TerminalNode Meta() { return getToken(JavaScriptParser.Meta, 0); }
		public ImportMetaExpressionContext(SingleExpressionImplContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).enterImportMetaExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JavaScriptParserListener ) ((JavaScriptParserListener)listener).exitImportMetaExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JavaScriptParserVisitor ) return ((JavaScriptParserVisitor<? extends T>)visitor).visitImportMetaExpression(this);
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
		public SingleExpressionImplContext lhs;
		public SingleExpressionImplContext rhs;
		public TerminalNode Assign() { return getToken(JavaScriptParser.Assign, 0); }
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
		}
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
		public TerminalNode Yield() { return getToken(JavaScriptParser.Yield, 0); }
		public TerminalNode YieldStar() { return getToken(JavaScriptParser.YieldStar, 0); }
		public ExpressionSequenceContext expressionSequence() {
			return getRuleContext(ExpressionSequenceContext.class,0);
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
		public SingleExpressionImplContext lhs;
		public SingleExpressionImplContext rhs;
		public AssignmentOperatorContext assignmentOperator() {
			return getRuleContext(AssignmentOperatorContext.class,0);
		}
		public List<SingleExpressionImplContext> singleExpressionImpl() {
			return getRuleContexts(SingleExpressionImplContext.class);
		}
		public SingleExpressionImplContext singleExpressionImpl(int i) {
			return getRuleContext(SingleExpressionImplContext.class,i);
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
		int _startState = 156;
		enterRecursionRule(_localctx, 156, RULE_singleExpressionImpl, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(982);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,113,_ctx) ) {
			case 1:
				{
				_localctx = new FunctionExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(920);
				anonymousFunction();
				}
				break;
			case 2:
				{
				_localctx = new ClassExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(921);
				match(Class);
				setState(923);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 99)) & ~0x3f) == 0 && ((1L << (_la - 99)) & 58789903L) != 0)) {
					{
					setState(922);
					identifier();
					}
				}

				setState(925);
				classTail();
				}
				break;
			case 3:
				{
				_localctx = new NewExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(926);
				match(New);
				setState(927);
				identifier();
				setState(928);
				arguments();
				}
				break;
			case 4:
				{
				_localctx = new NewExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(930);
				match(New);
				setState(931);
				singleExpressionImpl(0);
				setState(932);
				arguments();
				}
				break;
			case 5:
				{
				_localctx = new NewExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(934);
				match(New);
				setState(935);
				singleExpressionImpl(43);
				}
				break;
			case 6:
				{
				_localctx = new MetaExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(936);
				match(New);
				setState(937);
				match(Dot);
				setState(938);
				match(Target);
				}
				break;
			case 7:
				{
				_localctx = new ImportMetaExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(939);
				match(Import);
				setState(940);
				match(Dot);
				setState(941);
				match(Meta);
				}
				break;
			case 8:
				{
				_localctx = new DeleteExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(942);
				match(Delete);
				setState(943);
				singleExpressionImpl(37);
				}
				break;
			case 9:
				{
				_localctx = new VoidExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(944);
				match(Void);
				setState(945);
				singleExpressionImpl(36);
				}
				break;
			case 10:
				{
				_localctx = new TypeofExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(946);
				match(Typeof);
				setState(947);
				singleExpressionImpl(35);
				}
				break;
			case 11:
				{
				_localctx = new PreIncrementExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(948);
				match(PlusPlus);
				setState(949);
				singleExpressionImpl(34);
				}
				break;
			case 12:
				{
				_localctx = new PreDecreaseExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(950);
				match(MinusMinus);
				setState(951);
				singleExpressionImpl(33);
				}
				break;
			case 13:
				{
				_localctx = new UnaryPlusExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(952);
				match(Plus);
				setState(953);
				singleExpressionImpl(32);
				}
				break;
			case 14:
				{
				_localctx = new UnaryMinusExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(954);
				match(Minus);
				setState(955);
				singleExpressionImpl(31);
				}
				break;
			case 15:
				{
				_localctx = new BitNotExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(956);
				match(BitNot);
				setState(957);
				singleExpressionImpl(30);
				}
				break;
			case 16:
				{
				_localctx = new NotExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(958);
				match(Not);
				setState(959);
				singleExpressionImpl(29);
				}
				break;
			case 17:
				{
				_localctx = new AwaitExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(960);
				match(Await);
				setState(961);
				singleExpressionImpl(28);
				}
				break;
			case 18:
				{
				_localctx = new ImportExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(962);
				match(Import);
				setState(963);
				match(OpenParen);
				setState(964);
				singleExpressionImpl(0);
				setState(965);
				match(CloseParen);
				}
				break;
			case 19:
				{
				_localctx = new YieldExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(967);
				_la = _input.LA(1);
				if ( !(_la==Yield || _la==YieldStar) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(970);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,112,_ctx) ) {
				case 1:
					{
					setState(968);
					if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
					setState(969);
					expressionSequence(0);
					}
					break;
				}
				}
				break;
			case 20:
				{
				_localctx = new ThisExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(972);
				match(This);
				}
				break;
			case 21:
				{
				_localctx = new IdentifierExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(973);
				identifier();
				}
				break;
			case 22:
				{
				_localctx = new SuperExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(974);
				match(Super);
				}
				break;
			case 23:
				{
				_localctx = new LiteralExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(975);
				literal();
				}
				break;
			case 24:
				{
				_localctx = new ArrayLiteralExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(976);
				arrayLiteral();
				}
				break;
			case 25:
				{
				_localctx = new ObjectLiteralExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(977);
				objectLiteral();
				}
				break;
			case 26:
				{
				_localctx = new ParenthesizedExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(978);
				match(OpenParen);
				setState(979);
				expressionSequence(0);
				setState(980);
				match(CloseParen);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(1071);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,118,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1069);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,117,_ctx) ) {
					case 1:
						{
						_localctx = new OptionalChainExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(984);
						if (!(precpred(_ctx, 48))) throw new FailedPredicateException(this, "precpred(_ctx, 48)");
						setState(985);
						match(QuestionMarkDot);
						setState(986);
						singleExpressionImpl(49);
						}
						break;
					case 2:
						{
						_localctx = new PowerExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(987);
						if (!(precpred(_ctx, 27))) throw new FailedPredicateException(this, "precpred(_ctx, 27)");
						setState(988);
						match(Power);
						setState(989);
						singleExpressionImpl(27);
						}
						break;
					case 3:
						{
						_localctx = new MultiplicativeExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(990);
						if (!(precpred(_ctx, 26))) throw new FailedPredicateException(this, "precpred(_ctx, 26)");
						setState(991);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 469762048L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(992);
						singleExpressionImpl(27);
						}
						break;
					case 4:
						{
						_localctx = new AdditiveExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(993);
						if (!(precpred(_ctx, 25))) throw new FailedPredicateException(this, "precpred(_ctx, 25)");
						setState(994);
						_la = _input.LA(1);
						if ( !(_la==Plus || _la==Minus) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(995);
						singleExpressionImpl(26);
						}
						break;
					case 5:
						{
						_localctx = new CoalesceExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(996);
						if (!(precpred(_ctx, 24))) throw new FailedPredicateException(this, "precpred(_ctx, 24)");
						setState(997);
						match(NullCoalesce);
						setState(998);
						singleExpressionImpl(25);
						}
						break;
					case 6:
						{
						_localctx = new BitShiftExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(999);
						if (!(precpred(_ctx, 23))) throw new FailedPredicateException(this, "precpred(_ctx, 23)");
						setState(1000);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 30064771072L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1001);
						singleExpressionImpl(24);
						}
						break;
					case 7:
						{
						_localctx = new RelationalExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1002);
						if (!(precpred(_ctx, 22))) throw new FailedPredicateException(this, "precpred(_ctx, 22)");
						setState(1003);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 515396075520L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1004);
						singleExpressionImpl(23);
						}
						break;
					case 8:
						{
						_localctx = new InstanceofExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1005);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(1006);
						match(Instanceof);
						setState(1007);
						singleExpressionImpl(22);
						}
						break;
					case 9:
						{
						_localctx = new InExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1008);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(1009);
						match(In);
						setState(1010);
						singleExpressionImpl(21);
						}
						break;
					case 10:
						{
						_localctx = new EqualityExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1011);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(1012);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 8246337208320L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1013);
						singleExpressionImpl(20);
						}
						break;
					case 11:
						{
						_localctx = new BitAndExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1014);
						if (!(precpred(_ctx, 18))) throw new FailedPredicateException(this, "precpred(_ctx, 18)");
						setState(1015);
						match(BitAnd);
						setState(1016);
						singleExpressionImpl(19);
						}
						break;
					case 12:
						{
						_localctx = new BitXOrExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1017);
						if (!(precpred(_ctx, 17))) throw new FailedPredicateException(this, "precpred(_ctx, 17)");
						setState(1018);
						match(BitXOr);
						setState(1019);
						singleExpressionImpl(18);
						}
						break;
					case 13:
						{
						_localctx = new BitOrExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1020);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(1021);
						match(BitOr);
						setState(1022);
						singleExpressionImpl(17);
						}
						break;
					case 14:
						{
						_localctx = new LogicalAndExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1023);
						if (!(precpred(_ctx, 15))) throw new FailedPredicateException(this, "precpred(_ctx, 15)");
						setState(1024);
						match(And);
						setState(1025);
						singleExpressionImpl(16);
						}
						break;
					case 15:
						{
						_localctx = new LogicalOrExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1026);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(1027);
						match(Or);
						setState(1028);
						singleExpressionImpl(15);
						}
						break;
					case 16:
						{
						_localctx = new TernaryExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1029);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(1030);
						match(QuestionMark);
						setState(1031);
						singleExpressionImpl(0);
						setState(1032);
						match(Colon);
						setState(1033);
						singleExpressionImpl(13);
						}
						break;
					case 17:
						{
						_localctx = new AssignmentExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						((AssignmentExpressionContext)_localctx).lhs = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1035);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(1036);
						match(Assign);
						setState(1037);
						((AssignmentExpressionContext)_localctx).rhs = singleExpressionImpl(12);
						}
						break;
					case 18:
						{
						_localctx = new AssignmentOperatorExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						((AssignmentOperatorExpressionContext)_localctx).lhs = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1038);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(1039);
						assignmentOperator();
						setState(1040);
						((AssignmentOperatorExpressionContext)_localctx).rhs = singleExpressionImpl(11);
						}
						break;
					case 19:
						{
						_localctx = new MemberIndexExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1042);
						if (!(precpred(_ctx, 47))) throw new FailedPredicateException(this, "precpred(_ctx, 47)");
						setState(1044);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==QuestionMarkDot) {
							{
							setState(1043);
							match(QuestionMarkDot);
							}
						}

						setState(1046);
						match(OpenBracket);
						setState(1047);
						expressionSequence(0);
						setState(1048);
						match(CloseBracket);
						}
						break;
					case 20:
						{
						_localctx = new MemberDotExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1050);
						if (!(precpred(_ctx, 46))) throw new FailedPredicateException(this, "precpred(_ctx, 46)");
						setState(1052);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==QuestionMark) {
							{
							setState(1051);
							match(QuestionMark);
							}
						}

						setState(1054);
						match(Dot);
						setState(1056);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==Hashtag) {
							{
							setState(1055);
							match(Hashtag);
							}
						}

						setState(1058);
						identifierName();
						}
						break;
					case 21:
						{
						_localctx = new ArgumentsExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1059);
						if (!(precpred(_ctx, 42))) throw new FailedPredicateException(this, "precpred(_ctx, 42)");
						setState(1060);
						arguments();
						}
						break;
					case 22:
						{
						_localctx = new PostIncrementExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1061);
						if (!(precpred(_ctx, 39))) throw new FailedPredicateException(this, "precpred(_ctx, 39)");
						setState(1062);
						if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
						setState(1063);
						match(PlusPlus);
						}
						break;
					case 23:
						{
						_localctx = new PostDecreaseExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1064);
						if (!(precpred(_ctx, 38))) throw new FailedPredicateException(this, "precpred(_ctx, 38)");
						setState(1065);
						if (!(this.notLineTerminator())) throw new FailedPredicateException(this, "this.notLineTerminator()");
						setState(1066);
						match(MinusMinus);
						}
						break;
					case 24:
						{
						_localctx = new TemplateStringExpressionContext(new SingleExpressionImplContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_singleExpressionImpl);
						setState(1067);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(1068);
						templateStringLiteral();
						}
						break;
					}
					} 
				}
				setState(1073);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,118,_ctx);
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
	public static class InitializerContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 158, RULE_initializer);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1074);
			match(Assign);
			setState(1075);
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
	public static class AssignableContext extends JavaScriptRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public KeywordContext keyword() {
			return getRuleContext(KeywordContext.class,0);
		}
		public ArrayBindingPatternContext arrayBindingPattern() {
			return getRuleContext(ArrayBindingPatternContext.class,0);
		}
		public ObjectBindingPatternContext objectBindingPattern() {
			return getRuleContext(ObjectBindingPatternContext.class,0);
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
		enterRule(_localctx, 160, RULE_assignable);
		try {
			setState(1081);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,119,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1077);
				identifier();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1078);
				keyword();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1079);
				arrayBindingPattern();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1080);
				objectBindingPattern();
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
	public static class ObjectLiteralContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 162, RULE_objectLiteral);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1083);
			match(OpenBrace);
			setState(1095);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,122,_ctx) ) {
			case 1:
				{
				setState(1084);
				propertyAssignment();
				setState(1089);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,120,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1085);
						match(Comma);
						setState(1086);
						propertyAssignment();
						}
						} 
					}
					setState(1091);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,120,_ctx);
				}
				setState(1093);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(1092);
					match(Comma);
					}
				}

				}
				break;
			}
			setState(1097);
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
	public static class AnonymousFunctionContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 164, RULE_anonymousFunction);
		int _la;
		try {
			setState(1120);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,127,_ctx) ) {
			case 1:
				_localctx = new NamedFunctionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1099);
				functionDeclaration();
				}
				break;
			case 2:
				_localctx = new AnonymousFunctionDeclContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1101);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Async) {
					{
					setState(1100);
					match(Async);
					}
				}

				setState(1103);
				match(Function_);
				setState(1105);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Multiply) {
					{
					setState(1104);
					match(Multiply);
					}
				}

				setState(1107);
				match(OpenParen);
				setState(1109);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 262688L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 4503599627370495L) != 0)) {
					{
					setState(1108);
					formalParameterList();
					}
				}

				setState(1111);
				match(CloseParen);
				setState(1112);
				functionBody();
				}
				break;
			case 3:
				_localctx = new ArrowFunctionContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1114);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,126,_ctx) ) {
				case 1:
					{
					setState(1113);
					match(Async);
					}
					break;
				}
				setState(1116);
				arrowFunctionParameters();
				setState(1117);
				match(ARROW);
				setState(1118);
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
	public static class ArrowFunctionParametersContext extends JavaScriptRuleContext {
		public IdentifierNameContext identifierName() {
			return getRuleContext(IdentifierNameContext.class,0);
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
		enterRule(_localctx, 166, RULE_arrowFunctionParameters);
		int _la;
		try {
			setState(1128);
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
			case Meta:
			case Target:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1122);
				identifierName();
				}
				break;
			case OpenParen:
				enterOuterAlt(_localctx, 2);
				{
				setState(1123);
				match(OpenParen);
				setState(1125);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 262688L) != 0) || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 4503599627370495L) != 0)) {
					{
					setState(1124);
					formalParameterList();
					}
				}

				setState(1127);
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
	public static class ArrowFunctionBodyContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 168, RULE_arrowFunctionBody);
		try {
			setState(1132);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,130,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1130);
				singleExpression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1131);
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
	public static class AssignmentOperatorContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 170, RULE_assignmentOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1134);
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
	public static class LiteralContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 172, RULE_literal);
		try {
			setState(1143);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NullLiteral:
				enterOuterAlt(_localctx, 1);
				{
				setState(1136);
				match(NullLiteral);
				}
				break;
			case BooleanLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(1137);
				match(BooleanLiteral);
				}
				break;
			case StringLiteral:
				enterOuterAlt(_localctx, 3);
				{
				setState(1138);
				match(StringLiteral);
				}
				break;
			case BackTick:
				enterOuterAlt(_localctx, 4);
				{
				setState(1139);
				templateStringLiteral();
				}
				break;
			case RegularExpressionLiteral:
				enterOuterAlt(_localctx, 5);
				{
				setState(1140);
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
				setState(1141);
				numericLiteral();
				}
				break;
			case BigHexIntegerLiteral:
			case BigOctalIntegerLiteral:
			case BigBinaryIntegerLiteral:
			case BigDecimalIntegerLiteral:
				enterOuterAlt(_localctx, 7);
				{
				setState(1142);
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
	public static class TemplateStringLiteralContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 174, RULE_templateStringLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1145);
			match(BackTick);
			setState(1149);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==TemplateStringStartExpression || _la==TemplateStringAtom) {
				{
				{
				setState(1146);
				templateStringAtom();
				}
				}
				setState(1151);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1152);
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
	public static class TemplateStringAtomContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 176, RULE_templateStringAtom);
		try {
			setState(1159);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TemplateStringAtom:
				enterOuterAlt(_localctx, 1);
				{
				setState(1154);
				match(TemplateStringAtom);
				}
				break;
			case TemplateStringStartExpression:
				enterOuterAlt(_localctx, 2);
				{
				setState(1155);
				match(TemplateStringStartExpression);
				setState(1156);
				singleExpression();
				setState(1157);
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
	public static class NumericLiteralContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 178, RULE_numericLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1161);
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
	public static class BigintLiteralContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 180, RULE_bigintLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1163);
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
	public static class GetterContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 182, RULE_getter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1165);
			if (!(this.n("get"))) throw new FailedPredicateException(this, "this.n(\"get\")");
			setState(1166);
			identifier();
			setState(1167);
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
	public static class SetterContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 184, RULE_setter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1169);
			if (!(this.n("set"))) throw new FailedPredicateException(this, "this.n(\"set\")");
			setState(1170);
			identifier();
			setState(1171);
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
	public static class IdentifierNameContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 186, RULE_identifierName);
		try {
			setState(1175);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,134,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1173);
				identifier();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1174);
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
	public static class IdentifierContext extends JavaScriptRuleContext {
		public TerminalNode Identifier() { return getToken(JavaScriptParser.Identifier, 0); }
		public TerminalNode NonStrictLet() { return getToken(JavaScriptParser.NonStrictLet, 0); }
		public TerminalNode Async() { return getToken(JavaScriptParser.Async, 0); }
		public TerminalNode As() { return getToken(JavaScriptParser.As, 0); }
		public TerminalNode From() { return getToken(JavaScriptParser.From, 0); }
		public TerminalNode Yield() { return getToken(JavaScriptParser.Yield, 0); }
		public TerminalNode Of() { return getToken(JavaScriptParser.Of, 0); }
		public TerminalNode Target() { return getToken(JavaScriptParser.Target, 0); }
		public TerminalNode Meta() { return getToken(JavaScriptParser.Meta, 0); }
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
		enterRule(_localctx, 188, RULE_identifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1177);
			_la = _input.LA(1);
			if ( !(((((_la - 99)) & ~0x3f) == 0 && ((1L << (_la - 99)) & 58789903L) != 0)) ) {
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
	public static class ReservedWordContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 190, RULE_reservedWord);
		try {
			setState(1182);
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
				setState(1179);
				keyword();
				}
				break;
			case NullLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(1180);
				match(NullLiteral);
				}
				break;
			case BooleanLiteral:
				enterOuterAlt(_localctx, 3);
				{
				setState(1181);
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
	public static class KeywordContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 192, RULE_keyword);
		try {
			setState(1232);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Break:
				enterOuterAlt(_localctx, 1);
				{
				setState(1184);
				match(Break);
				}
				break;
			case Do:
				enterOuterAlt(_localctx, 2);
				{
				setState(1185);
				match(Do);
				}
				break;
			case Instanceof:
				enterOuterAlt(_localctx, 3);
				{
				setState(1186);
				match(Instanceof);
				}
				break;
			case Typeof:
				enterOuterAlt(_localctx, 4);
				{
				setState(1187);
				match(Typeof);
				}
				break;
			case Case:
				enterOuterAlt(_localctx, 5);
				{
				setState(1188);
				match(Case);
				}
				break;
			case Else:
				enterOuterAlt(_localctx, 6);
				{
				setState(1189);
				match(Else);
				}
				break;
			case New:
				enterOuterAlt(_localctx, 7);
				{
				setState(1190);
				match(New);
				}
				break;
			case Var:
				enterOuterAlt(_localctx, 8);
				{
				setState(1191);
				match(Var);
				}
				break;
			case Catch:
				enterOuterAlt(_localctx, 9);
				{
				setState(1192);
				match(Catch);
				}
				break;
			case Finally:
				enterOuterAlt(_localctx, 10);
				{
				setState(1193);
				match(Finally);
				}
				break;
			case Return:
				enterOuterAlt(_localctx, 11);
				{
				setState(1194);
				match(Return);
				}
				break;
			case Void:
				enterOuterAlt(_localctx, 12);
				{
				setState(1195);
				match(Void);
				}
				break;
			case Continue:
				enterOuterAlt(_localctx, 13);
				{
				setState(1196);
				match(Continue);
				}
				break;
			case For:
				enterOuterAlt(_localctx, 14);
				{
				setState(1197);
				match(For);
				}
				break;
			case Switch:
				enterOuterAlt(_localctx, 15);
				{
				setState(1198);
				match(Switch);
				}
				break;
			case While:
				enterOuterAlt(_localctx, 16);
				{
				setState(1199);
				match(While);
				}
				break;
			case Debugger:
				enterOuterAlt(_localctx, 17);
				{
				setState(1200);
				match(Debugger);
				}
				break;
			case Function_:
				enterOuterAlt(_localctx, 18);
				{
				setState(1201);
				match(Function_);
				}
				break;
			case This:
				enterOuterAlt(_localctx, 19);
				{
				setState(1202);
				match(This);
				}
				break;
			case With:
				enterOuterAlt(_localctx, 20);
				{
				setState(1203);
				match(With);
				}
				break;
			case Default:
				enterOuterAlt(_localctx, 21);
				{
				setState(1204);
				match(Default);
				}
				break;
			case If:
				enterOuterAlt(_localctx, 22);
				{
				setState(1205);
				match(If);
				}
				break;
			case Throw:
				enterOuterAlt(_localctx, 23);
				{
				setState(1206);
				match(Throw);
				}
				break;
			case Delete:
				enterOuterAlt(_localctx, 24);
				{
				setState(1207);
				match(Delete);
				}
				break;
			case In:
				enterOuterAlt(_localctx, 25);
				{
				setState(1208);
				match(In);
				}
				break;
			case Try:
				enterOuterAlt(_localctx, 26);
				{
				setState(1209);
				match(Try);
				}
				break;
			case Class:
				enterOuterAlt(_localctx, 27);
				{
				setState(1210);
				match(Class);
				}
				break;
			case Enum:
				enterOuterAlt(_localctx, 28);
				{
				setState(1211);
				match(Enum);
				}
				break;
			case Extends:
				enterOuterAlt(_localctx, 29);
				{
				setState(1212);
				match(Extends);
				}
				break;
			case Super:
				enterOuterAlt(_localctx, 30);
				{
				setState(1213);
				match(Super);
				}
				break;
			case Const:
				enterOuterAlt(_localctx, 31);
				{
				setState(1214);
				match(Const);
				}
				break;
			case Export:
				enterOuterAlt(_localctx, 32);
				{
				setState(1215);
				match(Export);
				}
				break;
			case Import:
				enterOuterAlt(_localctx, 33);
				{
				setState(1216);
				match(Import);
				}
				break;
			case Implements:
				enterOuterAlt(_localctx, 34);
				{
				setState(1217);
				match(Implements);
				}
				break;
			case StrictLet:
			case NonStrictLet:
				enterOuterAlt(_localctx, 35);
				{
				setState(1218);
				let_();
				}
				break;
			case Private:
				enterOuterAlt(_localctx, 36);
				{
				setState(1219);
				match(Private);
				}
				break;
			case Public:
				enterOuterAlt(_localctx, 37);
				{
				setState(1220);
				match(Public);
				}
				break;
			case Interface:
				enterOuterAlt(_localctx, 38);
				{
				setState(1221);
				match(Interface);
				}
				break;
			case Package:
				enterOuterAlt(_localctx, 39);
				{
				setState(1222);
				match(Package);
				}
				break;
			case Protected:
				enterOuterAlt(_localctx, 40);
				{
				setState(1223);
				match(Protected);
				}
				break;
			case Static:
				enterOuterAlt(_localctx, 41);
				{
				setState(1224);
				match(Static);
				}
				break;
			case Yield:
				enterOuterAlt(_localctx, 42);
				{
				setState(1225);
				match(Yield);
				}
				break;
			case YieldStar:
				enterOuterAlt(_localctx, 43);
				{
				setState(1226);
				match(YieldStar);
				}
				break;
			case Async:
				enterOuterAlt(_localctx, 44);
				{
				setState(1227);
				match(Async);
				}
				break;
			case Await:
				enterOuterAlt(_localctx, 45);
				{
				setState(1228);
				match(Await);
				}
				break;
			case From:
				enterOuterAlt(_localctx, 46);
				{
				setState(1229);
				match(From);
				}
				break;
			case As:
				enterOuterAlt(_localctx, 47);
				{
				setState(1230);
				match(As);
				}
				break;
			case Of:
				enterOuterAlt(_localctx, 48);
				{
				setState(1231);
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
	public static class Let_Context extends JavaScriptRuleContext {
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
		enterRule(_localctx, 194, RULE_let_);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1234);
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
	public static class EosContext extends JavaScriptRuleContext {
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
		enterRule(_localctx, 196, RULE_eos);
		try {
			setState(1240);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,137,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1236);
				match(SemiColon);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1237);
				match(EOF);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1238);
				if (!(this.lineTerminatorAhead())) throw new FailedPredicateException(this, "this.lineTerminatorAhead()");
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1239);
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
		case 29:
			return expressionStatement_sempred((ExpressionStatementContext)_localctx, predIndex);
		case 33:
			return continueStatement_sempred((ContinueStatementContext)_localctx, predIndex);
		case 34:
			return breakStatement_sempred((BreakStatementContext)_localctx, predIndex);
		case 35:
			return returnStatement_sempred((ReturnStatementContext)_localctx, predIndex);
		case 36:
			return yieldStatement_sempred((YieldStatementContext)_localctx, predIndex);
		case 44:
			return throwStatement_sempred((ThrowStatementContext)_localctx, predIndex);
		case 52:
			return classElement_sempred((ClassElementContext)_localctx, predIndex);
		case 53:
			return methodDefinition_sempred((MethodDefinitionContext)_localctx, predIndex);
		case 76:
			return expressionSequence_sempred((ExpressionSequenceContext)_localctx, predIndex);
		case 78:
			return singleExpressionImpl_sempred((SingleExpressionImplContext)_localctx, predIndex);
		case 91:
			return getter_sempred((GetterContext)_localctx, predIndex);
		case 92:
			return setter_sempred((SetterContext)_localctx, predIndex);
		case 98:
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
	private boolean expressionSequence_sempred(ExpressionSequenceContext _localctx, int predIndex) {
		switch (predIndex) {
		case 10:
			return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean singleExpressionImpl_sempred(SingleExpressionImplContext _localctx, int predIndex) {
		switch (predIndex) {
		case 11:
			return this.notLineTerminator();
		case 12:
			return precpred(_ctx, 48);
		case 13:
			return precpred(_ctx, 27);
		case 14:
			return precpred(_ctx, 26);
		case 15:
			return precpred(_ctx, 25);
		case 16:
			return precpred(_ctx, 24);
		case 17:
			return precpred(_ctx, 23);
		case 18:
			return precpred(_ctx, 22);
		case 19:
			return precpred(_ctx, 21);
		case 20:
			return precpred(_ctx, 20);
		case 21:
			return precpred(_ctx, 19);
		case 22:
			return precpred(_ctx, 18);
		case 23:
			return precpred(_ctx, 17);
		case 24:
			return precpred(_ctx, 16);
		case 25:
			return precpred(_ctx, 15);
		case 26:
			return precpred(_ctx, 14);
		case 27:
			return precpred(_ctx, 13);
		case 28:
			return precpred(_ctx, 12);
		case 29:
			return precpred(_ctx, 11);
		case 30:
			return precpred(_ctx, 47);
		case 31:
			return precpred(_ctx, 46);
		case 32:
			return precpred(_ctx, 42);
		case 33:
			return precpred(_ctx, 39);
		case 34:
			return this.notLineTerminator();
		case 35:
			return precpred(_ctx, 38);
		case 36:
			return this.notLineTerminator();
		case 37:
			return precpred(_ctx, 9);
		}
		return true;
	}
	private boolean getter_sempred(GetterContext _localctx, int predIndex) {
		switch (predIndex) {
		case 38:
			return this.n("get");
		}
		return true;
	}
	private boolean setter_sempred(SetterContext _localctx, int predIndex) {
		switch (predIndex) {
		case 39:
			return this.n("set");
		}
		return true;
	}
	private boolean eos_sempred(EosContext _localctx, int predIndex) {
		switch (predIndex) {
		case 40:
			return this.lineTerminatorAhead();
		case 41:
			return this.closeBrace();
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001\u0085\u04db\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
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
		"T\u0002U\u0007U\u0002V\u0007V\u0002W\u0007W\u0002X\u0007X\u0002Y\u0007"+
		"Y\u0002Z\u0007Z\u0002[\u0007[\u0002\\\u0007\\\u0002]\u0007]\u0002^\u0007"+
		"^\u0002_\u0007_\u0002`\u0007`\u0002a\u0007a\u0002b\u0007b\u0001\u0000"+
		"\u0003\u0000\u00c8\b\u0000\u0001\u0000\u0003\u0000\u00cb\b\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002\u0005\u0002\u00d2"+
		"\b\u0002\n\u0002\f\u0002\u00d5\t\u0002\u0001\u0002\u0001\u0002\u0001\u0003"+
		"\u0003\u0003\u00da\b\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0003\u0004\u00e2\b\u0004\u0001\u0004\u0003\u0004"+
		"\u00e5\b\u0004\u0001\u0005\u0003\u0005\u00e8\b\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0003\u0006\u0100\b\u0006\u0001\u0007\u0001"+
		"\u0007\u0003\u0007\u0104\b\u0007\u0001\u0007\u0001\u0007\u0001\b\u0004"+
		"\b\u0109\b\b\u000b\b\f\b\u010a\u0001\t\u0001\t\u0001\t\u0001\n\u0003\n"+
		"\u0111\b\n\u0001\n\u0001\n\u0003\n\u0115\b\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0003\n\u011c\b\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0005\u000b\u0122\b\u000b\n\u000b\f\u000b\u0125\t\u000b\u0001\u000b"+
		"\u0001\u000b\u0003\u000b\u0129\b\u000b\u0003\u000b\u012b\b\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\f\u0001\f\u0001\f\u0003\f\u0132\b\f\u0001\r\u0001"+
		"\r\u0003\r\u0136\b\r\u0001\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u0010\u0001\u0010\u0003\u0010\u013f\b\u0010\u0001\u0010\u0001"+
		"\u0010\u0003\u0010\u0143\b\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0012\u0001\u0012\u0001\u0012\u0003\u0012\u014b\b\u0012\u0001\u0013\u0001"+
		"\u0013\u0003\u0013\u014f\b\u0013\u0001\u0013\u0001\u0013\u0003\u0013\u0153"+
		"\b\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0003\u0013\u015c\b\u0013\u0001\u0014\u0001\u0014\u0001"+
		"\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0003\u0014\u0164\b\u0014\u0001"+
		"\u0014\u0001\u0014\u0003\u0014\u0168\b\u0014\u0001\u0015\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0005\u0015\u016e\b\u0015\n\u0015\f\u0015\u0171\t\u0015"+
		"\u0001\u0015\u0001\u0015\u0003\u0015\u0175\b\u0015\u0003\u0015\u0177\b"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0001\u0016\u0003"+
		"\u0016\u017e\b\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0003\u0017\u0183"+
		"\b\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0019\u0001\u0019\u0001"+
		"\u0019\u0001\u0019\u0005\u0019\u018c\b\u0019\n\u0019\f\u0019\u018f\t\u0019"+
		"\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001b\u0001\u001b\u0001\u001b"+
		"\u0003\u001b\u0197\b\u001b\u0001\u001c\u0001\u001c\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0003\u001e\u01a6\b\u001e\u0001\u001f"+
		"\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f"+
		"\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f"+
		"\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0003\u001f"+
		"\u01ba\b\u001f\u0001\u001f\u0001\u001f\u0003\u001f\u01be\b\u001f\u0001"+
		"\u001f\u0001\u001f\u0003\u001f\u01c2\b\u001f\u0001\u001f\u0001\u001f\u0001"+
		"\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0003\u001f\u01ca\b\u001f\u0001"+
		"\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001"+
		"\u001f\u0003\u001f\u01d3\b\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0003"+
		"\u001f\u01d8\b\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001"+
		"\u001f\u0003\u001f\u01df\b\u001f\u0001 \u0001 \u0001 \u0003 \u01e4\b "+
		"\u0001!\u0001!\u0001!\u0003!\u01e9\b!\u0001!\u0001!\u0001\"\u0001\"\u0001"+
		"\"\u0003\"\u01f0\b\"\u0001\"\u0001\"\u0001#\u0001#\u0001#\u0003#\u01f7"+
		"\b#\u0001#\u0001#\u0001$\u0001$\u0001$\u0003$\u01fe\b$\u0001$\u0001$\u0001"+
		"%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001&\u0001&\u0001&\u0001&\u0001"+
		"&\u0001&\u0001\'\u0001\'\u0003\'\u0210\b\'\u0001\'\u0001\'\u0003\'\u0214"+
		"\b\'\u0003\'\u0216\b\'\u0001\'\u0001\'\u0001(\u0004(\u021b\b(\u000b(\f"+
		"(\u021c\u0001)\u0001)\u0001)\u0001)\u0003)\u0223\b)\u0001*\u0001*\u0001"+
		"*\u0003*\u0228\b*\u0001+\u0001+\u0001+\u0001+\u0001,\u0001,\u0001,\u0001"+
		",\u0001,\u0001-\u0001-\u0001-\u0001-\u0003-\u0237\b-\u0001-\u0003-\u023a"+
		"\b-\u0001.\u0001.\u0001.\u0003.\u023f\b.\u0001.\u0003.\u0242\b.\u0001"+
		".\u0001.\u0001/\u0001/\u0001/\u00010\u00010\u00010\u00011\u00031\u024d"+
		"\b1\u00011\u00011\u00031\u0251\b1\u00011\u00011\u00011\u00031\u0256\b"+
		"1\u00011\u00011\u00011\u00012\u00012\u00012\u00012\u00013\u00013\u0003"+
		"3\u0261\b3\u00013\u00013\u00053\u0265\b3\n3\f3\u0268\t3\u00013\u00013"+
		"\u00014\u00014\u00014\u00034\u026f\b4\u00014\u00014\u00014\u00014\u0003"+
		"4\u0275\b4\u00014\u00014\u00014\u00014\u00034\u027b\b4\u00014\u00014\u0003"+
		"4\u027f\b4\u00015\u00015\u00035\u0283\b5\u00015\u00035\u0286\b5\u0001"+
		"5\u00015\u00015\u00035\u028b\b5\u00015\u00015\u00015\u00015\u00035\u0291"+
		"\b5\u00015\u00015\u00015\u00015\u00015\u00015\u00035\u0299\b5\u00015\u0001"+
		"5\u00015\u00035\u029e\b5\u00015\u00015\u00015\u00035\u02a3\b5\u00016\u0001"+
		"6\u00036\u02a7\b6\u00017\u00017\u00037\u02ab\b7\u00018\u00018\u00018\u0001"+
		"9\u00019\u00019\u00059\u02b3\b9\n9\f9\u02b6\t9\u00019\u00019\u00039\u02ba"+
		"\b9\u00019\u00039\u02bd\b9\u0001:\u0001:\u0001:\u0003:\u02c2\b:\u0001"+
		";\u0001;\u0001;\u0001<\u0001<\u0003<\u02c9\b<\u0001<\u0001<\u0001=\u0004"+
		"=\u02ce\b=\u000b=\f=\u02cf\u0001>\u0001>\u0001>\u0001>\u0001>\u0001>\u0003"+
		">\u02d8\b>\u0001?\u0001?\u0001?\u0005?\u02dd\b?\n?\f?\u02e0\t?\u0001@"+
		"\u0001@\u0001@\u0003@\u02e5\b@\u0003@\u02e7\b@\u0001A\u0001A\u0001A\u0001"+
		"A\u0001A\u0001A\u0001A\u0001A\u0001A\u0001A\u0001A\u0003A\u02f4\bA\u0001"+
		"A\u0003A\u02f7\bA\u0001A\u0001A\u0001A\u0003A\u02fc\bA\u0001A\u0001A\u0001"+
		"A\u0001A\u0001A\u0001A\u0001A\u0001A\u0001A\u0001A\u0001A\u0001A\u0001"+
		"A\u0001A\u0001A\u0001A\u0001A\u0003A\u030f\bA\u0001B\u0001B\u0001B\u0001"+
		"B\u0001B\u0003B\u0316\bB\u0001B\u0001B\u0001B\u0001B\u0001B\u0001B\u0005"+
		"B\u031e\bB\nB\fB\u0321\tB\u0001B\u0001B\u0003B\u0325\bB\u0001B\u0003B"+
		"\u0328\bB\u0001B\u0001B\u0003B\u032c\bB\u0001C\u0001C\u0003C\u0330\bC"+
		"\u0001C\u0001C\u0001C\u0001C\u0003C\u0336\bC\u0001D\u0001D\u0001D\u0001"+
		"D\u0001D\u0003D\u033d\bD\u0001D\u0001D\u0001D\u0001D\u0001D\u0001D\u0003"+
		"D\u0345\bD\u0001D\u0003D\u0348\bD\u0001D\u0001D\u0003D\u034c\bD\u0001"+
		"E\u0001E\u0001E\u0005E\u0351\bE\nE\fE\u0354\tE\u0001E\u0001E\u0001E\u0001"+
		"E\u0005E\u035a\bE\nE\fE\u035d\tE\u0003E\u035f\bE\u0001F\u0003F\u0362\b"+
		"F\u0001G\u0001G\u0003G\u0366\bG\u0001H\u0001H\u0001H\u0001I\u0001I\u0001"+
		"I\u0001I\u0001I\u0001I\u0001I\u0003I\u0372\bI\u0001J\u0001J\u0001J\u0001"+
		"J\u0005J\u0378\bJ\nJ\fJ\u037b\tJ\u0001J\u0003J\u037e\bJ\u0003J\u0380\b"+
		"J\u0001J\u0001J\u0001K\u0003K\u0385\bK\u0001K\u0001K\u0003K\u0389\bK\u0001"+
		"L\u0001L\u0001L\u0001L\u0001L\u0001L\u0005L\u0391\bL\nL\fL\u0394\tL\u0001"+
		"M\u0001M\u0001N\u0001N\u0001N\u0001N\u0003N\u039c\bN\u0001N\u0001N\u0001"+
		"N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001"+
		"N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001"+
		"N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001"+
		"N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001"+
		"N\u0001N\u0001N\u0003N\u03cb\bN\u0001N\u0001N\u0001N\u0001N\u0001N\u0001"+
		"N\u0001N\u0001N\u0001N\u0001N\u0003N\u03d7\bN\u0001N\u0001N\u0001N\u0001"+
		"N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001"+
		"N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001"+
		"N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001"+
		"N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001"+
		"N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001"+
		"N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0003N\u0415\bN\u0001N\u0001"+
		"N\u0001N\u0001N\u0001N\u0001N\u0003N\u041d\bN\u0001N\u0001N\u0003N\u0421"+
		"\bN\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001N\u0001"+
		"N\u0001N\u0005N\u042e\bN\nN\fN\u0431\tN\u0001O\u0001O\u0001O\u0001P\u0001"+
		"P\u0001P\u0001P\u0003P\u043a\bP\u0001Q\u0001Q\u0001Q\u0001Q\u0005Q\u0440"+
		"\bQ\nQ\fQ\u0443\tQ\u0001Q\u0003Q\u0446\bQ\u0003Q\u0448\bQ\u0001Q\u0001"+
		"Q\u0001R\u0001R\u0003R\u044e\bR\u0001R\u0001R\u0003R\u0452\bR\u0001R\u0001"+
		"R\u0003R\u0456\bR\u0001R\u0001R\u0001R\u0003R\u045b\bR\u0001R\u0001R\u0001"+
		"R\u0001R\u0003R\u0461\bR\u0001S\u0001S\u0001S\u0003S\u0466\bS\u0001S\u0003"+
		"S\u0469\bS\u0001T\u0001T\u0003T\u046d\bT\u0001U\u0001U\u0001V\u0001V\u0001"+
		"V\u0001V\u0001V\u0001V\u0001V\u0003V\u0478\bV\u0001W\u0001W\u0005W\u047c"+
		"\bW\nW\fW\u047f\tW\u0001W\u0001W\u0001X\u0001X\u0001X\u0001X\u0001X\u0003"+
		"X\u0488\bX\u0001Y\u0001Y\u0001Z\u0001Z\u0001[\u0001[\u0001[\u0001[\u0001"+
		"\\\u0001\\\u0001\\\u0001\\\u0001]\u0001]\u0003]\u0498\b]\u0001^\u0001"+
		"^\u0001_\u0001_\u0001_\u0003_\u049f\b_\u0001`\u0001`\u0001`\u0001`\u0001"+
		"`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001"+
		"`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001"+
		"`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001"+
		"`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001`\u0001"+
		"`\u0001`\u0001`\u0001`\u0003`\u04d1\b`\u0001a\u0001a\u0001b\u0001b\u0001"+
		"b\u0001b\u0003b\u04d9\bb\u0001b\u0000\u0002\u0098\u009cc\u0000\u0002\u0004"+
		"\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \""+
		"$&(*,.02468:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086"+
		"\u0088\u008a\u008c\u008e\u0090\u0092\u0094\u0096\u0098\u009a\u009c\u009e"+
		"\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6"+
		"\u00b8\u00ba\u00bc\u00be\u00c0\u00c2\u00c4\u0000\f\u0003\u0000ffpp||\u0001"+
		"\u0000fg\u0001\u0000\u001a\u001c\u0001\u0000\u0016\u0017\u0001\u0000 "+
		"\"\u0001\u0000#&\u0001\u0000\'*\u0001\u00000<\u0001\u0000@D\u0001\u0000"+
		"EH\u0004\u0000cfoossz|\u0001\u0000rs\u058e\u0000\u00c7\u0001\u0000\u0000"+
		"\u0000\u0002\u00ce\u0001\u0000\u0000\u0000\u0004\u00d3\u0001\u0000\u0000"+
		"\u0000\u0006\u00d9\u0001\u0000\u0000\u0000\b\u00e4\u0001\u0000\u0000\u0000"+
		"\n\u00e7\u0001\u0000\u0000\u0000\f\u00ff\u0001\u0000\u0000\u0000\u000e"+
		"\u0101\u0001\u0000\u0000\u0000\u0010\u0108\u0001\u0000\u0000\u0000\u0012"+
		"\u010c\u0001\u0000\u0000\u0000\u0014\u011b\u0001\u0000\u0000\u0000\u0016"+
		"\u011d\u0001\u0000\u0000\u0000\u0018\u012e\u0001\u0000\u0000\u0000\u001a"+
		"\u0135\u0001\u0000\u0000\u0000\u001c\u0137\u0001\u0000\u0000\u0000\u001e"+
		"\u0139\u0001\u0000\u0000\u0000 \u013e\u0001\u0000\u0000\u0000\"\u0144"+
		"\u0001\u0000\u0000\u0000$\u0147\u0001\u0000\u0000\u0000&\u015b\u0001\u0000"+
		"\u0000\u0000(\u0167\u0001\u0000\u0000\u0000*\u0169\u0001\u0000\u0000\u0000"+
		",\u017a\u0001\u0000\u0000\u0000.\u0182\u0001\u0000\u0000\u00000\u0184"+
		"\u0001\u0000\u0000\u00002\u0187\u0001\u0000\u0000\u00004\u0190\u0001\u0000"+
		"\u0000\u00006\u0193\u0001\u0000\u0000\u00008\u0198\u0001\u0000\u0000\u0000"+
		":\u019a\u0001\u0000\u0000\u0000<\u019e\u0001\u0000\u0000\u0000>\u01de"+
		"\u0001\u0000\u0000\u0000@\u01e3\u0001\u0000\u0000\u0000B\u01e5\u0001\u0000"+
		"\u0000\u0000D\u01ec\u0001\u0000\u0000\u0000F\u01f3\u0001\u0000\u0000\u0000"+
		"H\u01fa\u0001\u0000\u0000\u0000J\u0201\u0001\u0000\u0000\u0000L\u0207"+
		"\u0001\u0000\u0000\u0000N\u020d\u0001\u0000\u0000\u0000P\u021a\u0001\u0000"+
		"\u0000\u0000R\u021e\u0001\u0000\u0000\u0000T\u0224\u0001\u0000\u0000\u0000"+
		"V\u0229\u0001\u0000\u0000\u0000X\u022d\u0001\u0000\u0000\u0000Z\u0232"+
		"\u0001\u0000\u0000\u0000\\\u023b\u0001\u0000\u0000\u0000^\u0245\u0001"+
		"\u0000\u0000\u0000`\u0248\u0001\u0000\u0000\u0000b\u024c\u0001\u0000\u0000"+
		"\u0000d\u025a\u0001\u0000\u0000\u0000f\u0260\u0001\u0000\u0000\u0000h"+
		"\u027e\u0001\u0000\u0000\u0000j\u02a2\u0001\u0000\u0000\u0000l\u02a4\u0001"+
		"\u0000\u0000\u0000n\u02aa\u0001\u0000\u0000\u0000p\u02ac\u0001\u0000\u0000"+
		"\u0000r\u02bc\u0001\u0000\u0000\u0000t\u02be\u0001\u0000\u0000\u0000v"+
		"\u02c3\u0001\u0000\u0000\u0000x\u02c6\u0001\u0000\u0000\u0000z\u02cd\u0001"+
		"\u0000\u0000\u0000|\u02d7\u0001\u0000\u0000\u0000~\u02d9\u0001\u0000\u0000"+
		"\u0000\u0080\u02e6\u0001\u0000\u0000\u0000\u0082\u030e\u0001\u0000\u0000"+
		"\u0000\u0084\u032b\u0001\u0000\u0000\u0000\u0086\u0335\u0001\u0000\u0000"+
		"\u0000\u0088\u034b\u0001\u0000\u0000\u0000\u008a\u035e\u0001\u0000\u0000"+
		"\u0000\u008c\u0361\u0001\u0000\u0000\u0000\u008e\u0363\u0001\u0000\u0000"+
		"\u0000\u0090\u0367\u0001\u0000\u0000\u0000\u0092\u0371\u0001\u0000\u0000"+
		"\u0000\u0094\u0373\u0001\u0000\u0000\u0000\u0096\u0384\u0001\u0000\u0000"+
		"\u0000\u0098\u038a\u0001\u0000\u0000\u0000\u009a\u0395\u0001\u0000\u0000"+
		"\u0000\u009c\u03d6\u0001\u0000\u0000\u0000\u009e\u0432\u0001\u0000\u0000"+
		"\u0000\u00a0\u0439\u0001\u0000\u0000\u0000\u00a2\u043b\u0001\u0000\u0000"+
		"\u0000\u00a4\u0460\u0001\u0000\u0000\u0000\u00a6\u0468\u0001\u0000\u0000"+
		"\u0000\u00a8\u046c\u0001\u0000\u0000\u0000\u00aa\u046e\u0001\u0000\u0000"+
		"\u0000\u00ac\u0477\u0001\u0000\u0000\u0000\u00ae\u0479\u0001\u0000\u0000"+
		"\u0000\u00b0\u0487\u0001\u0000\u0000\u0000\u00b2\u0489\u0001\u0000\u0000"+
		"\u0000\u00b4\u048b\u0001\u0000\u0000\u0000\u00b6\u048d\u0001\u0000\u0000"+
		"\u0000\u00b8\u0491\u0001\u0000\u0000\u0000\u00ba\u0497\u0001\u0000\u0000"+
		"\u0000\u00bc\u0499\u0001\u0000\u0000\u0000\u00be\u049e\u0001\u0000\u0000"+
		"\u0000\u00c0\u04d0\u0001\u0000\u0000\u0000\u00c2\u04d2\u0001\u0000\u0000"+
		"\u0000\u00c4\u04d8\u0001\u0000\u0000\u0000\u00c6\u00c8\u0005\u0001\u0000"+
		"\u0000\u00c7\u00c6\u0001\u0000\u0000\u0000\u00c7\u00c8\u0001\u0000\u0000"+
		"\u0000\u00c8\u00ca\u0001\u0000\u0000\u0000\u00c9\u00cb\u0003z=\u0000\u00ca"+
		"\u00c9\u0001\u0000\u0000\u0000\u00ca\u00cb\u0001\u0000\u0000\u0000\u00cb"+
		"\u00cc\u0001\u0000\u0000\u0000\u00cc\u00cd\u0005\u0000\u0000\u0001\u00cd"+
		"\u0001\u0001\u0000\u0000\u0000\u00ce\u00cf\u0003\f\u0006\u0000\u00cf\u0003"+
		"\u0001\u0000\u0000\u0000\u00d0\u00d2\u0003\f\u0006\u0000\u00d1\u00d0\u0001"+
		"\u0000\u0000\u0000\u00d2\u00d5\u0001\u0000\u0000\u0000\u00d3\u00d1\u0001"+
		"\u0000\u0000\u0000\u00d3\u00d4\u0001\u0000\u0000\u0000\u00d4\u00d6\u0001"+
		"\u0000\u0000\u0000\u00d5\u00d3\u0001\u0000\u0000\u0000\u00d6\u00d7\u0005"+
		"\u0000\u0000\u0001\u00d7\u0005\u0001\u0000\u0000\u0000\u00d8\u00da\u0003"+
		"\u009aM\u0000\u00d9\u00d8\u0001\u0000\u0000\u0000\u00d9\u00da\u0001\u0000"+
		"\u0000\u0000\u00da\u00db\u0001\u0000\u0000\u0000\u00db\u00dc\u0005\u0000"+
		"\u0000\u0001\u00dc\u0007\u0001\u0000\u0000\u0000\u00dd\u00de\u0003\u009a"+
		"M\u0000\u00de\u00df\u0005\u0000\u0000\u0001\u00df\u00e5\u0001\u0000\u0000"+
		"\u0000\u00e0\u00e2\u0003\u0010\b\u0000\u00e1\u00e0\u0001\u0000\u0000\u0000"+
		"\u00e1\u00e2\u0001\u0000\u0000\u0000\u00e2\u00e3\u0001\u0000\u0000\u0000"+
		"\u00e3\u00e5\u0005\u0000\u0000\u0001\u00e4\u00dd\u0001\u0000\u0000\u0000"+
		"\u00e4\u00e1\u0001\u0000\u0000\u0000\u00e5\t\u0001\u0000\u0000\u0000\u00e6"+
		"\u00e8\u0003b1\u0000\u00e7\u00e6\u0001\u0000\u0000\u0000\u00e7\u00e8\u0001"+
		"\u0000\u0000\u0000\u00e8\u00e9\u0001\u0000\u0000\u0000\u00e9\u00ea\u0005"+
		"\u0000\u0000\u0001\u00ea\u000b\u0001\u0000\u0000\u0000\u00eb\u0100\u0003"+
		"\u000e\u0007\u0000\u00ec\u0100\u00030\u0018\u0000\u00ed\u0100\u0003\u0012"+
		"\t\u0000\u00ee\u0100\u0003&\u0013\u0000\u00ef\u0100\u00038\u001c\u0000"+
		"\u00f0\u0100\u0003d2\u0000\u00f1\u0100\u0003b1\u0000\u00f2\u0100\u0003"+
		":\u001d\u0000\u00f3\u0100\u0003<\u001e\u0000\u00f4\u0100\u0003>\u001f"+
		"\u0000\u00f5\u0100\u0003B!\u0000\u00f6\u0100\u0003D\"\u0000\u00f7\u0100"+
		"\u0003F#\u0000\u00f8\u0100\u0003H$\u0000\u00f9\u0100\u0003J%\u0000\u00fa"+
		"\u0100\u0003V+\u0000\u00fb\u0100\u0003L&\u0000\u00fc\u0100\u0003X,\u0000"+
		"\u00fd\u0100\u0003Z-\u0000\u00fe\u0100\u0003`0\u0000\u00ff\u00eb\u0001"+
		"\u0000\u0000\u0000\u00ff\u00ec\u0001\u0000\u0000\u0000\u00ff\u00ed\u0001"+
		"\u0000\u0000\u0000\u00ff\u00ee\u0001\u0000\u0000\u0000\u00ff\u00ef\u0001"+
		"\u0000\u0000\u0000\u00ff\u00f0\u0001\u0000\u0000\u0000\u00ff\u00f1\u0001"+
		"\u0000\u0000\u0000\u00ff\u00f2\u0001\u0000\u0000\u0000\u00ff\u00f3\u0001"+
		"\u0000\u0000\u0000\u00ff\u00f4\u0001\u0000\u0000\u0000\u00ff\u00f5\u0001"+
		"\u0000\u0000\u0000\u00ff\u00f6\u0001\u0000\u0000\u0000\u00ff\u00f7\u0001"+
		"\u0000\u0000\u0000\u00ff\u00f8\u0001\u0000\u0000\u0000\u00ff\u00f9\u0001"+
		"\u0000\u0000\u0000\u00ff\u00fa\u0001\u0000\u0000\u0000\u00ff\u00fb\u0001"+
		"\u0000\u0000\u0000\u00ff\u00fc\u0001\u0000\u0000\u0000\u00ff\u00fd\u0001"+
		"\u0000\u0000\u0000\u00ff\u00fe\u0001\u0000\u0000\u0000\u0100\r\u0001\u0000"+
		"\u0000\u0000\u0101\u0103\u0005\t\u0000\u0000\u0102\u0104\u0003\u0010\b"+
		"\u0000\u0103\u0102\u0001\u0000\u0000\u0000\u0103\u0104\u0001\u0000\u0000"+
		"\u0000\u0104\u0105\u0001\u0000\u0000\u0000\u0105\u0106\u0005\u000b\u0000"+
		"\u0000\u0106\u000f\u0001\u0000\u0000\u0000\u0107\u0109\u0003\f\u0006\u0000"+
		"\u0108\u0107\u0001\u0000\u0000\u0000\u0109\u010a\u0001\u0000\u0000\u0000"+
		"\u010a\u0108\u0001\u0000\u0000\u0000\u010a\u010b\u0001\u0000\u0000\u0000"+
		"\u010b\u0011\u0001\u0000\u0000\u0000\u010c\u010d\u0005n\u0000\u0000\u010d"+
		"\u010e\u0003\u0014\n\u0000\u010e\u0013\u0001\u0000\u0000\u0000\u010f\u0111"+
		"\u0003\u001e\u000f\u0000\u0110\u010f\u0001\u0000\u0000\u0000\u0110\u0111"+
		"\u0001\u0000\u0000\u0000\u0111\u0114\u0001\u0000\u0000\u0000\u0112\u0115"+
		"\u0003 \u0010\u0000\u0113\u0115\u0003\u0016\u000b\u0000\u0114\u0112\u0001"+
		"\u0000\u0000\u0000\u0114\u0113\u0001\u0000\u0000\u0000\u0115\u0116\u0001"+
		"\u0000\u0000\u0000\u0116\u0117\u0003\"\u0011\u0000\u0117\u0118\u0003\u00c4"+
		"b\u0000\u0118\u011c\u0001\u0000\u0000\u0000\u0119\u011a\u0005}\u0000\u0000"+
		"\u011a\u011c\u0003\u00c4b\u0000\u011b\u0110\u0001\u0000\u0000\u0000\u011b"+
		"\u0119\u0001\u0000\u0000\u0000\u011c\u0015\u0001\u0000\u0000\u0000\u011d"+
		"\u0123\u0005\t\u0000\u0000\u011e\u011f\u0003\u0018\f\u0000\u011f\u0120"+
		"\u0005\r\u0000\u0000\u0120\u0122\u0001\u0000\u0000\u0000\u0121\u011e\u0001"+
		"\u0000\u0000\u0000\u0122\u0125\u0001\u0000\u0000\u0000\u0123\u0121\u0001"+
		"\u0000\u0000\u0000\u0123\u0124\u0001\u0000\u0000\u0000\u0124\u012a\u0001"+
		"\u0000\u0000\u0000\u0125\u0123\u0001\u0000\u0000\u0000\u0126\u0128\u0003"+
		"\u0018\f\u0000\u0127\u0129\u0005\r\u0000\u0000\u0128\u0127\u0001\u0000"+
		"\u0000\u0000\u0128\u0129\u0001\u0000\u0000\u0000\u0129\u012b\u0001\u0000"+
		"\u0000\u0000\u012a\u0126\u0001\u0000\u0000\u0000\u012a\u012b\u0001\u0000"+
		"\u0000\u0000\u012b\u012c\u0001\u0000\u0000\u0000\u012c\u012d\u0005\u000b"+
		"\u0000\u0000\u012d\u0017\u0001\u0000\u0000\u0000\u012e\u0131\u0003\u001a"+
		"\r\u0000\u012f\u0130\u0005c\u0000\u0000\u0130\u0132\u0003\u001c\u000e"+
		"\u0000\u0131\u012f\u0001\u0000\u0000\u0000\u0131\u0132\u0001\u0000\u0000"+
		"\u0000\u0132\u0019\u0001\u0000\u0000\u0000\u0133\u0136\u0003\u00ba]\u0000"+
		"\u0134\u0136\u0005}\u0000\u0000\u0135\u0133\u0001\u0000\u0000\u0000\u0135"+
		"\u0134\u0001\u0000\u0000\u0000\u0136\u001b\u0001\u0000\u0000\u0000\u0137"+
		"\u0138\u0007\u0000\u0000\u0000\u0138\u001d\u0001\u0000\u0000\u0000\u0139"+
		"\u013a\u0003$\u0012\u0000\u013a\u013b\u0005\r\u0000\u0000\u013b\u001f"+
		"\u0001\u0000\u0000\u0000\u013c\u013f\u0005\u001a\u0000\u0000\u013d\u013f"+
		"\u0003\u00ba]\u0000\u013e\u013c\u0001\u0000\u0000\u0000\u013e\u013d\u0001"+
		"\u0000\u0000\u0000\u013f\u0142\u0001\u0000\u0000\u0000\u0140\u0141\u0005"+
		"c\u0000\u0000\u0141\u0143\u0003\u00ba]\u0000\u0142\u0140\u0001\u0000\u0000"+
		"\u0000\u0142\u0143\u0001\u0000\u0000\u0000\u0143!\u0001\u0000\u0000\u0000"+
		"\u0144\u0145\u0005d\u0000\u0000\u0145\u0146\u0005}\u0000\u0000\u0146#"+
		"\u0001\u0000\u0000\u0000\u0147\u014a\u0003\u00ba]\u0000\u0148\u0149\u0005"+
		"c\u0000\u0000\u0149\u014b\u0003\u00ba]\u0000\u014a\u0148\u0001\u0000\u0000"+
		"\u0000\u014a\u014b\u0001\u0000\u0000\u0000\u014b%\u0001\u0000\u0000\u0000"+
		"\u014c\u014e\u0005m\u0000\u0000\u014d\u014f\u0005]\u0000\u0000\u014e\u014d"+
		"\u0001\u0000\u0000\u0000\u014e\u014f\u0001\u0000\u0000\u0000\u014f\u0152"+
		"\u0001\u0000\u0000\u0000\u0150\u0153\u0003(\u0014\u0000\u0151\u0153\u0003"+
		".\u0017\u0000\u0152\u0150\u0001\u0000\u0000\u0000\u0152\u0151\u0001\u0000"+
		"\u0000\u0000\u0153\u0154\u0001\u0000\u0000\u0000\u0154\u0155\u0003\u00c4"+
		"b\u0000\u0155\u015c\u0001\u0000\u0000\u0000\u0156\u0157\u0005m\u0000\u0000"+
		"\u0157\u0158\u0005]\u0000\u0000\u0158\u0159\u0003\u009aM\u0000\u0159\u015a"+
		"\u0003\u00c4b\u0000\u015a\u015c\u0001\u0000\u0000\u0000\u015b\u014c\u0001"+
		"\u0000\u0000\u0000\u015b\u0156\u0001\u0000\u0000\u0000\u015c\'\u0001\u0000"+
		"\u0000\u0000\u015d\u015e\u0003 \u0010\u0000\u015e\u015f\u0003\"\u0011"+
		"\u0000\u015f\u0160\u0003\u00c4b\u0000\u0160\u0168\u0001\u0000\u0000\u0000"+
		"\u0161\u0163\u0003*\u0015\u0000\u0162\u0164\u0003\"\u0011\u0000\u0163"+
		"\u0162\u0001\u0000\u0000\u0000\u0163\u0164\u0001\u0000\u0000\u0000\u0164"+
		"\u0165\u0001\u0000\u0000\u0000\u0165\u0166\u0003\u00c4b\u0000\u0166\u0168"+
		"\u0001\u0000\u0000\u0000\u0167\u015d\u0001\u0000\u0000\u0000\u0167\u0161"+
		"\u0001\u0000\u0000\u0000\u0168)\u0001\u0000\u0000\u0000\u0169\u016f\u0005"+
		"\t\u0000\u0000\u016a\u016b\u0003,\u0016\u0000\u016b\u016c\u0005\r\u0000"+
		"\u0000\u016c\u016e\u0001\u0000\u0000\u0000\u016d\u016a\u0001\u0000\u0000"+
		"\u0000\u016e\u0171\u0001\u0000\u0000\u0000\u016f\u016d\u0001\u0000\u0000"+
		"\u0000\u016f\u0170\u0001\u0000\u0000\u0000\u0170\u0176\u0001\u0000\u0000"+
		"\u0000\u0171\u016f\u0001\u0000\u0000\u0000\u0172\u0174\u0003,\u0016\u0000"+
		"\u0173\u0175\u0005\r\u0000\u0000\u0174\u0173\u0001\u0000\u0000\u0000\u0174"+
		"\u0175\u0001\u0000\u0000\u0000\u0175\u0177\u0001\u0000\u0000\u0000\u0176"+
		"\u0172\u0001\u0000\u0000\u0000\u0176\u0177\u0001\u0000\u0000\u0000\u0177"+
		"\u0178\u0001\u0000\u0000\u0000\u0178\u0179\u0005\u000b\u0000\u0000\u0179"+
		"+\u0001\u0000\u0000\u0000\u017a\u017d\u0003\u001a\r\u0000\u017b\u017c"+
		"\u0005c\u0000\u0000\u017c\u017e\u0003\u001a\r\u0000\u017d\u017b\u0001"+
		"\u0000\u0000\u0000\u017d\u017e\u0001\u0000\u0000\u0000\u017e-\u0001\u0000"+
		"\u0000\u0000\u017f\u0183\u00030\u0018\u0000\u0180\u0183\u0003d2\u0000"+
		"\u0181\u0183\u0003b1\u0000\u0182\u017f\u0001\u0000\u0000\u0000\u0182\u0180"+
		"\u0001\u0000\u0000\u0000\u0182\u0181\u0001\u0000\u0000\u0000\u0183/\u0001"+
		"\u0000\u0000\u0000\u0184\u0185\u00032\u0019\u0000\u0185\u0186\u0003\u00c4"+
		"b\u0000\u01861\u0001\u0000\u0000\u0000\u0187\u0188\u0003@ \u0000\u0188"+
		"\u018d\u00036\u001b\u0000\u0189\u018a\u0005\r\u0000\u0000\u018a\u018c"+
		"\u00036\u001b\u0000\u018b\u0189\u0001\u0000\u0000\u0000\u018c\u018f\u0001"+
		"\u0000\u0000\u0000\u018d\u018b\u0001\u0000\u0000\u0000\u018d\u018e\u0001"+
		"\u0000\u0000\u0000\u018e3\u0001\u0000\u0000\u0000\u018f\u018d\u0001\u0000"+
		"\u0000\u0000\u0190\u0191\u0003@ \u0000\u0191\u0192\u00036\u001b\u0000"+
		"\u01925\u0001\u0000\u0000\u0000\u0193\u0196\u0003\u00a0P\u0000\u0194\u0195"+
		"\u0005\u000e\u0000\u0000\u0195\u0197\u0003\u009aM\u0000\u0196\u0194\u0001"+
		"\u0000\u0000\u0000\u0196\u0197\u0001\u0000\u0000\u0000\u01977\u0001\u0000"+
		"\u0000\u0000\u0198\u0199\u0005\f\u0000\u0000\u01999\u0001\u0000\u0000"+
		"\u0000\u019a\u019b\u0004\u001d\u0000\u0000\u019b\u019c\u0003\u0098L\u0000"+
		"\u019c\u019d\u0003\u00c4b\u0000\u019d;\u0001\u0000\u0000\u0000\u019e\u019f"+
		"\u0005^\u0000\u0000\u019f\u01a0\u0005\u0007\u0000\u0000\u01a0\u01a1\u0003"+
		"\u0098L\u0000\u01a1\u01a2\u0005\b\u0000\u0000\u01a2\u01a5\u0003\f\u0006"+
		"\u0000\u01a3\u01a4\u0005N\u0000\u0000\u01a4\u01a6\u0003\f\u0006\u0000"+
		"\u01a5\u01a3\u0001\u0000\u0000\u0000\u01a5\u01a6\u0001\u0000\u0000\u0000"+
		"\u01a6=\u0001\u0000\u0000\u0000\u01a7\u01a8\u0005J\u0000\u0000\u01a8\u01a9"+
		"\u0003\f\u0006\u0000\u01a9\u01aa\u0005X\u0000\u0000\u01aa\u01ab\u0005"+
		"\u0007\u0000\u0000\u01ab\u01ac\u0003\u0098L\u0000\u01ac\u01ad\u0005\b"+
		"\u0000\u0000\u01ad\u01ae\u0003\u00c4b\u0000\u01ae\u01df\u0001\u0000\u0000"+
		"\u0000\u01af\u01b0\u0005X\u0000\u0000\u01b0\u01b1\u0005\u0007\u0000\u0000"+
		"\u01b1\u01b2\u0003\u0098L\u0000\u01b2\u01b3\u0005\b\u0000\u0000\u01b3"+
		"\u01b4\u0003\f\u0006\u0000\u01b4\u01df\u0001\u0000\u0000\u0000\u01b5\u01b6"+
		"\u0005V\u0000\u0000\u01b6\u01b9\u0005\u0007\u0000\u0000\u01b7\u01ba\u0003"+
		"\u0098L\u0000\u01b8\u01ba\u00032\u0019\u0000\u01b9\u01b7\u0001\u0000\u0000"+
		"\u0000\u01b9\u01b8\u0001\u0000\u0000\u0000\u01b9\u01ba\u0001\u0000\u0000"+
		"\u0000\u01ba\u01bb\u0001\u0000\u0000\u0000\u01bb\u01bd\u0005\f\u0000\u0000"+
		"\u01bc\u01be\u0003\u0098L\u0000\u01bd\u01bc\u0001\u0000\u0000\u0000\u01bd"+
		"\u01be\u0001\u0000\u0000\u0000\u01be\u01bf\u0001\u0000\u0000\u0000\u01bf"+
		"\u01c1\u0005\f\u0000\u0000\u01c0\u01c2\u0003\u0098L\u0000\u01c1\u01c0"+
		"\u0001\u0000\u0000\u0000\u01c1\u01c2\u0001\u0000\u0000\u0000\u01c2\u01c3"+
		"\u0001\u0000\u0000\u0000\u01c3\u01c4\u0005\b\u0000\u0000\u01c4\u01df\u0003"+
		"\f\u0006\u0000\u01c5\u01c6\u0005V\u0000\u0000\u01c6\u01c9\u0005\u0007"+
		"\u0000\u0000\u01c7\u01ca\u0003\u009aM\u0000\u01c8\u01ca\u00034\u001a\u0000"+
		"\u01c9\u01c7\u0001\u0000\u0000\u0000\u01c9\u01c8\u0001\u0000\u0000\u0000"+
		"\u01ca\u01cb\u0001\u0000\u0000\u0000\u01cb\u01cc\u0005a\u0000\u0000\u01cc"+
		"\u01cd\u0003\u0098L\u0000\u01cd\u01ce\u0005\b\u0000\u0000\u01ce\u01cf"+
		"\u0003\f\u0006\u0000\u01cf\u01df\u0001\u0000\u0000\u0000\u01d0\u01d2\u0005"+
		"V\u0000\u0000\u01d1\u01d3\u0005p\u0000\u0000\u01d2\u01d1\u0001\u0000\u0000"+
		"\u0000\u01d2\u01d3\u0001\u0000\u0000\u0000\u01d3\u01d4\u0001\u0000\u0000"+
		"\u0000\u01d4\u01d7\u0005\u0007\u0000\u0000\u01d5\u01d8\u0003\u009aM\u0000"+
		"\u01d6\u01d8\u00034\u001a\u0000\u01d7\u01d5\u0001\u0000\u0000\u0000\u01d7"+
		"\u01d6\u0001\u0000\u0000\u0000\u01d8\u01d9\u0001\u0000\u0000\u0000\u01d9"+
		"\u01da\u0005e\u0000\u0000\u01da\u01db\u0003\u0098L\u0000\u01db\u01dc\u0005"+
		"\b\u0000\u0000\u01dc\u01dd\u0003\f\u0006\u0000\u01dd\u01df\u0001\u0000"+
		"\u0000\u0000\u01de\u01a7\u0001\u0000\u0000\u0000\u01de\u01af\u0001\u0000"+
		"\u0000\u0000\u01de\u01b5\u0001\u0000\u0000\u0000\u01de\u01c5\u0001\u0000"+
		"\u0000\u0000\u01de\u01d0\u0001\u0000\u0000\u0000\u01df?\u0001\u0000\u0000"+
		"\u0000\u01e0\u01e4\u0005P\u0000\u0000\u01e1\u01e4\u0003\u00c2a\u0000\u01e2"+
		"\u01e4\u0005l\u0000\u0000\u01e3\u01e0\u0001\u0000\u0000\u0000\u01e3\u01e1"+
		"\u0001\u0000\u0000\u0000\u01e3\u01e2\u0001\u0000\u0000\u0000\u01e4A\u0001"+
		"\u0000\u0000\u0000\u01e5\u01e8\u0005U\u0000\u0000\u01e6\u01e7\u0004!\u0001"+
		"\u0000\u01e7\u01e9\u0003\u00bc^\u0000\u01e8\u01e6\u0001\u0000\u0000\u0000"+
		"\u01e8\u01e9\u0001\u0000\u0000\u0000\u01e9\u01ea\u0001\u0000\u0000\u0000"+
		"\u01ea\u01eb\u0003\u00c4b\u0000\u01ebC\u0001\u0000\u0000\u0000\u01ec\u01ef"+
		"\u0005I\u0000\u0000\u01ed\u01ee\u0004\"\u0002\u0000\u01ee\u01f0\u0003"+
		"\u00bc^\u0000\u01ef\u01ed\u0001\u0000\u0000\u0000\u01ef\u01f0\u0001\u0000"+
		"\u0000\u0000\u01f0\u01f1\u0001\u0000\u0000\u0000\u01f1\u01f2\u0003\u00c4"+
		"b\u0000\u01f2E\u0001\u0000\u0000\u0000\u01f3\u01f6\u0005S\u0000\u0000"+
		"\u01f4\u01f5\u0004#\u0003\u0000\u01f5\u01f7\u0003\u0098L\u0000\u01f6\u01f4"+
		"\u0001\u0000\u0000\u0000\u01f6\u01f7\u0001\u0000\u0000\u0000\u01f7\u01f8"+
		"\u0001\u0000\u0000\u0000\u01f8\u01f9\u0003\u00c4b\u0000\u01f9G\u0001\u0000"+
		"\u0000\u0000\u01fa\u01fd\u0007\u0001\u0000\u0000\u01fb\u01fc\u0004$\u0004"+
		"\u0000\u01fc\u01fe\u0003\u0098L\u0000\u01fd\u01fb\u0001\u0000\u0000\u0000"+
		"\u01fd\u01fe\u0001\u0000\u0000\u0000\u01fe\u01ff\u0001\u0000\u0000\u0000"+
		"\u01ff\u0200\u0003\u00c4b\u0000\u0200I\u0001\u0000\u0000\u0000\u0201\u0202"+
		"\u0005\\\u0000\u0000\u0202\u0203\u0005\u0007\u0000\u0000\u0203\u0204\u0003"+
		"\u0098L\u0000\u0204\u0205\u0005\b\u0000\u0000\u0205\u0206\u0003\f\u0006"+
		"\u0000\u0206K\u0001\u0000\u0000\u0000\u0207\u0208\u0005W\u0000\u0000\u0208"+
		"\u0209\u0005\u0007\u0000\u0000\u0209\u020a\u0003\u0098L\u0000\u020a\u020b"+
		"\u0005\b\u0000\u0000\u020b\u020c\u0003N\'\u0000\u020cM\u0001\u0000\u0000"+
		"\u0000\u020d\u020f\u0005\t\u0000\u0000\u020e\u0210\u0003P(\u0000\u020f"+
		"\u020e\u0001\u0000\u0000\u0000\u020f\u0210\u0001\u0000\u0000\u0000\u0210"+
		"\u0215\u0001\u0000\u0000\u0000\u0211\u0213\u0003T*\u0000\u0212\u0214\u0003"+
		"P(\u0000\u0213\u0212\u0001\u0000\u0000\u0000\u0213\u0214\u0001\u0000\u0000"+
		"\u0000\u0214\u0216\u0001\u0000\u0000\u0000\u0215\u0211\u0001\u0000\u0000"+
		"\u0000\u0215\u0216\u0001\u0000\u0000\u0000\u0216\u0217\u0001\u0000\u0000"+
		"\u0000\u0217\u0218\u0005\u000b\u0000\u0000\u0218O\u0001\u0000\u0000\u0000"+
		"\u0219\u021b\u0003R)\u0000\u021a\u0219\u0001\u0000\u0000\u0000\u021b\u021c"+
		"\u0001\u0000\u0000\u0000\u021c\u021a\u0001\u0000\u0000\u0000\u021c\u021d"+
		"\u0001\u0000\u0000\u0000\u021dQ\u0001\u0000\u0000\u0000\u021e\u021f\u0005"+
		"M\u0000\u0000\u021f\u0220\u0003\u0098L\u0000\u0220\u0222\u0005\u0011\u0000"+
		"\u0000\u0221\u0223\u0003\u0010\b\u0000\u0222\u0221\u0001\u0000\u0000\u0000"+
		"\u0222\u0223\u0001\u0000\u0000\u0000\u0223S\u0001\u0000\u0000\u0000\u0224"+
		"\u0225\u0005]\u0000\u0000\u0225\u0227\u0005\u0011\u0000\u0000\u0226\u0228"+
		"\u0003\u0010\b\u0000\u0227\u0226\u0001\u0000\u0000\u0000\u0227\u0228\u0001"+
		"\u0000\u0000\u0000\u0228U\u0001\u0000\u0000\u0000\u0229\u022a\u0003\u00bc"+
		"^\u0000\u022a\u022b\u0005\u0011\u0000\u0000\u022b\u022c\u0003\f\u0006"+
		"\u0000\u022cW\u0001\u0000\u0000\u0000\u022d\u022e\u0005_\u0000\u0000\u022e"+
		"\u022f\u0004,\u0005\u0000\u022f\u0230\u0003\u0098L\u0000\u0230\u0231\u0003"+
		"\u00c4b\u0000\u0231Y\u0001\u0000\u0000\u0000\u0232\u0233\u0005b\u0000"+
		"\u0000\u0233\u0239\u0003\u000e\u0007\u0000\u0234\u0236\u0003\\.\u0000"+
		"\u0235\u0237\u0003^/\u0000\u0236\u0235\u0001\u0000\u0000\u0000\u0236\u0237"+
		"\u0001\u0000\u0000\u0000\u0237\u023a\u0001\u0000\u0000\u0000\u0238\u023a"+
		"\u0003^/\u0000\u0239\u0234\u0001\u0000\u0000\u0000\u0239\u0238\u0001\u0000"+
		"\u0000\u0000\u023a[\u0001\u0000\u0000\u0000\u023b\u0241\u0005Q\u0000\u0000"+
		"\u023c\u023e\u0005\u0007\u0000\u0000\u023d\u023f\u0003\u00a0P\u0000\u023e"+
		"\u023d\u0001\u0000\u0000\u0000\u023e\u023f\u0001\u0000\u0000\u0000\u023f"+
		"\u0240\u0001\u0000\u0000\u0000\u0240\u0242\u0005\b\u0000\u0000\u0241\u023c"+
		"\u0001\u0000\u0000\u0000\u0241\u0242\u0001\u0000\u0000\u0000\u0242\u0243"+
		"\u0001\u0000\u0000\u0000\u0243\u0244\u0003\u000e\u0007\u0000\u0244]\u0001"+
		"\u0000\u0000\u0000\u0245\u0246\u0005R\u0000\u0000\u0246\u0247\u0003\u000e"+
		"\u0007\u0000\u0247_\u0001\u0000\u0000\u0000\u0248\u0249\u0005Y\u0000\u0000"+
		"\u0249\u024a\u0003\u00c4b\u0000\u024aa\u0001\u0000\u0000\u0000\u024b\u024d"+
		"\u0005o\u0000\u0000\u024c\u024b\u0001\u0000\u0000\u0000\u024c\u024d\u0001"+
		"\u0000\u0000\u0000\u024d\u024e\u0001\u0000\u0000\u0000\u024e\u0250\u0005"+
		"Z\u0000\u0000\u024f\u0251\u0005\u001a\u0000\u0000\u0250\u024f\u0001\u0000"+
		"\u0000\u0000\u0250\u0251\u0001\u0000\u0000\u0000\u0251\u0252\u0001\u0000"+
		"\u0000\u0000\u0252\u0253\u0003\u00bc^\u0000\u0253\u0255\u0005\u0007\u0000"+
		"\u0000\u0254\u0256\u0003r9\u0000\u0255\u0254\u0001\u0000\u0000\u0000\u0255"+
		"\u0256\u0001\u0000\u0000\u0000\u0256\u0257\u0001\u0000\u0000\u0000\u0257"+
		"\u0258\u0005\b\u0000\u0000\u0258\u0259\u0003x<\u0000\u0259c\u0001\u0000"+
		"\u0000\u0000\u025a\u025b\u0005h\u0000\u0000\u025b\u025c\u0003\u00bc^\u0000"+
		"\u025c\u025d\u0003f3\u0000\u025de\u0001\u0000\u0000\u0000\u025e\u025f"+
		"\u0005j\u0000\u0000\u025f\u0261\u0003\u009aM\u0000\u0260\u025e\u0001\u0000"+
		"\u0000\u0000\u0260\u0261\u0001\u0000\u0000\u0000\u0261\u0262\u0001\u0000"+
		"\u0000\u0000\u0262\u0266\u0005\t\u0000\u0000\u0263\u0265\u0003h4\u0000"+
		"\u0264\u0263\u0001\u0000\u0000\u0000\u0265\u0268\u0001\u0000\u0000\u0000"+
		"\u0266\u0264\u0001\u0000\u0000\u0000\u0266\u0267\u0001\u0000\u0000\u0000"+
		"\u0267\u0269\u0001\u0000\u0000\u0000\u0268\u0266\u0001\u0000\u0000\u0000"+
		"\u0269\u026a\u0005\u000b\u0000\u0000\u026ag\u0001\u0000\u0000\u0000\u026b"+
		"\u026f\u0005y\u0000\u0000\u026c\u026d\u00044\u0006\u0000\u026d\u026f\u0003"+
		"\u00bc^\u0000\u026e\u026b\u0001\u0000\u0000\u0000\u026e\u026c\u0001\u0000"+
		"\u0000\u0000\u026e\u026f\u0001\u0000\u0000\u0000\u026f\u0270\u0001\u0000"+
		"\u0000\u0000\u0270\u027f\u0003j5\u0000\u0271\u0275\u0005y\u0000\u0000"+
		"\u0272\u0273\u00044\u0007\u0000\u0273\u0275\u0003\u00bc^\u0000\u0274\u0271"+
		"\u0001\u0000\u0000\u0000\u0274\u0272\u0001\u0000\u0000\u0000\u0274\u0275"+
		"\u0001\u0000\u0000\u0000\u0275\u0276\u0001\u0000\u0000\u0000\u0276\u027f"+
		"\u0003l6\u0000\u0277\u027b\u0005y\u0000\u0000\u0278\u0279\u00044\b\u0000"+
		"\u0279\u027b\u0003\u00bc^\u0000\u027a\u0277\u0001\u0000\u0000\u0000\u027a"+
		"\u0278\u0001\u0000\u0000\u0000\u027b\u027c\u0001\u0000\u0000\u0000\u027c"+
		"\u027f\u0003\u000e\u0007\u0000\u027d\u027f\u00038\u001c\u0000\u027e\u026e"+
		"\u0001\u0000\u0000\u0000\u027e\u0274\u0001\u0000\u0000\u0000\u027e\u027a"+
		"\u0001\u0000\u0000\u0000\u027e\u027d\u0001\u0000\u0000\u0000\u027fi\u0001"+
		"\u0000\u0000\u0000\u0280\u0281\u0005o\u0000\u0000\u0281\u0283\u00045\t"+
		"\u0000\u0282\u0280\u0001\u0000\u0000\u0000\u0282\u0283\u0001\u0000\u0000"+
		"\u0000\u0283\u0285\u0001\u0000\u0000\u0000\u0284\u0286\u0005\u001a\u0000"+
		"\u0000\u0285\u0284\u0001\u0000\u0000\u0000\u0285\u0286\u0001\u0000\u0000"+
		"\u0000\u0286\u0287\u0001\u0000\u0000\u0000\u0287\u0288\u0003n7\u0000\u0288"+
		"\u028a\u0005\u0007\u0000\u0000\u0289\u028b\u0003r9\u0000\u028a\u0289\u0001"+
		"\u0000\u0000\u0000\u028a\u028b\u0001\u0000\u0000\u0000\u028b\u028c\u0001"+
		"\u0000\u0000\u0000\u028c\u028d\u0005\b\u0000\u0000\u028d\u028e\u0003x"+
		"<\u0000\u028e\u02a3\u0001\u0000\u0000\u0000\u028f\u0291\u0005\u001a\u0000"+
		"\u0000\u0290\u028f\u0001\u0000\u0000\u0000\u0290\u0291\u0001\u0000\u0000"+
		"\u0000\u0291\u0292\u0001\u0000\u0000\u0000\u0292\u0293\u0003\u00b6[\u0000"+
		"\u0293\u0294\u0005\u0007\u0000\u0000\u0294\u0295\u0005\b\u0000\u0000\u0295"+
		"\u0296\u0003x<\u0000\u0296\u02a3\u0001\u0000\u0000\u0000\u0297\u0299\u0005"+
		"\u001a\u0000\u0000\u0298\u0297\u0001\u0000\u0000\u0000\u0298\u0299\u0001"+
		"\u0000\u0000\u0000\u0299\u029a\u0001\u0000\u0000\u0000\u029a\u029b\u0003"+
		"\u00b8\\\u0000\u029b\u029d\u0005\u0007\u0000\u0000\u029c\u029e\u0003r"+
		"9\u0000\u029d\u029c\u0001\u0000\u0000\u0000\u029d\u029e\u0001\u0000\u0000"+
		"\u0000\u029e\u029f\u0001\u0000\u0000\u0000\u029f\u02a0\u0005\b\u0000\u0000"+
		"\u02a0\u02a1\u0003x<\u0000\u02a1\u02a3\u0001\u0000\u0000\u0000\u02a2\u0282"+
		"\u0001\u0000\u0000\u0000\u02a2\u0290\u0001\u0000\u0000\u0000\u02a2\u0298"+
		"\u0001\u0000\u0000\u0000\u02a3k\u0001\u0000\u0000\u0000\u02a4\u02a6\u0003"+
		"n7\u0000\u02a5\u02a7\u0003\u009eO\u0000\u02a6\u02a5\u0001\u0000\u0000"+
		"\u0000\u02a6\u02a7\u0001\u0000\u0000\u0000\u02a7m\u0001\u0000\u0000\u0000"+
		"\u02a8\u02ab\u0003\u0092I\u0000\u02a9\u02ab\u0003p8\u0000\u02aa\u02a8"+
		"\u0001\u0000\u0000\u0000\u02aa\u02a9\u0001\u0000\u0000\u0000\u02abo\u0001"+
		"\u0000\u0000\u0000\u02ac\u02ad\u0005\u001f\u0000\u0000\u02ad\u02ae\u0003"+
		"\u00ba]\u0000\u02aeq\u0001\u0000\u0000\u0000\u02af\u02b4\u0003t:\u0000"+
		"\u02b0\u02b1\u0005\r\u0000\u0000\u02b1\u02b3\u0003t:\u0000\u02b2\u02b0"+
		"\u0001\u0000\u0000\u0000\u02b3\u02b6\u0001\u0000\u0000\u0000\u02b4\u02b2"+
		"\u0001\u0000\u0000\u0000\u02b4\u02b5\u0001\u0000\u0000\u0000\u02b5\u02b9"+
		"\u0001\u0000\u0000\u0000\u02b6\u02b4\u0001\u0000\u0000\u0000\u02b7\u02b8"+
		"\u0005\r\u0000\u0000\u02b8\u02ba\u0003v;\u0000\u02b9\u02b7\u0001\u0000"+
		"\u0000\u0000\u02b9\u02ba\u0001\u0000\u0000\u0000\u02ba\u02bd\u0001\u0000"+
		"\u0000\u0000\u02bb\u02bd\u0003v;\u0000\u02bc\u02af\u0001\u0000\u0000\u0000"+
		"\u02bc\u02bb\u0001\u0000\u0000\u0000\u02bds\u0001\u0000\u0000\u0000\u02be"+
		"\u02c1\u0003\u00a0P\u0000\u02bf\u02c0\u0005\u000e\u0000\u0000\u02c0\u02c2"+
		"\u0003\u009aM\u0000\u02c1\u02bf\u0001\u0000\u0000\u0000\u02c1\u02c2\u0001"+
		"\u0000\u0000\u0000\u02c2u\u0001\u0000\u0000\u0000\u02c3\u02c4\u0005\u0012"+
		"\u0000\u0000\u02c4\u02c5\u0003\u009aM\u0000\u02c5w\u0001\u0000\u0000\u0000"+
		"\u02c6\u02c8\u0005\t\u0000\u0000\u02c7\u02c9\u0003z=\u0000\u02c8\u02c7"+
		"\u0001\u0000\u0000\u0000\u02c8\u02c9\u0001\u0000\u0000\u0000\u02c9\u02ca"+
		"\u0001\u0000\u0000\u0000\u02ca\u02cb\u0005\u000b\u0000\u0000\u02cby\u0001"+
		"\u0000\u0000\u0000\u02cc\u02ce\u0003\u0002\u0001\u0000\u02cd\u02cc\u0001"+
		"\u0000\u0000\u0000\u02ce\u02cf\u0001\u0000\u0000\u0000\u02cf\u02cd\u0001"+
		"\u0000\u0000\u0000\u02cf\u02d0\u0001\u0000\u0000\u0000\u02d0{\u0001\u0000"+
		"\u0000\u0000\u02d1\u02d2\u0005\u0005\u0000\u0000\u02d2\u02d8\u0005\u0006"+
		"\u0000\u0000\u02d3\u02d4\u0005\u0005\u0000\u0000\u02d4\u02d5\u0003~?\u0000"+
		"\u02d5\u02d6\u0005\u0006\u0000\u0000\u02d6\u02d8\u0001\u0000\u0000\u0000"+
		"\u02d7\u02d1\u0001\u0000\u0000\u0000\u02d7\u02d3\u0001\u0000\u0000\u0000"+
		"\u02d8}\u0001\u0000\u0000\u0000\u02d9\u02de\u0003\u0080@\u0000\u02da\u02db"+
		"\u0005\r\u0000\u0000\u02db\u02dd\u0003\u0080@\u0000\u02dc\u02da\u0001"+
		"\u0000\u0000\u0000\u02dd\u02e0\u0001\u0000\u0000\u0000\u02de\u02dc\u0001"+
		"\u0000\u0000\u0000\u02de\u02df\u0001\u0000\u0000\u0000\u02df\u007f\u0001"+
		"\u0000\u0000\u0000\u02e0\u02de\u0001\u0000\u0000\u0000\u02e1\u02e2\u0005"+
		"\u0012\u0000\u0000\u02e2\u02e7\u0003\u009aM\u0000\u02e3\u02e5\u0003\u009a"+
		"M\u0000\u02e4\u02e3\u0001\u0000\u0000\u0000\u02e4\u02e5\u0001\u0000\u0000"+
		"\u0000\u02e5\u02e7\u0001\u0000\u0000\u0000\u02e6\u02e1\u0001\u0000\u0000"+
		"\u0000\u02e6\u02e4\u0001\u0000\u0000\u0000\u02e7\u0081\u0001\u0000\u0000"+
		"\u0000\u02e8\u02e9\u0003\u0092I\u0000\u02e9\u02ea\u0005\u0011\u0000\u0000"+
		"\u02ea\u02eb\u0003\u009aM\u0000\u02eb\u030f\u0001\u0000\u0000\u0000\u02ec"+
		"\u02ed\u0005\u0005\u0000\u0000\u02ed\u02ee\u0003\u009aM\u0000\u02ee\u02ef"+
		"\u0005\u0006\u0000\u0000\u02ef\u02f0\u0005\u0011\u0000\u0000\u02f0\u02f1"+
		"\u0003\u009aM\u0000\u02f1\u030f\u0001\u0000\u0000\u0000\u02f2\u02f4\u0005"+
		"o\u0000\u0000\u02f3\u02f2\u0001\u0000\u0000\u0000\u02f3\u02f4\u0001\u0000"+
		"\u0000\u0000\u02f4\u02f6\u0001\u0000\u0000\u0000\u02f5\u02f7\u0005\u001a"+
		"\u0000\u0000\u02f6\u02f5\u0001\u0000\u0000\u0000\u02f6\u02f7\u0001\u0000"+
		"\u0000\u0000\u02f7\u02f8\u0001\u0000\u0000\u0000\u02f8\u02f9\u0003\u0092"+
		"I\u0000\u02f9\u02fb\u0005\u0007\u0000\u0000\u02fa\u02fc\u0003r9\u0000"+
		"\u02fb\u02fa\u0001\u0000\u0000\u0000\u02fb\u02fc\u0001\u0000\u0000\u0000"+
		"\u02fc\u02fd\u0001\u0000\u0000\u0000\u02fd\u02fe\u0005\b\u0000\u0000\u02fe"+
		"\u02ff\u0003x<\u0000\u02ff\u030f\u0001\u0000\u0000\u0000\u0300\u0301\u0003"+
		"\u00b6[\u0000\u0301\u0302\u0005\u0007\u0000\u0000\u0302\u0303\u0005\b"+
		"\u0000\u0000\u0303\u0304\u0003x<\u0000\u0304\u030f\u0001\u0000\u0000\u0000"+
		"\u0305\u0306\u0003\u00b8\\\u0000\u0306\u0307\u0005\u0007\u0000\u0000\u0307"+
		"\u0308\u0003t:\u0000\u0308\u0309\u0005\b\u0000\u0000\u0309\u030a\u0003"+
		"x<\u0000\u030a\u030f\u0001\u0000\u0000\u0000\u030b\u030c\u0005\u0012\u0000"+
		"\u0000\u030c\u030f\u0003\u009aM\u0000\u030d\u030f\u0003\u00ba]\u0000\u030e"+
		"\u02e8\u0001\u0000\u0000\u0000\u030e\u02ec\u0001\u0000\u0000\u0000\u030e"+
		"\u02f3\u0001\u0000\u0000\u0000\u030e\u0300\u0001\u0000\u0000\u0000\u030e"+
		"\u0305\u0001\u0000\u0000\u0000\u030e\u030b\u0001\u0000\u0000\u0000\u030e"+
		"\u030d\u0001\u0000\u0000\u0000\u030f\u0083\u0001\u0000\u0000\u0000\u0310"+
		"\u0311\u0005\t\u0000\u0000\u0311\u032c\u0005\u000b\u0000\u0000\u0312\u0313"+
		"\u0005\t\u0000\u0000\u0313\u0315\u0003\u0090H\u0000\u0314\u0316\u0005"+
		"\r\u0000\u0000\u0315\u0314\u0001\u0000\u0000\u0000\u0315\u0316\u0001\u0000"+
		"\u0000\u0000\u0316\u0317\u0001\u0000\u0000\u0000\u0317\u0318\u0005\u000b"+
		"\u0000\u0000\u0318\u032c\u0001\u0000\u0000\u0000\u0319\u031a\u0005\t\u0000"+
		"\u0000\u031a\u031f\u0003\u0086C\u0000\u031b\u031c\u0005\r\u0000\u0000"+
		"\u031c\u031e\u0003\u0086C\u0000\u031d\u031b\u0001\u0000\u0000\u0000\u031e"+
		"\u0321\u0001\u0000\u0000\u0000\u031f\u031d\u0001\u0000\u0000\u0000\u031f"+
		"\u0320\u0001\u0000\u0000\u0000\u0320\u0324\u0001\u0000\u0000\u0000\u0321"+
		"\u031f\u0001\u0000\u0000\u0000\u0322\u0323\u0005\r\u0000\u0000\u0323\u0325"+
		"\u0003\u0090H\u0000\u0324\u0322\u0001\u0000\u0000\u0000\u0324\u0325\u0001"+
		"\u0000\u0000\u0000\u0325\u0327\u0001\u0000\u0000\u0000\u0326\u0328\u0005"+
		"\r\u0000\u0000\u0327\u0326\u0001\u0000\u0000\u0000\u0327\u0328\u0001\u0000"+
		"\u0000\u0000\u0328\u0329\u0001\u0000\u0000\u0000\u0329\u032a\u0005\u000b"+
		"\u0000\u0000\u032a\u032c\u0001\u0000\u0000\u0000\u032b\u0310\u0001\u0000"+
		"\u0000\u0000\u032b\u0312\u0001\u0000\u0000\u0000\u032b\u0319\u0001\u0000"+
		"\u0000\u0000\u032c\u0085\u0001\u0000\u0000\u0000\u032d\u032f\u0003\u00ba"+
		"]\u0000\u032e\u0330\u0003\u009eO\u0000\u032f\u032e\u0001\u0000\u0000\u0000"+
		"\u032f\u0330\u0001\u0000\u0000\u0000\u0330\u0336\u0001\u0000\u0000\u0000"+
		"\u0331\u0332\u0003\u0092I\u0000\u0332\u0333\u0005\u0011\u0000\u0000\u0333"+
		"\u0334\u0003\u008eG\u0000\u0334\u0336\u0001\u0000\u0000\u0000\u0335\u032d"+
		"\u0001\u0000\u0000\u0000\u0335\u0331\u0001\u0000\u0000\u0000\u0336\u0087"+
		"\u0001\u0000\u0000\u0000\u0337\u0338\u0005\u0005\u0000\u0000\u0338\u034c"+
		"\u0005\u0006\u0000\u0000\u0339\u033a\u0005\u0005\u0000\u0000\u033a\u033c"+
		"\u0003\u0090H\u0000\u033b\u033d\u0005\r\u0000\u0000\u033c\u033b\u0001"+
		"\u0000\u0000\u0000\u033c\u033d\u0001\u0000\u0000\u0000\u033d\u033e\u0001"+
		"\u0000\u0000\u0000\u033e\u033f\u0005\u0006\u0000\u0000\u033f\u034c\u0001"+
		"\u0000\u0000\u0000\u0340\u0341\u0005\u0005\u0000\u0000\u0341\u0344\u0003"+
		"\u008aE\u0000\u0342\u0343\u0005\r\u0000\u0000\u0343\u0345\u0003\u0090"+
		"H\u0000\u0344\u0342\u0001\u0000\u0000\u0000\u0344\u0345\u0001\u0000\u0000"+
		"\u0000\u0345\u0347\u0001\u0000\u0000\u0000\u0346\u0348\u0005\r\u0000\u0000"+
		"\u0347\u0346\u0001\u0000\u0000\u0000\u0347\u0348\u0001\u0000\u0000\u0000"+
		"\u0348\u0349\u0001\u0000\u0000\u0000\u0349\u034a\u0005\u0006\u0000\u0000"+
		"\u034a\u034c\u0001\u0000\u0000\u0000\u034b\u0337\u0001\u0000\u0000\u0000"+
		"\u034b\u0339\u0001\u0000\u0000\u0000\u034b\u0340\u0001\u0000\u0000\u0000"+
		"\u034c\u0089\u0001\u0000\u0000\u0000\u034d\u0352\u0003\u008eG\u0000\u034e"+
		"\u034f\u0005\r\u0000\u0000\u034f\u0351\u0003\u008cF\u0000\u0350\u034e"+
		"\u0001\u0000\u0000\u0000\u0351\u0354\u0001\u0000\u0000\u0000\u0352\u0350"+
		"\u0001\u0000\u0000\u0000\u0352\u0353\u0001\u0000\u0000\u0000\u0353\u035f"+
		"\u0001\u0000\u0000\u0000\u0354\u0352\u0001\u0000\u0000\u0000\u0355\u0356"+
		"\u0005\r\u0000\u0000\u0356\u035b\u0003\u008cF\u0000\u0357\u0358\u0005"+
		"\r\u0000\u0000\u0358\u035a\u0003\u008cF\u0000\u0359\u0357\u0001\u0000"+
		"\u0000\u0000\u035a\u035d\u0001\u0000\u0000\u0000\u035b\u0359\u0001\u0000"+
		"\u0000\u0000\u035b\u035c\u0001\u0000\u0000\u0000\u035c\u035f\u0001\u0000"+
		"\u0000\u0000\u035d\u035b\u0001\u0000\u0000\u0000\u035e\u034d\u0001\u0000"+
		"\u0000\u0000\u035e\u0355\u0001\u0000\u0000\u0000\u035f\u008b\u0001\u0000"+
		"\u0000\u0000\u0360\u0362\u0003\u008eG\u0000\u0361\u0360\u0001\u0000\u0000"+
		"\u0000\u0361\u0362\u0001\u0000\u0000\u0000\u0362\u008d\u0001\u0000\u0000"+
		"\u0000\u0363\u0365\u0003\u00a0P\u0000\u0364\u0366\u0003\u009eO\u0000\u0365"+
		"\u0364\u0001\u0000\u0000\u0000\u0365\u0366\u0001\u0000\u0000\u0000\u0366"+
		"\u008f\u0001\u0000\u0000\u0000\u0367\u0368\u0005\u0012\u0000\u0000\u0368"+
		"\u0369\u0003\u00ba]\u0000\u0369\u0091\u0001\u0000\u0000\u0000\u036a\u0372"+
		"\u0003\u00ba]\u0000\u036b\u0372\u0005}\u0000\u0000\u036c\u0372\u0003\u00b2"+
		"Y\u0000\u036d\u036e\u0005\u0005\u0000\u0000\u036e\u036f\u0003\u009aM\u0000"+
		"\u036f\u0370\u0005\u0006\u0000\u0000\u0370\u0372\u0001\u0000\u0000\u0000"+
		"\u0371\u036a\u0001\u0000\u0000\u0000\u0371\u036b\u0001\u0000\u0000\u0000"+
		"\u0371\u036c\u0001\u0000\u0000\u0000\u0371\u036d\u0001\u0000\u0000\u0000"+
		"\u0372\u0093\u0001\u0000\u0000\u0000\u0373\u037f\u0005\u0007\u0000\u0000"+
		"\u0374\u0379\u0003\u0096K\u0000\u0375\u0376\u0005\r\u0000\u0000\u0376"+
		"\u0378\u0003\u0096K\u0000\u0377\u0375\u0001\u0000\u0000\u0000\u0378\u037b"+
		"\u0001\u0000\u0000\u0000\u0379\u0377\u0001\u0000\u0000\u0000\u0379\u037a"+
		"\u0001\u0000\u0000\u0000\u037a\u037d\u0001\u0000\u0000\u0000\u037b\u0379"+
		"\u0001\u0000\u0000\u0000\u037c\u037e\u0005\r\u0000\u0000\u037d\u037c\u0001"+
		"\u0000\u0000\u0000\u037d\u037e\u0001\u0000\u0000\u0000\u037e\u0380\u0001"+
		"\u0000\u0000\u0000\u037f\u0374\u0001\u0000\u0000\u0000\u037f\u0380\u0001"+
		"\u0000\u0000\u0000\u0380\u0381\u0001\u0000\u0000\u0000\u0381\u0382\u0005"+
		"\b\u0000\u0000\u0382\u0095\u0001\u0000\u0000\u0000\u0383\u0385\u0005\u0012"+
		"\u0000\u0000\u0384\u0383\u0001\u0000\u0000\u0000\u0384\u0385\u0001\u0000"+
		"\u0000\u0000\u0385\u0388\u0001\u0000\u0000\u0000\u0386\u0389\u0003\u009a"+
		"M\u0000\u0387\u0389\u0003\u00bc^\u0000\u0388\u0386\u0001\u0000\u0000\u0000"+
		"\u0388\u0387\u0001\u0000\u0000\u0000\u0389\u0097\u0001\u0000\u0000\u0000"+
		"\u038a\u038b\u0006L\uffff\uffff\u0000\u038b\u038c\u0003\u009aM\u0000\u038c"+
		"\u0392\u0001\u0000\u0000\u0000\u038d\u038e\n\u0002\u0000\u0000\u038e\u038f"+
		"\u0005\r\u0000\u0000\u038f\u0391\u0003\u0098L\u0003\u0390\u038d\u0001"+
		"\u0000\u0000\u0000\u0391\u0394\u0001\u0000\u0000\u0000\u0392\u0390\u0001"+
		"\u0000\u0000\u0000\u0392\u0393\u0001\u0000\u0000\u0000\u0393\u0099\u0001"+
		"\u0000\u0000\u0000\u0394\u0392\u0001\u0000\u0000\u0000\u0395\u0396\u0003"+
		"\u009cN\u0000\u0396\u009b\u0001\u0000\u0000\u0000\u0397\u0398\u0006N\uffff"+
		"\uffff\u0000\u0398\u03d7\u0003\u00a4R\u0000\u0399\u039b\u0005h\u0000\u0000"+
		"\u039a\u039c\u0003\u00bc^\u0000\u039b\u039a\u0001\u0000\u0000\u0000\u039b"+
		"\u039c\u0001\u0000\u0000\u0000\u039c\u039d\u0001\u0000\u0000\u0000\u039d"+
		"\u03d7\u0003f3\u0000\u039e\u039f\u0005O\u0000\u0000\u039f\u03a0\u0003"+
		"\u00bc^\u0000\u03a0\u03a1\u0003\u0094J\u0000\u03a1\u03d7\u0001\u0000\u0000"+
		"\u0000\u03a2\u03a3\u0005O\u0000\u0000\u03a3\u03a4\u0003\u009cN\u0000\u03a4"+
		"\u03a5\u0003\u0094J\u0000\u03a5\u03d7\u0001\u0000\u0000\u0000\u03a6\u03a7"+
		"\u0005O\u0000\u0000\u03a7\u03d7\u0003\u009cN+\u03a8\u03a9\u0005O\u0000"+
		"\u0000\u03a9\u03aa\u0005\u0013\u0000\u0000\u03aa\u03d7\u0005{\u0000\u0000"+
		"\u03ab\u03ac\u0005n\u0000\u0000\u03ac\u03ad\u0005\u0013\u0000\u0000\u03ad"+
		"\u03d7\u0005z\u0000\u0000\u03ae\u03af\u0005`\u0000\u0000\u03af\u03d7\u0003"+
		"\u009cN%\u03b0\u03b1\u0005T\u0000\u0000\u03b1\u03d7\u0003\u009cN$\u03b2"+
		"\u03b3\u0005L\u0000\u0000\u03b3\u03d7\u0003\u009cN#\u03b4\u03b5\u0005"+
		"\u0014\u0000\u0000\u03b5\u03d7\u0003\u009cN\"\u03b6\u03b7\u0005\u0015"+
		"\u0000\u0000\u03b7\u03d7\u0003\u009cN!\u03b8\u03b9\u0005\u0016\u0000\u0000"+
		"\u03b9\u03d7\u0003\u009cN \u03ba\u03bb\u0005\u0017\u0000\u0000\u03bb\u03d7"+
		"\u0003\u009cN\u001f\u03bc\u03bd\u0005\u0018\u0000\u0000\u03bd\u03d7\u0003"+
		"\u009cN\u001e\u03be\u03bf\u0005\u0019\u0000\u0000\u03bf\u03d7\u0003\u009c"+
		"N\u001d\u03c0\u03c1\u0005p\u0000\u0000\u03c1\u03d7\u0003\u009cN\u001c"+
		"\u03c2\u03c3\u0005n\u0000\u0000\u03c3\u03c4\u0005\u0007\u0000\u0000\u03c4"+
		"\u03c5\u0003\u009cN\u0000\u03c5\u03c6\u0005\b\u0000\u0000\u03c6\u03d7"+
		"\u0001\u0000\u0000\u0000\u03c7\u03ca\u0007\u0001\u0000\u0000\u03c8\u03c9"+
		"\u0004N\u000b\u0000\u03c9\u03cb\u0003\u0098L\u0000\u03ca\u03c8\u0001\u0000"+
		"\u0000\u0000\u03ca\u03cb\u0001\u0000\u0000\u0000\u03cb\u03d7\u0001\u0000"+
		"\u0000\u0000\u03cc\u03d7\u0005[\u0000\u0000\u03cd\u03d7\u0003\u00bc^\u0000"+
		"\u03ce\u03d7\u0005k\u0000\u0000\u03cf\u03d7\u0003\u00acV\u0000\u03d0\u03d7"+
		"\u0003|>\u0000\u03d1\u03d7\u0003\u00a2Q\u0000\u03d2\u03d3\u0005\u0007"+
		"\u0000\u0000\u03d3\u03d4\u0003\u0098L\u0000\u03d4\u03d5\u0005\b\u0000"+
		"\u0000\u03d5\u03d7\u0001\u0000\u0000\u0000\u03d6\u0397\u0001\u0000\u0000"+
		"\u0000\u03d6\u0399\u0001\u0000\u0000\u0000\u03d6\u039e\u0001\u0000\u0000"+
		"\u0000\u03d6\u03a2\u0001\u0000\u0000\u0000\u03d6\u03a6\u0001\u0000\u0000"+
		"\u0000\u03d6\u03a8\u0001\u0000\u0000\u0000\u03d6\u03ab\u0001\u0000\u0000"+
		"\u0000\u03d6\u03ae\u0001\u0000\u0000\u0000\u03d6\u03b0\u0001\u0000\u0000"+
		"\u0000\u03d6\u03b2\u0001\u0000\u0000\u0000\u03d6\u03b4\u0001\u0000\u0000"+
		"\u0000\u03d6\u03b6\u0001\u0000\u0000\u0000\u03d6\u03b8\u0001\u0000\u0000"+
		"\u0000\u03d6\u03ba\u0001\u0000\u0000\u0000\u03d6\u03bc\u0001\u0000\u0000"+
		"\u0000\u03d6\u03be\u0001\u0000\u0000\u0000\u03d6\u03c0\u0001\u0000\u0000"+
		"\u0000\u03d6\u03c2\u0001\u0000\u0000\u0000\u03d6\u03c7\u0001\u0000\u0000"+
		"\u0000\u03d6\u03cc\u0001\u0000\u0000\u0000\u03d6\u03cd\u0001\u0000\u0000"+
		"\u0000\u03d6\u03ce\u0001\u0000\u0000\u0000\u03d6\u03cf\u0001\u0000\u0000"+
		"\u0000\u03d6\u03d0\u0001\u0000\u0000\u0000\u03d6\u03d1\u0001\u0000\u0000"+
		"\u0000\u03d6\u03d2\u0001\u0000\u0000\u0000\u03d7\u042f\u0001\u0000\u0000"+
		"\u0000\u03d8\u03d9\n0\u0000\u0000\u03d9\u03da\u0005\u0010\u0000\u0000"+
		"\u03da\u042e\u0003\u009cN1\u03db\u03dc\n\u001b\u0000\u0000\u03dc\u03dd"+
		"\u0005\u001d\u0000\u0000\u03dd\u042e\u0003\u009cN\u001b\u03de\u03df\n"+
		"\u001a\u0000\u0000\u03df\u03e0\u0007\u0002\u0000\u0000\u03e0\u042e\u0003"+
		"\u009cN\u001b\u03e1\u03e2\n\u0019\u0000\u0000\u03e2\u03e3\u0007\u0003"+
		"\u0000\u0000\u03e3\u042e\u0003\u009cN\u001a\u03e4\u03e5\n\u0018\u0000"+
		"\u0000\u03e5\u03e6\u0005\u001e\u0000\u0000\u03e6\u042e\u0003\u009cN\u0019"+
		"\u03e7\u03e8\n\u0017\u0000\u0000\u03e8\u03e9\u0007\u0004\u0000\u0000\u03e9"+
		"\u042e\u0003\u009cN\u0018\u03ea\u03eb\n\u0016\u0000\u0000\u03eb\u03ec"+
		"\u0007\u0005\u0000\u0000\u03ec\u042e\u0003\u009cN\u0017\u03ed\u03ee\n"+
		"\u0015\u0000\u0000\u03ee\u03ef\u0005K\u0000\u0000\u03ef\u042e\u0003\u009c"+
		"N\u0016\u03f0\u03f1\n\u0014\u0000\u0000\u03f1\u03f2\u0005a\u0000\u0000"+
		"\u03f2\u042e\u0003\u009cN\u0015\u03f3\u03f4\n\u0013\u0000\u0000\u03f4"+
		"\u03f5\u0007\u0006\u0000\u0000\u03f5\u042e\u0003\u009cN\u0014\u03f6\u03f7"+
		"\n\u0012\u0000\u0000\u03f7\u03f8\u0005+\u0000\u0000\u03f8\u042e\u0003"+
		"\u009cN\u0013\u03f9\u03fa\n\u0011\u0000\u0000\u03fa\u03fb\u0005,\u0000"+
		"\u0000\u03fb\u042e\u0003\u009cN\u0012\u03fc\u03fd\n\u0010\u0000\u0000"+
		"\u03fd\u03fe\u0005-\u0000\u0000\u03fe\u042e\u0003\u009cN\u0011\u03ff\u0400"+
		"\n\u000f\u0000\u0000\u0400\u0401\u0005.\u0000\u0000\u0401\u042e\u0003"+
		"\u009cN\u0010\u0402\u0403\n\u000e\u0000\u0000\u0403\u0404\u0005/\u0000"+
		"\u0000\u0404\u042e\u0003\u009cN\u000f\u0405\u0406\n\r\u0000\u0000\u0406"+
		"\u0407\u0005\u000f\u0000\u0000\u0407\u0408\u0003\u009cN\u0000\u0408\u0409"+
		"\u0005\u0011\u0000\u0000\u0409\u040a\u0003\u009cN\r\u040a\u042e\u0001"+
		"\u0000\u0000\u0000\u040b\u040c\n\f\u0000\u0000\u040c\u040d\u0005\u000e"+
		"\u0000\u0000\u040d\u042e\u0003\u009cN\f\u040e\u040f\n\u000b\u0000\u0000"+
		"\u040f\u0410\u0003\u00aaU\u0000\u0410\u0411\u0003\u009cN\u000b\u0411\u042e"+
		"\u0001\u0000\u0000\u0000\u0412\u0414\n/\u0000\u0000\u0413\u0415\u0005"+
		"\u0010\u0000\u0000\u0414\u0413\u0001\u0000\u0000\u0000\u0414\u0415\u0001"+
		"\u0000\u0000\u0000\u0415\u0416\u0001\u0000\u0000\u0000\u0416\u0417\u0005"+
		"\u0005\u0000\u0000\u0417\u0418\u0003\u0098L\u0000\u0418\u0419\u0005\u0006"+
		"\u0000\u0000\u0419\u042e\u0001\u0000\u0000\u0000\u041a\u041c\n.\u0000"+
		"\u0000\u041b\u041d\u0005\u000f\u0000\u0000\u041c\u041b\u0001\u0000\u0000"+
		"\u0000\u041c\u041d\u0001\u0000\u0000\u0000\u041d\u041e\u0001\u0000\u0000"+
		"\u0000\u041e\u0420\u0005\u0013\u0000\u0000\u041f\u0421\u0005\u001f\u0000"+
		"\u0000\u0420\u041f\u0001\u0000\u0000\u0000\u0420\u0421\u0001\u0000\u0000"+
		"\u0000\u0421\u0422\u0001\u0000\u0000\u0000\u0422\u042e\u0003\u00ba]\u0000"+
		"\u0423\u0424\n*\u0000\u0000\u0424\u042e\u0003\u0094J\u0000\u0425\u0426"+
		"\n\'\u0000\u0000\u0426\u0427\u0004N\"\u0000\u0427\u042e\u0005\u0014\u0000"+
		"\u0000\u0428\u0429\n&\u0000\u0000\u0429\u042a\u0004N$\u0000\u042a\u042e"+
		"\u0005\u0015\u0000\u0000\u042b\u042c\n\t\u0000\u0000\u042c\u042e\u0003"+
		"\u00aeW\u0000\u042d\u03d8\u0001\u0000\u0000\u0000\u042d\u03db\u0001\u0000"+
		"\u0000\u0000\u042d\u03de\u0001\u0000\u0000\u0000\u042d\u03e1\u0001\u0000"+
		"\u0000\u0000\u042d\u03e4\u0001\u0000\u0000\u0000\u042d\u03e7\u0001\u0000"+
		"\u0000\u0000\u042d\u03ea\u0001\u0000\u0000\u0000\u042d\u03ed\u0001\u0000"+
		"\u0000\u0000\u042d\u03f0\u0001\u0000\u0000\u0000\u042d\u03f3\u0001\u0000"+
		"\u0000\u0000\u042d\u03f6\u0001\u0000\u0000\u0000\u042d\u03f9\u0001\u0000"+
		"\u0000\u0000\u042d\u03fc\u0001\u0000\u0000\u0000\u042d\u03ff\u0001\u0000"+
		"\u0000\u0000\u042d\u0402\u0001\u0000\u0000\u0000\u042d\u0405\u0001\u0000"+
		"\u0000\u0000\u042d\u040b\u0001\u0000\u0000\u0000\u042d\u040e\u0001\u0000"+
		"\u0000\u0000\u042d\u0412\u0001\u0000\u0000\u0000\u042d\u041a\u0001\u0000"+
		"\u0000\u0000\u042d\u0423\u0001\u0000\u0000\u0000\u042d\u0425\u0001\u0000"+
		"\u0000\u0000\u042d\u0428\u0001\u0000\u0000\u0000\u042d\u042b\u0001\u0000"+
		"\u0000\u0000\u042e\u0431\u0001\u0000\u0000\u0000\u042f\u042d\u0001\u0000"+
		"\u0000\u0000\u042f\u0430\u0001\u0000\u0000\u0000\u0430\u009d\u0001\u0000"+
		"\u0000\u0000\u0431\u042f\u0001\u0000\u0000\u0000\u0432\u0433\u0005\u000e"+
		"\u0000\u0000\u0433\u0434\u0003\u009aM\u0000\u0434\u009f\u0001\u0000\u0000"+
		"\u0000\u0435\u043a\u0003\u00bc^\u0000\u0436\u043a\u0003\u00c0`\u0000\u0437"+
		"\u043a\u0003\u0088D\u0000\u0438\u043a\u0003\u0084B\u0000\u0439\u0435\u0001"+
		"\u0000\u0000\u0000\u0439\u0436\u0001\u0000\u0000\u0000\u0439\u0437\u0001"+
		"\u0000\u0000\u0000\u0439\u0438\u0001\u0000\u0000\u0000\u043a\u00a1\u0001"+
		"\u0000\u0000\u0000\u043b\u0447\u0005\t\u0000\u0000\u043c\u0441\u0003\u0082"+
		"A\u0000\u043d\u043e\u0005\r\u0000\u0000\u043e\u0440\u0003\u0082A\u0000"+
		"\u043f\u043d\u0001\u0000\u0000\u0000\u0440\u0443\u0001\u0000\u0000\u0000"+
		"\u0441\u043f\u0001\u0000\u0000\u0000\u0441\u0442\u0001\u0000\u0000\u0000"+
		"\u0442\u0445\u0001\u0000\u0000\u0000\u0443\u0441\u0001\u0000\u0000\u0000"+
		"\u0444\u0446\u0005\r\u0000\u0000\u0445\u0444\u0001\u0000\u0000\u0000\u0445"+
		"\u0446\u0001\u0000\u0000\u0000\u0446\u0448\u0001\u0000\u0000\u0000\u0447"+
		"\u043c\u0001\u0000\u0000\u0000\u0447\u0448\u0001\u0000\u0000\u0000\u0448"+
		"\u0449\u0001\u0000\u0000\u0000\u0449\u044a\u0005\u000b\u0000\u0000\u044a"+
		"\u00a3\u0001\u0000\u0000\u0000\u044b\u0461\u0003b1\u0000\u044c\u044e\u0005"+
		"o\u0000\u0000\u044d\u044c\u0001\u0000\u0000\u0000\u044d\u044e\u0001\u0000"+
		"\u0000\u0000\u044e\u044f\u0001\u0000\u0000\u0000\u044f\u0451\u0005Z\u0000"+
		"\u0000\u0450\u0452\u0005\u001a\u0000\u0000\u0451\u0450\u0001\u0000\u0000"+
		"\u0000\u0451\u0452\u0001\u0000\u0000\u0000\u0452\u0453\u0001\u0000\u0000"+
		"\u0000\u0453\u0455\u0005\u0007\u0000\u0000\u0454\u0456\u0003r9\u0000\u0455"+
		"\u0454\u0001\u0000\u0000\u0000\u0455\u0456\u0001\u0000\u0000\u0000\u0456"+
		"\u0457\u0001\u0000\u0000\u0000\u0457\u0458\u0005\b\u0000\u0000\u0458\u0461"+
		"\u0003x<\u0000\u0459\u045b\u0005o\u0000\u0000\u045a\u0459\u0001\u0000"+
		"\u0000\u0000\u045a\u045b\u0001\u0000\u0000\u0000\u045b\u045c\u0001\u0000"+
		"\u0000\u0000\u045c\u045d\u0003\u00a6S\u0000\u045d\u045e\u0005=\u0000\u0000"+
		"\u045e\u045f\u0003\u00a8T\u0000\u045f\u0461\u0001\u0000\u0000\u0000\u0460"+
		"\u044b\u0001\u0000\u0000\u0000\u0460\u044d\u0001\u0000\u0000\u0000\u0460"+
		"\u045a\u0001\u0000\u0000\u0000\u0461\u00a5\u0001\u0000\u0000\u0000\u0462"+
		"\u0469\u0003\u00ba]\u0000\u0463\u0465\u0005\u0007\u0000\u0000\u0464\u0466"+
		"\u0003r9\u0000\u0465\u0464\u0001\u0000\u0000\u0000\u0465\u0466\u0001\u0000"+
		"\u0000\u0000\u0466\u0467\u0001\u0000\u0000\u0000\u0467\u0469\u0005\b\u0000"+
		"\u0000\u0468\u0462\u0001\u0000\u0000\u0000\u0468\u0463\u0001\u0000\u0000"+
		"\u0000\u0469\u00a7\u0001\u0000\u0000\u0000\u046a\u046d\u0003\u009aM\u0000"+
		"\u046b\u046d\u0003x<\u0000\u046c\u046a\u0001\u0000\u0000\u0000\u046c\u046b"+
		"\u0001\u0000\u0000\u0000\u046d\u00a9\u0001\u0000\u0000\u0000\u046e\u046f"+
		"\u0007\u0007\u0000\u0000\u046f\u00ab\u0001\u0000\u0000\u0000\u0470\u0478"+
		"\u0005>\u0000\u0000\u0471\u0478\u0005?\u0000\u0000\u0472\u0478\u0005}"+
		"\u0000\u0000\u0473\u0478\u0003\u00aeW\u0000\u0474\u0478\u0005\u0004\u0000"+
		"\u0000\u0475\u0478\u0003\u00b2Y\u0000\u0476\u0478\u0003\u00b4Z\u0000\u0477"+
		"\u0470\u0001\u0000\u0000\u0000\u0477\u0471\u0001\u0000\u0000\u0000\u0477"+
		"\u0472\u0001\u0000\u0000\u0000\u0477\u0473\u0001\u0000\u0000\u0000\u0477"+
		"\u0474\u0001\u0000\u0000\u0000\u0477\u0475\u0001\u0000\u0000\u0000\u0477"+
		"\u0476\u0001\u0000\u0000\u0000\u0478\u00ad\u0001\u0000\u0000\u0000\u0479"+
		"\u047d\u0005~\u0000\u0000\u047a\u047c\u0003\u00b0X\u0000\u047b\u047a\u0001"+
		"\u0000\u0000\u0000\u047c\u047f\u0001\u0000\u0000\u0000\u047d\u047b\u0001"+
		"\u0000\u0000\u0000\u047d\u047e\u0001\u0000\u0000\u0000\u047e\u0480\u0001"+
		"\u0000\u0000\u0000\u047f\u047d\u0001\u0000\u0000\u0000\u0480\u0481\u0005"+
		"~\u0000\u0000\u0481\u00af\u0001\u0000\u0000\u0000\u0482\u0488\u0005\u0085"+
		"\u0000\u0000\u0483\u0484\u0005\u0084\u0000\u0000\u0484\u0485\u0003\u009a"+
		"M\u0000\u0485\u0486\u0005\n\u0000\u0000\u0486\u0488\u0001\u0000\u0000"+
		"\u0000\u0487\u0482\u0001\u0000\u0000\u0000\u0487\u0483\u0001\u0000\u0000"+
		"\u0000\u0488\u00b1\u0001\u0000\u0000\u0000\u0489\u048a\u0007\b\u0000\u0000"+
		"\u048a\u00b3\u0001\u0000\u0000\u0000\u048b\u048c\u0007\t\u0000\u0000\u048c"+
		"\u00b5\u0001\u0000\u0000\u0000\u048d\u048e\u0004[&\u0000\u048e\u048f\u0003"+
		"\u00bc^\u0000\u048f\u0490\u0003n7\u0000\u0490\u00b7\u0001\u0000\u0000"+
		"\u0000\u0491\u0492\u0004\\\'\u0000\u0492\u0493\u0003\u00bc^\u0000\u0493"+
		"\u0494\u0003n7\u0000\u0494\u00b9\u0001\u0000\u0000\u0000\u0495\u0498\u0003"+
		"\u00bc^\u0000\u0496\u0498\u0003\u00be_\u0000\u0497\u0495\u0001\u0000\u0000"+
		"\u0000\u0497\u0496\u0001\u0000\u0000\u0000\u0498\u00bb\u0001\u0000\u0000"+
		"\u0000\u0499\u049a\u0007\n\u0000\u0000\u049a\u00bd\u0001\u0000\u0000\u0000"+
		"\u049b\u049f\u0003\u00c0`\u0000\u049c\u049f\u0005>\u0000\u0000\u049d\u049f"+
		"\u0005?\u0000\u0000\u049e\u049b\u0001\u0000\u0000\u0000\u049e\u049c\u0001"+
		"\u0000\u0000\u0000\u049e\u049d\u0001\u0000\u0000\u0000\u049f\u00bf\u0001"+
		"\u0000\u0000\u0000\u04a0\u04d1\u0005I\u0000\u0000\u04a1\u04d1\u0005J\u0000"+
		"\u0000\u04a2\u04d1\u0005K\u0000\u0000\u04a3\u04d1\u0005L\u0000\u0000\u04a4"+
		"\u04d1\u0005M\u0000\u0000\u04a5\u04d1\u0005N\u0000\u0000\u04a6\u04d1\u0005"+
		"O\u0000\u0000\u04a7\u04d1\u0005P\u0000\u0000\u04a8\u04d1\u0005Q\u0000"+
		"\u0000\u04a9\u04d1\u0005R\u0000\u0000\u04aa\u04d1\u0005S\u0000\u0000\u04ab"+
		"\u04d1\u0005T\u0000\u0000\u04ac\u04d1\u0005U\u0000\u0000\u04ad\u04d1\u0005"+
		"V\u0000\u0000\u04ae\u04d1\u0005W\u0000\u0000\u04af\u04d1\u0005X\u0000"+
		"\u0000\u04b0\u04d1\u0005Y\u0000\u0000\u04b1\u04d1\u0005Z\u0000\u0000\u04b2"+
		"\u04d1\u0005[\u0000\u0000\u04b3\u04d1\u0005\\\u0000\u0000\u04b4\u04d1"+
		"\u0005]\u0000\u0000\u04b5\u04d1\u0005^\u0000\u0000\u04b6\u04d1\u0005_"+
		"\u0000\u0000\u04b7\u04d1\u0005`\u0000\u0000\u04b8\u04d1\u0005a\u0000\u0000"+
		"\u04b9\u04d1\u0005b\u0000\u0000\u04ba\u04d1\u0005h\u0000\u0000\u04bb\u04d1"+
		"\u0005i\u0000\u0000\u04bc\u04d1\u0005j\u0000\u0000\u04bd\u04d1\u0005k"+
		"\u0000\u0000\u04be\u04d1\u0005l\u0000\u0000\u04bf\u04d1\u0005m\u0000\u0000"+
		"\u04c0\u04d1\u0005n\u0000\u0000\u04c1\u04d1\u0005q\u0000\u0000\u04c2\u04d1"+
		"\u0003\u00c2a\u0000\u04c3\u04d1\u0005t\u0000\u0000\u04c4\u04d1\u0005u"+
		"\u0000\u0000\u04c5\u04d1\u0005v\u0000\u0000\u04c6\u04d1\u0005w\u0000\u0000"+
		"\u04c7\u04d1\u0005x\u0000\u0000\u04c8\u04d1\u0005y\u0000\u0000\u04c9\u04d1"+
		"\u0005f\u0000\u0000\u04ca\u04d1\u0005g\u0000\u0000\u04cb\u04d1\u0005o"+
		"\u0000\u0000\u04cc\u04d1\u0005p\u0000\u0000\u04cd\u04d1\u0005d\u0000\u0000"+
		"\u04ce\u04d1\u0005c\u0000\u0000\u04cf\u04d1\u0005e\u0000\u0000\u04d0\u04a0"+
		"\u0001\u0000\u0000\u0000\u04d0\u04a1\u0001\u0000\u0000\u0000\u04d0\u04a2"+
		"\u0001\u0000\u0000\u0000\u04d0\u04a3\u0001\u0000\u0000\u0000\u04d0\u04a4"+
		"\u0001\u0000\u0000\u0000\u04d0\u04a5\u0001\u0000\u0000\u0000\u04d0\u04a6"+
		"\u0001\u0000\u0000\u0000\u04d0\u04a7\u0001\u0000\u0000\u0000\u04d0\u04a8"+
		"\u0001\u0000\u0000\u0000\u04d0\u04a9\u0001\u0000\u0000\u0000\u04d0\u04aa"+
		"\u0001\u0000\u0000\u0000\u04d0\u04ab\u0001\u0000\u0000\u0000\u04d0\u04ac"+
		"\u0001\u0000\u0000\u0000\u04d0\u04ad\u0001\u0000\u0000\u0000\u04d0\u04ae"+
		"\u0001\u0000\u0000\u0000\u04d0\u04af\u0001\u0000\u0000\u0000\u04d0\u04b0"+
		"\u0001\u0000\u0000\u0000\u04d0\u04b1\u0001\u0000\u0000\u0000\u04d0\u04b2"+
		"\u0001\u0000\u0000\u0000\u04d0\u04b3\u0001\u0000\u0000\u0000\u04d0\u04b4"+
		"\u0001\u0000\u0000\u0000\u04d0\u04b5\u0001\u0000\u0000\u0000\u04d0\u04b6"+
		"\u0001\u0000\u0000\u0000\u04d0\u04b7\u0001\u0000\u0000\u0000\u04d0\u04b8"+
		"\u0001\u0000\u0000\u0000\u04d0\u04b9\u0001\u0000\u0000\u0000\u04d0\u04ba"+
		"\u0001\u0000\u0000\u0000\u04d0\u04bb\u0001\u0000\u0000\u0000\u04d0\u04bc"+
		"\u0001\u0000\u0000\u0000\u04d0\u04bd\u0001\u0000\u0000\u0000\u04d0\u04be"+
		"\u0001\u0000\u0000\u0000\u04d0\u04bf\u0001\u0000\u0000\u0000\u04d0\u04c0"+
		"\u0001\u0000\u0000\u0000\u04d0\u04c1\u0001\u0000\u0000\u0000\u04d0\u04c2"+
		"\u0001\u0000\u0000\u0000\u04d0\u04c3\u0001\u0000\u0000\u0000\u04d0\u04c4"+
		"\u0001\u0000\u0000\u0000\u04d0\u04c5\u0001\u0000\u0000\u0000\u04d0\u04c6"+
		"\u0001\u0000\u0000\u0000\u04d0\u04c7\u0001\u0000\u0000\u0000\u04d0\u04c8"+
		"\u0001\u0000\u0000\u0000\u04d0\u04c9\u0001\u0000\u0000\u0000\u04d0\u04ca"+
		"\u0001\u0000\u0000\u0000\u04d0\u04cb\u0001\u0000\u0000\u0000\u04d0\u04cc"+
		"\u0001\u0000\u0000\u0000\u04d0\u04cd\u0001\u0000\u0000\u0000\u04d0\u04ce"+
		"\u0001\u0000\u0000\u0000\u04d0\u04cf\u0001\u0000\u0000\u0000\u04d1\u00c1"+
		"\u0001\u0000\u0000\u0000\u04d2\u04d3\u0007\u000b\u0000\u0000\u04d3\u00c3"+
		"\u0001\u0000\u0000\u0000\u04d4\u04d9\u0005\f\u0000\u0000\u04d5\u04d9\u0005"+
		"\u0000\u0000\u0001\u04d6\u04d9\u0004b(\u0000\u04d7\u04d9\u0004b)\u0000"+
		"\u04d8\u04d4\u0001\u0000\u0000\u0000\u04d8\u04d5\u0001\u0000\u0000\u0000"+
		"\u04d8\u04d6\u0001\u0000\u0000\u0000\u04d8\u04d7\u0001\u0000\u0000\u0000"+
		"\u04d9\u00c5\u0001\u0000\u0000\u0000\u008a\u00c7\u00ca\u00d3\u00d9\u00e1"+
		"\u00e4\u00e7\u00ff\u0103\u010a\u0110\u0114\u011b\u0123\u0128\u012a\u0131"+
		"\u0135\u013e\u0142\u014a\u014e\u0152\u015b\u0163\u0167\u016f\u0174\u0176"+
		"\u017d\u0182\u018d\u0196\u01a5\u01b9\u01bd\u01c1\u01c9\u01d2\u01d7\u01de"+
		"\u01e3\u01e8\u01ef\u01f6\u01fd\u020f\u0213\u0215\u021c\u0222\u0227\u0236"+
		"\u0239\u023e\u0241\u024c\u0250\u0255\u0260\u0266\u026e\u0274\u027a\u027e"+
		"\u0282\u0285\u028a\u0290\u0298\u029d\u02a2\u02a6\u02aa\u02b4\u02b9\u02bc"+
		"\u02c1\u02c8\u02cf\u02d7\u02de\u02e4\u02e6\u02f3\u02f6\u02fb\u030e\u0315"+
		"\u031f\u0324\u0327\u032b\u032f\u0335\u033c\u0344\u0347\u034b\u0352\u035b"+
		"\u035e\u0361\u0365\u0371\u0379\u037d\u037f\u0384\u0388\u0392\u039b\u03ca"+
		"\u03d6\u0414\u041c\u0420\u042d\u042f\u0439\u0441\u0445\u0447\u044d\u0451"+
		"\u0455\u045a\u0460\u0465\u0468\u046c\u0477\u047d\u0487\u0497\u049e\u04d0"+
		"\u04d8";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}