plugins {
    id("gradle-plugin-dependency-configuration")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

dependencies {
    commonImplementation(project(":kotlin-gradle-plugin"))

//    commonCompileOnly(project(":kotlin-gradle-compiler-types"))
//    embedded(project(":kotlin-gradle-compiler-types")) { isTransitive = false }
}

apiValidation {
    nonPublicMarkers += "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi"
}

//TODO
//tasks {
//    apiBuild {
//        inputJar.value(jar.flatMap { it.archiveFile })
//    }
//}
