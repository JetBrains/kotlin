plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":kotlin-scripting-compiler"
    )
)