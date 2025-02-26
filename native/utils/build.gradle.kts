plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin/Native utils"

dependencies {
    compileOnly(kotlinStdlib())
    api(project(":kotlin-util-io"))

    testImplementation(commonDependency("junit:junit"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testImplementation(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            languageVersion = "1.7"
            apiVersion = "1.7"
            freeCompilerArgs += "-Xsuppress-version-warnings"
        }
    }
}

// TODO: this single known external consumer of this artifact is Kotlin/Native backend,
//  so publishing could be stopped after migration to monorepo
publish()

standardPublicJars()
