// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author lene
 */
public class ExternalToolsCheckinHandlerFactory extends CheckinHandlerFactory {
  @NotNull
  @Override
  public CheckinHandler createHandler(@NotNull final CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
    final ToolsProjectConfig config = ToolsProjectConfig.getInstance(panel.getProject());
    return new CheckinHandler() {
      @Override
      public RefreshableOnComponent getAfterCheckinConfigurationPanel(Disposable parentDisposable) {
        final JLabel label = new JLabel(ToolsBundle.message("tools.after.commit.description"));

        final ToolSelectComboBox toolComboBox = new ToolSelectComboBox(panel.getProject());

        BorderLayout layout = new BorderLayout();
        layout.setVgap(3);
        final JPanel panel = new JPanel(layout);
        panel.add(label, BorderLayout.NORTH);
        panel.add(toolComboBox, BorderLayout.CENTER);
        toolComboBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));

        if (toolComboBox.getValuableItemCount() == 0) {
          return null;
        }

        return new RefreshableOnComponent() {
          @Override
          public JComponent getComponent() {
            return panel;
          }

          @Override
          public void refresh() {
            String id = config.getAfterCommitToolsId();
            toolComboBox.selectTool(id);
          }

          @Override
          public void saveState() {
            Tool tool = toolComboBox.getSelectedTool();
            config.setAfterCommitToolId(tool != null ? tool.getActionId(): null);
          }

          @Override
          public void restoreState() {
            refresh();
          }
        };
      }

      @Override
      public void checkinSuccessful() {
        final String id = config.getAfterCommitToolsId();
        if (id == null) {
          return;
        }
        DataManager.getInstance()
                   .getDataContextFromFocusAsync()
                   .onSuccess(context -> UIUtil.invokeAndWaitIfNeeded((Runnable)() -> ToolAction.runTool(id, context)));
      }
    };
  }
}
