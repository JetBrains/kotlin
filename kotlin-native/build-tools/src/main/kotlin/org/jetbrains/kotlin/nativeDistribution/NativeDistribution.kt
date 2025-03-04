/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nativeDistribution

import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.kotlinNativeDist
import java.util.function.BiFunction

/**
 * Describes distribution of Native compiler rooted at [root].
 */
// TODO: Make an inline class after groovy buildscripts are gone.
class NativeDistribution(val root: Directory) {
    /**
     * Directory with compiler jars.
     *
     * @see compilerClasspath
     */
    val compilerJars: Directory
        get() = root.dir("konan/lib")

    /**
     * Directory with CLI tools.
     *
     * @see cinterop
     */
    val bin: Directory
        get() = root.dir("bin")

    /**
     * Directory with libraries for the compiler itself (e.g. bridges for LLVM bindings).
     */
    val nativeLibs: Directory
        get() = root.dir("konan/nativelib")

    /**
     * Root directory for compiler caches.
     *
     * @see cache
     * @see stdlibCache
     */
    val cachesRoot: Directory
        get() = root.dir("klib/cache")

    /**
     * Directory with distribution sources.
     *
     * @see stdlibSources
     */
    val sources: Directory
        get() = root.dir("sources")

    /**
     * Directory with .def files for platform libs.
     */
    val platformLibsDefinitions: Directory
        get() = root.dir("konan/platformDef")

    /**
     * Directory with misc tools.
     */
    val tools: Directory
        get() = root.dir("tools")

    /**
     * Directory with Swift Export support.
     */
    val swiftExport: Directory
        get() = root.dir("konan/swift_export")

    /**
     * Directory with all platform libs KLIBs for a specific [target].
     */
    fun platformLibs(target: String): Directory = root.dir("klib/platform/$target")

    /**
     * Classpath in which it's possible to run K/N compiler.
     */
    val compilerClasspath: FileCollection
        get() = compilerJars.asFileTree.matching {
            include("kotlin-native-compiler-embeddable.jar")
        }

    /**
     * Additional platform configuration.
     */
    val konanPlatforms: Directory
        get() = root.dir("konan/platforms")

    /**
     * `konan.properties` file with targets and dependencies descriptions.
     */
    val konanProperties: RegularFile
        get() = root.file("konan/konan.properties")

    private fun bin(tool: String) = if (PlatformInfo.isWindows()) bin.file("$tool.bat") else bin.file(tool)

    /**
     * `run_konan` command line executable.
     */
    val runKonan: RegularFile
        get() = bin("run_konan")

    /**
     * `cinterop` command line executable.
     */
    val cinterop: RegularFile
        get() = bin("cinterop")

    /**
     * `klib` command line executable.
     */
    val klib: RegularFile
        get() = bin("klib")

    /**
     * Runtime files for a specific [target].
     */
    fun runtime(target: String): Directory = root.dir("konan/targets/$target/native")

    /**
     * Platform library [name] klib for a specific [target].
     */
    fun platformLib(name: String, target: String): Directory = platformLibs(target).dir(name)

    /**
     * Static compiler cache of library [name] for a specific [target].
     */
    fun cache(name: String, target: String): Directory = cachesRoot.dir("${target}-gSTATIC/${name}-cache")

    /**
     * Archive with stdlib sources.
     */
    val stdlibSources: RegularFile
        get() = sources.file("kotlin-stdlib-native-sources.zip")

    /**
     * Standard library klib.
     */
    val stdlib: Directory
        get() = root.dir("klib/common/stdlib")

    /**
     * Static compiler cache of standard library for a specific [target].
     */
    fun stdlibCache(target: String): Directory = cache(name = "stdlib", target)
}

/**
 * Represents a configurable [NativeDistribution] location.
 *
 * This [Property] can be used as a task input or as a parameter in [WorkParameters][org.gradle.workers.WorkParameters]:
 * it's compatible with Gradle serialization (serializes into the root directory of the distribution).
 *
 * Can be created with [ObjectFactory.nativeDistributionProperty].
 */
