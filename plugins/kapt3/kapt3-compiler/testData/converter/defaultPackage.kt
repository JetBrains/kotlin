// CORRECT_ERROR_TYPES
// STRICT

//FILE: test/ClassRefAnnotation.java

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

//FILE: a.kt

class RootClass

//FILE: b.kt
package test

import java.lang.Number as JavaNumber
import RootClass

interface PackedClass {
    fun someMethod(): RootClass
    fun otherMethod(): JavaNumber
}

@ClassRefAnnotation(RootClass::class)
class PackedWithAnnotation

// EXPECTED_ERROR(kotlin:8:5) cannot find symbol
// EXPECTED_ERROR(kotlin:12:1) cannot find symbol
// EXPECTED_ERROR(other:-1:-1) test.PackedClass: Can't reference type 'RootClass' from default package in Java stub.
// EXPECTED_ERROR(other:-1:-1) test.PackedWithAnnotation: Can't reference type 'RootClass' from default package in Java stub.
