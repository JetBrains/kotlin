package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.internal.provider.LockableSetProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.language.internal.DefaultBinaryCollection
import org.gradle.language.internal.DefaultComponentDependencies
import org.gradle.language.nativeplatform.internal.ComponentWithNames
import org.gradle.language.nativeplatform.internal.DefaultNativeComponent
import org.gradle.language.nativeplatform.internal.Names
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeBinary
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSetImpl
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

abstract class AbstractKotlinNativeComponent @Inject constructor(
        private val name: String,
        override val sources: KotlinNativeSourceSetImpl,
        val objectFactory: ObjectFactory,
        fileOperations: FileOperations
) : DefaultNativeComponent(fileOperations),
    KotlinNativeComponent,
    ComponentWithNames {

    private val baseName: Property<String> = objectFactory.property(String::class.java).apply { set(name) }
    fun getBaseName(): Property<String> = baseName

    override val konanTargets: LockableSetProperty<KonanTarget> =
            LockableSetProperty(objectFactory.setProperty(KonanTarget::class.java)).apply {
                set(mutableSetOf(HostManager.host))
            }

    @Suppress("UNCHECKED_CAST")
    private val binaries = objectFactory.newInstance(DefaultBinaryCollection::class.java, KotlinNativeBinary::class.java)
            as DefaultBinaryCollection<KotlinNativeBinary>
    override fun getBinaries(): DefaultBinaryCollection<KotlinNativeBinary> = binaries

    override fun getName(): String = name

    private val names = Names.of(name)
    override fun getNames(): Names = names

    private val dependencies: DefaultComponentDependencies = objectFactory.newInstance(
            DefaultComponentDependencies::class.java,
            names.withSuffix("implementation"))

    override fun getDependencies() = dependencies

    override fun getImplementationDependencies(): Configuration = dependencies.implementationDependencies

    // region DSL

    override fun target(vararg targets: String) {
        val hostManager = HostManager()
        konanTargets.set(targets.map { hostManager.targetByName(it) })
    }

    override val extraOpts = mutableListOf<String>()

    override fun extraOpts(vararg values: Any) = extraOpts(values.toList())
    override fun extraOpts(values: List<Any>) {
        extraOpts.addAll(values.map { it.toString() })
    }

    // endregion
}