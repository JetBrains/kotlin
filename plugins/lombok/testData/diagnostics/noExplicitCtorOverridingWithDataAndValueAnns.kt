// FIR_IDENTICAL
// ISSUE: KT-83120

// FILE: DataExplicitCtor.java
import lombok.Data;

@Data
public class DataExplicitCtor {
    private final String name;

    public DataExplicitCtor(String name, String lastName) {
        this.name = name;
    }
}

// FILE: ValueExplicitCtor.java
import lombok.Value;

@Value
public class ValueExplicitCtor {
    int x;
    String y;

    public ValueExplicitCtor(int x) {
        this.x = x;
        this.y = "fixed";
    }
}

// FILE: DataExplicitCtorUsage.java
public class DataExplicitCtorUsage {
    public static void main(String[] args) {
        DataExplicitCtor dataExplicitCtor = new DataExplicitCtor("John"); // constructor DataExplicitCtor in class DataExplicitCtor cannot be applied to given types
    }
}

// FILE: ValueExplicitCtorUsage.java
public class ValueExplicitCtorUsage {
    public static void main(String[] args) {
        ValueExplicitCtor testJava = new ValueExplicitCtor(1, "John"); //constructor ValueExplicitCtor in class ValueExplicitCtor cannot be applied to given types
    }
}

// FILE: test.kt
fun usage() {
    DataExplicitCtor<!NO_VALUE_FOR_PARAMETER!>("John")<!>
    ValueExplicitCtor(1, <!TOO_MANY_ARGUMENTS!>"y"<!>)
}