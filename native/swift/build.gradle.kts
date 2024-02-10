plugins {
    kotlin("jvm")
}

tasks.register("sirAllTests") {
    dependsOn(
        ":native:swift:sir-compiler-bridge:test",
        ":native:swift:sir-printer:test",
        ":native:swift:swift-export-standalone:test",
    )
}
