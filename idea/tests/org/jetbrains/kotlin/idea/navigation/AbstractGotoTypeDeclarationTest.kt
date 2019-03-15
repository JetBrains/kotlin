/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigation

import com.intellij.openapi.actionSystem.IdeActions

abstract class AbstractGotoTypeDeclarationTest : AbstractGotoActionTest() {
    override val actionName: String
        get() = IdeActions.ACTION_GOTO_TYPE_DECLARATION
}
