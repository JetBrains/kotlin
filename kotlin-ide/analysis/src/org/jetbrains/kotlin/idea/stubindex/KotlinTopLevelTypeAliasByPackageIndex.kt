/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.psi.KtTypeAlias

class KotlinTopLevelTypeAliasByPackageIndex : StringStubIndexExtension<KtTypeAlias>() {
    override fun getKey(): StubIndexKey<String, KtTypeAlias> = KEY

    override fun get(s: String, project: Project, scope: GlobalSearchScope): Collection<KtTypeAlias> =
        StubIndex.getElements<String, KtTypeAlias>(
            KEY, s, project,
            scope, KtTypeAlias::class.java
        )

    companion object {
        val KEY = KotlinIndexUtil.createIndexKey(KotlinTopLevelTypeAliasByPackageIndex::class.java)
        val INSTANCE = KotlinTopLevelTypeAliasByPackageIndex()

        @JvmStatic
        fun getInstance() = INSTANCE
    }
}