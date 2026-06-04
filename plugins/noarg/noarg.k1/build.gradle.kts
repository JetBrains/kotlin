description = "Kotlin NoArg Compiler Plugin (K1)"

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:descriptors"))

    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

optInToK1Deprecation()

runtimeJar()
sourcesJar()
javadocJar()
