// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeEditor.printing;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

public class HTMLTextPainter {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeEditor.printing.HTMLTextPainter");

  private int myOffset = 0;
  private final EditorHighlighter myHighlighter;
  private final String myText;
  private final String myFileName;
  private int mySegmentEnd;
  private final PsiFile myPsiFile;
  private final Document myDocument;
  private int lineCount;
  private int myFirstLineNumber;
  private final boolean myPrintLineNumbers;
  private int myColumn;
  private final List<LineMarkerInfo<PsiElement>> myMethodSeparators = new ArrayList<>();
  private int myCurrentMethodSeparator;
  private final Project myProject;
  private final HtmlStyleManager htmlStyleManager;

  public HTMLTextPainter(@NotNull PsiFile psiFile, @NotNull Project project, boolean printLineNumbers) {
    this(psiFile, project, new HtmlStyleManager(false), printLineNumbers, true);
  }

  public HTMLTextPainter(@NotNull PsiFile psiFile, @NotNull Project project, @NotNull HtmlStyleManager htmlStyleManager, boolean printLineNumbers, boolean useMethodSeparators) {
    myProject = project;
    myPsiFile = psiFile;
    this.htmlStyleManager = htmlStyleManager;
    myPrintLineNumbers = printLineNumbers;
    myHighlighter = HighlighterFactory.createHighlighter(project, psiFile.getVirtualFile());

    myText = psiFile.getText();
    myHighlighter.setText(myText);
    mySegmentEnd = myText.length();
    myFileName = psiFile.getVirtualFile().getPresentableUrl();

    myDocument = PsiDocumentManager.getInstance(project).getDocument(psiFile);

    if (useMethodSeparators && myDocument != null) {
      myMethodSeparators.addAll(FileSeparatorProvider.getFileSeparators(psiFile, myDocument));
    }
    myCurrentMethodSeparator = 0;
  }

  private HTMLTextPainter(@NotNull PsiElement context, @NotNull String codeFragment) {
    myProject = context.getProject();
    myPsiFile = context.getContainingFile();
    if (myPsiFile == null) {
      throw new IllegalArgumentException("Bad context: no container file");
    }

    htmlStyleManager = new HtmlStyleManager(true);
    myPrintLineNumbers = false;
    myHighlighter = HighlighterFactory.createHighlighter(myProject, myPsiFile.getFileType());

    myText = codeFragment;
    myHighlighter.setText(myText);
    mySegmentEnd = myText.length();
    myFileName = "fragment";

    myDocument = null;
    myCurrentMethodSeparator = 0;
  }

  @NotNull
  public PsiFile getPsiFile() {
    return myPsiFile;
  }

  public void setSegment(int segmentStart, int segmentEnd, int firstLineNumber) {
    myOffset = segmentStart;
    mySegmentEnd = segmentEnd;
    myFirstLineNumber = firstLineNumber;
  }

