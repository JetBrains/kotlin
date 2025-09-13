/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

///**
// * Single source of truth for dependencies of a Kotlin source set.
// */
//internal suspend fun KotlinSourceSet.allSourceSetDependencies(): List<KotlinSourceSetDependency> {
//    return listOfNotNull(
////    gradleConfigurationDependencies(),
////        nativeStdlibDependency(),
////        transformedMetadataDependencies(),
////        cinteropDependencies(),
//    ).flatten()
//    // types of KotlinSourceSets:
//    // 1. Common
//    // 2. Common Native
//    // 3. Native platform
//    // 4. JVM platform
//    // 5. Android platform
//    // 6. JS platform
//    // 8. Wasm platform
//}

//private suspend fun KotlinSourceSet.nonPublishableSourceSetMetadataDependencies(): List<KotlinSourceSetDependency> {
//    val platformCompilations = internal.awaitPlatformCompilations()
//    if (!platformCompilations.any { compilation -> !compilation.isMain() }) return emptyList()
//
//}
//
//internal sealed class KotlinSourceSetDependency {
//    data class ResolvedDependency(val resolvedArtifact: ResolvedArtifact) : KotlinSourceSetDependency()
//    data class KotlinNativeBundleStdlib(val provider: Provider<File>) : KotlinSourceSetDependency()
//    data class TransformedMetadataKlibDependency(val provider: Provider<File>): KotlinSourceSetDependency()
//}