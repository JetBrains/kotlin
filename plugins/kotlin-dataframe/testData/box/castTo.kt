import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import java.net.URL

fun box(): String {
    val sample = dataFrameOf("full_name", "html_url", "stargazers_count", "topics", "watchers")(
        "JetBrains/JPS", URL("https://github.com/JetBrains/JPS"), 23, "[]", 23
    )

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
