// ISSUE: KT-76529, KT-89742
// DUMP_IR
// FILE: test/java/IdContainer.java

package test.java;

import lombok.*;
import lombok.experimental.*;

@Getter
public class IdContainer {
	int id = 0;

    public int publicId = 1;

    @Getter
    @FieldDefaults(level = AccessLevel.PROTECTED)
    public static class Nested {
    	int id = 3;

        @PackagePrivate int packageId = 5;
    }
}

// FILE: main.kt

package test

import kotlin.test.assertEquals
import test.java.IdContainer
import test.java.IdContainer.Nested

class Derived : Nested() {
    // Should access the field
    fun foo() = id
}

class DerivedContainer : IdContainer() {
    // Should call the getter, and not access the field
    fun bar() = id
}

fun box(): String {
    val container = DerivedContainer()
    assertEquals(container.bar(), 0)
    // Field
    assertEquals(container.publicId, 1)

    val derived = Derived()
    assertEquals(derived.foo(), 3)
    // Accessor
    assertEquals(derived.packageId, 5)

    return "OK"
}

// FILE: lombok.config

config.stopBubbling = true
lombok.addLombokGeneratedAnnotation = true
lombok.fieldDefaults.defaultPrivate = true
