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

class KotlinTypeAliasByExpansionShortNameIndex : StringStubIndexExtension<KtTypeAlias>() {
    override fun getKey(): StubIndexKey<String, KtTypeAlias> = KEY

    override fun get(key: String, project: Project, scope: GlobalSearchScope) =
        StubIndex.getElements(KEY, key, project, scope, KtTypeAlias::class.java)

    companion object {
        val KEY = KotlinIndexUtil.createIndexKey(KotlinTypeAliasByExpansionShortNameIndex::class.java)
        val INSTANCE = KotlinTypeAliasByExpansionShortNameIndex()

        @JvmStatic
        fun getInstance() = INSTANCE
    }
}