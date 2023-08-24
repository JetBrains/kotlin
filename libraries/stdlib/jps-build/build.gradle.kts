description = "Stdlib configuration for JPS build (to be interpreted during IDEA project import)"

val stdlibMinimal by configurations.creating
val stdlibSources by configurations.creating
val compilerLib by configurations.creating

val commonStdlib by configurations.creating
val commonStdlibSources by configurations.creating

val builtins by configurations.creating

dependencies {
    stdlibMinimal("org.jetbrains.kotlin:kotlin-stdlib-jvm-minimal-for-test:$bootstrapKotlinVersion")
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

    commonStdlib("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion:common")
    commonStdlibSources("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion:common-sources")
}