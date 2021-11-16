package org.jetbrains.kotlin.gradle.android

import com.android.build.gradle.internal.publishing.AndroidArtifacts
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.external.externalTarget
import org.jetbrains.kotlin.gradle.targets.external.ideDependencyConfigurations

fun KotlinMultiplatformExtension.android2() {
    val externalTargetHandle = externalTarget("android", KotlinPlatformType.jvm)
    setupAndroidMain(externalTargetHandle)
    setupAndroidUnitTest(externalTargetHandle)
}


internal fun KotlinSourceSet.setupAndroidArtifactTypeForIde(project: Project) {
    ideDependencyConfigurations(project).forEach { configuration ->
        configuration.attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.PROCESSED_JAR.type)
    }
}
