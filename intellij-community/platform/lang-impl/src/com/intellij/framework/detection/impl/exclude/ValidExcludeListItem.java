/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.framework.detection.impl.exclude;

import com.intellij.framework.FrameworkType;
import com.intellij.ide.presentation.VirtualFilePresentation;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;

/**
* @author nik
*/
class ValidExcludeListItem extends ExcludeListItem {
  private final VirtualFile myFile;
  private final FrameworkType myFrameworkType;

  ValidExcludeListItem(FrameworkType frameworkType, VirtualFile file) {
    myFrameworkType = frameworkType;
    myFile = file;
  }

  @Override
  public String getFrameworkTypeId() {
    return myFrameworkType != null ? myFrameworkType.getId() : null;
  }

  @Override
  public String getFileUrl() {
    return myFile != null ? myFile.getUrl() : null;
  }

  @Override
  public void renderItem(ColoredListCellRenderer renderer) {
    if (myFrameworkType != null) {
      renderer.setIcon(myFrameworkType.getIcon());
      renderer.append(myFrameworkType.getPresentableName());
      if (myFile != null) {
        renderer.append(" in " + myFile.getName());
        renderer.append(" (" + myFile.getPresentableUrl() + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
      }
    }
    else {
      renderer.setIcon(VirtualFilePresentation.getIcon(myFile));
      renderer.append(myFile.getName());
      renderer.append(" (" + myFile.getPresentableUrl() + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
  }

  @Override
  public String getPresentableFrameworkName() {
    return myFrameworkType != null ? myFrameworkType.getPresentableName() : null;
  }
}
