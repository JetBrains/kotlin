// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.fileTemplates.impl;

import com.intellij.diagnostic.PluginException;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplatesScheme;
import com.intellij.ide.fileTemplates.InternalTemplateBean;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.project.ProjectKt;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

@State(name = "FileTemplateManagerImpl", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class FileTemplateManagerImpl extends FileTemplateManager implements PersistentStateComponent<FileTemplateManagerImpl.State> {
  private static final Logger LOG = Logger.getInstance(FileTemplateManagerImpl.class);

  private final State myState = new State();
  private final ExportableFileTemplateSettings myDefaultSettings;
  private final Project myProject;

  private final FileTemplatesScheme myProjectScheme;
  private FileTemplatesScheme myScheme = FileTemplatesScheme.DEFAULT;
  private boolean myInitialized;

  public static FileTemplateManagerImpl getInstanceImpl(@NotNull Project project) {
    return (FileTemplateManagerImpl)getInstance(project);
  }

  FileTemplateManagerImpl(@NotNull Project project) {
    myDefaultSettings = ApplicationManager.getApplication().getService(ExportableFileTemplateSettings.class);
    myProject = project;

    myProjectScheme = project.isDefault() ? null : new FileTemplatesScheme("Project") {
      @NotNull
      @Override
      public String getTemplatesDir() {
        return FileUtilRt.toSystemDependentName(ProjectKt.getStateStore(project).getDirectoryStorePath(false) + "/" + TEMPLATES_DIR);
      }

      @NotNull
      @Override
      public Project getProject() {
        return project;
      }
    };
  }

  private FileTemplateSettings getSettings() {
    return myScheme == FileTemplatesScheme.DEFAULT ? myDefaultSettings : myProject.getService(FileTemplateSettings.class);
  }

  @NotNull
  @Override
  public FileTemplatesScheme getCurrentScheme() {
    return myScheme;
  }

  @Override
  public void setCurrentScheme(@NotNull FileTemplatesScheme scheme) {
    for (FTManager child : getAllManagers()) {
      child.saveTemplates();
    }
    setScheme(scheme);
  }

  private void setScheme(@NotNull FileTemplatesScheme scheme) {
    myScheme = scheme;
    myInitialized = true;
  }

  @NotNull
  @Override
  protected FileTemplateManager checkInitialized() {
    if (!myInitialized) {
      // loadState() not called; init default scheme
      setScheme(myScheme);
    }
    return this;
  }

  @Nullable
  @Override
  public FileTemplatesScheme getProjectScheme() {
    return myProjectScheme;
  }

  @NotNull
  @Override
  public FileTemplate[] getTemplates(@NotNull String category) {
    if (DEFAULT_TEMPLATES_CATEGORY.equals(category)) return ArrayUtil.mergeArrays(getInternalTemplates(), getAllTemplates());
    if (INCLUDES_TEMPLATES_CATEGORY.equals(category)) return getAllPatterns();
    if (CODE_TEMPLATES_CATEGORY.equals(category)) return getAllCodeTemplates();
    if (J2EE_TEMPLATES_CATEGORY.equals(category)) return getAllJ2eeTemplates();
    throw new IllegalArgumentException("Unknown category: " + category);
  }

  @Override
  @NotNull
  public FileTemplate[] getAllTemplates() {
    final Collection<FileTemplateBase> templates = getSettings().getDefaultTemplatesManager().getAllTemplates(false);
    return templates.toArray(FileTemplate.EMPTY_ARRAY);
  }

  @Override
  public FileTemplate getTemplate(@NotNull String templateName) {
    return getSettings().getDefaultTemplatesManager().findTemplateByName(templateName);
  }

  @Override
  @NotNull
  public FileTemplate addTemplate(@NotNull String name, @NotNull String extension) {
    return getSettings().getDefaultTemplatesManager().addTemplate(name, extension);
  }

  @Override
  public void removeTemplate(@NotNull FileTemplate template) {
    final String qName = ((FileTemplateBase)template).getQualifiedName();
    for (FTManager manager : getAllManagers()) {
      manager.removeTemplate(qName);
    }
  }

  @Override
  @NotNull
  public Properties getDefaultProperties() {
    @NonNls Properties props = new Properties();

    Calendar calendar = Calendar.getInstance();
    Date date = myTestDate == null ? calendar.getTime() : myTestDate;
    SimpleDateFormat sdfMonthNameShort = new SimpleDateFormat("MMM");
    SimpleDateFormat sdfMonthNameFull = new SimpleDateFormat("MMMM");
    SimpleDateFormat sdfDayNameShort = new SimpleDateFormat("EEE");
    SimpleDateFormat sdfDayNameFull = new SimpleDateFormat("EEEE");
    SimpleDateFormat sdfYearFull = new SimpleDateFormat("yyyy");

    props.setProperty("DATE", DateFormatUtil.formatDate(date));
    props.setProperty("TIME", DateFormatUtil.formatTime(date));
    props.setProperty("YEAR", sdfYearFull.format(date));
    props.setProperty("MONTH", getCalendarValue(calendar, Calendar.MONTH));
    props.setProperty("MONTH_NAME_SHORT", sdfMonthNameShort.format(date));
    props.setProperty("MONTH_NAME_FULL", sdfMonthNameFull.format(date));
    props.setProperty("DAY", getCalendarValue(calendar, Calendar.DAY_OF_MONTH));
    props.setProperty("DAY_NAME_SHORT", sdfDayNameShort.format(date));
    props.setProperty("DAY_NAME_FULL", sdfDayNameFull.format(date));
    props.setProperty("HOUR", getCalendarValue(calendar, Calendar.HOUR_OF_DAY));
    props.setProperty("MINUTE", getCalendarValue(calendar, Calendar.MINUTE));
    props.setProperty("SECOND", getCalendarValue(calendar, Calendar.SECOND));

    props.setProperty("USER", SystemProperties.getUserName());
    props.setProperty("PRODUCT_NAME", ApplicationNamesInfo.getInstance().getFullProductName());

    props.setProperty("DS", "$"); // Dollar sign, strongly needed for PHP, JS, etc. See WI-8979

    props.setProperty(PROJECT_NAME_VARIABLE, myProject.getName());

    return props;
  }

  @NotNull
  private static String getCalendarValue(final Calendar calendar, final int field) {
    int val = calendar.get(field);
    if (field == Calendar.MONTH) val++;
    final String result = Integer.toString(val);
    if (result.length() == 1) {
      return "0" + result;
    }
    return result;
  }

  @Override
  @NotNull
  public Collection<String> getRecentNames() {
    validateRecentNames(); // todo: no need to do it lazily
    return myState.getRecentNames();
  }

  @Override
  public void addRecentName(@NotNull @NonNls String name) {
    myState.addName(name);
  }

  private void validateRecentNames() {
    final Collection<FileTemplateBase> allTemplates = getSettings().getDefaultTemplatesManager().getAllTemplates(false);
    final List<String> allNames = new ArrayList<>(allTemplates.size());
    for (FileTemplate fileTemplate : allTemplates) {
      allNames.add(fileTemplate.getName());
    }
    myState.validateNames(allNames);
  }

  @Override
  @NotNull
  public FileTemplate[] getInternalTemplates() {
    List<InternalTemplateBean> internalTemplateBeans = InternalTemplateBean.EP_NAME.getExtensionList();
    List<FileTemplate> result = new ArrayList<>(internalTemplateBeans.size());
    for (InternalTemplateBean bean : internalTemplateBeans) {
      try {
        result.add(getInternalTemplate(bean.name));
      }
      catch (Exception e) {
        LOG.error("Can't find template " + bean.name, new PluginException(e, bean.getPluginId()));
      }
    }
    return result.toArray(FileTemplate.EMPTY_ARRAY);
  }

  @NotNull
  @Override
  public FileTemplate getInternalTemplate(@NotNull @NonNls String templateName) {
    FileTemplateBase template = (FileTemplateBase)findInternalTemplate(templateName);

    if (template == null) {
      template = (FileTemplateBase)getJ2eeTemplate(templateName); // Hack to be able to register class templates from the plugin.
      template.setReformatCode(true);
    }
    return template;
  }

  @Override
  public FileTemplate findInternalTemplate(@NotNull @NonNls String templateName) {
    FileTemplateBase template = getSettings().getInternalTemplatesManager().findTemplateByName(templateName);

    if (template == null) {
      // todo: review the hack and try to get rid of this weird logic completely
      template = getSettings().getDefaultTemplatesManager().findTemplateByName(templateName);
    }
    return template;
  }

  @Override
  @NotNull
  public String internalTemplateToSubject(@NotNull @NonNls String templateName) {
    for(InternalTemplateBean bean: InternalTemplateBean.EP_NAME.getExtensionList()) {
      if (bean.name.equals(templateName) && bean.subject != null) {
        return bean.subject;
      }
    }
    return StringUtil.toLowerCase(templateName);
  }

  @NotNull
  @Override
  public FileTemplate getCodeTemplate(@NotNull @NonNls String templateName) {
    return getTemplateFromManager(templateName, getSettings().getCodeTemplatesManager());
  }

  @NotNull
  @Override
  public FileTemplate getJ2eeTemplate(@NotNull @NonNls String templateName) {
    return getTemplateFromManager(templateName, getSettings().getJ2eeTemplatesManager());
  }

  @NotNull
  private static FileTemplate getTemplateFromManager(@NotNull final String templateName, @NotNull final FTManager ftManager) {
    FileTemplateBase template = ftManager.getTemplate(templateName);
    if (template != null) {
      return template;
    }
    template = ftManager.findTemplateByName(templateName);
    if (template != null) {
      return template;
    }

    throw new IllegalStateException("Template not found: " + templateName);
  }

  @Override
  @NotNull
  public FileTemplate getDefaultTemplate(@NotNull final String name) {
    final String templateQName = getQualifiedName(name);

    for (FTManager manager : getAllManagers()) {
      FileTemplateBase template = manager.getTemplate(templateQName);
      if (template != null) {
        if (template instanceof BundledFileTemplate) {
          template = ((BundledFileTemplate)template).clone();
          ((BundledFileTemplate)template).revertToDefaults();
        }
        return template;
      }
    }

    String message = "Default template not found: " + name;
    LOG.error(message);
    throw new RuntimeException(message);
  }

  @NotNull
  private String getQualifiedName(@NotNull String name) {
    return FileTypeManagerEx.getInstanceEx().getExtension(name).isEmpty() ? FileTemplateBase.getQualifiedName(name, "java") : name;
  }

  @Override
  @NotNull
  public FileTemplate[] getAllPatterns() {
    final Collection<FileTemplateBase> allTemplates = getSettings().getPatternsManager().getAllTemplates(false);
    return allTemplates.toArray(FileTemplate.EMPTY_ARRAY);
  }

  @Override
  public FileTemplate getPattern(@NotNull String name) {
    return getSettings().getPatternsManager().findTemplateByName(name);
  }

  @Override
  @NotNull
  public FileTemplate[] getAllCodeTemplates() {
    final Collection<FileTemplateBase> templates = getSettings().getCodeTemplatesManager().getAllTemplates(false);
    return templates.toArray(FileTemplate.EMPTY_ARRAY);
  }

  @Override
  @NotNull
  public FileTemplate[] getAllJ2eeTemplates() {
    final Collection<FileTemplateBase> templates = getSettings().getJ2eeTemplatesManager().getAllTemplates(false);
    return templates.toArray(FileTemplate.EMPTY_ARRAY);
  }

  @Override
  public void setTemplates(@NotNull String templatesCategory, @NotNull Collection<? extends FileTemplate> templates) {
    for (FTManager manager : getAllManagers()) {
      if (templatesCategory.equals(manager.getName())) {
        manager.updateTemplates(templates);
        break;
      }
    }
  }

  @Override
  public void saveAllTemplates() {
    for (FTManager manager : getAllManagers()) {
      manager.saveTemplates();
    }
  }

  public URL getDefaultTemplateDescription() {
    return myDefaultSettings.getDefaultTemplateDescription();
  }

  URL getDefaultIncludeDescription() {
    return myDefaultSettings.getDefaultIncludeDescription();
  }

  private Date myTestDate;

  @TestOnly
  public void setTestDate(Date testDate) {
    myTestDate = testDate;
  }

  @NotNull
  @Override
  public State getState() {
    myState.SCHEME = myScheme.getName();
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    XmlSerializerUtil.copyBean(state, myState);
    FileTemplatesScheme scheme = myProjectScheme != null && myProjectScheme.getName().equals(state.SCHEME) ? myProjectScheme : FileTemplatesScheme.DEFAULT;
    setScheme(scheme);
  }

  private FTManager[] getAllManagers() {
    return getSettings().getAllManagers();
  }

  @TestOnly
  public void setDefaultFileIncludeTemplateTextTemporarilyForTest(String simpleName, String text, @NotNull Disposable parentDisposable) {
    FTManager defaultTemplatesManager = getSettings().getPatternsManager();
    String qName = getQualifiedName(simpleName);
    FileTemplateBase oldTemplate = defaultTemplatesManager.getTemplate(qName);
    Map<String, FileTemplateBase> templates = defaultTemplatesManager.getTemplates();
    templates.put(qName, new FileTemplateBase() {
      @NotNull
      @Override
      public String getName() {
        return simpleName;
      }

      @Override
      public void setName(@NotNull String name) {
        throw new AbstractMethodError();
      }

      @Override
      public boolean isDefault() {
        return true;
      }

      @NotNull
      @Override
      public String getDescription() {
        throw new AbstractMethodError();
      }

      @NotNull
      @Override
      public String getExtension() {
        return qName.substring(simpleName.length());
      }

      @Override
      public void setExtension(@NotNull String extension) {
        throw new AbstractMethodError();
      }

      @NotNull
      @Override
      protected String getDefaultText() {
        return text;
      }
    });
    Disposer.register(parentDisposable, () -> templates.put(qName, oldTemplate));
  }

  public static class State {
    public List<String> RECENT_TEMPLATES = new ArrayList<>();
    public String SCHEME = FileTemplatesScheme.DEFAULT.getName();

    public void addName(@NotNull @NonNls String name) {
      RECENT_TEMPLATES.remove(name);
      RECENT_TEMPLATES.add(name);
    }

    @NotNull
    Collection<String> getRecentNames() {
      int size = RECENT_TEMPLATES.size();
      int resultSize = Math.min(FileTemplateManager.RECENT_TEMPLATES_SIZE, size);
      return RECENT_TEMPLATES.subList(size - resultSize, size);
    }

    void validateNames(List<String> validNames) {
      RECENT_TEMPLATES.retainAll(validNames);
    }
  }
}
