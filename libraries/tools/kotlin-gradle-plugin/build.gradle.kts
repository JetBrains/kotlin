import GenerateKgpBuildConstantsTask.Companion.registerGenerateKgpBuildConstantsTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import gradle.GradlePluginVariant
import org.gradle.api.file.RegularFile
import org.gradle.internal.os.OperatingSystem
import org.gradle.plugin.compatibility.compatibility
import org.jetbrains.kotlin.build.androidsdkprovisioner.ProvisioningType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi
import org.jetbrains.kotlin.testFederation.isSmokeTest

plugins {
    id("gradle-plugin-common-configuration")
    id("kotlin-git.gradle-build-conventions.binary-compatibility-extended")
    id("kotlin-git.gradle-build-conventions.kgp-npm-tooling-helper")
    id("android-sdk-provisioner")
    id("asm-deprecating-transformer")
    id("project-tests-convention")
    id("test-inputs-check")
    `java-test-fixtures`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    compilerOptions {
        optIn.addAll(
            listOf(
                "kotlin.RequiresOptIn",
                "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi",
                "org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi",
                "org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi",
                "org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi",
                "org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi",
                "org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl",
                "org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation",
            )
        )
    }
}

registerKotlinSourceForVersionRange(
    GradlePluginVariant.GRADLE_MIN,
    GradlePluginVariant.GRADLE_82,
)

registerKotlinSourceForVersionRange(
    GradlePluginVariant.GRADLE_MIN,
    GradlePluginVariant.GRADLE_86,
)

registerKotlinSourceForVersionRange(
    GradlePluginVariant.GRADLE_MIN,
    GradlePluginVariant.GRADLE_811,
)

tasks.test {
    useJUnitPlatform {
        exclude("**/*LincheckTest.class")
    }
    extensions.configure<TestInputsCheckExtension>("testInputsCheck") {
        // The regular test task still uses rootDir as its working directory, which is explicitly
        // marked non-cacheable by project-tests-convention. Keep the input check focused on the
        // cacheable KGP test tasks migrated in this change.
        enabled.set(false)
    }
    val jdk8Provider = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_1_8)
    val jdk11Provider = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_11_0)
    doFirst {
        systemProperty("jdk8Home", jdk8Provider.get())
        systemProperty("jdk11Home", jdk11Provider.get())
    }
}

val muteCommonFile: RegularFile = rootProject.layout.projectDirectory.file("tests/mute-common.csv")

tasks.withType<Test>().configureEach {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_21_0))
}

tasks.register<Test>("lincheckTest") {
    classpath = sourceSets.test.get().runtimeClasspath
    testClassesDirs = sourceSets.test.get().output.classesDirs
    jvmArgs(
        "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-exports", "java.base/jdk.internal.util=ALL-UNNAMED",
        "--add-exports", "java.base/sun.security.action=ALL-UNNAMED"
    )
    addFileProperty(muteCommonFile, "org.jetbrains.kotlin.test.mutes.file")
    useJUnitPlatform {
        include("**/*LincheckTest.class")
    }
}

binaryCompatibilityValidator {
    targets.configureEach {
        ignoredPackages.addAll(
            "org.jetbrains.kotlin.gradle.internal",
            "org.jetbrains.kotlin.gradle.plugin.internal",
            "org.jetbrains.kotlin.gradle.scripting.internal",
            "org.jetbrains.kotlin.gradle.targets.js.internal",
            "org.jetbrains.kotlin.gradle.targets.native.internal",
            "org.jetbrains.kotlin.gradle.tasks.internal",
            "org.jetbrains.kotlin.gradle.testing.internal",
        )
        ignoredMarkers.add("org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi")

        inputClasses.from(project.sourceSets.main.map { it.output.classesDirs })
        inputClasses.from(project.sourceSets.common.map { it.output.classesDirs })
    }

    val externalApiMarkers = setOf(
        "org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi",
        "org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi",
        "org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginPublicDsl",
    )

    targets.register("all") {
        // Dump of all public API, intended for regular usage in build scripts.
        ignoredMarkers.addAll(externalApiMarkers)
    }

    targets.register("external") {
        // Dump of all external API, intended for use in official JetBrains plugins like Compose.
        publicMarkers.addAll(externalApiMarkers)
    }
}

val unpublishedCompilerRuntimeDependencies = listOf(
    // TODO: remove in KT-70247
    ":compiler:cli", // for MessageRenderer, related to MessageCollector usage
    ":compiler:cli-base", // for compiler arguments setup, for logging via MessageCollector, CompilerSystemProperties, ExitCode
    ":compiler:arguments.common", // for compiler arguments parser setup (using `@Enables`, `@Disables` and other annotations)
    ":compiler:compiler.version", // for user projects buildscripts, `loadCompilerVersion`
    ":compiler:config", // for CommonCompilerArguments initialization
    ":compiler:config.jvm", // for K2JVMCompilerArguments initialization
    ":compiler:ir.serialization.common", // for PartialLinkageMode (K/N)
    ":compiler:util", // for CommonCompilerArguments initialization, K/N
    ":core:compiler.common", // for FUS statistics parsing all the compiler arguments
    ":core:compiler.common.jvm", // for FUS statistics parsing all the compiler arguments
    ":core:descriptors", // for `fromUIntToLong`
    ":core:util.runtime", // for stdlib extensions
    ":core:language.model", ":core:language.targets", ":core:language.targets.jvm", // For JvmTarget
    ":core:language.version-settings", // For LanguageFeature
    ":core:names", // For ClassId
    ":kotlin-build-common", // for incremental compilation setup
    ":js:js.config", // for k/js task
    ":wasm:wasm.config", // for k/js task
)

