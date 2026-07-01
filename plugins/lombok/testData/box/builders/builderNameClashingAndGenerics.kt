// ISSUE: KT-87137
// FILE: NameClashing.java

import lombok.Builder;

@Builder(builderClassName = "NameClashingBuilder")
public class NameClashing<T> {
    T field;

    public static class NameClashingBuilder<T> {
        void customMethod(T t) {
            field(t);
        }
    }

    public static void test() {
        NameClashingBuilder<String> builder = NameClashing.<String>builder();
        builder.field("FAIL").customMethod("OK");
        NameClashing<String> build = builder.build();
        String result = build.field;
    }
}

// FILE: NameClashingOnMethod.java

import lombok.Builder;

public class NameClashingOnMethod<T> {
    public T field;

    private NameClashingOnMethod() {}

    @Builder(builderClassName = "NameClashingOnMethodBuilder")
    public static <T> NameClashingOnMethod<T> create(T value) {
        NameClashingOnMethod<T> obj = new NameClashingOnMethod<>();
        obj.field = value;
        return obj;
    }

    public static class NameClashingOnMethodBuilder<T> {
        void customMethod(T t) {
            value(t);
        }
    }
}

// FILE: test.kt

fun testNameClashing() {
    val builder: NameClashing.NameClashingBuilder<String> = NameClashing.builder<String>()
    builder.field("FAIL").customMethod("OK")
    val obj: NameClashing<String> = builder.build()
    assertEquals("OK", obj.field)
}

fun testNameClashingOnMethod() {
    val builder: NameClashingOnMethod.NameClashingOnMethodBuilder<String> = NameClashingOnMethod.builder<String>()
    builder.value("FAIL").customMethod("OK")
    val obj: NameClashingOnMethod<String> = builder.build()
    assertEquals("OK", obj.field)
}

fun box(): String {
    NameClashing.test()
    testNameClashing()
    testNameClashingOnMethod()
    return "OK"
}
