// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB

// FILE: RawTypeBuilder.java
import lombok.Builder;

import java.util.List;

@Builder
public class RawTypeBuilder {
    List list;
}

// FILE: TestBuilders.java
import java.util.*;

public class TestBuilders {
    public static void main(String[] args) {
        RawTypeBuilder b1 = new RawTypeBuilder(Arrays.asList("a", "b"));
        RawTypeBuilder b2 = RawTypeBuilder.builder().list(Arrays.asList("a", "b")).build();
    }
}

// FILE: test.kt
fun box(): String {
    val b1 = RawTypeBuilder(mutableListOf("O"))
    val b2 = RawTypeBuilder.builder().list(mutableListOf("K")).build()
    return b1.list[0].toString() + b2.list[0].toString()
}