// WITH_STDLIB
// FULL_JDK

import lombok.Builder
import lombok.Singular

@Builder
class User(
    @Singular val names: List<String>,
    @Singular val pairs: Map<Int, Int>,
)

@Builder
class UserWithNull(
    @Singular(ignoreNullCollections = true) val names: List<String>,
    @Singular(ignoreNullCollections = true) val pairs: Map<Int, Int>,
)

@Builder
class UserWithNullableCollections(
    @Singular val names: List<String>?,
    @Singular val pairs: Map<Int, Int>?,
)

fun test_default() {
    User.builder()
        .name("User")
        .name(<!NULL_FOR_NONNULL_TYPE!>null<!>)
        .names(listOf("other"))
        .names(<!NULL_FOR_NONNULL_TYPE!>null<!>)
        .pair(1, 1)
        .pairs(mapOf(1 to 1))
        .pairs(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}

fun test_ignoreNullCollections() {
    UserWithNull.builder()
        .name("User")
        .name(<!NULL_FOR_NONNULL_TYPE!>null<!>)
        .names(listOf("other"))
        .names(null)
        .pair(1, 1)
        .pairs(mapOf(1 to 1))
        .pairs(null)
}

// AddAll (`names`) setter rejects a `null` argument disregarding it's nullable,
// the single-element adder also rejects `null` (the element type stays non-null).
fun test_nullableCollections() {
    UserWithNullableCollections.builder()
        .name("User")
        .name(<!NULL_FOR_NONNULL_TYPE!>null<!>)
        .names(listOf("other"))
        .names(<!NULL_FOR_NONNULL_TYPE!>null<!>)
        .pair(1, 1)
        .pairs(mapOf(1 to 1))
        .pairs(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}
