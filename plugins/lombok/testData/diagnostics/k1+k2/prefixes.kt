// FILE: Prefixes.java

import lombok.*;
import lombok.experimental.*;

@Getter @Setter @Accessors(fluent = true, prefix = {"p1", "p2"})
public class Prefixes {
    private String p1Field;
    private Integer p2Field;

    private String p1; // Empty getter/setter name

    private String noPrefix;

    void test() {
        field(); // ERROR: Ambiguity
        noPrefix(); // ERROR: no getter/setter for the field
    }
}

// FILE: test.kt

fun test() {
    Prefixes().apply {
        field()
        <!FUNCTION_EXPECTED, INVISIBLE_MEMBER!>noPrefix<!>()
    }
}
