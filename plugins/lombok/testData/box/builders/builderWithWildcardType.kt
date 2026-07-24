// WITH_STDLIB

// FILE: WildcardField.java
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WildcardField {
    List<? extends CharSequence> list;
}

// FILE: TestBuilders.java
import java.util.*;

public class TestBuilders {
    public static void main(String[] args) {
        List<String> strings = Arrays.asList("", "1");
        WildcardField wf = WildcardField.builder().list(strings).build();
    }
}

// FILE: test.kt
fun box(): String {
    val wf = WildcardField.builder().list(mutableListOf("OK")).build()
    val a: CharSequence = wf.getList()[0]
    return a as String
}