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
package com.intellij.codeInsight.editorActions.enter;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.CodeDocumentationUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Denis Zhdanov
 */
public class EnterAfterJavadocTagHandler extends EnterHandlerDelegateAdapter {

  private static final Context NOT_MATCHED_CONTEXT = new Context();
  
  @Override
  public Result preprocessEnter(@NotNull PsiFile file,
                                @NotNull Editor editor,
                                @NotNull Ref<Integer> caretOffset,
                                @NotNull Ref<Integer> caretAdvance,
                                @NotNull DataContext dataContext,
                                EditorActionHandler originalHandler) 
  {
    if (!CodeInsightSettings.getInstance().SMART_INDENT_ON_ENTER) {
      return Result.Continue;
    }

    Document document = editor.getDocument();
    CharSequence text = document.getCharsSequence();
    int line = document.getLineNumber(caretOffset.get());
    int start = document.getLineStartOffset(line);
    int end = document.getLineEndOffset(line);
    
    CodeDocumentationUtil.CommentContext commentContext = CodeDocumentationUtil.tryParseCommentContext(file, text, caretOffset.get(), start);
    if (!commentContext.docAsterisk) {
      return Result.Continue;
    }

    Context context = parse(text, start, end, caretOffset.get());
    if (!context.shouldGenerateLine()) {
      return context.shouldIndent() ? Result.DefaultForceIndent : Result.Continue;
    }

    String indentInsideJavadoc = CodeDocumentationUtil.getIndentInsideJavadoc(document, caretOffset.get());
    
    boolean restoreCaret = false;
    if (caretOffset.get() != context.endTagStartOffset) {
      editor.getCaretModel().moveToOffset(context.endTagStartOffset);
      restoreCaret = true;
    }
      
    originalHandler.execute(editor, dataContext);
    Project project = editor.getProject();
    if (indentInsideJavadoc != null &&
        project != null &&
        CodeStyleManager.getInstance(project).getDocCommentSettings(file).isLeadingAsteriskEnabled()) {
      document.insertString(editor.getCaretModel().getOffset(), "*" + indentInsideJavadoc);
    }
    
    if (restoreCaret) {
      editor.getCaretModel().moveToOffset(caretOffset.get());
    }
    
    return Result.DefaultForceIndent;
  }

  /**
   * Analyzes location at the given offset at the given text and returns the following information about that:
   * <pre>
   * <ol>
   *   <li>
   *      if text line that contains given offset is non-first and non-last javadoc line (has {@code '*'}
   *      as a first non-white space symbol); 
   *   </li>
   *   <li>
   *      if there is particular opening tag to the left of the given offset (its end offset is returned in case of
   *      the positive answer)
   *   </li>
   *   <li>
   *      if there is particular closing tag to the left of the given offset (its start offset is returned in case of
   *      the positive answer)
   *   </li>
   * </ol>
   </pre>
   * 
   * @param text          target text to analyze
   * @param startOffset   start offset to use within the given text (inclusive)
   * @param endOffset     end offset to use withing the given text (exclusive)
   * @param offset        interested offset
   * @return              object that encapsulates information about javadoc tags within the given text and offset
   */
  @NotNull
  static Context parse(@NotNull CharSequence text, int startOffset, int endOffset, int offset) {
    int asteriskOffset = StringUtil.indexOf(text, '*', startOffset, endOffset);
    if (asteriskOffset < 0) {
      return NOT_MATCHED_CONTEXT;
    }
    
    startOffset = asteriskOffset + 1;

    int startTagStartOffset = -1;
    int startTagEndOffset = -1;
    Set<CharSequence> closedTags = new HashSet<>();
    CharSequence startTag = null;
    
    // Try to find start tag to the left of the given offset.
    for (int i = offset - 1; i >= startOffset; i--) {
      char c = text.charAt(i);
      if (c == ' ' || c == '\t') {
        continue;
      }
      
      if (c == '>' && (startTagEndOffset < 0)) {
        if (i > startOffset && text.charAt(i - 1) == '/') {
          // Handle situation like '<p/>[offset]'
          break;
        }
        else {
          startTagEndOffset = i;
          continue;
        }
      }
      
      if (c == '<') {
        if (startTagEndOffset < 0 || i >= endOffset) {  // We are inside the tag.
          break;
        }
        
        if (text.charAt(i + 1) == '/') {
          CharSequence tag = text.subSequence(i + 2, startTagEndOffset);
          closedTags.add(tag);
          startTagEndOffset = -1;
        }
        else {
          CharSequence tag = text.subSequence(i + 1, startTagEndOffset);
          if (closedTags.remove(tag)) {
            startTagEndOffset = -1;
            continue;
          }
          startTagStartOffset = i;
          startTag = text.subSequence(i + 1, startTagEndOffset + 1);
          break;
        }
      }
    }
    
    if (startTagStartOffset < 0 || startTagEndOffset < 0) {
      return NOT_MATCHED_CONTEXT;
    }
    
    int endTagStartOffset = -1;
    
    // Try to find closing tag at or after the given offset.
    for (int i = offset; i < endOffset; i++) {
      char c = text.charAt(i);
      if (c == '<' && i < endOffset - 2 && text.charAt(i + 1) == '/' && startTag != null
          && CharArrayUtil.regionMatches(text, i + 2, endOffset, startTag)) 
      {
        endTagStartOffset = i;
        break;
      }
    }
    
    
    return new Context(text, startTagEndOffset, endTagStartOffset, offset);
  }
  
  static class Context {
    
    public final int startTagEndOffset;
    public final int endTagStartOffset;

    @Nullable private final CharSequence myText;
    private final int          myOffset;

    Context() {
      this(null, -1, -1, -1);
    }

    Context(@Nullable CharSequence text, int startTagEndOffset, int endTagStartOffset, int offset) {
      myText = text;
      this.startTagEndOffset = startTagEndOffset;
      this.endTagStartOffset = endTagStartOffset;
      myOffset = offset;
    }
    
    public boolean shouldGenerateLine() {
      return endTagStartOffset >= 0 && shouldIndent();
    }
    
    public boolean shouldIndent() {
      if (startTagEndOffset < 0 || myText == null) {
        return false;
      }
      for (int i = startTagEndOffset + 1; i < myOffset; i++) {
        char c = myText.charAt(i);
        if (c != ' ' && c != '\t') {
          return false;
        }
      }
      return true;
    }
  }
}
