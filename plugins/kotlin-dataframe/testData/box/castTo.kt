import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val sample =
        @Import DataFrame.readCSV("https://raw.githubusercontent.com/Kotlin/dataframe/master/data/jetbrains_repositories.csv")

    val organizations = listOf("https://raw.githubusercontent.com/Kotlin/dataframe/master/data/jetbrains_repositories.csv")
    organizations.forEach { organization ->
        val df = DataFrame.readCSV(organization).castTo(sample)
        println(organizations)
        println("Repositories: ${df.count()}")
        println("Top 10:")
        df.sortBy { stargazers_count.desc() }.take(10).print()
    }
    return "OK"
}
