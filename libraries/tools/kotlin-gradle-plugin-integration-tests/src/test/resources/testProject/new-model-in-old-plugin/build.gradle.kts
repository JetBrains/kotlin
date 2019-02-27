plugins {
    kotlin("jvm").version("<pluginMarkerVersion>")
    `maven-publish`
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

kotlin.target.compilations {
    all {
        kotlinOptions {
            allWarningsAsErrors = true
            jvmTarget = "1.8"
        }
    }

    val main by getting {
        defaultSourceSet.dependencies {
            api(kotlin("gradle-plugin-api"))
            implementation(kotlin("stdlib-jdk8"))
        }
    }

    val test by getting {
        defaultSourceSet.dependencies {
            implementation(kotlin("test-junit"))
        }
    }

    val benchmark by creating {
        defaultSourceSet.dependencies {
            implementation(main.compileDependencyFiles + main.output.allOutputs)
            runtimeOnly(main.runtimeDependencyFiles)

            implementation(kotlin("reflect"))
        }
    }
}

val runBenchmark by tasks.registering(JavaExec::class) {
    classpath = kotlin.target.compilations["benchmark"].run { runtimeDependencyFiles + output.allOutputs }
    main = "com.example.ABenchmarkKt"
}

publishing {
    publications {
        create("default", MavenPublication::class) {
            from(components.getByName("kotlin"))
            artifact(tasks.getByName("kotlinSourcesJar"))
        }
    }
    repositories {
        maven("${buildDir}/repo")
    }
}