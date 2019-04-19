// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractExternalFilter {
  private static final Logger LOG = Logger.getInstance(AbstractExternalFilter.class);

  protected static final Pattern ourAnchorSuffix = Pattern.compile("#(.*)$");
  protected static final Pattern ourHtmlFileSuffix = Pattern.compile("/([^/]*[.][hH][tT][mM][lL]?)$");
  protected static final String HTML = "<HTML>";
  protected static final String HTML_CLOSE = "</HTML>";

  private static final Pattern CLASS_DATA_START = Pattern.compile("START OF CLASS DATA", Pattern.CASE_INSENSITIVE);
  private static final Pattern CLASS_DATA_END = Pattern.compile("SUMMARY ========", Pattern.CASE_INSENSITIVE);
  private static final Pattern NON_CLASS_DATA_END = Pattern.compile("<A (NAME|ID)=", Pattern.CASE_INSENSITIVE);
  private static final Pattern ANNIHILATOR = Pattern.compile("/[^/^.]*/[.][.]/");
  private static final Pattern CHARSET_META = Pattern.compile("<meta[^>]+\\s*charset=\"?([\\w\\-]*)\\s*\">", Pattern.CASE_INSENSITIVE);

  private static final String FIELD_SUMMARY = "<!-- =========== FIELD SUMMARY =========== -->";
  private static final String CLASS_SUMMARY = "<div class=\"summary\">";
  private static final String GREATEST_END_SECTION = "<!-- ========= END OF CLASS DATA ========= -->";
  private static final String JAR_PROTOCOL = "jar:";
  private static final String HR = "<HR>";
  private static final String P = "<P>";
  private static final String DL = "<DL>";
  private static final String H2 = "H2";
  private static final String H2_CLOSE = "</H2>";
  private static final String BR = "<BR>";
  private static final String DT = "<DT>";

  protected static abstract class RefConvertor {
    private final @NotNull Pattern mySelector;

    public RefConvertor(@NotNull Pattern selector) {
      mySelector = selector;
    }

    protected abstract String convertReference(String root, String href);

    public CharSequence refFilter(String root, @NotNull CharSequence read) {
      CharSequence toMatch = StringUtilRt.toUpperCase(read);
      StringBuilder ready = new StringBuilder();
      int prev = 0;
      Matcher matcher = mySelector.matcher(toMatch);

      while (matcher.find()) {
        CharSequence before = read.subSequence(prev, matcher.start(1) - 1);     // Before reference
        CharSequence href = read.subSequence(matcher.start(1), matcher.end(1)); // The URL
        prev = matcher.end(1) + 1;
        ready.append(before);
        ready.append("\"");
        ready.append(ReadAction.compute(() -> convertReference(root, href.toString())));
        ready.append("\"");
      }

      ready.append(read, prev, read.length());

      return ready;
    }
  }

  protected static String doAnnihilate(String path) {
    int len = path.length();
    do {
      path = ANNIHILATOR.matcher(path).replaceAll("/");
    }
    while (len > (len = path.length()));
    return path;
  }

  public CharSequence correctRefs(String root, CharSequence read) {
    CharSequence result = read;
    for (RefConvertor converter : getRefConverters()) {
      result = converter.refFilter(root, result);
    }
    return result;
  }

  protected abstract RefConvertor[] getRefConverters();

  @Nullable
  public String getExternalDocInfo(String url) throws Exception {
    Application app = ApplicationManager.getApplication();
    if (!app.isUnitTestMode() && app.isDispatchThread() || app.isWriteAccessAllowed()) {
      LOG.error("May block indefinitely: shouldn't be called from EDT or under write lock");
      return null;
    }

    if (url == null || !MyJavadocFetcher.ourFree) {
      return null;
    }

    MyJavadocFetcher fetcher = new MyJavadocFetcher(url, (_url, input, result) -> doBuildFromStream(_url, input, result));
    try {
      app.executeOnPooledThread(fetcher).get();
    }
    catch (Exception e) {
      return null;
    }

    Exception exception = fetcher.myException;
    if (exception != null) {
      fetcher.myException = null;
      throw exception;
    }

    return correctDocText(url, fetcher.data);
  }

  @NotNull
  protected String correctDocText(@NotNull String url, @NotNull CharSequence data) {
    CharSequence docText = correctRefs(ourAnchorSuffix.matcher(url).replaceAll(""), data);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Filtered JavaDoc: " + docText + "\n");
    }
    return PlatformDocumentationUtil.fixupText(docText);
  }

  @Nullable
  public String getExternalDocInfoForElement(String docURL, PsiElement element) throws Exception {
    return getExternalDocInfo(docURL);
  }

  protected void doBuildFromStream(String url, Reader input, StringBuilder data) throws IOException {
    doBuildFromStream(url, input, data, true, true);
  }

  protected void doBuildFromStream(String url, Reader input, StringBuilder data, boolean searchForEncoding, boolean matchStart) throws IOException {
    ParseSettings settings = getParseSettings(url);
    Pattern startSection = settings.startPattern;
    Pattern endSection = settings.endPattern;
    boolean useDt = settings.useDt;

    data.append(HTML);
    URL baseUrl = VfsUtilCore.convertToURL(url);
    if (baseUrl != null) {
      data.append("<base href=\"").append(baseUrl).append("\">");
    }
    data.append("<style type=\"text/css\">" +
                "  ul.inheritance {\n" +
                "      margin:0;\n" +
                "      padding:0;\n" +
                "  }\n" +
                "  ul.inheritance li {\n" +
                "       display:inline;\n" +
                "       list-style-type:none;\n" +
                "  }\n" +
                "  ul.inheritance li ul.inheritance {\n" +
                "    margin-left:15px;\n" +
                "    padding-left:15px;\n" +
                "    padding-top:1px;\n" +
                "  }\n" +
                "</style>");

    String read;
    String contentEncoding = null;
    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed") BufferedReader buf = new BufferedReader(input);
    do {
      read = buf.readLine();
      if (read != null && searchForEncoding) {
        String foundEncoding = parseContentEncoding(read);
        if (foundEncoding != null) {
          contentEncoding = foundEncoding;
        }
      }
    }
    while (read != null && matchStart && !startSection.matcher(StringUtil.toUpperCase(read)).find());

    if (input instanceof MyReader && contentEncoding != null &&
        !(contentEncoding.equalsIgnoreCase(CharsetToolkit.UTF8) || contentEncoding.equals(((MyReader)input).getEncoding()))) {
      //restart page parsing with correct encoding
      try {
        data.setLength(0);
        doBuildFromStream(url, new MyReader(((MyReader)input).myInputStream, contentEncoding), data, false, true);
      }
      catch (ProcessCanceledException ignored) { }
      return;
    }

    if (read == null) {
      data.setLength(0);
      if (matchStart && !settings.forcePatternSearch && input instanceof MyReader) {
        try {
          Reader reader = contentEncoding != null ? new MyReader(((MyReader)input).myInputStream, contentEncoding)
                                                  : new MyReader(((MyReader)input).myInputStream, ((MyReader)input).getEncoding());
          doBuildFromStream(url, reader, data, false, false);
        }
        catch (ProcessCanceledException ignored) { }
      }
      return;
    }

    if (useDt) {
      boolean skip = false;

      do {
        if (StringUtil.containsIgnoreCase(read, H2_CLOSE) && !StringUtil.containsIgnoreCase(read, H2)) { // read=class name in <H2>
          data.append(H2_CLOSE);
          skip = true;
        }
        else if (endSection.matcher(read).find() || StringUtil.indexOfIgnoreCase(read, GREATEST_END_SECTION, 0) != -1) {
          data.append(HTML_CLOSE);
          return;
        }
        else if (!skip) {
          appendLine(data, read);
        }
      }
      while (((read = buf.readLine()) != null) && !StringUtil.toUpperCase(read).trim().equals(DL) &&
             !StringUtil.containsIgnoreCase(read, "<div class=\"description\""));

      data.append(DL);

      StringBuilder classDetails = new StringBuilder();
      while (((read = buf.readLine()) != null) && !StringUtil.toUpperCase(read).equals(HR) && !StringUtil.toUpperCase(read).equals(P)) {
        if (reachTheEnd(data, read, classDetails, endSection)) return;
        if (!skipBlockList(read)) {
          appendLine(classDetails, read);
        }
      }

      while (((read = buf.readLine()) != null) && !StringUtil.toUpperCase(read).equals(HR) && !StringUtil.toUpperCase(read).equals(P)) {
        if (reachTheEnd(data, read, classDetails, endSection)) return;
        if (!skipBlockList(read)) {
          appendLine(data, StringUtil.replace(read, DT, DT + BR));
        }
      }

      data.append(classDetails);
      data.append(P);
    }
    else {
      appendLine(data, read);
    }

    while (((read = buf.readLine()) != null) &&
           !endSection.matcher(read).find() &&
           StringUtil.indexOfIgnoreCase(read, GREATEST_END_SECTION, 0) == -1) {
      if (!skipBlockList(read)) {
        appendLine(data, read);
      }
    }

    data.append(HTML_CLOSE);
  }

  private static boolean skipBlockList(String read) {
    return StringUtil.containsIgnoreCase(read, HR) ||
           StringUtil.containsIgnoreCase(read, "<ul class=\"blockList\">") ||
           StringUtil.containsIgnoreCase(read, "<li class=\"blockList\">");
  }

  @NotNull
  protected ParseSettings getParseSettings(@NotNull String url) {
    Pattern startSection = CLASS_DATA_START;
    Pattern endSection = CLASS_DATA_END;
    boolean anchorPresent = false;

    Matcher anchorMatcher = ourAnchorSuffix.matcher(url);
    if (anchorMatcher.find()) {
      anchorPresent = true;
      startSection = Pattern.compile("<a (name|id)=\"" + Pattern.quote(StringUtil.escapeXmlEntities(anchorMatcher.group(1))) + "\"",
                                     Pattern.CASE_INSENSITIVE);
      endSection = NON_CLASS_DATA_END;
    }
    return new ParseSettings(startSection, endSection, !anchorPresent, anchorPresent);
  }

  private static boolean reachTheEnd(StringBuilder data, String read, StringBuilder classDetails, Pattern endSection) {
    if (StringUtil.indexOfIgnoreCase(read, FIELD_SUMMARY, 0) != -1 ||
        StringUtil.indexOfIgnoreCase(read, CLASS_SUMMARY, 0) != -1 ||
        StringUtil.indexOfIgnoreCase(read, GREATEST_END_SECTION, 0) != -1 ||
        endSection.matcher(read).find()) {
      data.append(classDetails);
      data.append(HTML_CLOSE);
      return true;
    }
    return false;
  }

  @Nullable
  static String parseContentEncoding(@NotNull String htmlLine) {
    if (htmlLine.contains("charset")) {
      Matcher matcher = CHARSET_META.matcher(htmlLine);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }
    return null;
  }

  private static void appendLine(StringBuilder buffer, String read) {
    buffer.append(read);
    buffer.append("\n");
  }

  private interface MyDocBuilder {
    void buildFromStream(String url, Reader input, StringBuilder result) throws IOException;
  }

  private static class MyJavadocFetcher implements Runnable {
    private static boolean ourFree = true;
    private final StringBuilder data = new StringBuilder();
    private final String url;
    private final MyDocBuilder myBuilder;
    private Exception myException;

    MyJavadocFetcher(String url, MyDocBuilder builder) {
      this.url = url;
      myBuilder = builder;
      //noinspection AssignmentToStaticFieldFromInstanceMethod
      ourFree = false;
    }

    @Override
    public void run() {
      try {
        if (url == null) {
          return;
        }

        if (url.startsWith(JAR_PROTOCOL)) {
          VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(BrowserUtil.getDocURL(url));
          if (file != null) {
            myBuilder.buildFromStream(url, new StringReader(VfsUtilCore.loadText(file)), data);
          }
        }
        else {
          URL parsedUrl = BrowserUtil.getURL(url);
          if (parsedUrl != null) {
            // gzip is disabled because in any case compressed JAR is downloaded
            HttpRequests.request(parsedUrl.toString()).gzip(false).connect(request -> {
              String contentEncoding = request.getConnection().getContentEncoding();

              byte[] bytes = request.readBytes(null);
              ByteArrayInputStream stream = new ByteArrayInputStream(bytes);

              if (contentEncoding == null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                  for (String htmlLine = reader.readLine(); htmlLine != null; htmlLine = reader.readLine()) {
                    contentEncoding = parseContentEncoding(htmlLine);
                    if (contentEncoding != null) {
                      break;
                    }
                  }
                }
              }

              myBuilder.buildFromStream(url, contentEncoding != null ? new MyReader(stream, contentEncoding) : new MyReader(stream), data);
              return null;
            });
          }
        }
      }
      catch (ProcessCanceledException ignored) { }
      catch (IOException e) {
        myException = e;
      }
      finally {
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        ourFree = true;
      }
    }
  }

  private static class MyReader extends InputStreamReader {
    private final ByteArrayInputStream myInputStream;

    MyReader(ByteArrayInputStream in) {
      super(in);
      in.reset();
      myInputStream = in;
    }

    MyReader(ByteArrayInputStream in, String charsetName) throws UnsupportedEncodingException {
      super(in, charsetName);
      in.reset();
      myInputStream = in;
    }
  }

  /**
   * Settings used for parsing of external documentation
   */
  protected static class ParseSettings {
    /**
     * Pattern defining the start of target fragment.
     */
    private final @NotNull Pattern startPattern;
    /**
     * Pattern defining the end of target fragment.
     */
    private final @NotNull Pattern endPattern;
    /**
     * If {@code false}, and line matching start pattern is not found, whole document will be processed.
     */
    private final boolean forcePatternSearch;
    /**
     * Replace table data by {@code <dt>}.
     */
    private final boolean useDt;

    public ParseSettings(@NotNull Pattern startPattern, @NotNull Pattern endPattern, boolean useDt, boolean forcePatternSearch) {
      this.startPattern = startPattern;
      this.endPattern = endPattern;
      this.useDt = useDt;
      this.forcePatternSearch = forcePatternSearch;
    }
  }
}