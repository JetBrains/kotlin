// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.config;

import com.intellij.internal.statistic.collectors.fus.fileTypes.FileTypeUsageSchemaDescriptor;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.extensions.GroovyScriptTypeDetector;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

/**
 * @author sergey.evdokimov
 */
public class GradleScriptTypeDetector extends GroovyScriptTypeDetector implements FileTypeUsageSchemaDescriptor {

  public GradleScriptTypeDetector() {
    super(GradleScriptType.INSTANCE);
  }

  @Override
  public boolean isSpecificScriptFile(@NotNull GroovyFile script) {
    return GradleConstants.EXTENSION.equals(script.getViewProvider().getVirtualFile().getExtension());
  }

  @Override
  public boolean describes(@NotNull VirtualFile file) {
    String name = file.getName();
    return FileTypeRegistry.getInstance().isFileOfType(file, GroovyFileType.GROOVY_FILE_TYPE) &&
           (name.equals(GradleConstants.DEFAULT_SCRIPT_NAME) || name.equals(GradleConstants.SETTINGS_FILE_NAME));
  }
}
