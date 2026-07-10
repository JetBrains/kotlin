// FIR_DUMP

import lombok.Builder;

@Builder
class GenericBuilder<T>(val value: T, val list: List<T>)

class Outer<K> {
    @Builder
    class Nested<T>(val value: T)
}

fun testBuilder() {
    val builderObj1 = GenericBuilder.builder<String>()
        .value("x")
        .list(listOf("s1", "s2", "s3"))
        .build()

    val builder: GenericBuilder.GenericBuilderBuilder<String> = GenericBuilder.builder<String>()
    val builder2: GenericBuilder.GenericBuilderBuilder<String> = builder.value("y").list(listOf("s11", "s22", "s33"))
    val builderObj2: GenericBuilder<String> = builder2.build()
}

fun testNested() {
    val builder: Outer.Nested.NestedBuilder<String> = Outer.Nested.builder<String>()
    val builder2: Outer.Nested.NestedBuilder<String> = builder.value("x")
    val obj: Outer.Nested<String> = builder2.build()
}

fun box(): String {
    testBuilder()
    testNested()
    return "OK"
}
