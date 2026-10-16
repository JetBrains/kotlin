// FILE: Outer.java

import lombok.*;

public class Outer {
    @NoArgsConstructor(force = true)
    public static class NestedNoArgs {
        public int value;

        public NestedNoArgs(int value) {
            this.value = value;
        }
    }

    @RequiredArgsConstructor
    public static class NestedRequiredArgs {
        public final int value;
        public int notRequired;
    }

    @AllArgsConstructor
    public static class NestedAllArgs {
        public int value;
        public int value2;
    }

    @Data
    public static class NestedData {
        private final int value;
    }

    @Value
    public static class NestedValue {
        int value;
    }

    static void javaUsage() {
        new NestedNoArgs();
        new NestedRequiredArgs(1);
        new NestedAllArgs(1, 2);
        new NestedData(1);
        new NestedValue(1);
    }
}

// FILE: GenericOuter.java

import lombok.*;

public class GenericOuter<T> {
    @NoArgsConstructor(force = true)
    public static class NestedNoArgs<T> {
        public T value;

        public NestedNoArgs(T value) {
            this.value = value;
        }
    }

    @AllArgsConstructor
    public static class NestedAllArgs<T> {
        public T value;
        public T value2;
    }

    static void javaUsage() {
        new GenericOuter.NestedNoArgs<String>();
        new GenericOuter.NestedAllArgs<String>("a", "b");
    }
}

// FILE: test.kt

import kotlin.test.assertEquals
import kotlin.test.assertNull

// Unlike their inner counterparts, the generated constructors of static nested classes take no outer instance.
fun box(): String {
    assertEquals(0, Outer.NestedNoArgs().value)
    assertEquals(1, Outer.NestedRequiredArgs(1).value)
    assertEquals(2, Outer.NestedAllArgs(1, 2).value2)
    assertEquals(1, Outer.NestedData(1).value)
    assertEquals(1, Outer.NestedValue(1).value)

    assertNull(GenericOuter.NestedNoArgs<String>().value)
    assertEquals("b", GenericOuter.NestedAllArgs("a", "b").value2)

    return "OK"
}
