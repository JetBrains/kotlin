/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.documentable

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator

/**
 * Helps find external documentables that are not provided by Dokka by default.
 *
 * By default, Dokka parses and makes available only documentables of the declarations found
 * in the user project itself. Meaning, if the project's source code contains a `fun foo()`,
 * it must be returned by [SourceToDocumentableTranslator]. However, if the user project
 * depends on an external library which has a `fun bar()`, it will __not__ be available and
 * documented out of the box.
 *
 * This provider helps you find documentables for the declarations that are present in
 * [DokkaSourceSet.classpath] during runtime, but are not necessarily from the user project itself.
 *
 * For example, it can help you find a class that comes from a dependency, which can be useful
 * if you want to get more information about a supertype of the project's class,
 * because [DClass.supertypes] only provides the supertype's DRI, but not its full documentable.
 *
 * It is expected to work with all languages supported by the analysis implementation,
 * meaning it should return Java classes if Java as an input language is supported.
 *
 * If you query DRIs of local project declarations (not external), it should generally work, although
 * it's not guaranteed that the returned value will be 100% equal to that provided by Dokka by default.
 *
 * Note: because classpath entries consist of compiled code, the returned documentables may have some
 * properties set to null or empty, because some information cannot be extracted from it in any way.
 * One such example is KDocs, they are present in source code, but not in compiled classes.
 */
public interface ExternalDocumentableProvider {

    /**
     * Returns a valid and fully initialized [DClasslike] if the [dri] points to a class-like
     * declaration (annotation, class, enum, interface, object) that can be found among
     * [DokkaSourceSet.classpath] entries.
     *
     * If the [dri] points to a non-class-like declaration (like a function),
     * or the declaration cannot be found, it returns `null`.
     *
     * Note: the implementation is not expected to cache results or return pre-computed values, so
     * it may need to analyze parts of the project and instantiate new documentables on every invocation.
     * Use this function sparingly, and cache results on your side if you need to.
     */
    public fun getClasslike(dri: DRI, sourceSet: DokkaSourceSet): DClasslike?
}
