import plugins.signLibraryPublication

plugins {
    id("java-platform")
    id("maven-publish")
}

dependencies {
    constraints {
        // kotlin-gradle-plugin-api
        api(project(":kotlin-gradle-plugin-api"))
        api(project(":kotlin-gradle-plugin-annotations"))
        api(project(":kotlin-gradle-plugin-model"))
        api(project(":native:kotlin-native-utils"))
        api(project(":kotlin-tooling-core"))

        // plugins
        api(project(":kotlin-gradle-plugin"))
        api(project(":atomicfu"))
        api(project(":kotlin-allopen"))
        api(project(":kotlin-lombok"))
        api(project(":kotlin-noarg"))
        api(project(":kotlin-sam-with-receiver"))
        api(project(":kotlin-serialization"))
        api(project(":kotlin-assignment"))
    }
}

configureCommonPublicationSettingsForGradle(signLibraryPublication, sbom = false)

publishing {
    publications {
        create<MavenPublication>("myPlatform") {
            from(components["javaPlatform"])
            pom {
                packaging = "pom"
            }
        }
    }
}
