/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 by Bart Kiers (original author) and Alexandre Vitorelli (contributor -> ported to CSharp)
 * Copyright (c) 2017-2020 by Ivan Kochurkin (Positive Technologies):
    added ECMAScript 6 support, cleared and transformed to the universal grammar.
 * Copyright (c) 2018 by Juan Alvarez (contributor -> ported to Go)
 * Copyright (c) 2019 by Student Main (contributor -> ES2020)
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

parser grammar JavaScriptParser;

@header {
import org.jetbrains.kotlin.js.parser.antlr.JavaScriptParserBase;
import org.jetbrains.kotlin.js.parser.antlr.JavaScriptRuleContext;
}

options {
    tokenVocab = JavaScriptLexer;
    superClass = JavaScriptParserBase;
    contextSuperClass = JavaScriptRuleContext;
}

program
    : HashBangLine? sourceElements? EOF
    ;

sourceElement
    : statement
    ;

optionalStatements
    : statement* EOF
    ;

optionalSingleExpression
    : singleExpression? EOF
    ;

optionalExpressionOrStatement
    : singleExpression EOF
    | statementList? EOF
    ;

optionalFunction
    : functionDeclaration? EOF
    ;

statement
    : block
    | variableStatement
    | importStatement
    | exportStatement
    | emptyStatement_
    | classDeclaration
    | functionDeclaration
    | expressionStatement
    | ifStatement
    | iterationStatement
    | continueStatement
    | breakStatement
    | returnStatement
    | yieldStatement
    | withStatement
    | labelledStatement
    | switchStatement
    | throwStatement
    | tryStatement
    | debuggerStatement
    ;

block
    : '{' statementList? '}'
    ;

statementList
    : statement+
    ;

importStatement
    : Import importFromBlock
    ;

importFromBlock
    : importDefault? (importNamespace | importModuleItems) importFrom eos
    | StringLiteral eos
    ;

importModuleItems
    : '{' (importAliasName ',')* (importAliasName ','?)? '}'
    ;

importAliasName
    : moduleExportName (As importedBinding)?
    ;

moduleExportName
    : identifierName
    | StringLiteral
    ;

// yield and await are permitted as BindingIdentifier in the grammar
importedBinding
    : Identifier
    | Yield
    | Await
    ;

importDefault
    : aliasName ','
    ;

importNamespace
    : ('*' | identifierName) (As identifierName)?
    ;

importFrom
    : From StringLiteral
    ;

aliasName
    : identifierName (As identifierName)?
    ;

exportStatement
    : Export Default? (exportFromBlock | declaration) eos # ExportDeclaration
    | Export Default singleExpression eos                 # ExportDefaultDeclaration
    ;

exportFromBlock
    : importNamespace importFrom eos
    | exportModuleItems importFrom? eos
    ;

exportModuleItems
    : '{' (exportAliasName ',')* (exportAliasName ','?)? '}'
    ;

exportAliasName
    : moduleExportName (As moduleExportName)?
    ;

declaration
    : variableStatement
    | classDeclaration
    | functionDeclaration
    ;

variableStatement
    : variableDeclarationList eos
    ;

variableDeclarationList
    : varModifier variableDeclaration (',' variableDeclaration)*
    ;

singleVariableDeclaration
    : varModifier variableDeclaration
    ;

variableDeclaration
    : assignable ('=' singleExpression)? // ECMAScript 6: Array & Object Matching
    ;

emptyStatement_
    : SemiColon
    ;

expressionStatement
    : {this.notOpenBraceAndNotFunction()}? expressionSequence eos
    ;

ifStatement
    : If '(' expressionSequence ')' statement (Else statement)?
    ;

