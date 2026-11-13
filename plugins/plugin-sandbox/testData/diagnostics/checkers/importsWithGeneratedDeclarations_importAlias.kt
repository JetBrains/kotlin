// RUN_PIPELINE_TILL: FRONTEND
package test

import org.jetbrains.kotlin.plugin.sandbox.NestedClassAndMaterializeMember as Generate

import test.MyClassWithNested.*

import test.MyClassWithNested.Nested
import test.MyClassWithNested.Nested.*

// does not exist
import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>FakeNested<!>
import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>FakeNested<!>.*

// cannot import member
import test.MyClassWithNested.<!CANNOT_BE_IMPORTED!>materialize<!>

@Generate
class MyClassWithNested

/* GENERATED_FIR_TAGS: classDeclaration */
