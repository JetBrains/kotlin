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
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.internal.Describables
import org.gradle.internal.DisplayName
import org.gradle.language.cpp.internal.NativeVariantIdentity
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeTestComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeTestExecutable
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSetImpl
import javax.inject.Inject

open class KotlinNativeTestSuite @Inject constructor(
    name: String,
    sources: KotlinNativeSourceSetImpl,
    override val testedComponent: KotlinNativeComponent,
    project: Project,
    objectFactory: ObjectFactory,
    fileOperations: FileOperations
) : AbstractKotlinNativeComponent(name, sources, project, objectFactory, fileOperations),
    KotlinNativeTestComponent {

    init {
        getImplementationDependencies().extendsFrom(testedComponent.getImplementationDependencies())
        konanTargets.set(project.provider { testedComponent.konanTargets.get() })
    }

    override fun getDisplayName(): DisplayName = Describables.withTypeAndName("Kotlin/Native test suite", name)

    private val testBinaryProperty: Property<KotlinNativeTestExecutable> =
            objectFactory.property(KotlinNativeTestExecutable::class.java)

    override fun getTestBinary() = testBinaryProperty

    fun addTestExecutable(variant: KotlinNativeVariant): KotlinNativeTestExecutable =
            objectFactory.newInstance(
                    KotlinNativeTestExecutableImpl::class.java,
                    "$name${variant.identity.name.capitalize()}",
                    getBaseName(),
                    dependencies,
                    this,
                    testedComponent.sources,
                    variant
            ).apply {
                binaries.add(this)
            }
}
