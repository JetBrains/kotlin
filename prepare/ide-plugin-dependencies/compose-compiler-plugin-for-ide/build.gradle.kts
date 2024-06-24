plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":plugins:compose-compiler-plugin:compiler-hosted",
    )
)