val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-api"))
    commonApi(project(":libraries:tools:gradle:fus-statistics-gradle-plugin"))

    for (compilerRuntimeDependency in unpublishedCompilerRuntimeDependencies) {
        commonCompileOnly(project(compilerRuntimeDependency)) { isTransitive = false }
    }
    commonCompileOnly(libs.guava)
    commonCompileOnly(project(":daemon-common")) {
        isTransitive = false
    }
    commonCompileOnly(project(":kotlin-daemon-client")) {
        isTransitive = false
    }
    commonCompileOnly(project(":kotlin-gradle-compiler-types"))
    commonCompileOnly(project(":kotlin-compiler-runner-unshaded")) {
        isTransitive = false
    }
    commonCompileOnly(project(":kotlin-gradle-statistics"))
    commonCompileOnly(project(":kotlin-gradle-build-metrics"))
    commonCompileOnly(project(":compiler:build-tools:kotlin-build-tools-jdk-utils"))
    commonCompileOnly(libs.android.gradle.plugin.gradle.api) {
        overrideTargetJvmVersion(11)
        isTransitive = false
    }
    commonCompileOnly(libs.android.gradle.plugin.gradle) {
        overrideTargetJvmVersion(11)
        isTransitive = false
    }
    commonCompileOnly(libs.android.gradle.plugin.builder) {
        overrideTargetJvmVersion(11)
        isTransitive = false
    }
    commonCompileOnly(libs.android.gradle.plugin.builder.model) {
        overrideTargetJvmVersion(11)
        isTransitive = false
    }
    commonCompileOnly(libs.android.tools.common) {
        overrideTargetJvmVersion(11)
        isTransitive = false
    }
    commonCompileOnly(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    commonCompileOnly(libs.develocity.gradlePlugin)
    commonCompileOnly(commonDependency("com.google.code.gson:gson"))
    commonCompileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json") {
        version {
            strictly(GradlePluginVariant.GRADLE_MIN.compatibleKotlinxJsonSerializationVersion)
        }
    }
    commonCompileOnly("com.github.gundy:semver4j:0.16.4:nodeps") {
        exclude(group = "*")
    }
    commonCompileOnly(project(":kotlin-tooling-metadata"))
    commonCompileOnly(project(":compiler:build-tools:kotlin-build-statistics"))
    commonCompileOnly(project(":native:swift:swift-export-standalone"))
    commonCompileOnly(libs.intellij.asm) { isTransitive = false }

    commonCompileOnly(libs.develocity.gradlePluginAdapter)

    commonImplementation(project(":kotlin-gradle-plugin-idea"))
    commonImplementation(project(":kotlin-gradle-plugin-idea-proto"))
    commonImplementation(project(":native:kotlin-klib-commonizer-api")) // TODO: consider removing in KT-70247

    commonImplementation(project(":compiler:build-tools:kotlin-build-statistics"))
    commonImplementation(project(":kotlin-util-klib-metadata")) // TODO: consider removing in KT-70247

    commonRuntimeOnly(project(":kotlin-compiler-runner")) { // TODO: consider removing in KT-70247
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
    }
    for (compilerRuntimeDependency in unpublishedCompilerRuntimeDependencies) {
        embedded(project(compilerRuntimeDependency)) { isTransitive = false }
    }

    embedded(project(":kotlin-gradle-build-metrics"))
    embedded(project(":kotlin-gradle-statistics"))
    embedded(libs.intellij.asm) { isTransitive = false }
    embedded(commonDependency("com.google.code.gson:gson")) { isTransitive = false }
    embedded(libs.develocity.gradlePluginAdapter)
    embedded("org.jetbrains.kotlinx:kotlinx-serialization-json") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        version {
            strictly(GradlePluginVariant.GRADLE_MIN.compatibleKotlinxJsonSerializationVersion)
        }
    }
    embedded(libs.guava) { isTransitive = false }
    embedded(libs.guava.failureaccess) { isTransitive = false }
    embedded(commonDependency("org.jetbrains.teamcity:serviceMessages")) { isTransitive = false }
    embedded(project(":kotlin-tooling-metadata")) { isTransitive = false }
    embedded("com.github.gundy:semver4j:0.16.4:nodeps") {
        exclude(group = "*")
    }

    commonCompileOnly(libs.apache.commons.compress)
    embedded(libs.apache.commons.compress)

    // Adding workaround KT-57317 for Gradle versions where Kotlin runtime <1.8.0
    "mainEmbedded"(project(":kotlin-build-tools-enum-compat"))

    commonCompileOnly(libs.bouncycastle.bcpkix.jdk18on)
    commonCompileOnly(libs.bouncycastle.bcpg.jdk18on)

    testCompileOnly(project(":compiler"))

    testImplementation(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    testImplementation(testFixtures(project(":kotlin-build-common")))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
    testImplementation(project(":kotlin-compiler-runner"))
    testImplementation(kotlin("test-junit5", coreDepsVersion))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)

    testImplementation(project(":kotlin-gradle-statistics"))
    testImplementation(project(":kotlin-tooling-metadata"))
    testImplementation(libs.lincheck)
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(libs.slf4j.api)

}

configurations.commonCompileClasspath.get().exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")

/**
 * Security Advisory: Vulnerable Transitive Dependencies
 *
 * The dependency com.android.tools.build:gradle:8.8.1 introduces several transitive
 * dependencies with known security vulnerabilities. The following configuration
 * enforces safer versions of these dependencies.
 *
 * Affected Libraries:
 * ├── com.google.protobuf
 * │   ├── protobuf-java:* → 3.25.5
 * │   └── protobuf-java-util:3.22.3
 * ├── io.netty
 * │   ├── netty-buffer:*
 * │   ├── netty-codec-http:* → 4.1.127.Final
 * │   ├── netty-codec-http2:* → 4.1.127.Final
 * │   ├── netty-common:* → 4.1.127.Final
 * │   └── netty-handler:* → 4.1.127.Final
 * ├── org.apache.commons
 * │   ├── commons-compress:* → 1.27.1
 * │   └── commons-io:* → 2.16.1
 * └── org.bouncycastle:bcpkix-jdk18on:* → 1.79
 *
 * Mitigated Vulnerabilities:
 * 1. Google Protobuf
 *    - CVE-2024-7254: Potential security vulnerability
 *
 * 2. Netty Components
 *    - CVE-2025-25193: Denial of Service Vulnerability
 *    - CVE-2024-47535: Network security vulnerability
 *    - CVE-2024-29025: Remote code execution risk
 *    - CVE-2023-4586: Information disclosure vulnerability
 *    - CVE-2023-34462: Potential denial of service
 *    - CVE-2025-58056: Inconsistent Interpretation of HTTP Requests
 *    - CVE-2025-58057: mproper Handling of Highly Compressed Data
 *
 * 3. Bouncy Castle
 *    - CVE-2024-34447: Cryptographic security issue
 *    - CVE-2024-30172: Potential encryption vulnerability
 *    - CVE-2024-30171: Security protocol weakness
 *    - CVE-2024-29857: Cryptographic implementation flaw
 */
configurations.all {
    resolutionStrategy.eachDependency {
        // Google Protobuf
        if (requested.group == "com.google.protobuf" && requested.name == "protobuf-java") {
            useVersion("3.25.6")
            because("CVE-2024-7254")
        }

        // Netty Components
        if (requested.group == "io.netty" &&
            listOf(
                "netty-buffer",
                "netty-codec-http2",
                "netty-handler-proxy",
            ).contains(requested.name)
        ) {
            useVersion("4.1.127.Final")
            because("CVE-2025-25193, CVE-2024-47535, CVE-2024-29025, CVE-2023-4586, CVE-2023-34462, CVE-2025-55163, CVE-2025-58056, CVE-2025-58057")
        }

        if (requested.group == "org.apache.commons" && requested.name == "commons-lang3") {
            useVersion(libs.versions.commons.lang.get())
            because("CVE-2025-48924")
        }

        checkAndOverrideBouncyCastleVersion(project)
    }
}

