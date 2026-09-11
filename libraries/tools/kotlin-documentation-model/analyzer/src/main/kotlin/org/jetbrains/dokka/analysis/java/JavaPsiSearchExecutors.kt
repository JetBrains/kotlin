/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java

import com.intellij.openapi.Disposable
import com.intellij.psi.impl.search.MethodSuperSearcher
import com.intellij.psi.search.searches.SuperMethodsSearch
import org.jetbrains.dokka.InternalDokkaApi

/**
 * Registers the executors of the PSI search extension points that Dokka needs but the IntelliJ platform
 * artifacts Dokka depends on no longer register themselves.
 *
 * Must be called once the application environment exists (i.e. after the analysis session has been built)
 * and before any Java documentation is parsed. [disposable] ties the registration to that environment.
 *
 * Currently only `com.intellij.superMethodsSearch` needs this: [SuperMethodsSearch] is what
 * `PsiMethod.findSuperMethods()` delegates to, and since platform 261 its only executor
 * ([MethodSuperSearcher]) lives in `java-indexing-impl`, which Dokka does not depend on. Without an
 * executor the search yields nothing and `findSuperMethods()` silently returns an empty array, breaking
 * Java documentation inheritance.
 */
@InternalDokkaApi
public fun registerJavaPsiSearchExecutors(disposable: Disposable) {
    val superMethodsSearch = SuperMethodsSearch.EP_NAME.point
    if (superMethodsSearch.extensionList.none { it is MethodSuperSearcher }) {
        superMethodsSearch.registerExtension(MethodSuperSearcher(), disposable)
    }
}
