/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.language.cpp.CppBinary
import org.gradle.nativeplatform.OperatingSystemFamily
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeBinary
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeFramework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

open class KotlinNativeFrameworkImpl @Inject constructor(
    name: String,
    baseName: Provider<String>,
    componentDependencies: KotlinNativeDependenciesImpl,
    component: KotlinNativeMainComponent,
    variant: KotlinNativeVariant,
    objects: ObjectFactory,
    configurations: ConfigurationContainer,
    fileOperations: FileOperations
) : AbstractKotlinNativeBinary(
    name,
    baseName,
    component,
    variant,
    CompilerOutputKind.FRAMEWORK,
    objects,
    componentDependencies,
    configurations,
    fileOperations
), KotlinNativeFramework {

    override val outputRootName: String = "lib"

    // A configuration containing exported klibs.
    override val export = configurations.create(names.withPrefix("export")).apply {
        isCanBeConsumed = false
        isTransitive = component.dependencies.transitiveExport
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinUsages.KOTLIN_API))
        attributes.attribute(CppBinary.DEBUGGABLE_ATTRIBUTE, debuggable)
        attributes.attribute(CppBinary.OPTIMIZED_ATTRIBUTE, optimized)
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        attributes.attribute(KotlinNativeBinary.KONAN_TARGET_ATTRIBUTE, konanTarget.name)
        attributes.attribute(KotlinNativeBinary.OLD_KONAN_TARGET_ATTRIBUTE, konanTarget.name)
        attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, konanTarget.getGradleOSFamily(objects))
        extendsFrom(componentDependencies.exportDependencies)
        getImplementationDependencies().extendsFrom(this)
    }

    override var embedBitcode: BitcodeEmbeddingMode =
        if (konanTarget == KonanTarget.IOS_ARM64 || konanTarget == KonanTarget.IOS_ARM32) {
            buildType.iosEmbedBitcode
        } else {
            BitcodeEmbeddingMode.DISABLE
        }
}