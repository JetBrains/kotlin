/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.codeInsight.template.macro;

import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * @author peter
 */
public abstract class CommentMacro extends MacroBase {
  private final Function<? super Commenter, String> myCommenterFunction;

  protected CommentMacro(String name, Function<? super Commenter, String> commenterFunction) {
    super(name, name + "()");
    myCommenterFunction = commenterFunction;
  }

  @Nullable
  @Override
  protected Result calculateResult(@NotNull Expression[] params, ExpressionContext context, boolean quick) {
    Editor editor = context.getEditor();
    Language language = editor == null ? null : PsiUtilBase.getLanguageInEditor(editor, context.getProject());
    Commenter commenter = language == null ? null : LanguageCommenters.INSTANCE.forLanguage(language);
    String lineCommentPrefix = commenter == null ? null : myCommenterFunction.apply(commenter);
    return lineCommentPrefix == null ? null : new TextResult(lineCommentPrefix.trim());
  }

  public static class LineCommentStart extends CommentMacro {
    public LineCommentStart() {
      super("lineCommentStart", Commenter::getLineCommentPrefix);
    }
  }

  public static class BlockCommentStart extends CommentMacro {
    public BlockCommentStart() {
      super("blockCommentStart", Commenter::getBlockCommentPrefix);
    }
  }

  public static class BlockCommentEnd extends CommentMacro {
    public BlockCommentEnd() {
      super("blockCommentEnd", Commenter::getBlockCommentSuffix);
    }
  }

  public static class AnyCommentStart extends CommentMacro {
    public AnyCommentStart() {
      super("commentStart",
            commenter -> {
              String line = commenter.getLineCommentPrefix();
              return StringUtil.isNotEmpty(line) ? line : commenter.getBlockCommentPrefix();
            });
    }
  }
  public static class AnyCommentEnd extends CommentMacro {
    public AnyCommentEnd() {
      super("commentEnd",
            commenter -> {
              String line = commenter.getLineCommentPrefix();
              return StringUtil.isNotEmpty(line) ? "" : commenter.getBlockCommentSuffix();
            });
    }
  }

}
