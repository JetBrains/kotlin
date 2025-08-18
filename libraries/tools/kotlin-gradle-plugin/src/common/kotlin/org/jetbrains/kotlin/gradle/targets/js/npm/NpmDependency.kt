/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.PACKAGE_JSON
import java.io.File

/**
 * Represents a dependency on an NPM dependency.
 *
 * This class is an internal KGP utility.
 *
 * Implements [FileCollectionDependency] to present to Gradle as a 'no-op' dependency
 * that participates in the dependency graph, but is ignored when resolving artifacts.
 * KGP uses the dependency graph computed by Gradle to create a `package.json` file,
 * and then use npm to resolve the artifacts.
 */
// We plan to investigate a replacement https://youtrack.jetbrains.com/issue/KT-76494
class NpmDependency internal constructor(
    @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.")
    val objectFactory: ObjectFactory,
    internal val emptyFileCollection: FileCollection,
    internal val name: String,
    internal val version: String,
    internal var reason: String? = null,
//    @Suppress("PropertyName")
// TODO when `scope` is removed, rename `_scope` to `scope`
    private val _scope: Scope = Scope.NORMAL,
) : FileCollectionDependency {

    /**
     * Represents the scope of an npm dependency.
     * These scopes determine how the dependency is categorized and used by npm.
     */
    enum class Scope {
        /** Dependencies with this scope are added in `dependencies` in `package.json`. */
        NORMAL,

        /** Dependencies with this scope are added in `devDependencies` in `package.json`. */
        DEV,

        /** Dependencies with this scope are added in `optionalDependencies` in `package.json`. */
        OPTIONAL,

        /** Dependencies with this scope are added in `peerDependencies` in `package.json`. */
        PEER,
    }


    @Deprecated("Internal utility. Scheduled for removal in Kotlin 2.4.")
    constructor(
        objectFactory: ObjectFactory,
        scope: Scope = Scope.NORMAL,
        name: String,
        version: String,
    ) : this(
        objectFactory = objectFactory,
        emptyFileCollection = objectFactory.fileCollection(),
        name = name,
        version = version,
        reason = null,
        _scope = scope,
    )

    /**
     * We don't want Gradle to resolve any artifacts from this dependency.
     * So, this always returns an empty [FileCollection].
     * Instead, npm will be used to download the artifacts.
     */
    override fun getFiles(): FileCollection = emptyFileCollection

    override fun getName(): String = name

    override fun getVersion(): String = version

    /**
     * 'Group' is not valid for npm dependencies.
     * Always returns `null`.
     */
    override fun getGroup(): String? = null

    override fun copy(): Dependency =
        @Suppress("DEPRECATION")
        copy(
            objectFactory = objectFactory,
            scope = _scope,
            name = name,
            version = version,
        )

    override fun getReason(): String? = reason
    override fun because(reason: String?) {
        this.reason = reason
    }

    override fun toString(): String {
        return buildString {
            append("NpmDependency(")
            append(name)
            append(":")
            append(version)
            if (_scope != Scope.NORMAL) {
                append("@")
                append(_scope)
            }
            if (reason != null) {
                append(", reason='$reason'")
            }
            append(")")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NpmDependency) return false

        if (name != other.name) return false
        if (version != other.version) return false
        if (_scope != other._scope) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + _scope.hashCode()
        return result
    }


    //region deprecated fns
    @Deprecated("Deprecated by Gradle. Scheduled for removal in Kotlin 2.4.")
    @Suppress("DeprecatedCallableAddReplaceWith")
    // Originally from SelfResolvingDependencyInternal.
    // Retained for binary compatibility.
    fun getTargetComponentId(): ComponentIdentifier? =
        null

    @Deprecated("Internal implementation detail, do not use. Scheduled for removal in Kotlin 2.4.")
//    @Suppress("DeprecatedCallableAddReplaceWith")
    // Retained for binary compatibility
    val scope: Scope get() = _scope

    @Deprecated("Deprecated by Gradle. Scheduled for removal in Kotlin 2.4.")
    @Suppress("DeprecatedCallableAddReplaceWith")
    override fun getBuildDependencies(): TaskDependency =
        emptyFileCollection.buildDependencies

    @Deprecated("Deprecated by Gradle. Scheduled for removal in Kotlin 2.4.")
    @Suppress("DeprecatedCallableAddReplaceWith")
    override fun resolve(): MutableSet<File> = mutableSetOf()

    @Deprecated("Deprecated by Gradle. Scheduled for removal in Kotlin 2.4.")
    @Suppress("DeprecatedCallableAddReplaceWith")
    override fun resolve(p0: Boolean): MutableSet<File> =
        @Suppress("DEPRECATION")
        resolve()

    @Deprecated("Deprecated by Gradle. Scheduled for removal in Kotlin 2.4.")
    @Suppress("DeprecatedCallableAddReplaceWith")
    override fun contentEquals(dependency: Dependency): Boolean =
        false
    //endregion


    //region data class fns
    @Deprecated("Internal implementation detail, do not use. Scheduled for removal in Kotlin 2.4.")
    @Suppress("DeprecatedCallableAddReplaceWith")
    fun copy(
        @Suppress("DEPRECATION")
        objectFactory: ObjectFactory = this@NpmDependency.objectFactory,
        scope: Scope = this@NpmDependency._scope,
        name: String = this@NpmDependency.name,
        version: String = this@NpmDependency.version,
    ): NpmDependency {
        return NpmDependency(
            objectFactory = objectFactory,
            emptyFileCollection = emptyFileCollection,
            name = name,
            version = version,
            reason = reason,
            _scope = scope,
        )
    }

    @Deprecated("Internal implementation detail, do not use. Scheduled for removal in Kotlin 2.4.")
    @Suppress("DeprecatedCallableAddReplaceWith")
    operator fun component1(): ObjectFactory =
        @Suppress("DEPRECATION")
        objectFactory

    @Deprecated("Internal implementation detail, do not use. Scheduled for removal in Kotlin 2.4.")
//    @Suppress("DeprecatedCallableAddReplaceWith")
    operator fun component2(): Scope = _scope
    //endregion

    companion object {
        internal fun NpmDependency.toDeclaration(): NpmDependencyDeclaration =
            NpmDependencyDeclaration(
                scope = this._scope,
                name = this.name,
                version = this.version,
            )
    }
}


internal fun directoryNpmDependency(
    objectFactory: ObjectFactory,
    scope: NpmDependency.Scope,
    name: String,
    directory: File,
): NpmDependency {
    check(directory.isDirectory) {
        "Dependency on local path should point on directory but $directory found"
    }

    return NpmDependency(
        objectFactory = objectFactory,
        emptyFileCollection = objectFactory.fileCollection(),
        _scope = scope,
        name = name,
        version = fileVersion(directory),
    )
}

internal fun fileVersion(directory: File): String =
    "$FILE_VERSION_PREFIX_${directory.absolutePath}"

internal fun moduleName(directory: File): String {
    val packageJson = directory.resolve(PACKAGE_JSON)

    if (packageJson.isFile) {
        return fromSrcPackageJson(packageJson)!!.name
    }

    return directory.name
}

internal const val FILE_VERSION_PREFIX_ = "file:"

//region deprecated utils
@Deprecated("Internal utility, no longer used. Scheduled for removal in Kotlin 2.4.", ReplaceWith("startsWith(\"file:\")"))
fun String.isFileVersion() =
    startsWith(FILE_VERSION_PREFIX_)

@Deprecated("Internal utility, no longer used. Scheduled for removal in Kotlin 2.4.")
@Suppress("unused")
const val FILE_VERSION_PREFIX = "file:"
//endregion
