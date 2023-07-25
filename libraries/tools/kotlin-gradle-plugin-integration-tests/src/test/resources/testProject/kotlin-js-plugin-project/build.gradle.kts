plugins {
    kotlin("js")
    `maven-publish`
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven { setUrl("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
}

kotlin.sourceSets {
    getByName("main") {
        dependencies {
            api("org.jetbrains.kotlinx:kotlinx-html-js:0.7.5")
            implementation(kotlin("stdlib-js"))
        }
    }
    getByName("test") {
        dependencies {
            implementation(kotlin("test-js"))
        }
    }
}

kotlin.target {
    nodejs()
    browser {
        testTask {
            useKarma {
                useChromeHeadless()
            }
            enabled = false // Task is disabled because it requires browser to be installed. That may be a problem on CI.
            // Disabled but configured task allows us to check at least a part of configuration cache correctness.
        }
    }
}

kotlin.target.compilations.create("benchmark") {
    defaultSourceSet.dependencies {
        val main by kotlin.target.compilations
        implementation(main.compileDependencyFiles + main.output.classesDirs)
        runtimeOnly(files(main.runtimeDependencyFiles))
    }
}

publishing {
    publications {
        create("default", MavenPublication::class.java) {
            from(components.getByName("kotlin"))
        }
    }
    repositories {
        maven("$buildDir/repo")
    }
}
