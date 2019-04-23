import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet


buildscript {
    repositories {
        maven("https://dl.bintray.com/orangy/maven")
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-allopen:1.3.30")
        classpath("org.jetbrains.gradle.benchmarks:benchmarks.plugin:0.1.7-dev-20")
        classpath("kotlinx.team:kotlinx.team.infra:0.1.0-dev-44")
    }
}

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("me.champeau.gradle.jmh").version("0.4.8")
    id("org.jetbrains.kotlin.plugin.allopen").version("1.3.30")
}

//apply(plugin = "org.jetbrains.gradle.benchmarks.plugin")


allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

dependencies {
    runtime(intellijDep())

    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:util"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compile(project(":compiler:backend.js"))
    compile(project(":compiler:ir.serialization.js"))
    compile(project(":kotlin-build-common"))
    runtime(project(":compiler:backend-common"))

    implementation("org.openjdk.jmh:jmh-core:+")
    implementation("org.openjdk.jmh:jmh-generator-annprocess:+")
}

val moveKToJmh by task<Copy> {
    from("$buildDir/classes/kotlin/main")
    into("$buildDir/classes/kotlin/jmh")
}

jmh {
    duplicateClassesStrategy = DuplicatesStrategy.WARN
}

sourceSets {
    "main" { projectDefault() }
//    "main" { }
//    "jmh" { projectDefault() }
    "test" { }
}

//
//sourceSets["jmh"].java.srcDir("src/jmh")
//sourceSets["jmh"].withConvention(KotlinSourceSet::class) {
//    kotlin.srcDir("src/jmh")
//}

//benchmark {
//    defaults {
//        iterations = 5 // number of iterations
//        iterationTime = 300 // time in ms per iteration
//    }
//
//    // Setup configurations
//    configurations {
//        // This one matches compilation base name, e.g. 'jvm', 'jvmTest', etc
//        register("jvm") {
//            jmhVersion = "1.21"
//        }
//    }
//}

val foo by task<Task> {
    dependsOn(":compiler:ir.serialization.js:fullRuntimeSources")
}
