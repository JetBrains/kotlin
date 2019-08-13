// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.macro;

import com.intellij.ide.DataManager;
import com.intellij.ide.macro.Macro.ExecutionCancelledException;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ConvertingIterator;
import com.intellij.util.containers.Convertor;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public final class MacroManager {
  private final Map<String, Macro> myMacrosMap = new LinkedHashMap<>();

  private static final Pattern MACRO_PATTERN = Pattern.compile("\\$.+\\$");

  public static MacroManager getInstance() {
    return ServiceManager.getService(MacroManager.class);
  }

  private MacroManager() {
    registerMacro(new SourcepathMacro());
    registerMacro(new FileDirMacro());
    registerMacro(new FileDirNameMacro());
    registerMacro(new FileParentDirMacro());
    registerMacro(new FileDirPathFromParentMacro());
    registerMacro(new FileExtMacro());
    registerMacro(new FileNameMacro());
    registerMacro(new FileNameWithoutExtension());
    registerMacro(new FileNameWithoutAllExtensions());
    registerMacro(new FilePathMacro());
    registerMacro(new UnixSeparatorsMacro());
    registerMacro(new FileEncodingMacro());
    registerMacro(new FileDirRelativeToProjectRootMacro());
    registerMacro(new FilePathRelativeToProjectRootMacro());
    registerMacro(new FileDirRelativeToSourcepathMacro());
    registerMacro(new FilePathRelativeToSourcepathMacro());
    registerMacro(new JdkPathMacro());
    registerMacro(new PromptMacro());
    registerMacro(new FilePromptMacro());
    registerMacro(new SourcepathEntryMacro());
    registerMacro(new ProjectFileDirMacro());
    registerMacro(new ProjectNameMacro());
    registerMacro(new ProjectPathMacro());
    registerMacro(new ContentRootMacro());

    registerMacro(new ModuleFilePathMacro());
    registerMacro(new ModuleFileDirMacro());
    registerMacro(new ModuleNameMacro());
    registerMacro(new AffectedModuleNamesMacro());
    registerMacro(new CompilerContextMakeMacro());
    registerMacro(new ModulePathMacro());
    registerMacro(new ModuleSdkPathMacro());

    registerMacro(new FileRelativePathMacro());
    registerMacro(new FileRelativeDirMacro());
    registerMacro(new LineNumberMacro());
    registerMacro(new ColumnNumberMacro());

    registerMacro(new ClipboardContentMacro());
    registerMacro(new SelectedTextMacro());
    registerMacro(new SelectionStartLineMacro());
    registerMacro(new SelectionStartColumnMacro());
    registerMacro(new SelectionEndLineMacro());
    registerMacro(new SelectionEndColumnMacro());

    if (File.separatorChar != '/') {
      registerMacro(new FileDirRelativeToProjectRootMacro2());
      registerMacro(new FilePathRelativeToProjectRootMacro2());
      registerMacro(new FileDirRelativeToSourcepathMacro2());
      registerMacro(new FilePathRelativeToSourcepathMacro2());
      registerMacro(new FileRelativeDirMacro2());
      registerMacro(new FileRelativePathMacro2());
    }
    for (Macro macro : Macro.EP_NAME.getExtensionList()) {
      registerMacro(macro);
    }
  }

  private void registerMacro(Macro macro) {
    myMacrosMap.put(macro.getName(), macro);
  }

  public Collection<Macro> getMacros() {
    return myMacrosMap.values();
  }

  @NotNull
  public Set<String> getMacroNames() {
    return myMacrosMap.keySet();
  }

  public void cacheMacrosPreview(DataContext dataContext) {
    dataContext = getCorrectContext(dataContext);
    for (Macro macro : getMacros()) {
      macro.cachePreview(dataContext);
    }
  }

  private static DataContext getCorrectContext(DataContext dataContext) {
    if (PlatformDataKeys.FILE_EDITOR.getData(dataContext) != null) {
      return dataContext;
    }
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return dataContext;
    }
    FileEditorManager editorManager = FileEditorManager.getInstance(project);
    VirtualFile[] files = editorManager.getSelectedFiles();
    if (files.length == 0) {
      return dataContext;
    }
    FileEditor fileEditor = editorManager.getSelectedEditor(files[0]);
    return fileEditor == null ? dataContext : DataManager.getInstance().getDataContext(fileEditor.getComponent());
  }

  public static boolean containsMacros(@Nullable String str) {
    if (str == null) return false;
    return MACRO_PATTERN.matcher(str).find();
  }

  /**
   * Expands all macros that are found in the {@code str}.
   */
  @Nullable
  public String expandMacrosInString(@Nullable String str, boolean firstQueueExpand, DataContext dataContext)
    throws ExecutionCancelledException {
    return expandMacrosInString(str, firstQueueExpand, dataContext, "", false);
  }

  @Nullable
  public String expandSilentMacros(@Nullable String str, boolean firstQueueExpand, DataContext dataContext)
    throws ExecutionCancelledException {
    return expandMacrosInString(str, firstQueueExpand, dataContext, "", true);
  }

  /**
   * Expand macros in a string.
   * @param str string possibly containing macros
   * @param firstQueueExpand expand only macros that does not implement {@link SecondQueueExpandMacro}
   * @param dataContext data context used for macro expansion
   * @param defaultExpandValue if macro is expended to null, {@code defaultExpandValue} will be used instead
   * @param onlySilent does not expand macros that may require interaction with user; {@code defaultExpandValue} will be used for such macros
   * @return string with macros expanded or null if some macro is expanded to null and {@code defaultExpandValue} is null
   * @throws ExecutionCancelledException
   */
  @Nullable
  public String expandMacrosInString(@Nullable String str,
                                     boolean firstQueueExpand,
                                     DataContext dataContext,
                                     @Nullable String defaultExpandValue,
                                     boolean onlySilent) throws ExecutionCancelledException {
    Iterator<Macro> macros = getMacros().iterator();
    if (onlySilent) {
      Convertor<Macro, Macro> convertor = macro -> macro instanceof PromptingMacro ? new Macro.Silent(macro, defaultExpandValue) : macro;
      macros = ConvertingIterator.create(getMacros().iterator(), convertor);
    }
    return expandMacroSet(str, firstQueueExpand, dataContext, macros, defaultExpandValue);
  }

  @Nullable
  private static String expandMacroSet(@Nullable String str,
                                       boolean firstQueueExpand,
                                       DataContext dataContext,
                                       Iterator<? extends Macro> macros,
                                       @Nullable String defaultExpandValue) throws ExecutionCancelledException {
    if (str == null) return null;
    while (macros.hasNext()) {
      Macro macro = macros.next();
      if (macro instanceof SecondQueueExpandMacro && firstQueueExpand) continue;
      String name = "$" + macro.getName() + "$";
      String macroNameWithParamStart = "$" + macro.getName() + "(";
      if (str.contains(name)) {
        for (int index = str.indexOf(name);
             index != -1 && index <= str.length() - name.length();
             index = str.indexOf(name, index)) {
          String expanded = macro.expand(dataContext);
          //if (dataContext instanceof DataManagerImpl.MyDataContext) {
          //  // hack: macro.expand() can cause UI events such as showing dialogs ('Prompt' macro) which may 'invalidate' the datacontext
          //  // since we know exactly that context is valid, we need to update its event count
          //  ((DataManagerImpl.MyDataContext)dataContext).setEventCount(IdeEventQueue.getInstance().getEventCount());
          //}
          expanded = expanded == null ? defaultExpandValue : expanded;
          if (expanded == null) {
            return null;
          }
          str = str.substring(0, index) + expanded + str.substring(index + name.length());
          index += expanded.length();
        }
      }
      else if(str.contains(macroNameWithParamStart)) {
        String macroNameWithParamEnd = ")$";
        Map<String, String> toReplace = null;
        int i = str.indexOf(macroNameWithParamStart);
        while (i != -1) {
          int j = str.indexOf(macroNameWithParamEnd, i + macroNameWithParamStart.length());
          if(j > i) {
            String param = str.substring(i + macroNameWithParamStart.length(), j);
            if(toReplace == null) toReplace = new THashMap<>();
            String expanded = macro.expand(dataContext, param);
            expanded = expanded == null ? defaultExpandValue : expanded;
            if (expanded == null) {
              return null;
            }
            toReplace.put(macroNameWithParamStart + param + macroNameWithParamEnd, expanded);
            i = j + macroNameWithParamEnd.length();
          } else {
            break;
          }
        }
        if(toReplace !=null) {
          for (Map.Entry<String, String> entry : toReplace.entrySet()) {
            str = StringUtil.replace(str, entry.getKey(), entry.getValue());
          }
        }
      }
    }
    return str;
  }
}
