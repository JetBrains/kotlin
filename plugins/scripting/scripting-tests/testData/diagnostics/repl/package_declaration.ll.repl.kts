// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE

// ISSUE: KT-84483

// SNIPPET

package org.first

class Foo(val bar: String)

// SNIPPET

package org.second

<!UNRESOLVED_REFERENCE!>Foo<!>("OK")
org.first.Foo("OK")
