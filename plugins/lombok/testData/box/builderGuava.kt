// WITH_GUAVA
// WITH_STDLIB
// FULL_JDK
// FILE: User.java
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Builder
@Data
public class User {
    @Singular private com.google.common.collect.ImmutableMap<String, Integer> numbers;
    @Singular private com.google.common.collect.ImmutableList<String> statuses;
    @Singular private com.google.common.collect.ImmutableTable<String, String, String> values;
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
import com.google.common.collect.ImmutableTable
import com.google.common.collect.HashBasedTable

fun box(): String {
    val userBuilder = User.builder()
        .status("wrong")
        .clearStatuses()
        .status("hello")
        .statuses(listOf("world", "!"))
        .number("1", 1)
        .numbers(mapOf("2" to 2, "3" to 3))
        .value("1", "a", "hello")
        .values(ImmutableTable.of("2", "b", "world"))

    val user = userBuilder.build()

    val outer = Other.builder()
        .withSingleSome(1)
        .withSome(listOf(2, 3))
        .build()

    val expectedNumbers = mapOf("1" to 1, "2" to 2, "3" to 3)
    val expectedStatuses = listOf("hello", "world", "!")
    val expectedSome = listOf(1, 2, 3)
    val expectedValues = HashBasedTable.create<String, String, String>().apply {
        put("1", "a", "hello")
        put("2", "b", "world")
    }

    return if (
        user.numbers == expectedNumbers &&
        user.statuses == expectedStatuses &&
        user.values == expectedValues &&
        outer.some == expectedSome
    ) {
        "OK"
    } else {
        "Error: $user"
    }
}
