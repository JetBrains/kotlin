description = "Kotlin Library (KLIB) metadata manipulation library"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

optInToK1Deprecation()

dependencies {
    api(kotlinStdlib())
    api(project(":kotlin-metadata"))
    implementation(project(":core:compiler.common"))
    implementation(project(":core:names"))
    implementation(project(":core:deserialization.common"))
    implementation(project(":compiler:serialization.common"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":kotlin-util-io"))
    implementation(protobufLite())
    testImplementation(kotlinTest("junit5"))
    testImplementation(project(":compiler:util"))
}

tasks.test {
    useJUnitPlatform()
}

sourcesJar()
javadocJar()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xallow-kotlin-package")
    }
}
