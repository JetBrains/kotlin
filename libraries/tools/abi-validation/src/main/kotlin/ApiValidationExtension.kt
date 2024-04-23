/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.klib.KlibSignatureVersion
import org.gradle.api.Action

public open class ApiValidationExtension {

    /**
     * Disables API validation checks completely.
     */
    public var validationDisabled: Boolean = false

    /**
     * Fully qualified package names that are not consider public API.
     * For example, it could be `kotlinx.coroutines.internal` or `kotlinx.serialization.implementation`.
     */
    public var ignoredPackages: MutableSet<String> = HashSet()

    /**
     * Projects that are ignored by the API check.
     */
    public var ignoredProjects: MutableSet<String> = HashSet()

    /**
     * Fully qualified names of annotations that effectively exclude declarations from being public.
     * Example of such annotation could be `kotlinx.coroutines.InternalCoroutinesApi`.
     */
    public var nonPublicMarkers: MutableSet<String> = HashSet()

    /**
     * Fully qualified names of classes that are ignored by the API check.
     * Example of such a class could be `com.package.android.BuildConfig`.
     */
    public var ignoredClasses: MutableSet<String> = HashSet()

    /**
     * Fully qualified names of annotations that can be used to explicitly mark public declarations. 
     * If at least one of [publicMarkers], [publicPackages] or [publicClasses] is defined,
     * all declarations not covered by any of them will be considered non-public. 
     * [ignoredPackages], [ignoredClasses] and [nonPublicMarkers] can be used for additional filtering.
     */
    public var publicMarkers: MutableSet<String> = HashSet()

    /**
     * Fully qualified package names that contain public declarations. 
     * If at least one of [publicMarkers], [publicPackages] or [publicClasses] is defined,
     * all declarations not covered by any of them will be considered non-public. 
     * [ignoredPackages], [ignoredClasses] and [nonPublicMarkers] can be used for additional filtering.
     */
    public var publicPackages: MutableSet<String> = HashSet()

    /**
     * Fully qualified names of public classes.
     * If at least one of [publicMarkers], [publicPackages] or [publicClasses] is defined,
     * all declarations not covered by any of them will be considered non-public.
     * [ignoredPackages], [ignoredClasses] and [nonPublicMarkers] can be used for additional filtering.
     */
    public var publicClasses: MutableSet<String> = HashSet()

    /**
     * Non-default Gradle SourceSet names that should be validated.
     * By default, only the `main` source set is checked.
     */
    public var additionalSourceSets: MutableSet<String> = HashSet()

    /**
     * A path to a directory containing an API dump.
     * The path should be relative to the project's root directory and should resolve to its subdirectory.
     * By default, it's `api`.
     */
    public var apiDumpDirectory: String = "api"

    /**
     * KLib ABI validation settings.
     *
     * @see KlibValidationSettings
     */
    @ExperimentalBCVApi
    public val klib: KlibValidationSettings = KlibValidationSettings()

    /**
     * Configure KLib ABI validation settings.
     */
    @ExperimentalBCVApi
    public fun klib(config: Action<KlibValidationSettings>) {
        config.execute(this.klib)
    }
}

/**
 * Settings affecting KLib ABI validation.
 */
@ExperimentalBCVApi
public open class KlibValidationSettings {
    /**
     * Enables KLib ABI validation checks.
     */
    public var enabled: Boolean = false
    /**
     * Specifies which version of signature KLib ABI dump should contain.
     * By default, or when explicitly set to null, the latest supported version will be used.
     *
     * This option covers some advanced scenarios and does not require any configuration by default.
     *
     * A linker uses signatures to look up symbols, thus signature changes brake binary compatibility and
     * should be tracked. Signature format itself is not stabilized yet and may change in the future. In that case,
     * a new version of a signature will be introduced. Change of a signature version will be reflected in a dump
     * causing a validation failure even if declarations itself remained unchanged.
     * However, if a klib supports multiple signature versions simultaneously, one my explicitly specify the version
     * that will be dumped to prevent changes in a dump file.
     */
    public var signatureVersion: KlibSignatureVersion = KlibSignatureVersion.LATEST
    /**
     * Fail validation if some build targets are not supported by the host compiler.
     * By default, ABI dumped only for supported files will be validated. This option makes validation behavior
     * stricter and treats having unsupported targets as an error.
     */
    public var strictValidation: Boolean = false
}
