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
    compileOnly(project(":kotlin-formver-compiler-plugin.core"))
    compileOnly(project(":kotlin-formver-compiler-plugin.uniqueness"))
    compileOnly(project(":kotlin-formver-compiler-plugin.common"))
    compileOnly(project(":kotlin-formver-compiler-plugin.viper"))

    compileOnly(project(":compiler:fir:checkers"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(intellijCore())

    compileOnly("viper:silicon_2.13:1.2-SNAPSHOT")
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
javadocJar()
sourcesJar()
