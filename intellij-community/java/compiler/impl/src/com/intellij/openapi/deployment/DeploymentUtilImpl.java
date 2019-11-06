// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.deployment;

import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.descriptors.ConfigFile;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author Alexey Kudravtsev
 */
public class DeploymentUtilImpl extends DeploymentUtil {
  private static final Logger LOG = Logger.getInstance(DeploymentUtilImpl.class);

  // OS X is sensitive for that
  private static void checkPathDoNotNavigatesUpFromFile(File file) {
    String path = file.getPath();
    int i = path.indexOf("..");
    if (i != -1) {
      String filepath = path.substring(0,i-1);
      File filepart = new File(filepath);
      if (filepart.exists() && !filepart.isDirectory()) {
        LOG.error("Incorrect file path: '" + path + '\'');
      }
    }
  }

  private static String createCopyErrorMessage(final File fromFile, final File toFile) {
    return CompilerBundle.message("message.text.error.copying.file.to.file", FileUtil.toSystemDependentName(fromFile.getPath()),
                              FileUtil.toSystemDependentName(toFile.getPath()));
  }

  @Override
  @Nullable
  public String getConfigFileErrorMessage(final ConfigFile configFile) {
    if (configFile.getVirtualFile() == null) {
      String path = FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(configFile.getUrl()));
      return CompilerBundle.message("mesage.text.deployment.descriptor.file.not.exist", path);
    }
    PsiFile psiFile = configFile.getPsiFile();
    if (psiFile == null || !psiFile.isValid()) {
      return CompilerBundle.message("message.text.deployment.description.invalid.file");
    }

    if (psiFile instanceof XmlFile) {
      XmlDocument document = ((XmlFile)psiFile).getDocument();
      if (document == null || document.getRootTag() == null) {
        return CompilerBundle.message("message.text.xml.file.invalid", FileUtil.toSystemDependentName(
          VfsUtilCore.urlToPath(configFile.getUrl())));
      }
    }
    return null;
  }

}
