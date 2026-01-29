// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-83334
// WITH_STDLIB

// FILE: GenericBuilder.java
import lombok.Builder;
import java.util.List;

@Builder
public class GenericBuilder<T> {
    private final T value;
    private final List<T> list;
}

// FILE: GenericSuperBuilder.java
@lombok.experimental.SuperBuilder
public class GenericSuperBuilder<T> {
    private final T value;
}

// FILE: GenericSuperBuilder2.java
@lombok.experimental.SuperBuilder
class GenericSuperBuilder2 extends GenericSuperBuilder<Integer> {
    private final String value2;
}

// FILE: Outer.java
import lombok.Builder;

public class Outer<K> {
    @Builder
    public static class Nested<T> {
        private final T value;
    }
}

// FILE: Ctor.java
import lombok.Builder;

public class Ctor<T> {
    private final T value;

    @Builder
    Ctor(T value) {
        this.value = value;
    }
}

// FILE: TestBuilders.java
import java.util.Arrays;

public class TestBuilders {
    public static void main(String[] args) {
        GenericBuilder.<String>builder().value("x").list(Arrays.asList("s1", "s2", "s3")).build(); //OK
        GenericSuperBuilder.<String>builder().value("str").build();  //OK
        GenericSuperBuilder2.builder().value(10).value2("str").build();  //OK
        Outer.Nested.<String>builder().value("x").build(); // OK
        Ctor.<String>builder().value("x").build(); // OK
    }
}

// FILE: test.kt
fun testBuilder() {
    val builderObj1 = GenericBuilder.builder<String>()
        .value("x")
        .list(listOf("s1", "s2", "s3"))
        .build()

    val builder: GenericBuilder.GenericBuilderBuilder<String> = GenericBuilder.builder<String>()
    val builder2: GenericBuilder.GenericBuilderBuilder<String> = builder.value("y").list(listOf("s11", "s22", "s33"))
    val builderObj2: GenericBuilder<String> = builder2.build();
}

fun testSuperBuilder() {
    val superBuilder: GenericSuperBuilder.GenericSuperBuilderBuilder<String, *, *> = GenericSuperBuilder.builder<String>();
    val superBuilder2: GenericSuperBuilder.GenericSuperBuilderBuilder<String, *, *> = superBuilder.value("str")
    val superBuilderObj: GenericSuperBuilder<String> = superBuilder2.build()
}

fun testSuperBuilder2() {
    // In Java it's yellow code, compiler reports the following:
    //     warning: [unchecked] builder() in GenericSuperBuilder3 overrides <T>builder() in GenericSuperBuilder
    //     @lombok.experimental.SuperBuilder
    //     ^
    //       return type requires unchecked conversion from GenericSuperBuilder3Builder<?,?> to GenericSuperBuilderBuilder<T,?,?>
    //       where T is a type-variable:
    //         T extends Object declared in method <T>builder()
    // However, support it in Kotlin at well.

    GenericSuperBuilder2.builder().value(10).value2("str").build();

    val superBuilder: GenericSuperBuilder2.GenericSuperBuilder2Builder<*, *> = GenericSuperBuilder2.builder()
    val superBuilder2: GenericSuperBuilder2.GenericSuperBuilder2Builder<*, *> = superBuilder.value(10)
    val superBuilder3: GenericSuperBuilder2.GenericSuperBuilder2Builder<*, *> = superBuilder2.value2("str")
    val superBuilderObj: GenericSuperBuilder2 = superBuilder3.build()
}

fun testCtor() {
    Ctor.builder<String>().value("x").build()
}

fun testNested() {
    val builder: Outer.Nested.NestedBuilder<String> = Outer.Nested.builder<String>()
    val builder2: Outer.Nested.NestedBuilder<String> = builder.value("x")
    val obj: Outer.Nested<String> = builder2.build()
}

fun box(): String {
    testBuilder()
    testSuperBuilder()
    testSuperBuilder2()
    testCtor()
    testNested()
    return "OK"
}
