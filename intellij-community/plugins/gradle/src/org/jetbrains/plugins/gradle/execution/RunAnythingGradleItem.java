// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution;

import com.intellij.ide.actions.runAnything.items.RunAnythingItemBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.cmd.GradleCommandLineOptionsProvider;

import javax.swing.*;
import java.awt.*;

/**
 * @author Vladislav.Soroka
 */
public class RunAnythingGradleItem extends RunAnythingItemBase {

  public RunAnythingGradleItem(@NotNull String command, @Nullable Icon icon) {
    super(command, icon);
  }

  @NotNull
  @Override
  public Component createComponent(@Nullable String pattern, boolean isSelected, boolean hasFocus) {
    String command = getCommand();
    JPanel component = (JPanel)super.createComponent(pattern, isSelected, hasFocus);

    int spaceIndex = StringUtil.lastIndexOf(command, ' ', 0, command.length());
    String toComplete = spaceIndex < 0 ? "" : command.substring(spaceIndex + 1);
    if (toComplete.startsWith("-")) {
      boolean isLongOpt = toComplete.startsWith("--");
      Options options = GradleCommandLineOptionsProvider.getSupportedOptions();
      Option option = options.getOption(toComplete.substring(isLongOpt ? 2 : 1));
      if (option != null) {
        String description = option.getDescription();

        if (description != null) {
          SimpleColoredComponent descriptionComponent = new SimpleColoredComponent();
          descriptionComponent
            .append(" " + StringUtil.shortenTextWithEllipsis(description, 200, 0), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
          component.add(descriptionComponent, BorderLayout.EAST);
        }
      }
    }

    return component;
  }
}