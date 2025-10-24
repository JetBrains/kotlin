import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import java.math.BigInteger

fun box(): String {
    // multiple columns
    val personsDfNullable = dataFrameOf(
        "name", "age", "city", "weight", "height", "yearsToRetirement", "workExperienceYears", "dependentsCount", "annualIncome", "bigNumber",
    )(
        "Alice", 15, "London", 99.5, "1.85", 50, 0.toShort(), 0.toByte(), 0L, BigInteger.valueOf(12),
        null, null, null, null, null, null, null, null, null, null,
        "Bob", 20, "Paris", 140.0, "1.35", 45, 2.toShort(), 0.toByte(), 12000L, BigInteger.valueOf(64564),
        "Charlie", 100, "Dubai", 75.0, "1.95", 0, 70.toShort(), 0.toByte(), 0L, BigInteger.valueOf(-2134),
        "Rose", 1, "Moscow", 45.33, "0.79", 64, 0.toShort(), 2.toByte(), 0L, BigInteger.valueOf(3),
        "Dylan", 35, "London", 23.4, "1.83", 30, 15.toShort(), 1.toByte(), 90000L, BigInteger.valueOf(547567),
        "Eve", 40, "Paris", 56.72, "1.85", 25, 18.toShort(), 3.toByte(), 125000L, BigInteger.valueOf(32432),
        "Frank", 55, "Dubai", 78.9, "1.35", 10, 35.toShort(), 2.toByte(), 145000L, BigInteger.valueOf(2),
        "Grace", 29, "Moscow", 67.8, "1.65", 36, 5.toShort(), 1.toByte(), 70000L, BigInteger.valueOf(-234324),
        "Hank", 60, "Paris", 80.22, "1.75", 5, 40.toShort(), 4.toByte(), 200000L, BigInteger.valueOf(-546),
        "Isla", 22, "London", 75.1, "1.85", 43, 1.toShort(), 0.toByte(), 30000L, BigInteger.valueOf(2),
    ).group { name and age }.into("data")

    val personsDf = personsDfNullable.dropNulls { colsAtAnyDepth() }

    // scenario #0: all numerical columns
    personsDfNullable.cumSum().let { df ->
        df.compareSchemas(strict = true)

        val cumSum01n: DataColumn<Int?> = df.data.age
        val cumSum02n: DataColumn<Double> = df.weight
        val cumSum03n: DataColumn<Int?> = df.yearsToRetirement
        val cumSum04n: DataColumn<Int?> = df.workExperienceYears
        val cumSum05n: DataColumn<Int?> = df.dependentsCount
        val cumSum06n: DataColumn<Long?> = df.annualIncome
        val cumSum07n: DataColumn<String?> = df.data.name
        val cumSum08n: DataColumn<String?> = df.city
        val cumSum09n: DataColumn<String?> = df.height
        val cumSum10n: DataColumn<BigInteger?> = df.bigNumber
    }
    personsDf.cumSum().let { df ->
        df.compareSchemas(strict = true)

        val cumSum01: DataColumn<Int> = df.data.age
        val cumSum02: DataColumn<Double> = df.weight
        val cumSum03: DataColumn<Int> = df.yearsToRetirement
        val cumSum04: DataColumn<Int> = df.workExperienceYears
        val cumSum05: DataColumn<Int> = df.dependentsCount
        val cumSum06: DataColumn<Long> = df.annualIncome
        val cumSum07: DataColumn<String> = df.data.name
        val cumSum08: DataColumn<String> = df.city
        val cumSum09: DataColumn<String> = df.height
        val cumSum10: DataColumn<BigInteger> = df.bigNumber
    }

    // scenario #1: particular column
    personsDfNullable.cumSum { data.age }.let { df ->
        df.compareSchemas(strict = true)

        val cumSum11n: DataColumn<Int?> = df.data.age
        // other columns should be unaffected
        val cumSum111n: DataColumn<String?> = df.city
    }

    personsDf.cumSum { data.age }.let { df ->
        df.compareSchemas(strict = true)

        val cumSum11: DataColumn<Int> = df.data.age
    }
    // scenario #1.1: particular column with converted type
    personsDfNullable.cumSum { dependentsCount }.let { df ->
        df.compareSchemas(strict = true)

        val cumSum111n: DataColumn<Int?> = df.dependentsCount
    }
    personsDf.cumSum { dependentsCount }.let { df ->
        df.compareSchemas(strict = true)

        val cumSum111: DataColumn<Int> = df.dependentsCount
    }
    // scenario #1.2: particular column with null -> NaN
    personsDfNullable.cumSum { weight }.let { df ->
        df.compareSchemas(strict = true)

        val cumSum121n: DataColumn<Double> = df.weight
    }

    personsDf.cumSum { weight }.let { df ->
        df.compareSchemas(strict = true)

        val cumSum121: DataColumn<Double> = df.weight
    }
    // scenario #2: cumSum of values per columns separately
    personsDfNullable.cumSum { weight and workExperienceYears and dependentsCount and annualIncome }.let { df ->
        df.compareSchemas(strict = true)

        val cumSum32n: DataColumn<Double> = df.weight
        val cumSum33n: DataColumn<Int?> = df.workExperienceYears
        val cumSum34n: DataColumn<Int?> = df.dependentsCount
        val cumSum35n: DataColumn<Long?> = df.annualIncome
    }
    personsDf.cumSum { weight and workExperienceYears and dependentsCount and annualIncome }.let { df ->
        df.compareSchemas(strict = true)

        val cumSum32: DataColumn<Double> = df.weight
        val cumSum33: DataColumn<Int> = df.workExperienceYears
        val cumSum34: DataColumn<Int> = df.dependentsCount
        val cumSum35: DataColumn<Long> = df.annualIncome
    }
    return "OK"
}
