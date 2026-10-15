// ISSUE: KT-89742
// FILE: test/LevelExample.java

package test;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.PackagePrivate;

@FieldDefaults(level=AccessLevel.PRIVATE)
public class LevelExample {
    int defaultField = 1;
    public int publicField = 2;
    @PackagePrivate int packagePrivateField = 3;

    @FieldDefaults(level=AccessLevel.PROTECTED)
    public static class NestedExample {
        int defaultField = 4;
        @PackagePrivate int packagePrivateField = 5;
    }
}

// FILE: main.kt

package test

class Derived : LevelExample.NestedExample() {
    init {
        // protected access, should be allowed
        defaultField = 40
        // @PackagePrivate, same package, should be allowed
        packagePrivateField = 50
    }
}

fun foo() {
    val example = LevelExample()

    // defaultField becomes private, should not be allowed
    example.<!INVISIBLE_REFERENCE!>defaultField<!> = 10

    // publicField keeps its explicit public modifier
    example.publicField = 20

    // packagePrivateField: @PackagePrivate overrides level, same package, access allowed
    example.packagePrivateField = 30

    // same package, protected access, should be allowed
    LevelExample.NestedExample().defaultField = 60
}

// FILE: other.kt

package other

import test.*

class Derived : LevelExample.NestedExample() {
    init {
        // protected access, should be allowed
        defaultField = 40
        // @PackagePrivate, different package, should not be allowed
        <!INVISIBLE_REFERENCE!>packagePrivateField<!> = 50
    }
}

fun foo() {
    val example = LevelExample()

    // defaultField becomes private, should not be allowed
    example.<!INVISIBLE_REFERENCE!>defaultField<!> = 10

    // publicField keeps its explicit public modifier
    example.publicField = 20

    // packagePrivateField: @PackagePrivate overrides level, different package, access forbidden
    example.<!INVISIBLE_REFERENCE!>packagePrivateField<!> = 30

    // different package, protected access, should not be allowed
    LevelExample.NestedExample().<!INVISIBLE_REFERENCE!>defaultField<!> = 60
}
