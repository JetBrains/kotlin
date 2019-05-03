plugins {
    kotlin("js").version("<pluginMarkerVersion>")
    id("kotlin-dce-js")
    `maven-publish`
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

kotlin.sourceSets {
    getByName("main") {
        dependencies {
            api("org.jetbrains.kotlinx:kotlinx-html-js:0.6.10")
            implementation(kotlin("stdlib-js"))
        }
    }
    getByName("test") {
        dependencies {
            implementation(kotlin("test-js"))
        }
    }
}

kotlin.target.compilations.create("benchmark") {
    defaultSourceSet.dependencies {
        val main by kotlin.target.compilations
        implementation(main.compileDependencyFiles + main.output.classesDirs)
        runtimeOnly(main.runtimeDependencyFiles)
    }
}

publishing {
    publications {
        create("default", MavenPublication::class.java) {
            from(components.getByName("kotlin"))
            artifact(tasks.getByName("kotlinSourcesJar"))
        }
    }
    repositories {
        maven("$buildDir/repo")
    }
}