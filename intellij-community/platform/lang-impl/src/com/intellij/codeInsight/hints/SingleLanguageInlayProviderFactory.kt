// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.openapi.project.Project


class SingleLanguageInlayProviderFactory : InlayHintsProviderFactory {
  override fun getProvidersInfo(project: Project): List<ProviderInfo<out Any>> = InlayHintsProviderExtension.findProviders()
}