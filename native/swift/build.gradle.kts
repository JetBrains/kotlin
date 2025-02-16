plugins {
    kotlin("jvm")
}

tasks.register("sirAllTests") {
    dependsOn(
        ":native:swift:swift-export-standalone:test",
        ":native:swift:swift-export-standalone-integration-tests:simple:test",
        ":native:swift:swift-export-standalone-integration-tests:external:test",
        ":native:swift:swift-export-ide:test",
        ":native:swift:sir-compiler-bridge:test",
        ":native:swift:sir-printer:test",
    )
}