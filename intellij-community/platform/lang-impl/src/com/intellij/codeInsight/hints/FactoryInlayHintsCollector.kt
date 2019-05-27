// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl

abstract class FactoryInlayHintsCollector(editor: Editor) : InlayHintsCollector {
  val factory = PresentationFactory(editor as EditorImpl)
}