iterationStatement
    : Do statement While '(' expressionSequence ')' eos                                                                     # DoStatement
    | While '(' expressionSequence ')' statement                                                                            # WhileStatement
    | For '(' (vars=expressionSequence | var=variableDeclarationList)?
            ';' condition=expressionSequence?
            ';' increment=expressionSequence?
          ')' statement # ForStatement
    | For '(' (singleExpression | singleVariableDeclaration) In expressionSequence ')' statement                              # ForInStatement
    | For Await? '(' (singleExpression | singleVariableDeclaration) Of expressionSequence ')' statement                       # ForOfStatement
    ;

varModifier // let, const - ECMAScript 6
    : Var
    | let_
    | Const
    ;

continueStatement
    : Continue ({this.notLineTerminator()}? identifier)? eos
    ;

breakStatement
    : Break ({this.notLineTerminator()}? identifier)? eos
    ;

returnStatement
    : Return ({this.notLineTerminator()}? expressionSequence)? eos
    ;

yieldStatement
    : (Yield | YieldStar) ({this.notLineTerminator()}? expressionSequence)? eos
    ;

withStatement
    : With '(' expressionSequence ')' statement
    ;

switchStatement
    : Switch '(' expressionSequence ')' caseBlock
    ;

caseBlock
    : '{' beforeDefault=caseClauses? (defaultClause afterDefault=caseClauses?)? '}'
    ;

caseClauses
    : caseClause+
    ;

caseClause
    : Case expressionSequence ':' statementList?
    ;

defaultClause
    : Default ':' statementList?
    ;

labelledStatement
    : identifier ':' statement
    ;

throwStatement
    : Throw {this.notLineTerminator()}? expressionSequence eos
    ;

tryStatement
    : Try block (catchProduction finallyProduction? | finallyProduction)
    ;

catchProduction
    : Catch ('(' assignable? ')')? block
    ;

finallyProduction
    : Finally block
    ;

debuggerStatement
    : Debugger eos
    ;

functionDeclaration
    : Async? Function_ '*'? identifier '(' formalParameterList? ')' functionBody
    ;

classDeclaration
    : Class identifier classTail
    ;

classTail
    : (Extends singleExpression)? '{' classElement* '}'
    ;

classElement
    : (Static | {this.n("static")}? identifier)? methodDefinition
    | (Static | {this.n("static")}? identifier)? fieldDefinition
    | (Static | {this.n("static")}? identifier) block
    | emptyStatement_
    ;

methodDefinition
    : (Async {this.notLineTerminator()}?)? '*'? classElementName '(' formalParameterList? ')' functionBody
    | '*'? getter '(' ')' functionBody
    | '*'? setter '(' formalParameterList? ')' functionBody
    ;

fieldDefinition
    : classElementName initializer?
    ;

classElementName
    : propertyName
    | privateIdentifier
    ;

privateIdentifier
    : '#' identifierName
    ;

formalParameterList
    : formalParameterArg (',' formalParameterArg)* (',' restParameterArg)?
    | restParameterArg
    ;

formalParameterArg
    : assignable ('=' singleExpression)? // ECMAScript 6: Initialization
    ;

restParameterArg // ECMAScript 6: Rest Parameter
    : Ellipsis singleExpression
    ;

functionBody
    : '{' sourceElements? '}'
    ;

sourceElements
    : sourceElement+
    ;

arrayLiteral
    : ('[' elementList ']')
    ;

// JavaScript supports arrays like [,,1,2,,].
elementList
    : ','* arrayElement? (','+ arrayElement) * ','* // Yes, everything is optional
    ;

arrayElement
    : Ellipsis? singleExpression
    ;

propertyAssignment
    : propertyName ':' singleExpression                                  # PropertyExpressionAssignment
    | '[' singleExpression ']' ':' singleExpression                      # ComputedPropertyExpressionAssignment
    | Async? '*'? propertyName '(' formalParameterList? ')' functionBody # FunctionProperty
    | getter '(' ')' functionBody                                        # PropertyGetter
    | setter '(' formalParameterArg ')' functionBody                     # PropertySetter
    | Ellipsis? singleExpression                                         # PropertyShorthand
    ;

