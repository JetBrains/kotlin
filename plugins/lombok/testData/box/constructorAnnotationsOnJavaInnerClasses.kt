// ISSUE: KT-89169
// FILE: Outer.java

import lombok.*;

public class Outer {
    @NoArgsConstructor(force = true)
    public class InnerNoArgs {
        public int value;

        public InnerNoArgs(int value) {
            this.value = value;
        }
    }

    @RequiredArgsConstructor
    public class InnerRequiredArgs {
        public final int value;
        public int notRequired;
    }

    @AllArgsConstructor
    public class InnerAllArgs {
        public int value;
        public int value2;
    }

    @Data
    public class InnerData {
        private final int value;
    }

    @Value
    public class InnerValue {
        int value;
    }

    void javaUsage() {
        new InnerNoArgs();
        new InnerRequiredArgs(1);
        new InnerAllArgs(1, 2);
        new InnerData(1);
        new InnerValue(1);
    }
}

// FILE: GenericOuter.java

import lombok.*;

public class GenericOuter<T> {
    @NoArgsConstructor(force = true)
    public class InnerNoArgs {
        public T value;

        public InnerNoArgs(T value) {
            this.value = value;
        }
    }

    @RequiredArgsConstructor
    public class InnerRequiredArgs {
        public final T value;
        public T notRequired;
    }

    @AllArgsConstructor
    public class InnerAllArgs {
        public T value;
        public T value2;
    }

    @Data
    public class InnerData {
        private final T value;
    }

    @Value
    public class InnerValue {
        T value;
    }

    static void javaUsage() {
        GenericOuter<String> outer = new GenericOuter<String>();
        outer.new InnerNoArgs();
        outer.new InnerRequiredArgs("a");
        outer.new InnerAllArgs("a", "b");
        outer.new InnerData("a");
        outer.new InnerValue("a");
    }
}

// FILE: test.kt

// `Outer` pins the dispatch receiver the generated constructor carries, `GenericOuter` pins that it declares
// only the inner class's own type parameters and not the outer class's as well.
fun box(): String {
    val outer = Outer()
    assertEquals(0, outer.InnerNoArgs().value)
    assertEquals(1, outer.InnerRequiredArgs(1).value)
    assertEquals(2, outer.InnerAllArgs(1, 2).value2)
    assertEquals(1, outer.InnerData(1).value)
    assertEquals(1, outer.InnerValue(1).value)

    val genericOuter = GenericOuter<String>()
    assertEquals(null, genericOuter.InnerNoArgs().value)
    assertEquals("a", genericOuter.InnerRequiredArgs("a").value)
    assertEquals("b", genericOuter.InnerAllArgs("a", "b").value2)
    assertEquals("a", genericOuter.InnerData("a").value)
    assertEquals("a", genericOuter.InnerValue("a").value)

    return "OK"
}
