// WITH_STDLIB
// FULL_JDK
// FILE: User.java
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Builder
@Data
public class User {
    @Singular private java.util.Map numbers;
    @Singular private java.util.List statuses;
}

// FILE: test.kt
fun box(): String {
    val userBuilder = User.builder()
        .status(10)
        .status("hello")
        .statuses(listOf("world", 20))
        .number("1", 1)
        .numbers(mapOf(2 to "2"))

    val user = userBuilder.build()

    val expectedNumbers = mapOf("1" to 1, 2 to "2")
    val expectedStatuses = listOf(10, "hello", "world", 20)

    return if (
        user.numbers == expectedNumbers &&
        user.statuses == expectedStatuses
    ) {
        "OK"
    } else {
        "Error: $user"
    }
}