propertyName
    : identifierName
    | StringLiteral
    | numericLiteral
    | '[' singleExpression ']'
    ;

arguments
    : '(' (argument (',' argument)* ','?)? ')'
    ;

argument
    : Ellipsis? (singleExpression | identifier)
    ;

expressionSequence
    : lhs=expressionSequence ',' rhs=expressionSequence
    | singleExpression
    ;

singleExpression
    : singleExpressionImpl
    ;

singleExpressionImpl
    : anonymousFunction                                                            # FunctionExpression
    | Class identifier? classTail                                                  # ClassExpression
    | singleExpressionImpl '?.' singleExpressionImpl                               # OptionalChainExpression
    | singleExpressionImpl '?.'? '[' expressionSequence ']'                        # MemberIndexExpression
    | singleExpressionImpl '?'? '.' '#'? identifierName                            # MemberDotExpression
    // Split to try `new Date()` first, then `new Date`.
    | New identifier arguments                                                     # NewExpression
    | New singleExpressionImpl arguments                                           # NewExpression
    | New singleExpressionImpl                                                     # NewExpression
    | singleExpressionImpl arguments                                               # ArgumentsExpression
    | New '.' identifier                                                           # MetaExpression // new.target
    | singleExpressionImpl {this.notLineTerminator()}? '++'                        # PostIncrementExpression
    | singleExpressionImpl {this.notLineTerminator()}? '--'                        # PostDecreaseExpression
    | Delete singleExpressionImpl                                                  # DeleteExpression
    | Void singleExpressionImpl                                                    # VoidExpression
    | Typeof singleExpressionImpl                                                  # TypeofExpression
    | '++' singleExpressionImpl                                                    # PreIncrementExpression
    | '--' singleExpressionImpl                                                    # PreDecreaseExpression
    | '+' singleExpressionImpl                                                     # UnaryPlusExpression
    | '-' singleExpressionImpl                                                     # UnaryMinusExpression
    | '~' singleExpressionImpl                                                     # BitNotExpression
    | '!' singleExpressionImpl                                                     # NotExpression
    | Await singleExpressionImpl                                                   # AwaitExpression
    | <assoc = right> singleExpressionImpl '**' singleExpressionImpl               # PowerExpression
    | singleExpressionImpl ('*' | '/' | '%') singleExpressionImpl                  # MultiplicativeExpression
    | singleExpressionImpl ('+' | '-') singleExpressionImpl                        # AdditiveExpression
    | singleExpressionImpl '??' singleExpressionImpl                               # CoalesceExpression
    | singleExpressionImpl ('<<' | '>>' | '>>>') singleExpressionImpl              # BitShiftExpression
    | singleExpressionImpl ('<' | '>' | '<=' | '>=') singleExpressionImpl          # RelationalExpression
    | singleExpressionImpl Instanceof singleExpressionImpl                         # InstanceofExpression
    | singleExpressionImpl In singleExpressionImpl                                 # InExpression
    | singleExpressionImpl ('==' | '!=' | '===' | '!==') singleExpressionImpl      # EqualityExpression
    | singleExpressionImpl '&' singleExpressionImpl                                # BitAndExpression
    | singleExpressionImpl '^' singleExpressionImpl                                # BitXOrExpression
    | singleExpressionImpl '|' singleExpressionImpl                                # BitOrExpression
    | singleExpressionImpl '&&' singleExpressionImpl                               # LogicalAndExpression
    | singleExpressionImpl '||' singleExpressionImpl                               # LogicalOrExpression
    | <assoc = right> singleExpressionImpl '?' singleExpressionImpl ':' singleExpressionImpl # TernaryExpression
    | <assoc = right> lhs=singleExpressionImpl '=' rhs=singleExpressionImpl                # AssignmentExpression
    | <assoc = right> lhs=singleExpressionImpl assignmentOperator rhs=singleExpressionImpl # AssignmentOperatorExpression
    | Import '(' singleExpressionImpl ')'                                          # ImportExpression
    | singleExpressionImpl templateStringLiteral                                   # TemplateStringExpression // ECMAScript 6
    | (Yield | YieldStar) ({this.notLineTerminator()}? expressionSequence)?        # YieldExpression          // ECMAScript 6
    | This                                                                         # ThisExpression
    | identifier                                                                   # IdentifierExpression
    | Super                                                                        # SuperExpression
    | Import '.' Meta                                                              # ImportMetaExpression
    | literal                                                                      # LiteralExpression
    | arrayLiteral                                                                 # ArrayLiteralExpression
    | objectLiteral                                                                # ObjectLiteralExpression
    | '(' expressionSequence ')'                                                   # ParenthesizedExpression
    ;

