// FIR_IDENTICAL
// ISSUE: KT-83256

// FILE: ValuePlusDataBasic.java
import lombok.Data;
import lombok.NonNull;
import lombok.Value;

@Value
@Data
public class ValuePlusDataBasic {
    String name;

    @NonNull
    String lastName;
}

// FILE: ValueSetterFinalField.java
import lombok.Setter;
import lombok.Value;

@Value
public class ValueSetterFinalField {
    @Setter
    int x;
}

// FILE: TestJavaUsage.java
public class TestJavaUsage {
    public static void main(String[] args) {
        ValuePlusDataBasic valuePlusDataBasic = new ValuePlusDataBasic("John", "Snow");
        valuePlusDataBasic.setName("");     // error: cannot find symbol
        valuePlusDataBasic.setLastName(""); // error: cannot find symbol

        ValueSetterFinalField valueSetterFinalField = new ValueSetterFinalField(1);
        valueSetterFinalField.setX(1);      // cannot find symbol
    }
}

// FILE: test.kt

fun usage() {
    val valuePlusDataBasic = ValuePlusDataBasic("John", "Snow")
    valuePlusDataBasic.<!UNRESOLVED_REFERENCE!>setName<!>("")
    valuePlusDataBasic.<!UNRESOLVED_REFERENCE!>setLastName<!>("")

    val valueSetterFinalField = ValueSetterFinalField(1)
    valueSetterFinalField.<!UNRESOLVED_REFERENCE!>setX<!>(1)
}
