// LL_FIR_DIVERGENCE
// KT-75132
// LL_FIR_DIVERGENCE
// FILE: test_NestedClassAndMaterializeMember.kt
package test

import org.jetbrains.kotlin.plugin.sandbox.NestedClassAndMaterializeMember

import test.MyClassWithNested.*

import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>Nested<!>
import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>Nested<!>.*

// does not exist
import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>FakeNested<!>
import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>FakeNested<!>.*

// cannot import member
import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>materialize<!>

@NestedClassAndMaterializeMember
class MyClassWithNested

// FILE: test_CompanionWithFoo.kt
package test

import org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo

import test.MyClassWithCompanion.*
import test.MyClassWithCompanion.Companion
import test.MyClassWithCompanion.Companion.foo

// cannot star import from companion
import test.MyClassWithCompanion.<!CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON!>Companion<!>.*

@CompanionWithFoo
class MyClassWithCompanion



