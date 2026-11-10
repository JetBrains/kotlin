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

// FILE: DataClass.java
import lombok.Data;
@Data
public class DataClass {
    private final String field;
}

// FILE: SuperBuilderClass.java
import lombok.experimental.SuperBuilder;
@SuperBuilder
public class SuperBuilderClass {
    String field;
}

// FILE: AllArgsStaticName.java
import lombok.AllArgsConstructor;
@AllArgsConstructor(staticName = "of")
public class AllArgsStaticName {
}

// FILE: AllAndNoArgs.java
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
@AllArgsConstructor
@NoArgsConstructor
public class AllAndNoArgs {
    String field;
}

// FILE: RequiredArgsWithoutRequiredFields.java
import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor
public class RequiredArgsWithoutRequiredFields {
    String field;
}

// FILE: AllArgsWithoutFields.java
import lombok.AllArgsConstructor;
@AllArgsConstructor
public class AllArgsWithoutFields {
}

// FILE: DataWithoutRequiredFields.java
import lombok.Data;
@Data
public class DataWithoutRequiredFields {
    private String field;
}

// FILE: BuilderWithExplicitCtor.java
import lombok.Builder;
@Builder
public class BuilderWithExplicitCtor {
    String field;
    public BuilderWithExplicitCtor() {}
    public BuilderWithExplicitCtor(String field) { this.field = field; }
}

// FILE: Plain.java
public class Plain {
    String field;
}

// FILE: test.kt
class SubAllArgs : <!INVISIBLE_REFERENCE!>AllArgs<!>()

class SubAllArgsSecondary : AllArgs {
    constructor() : <!INVISIBLE_REFERENCE!>super<!>()
}

class SubAllAndNoArgs : AllAndNoArgs()

fun main() {
    // In fact, all default constructors called below do not exist
    BuilderClass()
    <!INVISIBLE_REFERENCE!>AllArgs<!>()
    <!INVISIBLE_REFERENCE!>RequiredArgs<!>()
    <!INVISIBLE_REFERENCE!>ValueClass<!>()
    <!INVISIBLE_REFERENCE!>DataClass<!>()
    <!INVISIBLE_REFERENCE!>SuperBuilderClass<!>()
    <!INVISIBLE_REFERENCE!>AllArgsStaticName<!>()
    val reference: () -> AllArgs = ::<!INITIALIZER_TYPE_MISMATCH!>AllArgs<!>

    // These default constructors exist
    AllAndNoArgs()
    RequiredArgsWithoutRequiredFields()
    AllArgsWithoutFields()
    DataWithoutRequiredFields()
    BuilderWithExplicitCtor()
    Plain()
    val plainReference: () -> Plain = ::Plain
}
