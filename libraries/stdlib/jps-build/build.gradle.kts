import org.jetbrains.kotlin.ideaExt.*
import org.jetbrains.gradle.ext.TopLevelArtifact
import java.util.regex.Pattern

description = "Stdlib configuration for JPS build (to be interpreted during IDEA project import)"

val stdlibMinimal by configurations.creating
val stdlibJS by configurations.creating
val stdlibSources by configurations.creating
val compilerLib by configurations.creating

val commonStdlib by configurations.creating
val commonStdlibSources by configurations.creating

val builtins by configurations.creating

dependencies {
    stdlibMinimal("org.jetbrains.kotlin:kotlin-stdlib-minimal-for-test:$bootstrapKotlinVersion")
    stdlibJS("org.jetbrains.kotlin:kotlin-stdlib-js:$bootstrapKotlinVersion") { isTransitive = false }
    stdlibSources("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion:sources") { isTransitive = false }

    builtins("org.jetbrains.kotlin:builtins:$bootstrapKotlinVersion")

    compilerLib("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion")
    compilerLib("org.jetbrains.kotlin:kotlin-stdlib-js:$bootstrapKotlinVersion")
    compilerLib("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$bootstrapKotlinVersion")
    compilerLib("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$bootstrapKotlinVersion")

    compilerLib("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion:sources")
    compilerLib("org.jetbrains.kotlin:kotlin-stdlib-js:$bootstrapKotlinVersion:sources")
    compilerLib("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$bootstrapKotlinVersion:sources")
    compilerLib("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$bootstrapKotlinVersion:sources")

    commonStdlib("org.jetbrains.kotlin:kotlin-stdlib-common:$bootstrapKotlinVersion")
    commonStdlibSources("org.jetbrains.kotlin:kotlin-stdlib-common:$bootstrapKotlinVersion:sources")
}

val distDir: String by rootProject.extra
val distLibDir: File by rootProject.extra

task<Copy>("distRoot") {
    destinationDir = File(distDir)
    dependsOn(stdlibMinimal)
    from(stdlibMinimal)
    rename("-${Pattern.quote(bootstrapKotlinVersion)}", "")
}

task<Copy>("distLib") {
    destinationDir = distLibDir
    dependsOn(compilerLib)
    from(compilerLib)

    rename("-${Pattern.quote(bootstrapKotlinVersion)}", "")
}