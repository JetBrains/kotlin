// FIR_IDENTICAL
// SKIP_TXT
// WITH_STDLIB
// ISSUE: KT-76414
// IGNORE_NON_REVERSED_RESOLVE

// FILE: main.kt
const val MY_CONST = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>JavaClass.BAZ<!>

// FILE: GeneratedSerializer.kt
package kotlinx.serialization.internal

import kotlinx.serialization.*

public interface GeneratedSerializer<T> : KSerializer<T> {
    public fun childSerializers(): Array<KSerializer<*>>
}

// FILE: JavaClass.java
public class JavaClass {
    public static final int BAZ = MySerializableClass.$serializer.BAR; // `BAR` is required to trigger light classes computation
}

// FILE: test.kt
import kotlinx.serialization.*

@Serializable
class MySerializableClass(val a: Int)
