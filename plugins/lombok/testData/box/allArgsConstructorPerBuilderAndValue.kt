// FILE: ValueAndBuilder.java
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ValueAndBuilder {
    // Part of constructor because not initialized.
    String name;

    // Not part of constructor: @Value makes it final, and it is initialized.
    int age = 10;

    static ValueAndBuilder javaUsage() {
        return new ValueAndBuilder("OK");
    }
}

// FILE: test.kt
fun box(): String {
    ValueAndBuilder.javaUsage()
    return ValueAndBuilder("OK").getName()
}
