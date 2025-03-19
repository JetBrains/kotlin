plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-dataframe-compiler-plugin"
    )
)
