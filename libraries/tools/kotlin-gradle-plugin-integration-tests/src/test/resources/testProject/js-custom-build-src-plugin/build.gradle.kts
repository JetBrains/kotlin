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
            notCompatibleWithConfigurationCache("Resolving the configuration should happen during the execution phase")
            doLast {
                configurations.named(compilations["main"].npmAggregatedConfigurationName).get().resolve()
            }
        }
    }
}