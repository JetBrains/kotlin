// SCRIPT_PROVIDED_PROPERTIES: prop1: kotlin.String

val prop1 = 42 // TODO: Error should be reported on this shadowing, see KT-65809

val rv = 0 <!NONE_APPLICABLE!>+<!> prop1
