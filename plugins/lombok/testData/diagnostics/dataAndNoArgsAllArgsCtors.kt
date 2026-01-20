// FIR_IDENTICAL
// ISSUE: KT-83204

// FILE: DataAndNoArgs.java
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor(force = true)
public class DataAndNoArgs {
    final String name;

    @NonNull
    String lastName;

    int normalField;
}

// FILE: DataAndAllArgs.java

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataAndAllArgs {
    final String name;
    int normalField;
}

// FILE: TestJavaUsage.java
public class TestJavaUsage {
    public static void main(String[] args) {
        DataAndNoArgs o1 = new DataAndNoArgs("name", "lastName"); // constructor DataAndNoArgs in class DataAndNoArgs cannot be applied to given types
        DataAndAllArgs o2 = new DataAndAllArgs("abc"); // error
    }
}

// FILE: UsageFromKotlin.kt
fun main() {
    DataAndNoArgs(<!TOO_MANY_ARGUMENTS!>"name"<!>, <!TOO_MANY_ARGUMENTS!>"lastName"<!>)
    DataAndNoArgs()
    <!NONE_APPLICABLE!>DataAndAllArgs<!>("abc")
    DataAndAllArgs("abc", 3)
}
