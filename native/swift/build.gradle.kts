plugins {
    kotlin("jvm")
}

tasks.register("sirAllTests") {
    dependsOn(
        ":native:swift:sir-compiler-bridge:test",
        ":native:swift:sir-printer:test"
    )
}

if (kotlinBuildProperties.isSwiftExportPluginPublishingEnabled) {
    /**
     * An umbrella task to publish all artifacts of the Swift Export tool.
     */
    tasks.register("publishAllArtifacts") {
        dependsOn(
            ":native:swift:sir:publish",
            ":native:swift:sir-compiler-bridge:publish",
            ":native:swift:sir-passes:publish",
            ":native:swift:sir-printer:publish",
            ":native:swift:swift-export-standalone:publish",
            ":native:swift:swift-export-embeddable:publish",
        )
    }
}