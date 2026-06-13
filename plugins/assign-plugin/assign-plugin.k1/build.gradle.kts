description = "Kotlin Assignment Compiler Plugin (K1)"

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":kotlin-assignment-compiler-plugin.common"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))

    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

optInToK1Deprecation()

runtimeJar()
sourcesJar()
javadocJar()
