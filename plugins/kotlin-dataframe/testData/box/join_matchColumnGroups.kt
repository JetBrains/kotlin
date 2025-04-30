import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val typed = dataFrameOf("name", "age", "city", "weight")(
        "Alice", 15, "London", 54,
        "Bob", 45, "Dubai", 87,
        "Charlie", 20, "Moscow", null,
        "Charlie", 40, "Milan", null,
        "Bob", 30, "Tokyo", 68,
        "Alice", 20, null, 55,
        "Charlie", 30, "Moscow", 90,
    )

    val typed2 = dataFrameOf("name", "origin", "grade", "age")(
        "Alice", "London", 3, "young",
        "Alice", "London", 5, "old",
        "Bob", "Tokyo", 4, "young",
        "Bob", "Paris", 5, "old",
        "Charlie", "Moscow", 1, "young",
        "Charlie", "Moscow", 2, "old",
        "Bob", "Paris", 4, null,
    )

    val add2 = typed.group { name and age }.into("gr").add {
        "gr1" {
            "gr2" {
                "i" from { index() }
            }
        }
    }
    val add3 = typed2.group { origin and age }.into("gr").add {
        "gr1" {
            "gr2" {
                "i" from { index() }
            }
        }
    }

    val joinWithGroups2 = add2.innerJoin(add3) { gr1.match(gr1) }

    joinWithGroups2.assert()

    return "OK"
}
