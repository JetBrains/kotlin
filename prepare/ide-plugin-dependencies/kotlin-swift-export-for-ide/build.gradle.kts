plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":native:swift:sir",
        ":native:swift:sir-light-classes",
        ":native:swift:sir-printer",
        ":native:swift:sir-providers",
        ":native:swift:swift-export-ide",
    )
)
