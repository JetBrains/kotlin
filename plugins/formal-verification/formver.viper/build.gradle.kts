plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    mavenLocal()
}

dependencies {
    implementation("viper:silicon:1.1-SNAPSHOT")
    compileOnly(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
javadocJar()
sourcesJar()
