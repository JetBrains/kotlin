// ISSUE: KT-74315

// FILE: Klass.java

import lombok.Builder;

@Builder
class Klass {
    public String str;

    // Use the class both for extending `Klass` builder and for generated methods of `KlassBuilder`
    @Builder
    static class KlassBuilder {
        public int integer;

        public KlassBuilder() {}

        public KlassBuilder(int integer, String str) {
            this.integer = integer;
            this.str = str;
        }

        void customMethod() {}
    }
}

// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val klassBuilder: Klass.KlassBuilder = Klass.builder() // Use the generated method on the existing class
    val class1: Klass = klassBuilder.str("hello").build() // Use the generated methods on the existing class
    klassBuilder.customMethod() // Use the existing custom method on the existing class
    assertEquals("hello", class1.str)

    val klassBuilderBuilder: Klass.KlassBuilder.KlassBuilderBuilder =
        Klass.KlassBuilder.builder() // Use the generated method on the existing class
    val class2: Klass.KlassBuilder =
        klassBuilderBuilder.integer(42).build() // Use the generated methods of the generated class

    assertEquals(42, class2.integer)
    return "OK"
}