initializer
    // TODO: must be `= AssignmentExpression` and we have such label alredy but it doesn't respect the specification.
    //  See https://tc39.es/ecma262/multipage/ecmascript-language-expressions.html#prod-Initializer
    : '=' singleExpression
    ;

assignable
    : identifier
    | keyword
    | arrayLiteral
    | objectLiteral
    ;

objectLiteral
    : '{' (propertyAssignment (',' propertyAssignment)* ','?)? '}'
    ;

anonymousFunction
    : functionDeclaration                                             # NamedFunction
    | Async? Function_ '*'? '(' formalParameterList? ')' functionBody # AnonymousFunctionDecl
    | Async? arrowFunctionParameters '=>' arrowFunctionBody           # ArrowFunction
    ;

arrowFunctionParameters
    : propertyName
    | '(' formalParameterList? ')'
    ;

arrowFunctionBody
    : singleExpression
    | functionBody
    ;

assignmentOperator
    : '*='
    | '/='
    | '%='
    | '+='
    | '-='
    | '<<='
    | '>>='
    | '>>>='
    | '&='
    | '^='
    | '|='
    | '**='
    | '??='
    ;

literal
    : NullLiteral
    | BooleanLiteral
    | StringLiteral
    | templateStringLiteral
    | RegularExpressionLiteral
    | numericLiteral
    | bigintLiteral
    ;

templateStringLiteral
    : BackTick templateStringAtom* BackTick
    ;

templateStringAtom
    : TemplateStringAtom
    | TemplateStringStartExpression singleExpression TemplateCloseBrace
    ;

numericLiteral
    : DecimalLiteral
    | HexIntegerLiteral
    | OctalIntegerLiteral
    | OctalIntegerLiteral2
    | BinaryIntegerLiteral
    ;

bigintLiteral
    : BigDecimalIntegerLiteral
    | BigHexIntegerLiteral
    | BigOctalIntegerLiteral
    | BigBinaryIntegerLiteral
    ;

getter
    : {this.n("get")}? identifier classElementName
    ;

setter
    : {this.n("set")}? identifier classElementName
    ;

identifierName
    : identifier
    | reservedWord
    ;

identifier
    : Identifier
    | NonStrictLet
    | Async
    | As
    | From
    | Yield
    | Of
    ;

reservedWord
    : keyword
    | NullLiteral
    | BooleanLiteral
    ;

keyword
    : Break
    | Do
    | Instanceof
    | Typeof
    | Case
    | Else
    | New
    | Var
    | Catch
    | Finally
    | Return
    | Void
    | Continue
    | For
    | Switch
    | While
    | Debugger
    | Function_
    | This
    | With
    | Default
    | If
    | Throw
    | Delete
    | In
    | Try
    | Class
    | Enum
    | Extends
    | Super
    | Const
    | Export
    | Import
    | Implements
    | let_
    | Private
    | Public
    | Interface
    | Package
    | Protected
    | Static
    | Yield
    | YieldStar    
    | Async
    | Await
    | From
    | As
    | Of
    ;

let_
    : NonStrictLet
    | StrictLet
    ;

eos
    : SemiColon
    | EOF
    | {this.lineTerminatorAhead()}?
    | {this.closeBrace()}?
    ;