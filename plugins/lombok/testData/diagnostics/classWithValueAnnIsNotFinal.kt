// FIR_IDENTICAL
// ISSUE: KT-83252

// FILE: ValueFinalDefault.java
import lombok.Value;

@Value
public class ValueFinalDefault {
    int x;
}

// FILE: TestJavaUsage.java

public class ValueDefaultChild extends ValueFinalDefault { }  // error: cannot inherit from final ValueFinalDefault

// FILE: TestKotlinUsage.kt

class KotlinChild : ValueFinalDefault(1) // It shouldn't be OK