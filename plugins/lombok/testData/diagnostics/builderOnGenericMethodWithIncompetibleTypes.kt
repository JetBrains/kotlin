// FIR_IDENTICAL
// WITH_STDLIB

// FILE: BuilderOnMethod.java
import lombok.Builder;

public class BuilderOnMethod<T> {
    final T name;

    private BuilderOnMethod(T name) {
        this.name = name;
    }

    @Builder
    public static <T> BuilderOnMethod<T> create(T name) {
        return new BuilderOnMethod<T>(name);
    }
}

// FILE: TestBuilders.java
import java.util.Arrays;

public class TestBuilders {
    public static void main(String[] args) {
        BuilderOnMethod test2 = BuilderOnMethod.<String>builder().name(1).build();
    }
}

// FILE: test.kt
fun test() {
    BuilderOnMethod.builder<String>().name(<!ARGUMENT_TYPE_MISMATCH!>1<!>).build()
}