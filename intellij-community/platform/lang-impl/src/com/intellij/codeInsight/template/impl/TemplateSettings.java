// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.AbstractBundle;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.openapi.application.ex.DecodeDefaultsUtil;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.BaseSchemeProcessor;
import com.intellij.openapi.options.SchemeManager;
import com.intellij.openapi.options.SchemeManagerFactory;
import com.intellij.openapi.options.SchemeState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.serviceContainer.NonInjectable;
import com.intellij.util.SmartList;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.xmlb.Converter;
import com.intellij.util.xmlb.annotations.OptionTag;
import kotlin.Lazy;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@State(
  name = "TemplateSettings",
  storages = @Storage("templates.xml"),
  additionalExportFile = TemplateSettings.TEMPLATES_DIR_PATH
)
public final class TemplateSettings implements PersistentStateComponent<TemplateSettings.State> {
  private static final Logger LOG = Logger.getInstance(TemplateSettings.class);

  @NonNls public static final String USER_GROUP_NAME = "user";
  @NonNls private static final String TEMPLATE_SET = "templateSet";
  @NonNls private static final String GROUP = "group";
  @NonNls public static final String TEMPLATE = "template";

  public static final char SPACE_CHAR = ' ';
  public static final char TAB_CHAR = '\t';
  public static final char ENTER_CHAR = '\n';
  public static final char DEFAULT_CHAR = 'D';
  public static final char CUSTOM_CHAR = 'C';
  public static final char NONE_CHAR = 'N';

  @NonNls private static final String SPACE = "SPACE";
  @NonNls private static final String TAB = "TAB";
  @NonNls private static final String ENTER = "ENTER";
  @NonNls private static final String CUSTOM = "CUSTOM";
  @NonNls private static final String NONE = "NONE";

  @NonNls private static final String NAME = "name";
  @NonNls private static final String VALUE = "value";
  @NonNls private static final String DESCRIPTION = "description";
  @NonNls private static final String SHORTCUT = "shortcut";

  @NonNls private static final String VARIABLE = "variable";
  @NonNls private static final String EXPRESSION = "expression";
  @NonNls private static final String DEFAULT_VALUE = "defaultValue";
  @NonNls private static final String ALWAYS_STOP_AT = "alwaysStopAt";

  @NonNls static final String CONTEXT = "context";
  @NonNls private static final String TO_REFORMAT = "toReformat";
  @NonNls private static final String TO_SHORTEN_FQ_NAMES = "toShortenFQNames";
  @NonNls private static final String USE_STATIC_IMPORT = "useStaticImport";

  @NonNls private static final String DEACTIVATED = "deactivated";

  @NonNls private static final String RESOURCE_BUNDLE = "resource-bundle";
  @NonNls private static final String KEY = "key";
  @NonNls private static final String ID = "id";

  static final String TEMPLATES_DIR_PATH = "templates";

  private final MultiMap<String, TemplateImpl> myTemplates = MultiMap.createLinked();

  private final Map<String, Template> myTemplatesById = new LinkedHashMap<>();
  private final Map<TemplateKey, TemplateImpl> myDefaultTemplates = new LinkedHashMap<>();

  private int myMaxKeyLength = 0;
  private final SchemeManager<TemplateGroup> mySchemeManager;

  private State myState = new State();
  private final Map<Pair<String, String>, PluginInfo> myPredefinedTemplates = new HashMap<>();

  static final class ShortcutConverter extends Converter<Character> {
    @NotNull
    @Override
    public Character fromString(@NotNull String shortcut) {
      return TAB.equals(shortcut) ? TAB_CHAR :
             ENTER.equals(shortcut) ? ENTER_CHAR :
             CUSTOM.equals(shortcut) ? CUSTOM_CHAR :
             NONE.equals(shortcut) ? NONE_CHAR :
             SPACE_CHAR;
    }

    @NotNull
    @Override
    public String toString(@NotNull Character shortcut) {
      return shortcut == TAB_CHAR ? TAB :
             shortcut == ENTER_CHAR ? ENTER :
             shortcut == CUSTOM_CHAR ? CUSTOM :
             shortcut == NONE_CHAR ? NONE :
             SPACE;
    }
  }

  final static class State {
    @OptionTag(nameAttribute = "", valueAttribute = "shortcut", converter = ShortcutConverter.class)
    public char defaultShortcut = TAB_CHAR;

    public List<TemplateSettings.TemplateKey> deletedKeys = new SmartList<>();
  }

