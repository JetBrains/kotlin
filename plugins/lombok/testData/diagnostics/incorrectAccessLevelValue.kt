// ISSUE: KT-85472

// FILE: Java.java

import lombok.AccessLevel;
import lombok.Getter;

public class Java {
    @Getter(AccessLevel.IncorrectAccessLevel) private String field;
}


// FILE: test.kt

fun test() {
    Java().getField()
}

