/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "KotlinRefactoringSettings", storages = arrayOf(Storage("kotlinRefactoring.xml")))
class KotlinRefactoringSettings : PersistentStateComponent<KotlinRefactoringSettings> {
    @JvmField
    var MOVE_TO_UPPER_LEVEL_SEARCH_IN_COMMENTS = false

    @JvmField
    var MOVE_TO_UPPER_LEVEL_SEARCH_FOR_TEXT = false

    @JvmField
    var RENAME_SEARCH_IN_COMMENTS_FOR_PACKAGE = false

    @JvmField
    var RENAME_SEARCH_IN_COMMENTS_FOR_CLASS = false

    @JvmField
    var RENAME_SEARCH_IN_COMMENTS_FOR_METHOD = false

    @JvmField
    var RENAME_SEARCH_IN_COMMENTS_FOR_FIELD = false

    @JvmField
    var RENAME_SEARCH_IN_COMMENTS_FOR_VARIABLE = false

    @JvmField
    var RENAME_SEARCH_FOR_TEXT_FOR_PACKAGE = false

    @JvmField
    var RENAME_SEARCH_FOR_TEXT_FOR_CLASS = false

    @JvmField
    var RENAME_SEARCH_FOR_TEXT_FOR_METHOD = false

    @JvmField
    var RENAME_SEARCH_FOR_TEXT_FOR_FIELD = false

    @JvmField
    var RENAME_SEARCH_FOR_TEXT_FOR_VARIABLE = false

    @JvmField
    var MOVE_PREVIEW_USAGES = true

    @JvmField
    var MOVE_SEARCH_IN_COMMENTS = true

    @JvmField
    var MOVE_SEARCH_FOR_TEXT = true

    @JvmField
    var MOVE_DELETE_EMPTY_SOURCE_FILES = true

    @JvmField
    var MOVE_MPP_DECLARATIONS = true

    @JvmField
    var EXTRACT_INTERFACE_JAVADOC: Int = 0

    @JvmField
    var EXTRACT_SUPERCLASS_JAVADOC: Int = 0

    @JvmField
    var PULL_UP_MEMBERS_JAVADOC: Int = 0

    @JvmField
    var PUSH_DOWN_PREVIEW_USAGES: Boolean = false

    @JvmField
    var INLINE_METHOD_THIS: Boolean = false

    @JvmField
    var INLINE_LOCAL_THIS: Boolean = false

    @JvmField
    var INLINE_TYPE_ALIAS_THIS: Boolean = false

    var renameInheritors = true
    var renameParameterInHierarchy = true
    var renameVariables = true
    var renameTests = true
    var renameOverloads = true

    override fun getState() = this

    override fun loadState(state: KotlinRefactoringSettings) = XmlSerializerUtil.copyBean(state, this)

    companion object {
        @JvmStatic
        val instance: KotlinRefactoringSettings
            get() = ServiceManager.getService(KotlinRefactoringSettings::class.java)
    }
}