  public static class TemplateKey {
    private String groupName;
    private String key;

    @SuppressWarnings("UnusedDeclaration")
    public TemplateKey() {}

    private TemplateKey(String groupName, String key) {
      this.groupName = groupName;
      this.key = key;
    }

    public static TemplateKey keyOf(TemplateImpl template) {
      return new TemplateKey(template.getGroupName(), template.getKey());
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TemplateKey that = (TemplateKey)o;
      return Comparing.equal(groupName, that.groupName) && Comparing.equal(key, that.key);
    }

    public int hashCode() {
      int result = groupName != null ? groupName.hashCode() : 0;
      result = 31 * result + (key != null ? key.hashCode() : 0);
      return result;
    }

    public String getGroupName() {
      return groupName;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setGroupName(String groupName) {
      this.groupName = groupName;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    @Override
    public String toString() {
      return getKey() + "@" + getGroupName();
    }
  }

  private TemplateKey myLastSelectedTemplate;

  public TemplateSettings() {
    this(SchemeManagerFactory.getInstance());
  }

  @NonInjectable
  public TemplateSettings(@NotNull SchemeManagerFactory factory) {
    mySchemeManager = factory.create(TEMPLATES_DIR_PATH, new BaseSchemeProcessor<TemplateGroup, TemplateGroup>() {
      @Nullable
      @Override
      public TemplateGroup readScheme(@NotNull Element element, boolean duringLoad) {
        TemplateGroup readGroup = parseTemplateGroup(element, element.getAttributeValue("group"), getClass().getClassLoader());
        TemplateGroup group = readGroup == null ? null : mergeParsedGroup(element, false, false, readGroup);
        if (group != null) {
          group.setModified(false);
        }
        return group;
      }

      @Override
      public void beforeReloaded(@NotNull SchemeManager<TemplateGroup> schemeManager) {
        for (TemplateGroup group : schemeManager.getAllSchemes()) {
          schemeManager.removeScheme(group);
        }
        myTemplates.clear();
        myDefaultTemplates.clear();
      }

      @Override
      public void reloaded(@NotNull SchemeManager<TemplateGroup> schemeManager, @NotNull Collection<? extends TemplateGroup> groups) {
        doLoadTemplates(groups);
      }

      @NotNull
      @Override
      public SchemeState getState(@NotNull TemplateGroup template) {
        if (template.isModified()) {
          return SchemeState.POSSIBLY_CHANGED;
        }

        for (TemplateImpl t : template.getElements()) {
          if (differsFromDefault(t)) {
            return SchemeState.POSSIBLY_CHANGED;
          }
        }
        return SchemeState.NON_PERSISTENT;
      }

      @NotNull
      @Override
      public Element writeScheme(@NotNull TemplateGroup template) {
        Element templateSetElement = new Element(TEMPLATE_SET);

        List<TemplateImpl> elements = template.getElements();
        if (!elements.isEmpty()) {
          boolean isGroupAttributeAdded = false;
          Lazy<Map<String, TemplateContextType>> idToType = TemplateContext.getIdToType();
          for (TemplateImpl t : elements) {
            TemplateImpl defaultTemplate = getDefaultTemplate(t);
            if (defaultTemplate == null || !t.equals(defaultTemplate) || !t.contextsEqual(defaultTemplate)) {
              if (!isGroupAttributeAdded) {
                isGroupAttributeAdded = true;
                // add attribute only if not empty to avoid empty file (due to group attribute element will be not considered as empty)
                templateSetElement.setAttribute(GROUP, template.getName());
              }

              templateSetElement.addContent(serializeTemplate(t, defaultTemplate, idToType));
            }
          }
        }

        template.setModified(false);
        return templateSetElement;
      }

      @Override
      public void onSchemeAdded(@NotNull final TemplateGroup scheme) {
        for (TemplateImpl template : scheme.getElements()) {
          addTemplateImpl(template);
        }
      }

      @Override
      public void onSchemeDeleted(@NotNull final TemplateGroup scheme) {
        for (TemplateImpl template : scheme.getElements()) {
          removeTemplate(template);
        }
      }
    });

    doLoadTemplates(mySchemeManager.loadSchemes());
  }

  private void doLoadTemplates(@NotNull Collection<? extends TemplateGroup> groups) {
    for (TemplateGroup group : groups) {
      for (TemplateImpl template : group.getElements()) {
        addTemplateImpl(template);
      }
    }
    loadDefaultLiveTemplates();
  }

  public static TemplateSettings getInstance() {
    return ServiceManager.getService(TemplateSettings.class);
  }

  private boolean differsFromDefault(TemplateImpl t) {
    TemplateImpl def = getDefaultTemplate(t);
    return def == null || !t.equals(def) || !t.contextsEqual(def);
  }

  @Nullable
  public TemplateImpl getDefaultTemplate(TemplateImpl t) {
    return myDefaultTemplates.get(TemplateKey.keyOf(t));
  }

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    myState = state;

    applyNewDeletedTemplates();
  }

