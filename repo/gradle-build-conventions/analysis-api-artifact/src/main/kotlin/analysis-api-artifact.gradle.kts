standardPublicJars()

analysisApiPublishingLatch {
    val propertyName = "analysis.api.version"
    version = when (val rawVersion = rootProject.findProperty(propertyName)?.toString()) {
        null -> error("Analysis API version isn't configured: pass the '$propertyName' property")
        "kotlin.version" -> rootProject.extra["kotlinVersion"] as String
        else -> rawVersion
    }

    sourceSets {
        "main" { none() }
        "test" { none() }
    }

    publish()
}
