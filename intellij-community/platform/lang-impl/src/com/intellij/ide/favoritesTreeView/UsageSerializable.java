/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.codeInsight.folding.impl.GenericElementSignatureProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageInfo2UsageAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class UsageSerializable implements WorkingSetSerializable<UsageInfo, InvalidUsageNoteNode> {
  private static final String separator = "<>";

  @Override
  public String getId() {
    return UsageInfo.class.getName();
  }

  @Override
  public int getVersion() {
    return 0;
  }

  @Override
  public void serializeMe(UsageInfo info, StringBuilder os) throws IOException {
    //final SmartPsiElementPointer<?> pointer = info.getSmartPointer();
    final GenericElementSignatureProvider provider = new GenericElementSignatureProvider();
    PsiElement element = info.getElement();
    VirtualFile virtualFile = info.getVirtualFile();
    if (element == null || virtualFile == null) throw new IOException(info + " is invalid");
    final String signature = provider.getSignature(element);
    append(os, virtualFile.getPath());
    os.append(separator);
    append(os, StringUtil.notNullize(signature));
    os.append(separator);
    final ProperTextRange rangeInElement = info.getRangeInElement();
    if (rangeInElement == null) {
      append(os, "-1");
      os.append(separator);
      append(os, "-1");
      os.append(separator);
    }
    else {
      append(os, String.valueOf(rangeInElement.getStartOffset()));
      os.append(separator);
      append(os, String.valueOf(rangeInElement.getEndOffset()));
      os.append(separator);
    }
    append(os, String.valueOf(info.isNonCodeUsage()));
    os.append(separator);
    append(os, String.valueOf(info.isDynamicUsage()));
    os.append(separator);
    final String text = new UsageInfo2UsageAdapter(info).getPlainText();
    append(os, text);
    os.append(separator);
  }

  private static void append(final StringBuilder sb, @NotNull String s) {
    sb.append(StringUtil.escapeXmlEntities(s));
  }

  @Override
  public UsageInfo deserializeMe(Project project, String is) throws IOException {
    return new Reader(is).execute(project);
  }

  private static class Reader {
    private int idx;
    private final String is;

    private Reader(String is) {
      idx = 0;
      this.is = is;
    }

    private String readNext(final boolean allowEnd) {
      int idxNext = is.indexOf(separator, idx);
      if (idxNext == -1) {
        if (allowEnd) {
          return StringUtil.unescapeXmlEntities(is.substring(idx));
        }
      }
      final String s = is.substring(idx, idxNext);
      idx = idxNext + separator.length();
      return s;
    }

    public UsageInfo execute(final Project project) {
      final GenericElementSignatureProvider provider = new GenericElementSignatureProvider();

      final String path = readNext(false);
      if (path == null) return null;
      final VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(path));
      if (file == null) return null;
      PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
      if (psiFile == null) return null;
      final String signature = readNext(false);
      final PsiElement element = provider.restoreBySignature(psiFile, signature, new StringBuilder());
      if (element == null) return null;
      final String startStr = readNext(false);
      if (startStr == null) return null;
      final String endStr = readNext(false);
      if (endStr == null) return null;
      final String nonCodeUsageStr = readNext(false);
      if (nonCodeUsageStr == null) return null;
      final String dynamicUsageStr = readNext(false);
      if (dynamicUsageStr == null) return null;

      final String text = readNext(true);
      if (text == null) return null;

      final boolean nonCodeUsage = Boolean.parseBoolean(nonCodeUsageStr);
      final int start = Integer.parseInt(startStr);
      final int end = Integer.parseInt(endStr);
      final UsageInfo info = new UsageInfo(element, start, end, nonCodeUsage);
      final boolean dynamicUsage = Boolean.parseBoolean(dynamicUsageStr);
      info.setDynamicUsage(dynamicUsage);

      return info;
      /*final String newText = new UsageInfo2UsageAdapter(info).getPlainText();
      if (! Comparing.equal(newText, text)) {
        LOG.info("Usage not restored, oldText:\n'" + text + "'\nnew text: '\n" + newText + "'");
        return null;
      }*/
    }
  }

  @Override
  public InvalidUsageNoteNode deserializeMeInvalid(Project project, String is) throws IOException {
    /*is.readUTF(); //file
    is.readUTF();
    is.readInt();
    is.readInt();
    is.readBoolean();
    is.readBoolean();
    return new InvalidUsageNoteNode(Collections.singletonList(new TextChunk(new TextAttributes(), is.readUTF())));*/
    return null;
  }
}
