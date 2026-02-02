// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB
// ISSUE: KT-84059

// FILE: ToBuilderGeneric.java
import lombok.Builder;

@Builder(toBuilder = true)
public class ToBuilderGeneric<T> {
    T value;
    String tag;
}


// FILE: TestBuilders.java
import java.util.*;

public class TestBuilders {
    public static void main(String[] args) {
        ToBuilderGeneric<String> orig = ToBuilderGeneric.<String>builder().value("a").tag("t").build();
        ToBuilderGeneric<String> modified = orig.toBuilder().value("b").build();
        String value = modified.value;
        String tag = modified.tag;
    }
}

// FILE: test.kt
fun box(): String {
    val orig = ToBuilderGeneric.builder<String>().value("not OK").tag("K").build()
    val modified: ToBuilderGeneric<String> = orig.<!CANNOT_INFER_PARAMETER_TYPE!>toBuilder<!>().value("O").build()
    return modified.value + modified.tag
}