plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/kotlin-formver/maven")
    }
}

dependencies {
    compileOnly(project(":kotlin-formver-compiler-plugin.common"))
    compileOnly(project(":kotlin-formver-compiler-plugin.viper"))
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:plugin-utils"))

    compileOnly("viper:silicon_2.13:1.2-SNAPSHOT")
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
javadocJar()
sourcesJar()
