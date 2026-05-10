// ISSUE: KT-85693

// FILE: NoneAccessLevel.java

import lombok.*;

@AllArgsConstructor(access = AccessLevel.NONE)
public class NoneAccessLevel {
    int intField;
}

// FILE: test.kt

fun usage() {
    NoneAccessLevel(<!TOO_MANY_ARGUMENTS!>1<!>);
}
