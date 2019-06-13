// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.fileTemplates;

import com.intellij.application.options.CodeStyle;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.fileTemplates.impl.CustomFileTemplate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ClassLoaderUtil;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.TIntObjectHashMap;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.Token;
import org.apache.velocity.runtime.parser.node.*;
import org.apache.velocity.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author MYakovlev
 */
public class FileTemplateUtil {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.fileTemplates.FileTemplateUtil");
  private static final CreateFromTemplateHandler DEFAULT_HANDLER = new DefaultCreateFromTemplateHandler();

  @NotNull
  public static String[] calculateAttributes(@NotNull String templateContent, @NotNull Properties properties, boolean includeDummies, @NotNull Project project) throws ParseException {
    Set<String> propertiesNames = new HashSet<>();
    for (Enumeration e = properties.propertyNames(); e.hasMoreElements(); ) {
      propertiesNames.add((String)e.nextElement());
    }
    return calculateAttributes(templateContent, propertiesNames, includeDummies, project);
  }

  public static String[] calculateAttributes(@NotNull String templateContent, @NotNull Map<String, Object> properties, boolean includeDummies, @NotNull Project project) throws ParseException {
    return calculateAttributes(templateContent, properties.keySet(), includeDummies, project);
  }

  @NotNull
  private static String[] calculateAttributes(@NotNull String templateContent, @NotNull Set<String> propertiesNames, boolean includeDummies, @NotNull Project project) throws ParseException {
    final Set<String> unsetAttributes = new LinkedHashSet<>();
    final Set<String> definedAttributes = new HashSet<>();
    SimpleNode template = VelocityTemplateContext.withContext(project, ()->VelocityWrapper.parse(new StringReader(templateContent), "MyTemplate"));
    collectAttributes(unsetAttributes, definedAttributes, template, propertiesNames, includeDummies, new HashSet<>(), project);
    for (String definedAttribute : definedAttributes) {
      unsetAttributes.remove(definedAttribute);
    }
    return ArrayUtilRt.toStringArray(unsetAttributes);
  }

  private static void collectAttributes(@NotNull Set<? super String> referenced,
                                        @NotNull Set<? super String> defined,
                                        @NotNull Node apacheNode,
                                        @NotNull Set<String> propertiesNames,
                                        final boolean includeDummies,
                                        @NotNull Set<? super String> visitedIncludes,
                                        @NotNull Project project) throws ParseException {
    int childCount = apacheNode.jjtGetNumChildren();
    for (int i = 0; i < childCount; i++) {
      Node apacheChild = apacheNode.jjtGetChild(i);
      collectAttributes(referenced, defined, apacheChild, propertiesNames, includeDummies, visitedIncludes, project);
      if (apacheChild instanceof ASTReference) {
        ASTReference apacheReference = (ASTReference)apacheChild;
        String s = apacheReference.literal();
        s = referenceToAttribute(s, includeDummies);
        if (s != null && s.length() > 0 && !propertiesNames.contains(s)) {
          referenced.add(s);
        }
      }
      else if (apacheChild instanceof ASTSetDirective) {
        ASTReference lhs = (ASTReference)apacheChild.jjtGetChild(0);
        String attr = referenceToAttribute(lhs.literal(), false);
        if (attr != null) {
          defined.add(attr);
        }
      }
      else if (apacheChild instanceof ASTDirective && "parse".equals(((ASTDirective)apacheChild).getDirectiveName()) && apacheChild.jjtGetNumChildren() == 1) {
        Node literal = apacheChild.jjtGetChild(0);
        if (literal instanceof ASTStringLiteral && literal.jjtGetNumChildren() == 0) {
          Token firstToken = literal.getFirstToken();
          if (firstToken != null) {
            String s = StringUtil.unquoteString(firstToken.toString());
            FileTemplateManager templateManager = FileTemplateManager.getInstance(project);
            FileTemplate includedTemplate = templateManager.getTemplate(s);
            if (includedTemplate == null) {
              includedTemplate = templateManager.getPattern(s);
            }
            if (includedTemplate != null && visitedIncludes.add(s)) {
              SimpleNode template = VelocityWrapper.parse(new StringReader(includedTemplate.getText()), "MyTemplate");
              collectAttributes(referenced, defined, template, propertiesNames, includeDummies, visitedIncludes, project);
            }
          }
        }
      }
    }
  }


