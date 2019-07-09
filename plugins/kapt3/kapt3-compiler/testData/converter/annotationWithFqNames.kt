// CORRECT_ERROR_TYPES
// NO_VALIDATION

//FILE: lib/Anno.java
package lib;

public @interface Anno {
    Class<?>[] impls() default {};
}

//FILE: lib/impl/Impl.java
package lib.impl;

public class Impl {}

// FILE: test.kt
@file:Suppress("UNRESOLVED_REFERENCE", "ANNOTATION_ARGUMENT_MUST_BE_CONST", "NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION")
package test

import lib.Anno
import lib.impl.Impl

typealias Joo = Impl

@Anno(impls = [Impl::class])
class Foo

@Anno(impls = [Impl::class, ABC::class])
class Bar

@Anno(impls = [Joo::class])
class Boo