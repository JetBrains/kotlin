// ISSUE: KT-83121

// FILE: DataWithStatic.java
import lombok.Data;
@Data(staticConstructor = "of")
public class DataWithStatic {
    private final String name;
}

// FILE: ValueWithStatic.java
import lombok.Value;
@Value(staticConstructor = "of")
public class ValueWithStatic {
    String name;
}

// FILE: RequiredArgsWithStatic.java
import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor(staticName = "of")
public class RequiredArgsWithStatic {
    private final String name;
}

// FILE: AllArgsWithStatic.java
import lombok.AllArgsConstructor;
@AllArgsConstructor(staticName = "of")
public class AllArgsWithStatic {
    String name;
}

// FILE: NoArgsWithStatic.java
import lombok.NoArgsConstructor;
@NoArgsConstructor(staticName = "of")
public class NoArgsWithStatic {
    String name;
}

// FILE: test.kt
fun main() {
    DataWithStatic()
    DataWithStatic.of("name")

    ValueWithStatic()
    ValueWithStatic.of("name")

    RequiredArgsWithStatic()
    RequiredArgsWithStatic.of("name")

    AllArgsWithStatic()
    AllArgsWithStatic.of("name")

    NoArgsWithStatic()
    NoArgsWithStatic.of()
}
