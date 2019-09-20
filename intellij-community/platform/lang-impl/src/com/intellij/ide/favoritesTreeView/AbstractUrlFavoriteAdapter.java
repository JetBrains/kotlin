/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.ide.favoritesTreeView;

import com.intellij.ide.projectView.impl.AbstractUrl;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AbstractUrlFavoriteAdapter extends AbstractUrl {
  @NotNull
  private final FavoriteNodeProvider myNodeProvider;

  public AbstractUrlFavoriteAdapter(String url, String moduleName, @NotNull FavoriteNodeProvider nodeProvider) {
    super(url, moduleName, nodeProvider.getFavoriteTypeId());
    myNodeProvider = nodeProvider;
  }

  @Override
  public Object[] createPath(Project project) {
    return myNodeProvider.createPathFromUrl(project, url, moduleName);
  }

  @Override
  protected AbstractUrl createUrl(String moduleName, String url) {
    return null;
  }

  @Override
  public AbstractUrl createUrlByElement(Object element) {
    return null;
  }

  @NotNull
  FavoriteNodeProvider getNodeProvider() {
    return myNodeProvider;
  }
}