  void applyNewDeletedTemplates() {
    for (TemplateKey templateKey : myState.deletedKeys) {
      if (templateKey.groupName == null) {
        for (TemplateImpl template : new ArrayList<>(myTemplates.get(templateKey.key))) {
          removeTemplate(template);
        }
      }
      else {
        TemplateImpl toDelete = getTemplate(templateKey.key, templateKey.groupName);
        if (toDelete != null) {
          removeTemplate(toDelete);
        }
      }
    }
  }

  @Nullable
  public String getLastSelectedTemplateKey() {
    return myLastSelectedTemplate != null ? myLastSelectedTemplate.key : null;
  }

  @Nullable
  public String getLastSelectedTemplateGroup() {
    return myLastSelectedTemplate != null ? myLastSelectedTemplate.groupName : null;
  }

  public void setLastSelectedTemplate(@Nullable String group, @Nullable String key) {
    myLastSelectedTemplate = group == null ? null : new TemplateKey(group, key);
  }

  @SuppressWarnings("unused")
  public Collection<? extends TemplateImpl> getTemplatesAsList() {
    return myTemplates.values();
  }

  public TemplateImpl[] getTemplates() {
    final Collection<? extends TemplateImpl> all = myTemplates.values();
    return all.toArray(new TemplateImpl[0]);
  }

  public char getDefaultShortcutChar() {
    return myState.defaultShortcut;
  }

  public void setDefaultShortcutChar(char defaultShortcutChar) {
    myState.defaultShortcut = defaultShortcutChar;
  }

  @NotNull
  public Collection<TemplateImpl> getTemplates(@NotNull String key) {
    return myTemplates.get(key);
  }

  @Nullable
  public TemplateImpl getTemplate(@NonNls String key, String group) {
    final Collection<TemplateImpl> templates = myTemplates.get(key);
    for (TemplateImpl template : templates) {
      if (template.getGroupName().equals(group)) {
        return template;
      }
    }
    return null;
  }

  public Template getTemplateById(@NonNls String id) {
    return myTemplatesById.get(id);
  }

  public int getMaxKeyLength() {
    return myMaxKeyLength;
  }

  public void addTemplate(Template template) {
    clearPreviouslyRegistered(template);
    addTemplateImpl(template);

    TemplateImpl templateImpl = (TemplateImpl)template;
    String groupName = templateImpl.getGroupName();
    TemplateGroup group = mySchemeManager.findSchemeByName(groupName);
    if (group == null) {
      group = new TemplateGroup(groupName);
      mySchemeManager.addScheme(group);
    }
    group.addElement(templateImpl);
  }

  private void clearPreviouslyRegistered(final Template template) {
    TemplateImpl existing = getTemplate(template.getKey(), ((TemplateImpl) template).getGroupName());
    if (existing != null) {
      LOG.info("Template with key " + template.getKey() + " and id " + template.getId() + " already registered");
      TemplateGroup group = mySchemeManager.findSchemeByName(existing.getGroupName());
      if (group != null) {
        group.removeElement(existing);
        if (group.isEmpty()) {
          mySchemeManager.removeScheme(group);
        }
      }
      myTemplates.remove(template.getKey(), existing);
    }
  }

  private void addTemplateImpl(@NotNull Template template) {
    TemplateImpl templateImpl = (TemplateImpl)template;
    if (getTemplate(templateImpl.getKey(), templateImpl.getGroupName()) == null) {
      myTemplates.putValue(template.getKey(), templateImpl);
    }

    myMaxKeyLength = Math.max(myMaxKeyLength, template.getKey().length());
    myState.deletedKeys.remove(TemplateKey.keyOf((TemplateImpl)template));
  }

  private void addTemplateById(Template template) {
    if (!myTemplatesById.containsKey(template.getId())) {
      final String id = template.getId();
      if (id != null) {
        myTemplatesById.put(id, template);
      }
    }
  }

