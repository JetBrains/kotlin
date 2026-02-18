// FIR_IDENTICAL
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB
// ISSUE: KT-84058

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
        BuilderOnMethod test1 = BuilderOnMethod.create("name");
        BuilderOnMethod test2 = BuilderOnMethod.<Integer>builder().name(1).build();
    }
}

// FILE: test.kt
fun box(): String {
    val test1 = BuilderOnMethod.create("OK")
    val test2 = BuilderOnMethod.builder<Int>().name(1).build()
    val test3 = BuilderOnMethod<Int>.builder<String>().name("").build()
    return test1.name
}