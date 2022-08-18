// WITH_STDLIB
// FULL_JDK
// FILE: User.java
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Builder
@Data
public class User {
    @Singular private java.util.Map<String, Integer> numbers;
    @Singular private java.util.List<String> statuses;
}

// FILE: Other.java
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Builder(setterPrefix = "with")
@Data
public class Other {
    @Singular("singleSome") private java.util.List<Integer> some;
}

// FILE: test.kt
fun box(): String {
    val userBuilder = User.builder()
        .status("wrong")
        .clearStatuses()
        .status("hello")
        .statuses(listOf("world", "!"))
        .number("1", 1)
        .numbers(mapOf("2" to 2, "3" to 3))

    val user = userBuilder.build()

    val outer = Other.builder()
        .withSingleSome(1)
        .withSome(listOf(2, 3))
        .build()

    val expectedNumbers = mapOf("1" to 1, "2" to 2, "3" to 3)
    val expectedStatuses = listOf("hello", "world", "!")
    val expectedSome = listOf(1, 2, 3)

    return if (user.numbers == expectedNumbers && user.statuses == expectedStatuses && outer.some == expectedSome) {
        "OK"
    } else {
        "Error: $user"
    }
}
