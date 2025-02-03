plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    api(gradleApi())
    api(project(":kotlin-gradle-plugin-api"))
    api(project(":native:kotlin-native-utils"))
    implementation(projectTests(":generators")) {
        // because of hacky projectTests this transitive dependency does not work well
        // also it may bring a lot of unrelated dependencies
        isTransitive = false
    }
    implementation(project(":core:util.runtime"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(kotlin("test"))
}

val generateGroupName = "Generate"

val generateMppTargetContainerWithPresets by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.MppPresetFunctionsCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
    setKGPSourceRootPaths()
}

val generateAbstractBinaryContainer by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.MppNativeBinaryDSLCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
    setKGPSourceRootPaths()
}

val generateAbstractKotlinArtifactsExtensionImplementation by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.KotlinArtifactsDSLCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
    setKGPSourceRootPaths()
}

val generateMppSourceSetConventions by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.MppSourceSetConventionsCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
    setKGPSourceRootPaths()
}


fun JavaForkOptions.setKGPSourceRootPaths() {
    systemProperty(
        "org.jetbrains.kotlin.generators.gradle.dsl.kotlinGradlePluginSourceRoot",
        project(":kotlin-gradle-plugin").projectDir.resolve("src/common/kotlin").absolutePath
    )

    systemProperty(
        "org.jetbrains.kotlin.generators.gradle.dsl.kotlinGradlePluginApiSourceRoot",
        project(":kotlin-gradle-plugin-api").projectDir.resolve("src/common/kotlin").absolutePath
    )
}

projectTest(jUnitMode = JUnitMode.JUnit4, parallel = true) {
    useJUnit() // use JUnit4 as the `:generators` tests use JUnit 4, and we reuse the logic.
    setKGPSourceRootPaths()
}