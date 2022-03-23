rootProject.name = "reflect-sandbox"
pluginManagement {
    // Uncomment to use a released version
//    resolutionStrategy {
//        this.eachPlugin {
//
//            if (requested.id.id == "org.jetbrains.reflekt") {
//                useModule("org.jetbrains.reflekt:gradle-plugin:${this.requested.version}")
//            }
//        }
//    }

    repositories {
        mavenLocal()
        // add the dependency to Reflekt Maven repository
        // Uncomment to use a released version
        maven(url = uri("https://packages.jetbrains.team/maven/p/reflekt/reflekt"))
        // Necessary only for this example, for Kotless library
        maven(url = uri("https://plugins.gradle.org/m2/"))
    }
}
