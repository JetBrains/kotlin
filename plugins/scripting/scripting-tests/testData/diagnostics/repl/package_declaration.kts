// ISSUE: KT-84483

// SNIPPET

package org.first

class Foo(val bar: String)

// SNIPPET

package org.second

Foo("OK")
org.first.<!UNRESOLVED_REFERENCE!>Foo<!>("OK")
