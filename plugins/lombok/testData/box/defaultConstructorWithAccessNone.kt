// ISSUE: KT-91096

// FILE: AllArgsNone.java
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.NONE)
public class AllArgsNone {
    String field = "OK";

    static AllArgsNone javaUsage() {
        return new AllArgsNone();
    }
}

// FILE: test.kt
fun box(): String {
    AllArgsNone.javaUsage()
    return AllArgsNone().field
}
