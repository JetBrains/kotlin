// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-83334

// FILE: GenericBuilder.java
import lombok.Builder;

@Builder
public class GenericBuilder<T> {
    private final T value;
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

// FILE: TestBuidlers.java
public class TestBuidlers {
    public static void main(String[] args) {
        GenericBuilder.<String>builder().value("x").build(); //OK
        GenericSuperBuilder.<String>builder().value("str").build();  //OK
        GenericSuperBuilder2.builder().value(10).value2("str").build();  //OK
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

// FILE: test.kt
fun testBuilder() {
    val builderObj1 = GenericBuilder.builder<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>().value(<!ARGUMENT_TYPE_MISMATCH!>"x"<!>).build()

    val builder: GenericBuilder.GenericBuilderBuilder<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!> = GenericBuilder.builder<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>()
    val builder2: GenericBuilder.GenericBuilderBuilder<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!> = builder.<!UNRESOLVED_REFERENCE!>value<!>("x")
    val builderObj2: GenericBuilder<String> = builder2.<!UNRESOLVED_REFERENCE!>build<!>();
}

fun testSuperBuilder() {
    val superBuilder: GenericSuperBuilder.GenericSuperBuilderBuilder<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, *, *><!> = GenericSuperBuilder.builder<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>();
    val superBuilder2: GenericSuperBuilder.GenericSuperBuilderBuilder<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, *, *><!> = superBuilder.<!UNRESOLVED_REFERENCE!>value<!>("str")
    val superBuilderObj: GenericSuperBuilder<String> = superBuilder2.<!UNRESOLVED_REFERENCE!>build<!>()
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

    GenericSuperBuilder2.builder().value(<!ARGUMENT_TYPE_MISMATCH!>10<!>).value2("str").build();

    val superBuilder: GenericSuperBuilder2.GenericSuperBuilder2Builder<*, *> = GenericSuperBuilder2.builder()
    val superBuilder2: GenericSuperBuilder2.GenericSuperBuilder2Builder<*, *> = superBuilder.value(<!ARGUMENT_TYPE_MISMATCH!>10<!>)
    val superBuilder3: GenericSuperBuilder2.GenericSuperBuilder2Builder<*, *> = superBuilder2.value2("str")
    val superBuilderObj: GenericSuperBuilder2 = superBuilder3.build()
}

fun testCtor() {
    Ctor.builder<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>().value(<!ARGUMENT_TYPE_MISMATCH!>"x"<!>).build()
}

fun box(): String {
    testBuilder()
    testSuperBuilder()
    testSuperBuilder2()
    testCtor()
    return "OK"
}
