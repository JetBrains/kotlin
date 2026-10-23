// WITH_STDLIB
// FULL_JDK

import lombok.Builder
import lombok.Singular

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class User(
    @Singular val names: List<String>,
    @Singular val pairs: Map<Int, Int>,
)

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class UserWithNull(
    @Singular(ignoreNullCollections = true) val names: List<String>,
    @Singular(ignoreNullCollections = true) val pairs: Map<Int, Int>,
)

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class UserWithNullableCollections(
    @Singular val names: List<String>?,
    @Singular val pairs: Map<Int, Int>?,
)

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class GenericUser<T>(
    @Singular val items: List<T>,
)

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class GenericUserWithNullableElements<T>(
    @Singular val items: List<T?>,
)

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class BoundedUser<T : CharSequence>(
    @Singular val items: List<T>,
)

fun test_default() {
    User.<!UNRESOLVED_REFERENCE!>builder<!>()
        .name("User")
        .name(null)
        .names(listOf("other"))
        .names(null)
        .pair(1, 1)
        .pairs(mapOf(1 to 1))
        .pairs(null)
}

fun test_ignoreNullCollections() {
    UserWithNull.<!UNRESOLVED_REFERENCE!>builder<!>()
        .name("User")
        .name(null)
        .names(listOf("other"))
        .names(null)
        .pair(1, 1)
        .pairs(mapOf(1 to 1))
        .pairs(null)
}

// AddAll (`names`) setter rejects a `null` argument disregarding it's nullable,
// the single-element adder also rejects `null` (the element type stays non-null).
fun test_nullableCollections() {
    UserWithNullableCollections.<!UNRESOLVED_REFERENCE!>builder<!>()
        .name("User")
        .name(null)
        .names(listOf("other"))
        .names(null)
        .pair(1, 1)
        .pairs(mapOf(1 to 1))
        .pairs(null)
}

// The element type is a non-null-by-default type parameter — same rejection as a concrete non-null element type.
fun test_genericDefault() {
    GenericUser.<!UNRESOLVED_REFERENCE!>builder<!><String>()
        .item("a")
        .item(null)
        .items(listOf("b"))
        .items(null)
}

// The element type is a nullable type parameter (`T?`) — `null` elements are accepted, but the
// collection argument itself is still non-null.
fun test_genericNullableElements() {
    GenericUserWithNullableElements.<!UNRESOLVED_REFERENCE!>builder<!><String>()
        .item("a")
        .item(null)
        .items(listOf("b", null))
        .items(null)
}

// A bounded type parameter (`T : CharSequence`) is substituted into the builder scope but stays non-null by default.
fun test_bounded() {
    BoundedUser.<!UNRESOLVED_REFERENCE!>builder<!><String>()
        .item("a")
        .item(null)
}