  public void paint(@Nullable TreeMap refMap, @NotNull Writer writer, boolean isStandalone) throws IOException {
    HighlighterIterator hIterator = myHighlighter.createIterator(myOffset);
    if (hIterator.atEnd()) {
      return;
    }

    lineCount = myFirstLineNumber;
    TextAttributes prevAttributes = null;
    Iterator refKeys = null;

    int refOffset = -1;
    PsiReference ref = null;
    if (refMap != null) {
      refKeys = refMap.keySet().iterator();
      if (refKeys.hasNext()) {
        Integer key = (Integer)refKeys.next();
        ref = (PsiReference)refMap.get(key);
        refOffset = key.intValue();
      }
    }

    int referenceEnd = -1;
    if (isStandalone) {
      writeHeader(writer, new File(myFileName).getName());
    }
    else {
      ensureStyles();
    }
    writer.write("<pre>");
    if (myFirstLineNumber == 0) {
      writeLineNumber(writer);
    }
    String closeTag = null;

    getMethodSeparator(hIterator.getStart());

    while (!hIterator.atEnd()) {
      int hStart = hIterator.getStart();
      int hEnd = hIterator.getEnd();
      if (hEnd > mySegmentEnd) {
        break;
      }

      // write whitespace as is
      for (; hStart < hEnd; hStart++) {
        char c = myText.charAt(hStart);
        if (Character.isWhitespace(c)) {
          if (closeTag != null && c == '\n') {
            writer.write(closeTag);
            closeTag = null;
            prevAttributes = null;
          }
          if (c == '\n') {
            writeLineSeparatorAndNumber(writer, hStart);
          }
          else {
            writer.write(c);
          }
        }
        else {
          break;
        }
      }

      if (hStart == hEnd) {
        hIterator.advance();
        continue;
      }

      if (refOffset > 0 && hStart <= refOffset && hEnd > refOffset) {
        referenceEnd = writeReferenceTag(writer, ref);
      }

      TextAttributes textAttributes = hIterator.getTextAttributes();
      if (htmlStyleManager.isDefaultAttributes(textAttributes)) {
        textAttributes = null;
      }

      if (!equals(prevAttributes, textAttributes) && referenceEnd < 0) {
        if (closeTag != null) {
          writer.write(closeTag);
          closeTag = null;
        }
        if (textAttributes != null) {
          htmlStyleManager.writeTextStyle(writer, textAttributes);
          closeTag = "</span>";
        }
        prevAttributes = textAttributes;
      }

      writeString(writer, myText, hStart, hEnd - hStart, myPsiFile);
      if (referenceEnd > 0 && hEnd >= referenceEnd) {
        writer.write("</a>");
        referenceEnd = -1;
        if (refKeys.hasNext()) {
          Integer key = (Integer)refKeys.next();
          ref = (PsiReference)refMap.get(key);
          refOffset = key.intValue();
        }
      }
      hIterator.advance();
    }

    if (closeTag != null) {
      writer.write(closeTag);
    }

    writer.write("</pre>\n");
    if (isStandalone) {
      writer.write("</body>\n");
      writer.write("</html>");
    }
  }

  protected void ensureStyles() {
    htmlStyleManager.ensureStyles(myHighlighter.createIterator(myOffset), myMethodSeparators);
  }

  @Nullable
  private LineMarkerInfo getMethodSeparator(int offset) {
    if (myDocument == null) {
      return null;
    }

    int line = myDocument.getLineNumber(Math.max(0, Math.min(myDocument.getTextLength(), offset)));
    LineMarkerInfo marker = null;
    LineMarkerInfo tmpMarker;
    while (myCurrentMethodSeparator < myMethodSeparators.size() &&
           (tmpMarker = myMethodSeparators.get(myCurrentMethodSeparator)) != null &&
           FileSeparatorProvider.getDisplayLine(tmpMarker, myDocument) <= line) {
      marker = tmpMarker;
      myCurrentMethodSeparator++;
    }
    return marker;
  }

