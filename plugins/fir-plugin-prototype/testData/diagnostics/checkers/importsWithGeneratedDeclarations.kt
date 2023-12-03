// FILE: test_NestedClassAndMaterializeMember.kt
package test

import org.jetbrains.kotlin.fir.plugin.NestedClassAndMaterializeMember

import test.MyClassWithNested.*

import test.MyClassWithNested.Nested
import test.MyClassWithNested.Nested.*

// does not exist
import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>FakeNested<!>
import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>FakeNested<!>.*

// cannot import member
import test.MyClassWithNested.<!CANNOT_BE_IMPORTED!>materialize<!>

@NestedClassAndMaterializeMember
class MyClassWithNested

// FILE: test_CompanionWithFoo.kt
package test

import org.jetbrains.kotlin.fir.plugin.CompanionWithFoo

import test.MyClassWithCompanion.*
import test.MyClassWithCompanion.Companion
import test.MyClassWithCompanion.Companion.foo

// cannot star import from companion
import test.MyClassWithCompanion.<!CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON!>Companion<!>.*

@CompanionWithFoo
class MyClassWithCompanion



