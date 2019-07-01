/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.*
import org.gradle.api.plugins.ExtensionAware
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import javax.inject.Inject

/*
Use the following naming scheme:
    executable('foo', [debug, release]) -> fooDebugExecutable + fooReleaseExecutable
    executable() -> debugExecutable, releaseExecutable
    executable([debug]) -> debugExecutable
*/

open class KotlinNativeBinaryContainer @Inject constructor(
    override val target: KotlinNativeTarget,
    backingContainer: DomainObjectSet<NativeBinary>
) : AbstractKotlinNativeBinaryContainer(),
    DomainObjectSet<NativeBinary> by backingContainer
{
    final override val project: Project
        get() = target.project

    private val defaultCompilation: KotlinNativeCompilation
        get() = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    private val defaultTestCompilation: KotlinNativeCompilation
        get() = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)

    private val nameToBinary = mutableMapOf<String, NativeBinary>()
    internal val prefixGroups: NamedDomainObjectSet<PrefixGroup> = project.container(PrefixGroup::class.java)

    // region DSL getters.
    private inline fun <reified T : NativeBinary> getBinary(
        namePrefix: String,
        buildType: NativeBuildType,
        outputKind: NativeOutputKind
    ): T {
        val classifier = outputKind.taskNameClassifier
        val name = generateBinaryName(namePrefix, buildType, classifier)
        val binary = getByName(name)
        require(binary is T && binary.buildType == buildType) {
            "Binary $name has incorrect outputKind or build type.\n" +
                    "Expected: ${buildType.getName()} $classifier. Actual: ${binary.buildType.getName()} ${binary.outputKind.taskNameClassifier}."
        }
        return binary as T
    }

    private inline fun <reified T : NativeBinary> findBinary(
        namePrefix: String,
        buildType: NativeBuildType,
        outputKind: NativeOutputKind
    ): T? {
        val classifier = outputKind.taskNameClassifier
        val name = generateBinaryName(namePrefix, buildType, classifier)
        val binary = findByName(name)
        return if (binary is T && binary.buildType == buildType) {
            binary
        } else {
            null
        }
    }

    override fun getByName(name: String): NativeBinary = nameToBinary.getValue(name)
    override fun findByName(name: String): NativeBinary? = nameToBinary[name]

    private fun checkDeprecatedTestAccess(namePrefix: String, buildType: NativeBuildType, warning: String) {
        if (namePrefix == DEFAULT_TEST_NAME_PREFIX && buildType == DEFAULT_TEST_BUILD_TYPE) {
            project.logger.warn(warning)
        }
    }

    override fun getExecutable(namePrefix: String, buildType: NativeBuildType): Executable {
        checkDeprecatedTestAccess(namePrefix, buildType, GET_TEST_DEPRECATION_WARNING)
        return getBinary(namePrefix, buildType, NativeOutputKind.EXECUTABLE)
    }

    override fun getStaticLib(namePrefix: String, buildType: NativeBuildType): StaticLibrary =
        getBinary(namePrefix, buildType, NativeOutputKind.STATIC)

    override fun getSharedLib(namePrefix: String, buildType: NativeBuildType): SharedLibrary =
        getBinary(namePrefix, buildType, NativeOutputKind.DYNAMIC)

    override fun getFramework(namePrefix: String, buildType: NativeBuildType): Framework =
        getBinary(namePrefix, buildType, NativeOutputKind.FRAMEWORK)

    override fun getTest(namePrefix: String, buildType: NativeBuildType): TestExecutable =
        getBinary(namePrefix, buildType, NativeOutputKind.TEST)

    override fun findExecutable(namePrefix: String, buildType: NativeBuildType): Executable? {
        checkDeprecatedTestAccess(namePrefix, buildType, FIND_TEST_DEPRECATED_WARNING)
        return findBinary(namePrefix, buildType, NativeOutputKind.EXECUTABLE)
    }

    override fun findStaticLib(namePrefix: String, buildType: NativeBuildType): StaticLibrary? =
        findBinary(namePrefix, buildType, NativeOutputKind.STATIC)

    override fun findSharedLib(namePrefix: String, buildType: NativeBuildType): SharedLibrary? =
        findBinary(namePrefix, buildType, NativeOutputKind.DYNAMIC)

    override fun findFramework(namePrefix: String, buildType: NativeBuildType): Framework? =
        findBinary(namePrefix, buildType, NativeOutputKind.FRAMEWORK)

    override fun findTest(namePrefix: String, buildType: NativeBuildType): TestExecutable? =
        findBinary(namePrefix, buildType, NativeOutputKind.TEST)
    // endregion.

    // region Factories
    override fun <T : NativeBinary> createBinaries(
        namePrefix: String,
        baseName: String,
        outputKind: NativeOutputKind,
        buildTypes: Collection<NativeBuildType>,
        create: (name: String, baseName: String, buildType: NativeBuildType, compilation: KotlinNativeCompilation) -> T,
        configure: T.() -> Unit
    ) {
        val prefixGroup = prefixGroups.findByName(namePrefix) ?: PrefixGroup(namePrefix).also {
            prefixGroups.add(it)
        }

        buildTypes.forEach { buildType ->
            val name = generateBinaryName(namePrefix, buildType, outputKind.taskNameClassifier)

            require(name !in nameToBinary) {
                "Cannot create binary $name: binary with such a name already exists"
            }

            require(outputKind.availableFor(target.konanTarget)) {
                "Cannot create ${outputKind.description}: $name. Binaries of this kind are not available for target ${target.name}"
            }

            val compilation = if (outputKind == NativeOutputKind.TEST) defaultTestCompilation else defaultCompilation
            val binary = create(name, baseName, buildType, compilation)
            add(binary)
            prefixGroup.binaries.add(binary)
            nameToBinary[binary.name] = binary
            // Allow accessing binaries as properties of the container in Groovy DSL.
            if (this is ExtensionAware) {
                extensions.add(binary.name, binary)
            }
            binary.configure()
        }
    }

    companion object {
        internal val DEFAULT_TEST_BUILD_TYPE = NativeBuildType.DEBUG
        internal val DEFAULT_TEST_NAME_PREFIX = "test"

        internal fun generateBinaryName(prefix: String, buildType: NativeBuildType, outputKindClassifier: String) =
            lowerCamelCaseName(prefix, buildType.getName(), outputKindClassifier)

        // TODO: Remove in 1.3.50.
        private val GET_TEST_DEPRECATION_WARNING = """
            |
            |Probably you are accessing the default test binary using the 'binaries.getExecutable("$DEFAULT_TEST_NAME_PREFIX", ${DEFAULT_TEST_BUILD_TYPE.name})' method.
            |Since 1.3.40 tests are represented by a separate binary type. To get the default test binary, use:
            |
            |    binaries.getTest("DEBUG")
            |
            """.trimMargin()

        private val FIND_TEST_DEPRECATED_WARNING = """
            |
            |Probably you are accessing the default test binary using the 'binaries.findExecutable("$DEFAULT_TEST_NAME_PREFIX", ${DEFAULT_TEST_BUILD_TYPE.name})' method.
            |Since 1.3.40 tests are represented by a separate binary type. To get the default test binary, use:
            |
            |    binaries.findTest("DEBUG")
            |
            """.trimMargin()
    }
    // endregion.

    internal inner class PrefixGroup(
        private val name: String
    ) : Named {
        override fun getName(): String = name
        val binaries: DomainObjectSet<NativeBinary> = WrapUtil.toDomainObjectSet(NativeBinary::class.java)

        val linkTaskName: String
            get() = lowerCamelCaseName("link", name, target.targetName)
    }
}
