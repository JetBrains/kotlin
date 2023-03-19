plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin/Native utils"

dependencies {
    compileOnly(kotlinStdlib())
    api(project(":kotlin-util-io"))
    api(project(":kotlin-util-klib"))
    api(platform(project(":kotlin-gradle-plugins-bom")))

    testImplementation(commonDependency("junit:junit"))
    testImplementation(kotlinStdlib())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testApiJUnit5()
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

    withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

publish()

standardPublicJars()
