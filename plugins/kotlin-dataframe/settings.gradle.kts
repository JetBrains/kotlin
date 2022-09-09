pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    }

}

rootProject.name = "kotlin-dataframe"

//includeBuild("../../") {
//    dependencySubstitution {
//        substitute(module("org.jetbrains.kotlinx:dataframe")).using(project(":"))
//        substitute(module("org.jetbrains.kotlinx.dataframe:symbol-processor")).using(project(":plugins:symbol-processor"))
//    }
//}
//
//includeBuild("../../bridge-generator") {
//    dependencySubstitution {
//        substitute(module("org.jetbrains.kotlinx.dataframe:bridge-generator")).using(project(":"))
//    }
//}