  /**
   * Removes each two leading '\', removes leading $, removes {}
   * Examples:
   * $qqq   -> qqq
   * \$qqq  -> qqq if dummy attributes are collected too, null otherwise
   * \\$qqq -> qqq
   * ${qqq} -> qqq
   */
  @Nullable
  private static String referenceToAttribute(@NotNull String attrib, boolean includeDummies) {
    while (attrib.startsWith("\\\\")) {
      attrib = attrib.substring(2);
    }
    if (attrib.startsWith("\\$")) {
      if (includeDummies) {
        attrib = attrib.substring(1);
      }
      else return null;
    }
    if (!StringUtil.startsWithChar(attrib, '$')) {
      return null;
    }
    attrib = attrib.substring(1);
    if (StringUtil.startsWithChar(attrib, '{')) {
      String cleanAttribute = null;
      for (int i = 1; i < attrib.length(); i++) {
        char currChar = attrib.charAt(i);
        if (currChar == '{' || currChar == '.') {
          // Invalid match
          cleanAttribute = null;
          break;
        }
        else if (currChar == '}') {
          // Valid match
          cleanAttribute = attrib.substring(1, i);
          break;
        }
      }
      attrib = cleanAttribute;
    }
    else {
      for (int i = 0; i < attrib.length(); i++) {
        char currChar = attrib.charAt(i);
        if (currChar == '{' || currChar == '}' || currChar == '.') {
          attrib = attrib.substring(0, i);
          break;
        }
      }
    }
    return attrib;
  }

  @NotNull
  public static String mergeTemplate(@NotNull Map attributes, @NotNull String content, boolean useSystemLineSeparators) throws IOException {
    VelocityContext context = createVelocityContext();
    for (final Object o : attributes.keySet()) {
      String name = (String)o;
      context.put(name, attributes.get(name));
    }
    return mergeTemplate(content, context, useSystemLineSeparators, null);
  }

  @NotNull
  private static VelocityContext createVelocityContext() {
    VelocityContext context = new VelocityContext();
    context.put("StringUtils", StringUtils.class);
    return context;
  }

  @NotNull
  public static String mergeTemplate(@NotNull Properties attributes, @NotNull String content, boolean useSystemLineSeparators) throws IOException {
    return mergeTemplate(attributes, content, useSystemLineSeparators, null);
  }

  @NotNull
  public static String mergeTemplate(@NotNull Properties attributes, @NotNull String content, boolean useSystemLineSeparators,
                                     @Nullable Consumer<? super VelocityException> exceptionHandler) {
    VelocityContext context = createVelocityContext();
    Enumeration<?> names = attributes.propertyNames();
    while (names.hasMoreElements()) {
      String name = (String)names.nextElement();
      context.put(name, attributes.getProperty(name));
    }
    return mergeTemplate(content, context, useSystemLineSeparators, exceptionHandler);
  }

  @NotNull
  private static String mergeTemplate(String templateContent, final VelocityContext context, boolean useSystemLineSeparators,
                                      @Nullable Consumer<? super VelocityException> exceptionHandler) {
    final StringWriter stringWriter = new StringWriter();
    try {
      Project project;
      final Object projectName = context.get(FileTemplateManager.PROJECT_NAME_VARIABLE);
      if (projectName instanceof String) {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        project = ContainerUtil.find(projects, project1 -> projectName.equals(project1.getName()));
      }
      else {
        project = null;
      }
      VelocityTemplateContext.withContext(project, ()->VelocityWrapper.evaluate(project, context, stringWriter, templateContent));
    }
    catch (final VelocityException e) {
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        LOG.error(e);
      }
      LOG.info("Error evaluating template:\n" + templateContent, e);
      if (exceptionHandler == null) {
        ApplicationManager.getApplication()
          .invokeLater(() -> Messages.showErrorDialog(IdeBundle.message("error.parsing.file.template", e.getMessage()),
                                                      IdeBundle.message("title.velocity.error")));
      }
      else {
        exceptionHandler.consume(e);
      }
    }
    final String result = stringWriter.toString();

    if (useSystemLineSeparators) {
      final String newSeparator = CodeStyle.getDefaultSettings().getLineSeparator();
      if (!"\n".equals(newSeparator)) {
        return StringUtil.convertLineSeparators(result, newSeparator);
      }
    }

