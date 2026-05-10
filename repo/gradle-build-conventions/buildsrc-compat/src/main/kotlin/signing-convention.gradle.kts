plugins {
    id("signing")
}

configurations.configureEach {
    if (name == "kotlinBouncyCastleConfiguration") {
        resolutionStrategy.eachDependency {
            checkAndOverrideBouncyCastleVersion(project)
        }
    }
}
