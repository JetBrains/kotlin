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
        api(project(":native:kotlin-native-utils"))
        api(project(":kotlin-tooling-core"))
        api(project(":libraries:tools:gradle:fus-statistics-gradle-plugin"))

        // plugins
        api(project(":kotlin-gradle-plugin"))
        api(project(":gradle:kotlin-gradle-ecosystem-plugin"))
        api(project(":atomicfu"))
        api(project(":compose-compiler-gradle-plugin"))
        api(project(":kotlin-allopen"))
        api(project(":kotlin-lombok"))
        api(project(":kotlin-noarg"))
        api(project(":kotlin-power-assert"))
        api(project(":kotlin-sam-with-receiver"))
        api(project(":kotlin-serialization"))
        api(project(":kotlin-assignment"))
        api(project(":kotlin-dataframe"))
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
