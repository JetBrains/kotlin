import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

publish()

description = "Kotlin pre 1.3 experimental coroutines compatibility library"

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String


sourceSets {
    "main" {
        java.srcDirs("src", "jvm/src")
    }
    "test" {
        java.srcDirs("jvm/test")
    }
    "migrationTest" {
        if(!kotlinBuildProperties.isInIdeaSync)
        java.srcDirs("jvm/test")
    }
}
val migrationTestSourceSet = sourceSets["migrationTest"]

configurations {
    "migrationTestImplementation" {
        extendsFrom(testImplementation.get())
    }
}

dependencies {
    api(kotlinStdlib())
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    "migrationTestImplementation"(sourceSets.main.get().output)
}

tasks {
    val compileKotlin by existing(KotlinCompile::class) {
        kotlinOptions {
            languageVersion = "1.3"
            apiVersion = "1.3"
            freeCompilerArgs = listOf(
                "-Xmulti-platform",
                "-Xallow-kotlin-package",
                "-Xmultifile-parts-inherit",
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xopt-in=kotlin.contracts.ExperimentalContracts",
                "-Xcoroutines=enable",
                "-XXLanguage:-ReleaseCoroutines",
                "-Xno-use-ir",
                "-module-name", "kotlin-coroutines-experimental-compat"
            )
        }
    }
    val compileTestKotlin by existing(KotlinCompile::class) {
        kotlinOptions {
            languageVersion = "1.2"
            apiVersion = "1.2"
            freeCompilerArgs = listOf("-Xcoroutines=enable")
        }
    }
    val compileMigrationTestKotlin by existing(KotlinCompile::class) {
        kotlinOptions {
            languageVersion = "1.3"
            apiVersion = "1.3"
            freeCompilerArgs = listOf()
        }
    }

    val migrationTestClasses by existing
    val migrationTest by creating(Test::class) {
        dependsOn(migrationTestClasses)
        group = "verification"
        testClassesDirs = migrationTestSourceSet.output.classesDirs
        classpath = migrationTestSourceSet.runtimeClasspath
    }

    val check by existing {
        dependsOn(migrationTest)
    }


    val jar by existing(Jar::class) {
        callGroovy("manifestAttributes", manifest, project, "Main")
    }
}

sourcesJar()
javadocJar()
