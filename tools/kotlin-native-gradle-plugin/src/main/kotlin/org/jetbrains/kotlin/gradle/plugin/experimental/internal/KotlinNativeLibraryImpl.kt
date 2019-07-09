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

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.component.PublishableComponent
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.language.cpp.internal.DefaultUsageContext
import org.gradle.nativeplatform.Linkage
import org.gradle.nativeplatform.TargetMachine
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeLibrary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import javax.inject.Inject

open class KotlinNativeLibraryImpl @Inject constructor(
        name: String,
        baseName: Provider<String>,
        componentDependencies: KotlinNativeDependenciesImpl,
        component: KotlinNativeMainComponent,
        variant: KotlinNativeVariant,
        projectLayout: ProjectLayout,
        objects: ObjectFactory,
        configurations: ConfigurationContainer,
        fileOperations: FileOperations
) : AbstractKotlinNativeBinary(name,
        baseName,
        component,
        variant,
        CompilerOutputKind.LIBRARY,
        objects,
        componentDependencies,
        configurations,
        fileOperations),
    KotlinNativeLibrary,
    SoftwareComponentInternal, // TODO: SoftwareComponentInternal will be replaced with ComponentWithVariants by Gradle
    PublishableComponent
{
    override fun getCoordinates(): ModuleVersionIdentifier = identity.coordinates

    // Properties

    // The link elements configuration is created by the NativeBase plugin.
    private val linkElementsProperty: Property<Configuration> = objects.property(Configuration::class.java)
    private val linkFileProperty: RegularFileProperty = projectLayout.fileProperty()

    // Interface

    override fun getLinkElements()  = linkElementsProperty
    override fun getLinkFile() = linkFileProperty

    override fun getUsages(): Set<UsageContext> = linkElementsProperty.get().let {
        setOf(DefaultUsageContext(identity.linkUsageContext, it.allArtifacts, it))
    }

    override fun getLinkAttributes(): AttributeContainer = identity.linkUsageContext.attributes

    // TODO: Does Klib really match a static linkage in Gradle's terms?
    override fun getLinkage(): Linkage? = Linkage.STATIC

    override val outputRootName = "lib"

    override fun getTargetMachine(): TargetMachine = konanTarget.toTargetMachine(objects)
}
