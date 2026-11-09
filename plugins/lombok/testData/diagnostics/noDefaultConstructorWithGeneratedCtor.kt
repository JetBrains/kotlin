// ISSUE: KT-91096

// FILE: BuilderClass.java
import lombok.Builder;
@Builder
public class BuilderClass {
    String field = "value";
}

// FILE: AllArgs.java
import lombok.AllArgsConstructor;
@AllArgsConstructor
public class AllArgs {
    String field;
}

// FILE: RequiredArgs.java
import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor
public class RequiredArgs {
    private final String field;
}

// FILE: ValueClass.java
import lombok.Value;
@Value
public class ValueClass {
    String field;
}

// FILE: test.kt
fun main() {
    // In fact, all default constructors called below do not exist
    BuilderClass()
    AllArgs()
    RequiredArgs()
    ValueClass()
}
