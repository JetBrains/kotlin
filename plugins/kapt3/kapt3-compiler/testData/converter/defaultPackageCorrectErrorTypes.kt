// CORRECT_ERROR_TYPES
// STRICT

// EXPECTED_ERROR: (kotlin:43:1) cannot find symbol
// EXPECTED_ERROR: (other:-1:-1) test.PackedClass: Can't reference type 'RootClass' from default package in Java stub.
// EXPECTED_ERROR: (other:-1:-1) test.PackedClass: Can't reference type 'AnotherRootClass' from default package in Java stub.
// EXPECTED_ERROR: (other:-1:-1) test.PackedWithAnnotation: Can't reference type 'RootClass' from default package in Java stub.

// FILE: test/ClassRefAnnotation.java

package test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ClassRefAnnotation {
    Class<?>[] value();
}

// FILE: a.kt

class RootClass

class AnotherRootClass

// FILE: b.kt
package test

import java.lang.Number as JavaNumber
import RootClass
import AnotherRootClass as Arc

interface PackedClass {
    fun someMethod(): RootClass
    fun otherMethod(): JavaNumber
    fun oneMoreMethod(): Arc
}

@ClassRefAnnotation(RootClass::class)
class PackedWithAnnotation
