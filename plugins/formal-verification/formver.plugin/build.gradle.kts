plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":kotlin-formver-compiler-plugin.core"))
    compileOnly(project(":kotlin-formver-compiler-plugin.common"))
    compileOnly(project(":kotlin-formver-compiler-plugin.viper"))

    compileOnly(project(":compiler:fir:checkers"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
javadocJar()
sourcesJar()
