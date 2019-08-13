/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
 */
public class WeighingNewActionGroup extends WeighingActionGroup {
  private ActionGroup myDelegate;

  @Override
  protected ActionGroup getDelegate() {
    if (myDelegate == null) {
      myDelegate = (ActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_NEW);
      getTemplatePresentation().setText(myDelegate.getTemplatePresentation().getText());
      setPopup(myDelegate.isPopup());
    }
    return myDelegate;
  }

  @Override
  public boolean isDumbAware() {
    return true;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setText(getTemplatePresentation().getText());
  }

  @Override
  protected boolean shouldBeChosenAnyway(AnAction action) {
    final Class<? extends AnAction> aClass = action.getClass();
    return aClass == CreateFileAction.class || aClass == CreateDirectoryOrPackageAction.class ||
           "NewModuleInGroupAction".equals(aClass.getSimpleName()); //todo why is it in idea module?
  }
}
