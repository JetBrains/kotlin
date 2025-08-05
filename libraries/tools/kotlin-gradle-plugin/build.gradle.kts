import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import gradle.GradlePluginVariant
import org.jetbrains.kotlin.build.androidsdkprovisioner.ProvisioningType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("gradle-plugin-common-configuration")
    id("kotlin-git.gradle-build-conventions.binary-compatibility-extended")
    id("android-sdk-provisioner")
    id("asm-deprecating-transformer")
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

tasks.test {
    useJUnit {
        exclude("**/*LincheckTest.class")
    }
}

tasks.register<Test>("lincheckTest") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))

    jvmArgs(
        "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-exports", "java.base/jdk.internal.util=ALL-UNNAMED",
        "--add-exports", "java.base/sun.security.action=ALL-UNNAMED"
    )
    filter { include("**/*LincheckTest.class") }
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
    ":compiler:cli-common", // for compiler arguments setup, for logging via MessageCollector, CompilerSystemProperties, ExitCode
    ":compiler:compiler.version", // for user projects buildscripts, `loadCompilerVersion`
    ":compiler:config", // for CommonCompilerArguments initialization
    ":compiler:config.jvm", // for K2JVMCompilerArguments initialization
    ":compiler:ir.serialization.common", // for PartialLinkageMode (K/N)
    ":compiler:util", // for CommonCompilerArguments initialization, K/N
    ":core:compiler.common", // for FUS statistics parsing all the compiler arguments
    ":core:compiler.common.jvm", // for FUS statistics parsing all the compiler arguments
    ":core:descriptors", // for `fromUIntToLong`
    ":core:util.runtime", // for stdlib extensions
    ":kotlin-build-common", // for incremental compilation setup
    ":wasm:wasm.config", // for k/js task
)

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-api"))
    commonApi(project(":kotlin-gradle-plugin-model"))
    commonApi(project(":libraries:tools:gradle:fus-statistics-gradle-plugin"))

    // Following two dependencies is a workaround for IDEA import to pick-up them correctly
    commonCompileOnly(project(":kotlin-gradle-plugin-api")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-api-common")
        }
    }
    commonCompileOnly(project(":kotlin-gradle-plugin-model")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-model-common")
        }
    }

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
    commonCompileOnly(libs.android.gradle.plugin.gradle.api) { isTransitive = false }
    commonCompileOnly(libs.android.gradle.plugin.gradle) { isTransitive = false }
    commonCompileOnly(libs.android.gradle.plugin.builder) { isTransitive = false }
    commonCompileOnly(libs.android.gradle.plugin.builder.model) { isTransitive = false }
    commonCompileOnly(libs.android.tools.common) { isTransitive = false }
    commonCompileOnly(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    commonCompileOnly(libs.develocity.gradlePlugin)
    commonCompileOnly(commonDependency("com.google.code.gson:gson"))
    commonCompileOnly("com.github.gundy:semver4j:0.16.4:nodeps") {
        exclude(group = "*")
    }
    commonCompileOnly(project(":kotlin-tooling-metadata"))
    commonCompileOnly(project(":compiler:build-tools:kotlin-build-statistics"))
    commonCompileOnly(project(":native:swift:swift-export-standalone"))
    commonCompileOnly(libs.intellij.asm) { isTransitive = false }

    commonImplementation(project(":kotlin-gradle-plugin-idea"))
    commonImplementation(project(":kotlin-gradle-plugin-idea-proto"))
    commonImplementation(project(":native:kotlin-klib-commonizer-api")) // TODO: consider removing in KT-70247

    commonImplementation(project(":compiler:build-tools:kotlin-build-statistics"))
    commonImplementation(project(":kotlin-util-klib-metadata")) // TODO: consider removing in KT-70247

    commonImplementation(project(":libraries:tools:abi-validation:abi-tools-api"))

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
    embedded(libs.guava) { isTransitive = false }
    embedded(libs.guava.failureaccess) { isTransitive = false }
    embedded(commonDependency("org.jetbrains.teamcity:serviceMessages")) { isTransitive = false }
    embedded(project(":kotlin-tooling-metadata")) { isTransitive = false }
    embedded("com.github.gundy:semver4j:0.16.4:nodeps") {
        exclude(group = "*")
    }

    commonCompileOnly(libs.apache.commons.compress)
    embedded(libs.apache.commons.compress)

    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        // Adding workaround KT-57317 for Gradle versions where Kotlin runtime <1.8.0
        "mainEmbedded"(project(":kotlin-build-tools-enum-compat"))
    }

    commonCompileOnly("org.bouncycastle:bcpkix-jdk18on:1.80")
    commonCompileOnly("org.bouncycastle:bcpg-jdk18on:1.80")

    testCompileOnly(project(":compiler"))
    testCompileOnly(project(":kotlin-annotation-processing"))

    testImplementation(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    testImplementation(projectTests(":kotlin-build-common"))
    testImplementation(project(":kotlin-compiler-runner"))
    testImplementation(kotlinTest("junit"))
    testImplementation(libs.junit.jupiter.api)

    testImplementation(project(":kotlin-gradle-statistics"))
    testImplementation(project(":kotlin-tooling-metadata"))
    testImplementation(libs.lincheck)
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

configurations.commonCompileClasspath.get().exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-core")

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    configurations.commonApi.get().exclude("com.android.tools.external.com-intellij", "intellij-core")
}

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
 * │   ├── netty-codec-http:* → 4.1.118.Final
 * │   ├── netty-codec-http2:* → 4.1.118.Final
 * │   ├── netty-common:* → 4.1.118.Final
 * │   └── netty-handler:* → 4.1.118.Final
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
            useVersion("4.1.118.Final")
            because("CVE-2025-25193, CVE-2024-47535, CVE-2024-29025, CVE-2023-4586, CVE-2023-34462")
        }

        // Bouncy Castle
        if (requested.group == "org.bouncycastle" && requested.name == "bcpkix-jdk18on") {
            useVersion("1.80")
            because("CVE-2024-34447, CVE-2024-30172, CVE-2024-30171, CVE-2024-29857")
        }
    }
}