  private int writeReferenceTag(Writer writer, PsiReference ref) throws IOException {
    PsiFile refFile = Objects.requireNonNull(ref.resolve()).getContainingFile();
    PsiDirectoryFactory psiDirectoryFactory = PsiDirectoryFactory.getInstance(myProject);
    String refPackageName = psiDirectoryFactory.getQualifiedName(refFile.getContainingDirectory(), false);
    String psiPackageName = psiDirectoryFactory.getQualifiedName(myPsiFile.getContainingDirectory(), false);

    StringBuilder fileName = new StringBuilder();
    if (!psiPackageName.equals(refPackageName)) {
      StringTokenizer tokens = new StringTokenizer(psiPackageName, ".");
      while(tokens.hasMoreTokens()) {
        tokens.nextToken();
        fileName.append("../");
      }

      StringTokenizer refTokens = new StringTokenizer(refPackageName, ".");
      while(refTokens.hasMoreTokens()) {
        String token = refTokens.nextToken();
        fileName.append(token);
        fileName.append('/');
      }
    }
    fileName.append(ExportToHTMLManager.getHTMLFileName(refFile));
    //noinspection HardCodedStringLiteral
    writer.write("<a href=\""+fileName+"\">");
    return ref.getElement().getTextRange().getEndOffset();
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private void writeString(Writer writer, CharSequence charArray, int start, int length, @NotNull PsiFile psiFile) throws IOException {
    for (int i = start; i < start + length; i++) {
      char c = charArray.charAt(i);
      if (c == '<') {
        writeChar(writer, "&lt;");
      }
      else if (c == '>') {
        writeChar(writer, "&gt;");
      }
      else if (c == '&') {
        writeChar(writer, "&amp;");
      }
      else if (c == '\"') {
        writeChar(writer, "&quot;");
      }
      else if (c == '\t') {
        int tabSize = CodeStyle.getIndentOptions(psiFile).TAB_SIZE;
        if (tabSize <= 0) tabSize = 1;
        int nSpaces = tabSize - myColumn % tabSize;
        for (int j = 0; j < nSpaces; j++) {
          writeChar(writer, " ");
        }
      }
      else if (c == '\n' || c == '\r') {
        if (c == '\r' && i + 1 < start + length && charArray.charAt(i + 1) == '\n') {
          //noinspection AssignmentToForLoopParameter
          i++;
        }
        else if (c == '\n') {
          writeChar(writer, " ");
        }

        writeLineSeparatorAndNumber(writer, i);
      }
      else {
        writer.write(c);
        myColumn++;
      }
    }
  }

  private void writeLineSeparatorAndNumber(@NotNull Writer writer, int i) throws IOException {
    LineMarkerInfo marker = getMethodSeparator(i + 1);
    if (marker == null) {
      writer.write('\n');
    }
    else {
      writer.write("<hr class=\"" + htmlStyleManager.getSeparatorClassName(marker.separatorColor) + "\">");
    }
    writeLineNumber(writer);
  }

  private void writeChar(Writer writer, String s) throws IOException {
    writer.write(s);
    myColumn++;
  }

  private void writeLineNumber(@NonNls Writer writer) throws IOException {
    myColumn = 0;
    lineCount++;
    if (myPrintLineNumbers) {
      writer.write("<a name=\"l" + lineCount + "\">");

//      String numberCloseTag = writeFontTag(writer, ourLineNumberAttributes);

      writer.write("<span class=\"ln\">");
      String s = Integer.toString(lineCount);
      writer.write(s);
      int extraSpaces = 4 - s.length();
      do {
        writer.write(' ');
      } while (extraSpaces-- > 0);
      writer.write("</span></a>");
    }
  }

  private void writeHeader(@NonNls Writer writer, @Nullable String title) throws IOException {
    writer.write("<html>\n");
    writer.write("<head>\n");
    writer.write("<title>" + title + "</title>\n");
    writer.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");
    ensureStyles();
    htmlStyleManager.writeStyleTag(writer, myPrintLineNumbers);
    writer.write("</head>\n");
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    writer.write("<body bgcolor=\"" + ColorUtil.toHtmlColor(scheme.getDefaultBackground()) + "\">\n");
    writer.write("<table CELLSPACING=0 CELLPADDING=5 COLS=1 WIDTH=\"100%\" BGCOLOR=\"#" + ColorUtil.toHex(new JBColor(Gray.xC0, Gray.x60)) + "\" >\n");
    writer.write("<tr><td><center>\n");
    writer.write("<font face=\"Arial, Helvetica\" color=\"#000000\">\n");
    writer.write(title + "</font>\n");
    writer.write("</center></td></tr></table>\n");
  }

  private static boolean equals(TextAttributes attributes1, TextAttributes attributes2) {
    if (attributes2 == null) {
      return attributes1 == null;
    }
    if(attributes1 == null) {
      return false;
    }
    if(!Comparing.equal(attributes1.getForegroundColor(), attributes2.getForegroundColor())) {
      return false;
    }
    if(attributes1.getFontType() != attributes2.getFontType()) {
      return false;
    }
    if(!Comparing.equal(attributes1.getBackgroundColor(), attributes2.getBackgroundColor())) {
      return false;
    }
    if(!Comparing.equal(attributes1.getEffectColor(), attributes2.getEffectColor())) {
      return false;
    }
    return true;
  }

  /**
   * Converts the code fragment to HTML with in-line styles.
   * The information about language, project and markup settings is getting
   * from {@code context} parameter.
   *
   * The code tokens in HTML are highlighted by lexer-based highlighter.
   * There is no formatting activity in this call.
   *
   * @param context the element that provide information about language, project and markup settings
   * @param codeFragment the code fragment that need to be converted to HTML
   * @return the HTML fragment in {@code pre}-tag container
   */
  @NotNull
  public static String convertCodeFragmentToHTMLFragmentWithInlineStyles(@NotNull PsiElement context, @NotNull String codeFragment) {
    try {
      StringWriter writer = new StringWriter();
      new HTMLTextPainter(context, codeFragment).paint(null, writer, false);
      return writer.toString();
    }
    catch (Throwable e) {
      LOG.error(e);
      return String.format("<pre>%s</pre>\n", codeFragment);
    }
  }
}
