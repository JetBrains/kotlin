// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.projectFingerprint;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class FileFingerprint {
  private final String myFileName;
  private final String myFilePath;
  private final String myLanguageId;
  private final int myLinesCount;
  private final long myModificationStamp;

  public FileFingerprint(@NotNull PsiFile psiFile, @NotNull Document document) {
    VirtualFile vFile = psiFile.getVirtualFile();
    myFileName = psiFile.getName();
    myFilePath = vFile.getUrl();
    myLanguageId = psiFile.getLanguage().getID();
    myLinesCount = document.getLineCount();
    myModificationStamp = vFile.getTimeStamp();
  }

  @NotNull
  public Element createElementContent() {
    Element fingerprint = new Element("file_fingerprint");
    fingerprint.addContent(new Element("file_name").addContent(myFileName));
    fingerprint.addContent(new Element("file_path").addContent(myFilePath));
    fingerprint.addContent(new Element("language").addContent(myLanguageId));
    fingerprint.addContent(new Element("lines_count").addContent(String.valueOf(myLinesCount)));
    fingerprint.addContent(new Element("modification_timestamp").addContent(String.valueOf(myModificationStamp)));
    return fingerprint;
  }
}
