// SNIPPET

// Future explicit type.
val a by lazy { b }

// Self-reference explicit type.
val b: Int by lazy { b }

// Previous implicit type.
val c by lazy { a }

// Self-reference implicit type.
<!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>val d by <!CANNOT_INFER_PARAMETER_TYPE!><!CANNOT_INFER_PARAMETER_TYPE!>lazy<!> { <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>d<!> }<!><!>