// `DirectoryProperty` is magically serializable by Gradle (not via java serialization), while `Directory` is not serializable at all.
// So, it's essential to have a separate `NativeDistributionProperty`, because regular `Property<NativeDistribution>` wouldn't get serialized.
// And it's also essential for `NativeDistributionProperty` to be a `value class` around `DirectoryProperty`, so for java reflection
// (and, consequently, for Gradle serialization) `NativeDistributionProperty` would look exactly like `DirectoryProperty`.
@JvmInline
value class NativeDistributionProperty internal constructor(private val directoryProperty: DirectoryProperty) : Property<NativeDistribution> {
    private inline fun <T : Any?> fwd(f: DirectoryProperty.() -> T) = directoryProperty.f()
    private inline fun <T : Any?> fwdNullable(value: NativeDistribution?, f: DirectoryProperty.(Directory?) -> T) = directoryProperty.f(value?.root)

    private inline fun <T : Any?> fwd(value: NativeDistribution, f: DirectoryProperty.(Directory) -> T) = directoryProperty.f(value.root)
    private inline fun <T : Any?> fwd(provider: Provider<out NativeDistribution?>, f: DirectoryProperty.(Provider<out Directory?>) -> T) = directoryProperty.f(provider.map { it.root })

    private fun ret(value: Directory): NativeDistribution = NativeDistribution(value)
    private fun retNullable(value: Directory?): NativeDistribution? = value?.let(::NativeDistribution)
    private fun ret(provider: Provider<Directory>): Provider<NativeDistribution> = provider.map(::NativeDistribution)
    private fun ret(property: Property<Directory>): Property<NativeDistribution> = NativeDistributionProperty(property as DirectoryProperty)
    private fun ret(property: DirectoryProperty): NativeDistributionProperty = NativeDistributionProperty(property)

    override fun set(value: NativeDistribution?) = fwdNullable(value, DirectoryProperty::set)
    override fun set(provider: Provider<out NativeDistribution?>) = fwd(provider, DirectoryProperty::set)
    override fun value(value: NativeDistribution?) = ret(fwdNullable(value, DirectoryProperty::value))
    override fun value(provider: Provider<out NativeDistribution?>) = ret(fwd(provider, DirectoryProperty::value))
    override fun unset() = ret(fwd(DirectoryProperty::unset))
    override fun convention(value: NativeDistribution?) = ret(fwdNullable(value, DirectoryProperty::convention))
    override fun convention(provider: Provider<out NativeDistribution?>) = ret(fwd(provider, DirectoryProperty::convention))
    override fun unsetConvention() = ret(fwd(DirectoryProperty::unsetConvention))
    override fun finalizeValue() = fwd(DirectoryProperty::finalizeValue)
    override fun get() = ret(fwd(DirectoryProperty::get))
    override fun getOrNull() = retNullable(fwd(DirectoryProperty::getOrNull))
    override fun getOrElse(defaultValue: NativeDistribution) = ret(fwd(defaultValue, DirectoryProperty::getOrElse))
    override fun isPresent(): Boolean = fwd(DirectoryProperty::isPresent)
    override fun orElse(value: NativeDistribution) = ret(fwd(value, DirectoryProperty::orElse))
    override fun orElse(provider: Provider<out NativeDistribution?>) = ret(fwd(provider, DirectoryProperty::orElse))
    override fun finalizeValueOnRead() = fwd(DirectoryProperty::finalizeValueOnRead)
    override fun disallowChanges() = fwd(DirectoryProperty::disallowChanges)
    override fun disallowUnsafeRead() = fwd(DirectoryProperty::disallowUnsafeRead)

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun forUseAtConfigurationTime() = ret(fwd(DirectoryProperty::forUseAtConfigurationTime))

    override fun <S : Any?> map(transformer: Transformer<out S?, in NativeDistribution>): Provider<S> = directoryProperty.map {
        transformer.transform(NativeDistribution(it))
    }

    override fun filter(spec: Spec<in NativeDistribution>): Provider<NativeDistribution> = ret(directoryProperty.filter { spec.isSatisfiedBy(NativeDistribution(it)) })

    override fun <S : Any?> flatMap(transformer: Transformer<out Provider<out S?>?, in NativeDistribution>): Provider<S> = directoryProperty.flatMap {
        transformer.transform(NativeDistribution(it))
    }

    override fun <U : Any?, R : Any?> zip(right: Provider<U?>, combiner: BiFunction<in NativeDistribution, in U, out R?>): Provider<R> = directoryProperty.zip<U, R>(right) { lhs, rhs -> combiner.apply(ret(lhs), rhs) }
}

/**
 * Creates a new [NativeDistributionProperty]. The property has no initial value.
 */
fun ObjectFactory.nativeDistributionProperty() = NativeDistributionProperty(directoryProperty())

/**
 * Get the default Native distribution location.
 *
 * There's no guarantee what's inside this distribution.
 * Use together with `dependsOn(":kotlin-native:<required-kind-of-distribution>").
 */
// TODO: Having tasks depend on this distribution is both errorprone
//       and sometimes incompatible with Gradle isolation mechanisms.
val Project.nativeDistribution: Provider<NativeDistribution>
    get() = layout.dir(provider { kotlinNativeDist }).map { NativeDistribution(it) }