  public void removeTemplate(@NotNull Template template) {
    myTemplates.remove(template.getKey(), (TemplateImpl)template);

    TemplateGroup group = mySchemeManager.findSchemeByName(((TemplateImpl)template).getGroupName());
    if (group != null) {
      group.removeElement((TemplateImpl)template);
      if (group.isEmpty()) {
        mySchemeManager.removeScheme(group);
      }
    }
  }

  @NotNull
  private static TemplateImpl createTemplate(@NotNull String key, String string, @NotNull String group, String description, @Nullable String shortcut, String id) {
    TemplateImpl template = new TemplateImpl(key, string, group, false);
    template.setId(id);
    template.setDescription(description);
    if (TAB.equals(shortcut)) {
      template.setShortcutChar(TAB_CHAR);
    }
    else if (ENTER.equals(shortcut)) {
      template.setShortcutChar(ENTER_CHAR);
    }
    else if (SPACE.equals(shortcut)) {
      template.setShortcutChar(SPACE_CHAR);
    }
    else if (NONE.equals(shortcut)) {
      template.setShortcutChar(NONE_CHAR);
    }
    else {
      template.setShortcutChar(DEFAULT_CHAR);
    }
    return template;
  }

  private void loadDefaultLiveTemplates() {
    try {
      for (DefaultLiveTemplatesProvider provider : DefaultLiveTemplatesProvider.EP_NAME.getExtensionList()) {
        loadDefaultLiveTemplatesFromProvider(provider);
      }

      for (DefaultLiveTemplateEP ep : DefaultLiveTemplateEP.EP_NAME.getExtensionList()) {
        String file = ep.getFile();
        if (file == null) continue;
        ClassLoader pluginClassLoader = ep.getPluginDescriptor().getPluginClassLoader();
        readDefTemplate(pluginClassLoader, file, !ep.getHidden(), pluginClassLoader,
                        PluginInfoDetectorKt.getPluginInfoByDescriptor(ep.getPluginDescriptor()));
      }
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

  private void loadDefaultLiveTemplatesFromProvider(DefaultLiveTemplatesProvider provider) throws JDOMException, IOException {
    for (String defTemplate : provider.getDefaultLiveTemplateFiles()) {
      readDefTemplate(provider, defTemplate, true, provider.getClass().getClassLoader(),
                      PluginInfoDetectorKt.getPluginInfo(provider.getClass()));
    }
    try {
      String[] hidden = provider.getHiddenLiveTemplateFiles();
      if (hidden != null) {
        for (String s : hidden) {
          readDefTemplate(provider, s, false, provider.getClass().getClassLoader(),
                          PluginInfoDetectorKt.getPluginInfo(provider.getClass()));
        }
      }
    }
    catch (AbstractMethodError ignore) {
    }
  }

  private void readDefTemplate(@NotNull Object requestor,
                               @NotNull String defTemplate,
                               boolean registerTemplate, ClassLoader loader, PluginInfo info) throws JDOMException, InvalidDataException, IOException {
    InputStream inputStream = DecodeDefaultsUtil.getDefaultsInputStream(requestor, defTemplate);
    if (inputStream != null) {
      Element element = JDOMUtil.load(inputStream);
      TemplateGroup defGroup = parseTemplateGroup(element, getDefaultTemplateName(defTemplate), loader);
      if (defGroup != null) {
        for (TemplateImpl template : defGroup.getElements()) {
          String key = template.getKey();
          String groupName = template.getGroupName();
          if (StringUtil.isNotEmpty(key) && StringUtil.isNotEmpty(groupName)) {
            myPredefinedTemplates.put(Pair.create(key, groupName), info);
          }
        }

        TemplateGroup group = mergeParsedGroup(element, true, registerTemplate, defGroup);
        if (group != null && group.getReplace() != null) {
          for (TemplateImpl template : myTemplates.get(group.getReplace())) {
            removeTemplate(template);
          }
        }
      }
    }
  }

  @Nullable
  public PluginInfo findPluginForPredefinedTemplate(TemplateImpl template) {
    return myPredefinedTemplates.get(Pair.create(template.getKey(), template.getGroupName()));
  }

  private static String getDefaultTemplateName(String defTemplate) {
    return defTemplate.substring(defTemplate.lastIndexOf('/') + 1);
  }

  @Nullable
  private static TemplateGroup parseTemplateGroup(@NotNull Element element, @NonNls String defGroupName, @NotNull ClassLoader classLoader) {
    if (!TEMPLATE_SET.equals(element.getName())) {
      LOG.error("Ignore invalid template scheme: " + JDOMUtil.writeElement(element));
      return null;
    }

    String groupName = element.getAttributeValue(GROUP);
    if (StringUtil.isEmpty(groupName)) {
      groupName = defGroupName;
    }

    TemplateGroup result = new TemplateGroup(groupName, element.getAttributeValue("REPLACE"));

    for (Element child : element.getChildren(TEMPLATE)) {
      try {
        result.addElement(readTemplateFromElement(groupName, child, classLoader));
      }
      catch (Exception e) {
        LOG.warn("failed to load template " + element.getAttributeValue(NAME), e);
      }
    }
    return result;
  }

  @Nullable
  private TemplateGroup mergeParsedGroup(@NotNull Element element,
                                         boolean isDefault,
                                         boolean registerTemplate,
                                         TemplateGroup parsedGroup) {
    TemplateGroup result = new TemplateGroup(parsedGroup.getName(), element.getAttributeValue("REPLACE"));

    Map<String, TemplateImpl> created = new LinkedHashMap<>();

    for (TemplateImpl template : parsedGroup.getElements()) {
      if (isDefault) {
        myDefaultTemplates.put(TemplateKey.keyOf(template), template);
      }
      TemplateImpl existing = getTemplate(template.getKey(), template.getGroupName());
      boolean defaultTemplateModified = isDefault && (myState.deletedKeys.contains(TemplateKey.keyOf(template)) ||
                                                      myTemplatesById.containsKey(template.getId()) ||
                                                      existing != null);

      if(!defaultTemplateModified) {
        created.put(template.getKey(), template);
      }
      if (isDefault && existing != null) {
        existing.getTemplateContext().setDefaultContext(template.getTemplateContext());
      }
    }

    if (registerTemplate) {
      TemplateGroup existingScheme = mySchemeManager.findSchemeByName(result.getName());
      if (existingScheme != null) {
        result = existingScheme;
      }
    }

    for (TemplateImpl template : created.values()) {
      if (registerTemplate) {
        clearPreviouslyRegistered(template);
        addTemplateImpl(template);
      }
      addTemplateById(template);

      result.addElement(template);
    }

    if (registerTemplate) {
      TemplateGroup existingScheme = mySchemeManager.findSchemeByName(result.getName());
      if (existingScheme == null && !result.isEmpty()) {
        mySchemeManager.addScheme(result, false);
      }
    }

    return result.isEmpty() ? null : result;
  }

  public static TemplateImpl readTemplateFromElement(final String groupName, @NotNull Element element, @NotNull ClassLoader classLoader) {
    String name = element.getAttributeValue(NAME);
    String value = element.getAttributeValue(VALUE);
    String description;
    String resourceBundle = element.getAttributeValue(RESOURCE_BUNDLE);
    String key = element.getAttributeValue(KEY);
    String id = element.getAttributeValue(ID);
    if (resourceBundle != null && key != null) {
      ResourceBundle bundle = AbstractBundle.getResourceBundle(resourceBundle, classLoader);
      description = bundle.getString(key);
    }
    else {
      description = element.getAttributeValue(DESCRIPTION);
    }

    String shortcut = element.getAttributeValue(SHORTCUT);
    TemplateImpl template = createTemplate(name, value, groupName, description, shortcut, id);

    template.setToReformat(Boolean.parseBoolean(element.getAttributeValue(TO_REFORMAT)));
    template.setToShortenLongNames(Boolean.parseBoolean(element.getAttributeValue(TO_SHORTEN_FQ_NAMES)));
    template.setDeactivated(Boolean.parseBoolean(element.getAttributeValue(DEACTIVATED)));

    String useStaticImport = element.getAttributeValue(USE_STATIC_IMPORT);
    if (useStaticImport != null) {
      template.setValue(TemplateImpl.Property.USE_STATIC_IMPORT_IF_POSSIBLE, Boolean.parseBoolean(useStaticImport));
    }

    for (Element e : element.getChildren(VARIABLE)) {
      String variableName = e.getAttributeValue(NAME);
      String expression = e.getAttributeValue(EXPRESSION);
      String defaultValue = e.getAttributeValue(DEFAULT_VALUE);
      boolean isAlwaysStopAt = Boolean.parseBoolean(e.getAttributeValue(ALWAYS_STOP_AT));
      template.addVariable(variableName, expression, defaultValue, isAlwaysStopAt);
    }

    Element context = element.getChild(CONTEXT);
    if (context != null) {
      template.getTemplateContext().readTemplateContext(context);
    }

    return template;
  }

  @NotNull
  public static Element serializeTemplate(@NotNull TemplateImpl template, @Nullable TemplateImpl defaultTemplate, @NotNull Lazy<Map<String, TemplateContextType>> idToType) {
    Element element = new Element(TEMPLATE);
    final String id = template.getId();
    if (id != null) {
      element.setAttribute(ID, id);
    }
    element.setAttribute(NAME, template.getKey());
    element.setAttribute(VALUE, template.getString());
    if (template.getShortcutChar() == TAB_CHAR) {
      element.setAttribute(SHORTCUT, TAB);
    }
    else if (template.getShortcutChar() == ENTER_CHAR) {
      element.setAttribute(SHORTCUT, ENTER);
    }
    else if (template.getShortcutChar() == SPACE_CHAR) {
      element.setAttribute(SHORTCUT, SPACE);
    }
    else if (template.getShortcutChar() == NONE_CHAR) {
      element.setAttribute(SHORTCUT, NONE);
    }
    if (template.getDescription() != null) {
      element.setAttribute(DESCRIPTION, template.getDescription());
    }
    element.setAttribute(TO_REFORMAT, Boolean.toString(template.isToReformat()));
    element.setAttribute(TO_SHORTEN_FQ_NAMES, Boolean.toString(template.isToShortenLongNames()));
    if (template.getValue(Template.Property.USE_STATIC_IMPORT_IF_POSSIBLE)
        != Template.getDefaultValue(Template.Property.USE_STATIC_IMPORT_IF_POSSIBLE)) {
      element.setAttribute(USE_STATIC_IMPORT, Boolean.toString(template.getValue(Template.Property.USE_STATIC_IMPORT_IF_POSSIBLE)));
    }
    if (template.isDeactivated()) {
      element.setAttribute(DEACTIVATED, Boolean.toString(true));
    }

    for (int i = 0; i < template.getVariableCount(); i++) {
      Element variableElement = new Element(VARIABLE);
      variableElement.setAttribute(NAME, template.getVariableNameAt(i));
      variableElement.setAttribute(EXPRESSION, template.getExpressionStringAt(i));
      variableElement.setAttribute(DEFAULT_VALUE, template.getDefaultValueStringAt(i));
      variableElement.setAttribute(ALWAYS_STOP_AT, Boolean.toString(template.isAlwaysStopAt(i)));
      element.addContent(variableElement);
    }

    Element contextElement = template.getTemplateContext().writeTemplateContext(defaultTemplate == null ? null : defaultTemplate.getTemplateContext(), idToType);
    if (contextElement != null) {
      element.addContent(contextElement);
    }
    return element;
  }

  public void setTemplates(@NotNull List<? extends TemplateGroup> newGroups) {
    myTemplates.clear();
    myState.deletedKeys.clear();
    for (TemplateImpl template : myDefaultTemplates.values()) {
      myState.deletedKeys.add(TemplateKey.keyOf(template));
    }
    myMaxKeyLength = 0;
    List<TemplateGroup> schemes = new SmartList<>();
    for (TemplateGroup group : newGroups) {
      if (!group.isEmpty()) {
        schemes.add(group);
        for (TemplateImpl template : group.getElements()) {
          clearPreviouslyRegistered(template);
          addTemplateImpl(template);
        }
      }
    }
    mySchemeManager.setSchemes(schemes);
  }

  public List<TemplateGroup> getTemplateGroups() {
    return mySchemeManager.getAllSchemes();
  }

  @NotNull
  public List<TemplateImpl> collectMatchingCandidates(@NotNull String key, @Nullable Character shortcutChar, boolean hasArgument) {
    final Collection<TemplateImpl> templates = getTemplates(key);
    if (templates.isEmpty()) {
      return Collections.emptyList();
    }

    List<TemplateImpl> candidates = new ArrayList<>();
    for (TemplateImpl template : templates) {
      if (template.isDeactivated()) {
        continue;
      }
      if (shortcutChar != null && getShortcutChar(template) != shortcutChar) {
        continue;
      }
      if (hasArgument && !template.hasArgument()) {
        continue;
      }
      candidates.add(template);
    }
    return candidates;
  }

  public char getShortcutChar(TemplateImpl template) {
    char c = template.getShortcutChar();
    return c == DEFAULT_CHAR ? getDefaultShortcutChar() : c;
  }

  public List<TemplateKey> getDeletedTemplates() {
    return myState.deletedKeys;
  }

  public void reset() {
    myState.deletedKeys.clear();
    loadDefaultLiveTemplates();
  }
}
