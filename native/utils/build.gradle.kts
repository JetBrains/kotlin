plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin/Native utils"

dependencies {
    compileOnly(kotlinStdlib())
    api(project(":kotlin-util-io"))
    api(platform(project(":kotlin-gradle-plugins-bom")))

    testImplementation(commonDependency("junit:junit"))
    testImplementation(kotlinStdlib())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            languageVersion = "1.4"
            apiVersion = "1.4"
            freeCompilerArgs += "-Xsuppress-version-warnings"
        }
    }
}

// TODO: this single known external consumer of this artifact is Kotlin/Native backend,
//  so publishing could be stopped after migration to monorepo
publish()

standardPublicJars()