tasks {
    named<ProcessResources>("processCommonResources") {
        val propertiesToExpand = mapOf(
            "projectVersion" to project.version,
            "kotlinNativeVersion" to project.kotlinNativeVersion,
            "kotlinWebNpmToolingDirName" to kotlinWebNpmToolingDirName,
            "bouncyCastleVersion" to libs.versions.bouncycastle.get(),
        )
        for ((name, value) in propertiesToExpand) {
            inputs.property(name, value)
        }
        filesMatching("project.properties") {
            expand(propertiesToExpand)
        }
    }

    withType<ShadowJar>().configureEach {
        relocate("com.github.gundy", "$kotlinEmbeddableRootPackage.com.github.gundy")
        val baseSourcePackage = "org.jetbrains.kotlin"
        val baseTargetPackage = "org.jetbrains.kotlin.gradle.internal"
        relocate("kotlinx.serialization", baseTargetPackage)
        val packages: Map<String, List<String>> = mapOf(
            "analyzer" to emptyList(),
            "build" to listOf(
                "org.jetbrains.kotlin.build.report.**",
            ),
            "backend" to emptyList(),
            "builtins" to emptyList(),
            "config" to listOf(
                "org.jetbrains.kotlin.config.ApiVersion**", // used a lot in buildscripts
                "org.jetbrains.kotlin.config.JvmTarget**", // used a lot in buildscripts
                "org.jetbrains.kotlin.config.KotlinCompilerVersion", // used a lot in buildscripts
                "org.jetbrains.kotlin.config.Services**", // required to initialize `CompilerEnvironment`
            ),
            "constant" to emptyList(),
            "container" to emptyList(),
            "contracts" to emptyList(),
            "descriptors" to emptyList(),
            "extensions" to emptyList(),
            "idea" to emptyList(),
            "ir" to emptyList(),
            "kapt3.diagnostic" to emptyList(),
            "load" to emptyList(),
            "metadata" to emptyList(),
            "modules" to emptyList(),
            "mpp" to emptyList(),
            "name" to emptyList(),
            "platform" to emptyList(),
            "progress" to emptyList(),
            "renderer" to emptyList(),
            "resolve" to emptyList(),
            "serialization" to emptyList(),
            "storage" to emptyList(),
            "types" to emptyList(),
            "type" to emptyList(),
            "utils" to emptyList(),
            "util" to listOf(
                "org.jetbrains.kotlin.util.Logger", // symbol from a standalone published artifact, don't relocate usages
                "org.jetbrains.kotlin.util.UtilKt", // class from kotlin-util-io which is a transitive API dependency of KGP-API, don't relocate usages
                "org.jetbrains.kotlin.util.capitalizeDecapitalize.CapitalizeDecapitalizeKt", // used in standalone published artifacts that the plugin depends on
            ),
        )
        packages.forEach { (pkg, exclusions) ->
            relocate("$baseSourcePackage.$pkg.", "$baseTargetPackage.$pkg.") {
                exclusions.forEach { exclude(it) }
            }
        }

        /*
        Disable Kotlin Module remapping to allow our own 'KotlinModuleMetadataVersionBasedSkippingTransformer' to run
         */
        enableKotlinModuleRemapping = false
        transform(KotlinModuleMetadataVersionBasedSkippingTransformer::class.java) {
            /*
             * This excludes .kotlin_module files for compiler modules from the fat jars.
             * These files are required only at compilation time, but we include the modules only for runtime
             * Hack for not limiting LV to 1.8 for those modules. To be removed after KT-70247
             */
            pivotVersion = KotlinMetadataPivotVersion(1, 9, 0)
        }
        asmDeprecation {
            val exclusions = listOf(
                "org.jetbrains.kotlin.gradle.**", // part of the plugin
                "org.jetbrains.kotlin.statistics.**", // part of the plugin
                "org.jetbrains.kotlin.tooling.**", // part of the plugin
                "org.jetbrains.kotlin.org.**", // already shadowed dependencies
                "org.jetbrains.kotlin.com.**", // already shadowed dependencies
                "org.jetbrains.kotlin.it.unimi.**", // already shadowed dependencies
                "org.jetbrains.kotlin.internal.**", // already internal package
            )
            val deprecationMessage = """
                You're using a Kotlin compiler class bundled into KGP for its internal needs.
                This is discouraged and will not be supported in future releases.
                The class in this artifact is scheduled for removal in Kotlin 2.2. Please define dependency on it in an alternative way.
                See https://kotl.in/gradle/internal-compiler-symbols for more details
            """.trimIndent()
            deprecateClassesByPattern("org.jetbrains.kotlin.**", deprecationMessage, exclusions)
        }
    }
    GradlePluginVariant.values().forEach { variant ->
        val sourceSet = sourceSets.getByName(variant.sourceSetName)
        val taskSuffix = sourceSet.jarTaskName.capitalize()
        val shadowJarTaskName = "$EMBEDDABLE_COMPILER_TASK_NAME$taskSuffix"
        asmDeprecation {
            val dumpTask = registerDumpDeprecationsTask(shadowJarTaskName, taskSuffix)
            val dumpAllTask = getOrCreateTask<Task>("dumpDeprecations") {
                dependsOn(dumpTask)
            }
            val expectedFileDoesNotExistMessage = """
                The file with expected deprecations for the compiler modules bundled into KGP does not exist.
                Run ./gradlew ${project.path}:${dumpTask.name} first to create it.
                You may also use ./gradlew ${project.path}:${dumpAllTask.name} to dump deprecations of all fat jars.
                Context: https://youtrack.jetbrains.com/issue/KT-70251
            """.trimIndent()
            val checkFailureMessage = """
                Expected deprecations applied to the compiler modules bundled into KGP does not match with the actually applied ones.
                Run ./gradlew ${project.path}:${dumpTask.name} to see the difference.
                You may also use ./gradlew ${project.path}:${dumpAllTask.name} to dump deprecations of all fat jars.
                Use INFO level log for the exact deprecated classes set.
                Either commit the difference or adjust the package relocation rules in ${buildFile.absolutePath}
                Please be sure to leave a comment explaining any changes related to this failure clear enough.
                Context: https://youtrack.jetbrains.com/issue/KT-70251
            """.trimIndent()
            val checkTask =
                registerCheckDeprecationsTask(shadowJarTaskName, taskSuffix, expectedFileDoesNotExistMessage, checkFailureMessage)
            named("check") {
                dependsOn(checkTask)
            }
        }
    }
}

tasks.named("validatePlugins") {
    // We're manually registering and wiring validation tasks for each plugin variant
    enabled = false
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
    }
}

