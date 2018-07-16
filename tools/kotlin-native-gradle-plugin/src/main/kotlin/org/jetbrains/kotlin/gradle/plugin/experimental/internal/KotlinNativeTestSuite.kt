package org.jetbrains.kotlin.gradle.plugin.experimental.internal

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
        objectFactory: ObjectFactory,
        fileOperations: FileOperations
) : AbstractKotlinNativeComponent(name, sources, objectFactory, fileOperations),
    KotlinNativeTestComponent {

    init {
        getImplementationDependencies().extendsFrom(testedComponent.getImplementationDependencies())
    }

    override fun getDisplayName(): DisplayName = Describables.withTypeAndName("Kotlin/Native test suite", name)

    private val testBinaryProperty: Property<KotlinNativeTestExecutable> =
            objectFactory.property(KotlinNativeTestExecutable::class.java)

    override fun getTestBinary() = testBinaryProperty

    fun addTestExecutable(identity: NativeVariantIdentity): KotlinNativeTestExecutable =
            objectFactory.newInstance(
                    KotlinNativeTestExecutableImpl::class.java,
                    "$name${identity.name.capitalize()}",
                    getBaseName(),
                    getImplementationDependencies(),
                    this,
                    testedComponent.sources,
                    identity
            ).apply {
                binaries.add(this)
            }
}
