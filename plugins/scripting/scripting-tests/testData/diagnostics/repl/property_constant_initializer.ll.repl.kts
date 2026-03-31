// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE

// SNIPPET

const val x = 1
val y = x + 1

// SNIPPET

const val z = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!><!UNRESOLVED_REFERENCE!>x<!> + <!UNRESOLVED_REFERENCE!>y<!> + 1<!>
