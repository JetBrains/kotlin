// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB

// FILE: BoundedTypeParameter.java
import lombok.Builder;
import lombok.Getter;

@Builder
public class BoundedTypeParameter<T extends CharSequence> {
    T value;
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
fun box(): String {
    val test1 = BoundedTypeParameter.builder<String>().value("OK").build()
    val test2 = BoundedTypeParameter("1")
    return test1.value
}