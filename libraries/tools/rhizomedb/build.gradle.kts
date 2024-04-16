plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))

    commonCompileOnly(project(":kotlin-gradle-plugin"))
    commonCompileOnly(project(":kotlin-compiler-embeddable"))
}

gradlePlugin {
    plugins {
        create("rhizomedb") {
            id = "org.jetbrains.kotlin.plugin.rhizomedb"
            displayName = "Kotlin compiler plugin for Fleet rhizomedb library"
            description = displayName
            implementationClass = "com.jetbrains.rhizomedb.gradle.RhizomedbGradleSubplugin"
        }
    }
}
