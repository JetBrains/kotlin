// ISSUE: KT-84058

// FILE: BuilderOnMethod.java

import lombok.Builder;

public class BuilderOnMethod<T> {
    public final T name;

    private BuilderOnMethod(T name) {
        this.name = name;
    }

    @Builder
    public static <T> BuilderOnMethod<T> create(T name) {
        return new BuilderOnMethod<>(name);
    }
}

// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val builder = BuilderOnMethod.builder<String>()
    val result = builder.name("name").build()
    assertEquals("name", result.name)
    return "OK"
}
