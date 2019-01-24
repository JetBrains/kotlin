import org.jetbrains.kotlin.ideaExt.*
import org.jetbrains.gradle.ext.TopLevelArtifact
import java.util.regex.Pattern

description = "Stdlib configuration for JPS build (to be interpreted during IDEA project import)"

repositories {
    maven(bootstrapKotlinRepo!!.replace("artifacts/content/maven/", "artifacts/content/internal/repo"))
}

val distLib by configurations.creating
val distCommon by configurations.creating
val distRoot by configurations.creating
val builtins by configurations.creating

dependencies {
    distRoot("org.jetbrains.kotlin:kotlin-stdlib-minimal-for-test:$bootstrapKotlinVersion")
    builtins("org.jetbrains.kotlin:builtins:$bootstrapKotlinVersion")

    distLib("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion")
    distLib("org.jetbrains.kotlin:kotlin-stdlib-js:$bootstrapKotlinVersion")
    distLib("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$bootstrapKotlinVersion")
    distLib("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$bootstrapKotlinVersion")

    distLib("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion:sources")
    distLib("org.jetbrains.kotlin:kotlin-stdlib-js:$bootstrapKotlinVersion:sources")
    distLib("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$bootstrapKotlinVersion:sources")
    distLib("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$bootstrapKotlinVersion:sources")

    distCommon("org.jetbrains.kotlin:kotlin-stdlib-common:$bootstrapKotlinVersion")
    distCommon("org.jetbrains.kotlin:kotlin-stdlib-common:$bootstrapKotlinVersion:sources")
}

val distDir: String by rootProject.extra
val distLibDir: File by rootProject.extra

fun TopLevelArtifact.addFromConfiguration(configuration: Configuration) {
    configuration.resolve().forEach {
        file(it)
    }
}

afterEvaluate {
    rootProject.idea {
        project {
            settings {
                ideArtifacts {
                    create("dist_auto_stdlib_reference_dont_use") {
                        addFromConfiguration(distRoot)
                        addFromConfiguration(distCommon)
                        addFromConfiguration(distLib)
                        addFromConfiguration(builtins)
                    }
                }
            }
        }
    }
}

task<Copy>("distRoot") {
    destinationDir = File(distDir)
    dependsOn(distRoot)
    from(distRoot)
    rename("-${Pattern.quote(bootstrapKotlinVersion)}", "")
}

task<Copy>("distLib") {
    destinationDir = distLibDir
    dependsOn(distLib)
    from(distLib)

    rename("-${Pattern.quote(bootstrapKotlinVersion)}", "")
}