/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.codeInspection;

import com.intellij.ide.DataManager;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.properties.charset.Native2AsciiCharset;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.ChangeFileEncodingAction;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import com.intellij.openapi.vfs.encoding.EncodingUtil;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class LossyEncodingInspection extends LocalInspectionTool {
  private static final Logger LOG = Logger.getInstance(LossyEncodingInspection.class);

  @Override
  @Nls
  @NotNull
  public String getGroupDisplayName() {
    return InspectionsBundle.message("group.names.internationalization.issues");
  }

  @Override
  @Nls
  @NotNull
  public String getDisplayName() {
    return InspectionsBundle.message("lossy.encoding");
  }

  @Override
  @NonNls
  @NotNull
  public String getShortName() {
    return "LossyEncoding";
  }

  @Override
  @Nullable
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (InjectedLanguageManager.getInstance(file.getProject()).isInjectedFragment(file)) return null;
    if (!file.isPhysical()) return null;
    FileViewProvider viewProvider = file.getViewProvider();
    if (viewProvider.getBaseLanguage() != file.getLanguage()) return null;
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return null;
    if (!virtualFile.isInLocalFileSystem()) return null;
    CharSequence text = viewProvider.getContents();
    Charset charset = LoadTextUtil.extractCharsetFromFileContent(file.getProject(), virtualFile, text);

    // no sense in checking transparently decoded file: all characters there are already safely encoded
    if (charset instanceof Native2AsciiCharset) return null;

    List<ProblemDescriptor> descriptors = new SmartList<>();
    boolean ok = checkFileLoadedInWrongEncoding(file, manager, isOnTheFly, virtualFile, charset, descriptors);
    if (ok) {
      checkIfCharactersWillBeLostAfterSave(file, manager, isOnTheFly, text, charset, descriptors);
    }

    return descriptors.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static boolean checkFileLoadedInWrongEncoding(@NotNull PsiFile file,
                                                        @NotNull InspectionManager manager,
                                                        boolean isOnTheFly,
                                                        @NotNull VirtualFile virtualFile,
                                                        @NotNull Charset charset,
                                                        @NotNull List<? super ProblemDescriptor> descriptors) {
    if (FileDocumentManager.getInstance().isFileModified(virtualFile) // when file is modified, it's too late to reload it
        || !EncodingUtil.canReload(virtualFile) // can't reload in another encoding, no point trying
      ) {
      return true;
    }
    if (!isGoodCharset(virtualFile, charset)) {
      LocalQuickFix[] fixes = getFixes(file, virtualFile, charset);
      descriptors.add(manager.createProblemDescriptor(file, "File was loaded in the wrong encoding: '" + charset + "'", true,
                                                      ProblemHighlightType.GENERIC_ERROR, isOnTheFly, fixes));
      return false;
    }
    return true;
  }

  @NotNull
  private static LocalQuickFix[] getFixes(@NotNull PsiFile file,
                                          @NotNull VirtualFile virtualFile,
                                          @NotNull Charset wrongCharset) {
    Set<Charset> suspects = ContainerUtil.newHashSet(CharsetToolkit.getDefaultSystemCharset(), CharsetToolkit.getPlatformCharset());
    suspects.remove(wrongCharset);
    List<Charset> goodCharsets = ContainerUtil.filter(suspects, c -> isGoodCharset(virtualFile, c));
    List<LocalQuickFix> fixes = new ArrayList<>();
    if (!goodCharsets.isEmpty()) {
      Charset goodCharset = goodCharsets.get(0);
      fixes.add(new LocalQuickFix() {
        @Nls
        @NotNull
        @Override
        public String getFamilyName() {
          return "Reload in '" + goodCharset.displayName()+"'";
        }

        @Override
        public boolean startInWriteAction() {
          return false;
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
          Document document = PsiDocumentManager.getInstance(project).getDocument(file);
          if (document == null) return;
          ChangeFileEncodingAction.changeTo(project, document, null, virtualFile, goodCharset, EncodingUtil.Magic8.ABSOLUTELY, EncodingUtil.Magic8.ABSOLUTELY);
        }
      });
      fixes.add(new LocalQuickFix() {
        @Nls
        @NotNull
        @Override
        public String getFamilyName() {
          return "Set project encoding to '" + goodCharset.displayName()+"'";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
          EncodingProjectManager.getInstance(project).setDefaultCharsetName(goodCharset.name());
        }
      });
    }
    fixes.add(new ReloadInAnotherEncodingFix(file));
    return fixes.toArray(LocalQuickFix.EMPTY_ARRAY);
  }

  // check if file was loaded in correct encoding
  // returns true if text converted with charset is equals to the bytes currently on disk
  private static boolean isGoodCharset(@NotNull VirtualFile virtualFile, @NotNull Charset charset) {
    FileDocumentManager documentManager = FileDocumentManager.getInstance();
    Document document = documentManager.getDocument(virtualFile);
    if (document == null) return true;
    byte[] loadedBytes;
    byte[] bytesToSave;
    try {
      loadedBytes = virtualFile.contentsToByteArray();
      bytesToSave = new String(loadedBytes, charset).getBytes(charset);
    }
    catch (Exception e) {
      return true;
    }
    if (loadedBytes.length == 0 && bytesToSave.length == 0) {
      // hold on, file was just created, no content was written yet
      return true;
    }
    byte[] bom = virtualFile.getBOM();
    if (bom != null && !ArrayUtil.startsWith(bytesToSave, bom)) {
      bytesToSave = ArrayUtil.mergeArrays(bom, bytesToSave); // for 2-byte encodings String.getBytes(Charset) adds BOM automatically
    }

    boolean equals = Arrays.equals(bytesToSave, loadedBytes);
    if (!equals && LOG.isDebugEnabled()) {
      try {
        String tempDir = FileUtil.getTempDirectory();
        FileUtil.writeToFile(new File(tempDir, "lossy-bytes-to-save"), bytesToSave);
        FileUtil.writeToFile(new File(tempDir, "lossy-loaded-bytes"), loadedBytes);
        LOG.debug("lossy bytes dumped into " + tempDir);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return equals;
  }

  private static void checkIfCharactersWillBeLostAfterSave(@NotNull PsiFile file,
                                                           @NotNull InspectionManager manager,
                                                           boolean isOnTheFly,
                                                           @NotNull CharSequence text,
                                                           @NotNull Charset charset,
                                                           @NotNull List<ProblemDescriptor> descriptors) {
    CharBuffer buffer = CharBuffer.wrap(text);

    int textLength = text.length();
    CharBuffer back = CharBuffer.allocate(textLength); // must be enough, error otherwise

    Ref<ByteBuffer> outRef = Ref.create();

    //do not report too many errors
    for (int pos = 0, errorCount = 0; pos < text.length() && errorCount < 200; errorCount++) {
      TextRange errRange = nextUnmappable(buffer, pos, outRef, back, charset);
      if (errRange == null) break;
      ProblemDescriptor lastDescriptor = ContainerUtil.getLastItem(descriptors);
      if (lastDescriptor != null && lastDescriptor.getTextRangeInElement().getEndOffset() == errRange.getStartOffset()) {
        // combine two adjacent descriptors
        errRange = lastDescriptor.getTextRangeInElement().union(errRange);
        descriptors.remove(descriptors.size() - 1);
      }
      String message = InspectionsBundle.message("unsupported.character.for.the.charset", charset);
      ProblemDescriptor descriptor =
            manager.createProblemDescriptor(file, errRange, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly,
                                            new ChangeEncodingFix(file));
      descriptors.add(descriptor);
      pos = errRange.getEndOffset();
    }
  }

  // returns null if OK
  // range of the characters either failed to be encoded to bytes or failed to be decoded back or decoded to chars different from the original
  private static TextRange nextUnmappable(@NotNull CharBuffer in,
                                          int position,
                                          @NotNull Ref<ByteBuffer> outRef,
                                          @NotNull CharBuffer back,
                                          @NotNull Charset charset) {
    CharsetEncoder encoder = charset.newEncoder()
                                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                                    .onMalformedInput(CodingErrorAction.REPORT);
    int textLength = in.limit() - position;

    ByteBuffer out = outRef.get();
    if (out == null) {
      outRef.set(out = ByteBuffer.allocate((int)(encoder.averageBytesPerChar() * textLength)));
    }
    out.rewind();
    out.limit(out.capacity());
    in.rewind();
    in.position(position);
    CoderResult cr;
    for (;;) {
      cr = in.hasRemaining() ? encoder.encode(in, out, true) : CoderResult.UNDERFLOW;
      if (cr.isUnderflow()) {
        cr = encoder.flush(out);
      }

      if (!cr.isOverflow()) {
        break;
      }

      int n = 3 * out.capacity()/2 + 1;
      ByteBuffer tmp = ByteBuffer.allocate(n);
      out.flip();
      tmp.put(out);
      outRef.set(out = tmp);
    }
    if (cr.isError()) {
      return TextRange.from(in.position(), cr.length());
    }
    // phew, encoded successfully. now check if we can decode it back with char-to-char precision
    int outLength = out.position();
    CharsetDecoder decoder = charset.newDecoder()
                                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                                    .onMalformedInput(CodingErrorAction.REPORT);
    out.rewind();
    out.limit(outLength);
    back.rewind();
    CoderResult dr = decoder.decode(out, back, true);
    if (dr.isError()) {
      return TextRange.from(back.position(), dr.length());
    }
    if (back.position() != textLength) {
      return TextRange.from(Math.min(textLength, back.position()), 1);
    }
    // ok, we decoded it back to string. now compare if the strings are identical
    in.rewind();
    in.position(position);
    back.rewind();
    int len = StringUtil.commonPrefixLength(in, back);
    if (len == textLength) return null;
    return TextRange.from(len, 1);  // lets report only the first diff char
  }

  private static class ReloadInAnotherEncodingFix extends ChangeEncodingFix {
    ReloadInAnotherEncodingFix(@NotNull PsiFile file) {
      super(file);
    }

    @NotNull
    @Override
    public String getText() {
      return "Reload in another encoding";
    }

    @Override
    public void invoke(@NotNull Project project,
                       @NotNull PsiFile file,
                       @Nullable Editor editor,
                       @NotNull PsiElement startElement,
                       @NotNull PsiElement endElement) {
      if (FileDocumentManager.getInstance().isFileModified(file.getVirtualFile())) return;
      super.invoke(project, file, editor, startElement, endElement);
    }
  }

  private static class ChangeEncodingFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    ChangeEncodingFix(@NotNull PsiFile file) {
      super(file);
    }

    @NotNull
    @Override
    public String getText() {
      return getFamilyName();
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return "Change file encoding";
    }

    @Override
    public void invoke(@NotNull Project project,
                       @NotNull PsiFile file,
                       @Nullable Editor editor,
                       @NotNull PsiElement startElement,
                       @NotNull PsiElement endElement) {
      VirtualFile virtualFile = file.getVirtualFile();

      DataContext dataContext = createDataContext(editor, editor == null ? null : editor.getComponent(), virtualFile, project);
      ListPopup popup = new ChangeFileEncodingAction().createPopup(dataContext);
      if (popup != null) {
        popup.showInBestPositionFor(dataContext);
      }
    }

    @NotNull
    static DataContext createDataContext(Editor editor, Component component, VirtualFile selectedFile, Project project) {
      DataContext parent = DataManager.getInstance().getDataContext(component);
      DataContext context = SimpleDataContext.getSimpleContext(PlatformDataKeys.CONTEXT_COMPONENT.getName(), editor == null ? null : editor.getComponent(), parent);
      DataContext projectContext = SimpleDataContext.getSimpleContext(CommonDataKeys.PROJECT.getName(), project, context);
      return SimpleDataContext.getSimpleContext(CommonDataKeys.VIRTUAL_FILE.getName(), selectedFile, projectContext);
    }
  }
}
