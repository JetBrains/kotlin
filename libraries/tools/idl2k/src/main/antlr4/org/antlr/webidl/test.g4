grammar test;

testme
    : namespace? declarations EOF
    ;

declarations
    : declaration*
    ;

namespace
    : 'namespace' IDENTIFIER ';'
    ;

declaration
    : 'interface' IDENTIFIER
    ;

IDENTIFIER
    : [A-Za-z_][A-Za-z_0-9]*
    ;

WHITESPACE_WEBIDL
	: [\t\n\r ]+ -> channel(HIDDEN)
;