tasks {
    named<ProcessResources>("processCommonResources") {
        val propertiesToExpand = mapOf(
            "projectVersion" to project.version,
            "kotlinNativeVersion" to project.kotlinNativeVersion,
            "kotlinWebNpmToolingDirName" to kotlinWebNpmToolingDirName,
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
        transform(KotlinModuleMetadataVersionBasedSkippingTransformer::class.java) {
            /*
             * This excludes .kotlin_module files for compiler modules from the fat jars.
             * These files are required only at compilation time, but we include the modules only for runtime
             * Hack for not limiting LV to 1.7 for those modules. To be removed after KT-70247
             */
            pivotVersion = KotlinMetadataPivotVersion(1, 8, 0)
        }
        asmDeprecation {
            val exclusions = listOf(
                "org.jetbrains.kotlin.gradle.**", // part of the plugin
                "org.jetbrains.kotlin.project.model.**", // part of the plugin
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
        if (kotlinBuildProperties.isInJpsBuildIdeaSync) return@forEach
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

projectTest {
    workingDir = rootDir
}

gradlePlugin {
    plugins {
        create("kotlinJvmPlugin") {
            id = "org.jetbrains.kotlin.jvm"
            description = "Kotlin JVM plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper"
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
        }
        create("kotlinAndroidPlugin") {
            id = "org.jetbrains.kotlin.android"
            description = "Kotlin Android plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper"
        }
        create("kotlinAndroidExtensionsPlugin") {
            id = "org.jetbrains.kotlin.android.extensions"
            description = "Kotlin Android Extensions plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.internal.AndroidExtensionsSubpluginIndicator"
        }
        create("kotlinParcelizePlugin") {
            id = "org.jetbrains.kotlin.plugin.parcelize"
            description = "Kotlin Parcelize plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.internal.ParcelizeSubplugin"
        }
        create("kotlinKaptPlugin") {
            id = "org.jetbrains.kotlin.kapt"
            description = "Kotlin Kapt plugin"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin"
        }
        create("kotlinScriptingPlugin") {
            id = "org.jetbrains.kotlin.plugin.scripting"
            description = "Gradle plugin for kotlin scripting"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin"
        }
        create("kotlinNativeCocoapodsPlugin") {
            id = "org.jetbrains.kotlin.native.cocoapods"
            description = "Kotlin Native plugin for CocoaPods integration"
            displayName = description
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin"
        }
    }
}

// Gradle plugins functional tests
if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {

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
        }
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

    functionalTestCompilation.configurations.pluginConfiguration.dependencies.add(
        dependencies.create("org.jetbrains.kotlin:kotlin-serialization-compiler-plugin-embeddable")
    )
    functionalTestCompilation.associateWith(kotlin.target.compilations.getByName(gradlePluginVariantForFunctionalTests.sourceSetName))
    functionalTestCompilation.associateWith(kotlin.target.compilations.getByName("common"))
    functionalTestCompilation.associateWith(testFixturesCompilation)

    tasks.register<Test>("functionalTest") {
        systemProperty("kotlinVersion", rootProject.extra["kotlinVersion"] as String)
    }

    tasks.register<Test>("functionalUnitTest") {
        include("**/org/jetbrains/kotlin/gradle/unitTests/**")
        systemProperty("kotlinVersion", rootProject.extra["kotlinVersion"] as String)
    }

    tasks.register<Test>("functionalRegressionTest") {
        include("**/org/jetbrains/kotlin/gradle/regressionTests/**")
    }

    tasks.register<Test>("functionalDependencyResolutionTest") {
        include("**/org/jetbrains/kotlin/gradle/dependencyResolutionTests/**")
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
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(17))
        })
        dependsOnKotlinGradlePluginInstall()
        androidSdkProvisioner {
            provideToThisTaskAsSystemProperty(ProvisioningType.SDK)
            dependsOn(acceptLicensesTask)
        }
        maxParallelForks = 8
        maxHeapSize = "4G" // KT-72460 to investigate why we need to change heap size

        testLogging {
            events("passed", "skipped", "failed")
        }

        systemProperty("resourcesPath", layout.projectDirectory.dir("src/functionalTest/resources").asFile)

        //region custom Maven Local directory
        // The Maven Local dir that Gradle uses can be customised via system property `maven.repo.local`.
        // The functional tests require artifacts are published to Maven Local.
        // To make sure the tests uses the same `maven.repo.local` as is configured
        // in the buildscript, forward the value of `maven.repo.local` into the test process.
        val mavenRepoLocal = providers.systemProperty("maven.repo.local").orNull
        if (mavenRepoLocal != null) {
            // Only set `maven.repo.local` if it's present in the buildscript,
            // to avoid `maven.repo.local` being `null`.
            systemProperty("maven.repo.local", mavenRepoLocal)
        }
        //endregion
    }

    dependencies {
        val implementation = project.configurations.getByName(functionalTestSourceSet.implementationConfigurationName)
        val compileOnly = project.configurations.getByName(functionalTestSourceSet.compileOnlyConfigurationName)

        implementation("com.android.tools.build:gradle:8.8.1")
        implementation("com.android.tools.build:gradle-api:8.8.1")
        compileOnly("com.android.tools:common:31.7.2")
        implementation(gradleKotlinDsl())
        implementation(project(":kotlin-gradle-plugin-tcs-android"))
        implementation(project(":kotlin-tooling-metadata"))
        implementation(project.dependencies.testFixtures(project(":kotlin-gradle-plugin-idea")))
        implementation("com.github.gundy:semver4j:0.16.4:nodeps") {
            exclude(group = "*")
        }
        implementation("org.reflections:reflections:0.10.2")
        implementation(project(":compose-compiler-gradle-plugin"))
        implementation(libs.kotlinx.serialization.json)
    }

    tasks.named("check") {
        dependsOn("functionalTest")
        dependsOn("lincheckTest")
    }
}

fun avoidPublishingTestFixtures() {
    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
    javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
}
avoidPublishingTestFixtures()

registerKotlinSourceForVersionRange(
    GradlePluginVariant.GRADLE_MIN,
    GradlePluginVariant.GRADLE_88,
)
