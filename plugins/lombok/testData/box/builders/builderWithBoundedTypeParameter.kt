// IGNORE_BACKEND_K2: ANY

// FILE: BoundedTypeParameter.java
import lombok.Builder;
import lombok.Getter;

@Builder
public class BoundedTypeParameter<T extends CharSequence> {
    T value;
}

// FILE: SelfReferentialBound.java
import lombok.Builder;
import lombok.Getter;

// A bound that mentions the type parameter it bounds.
@Builder
@Getter
public class SelfReferentialBound<T extends Comparable<T>> {
    private final T value;
}

// FILE: ForwardReferentialBound.java
import lombok.Builder;
import lombok.Getter;

// A bound that mentions a type parameter declared after the one it bounds.
@Builder
@Getter
public class ForwardReferentialBound<A extends Comparable<B>, B> {
    private final A first;
    private final B second;
}

// FILE: TestBuilders.java
import java.util.Arrays;

public class TestBuilders {
    public static void main(String[] args) {
        BoundedTypeParameter<String> test1 = BoundedTypeParameter.<String>builder().value("").build();
        BoundedTypeParameter test2 = new BoundedTypeParameter<>("1");
    }
}

// FILE: test.kt
import kotlin.test.assertEquals

class Key : Comparable<String> {
    override fun compareTo(other: String): Int = 0
}

fun box(): String {
    val test1 = BoundedTypeParameter.builder<String>().value("OK").build()
    val test2 = BoundedTypeParameter("1")

    assertEquals(1, SelfReferentialBound.builder<<!UPPER_BOUND_VIOLATED!>Int<!>>().value(1).build().value)
    assertEquals("second", ForwardReferentialBound.builder<<!UPPER_BOUND_VIOLATED!>Key<!>, String>().first(Key()).second("second").build().second)

    return test1.value
}
