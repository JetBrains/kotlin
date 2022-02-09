plugins {
    id("gradle-plugin-dependency-configuration")
}

dependencies {
    api(gradleApi())
    api(project(":kotlin-gradle-plugin-api"))
    api(project(":native:kotlin-native-utils"))
}

val generateMppTargetContainerWithPresets by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.MppPresetFunctionsCodegenKt",
    sourceSets["main"]
)

val generateAbstractBinaryContainer by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.MppNativeBinaryDSLCodegenKt",
    sourceSets["main"]
)

val generateKpmNativeVariants by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.KpmNativeVariantCodegenKt",
    sourceSets["main"]
)

listOf(generateMppTargetContainerWithPresets, generateAbstractBinaryContainer, generateKpmNativeVariants).forEach {
    it.systemProperty(
        "org.jetbrains.kotlin.generators.gradle.dsl.outputSourceRoot",
        project(":kotlin-gradle-plugin").projectDir.resolve("src/main/kotlin").absolutePath
    )
}

