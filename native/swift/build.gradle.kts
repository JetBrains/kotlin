plugins {
    kotlin("jvm")
}

tasks.register("sirAllTests") {
    dependsOn(
        ":native:swift:sir:test",
        ":native:swift:sir-analysis-api:test",
        ":native:swift:sir-compiler-bridge:test",
        ":native:swift:sir-passes:test",
        ":native:swift:sir-printer:test",
        ":native:swift:swift-export-standalone:test",
    )
}
