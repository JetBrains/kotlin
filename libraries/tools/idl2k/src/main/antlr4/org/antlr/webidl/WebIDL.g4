/*
BSD License

Copyright (c) 2013, 2015 Rainer Schuster
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of Rainer Schuster nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Web IDL grammar derived from:

    http://heycam.github.io/webidl/

    Web IDL (Second Edition)
    W3C Editor's Draft 13 November 2014
 */
grammar WebIDL;

// Note: Replaced keywords: const, default, enum, interface, null.
// Note: Added "wrapper" rule webIDL with EOF token.

webIDL
	: namespace? definitions EOF
;

namespaceScope
  	: '*' | 'cpp' | 'java' | 'py' | 'perl' | 'rb' | 'cocoa' | 'csharp'
;

namespaceRest
    : IDENTIFIER_WEBIDL ( '.' IDENTIFIER_WEBIDL )*
    ;

namespace
	: 'namespace' namespaceScope? namespaceRest ';';

definitions
	: extendedAttributeList definition definitions
	| /* empty */
;

definition
	: callbackOrInterface
	| partial
	| dictionary
	| enum_
	| typedef
	| exception_
	| const_
	| module
	| implementsStatement
;

module
	: 'module' IDENTIFIER_WEBIDL '{' definitions '}' ';'
	;

callbackOrInterface
	: 'callback' callbackRestOrInterface
	| interface_
;

exception_
	: 'exception' IDENTIFIER_WEBIDL inheritance '{' dictionaryMembers '}' ';'
	;

callbackRestOrInterface
	: callbackRest
	| interface_
;

interface_
	: 'interface' IDENTIFIER_WEBIDL inheritance '{' interfaceMembers '}' ';'
;

partial
	: 'partial' partialDefinition
;

partialDefinition
	: partialInterface
	| partialDictionary
;

partialInterface
	: 'interface' IDENTIFIER_WEBIDL '{' interfaceMembers '}' ';'
;

interfaceMembers
	: extendedAttributeList interfaceMember interfaceMembers
	| /* empty */
;

interfaceMember
	: const_
	| operation
	| serializer
	| stringifier
	| staticMember
	| iterable
	| readonlyMember
	| readWriteAttribute
	| readWriteMaplike
	| readWriteSetlike
	| typedef
;

dictionary
	: 'dictionary' IDENTIFIER_WEBIDL inheritance '{' dictionaryMembers '}' ';'
;

dictionaryMembers
	: extendedAttributeList dictionaryMember dictionaryMembers
	| /* empty */
;

dictionaryMember
	: required type IDENTIFIER_WEBIDL default_ ';'
;

required
	: 'required'
	| /* empty */
;

partialDictionary
	: 'dictionary' IDENTIFIER_WEBIDL '{' dictionaryMembers '}' ';'
;

default_
	: '=' defaultValue
	| /* empty */
;

defaultValue
	: constValue
	| STRING_WEBIDL
	| '[' ']'
;

inheritance
	: ':' IDENTIFIER_WEBIDL ( ',' IDENTIFIER_WEBIDL )*
	| /* empty */
;

enum_
	: 'enum' IDENTIFIER_WEBIDL '{' enumValueList '}' ';'
;

enumValueList
	: STRING_WEBIDL enumValueListComma
;

enumValueListComma
	: ',' enumValueListString
	| /* empty */
;

enumValueListString
	: STRING_WEBIDL enumValueListComma
	| /* empty */
;

callbackRest
	: IDENTIFIER_WEBIDL '=' returnType '(' argumentList ')' ';'
;

typedef
	: 'typedef' type IDENTIFIER_WEBIDL ';'
;

implementsStatement
	: IDENTIFIER_WEBIDL 'implements' IDENTIFIER_WEBIDL ';'
;

const_
	: 'const' constType IDENTIFIER_WEBIDL '=' constValue ';'
;

constValue
	: booleanLiteral
	| floatLiteral
	| INTEGER_WEBIDL
	| 'null'
;

