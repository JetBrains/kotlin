plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":native:swift:sir",
        ":native:swift:sir-light-classes",
        ":native:swift:sir-printer",
        ":native:swift:sir-providers",
        ":native:analysis-api-klib-reader",
    )
)
