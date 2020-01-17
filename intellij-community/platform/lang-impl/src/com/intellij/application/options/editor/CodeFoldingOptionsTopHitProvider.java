// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor;

import com.intellij.ide.ui.OptionsSearchTopHitProvider;
import com.intellij.ide.ui.search.OptionDescription;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.BeanConfigurable;
import com.intellij.openapi.options.ConfigurableWithOptionDescriptors;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class CodeFoldingOptionsTopHitProvider implements OptionsSearchTopHitProvider.ApplicationLevelProvider {
  @NotNull
  @Override
  public String getId() {
    return CodeFoldingConfigurable.ID;
  }

  @NotNull
  @Override
  public Collection<OptionDescription> getOptions() {
    String byDefault = ApplicationBundle.message("label.fold.by.default");
    List<OptionDescription> result = new ArrayList<>();
    CodeFoldingOptionsProviderEP.EP_NAME.forEachExtensionSafe(ep -> {
      CodeFoldingOptionsProvider wrapper = ConfigurableWrapper.wrapConfigurable(ep);
      UnnamedConfigurable configurable =
        wrapper instanceof ConfigurableWrapper ? ((ConfigurableWrapper)wrapper).getConfigurable() : wrapper;
      if (!(configurable instanceof ConfigurableWithOptionDescriptors)) {
        return;
      }

      String title = configurable instanceof BeanConfigurable ? ((BeanConfigurable<?>)configurable).getTitle() : null;
      String prefix = title == null ? byDefault + " " : StringUtil.trimEnd(byDefault, ':') + " in " + title + ": ";
      result.addAll(((ConfigurableWithOptionDescriptors)configurable).getOptionDescriptors(CodeFoldingConfigurable.ID, s -> prefix + s));
    });
    return result;
  }
}
