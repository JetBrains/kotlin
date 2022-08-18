// FIR_IDENTICAL
// WITH_STDLIB
// FULL_JDK
// FILE: User.java
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Builder
@Data
public class User {
    @Singular private java.util.List<String> names;
}

// FILE: UserWithoutNull.java
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Builder
@Data
public class UserWithoutNull {
    @Singular(ignoreNullCollections = false) private java.util.List<String> names;
}

// FILE: UserWithNull.java
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Builder
@Data
public class UserWithNull {
    @Singular(ignoreNullCollections = true) private java.util.List<String> names;
}

// FILE: test.kt
fun test() {
    User.builder()
        .name("User")
        .name(<!NULL_FOR_NONNULL_TYPE!>null<!>) // error

    UserWithoutNull.builder()
        .name("User")
        .name(<!NULL_FOR_NONNULL_TYPE!>null<!>) // error

    UserWithNull.builder()
        .name("User")
        .name(null) // ok
}
