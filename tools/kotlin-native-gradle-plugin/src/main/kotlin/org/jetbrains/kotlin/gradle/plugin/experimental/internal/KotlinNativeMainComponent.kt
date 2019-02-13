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

import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.internal.Describables
import org.gradle.internal.DisplayName
import org.gradle.language.ProductionComponent
import org.gradle.language.nativeplatform.internal.PublicationAwareComponent
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeBinary
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSetImpl
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import javax.inject.Inject

open class KotlinNativeMainComponent @Inject constructor(
        name: String,
        sources: KotlinNativeSourceSetImpl,
        project: Project,
        objectFactory: ObjectFactory,
        fileOperations: FileOperations
) : AbstractKotlinNativeComponent(name, sources, project, objectFactory, fileOperations),
    PublicationAwareComponent,
    ProductionComponent {

    override fun getDisplayName(): DisplayName = Describables.withTypeAndName("Kotlin/Native component", name)

    val outputKinds: SetProperty<OutputKind> = objectFactory.setProperty(OutputKind::class.java).apply {
        set(mutableSetOf(OutputKind.KLIBRARY))
    }

    private val metadataDependencies = project.configurations.create(names.withPrefix("metadata")).apply {
        isCanBeConsumed = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, KotlinUsages.KOTLIN_API))
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
        extendsFrom(getImplementationDependencies())
    }

    private val mainPublication = KotlinNativePublicationVariants()

    override fun getMainPublication(): KotlinNativePublicationVariants = mainPublication

    private val developmentBinaryProperty: Property<KotlinNativeBinary> =
            objectFactory.property(KotlinNativeBinary::class.java)

    override fun getDevelopmentBinary() = developmentBinaryProperty

    private fun <T : KotlinNativeBinary> addBinary(type: Class<T>, variant: KotlinNativeVariant): T {
        val name = "$name${variant.identity.name.capitalize()}"
        return objectFactory.newInstance(type, name, baseName, dependencies, this, variant).apply {
            binaries.add(this)
        }
    }

    private inline fun <reified T : KotlinNativeBinary> addBinary(variant: KotlinNativeVariant): T =
            addBinary(T::class.java, variant)

    fun addExecutable(variant: KotlinNativeVariant) = addBinary<KotlinNativeExecutableImpl>(variant)
    fun addLibrary(variant: KotlinNativeVariant) = addBinary<KotlinNativeLibraryImpl>(variant)
    fun addFramework(variant: KotlinNativeVariant) = addBinary<KotlinNativeFrameworkImpl>(variant)

    fun addBinary(kind: OutputKind, variant: KotlinNativeVariant) = addBinary(kind.binaryClass, variant)

    // region Kotlin/Native component passed to Gradle to determine publication variants
    // TODO: SoftwareComponentInternal will be replaced with ComponentWithVariants by Gradle
    inner class KotlinNativePublicationVariants: ComponentWithVariants, SoftwareComponentInternal {

        private val variants = WrapUtil.toDomainObjectSet(SoftwareComponent::class.java)
        override fun getVariants(): MutableSet<SoftwareComponent> = variants

        override fun getName(): String = this@KotlinNativeMainComponent.name

        override fun getUsages(): Set<UsageContext> =
            setOf(MetadataUsageContext("metadata-api", project.objects, metadataDependencies))
    }
    // endregion

    companion object {
        @JvmStatic val EXECUTABLE = OutputKind.EXECUTABLE
        @JvmStatic val KLIBRARY = OutputKind.KLIBRARY
        @JvmStatic val FRAMEWORK = OutputKind.FRAMEWORK
        @JvmStatic val DYNAMIC = OutputKind.DYNAMIC
        @JvmStatic val STATIC = OutputKind.STATIC
    }
}