    return result;
  }

  @NotNull
  public static PsiElement createFromTemplate(@NotNull final FileTemplate template,
                                              @Nullable final String fileName,
                                              @Nullable Properties props,
                                              @NotNull final PsiDirectory directory) throws Exception {
    Map<String, Object> map;
    if (props != null) {
      map = new HashMap<>();
      putAll(map, props);
    }
    else {
      map = null;
    }
    return createFromTemplate(template, fileName, map, directory, null);
  }

  @NotNull
  public static PsiElement createFromTemplate(@NotNull final FileTemplate template,
                                              @Nullable String fileName,
                                              @Nullable Properties props,
                                              @NotNull final PsiDirectory directory,
                                              @Nullable ClassLoader classLoader) throws Exception {
    Map<String, Object> map;
    if (props != null) {
      map = new HashMap<>();
      putAll(map, props);
    }
    else {
      map = null;
    }
    return createFromTemplate(template, fileName, map, directory, classLoader);
  }

  @NotNull
  public static PsiElement createFromTemplate(@NotNull final FileTemplate template,
                                              @Nullable String fileName,
                                              @Nullable Map<String, Object> propsMap,
                                              @NotNull final PsiDirectory directory,
                                              @Nullable ClassLoader classLoader) throws Exception {
    Project project = directory.getProject();
    FileTemplateManager.getInstance(project).addRecentName(template.getName());

    if (propsMap == null) {
      Properties p = FileTemplateManager.getInstance(project).getDefaultProperties();
      propsMap = new HashMap<>();
      putAll(propsMap, p);
    }

    Properties p = new Properties();
    fillDefaultProperties(p, directory);
    putAll(propsMap, p);

    final CreateFromTemplateHandler handler = findHandler(template);
    if (fileName != null && propsMap.get(FileTemplate.ATTRIBUTE_NAME) == null) {
      propsMap.put(FileTemplate.ATTRIBUTE_NAME, fileName);
    }
    else if (fileName == null && handler.isNameRequired()) {
      fileName = (String)propsMap.get(FileTemplate.ATTRIBUTE_NAME);
      if (fileName == null) {
        throw new Exception("File name must be specified");
      }
    }
    propsMap.put(FileTemplate.ATTRIBUTE_FILE_NAME, fileName + (StringUtil.isEmpty(template.getExtension()) ? "" : "." + template.getExtension()));
    String dirPath = getDirPathRelativeToProjectBaseDir(directory);
    if (dirPath != null) {
      propsMap.put(FileTemplate.ATTRIBUTE_DIR_PATH, dirPath);
    }

    //Set escaped references to dummy values to remove leading "\" (if not already explicitly set)
    String[] dummyRefs = calculateAttributes(template.getText(), propsMap, true, directory.getProject());
    for (String dummyRef : dummyRefs) {
      propsMap.put(dummyRef, "");
    }

    handler.prepareProperties(propsMap, fileName, template);
    handler.prepareProperties(propsMap);

    Map<String, Object> props_ = propsMap;
    String fileName_ = fileName;
    String mergedText = ClassLoaderUtil.computeWithClassLoader(
      classLoader != null ? classLoader : FileTemplateUtil.class.getClassLoader(),
      (ThrowableComputable<String, IOException>)() -> template.getText(props_));
    String templateText = StringUtil.convertLineSeparators(mergedText);

    return WriteCommandAction
        .writeCommandAction(project)
        .withName(handler.commandName(template))
        .compute(()->handler.createFromTemplate(project, directory, fileName_, template, templateText, props_));
  }

  @Nullable
  private static String getDirPathRelativeToProjectBaseDir(@NotNull PsiDirectory directory) {
    VirtualFile baseDir = directory.getProject().getBaseDir();
    return baseDir != null ? VfsUtilCore.getRelativePath(directory.getVirtualFile(), baseDir) : null;
  }

  @NotNull
  public static CreateFromTemplateHandler findHandler(@NotNull FileTemplate template) {
    for (CreateFromTemplateHandler handler : CreateFromTemplateHandler.EP_NAME.getExtensionList()) {
      if (handler.handlesTemplate(template)) {
        return handler;
      }
    }
    return DEFAULT_HANDLER;
  }

  public static void fillDefaultProperties(@NotNull Properties props, @NotNull PsiDirectory directory) {
    for (DefaultTemplatePropertiesProvider provider : DefaultTemplatePropertiesProvider.EP_NAME.getExtensionList()) {
      provider.fillProperties(directory, props);
    }
    props.setProperty(FileTemplate.ATTRIBUTE_FILE_NAME, "");
    props.setProperty(FileTemplate.ATTRIBUTE_DIR_PATH, "");
  }

  @NotNull
  public static String indent(@NotNull String methodText, @NotNull Project project, FileType fileType) {
    int indent = CodeStyle.getSettings(project).getIndentSize(fileType);
    return methodText.replaceAll("\n", "\n" + StringUtil.repeatSymbol(' ', indent));
  }

  public static boolean canCreateFromTemplate(@NotNull PsiDirectory[] dirs, @NotNull FileTemplate template) {
    FileType fileType = getFileType(template);
    if (fileType.equals(FileTypes.UNKNOWN)) return false;
    CreateFromTemplateHandler handler = findHandler(template);
    return handler.canCreate(dirs);
  }

  @NotNull
  protected static FileType getFileType(@NotNull FileTemplate template) {
    FileType fileType = FileTypeManagerEx.getInstanceEx().getFileTypeByExtension(template.getExtension());
    if (fileType.equals(FileTypes.UNKNOWN)) {
      return FileTypeManagerEx.getInstanceEx().getFileTypeByExtension(FileUtilRt.getExtension(template.getExtension()));
    }
    return fileType;
  }

  @Nullable
  public static Icon getIcon(@NotNull FileTemplate fileTemplate) {
    return getFileType(fileTemplate).getIcon();
  }

  public static void putAll(@NotNull Map<String, Object> props, @NotNull Properties p) {
    for (Enumeration<?> e = p.propertyNames(); e.hasMoreElements(); ) {
      String s = (String)e.nextElement();
      props.putIfAbsent(s, p.getProperty(s));
    }
  }

  @NotNull
  public static FileTemplate createTemplate(@NotNull String prefName,
                                            @NotNull String extension,
                                            @NotNull String content,
                                            @NotNull FileTemplate[] templates) {
    final Set<String> names = new HashSet<>();
    for (FileTemplate template : templates) {
      names.add(template.getName());
    }
    String name = prefName;
    int i = 0;
    while (names.contains(name)) {
      name = prefName + " (" + ++i + ")";
    }
    final FileTemplate newTemplate = new CustomFileTemplate(name, extension);
    newTemplate.setText(content);
    return newTemplate;
  }

  @NotNull
  public static Pattern getTemplatePattern(@NotNull FileTemplate template,
                                           @NotNull Project project,
                                           @NotNull TIntObjectHashMap<String> offsetToProperty) {
    String templateText = template.getText().trim();
    String regex = templateToRegex(templateText, offsetToProperty, project);
    regex = StringUtil.replace(regex, "with", "(?:with|by)");
    regex = ".*(" + regex + ").*";
    return Pattern.compile(regex, Pattern.DOTALL);
  }

  @NotNull
  private static String templateToRegex(@NotNull String text, @NotNull TIntObjectHashMap<String> offsetToProperty, @NotNull Project project) {
    List<Object> properties = new ArrayList<>(FileTemplateManager.getInstance(project).getDefaultProperties().keySet());
    properties.add(FileTemplate.ATTRIBUTE_PACKAGE_NAME);

    String regex = escapeRegexChars(text);
    // first group is a whole file header
    int groupNumber = 1;
    for (Object property : properties) {
      String name = property.toString();
      String escaped = escapeRegexChars("${" + name + "}");
      boolean first = true;
      for (int i = regex.indexOf(escaped); i != -1 && i < regex.length(); i = regex.indexOf(escaped, i + 1)) {
        String replacement = first ? "([^\\n]*)" : "\\" + groupNumber;
        int delta = escaped.length() - replacement.length();
        int[] offs = offsetToProperty.keys();
        for (int off : offs) {
          if (off > i) {
            String prop = offsetToProperty.remove(off);
            offsetToProperty.put(off - delta, prop);
          }
        }
        offsetToProperty.put(i, name);
        regex = regex.substring(0, i) + replacement + regex.substring(i + escaped.length());
        if (first) {
          groupNumber++;
          first = false;
        }
      }
    }
    return regex;
  }

  @NotNull
  private static String escapeRegexChars(@NotNull String regex) {
    regex = StringUtil.replace(regex, "|", "\\|");
    regex = StringUtil.replace(regex, ".", "\\.");
    regex = StringUtil.replace(regex, "*", "\\*");
    regex = StringUtil.replace(regex, "+", "\\+");
    regex = StringUtil.replace(regex, "?", "\\?");
    regex = StringUtil.replace(regex, "$", "\\$");
    regex = StringUtil.replace(regex, "(", "\\(");
    regex = StringUtil.replace(regex, ")", "\\)");
    regex = StringUtil.replace(regex, "[", "\\[");
    regex = StringUtil.replace(regex, "]", "\\]");
    regex = StringUtil.replace(regex, "{", "\\{");
    regex = StringUtil.replace(regex, "}", "\\}");
    return regex;
  }
}