gradlePlugin {
    plugins {
        create("kotlinJvmPlugin") {
            id = "org.jetbrains.kotlin.jvm"
            description = "Kotlin JVM plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
        create("kotlinJsPlugin") {
            id = "org.jetbrains.kotlin.js"
            description = "Kotlin JS plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.KotlinJsPluginWrapper"
        }
        create("kotlinMultiplatformPlugin") {
            id = "org.jetbrains.kotlin.multiplatform"
            description = "Kotlin Multiplatform plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
        create("kotlinAndroidPlugin") {
            id = "org.jetbrains.kotlin.android"
            description = "Kotlin Android plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
        create("kotlinParcelizePlugin") {
            id = "org.jetbrains.kotlin.plugin.parcelize"
            description = "Kotlin Parcelize plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.internal.ParcelizeSubplugin"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
        create("kotlinKaptPlugin") {
            id = "org.jetbrains.kotlin.kapt"
            description = "Kotlin Kapt plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
        create("kotlinScriptingPlugin") {
            id = "org.jetbrains.kotlin.plugin.scripting"
            description = "Gradle plugin for kotlin scripting"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
        create("kotlinNativeCocoapodsPlugin") {
            id = "org.jetbrains.kotlin.native.cocoapods"
            description = "Kotlin Native plugin for CocoaPods integration"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin"

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

// Gradle plugins functional tests

// Workaround for KT-75550
tasks.named("gradle813Jar") {
    enabled = false
}

val gradlePluginVariantForFunctionalTests = GradlePluginVariant.GRADLE_813
val gradlePluginVariantSourceSet = sourceSets.getByName(gradlePluginVariantForFunctionalTests.sourceSetName)
val functionalTestSourceSet = sourceSets.create("functionalTest") {
    compileClasspath += gradlePluginVariantSourceSet.output
    runtimeClasspath += gradlePluginVariantSourceSet.output

    configurations.getByName(implementationConfigurationName) {
        extendsFrom(configurations.getByName(gradlePluginVariantSourceSet.implementationConfigurationName))
        extendsFrom(configurations.getByName(testSourceSet.implementationConfigurationName))
    }

    configurations.getByName(runtimeOnlyConfigurationName) {
        extendsFrom(configurations.getByName(gradlePluginVariantSourceSet.runtimeOnlyConfigurationName))
        extendsFrom(configurations.getByName(testSourceSet.runtimeOnlyConfigurationName))
    }
}

sourceSets.getByName("testFixtures") {
    /*
     * testFixtures source set is closer to regular dependencies,
     * so that it already has access to main and its transitive API dependencies.
     * Thus, there's no need to copy the main dependencies.
     *
     * Instead of copying dependencies from testSourceSet, define granular dependencies here,
     * as textFixtures are shared with integration test projects,
     * and it's preferable to have granular control over them.
     * Also, it prevents compilation problems due to dependencies from the test source set of too high LV (like compiler modules).
     */
    dependencies {
        add(implementationConfigurationName, commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
        add(implementationConfigurationName, gradleApi())
        add(implementationConfigurationName, libs.junit.jupiter.api)
    }
}

fun KotlinWithJavaCompilation<*, *>.enableKotlinSerializationPlugin() {
    val version = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    configurations.pluginConfiguration.dependencies.add(
        dependencies.create("org.jetbrains.kotlin:kotlin-serialization-compiler-plugin-embeddable:${version}")
    )
}

// Enforce lowest jvm version to make testFixtures compatible with KGP-IT injections
val testFixturesCompilation = kotlin.target.compilations.getByName("testFixtures")
testFixturesCompilation.compileJavaTaskProvider.configure {
    sourceCompatibility = JavaLanguageVersion.of(8).toString()
    targetCompatibility = JavaLanguageVersion.of(8).toString()
}
testFixturesCompilation.compileTaskProvider.configure {
    with(this as KotlinCompile) {
        configureGradleCompatibility()
    }
}
testFixturesCompilation.enableKotlinSerializationPlugin()

val functionalTestCompilation = kotlin.target.compilations.getByName("functionalTest")
functionalTestCompilation.compileJavaTaskProvider.configure {
    sourceCompatibility = JavaLanguageVersion.of(17).toString()
    targetCompatibility = JavaLanguageVersion.of(17).toString()
}
functionalTestCompilation.compileTaskProvider.configure {
    with(this as KotlinCompile) {
        kotlinJavaToolchain.toolchain.use(project.getToolchainLauncherFor(JdkMajorVersion.JDK_17_0))
    }
}

functionalTestCompilation.enableKotlinSerializationPlugin()
functionalTestCompilation.associateWith(kotlin.target.compilations.getByName(gradlePluginVariantForFunctionalTests.sourceSetName))
functionalTestCompilation.associateWith(kotlin.target.compilations.getByName("common"))
functionalTestCompilation.associateWith(testFixturesCompilation)

// Explicit list of project dependencies that functional tests resolve at runtime.
// isTransitive=false avoids resolving the full graph (which triggers sourcesJar errors).
val functionalTestBuildDeps: Configuration = configurations.detachedConfiguration(
    project.dependencies.project(":kotlin-compiler-embeddable"),
    project.dependencies.project(":kotlin-scripting-compiler-embeddable"),
    project.dependencies.project(":kotlin-scripting-compiler-impl-embeddable"),
    project.dependencies.project(":kotlin-stdlib"),
    project.dependencies.project(":kotlin-stdlib-jdk7"),
    project.dependencies.project(":kotlin-stdlib-jdk8"),
    project.dependencies.project(":kotlin-test"),
    project.dependencies.project(":kotlin-reflect"),
    project.dependencies.project(":kotlin-parcelize-compiler"),
    project.dependencies.project(":kotlin-script-runtime"),
    project.dependencies.project(":kotlin-scripting-common"),
    project.dependencies.project(":kotlin-scripting-jvm"),
    project.dependencies.project(":kotlin-gradle-plugin"),
    project.dependencies.project(":kotlin-gradle-plugin-api"),
    project.dependencies.project(":kotlin-gradle-plugin-annotations"),
    project.dependencies.project(":kotlin-gradle-plugin-idea"),
    project.dependencies.project(":kotlin-gradle-plugin-idea-proto"),
    project.dependencies.project(":kotlin-tooling-metadata"),
    project.dependencies.project(":kotlin-tooling-core"),
    project.dependencies.project(":native:kotlin-klib-commonizer-embeddable"),
    project.dependencies.project(":native:kotlin-klib-commonizer-api"),
    project.dependencies.project(":native:swift:swift-export-embeddable"),
    project.dependencies.project(":compiler:build-tools:kotlin-build-statistics"),
    project.dependencies.project(":compiler:build-tools:kotlin-build-tools-api"),
    project.dependencies.project(":compiler:build-tools:kotlin-build-tools-impl"),
    project.dependencies.project(":compiler:build-tools:kotlin-build-tools-compat"),
    project.dependencies.project(":compiler:build-tools:kotlin-build-tools-cri-impl"),
    project.dependencies.project(":kotlin-util-klib-metadata"),
    project.dependencies.project(":libraries:tools:abi-validation:abi-tools-api"),
    project.dependencies.project(":libraries:tools:abi-validation:abi-tools"),
    project.dependencies.project(":kotlin-metadata-jvm"),
)

// Stubs: empty JARs + POMs for artifacts tests resolve at runtime.
// ALL third-party deps are replaced with org.test:* stubs — no real coordinates.
val thirdPartyStubs = listOf(
    // Transitive of kotlin-stdlib
    "org.jetbrains:annotations:13.0",
    // Older kotlin versions referenced by mock tests
    "org.jetbrains.kotlin:kotlin-stdlib:1.7.10",
    "org.jetbrains.kotlin:kotlin-stdlib-common:1.3.10",
    "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.10",
    "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.0",
    "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.10",
    "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0",
    // junit/hamcrest (transitives of kotlin-test-junit)
    "junit:junit:4.13.2",
    "org.hamcrest:hamcrest-core:1.3",
    // Test stubs for mock-based dependency resolution tests (replaces mvikotlin, okio, ktor, etc.)
    "org.test:mock-kmp-lib:1.0",           // KMP lib (was mvikotlin:3.0.2)
    "org.test:mock-kmp-lib:2.0",           // KMP lib (was mvikotlin:3.2.1)
    "org.test:mock-kmp-lib-jvm:1.0",
    "org.test:mock-kmp-lib-jvm:2.0",
    "org.test:mock-kmp-lib-linuxx64:1.0",
    "org.test:mock-kmp-lib-android:1.0",
    "org.test:mock-kmp-lib-rx-linuxx64:1.0",
    "org.test:mock-kmp-lib-rx2-linuxx64:1.0",
    "org.test:mock-kmp-lib-utils-linuxx64:1.0",
    "org.test:mock-transitive-a-jvm:1.0",  // (was essenty:lifecycle-jvm)
    "org.test:mock-transitive-a-jvm:2.0",
    "org.test:mock-transitive-a-linuxx64:1.0",
    "org.test:mock-transitive-b-jvm:1.0",  // (was essenty:instance-keeper-jvm)
    "org.test:mock-transitive-b-jvm:2.0",
    "org.test:mock-transitive-b-linuxx64:1.0",
    "org.test:mock-transitive-c-linuxx64:1.0",
    "org.test:mock-kmp-lib-2:1.0",         // (was okio:3.2.0)
    "org.test:mock-kmp-lib-2:2.0",         // (was okio:3.3.0)
    "org.test:mock-kmp-lib-2-jvm:1.0",
    "org.test:mock-jvm-lib-a:1.0",         // (was ktor-client-core)
    "org.test:mock-jvm-lib-b:1.0",         // (was ktor-http)
    "org.test:mock-jvm-lib-c:1.0",         // (was ktor-utils)
    "org.test:mock-coroutines-common:1.0",
    "org.test:mock-coroutines-jvm:1.0",
    "org.test:mock-coroutines-io:1.0",
    "org.test:mock-kotlinx-io:1.0",
    "org.test:mock-atomicfu-common:1.0",
    "org.test:mock-serialization-json-jvm:1.0",
    "org.test:mock-serialization-json-jvm:1.1",
    "org.test:mock-serialization-json-js:1.1",
    "org.test:mock-serialization-json-linuxarm64:1.1",
    "org.test:mock-serialization-core:1.1",
    "org.test:mock-serialization-core-jvm:1.0",
    "org.test:mock-serialization-core-jvm:1.1",
    "org.test:mock-serialization-core-js:1.1",
    "org.test:mock-serialization-core-linuxarm64:1.1",
    // SwiftExport test stubs: KMP libraries with readable names
    "org.test:kmp-lib:1.0",
    "org.test:kmp-lib:1.1",             // (was coroutines:1.6.1)
    "org.test:kmp-lib:1.2",             // (was coroutines:1.6.4)
    "org.test:kmp-lib:1.5",             // (was coroutines:1.7.2)
    "org.test:kmp-lib:2.0",
    "org.test:kmp-lib:3.0",
    "org.test:kmp-lib:3.1",
    "org.test:kmp-lib-d:1.0",           // (was serialization-json)
    "org.test:kmp-lib-d:1.1",
    "org.test:kmp-lib-d:1.5",
    "org.test:kmp-lib-d:1.6",
    "org.test:dep-of-kmp-lib:1.0",
    "org.test:kmp-lib-b:1.0",
    "org.test:kmp-lib-c:1.0",
    "org.test:kmp-lib-d:1.0",
    "org.test:kmp-runtime-a:1.0",
    "org.test:jvm-lib:1.0",
    "org.test:kmp-compose-like:1.0",
)

val functionalTestDepsDir = layout.buildDirectory.dir("functionalTestDependencies")
data class DepCoords(val group: String, val name: String, val version: String)



val populateFunctionalTestRepo = tasks.register("populateFunctionalTestRepo") {
    val outputDir = functionalTestDepsDir
    val stubs = thirdPartyStubs
    notCompatibleWithConfigurationCache("resolves detached configurations at execution time")
    outputs.dir(outputDir)
    doLast {
        val repoDir = outputDir.get().asFile
        repoDir.deleteRecursively()
        repoDir.mkdirs()

        val emptyJar = byteArrayOf(0x50, 0x4B, 0x05, 0x06, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        fun mavenDir(group: String, name: String, version: String) =
            repoDir.resolve(group.replace('.', '/')).resolve(name).resolve(version).also { it.mkdirs() }

        fun generatePom(group: String, name: String, version: String, deps: List<DepCoords> = emptyList(), hasModuleFile: Boolean = false): String {
            val depsXml = if (deps.isEmpty()) "" else "\n  <dependencies>\n" +
                deps.joinToString("\n") { "    <dependency>\n      <groupId>${it.group}</groupId>\n      <artifactId>${it.name}</artifactId>\n      <version>${it.version}</version>\n    </dependency>" } +
                "\n  </dependencies>"
            // Only add published-with-gradle-metadata marker if a .module file exists.
            // Without this marker, Gradle uses the POM for dependency metadata (transitives).
            // With the marker but no .module file, Gradle may ignore POM transitives entirely.
            val metadataMarker = if (hasModuleFile) "\n  <!-- do_not_remove: published-with-gradle-metadata -->" else ""
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project>$metadataMarker\n  <modelVersion>4.0.0</modelVersion>\n  <groupId>$group</groupId>\n  <artifactId>$name</artifactId>\n  <version>$version</version>$depsXml\n</project>"
        }

        // Resolve at execution time (not config time) to avoid mutation errors
        val resolved = functionalTestBuildDeps.resolvedConfiguration.resolvedArtifacts
        val kotlinVersion = resolved.firstOrNull()?.moduleVersion?.id?.version ?: "unknown"
        val annotationsDep = DepCoords("org.jetbrains", "annotations", "13.0")
        val stdlibDep = DepCoords("org.jetbrains.kotlin", "kotlin-stdlib", kotlinVersion)
        val kmpLibDep = DepCoords("org.test", "kmp-lib", "1.0")
        val knownTransitives = mapOf(
            // Our Kotlin project artifacts
            "kotlin-stdlib" to listOf(annotationsDep),
            "kotlin-stdlib-jdk7" to listOf(stdlibDep),
            "kotlin-stdlib-jdk8" to listOf(stdlibDep, DepCoords("org.jetbrains.kotlin", "kotlin-stdlib-jdk7", kotlinVersion)),
            "kotlin-test" to listOf(stdlibDep),
            "kotlin-test-junit" to listOf(DepCoords("junit", "junit", "4.13.2")),
            "junit" to listOf(DepCoords("org.hamcrest", "hamcrest-core", "1.3")),
            // Test stubs: declare transitives so POM-based resolution works
            "mock-kmp-lib-jvm" to listOf(
                DepCoords("org.test", "mock-transitive-a-jvm", "1.0"),
                DepCoords("org.test", "mock-transitive-b-jvm", "1.0"),
            ),
            "mock-jvm-lib-a" to listOf(
                DepCoords("org.test", "mock-jvm-lib-b", "1.0"),
                DepCoords("org.test", "mock-jvm-lib-c", "1.0"),
            ),
            "mock-serialization-json-jvm" to listOf(
                DepCoords("org.test", "mock-serialization-core-jvm", "1.0"),
            ),
            // SwiftExport test stubs
            "kmp-lib" to listOf(DepCoords("org.test", "dep-of-kmp-lib", "1.0")),
            "kmp-runtime-a" to listOf(kmpLibDep),
            "kmp-compose-like" to listOf(kmpLibDep),
            "kmp-lib-b" to listOf(DepCoords("org.test", "kmp-lib-d", "1.0")),
        )

        // Resolve at execution time (not config time) to avoid mutation errors
        resolved.forEach { artifact ->
            val id = artifact.moduleVersion.id
            val dir = mavenDir(id.group, id.name, id.version)
            artifact.file.copyTo(dir.resolve(artifact.file.name), overwrite = true)
            // Copy metadata (.pom, .module) and essential JARs (-all.jar) from ~/.m2.
            // Skip sources, javadoc, and checksum files to save disk space.
            val m2Dir = File(System.getProperty("user.home"), ".m2/repository/${id.group.replace('.', '/')}/${id.name}/${id.version}")
            if (m2Dir.exists()) {
                m2Dir.listFiles()?.filter { f ->
                    f.name.endsWith(".pom") || f.name.endsWith(".module") ||
                    (f.name.endsWith(".jar") && !f.name.contains("-sources") && !f.name.contains("-javadoc"))
                }?.forEach { file -> file.copyTo(dir.resolve(file.name), overwrite = true) }
            } else {
                dir.resolve("${id.name}-${id.version}.pom")
                    .writeText(generatePom(id.group, id.name, id.version, knownTransitives[id.name].orEmpty()))
            }
        }

        // Also copy JS/Wasm sub-module artifacts that kotlin-stdlib's .module redirects to
        val m2Base = File(System.getProperty("user.home"), ".m2/repository/org/jetbrains/kotlin")
        listOf("kotlin-stdlib-js", "kotlin-stdlib-wasm-js", "kotlin-stdlib-wasm-wasi",
               "kotlin-stdlib-common",
               "kotlin-dom-api-compat",
               "kotlin-test-js", "kotlin-test-junit", "kotlin-test-junit5",
               "kotlin-test-annotations-common", "kotlin-test-common",
               "kotlin-test-wasm-js", "kotlin-test-wasm-wasi").forEach { subModule ->
            val m2SubDir = m2Base.resolve("$subModule/$kotlinVersion")
            if (m2SubDir.exists()) {
                val targetDir = mavenDir("org.jetbrains.kotlin", subModule, kotlinVersion)
                m2SubDir.listFiles()?.filter { f ->
                    f.name.endsWith(".pom") || f.name.endsWith(".module") ||
                    f.name.endsWith(".jar") || f.name.endsWith(".klib")
                }?.forEach { file -> file.copyTo(targetDir.resolve(file.name), overwrite = true) }
            }
        }

        // KMP stubs need Gradle Module Metadata with native variants for klib resolution
        val kmpStubs = setOf("kmp-lib", "kmp-lib-b", "kmp-lib-c", "kmp-lib-d", "dep-of-kmp-lib", "kmp-compose-like", "kmp-runtime-a",
            "mock-kmp-lib", "mock-kmp-lib-2")
        val nativeTargets = listOf(
            "ios_arm64", "ios_simulator_arm64", "ios_x64",
            "linux_x64", "linux_arm64",
            "macos_x64", "macos_arm64",
            "mingw_x64",
        )

        fun generateKmpModule(group: String, name: String, version: String, deps: List<DepCoords> = emptyList()): String {
            val depsJson = if (deps.isEmpty()) "" else """,
      "dependencies": [${deps.joinToString(", ") { """{"group": "${it.group}", "module": "${it.name}", "version": {"requires": "${it.version}"}}""" }}]"""
            val variants = nativeTargets.joinToString(",\n") { target ->
                val camelTarget = target.split("_").joinToString("") { it.replaceFirstChar(Char::titlecase) }.replaceFirstChar(Char::lowercase)
                """    {
      "name": "${camelTarget}ApiElements-published",
      "attributes": {
        "org.gradle.category": "library",
        "org.gradle.jvm.environment": "non-jvm",
        "org.gradle.usage": "kotlin-api",
        "org.jetbrains.kotlin.native.target": "$target",
        "org.jetbrains.kotlin.platform.type": "native"
      },
      "files": [{"name": "$name.klib", "url": "$name.klib"}]$depsJson
    }"""
            }
            // JVM variant uses available-at to redirect to a separate sub-module (like real KMP publishing).
            // This ensures the resolved component ID is "group:name-jvm:version" (separate module)
            // rather than "group:name:version" with a classifier.
            val groupPath = group.replace('.', '/')
            return """{
  "formatVersion": "1.1",
  "component": {"group": "$group", "module": "$name", "version": "$version"},
  "variants": [
    {"name": "jvmApiElements-published", "attributes": {"org.gradle.category": "library", "org.gradle.jvm.environment": "standard-jvm", "org.gradle.usage": "java-api", "org.jetbrains.kotlin.platform.type": "jvm"}, "available-at": {"url": "../../$name-jvm/$version/$name-jvm-$version.module", "group": "$group", "module": "$name-jvm", "version": "$version"}},
    {"name": "jvmRuntimeElements-published", "attributes": {"org.gradle.category": "library", "org.gradle.jvm.environment": "standard-jvm", "org.gradle.usage": "java-runtime", "org.jetbrains.kotlin.platform.type": "jvm"}, "available-at": {"url": "../../$name-jvm/$version/$name-jvm-$version.module", "group": "$group", "module": "$name-jvm", "version": "$version"}},
    {"name": "jsApiElements-published", "attributes": {"org.gradle.category": "library", "org.gradle.jvm.environment": "non-jvm", "org.gradle.usage": "kotlin-api", "org.jetbrains.kotlin.platform.type": "js"}, "files": [{"name": "$name-js-$version.klib", "url": "$name-js-$version.klib"}]$depsJson},
    {"name": "wasmJsApiElements-published", "attributes": {"org.gradle.category": "library", "org.gradle.jvm.environment": "non-jvm", "org.gradle.usage": "kotlin-api", "org.jetbrains.kotlin.platform.type": "wasm", "org.jetbrains.kotlin.wasm.target": "js"}, "files": [{"name": "$name-wasm-js-$version.klib", "url": "$name-wasm-js-$version.klib"}]$depsJson},
$variants
  ]
}"""
        }

        // Generate a sub-module .module for JVM variant (referenced via available-at from root module)
        fun generateJvmSubModule(group: String, name: String, version: String, deps: List<DepCoords> = emptyList()): String {
            val depsJson = if (deps.isEmpty()) "" else """,
      "dependencies": [${deps.joinToString(", ") { """{"group": "${it.group}", "module": "${it.name}", "version": {"requires": "${it.version}"}}""" }}]"""
            return """{
  "formatVersion": "1.1",
  "component": {"group": "$group", "module": "$name-jvm", "version": "$version"},
  "variants": [
    {"name": "apiElements-published", "attributes": {"org.gradle.category": "library", "org.gradle.jvm.environment": "standard-jvm", "org.gradle.libraryelements": "jar", "org.gradle.usage": "java-api", "org.jetbrains.kotlin.platform.type": "jvm"}, "files": [{"name": "$name-jvm-$version.jar", "url": "$name-jvm-$version.jar"}]$depsJson},
    {"name": "runtimeElements-published", "attributes": {"org.gradle.category": "library", "org.gradle.jvm.environment": "standard-jvm", "org.gradle.libraryelements": "jar", "org.gradle.usage": "java-runtime", "org.jetbrains.kotlin.platform.type": "jvm"}, "files": [{"name": "$name-jvm-$version.jar", "url": "$name-jvm-$version.jar"}]$depsJson}
  ]
}"""
        }

        // Install third-party stubs: empty JARs + POMs + KMP module metadata
        stubs.forEach { coordStr ->
            val (group, name, version) = coordStr.split(":")
            val dir = mavenDir(group, name, version)
            dir.resolve("$name-$version.jar").writeBytes(emptyJar)
            val isKmp = name in kmpStubs
            dir.resolve("$name-$version.pom").writeText(generatePom(group, name, version, hasModuleFile = isKmp))
            if (name in kmpStubs) {
                val moduleDeps = knownTransitives[name].orEmpty()
                dir.resolve("$name-$version.module").writeText(generateKmpModule(group, name, version, moduleDeps))
                // Create klib for native variants
                dir.resolve("$name.klib").writeBytes(emptyJar)
                // JS and Wasm stubs
                dir.resolve("$name-js-$version.klib").writeBytes(emptyJar)
                dir.resolve("$name-wasm-js-$version.klib").writeBytes(emptyJar)
                // JVM sub-module (separate Maven module, referenced via available-at)
                val jvmDir = mavenDir(group, "$name-jvm", version)
                jvmDir.resolve("$name-jvm-$version.jar").writeBytes(emptyJar)
                jvmDir.resolve("$name-jvm-$version.pom").writeText(generatePom(group, "$name-jvm", version, moduleDeps))
                jvmDir.resolve("$name-jvm-$version.module").writeText(generateJvmSubModule(group, name, version, moduleDeps))
            }
        }
    }
}

tasks.register<Test>("functionalTest") {
    systemProperty("kotlinVersion", rootProject.extra["kotlinVersion"] as String)
    addFileProperty(muteCommonFile, "org.jetbrains.kotlin.test.mutes.file")
    useJUnitPlatform()
    dependsOn(populateFunctionalTestRepo)

    @OptIn(TemporaryTestFederationApi::class)
    isSmokeTest = true
}

val acceptLicensesTask = with(androidSdkProvisioner) {
    registerAcceptLicensesTask()
}

tasks.withType<Test>().configureEach {
    if (!name.startsWith("functional")) return@configureEach

    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Runs functional tests"
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    workingDir = projectDir
    // No dependsOnKotlinGradlePluginInstall() — replaced by syncFunctionalTestDependencies
    // which copies explicit project outputs to build/functionalTestDependencies/.
    androidSdkProvisioner {
        provideToThisTaskAsSystemProperty(ProvisioningType.SDK)
        dependsOn(acceptLicensesTask)
    }
    // TeamCity agents can have 16G RAM; keep CI below the local default to avoid killing the Gradle daemon.
    maxParallelForks = providers.gradleProperty("kotlin.test.maxParallelForks")
        .map(String::toInt)
        .orElse(kotlinBuildProperties.isTeamcityBuild.map { if (it) 4 else 8 })
        .get()
    maxHeapSize = "4G" // KT-72460 to investigate why we need to change heap size

    testLogging {
        events("passed", "skipped", "failed")
    }

    addClasspathProperty(
        project.files(layout.projectDirectory.dir("src/functionalTest/resources")),
        "resourcesPath"
    )

    addFileProperty(
        rootProject.layout.projectDirectory.file("kotlin-native/konan/konan.properties"),
        "konanProperties"
    )

    // Build-local directory with resolved project dependencies.
    // Tests use this as a flatDir repo instead of mavenLocal().
    addClasspathProperty(project.files(functionalTestDepsDir), "functionalTestDepsDir")

    // Redirect AGP's analytics/preferences directory to the build directory so that
    // tests do not write analytics.settings to ~/.android.
    // Clear deprecated ANDROID_SDK_HOME to prevent conflict with ANDROID_USER_HOME -
    // CI sets ANDROID_SDK_HOME for SDK location, but AGP also interprets it as preferences root.
    // SDK resolution uses android.sdk.root system property (from provisioner), not this env var.
    environment.remove("ANDROID_SDK_HOME")
    environment("ANDROID_USER_HOME", layout.buildDirectory.dir("android-user-home").get().asFile.absolutePath)

    extensions.configure<TestInputsCheckExtension>("testInputsCheck") {
        val androidSdkDirectory = provider { project.configurations.getByName("androidSdk").singleFile.canonicalFile }
        coarseInputDirectories.from(androidSdkDirectory)
        val konanDataDir = providers.gradleProperty("konan.data.dir")
            .orElse(providers.environmentVariable("KONAN_DATA_DIR"))
            .orElse(provider { System.getProperty("user.home") + File.separator + ".konan" })

        with(extraPermissions) {
            // Gradle's ProjectBuilder calls System.getProperties(), and functional tests
            // temporarily override host and IDE-sync properties.
            add("""permission java.util.PropertyPermission "*", "read,write";""")

            // Gradle's dependency resolution in ProjectBuilder needs proxy configuration access.
            add("""permission java.net.NetPermission "getProxySelector";""")

            // Gradle/AGP creates an MBean server while initializing ProjectBuilder services.
            add("""permission javax.management.MBeanServerPermission "*";""")
            // Gradle/AGP queries and invokes MBeans for JVM memory/container monitoring.
            add("""permission javax.management.MBeanPermission "*", "*";""")
            // Gradle/AGP registers trusted MBeans as part of the same monitoring setup.
            add("""permission javax.management.MBeanTrustPermission "*";""")

            // AGP reads and writes the legacy ~/.android directory even when ANDROID_USER_HOME is
            // redirected. Some AGP code paths (e.g. analytics.settings) hardcode the legacy path.
            val androidDir = File(System.getProperty("user.home"), ".android")
            // AGP checks and creates the legacy ~/.android directory itself.
            add("""permission java.io.FilePermission "${androidDir.absolutePath}", "read,write";""")
            // AGP reads and updates legacy files such as analytics.settings under ~/.android.
            add("""permission java.io.FilePermission "${androidDir.absolutePath}/-", "read,write";""")

            // Gradle probes ~/.m2 during Maven local initialization even when
            // mavenLocal() is not used — only settings.xml and repository dir are checked.
            val m2Home = File(System.getProperty("user.home"), ".m2")
            add("""permission java.io.FilePermission "${m2Home.absolutePath}${File.separator}settings.xml", "read";""")
            add("""permission java.io.FilePermission "${m2Home.absolutePath}${File.separator}repository", "read";""")

            // K/N writes lock files and caches toolchain archives under the konan data directory.
            // DependencyProcessor.downloadDependency() deletes stale extraction residue before
            // re-extracting; `delete` is a separate FilePermission action from `write`.
            addAll(konanDataDir.map {
                listOf(
                    // K/N resolves, extracts, updates, and cleans cached toolchain files.
                    """permission java.io.FilePermission "$it/-", "read,write,delete";""",
                    // K/N checks the konan data directory root before accessing cached files.
                    """permission java.io.FilePermission "$it", "read";""",
                )
            })

            // K/N dependency resolution executes tar from the environment PATH to extract toolchain archives.
            add("""permission java.io.FilePermission "<<ALL FILES>>", "execute";""")

            // K/N DependencyExtractor creates hard links when extracting toolchain archives.
            add("""permission java.nio.file.LinkPermission "hard";""")

            // Linux: Gradle reads cgroup memory limits and /proc info during ProjectBuilder
            // initialization (container-aware heap sizing, CPU-count probes).
            if (!OperatingSystem.current().isMacOsX && !OperatingSystem.current().isWindows) {
                // Gradle reads process and CPU information from procfs on Linux.
                add("""permission java.io.FilePermission "/proc/-", "read";""")
                // Gradle reads cgroup memory limits from sysfs on Linux.
                add("""permission java.io.FilePermission "/sys/fs/cgroup/-", "read";""")
            }

            // Android SDK directory.
            // AGP's LocalRepoLoaderImpl.writePackage mutates package.xml inside the provisioned
            // SDK during initialization, so write access is required on the provisioned tree.
            // The directory is tracked as a real input by androidSdkProvisioner. coarseInputDirectories
            // keeps test-inputs-check from expanding the entire SDK into the generated policy.
            // Use provider-backed addAll for lazy evaluation - configuration resolution must not
            // happen at configuration time.
            addAll(androidSdkDirectory.map { provisionedDirectory ->
                buildList {
                    val provisionedPath = provisionedDirectory.canonicalPath
                    // AGP reads SDK packages and updates package.xml files in the provisioned SDK.
                    add("""permission java.io.FilePermission "$provisionedPath/-", "read,write";""")
                    // AGP checks the provisioned SDK root before scanning or updating packages.
                    add("""permission java.io.FilePermission "$provisionedPath", "read,write";""")
                    val androidHome = System.getenv("ANDROID_HOME")
                    if (androidHome != null && androidHome != provisionedPath) {
                        // Some AGP/Android tooling probes ANDROID_HOME content even when sdk.dir points elsewhere.
                        add("""permission java.io.FilePermission "$androidHome/-", "read";""")
                        // Android tooling checks the ANDROID_HOME root before probing its content.
                        add("""permission java.io.FilePermission "$androidHome", "read";""")
                    }
                }
            })
        }
    }

    // No maven.repo.local forwarding — functional tests resolve JetBrains artifacts
    // from the build-local flatDir repo, not from ~/.m2.
}

dependencies {
    val implementation = project.configurations.getByName(functionalTestSourceSet.implementationConfigurationName)
    val compileOnly = project.configurations.getByName(functionalTestSourceSet.compileOnlyConfigurationName)

    implementation(libs.android.gradle.plugin.gradle)
    implementation(libs.android.gradle.plugin.gradle.api)
    compileOnly(libs.android.tools.common)
    implementation(gradleKotlinDsl())
    implementation(project(":kotlin-gradle-plugin-tcs-android"))
    implementation(project(":kotlin-tooling-metadata"))
    implementation(project.dependencies.testFixtures(project(":kotlin-gradle-plugin-idea")))
    implementation("com.github.gundy:semver4j:0.16.4:nodeps") {
        exclude(group = "*")
    }
    implementation("org.reflections:reflections:0.10.2")
    implementation(project(":compose-compiler-gradle-plugin"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json") {
        version {
            strictly(GradlePluginVariant.GRADLE_MIN.compatibleKotlinxJsonSerializationVersion)
        }
    }
    implementation(intellijPlatformUtil())
    implementation(libs.junit.jupiter.engine)
}

tasks.named("check") {
    dependsOn("functionalTest")
    dependsOn("lincheckTest")
}

fun avoidPublishingTestFixtures() {
    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
    javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
}
avoidPublishingTestFixtures()

tasks.withType<Jar>().configureEach {
    if (name.endsWith("SourcesJar")) {
        // FIXME: Entry org/jetbrains/kotlin/cli/common/arguments/CommonCompilerArguments.kt is a duplicate
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

kotlin {
    target.compilations.getByName("common").enableKotlinSerializationPlugin()
}

val generateKgpBuildConstants = registerGenerateKgpBuildConstantsTask {
    defaultYarnVersion = libs.versions.yarn
}

kotlin.sourceSets.common {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    generatedKotlin.srcDir(generateKgpBuildConstants)

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    generatedKotlin.srcDir(tasks.generateNpmVersionsKotlinClass)

    resources.srcDir(tasks.prepareKgpNpmToolingLockFiles)
}

node {
    version = nodejsVersion
}

tasks.test {
    val kgpNpmToolingPackageJson = kgpNpmTooling.npmToolingProjectDir.file("package.json")
    inputs.file(kgpNpmToolingPackageJson)
        .withPropertyName("kgpNpmToolingPackageJson")
        .withPathSensitivity(PathSensitivity.NAME_ONLY)
        .normalizeLineEndings()
    jvmArgumentProviders.add {
        listOf("-DkgpNpmToolingPackageJson=${kgpNpmToolingPackageJson.orNull?.asFile?.invariantSeparatorsPath}")
    }
}
