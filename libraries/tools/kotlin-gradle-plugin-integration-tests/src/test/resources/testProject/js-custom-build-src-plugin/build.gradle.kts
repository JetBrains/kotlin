plugins {
    id("my-plugin")
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(npm("async", "3.2.4"))
}

kotlin {
    js {
        tasks.register("checkConfigurationsResolve") {
            val npmAggregatedFiles: FileCollection = configurations.named(kotlin.js().compilations["main"].npmAggregatedConfigurationName).get()
            inputs.files(npmAggregatedFiles).withPropertyName("npmAggregatedFiles")
            doLast {
                npmAggregatedFiles.files
            }
        }
    }
}