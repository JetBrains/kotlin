import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    compareSchemas(
        df.select { name and age },
        df.select { allBefore { city } },
        df.select { allBefore(city) },
    )

    compareSchemas(
        df.select { name and age },
        df.select { allUpTo { age } },
        df.select { allUpTo(age) },
    )

    compareSchemas(
        df.select { weight and isHappy },
        df.select { allAfter { city } },
        df.select { allAfter(city) },
    )

    compareSchemas(
        df.select { weight and isHappy },
        df.select { allFrom { weight } },
        df.select { allFrom(weight) },
    )

    compareSchemas(
        df.select { name and age and city and weight and isHappy },
        df.select { all() }
    )

    compareSchemas(
        df.select { name.firstName and name.lastName },
        df.select { name.allCols() },
        df.select { name.allCols().all() },
    )

    compareSchemas(
        df.select { weight and isHappy },
        df.select { all().allAfter(city) },
//        df.select { all().allAfter { city } },
    )

    compareSchemas(
        df.select { age },
        df.select { colsOf<Int?>().allBefore(weight) },
        df.select { allBefore(weight).colsOf<Int?>() },
    )


    return "OK"
}
