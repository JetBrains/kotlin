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
    @Singular private java.util.Map<Integer, Integer> pairs;
}

// FILE: UserWithoutNull.java
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Builder
@Data
public class UserWithoutNull {
    @Singular(ignoreNullCollections = false) private java.util.List<String> names;
    @Singular(ignoreNullCollections = false) private java.util.Map<Integer, Integer> pairs;
}

// FILE: UserWithNull.java
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Builder
@Data
public class UserWithNull {
    @Singular(ignoreNullCollections = true) private java.util.List<String> names;
    @Singular(ignoreNullCollections = true) private java.util.Map<Integer, Integer> pairs;
}

// FILE: test.kt
fun test_1() {
    User.builder()
        .name("User")
        .name(null)
        .names(listOf("other"))
        .names(listOf(null))
        .names(<!NULL_FOR_NONNULL_TYPE!>null<!>)
        .pair(null, 1)
        .pair(1, null)
        .pairs(mapOf(1 to 1))
        .pairs(mapOf(null to 1))
        .pairs(mapOf(1 to null))
        .pairs(mapOf(null to null))
        .pairs(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}

fun test_2() {
    UserWithoutNull.builder()
        .name("User")
        .name(null)
        .names(listOf("other"))
        .names(listOf(null))
        .names(<!NULL_FOR_NONNULL_TYPE!>null<!>)
        .pair(null, 1)
        .pair(1, null)
        .pairs(mapOf(1 to 1))
        .pairs(mapOf(null to 1))
        .pairs(mapOf(1 to null))
        .pairs(mapOf(null to null))
        .pairs(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}

fun test_3() {
    UserWithNull.builder()
        .name("User")
        .name(null)
        .names(listOf("other"))
        .names(listOf(null))
        .names(null)
        .pair(null, 1)
        .pair(1, null)
        .pairs(mapOf(1 to 1))
        .pairs(mapOf(null to 1))
        .pairs(mapOf(1 to null))
        .pairs(mapOf(null to null))
        .pairs(null)
}
