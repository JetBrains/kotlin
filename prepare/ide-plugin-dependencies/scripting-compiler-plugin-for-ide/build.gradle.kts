plugins {
    id("root-config")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-scripting-compiler"
    )
)