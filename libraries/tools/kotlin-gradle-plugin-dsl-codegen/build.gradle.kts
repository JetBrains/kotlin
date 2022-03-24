plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    api(gradleApi())
    api(project(":kotlin-gradle-plugin-api"))
    api(project(":native:kotlin-native-utils"))
}

val generateGroupName = "Generate"

val generateMppTargetContainerWithPresets by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.MppPresetFunctionsCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
}

val generateAbstractBinaryContainer by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.MppNativeBinaryDSLCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
}

val generateKpmNativeVariants by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.KpmNativeVariantCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
}

listOf(generateMppTargetContainerWithPresets, generateAbstractBinaryContainer, generateKpmNativeVariants).forEach {
    it.systemProperty(
        "org.jetbrains.kotlin.generators.gradle.dsl.outputSourceRoot",
        project(":kotlin-gradle-plugin").projectDir.resolve("src/main/kotlin").absolutePath
    )
}

