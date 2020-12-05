import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask

plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    jcenter()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
}

kotlin {
    sourceSets["commonMain"].dependencies {
        implementation(kotlin("stdlib-common"))
    }

    iosArm32()
    iosArm64()
    iosX64()

    targets.withType(KotlinNativeTarget::class.java) {
        binaries.framework(listOf(DEBUG))
    }

}

val frameworksToMerge = kotlin.targets
    .withType(KotlinNativeTarget::class.java)
    .map { it.binaries.getFramework("DEBUG") }

val fat = tasks.create("fat", FatFrameworkTask::class.java) {
    from(frameworksToMerge)
}
