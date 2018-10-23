plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin/Native deserializer and library reader"

jvmTarget = "1.6"

dependencies {

    // Compile-only dependencies are needed for compilation of this module:
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:cli-common"))

    // This dependency is necessary to keep the right dependency record inside of POM file:
    compile(projectRuntimeJar(":kotlin-compiler"))

    compile(project(":kotlin-native:kotlin-native-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

standardPublicJars()

publish()