booleanLiteral
	: 'true'
	| 'false'
;

floatLiteral
	: FLOAT_WEBIDL
	| '-Infinity'
	| 'Infinity'
	| 'NaN'
;

serializer
	: 'serializer' serializerRest
;

serializerRest
	: operationRest
	| '=' serializationPattern
	| /* empty */
;

serializationPattern
	: '{' serializationPatternMap '}'
	| '[' serializationPatternList ']'
	| IDENTIFIER_WEBIDL
;

serializationPatternMap
	: 'getter'
	| 'inherit' identifiers
	| IDENTIFIER_WEBIDL identifiers
	| /* empty */
;

serializationPatternList
	: 'getter'
	| IDENTIFIER_WEBIDL identifiers
	| /* empty */
;

stringifier
	: 'stringifier' stringifierRest
;

stringifierRest
	: readOnly attributeRest
	| returnType operationRest
	| ';'
;

staticMember
	: 'static' staticMemberRest
;

staticMemberRest
	: readOnly attributeRest
	| returnType operationRest
;

readonlyMember
	: 'readonly' readonlyMemberRest
;

readonlyMemberRest
	: attributeRest
	| maplikeRest
	| setlikeRest
;

readWriteAttribute
	: 'inherit' readOnly attributeRest
	| attributeRest
;

attributeRest
	: 'attribute' type (IDENTIFIER_WEBIDL | 'required') attributeAnnotations* ';'
;

attributeAnnotations
	: IDENTIFIER_WEBIDL ( '(' type (',' type)* ')' )?
;

inherit
	: 'inherit'
	| /* empty */
;

readOnly
	: 'readonly'
	| /* empty */
;

operation
	: returnType operationRest
	| specialOperation
;

specialOperation
	: special specials returnType operationRest
;

specials
	: special specials
	| /* empty */
;

special
	: 'getter'
	| 'setter'
	| 'creator'
	| 'deleter'
	| 'legacycaller'
;

operationRest
	: optionalIdentifier '(' argumentList ')' attributeAnnotations* ';'
;

optionalIdentifier
	: IDENTIFIER_WEBIDL
	| /* empty */
;

argumentList
	: argument arguments
	| /* empty */
;

arguments
	: ',' argument arguments
	| /* empty */
;

argument
	: extendedAttributeList optionalOrRequiredArgument
;

optionalOrRequiredArgument
	: 'optional'? ('in'|'out')? type ellipsis argumentName default_
;

argumentName
	: argumentNameKeyword
	| IDENTIFIER_WEBIDL
;

ellipsis
	: '...'
	| /* empty */
;

iterable
	: 'iterable' '<' type optionalType '>' ';'
	| 'legacyiterable' '<' type '>' ';'
;

optionalType
	: ',' type
	| /* empty */
;

readWriteMaplike
	: maplikeRest
;

readWriteSetlike
	: setlikeRest
;

maplikeRest
	: 'maplike' '<' type ',' type '>' ';'
;

setlikeRest
	: 'setlike' '<' type '>' ';'
;

extendedAttributeList
	: '[' extendedAttribute extendedAttributes ']'
	| /* empty */
;

extendedAttributes
	: ',' extendedAttribute extendedAttributes
	| /* empty */
;

extendedAttribute
	: extendedAttributeNamePart? IDENTIFIER_WEBIDL ('(' argumentList ')')?
	| extendedAttributeNamePart? IDENTIFIER_WEBIDL? '(' argumentList ')'
	| extendedAttributeNamePart? IDENTIFIER_WEBIDL? '(' identifierList ')'
	| /* empty */
;

extendedAttributeNamePart
	: IDENTIFIER_WEBIDL '='
;

argumentNameKeyword
	: 'attribute'
	| 'callback'
	| 'const'
	| 'creator'
	| 'deleter'
	| 'dictionary'
	| 'enum'
	| 'getter'
	| 'implements'
	| 'inherit'
	| 'interface'
	| 'iterable'
	| 'legacycaller'
	| 'legacyiterable'
	| 'maplike'
	| 'partial'
	| 'required'
	| 'serializer'
	| 'setlike'
	| 'setter'
	| 'static'
	| 'stringifier'
	| 'typedef'
	| 'unrestricted'
	| 'namespace'
