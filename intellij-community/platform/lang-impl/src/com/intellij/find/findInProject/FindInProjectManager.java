// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.find.findInProject;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.find.replaceInProject.ReplaceInProjectManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FindInProjectManager {
  private final Project myProject;
  private volatile boolean myIsFindInProgress;

  public static FindInProjectManager getInstance(Project project) {
    return ServiceManager.getService(project, FindInProjectManager.class);
  }

  public FindInProjectManager(Project project) {
    myProject = project;
  }

  /**
   *
   * @param model would be used for search if not null, otherwise shared (project-level) model would be used
   */
  public void findInProject(@NotNull DataContext dataContext, @Nullable FindModel model) {

    final FindManager findManager = FindManager.getInstance(myProject);
    final FindModel findModel;
    if (model != null) {
      findModel = model.clone();
    }
    else {
      findModel = findManager.getFindInProjectModel().clone();
      findModel.setReplaceState(false);
      initModel(findModel, dataContext);
    }

    findManager.showFindDialog(findModel, () -> {
      if (findModel.isReplaceState()) {
        ReplaceInProjectManager.getInstance(myProject).replaceInPath(findModel);
      } else {
        findInPath(findModel);
      }
    });
  }

  public void findInPath(@NotNull FindModel findModel) {
    startFindInProject(findModel);
  }

  @SuppressWarnings("WeakerAccess")
  protected void initModel(@NotNull FindModel findModel, @NotNull DataContext dataContext) {
    FindInProjectUtil.setDirectoryName(findModel, dataContext);

    String text = PlatformDataKeys.PREDEFINED_TEXT.getData(dataContext);
    if (text != null) {
      FindModel.initStringToFindNoMultiline(findModel, text);
    }
    else {
      FindInProjectUtil.initStringToFindFromDataContext(findModel, dataContext);
    }
  }

  public void startFindInProject(@NotNull FindModel findModel) {
    if (findModel.getDirectoryName() != null && FindInProjectUtil.getDirectory(findModel) == null) {
      return;
    }

    UsageViewManager manager = UsageViewManager.getInstance(myProject);

    if (manager == null) return;
    final FindManager findManager = FindManager.getInstance(myProject);
    findManager.getFindInProjectModel().copyFrom(findModel);
    final FindModel findModelCopy = findModel.clone();
    final UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation(findModelCopy);
    final FindUsagesProcessPresentation processPresentation =
      FindInProjectUtil.setupProcessPresentation(myProject, presentation);
    ConfigurableUsageTarget usageTarget = new FindInProjectUtil.StringUsageTarget(myProject, findModel);

    ((FindManagerImpl)FindManager.getInstance(myProject)).getFindUsagesManager().addToHistory(usageTarget);

    manager.searchAndShowUsages(new UsageTarget[]{usageTarget},
                                () -> processor -> {
                                  myIsFindInProgress = true;

                                  try {
                                    Processor<UsageInfo> consumer = info -> {
                                      Usage usage = UsageInfo2UsageAdapter.CONVERTER.fun(info);
                                      usage.getPresentation().getIcon(); // cache icon
                                      return processor.process(usage);
                                    };
                                    FindInProjectUtil.findUsages(findModelCopy, myProject, consumer, processPresentation);
                                  }
                                  finally {
                                    myIsFindInProgress = false;
                                  }
                                },
                                processPresentation,
                                presentation,
                                null
    );
  }

  public boolean isWorkInProgress() {
    return myIsFindInProgress;
  }

  public boolean isEnabled() {
    return !myIsFindInProgress && !ReplaceInProjectManager.getInstance(myProject).isWorkInProgress();
  }
}
