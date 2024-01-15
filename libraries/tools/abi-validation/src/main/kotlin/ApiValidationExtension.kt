/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

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
}
