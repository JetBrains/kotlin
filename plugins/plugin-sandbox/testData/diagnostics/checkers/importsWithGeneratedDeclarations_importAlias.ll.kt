// LL_FIR_DIVERGENCE
// KT-91614: The standalone KotlinAnnotationsResolver doesn't resolve an annotation used through an import alias
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
package test

import org.jetbrains.kotlin.plugin.sandbox.NestedClassAndMaterializeMember as Generate

import test.MyClassWithNested.*

import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>Nested<!>
import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>Nested<!>.*

// does not exist
import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>FakeNested<!>
import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>FakeNested<!>.*

// cannot import member
import test.MyClassWithNested.<!UNRESOLVED_IMPORT!>materialize<!>

@Generate
class MyClassWithNested

/* GENERATED_FIR_TAGS: classDeclaration */