;

type
	: singleType
	| unionType typeSuffix
;

singleType
	: nonAnyType
	| 'any' typeSuffixStartingWithArray
;

unionType
	: '(' unionMemberType ( 'or' unionMemberType )* ')'
;

unionMemberType
	: nonAnyType
	| unionType typeSuffix
	| 'any' '[' ']' typeSuffix
;

unionMemberTypes
	: 'or' unionMemberType unionMemberTypes
	| /* empty */
;

nonAnyType
	: primitiveType typeSuffix
	| promiseType null_
	| 'ByteString' typeSuffix
	| 'DOMString' typeSuffix
	| 'USVString' typeSuffix
	| IDENTIFIER_WEBIDL typeSuffix
	| sequenceType null_
	| 'object' typeSuffix
	| 'Date' typeSuffix
	| 'RegExp' typeSuffix
	| 'DOMException' typeSuffix
	| IDENTIFIER_WEBIDL '<' type '>' null_
;

constType
	: primitiveType null_
	| IDENTIFIER_WEBIDL null_
;

primitiveType
	: unsignedIntegerType
	| unrestrictedFloatType
	| 'boolean'
	| 'byte'
	| 'octet'
;

unrestrictedFloatType
	: 'unrestricted' floatType
	| floatType
;

floatType
	: 'float'
	| 'double'
;

unsignedIntegerType
	: 'unsigned' integerType
	| integerType
;

integerType
	: 'short'
	| 'long'+
;

sequenceType
    : 'sequence' '<' type '>'
    | 'FrozenArray' '<' type '>'
;

promiseType
	: 'Promise' '<' returnType '>'
;

typeSuffix
	: '[' ']' typeSuffix
	| '?' typeSuffixStartingWithArray
	| /* empty */
;

typeSuffixStartingWithArray
	: '[' ']' typeSuffix
	| /* empty */
;

null_
	: '?'
	| /* empty */
;

returnType
	: type
	| 'void'
;

identifierList
	: IDENTIFIER_WEBIDL identifiers
;

identifiers
	: ',' IDENTIFIER_WEBIDL identifiers
	| /* empty */
;

extendedAttributeNoArgs
	: IDENTIFIER_WEBIDL
;

extendedAttributeArgList
	: IDENTIFIER_WEBIDL '(' argumentList ')'
;

extendedAttributeIdent
	: IDENTIFIER_WEBIDL '=' IDENTIFIER_WEBIDL
;

extendedAttributeIdentList
	: IDENTIFIER_WEBIDL '=' '(' identifierList ')'
;

extendedAttributeNamedArgList
	: IDENTIFIER_WEBIDL '=' IDENTIFIER_WEBIDL '(' argumentList ')'
;


INTEGER_WEBIDL
	: '-'?('0'([Xx][0-9A-Fa-f]+|[0-7]*)|[1-9][0-9]*)
;

FLOAT_WEBIDL
    : '-'?(([0-9]+'.'[0-9]*|[0-9]*'.'[0-9]+)([Ee][+\-]?[0-9]+)?|[0-9]+[Ee][+\-]?[0-9]+)
;

IDENTIFIER_WEBIDL
	: [A-Z_a-z][0-9A-Z_a-z]*
;

STRING_WEBIDL
	: '"' ~["]* '"'
;

WHITESPACE_WEBIDL
	: [\t\n\r ]+ -> channel(HIDDEN)
;

COMMENT_WEBIDL
	: ('//'~[\n\r]*|'/*'(.|'\n')*?'*/')+ -> channel(HIDDEN)
; // Note: '/''/'~[\n\r]* instead of '/''/'.* (non-greedy because of wildcard).

OTHER_WEBIDL
	: ~[\t\n\r 0-9A-Z_a-z]
;
