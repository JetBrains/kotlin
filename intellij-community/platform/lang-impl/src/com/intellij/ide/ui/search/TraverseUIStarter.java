// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.ui.search;

import com.intellij.application.options.OptionsContainingConfigurable;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.impl.AllFileTemplatesConfigurable;
import com.intellij.ide.fileTemplates.impl.BundledFileTemplate;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerConfigurable;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.keymap.impl.ui.KeymapPanel;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import com.intellij.util.io.URLUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Used in installer's "build searchable options" step.
 *
 * In order to run locally, use "TraverseUi" run configuration (pass corresponding "idea.platform.prefix" property via VM options,
 * and choose correct main module).
 *
 * Pass {@code true} as the second parameter to have searchable options split by modules.
 */
@SuppressWarnings({"CallToPrintStackTrace", "UseOfSystemOutOrSystemErr"})
public final class TraverseUIStarter implements ApplicationStarter {
  private static final String OPTIONS = "options";
  private static final String CONFIGURABLE = "configurable";
  private static final String ID = "id";
  private static final String CONFIGURABLE_NAME = "configurable_name";
  private static final String OPTION = "option";
  private static final String NAME = "name";
  private static final String PATH = "path";
  private static final String HIT = "hit";

  private static final String ROOT_ACTION_MODULE = "intellij.platform.ide";

  private String OUTPUT_PATH;
  private boolean SPLIT_BY_RESOURCE_PATH;

  @Override
  public String getCommandName() {
    return "traverseUI";
  }

  @Override
  public void premain(@NotNull List<String> args) {
    OUTPUT_PATH = args.get(1);
    SPLIT_BY_RESOURCE_PATH = args.size() > 2 && Boolean.parseBoolean(args.get(2));
  }

  @Override
  public void main(@NotNull List<String> args) {
    System.out.println("Starting searchable options index builder");
    try {
      startup(OUTPUT_PATH, SPLIT_BY_RESOURCE_PATH);
      ((ApplicationEx)ApplicationManager.getApplication()).exit(ApplicationEx.FORCE_EXIT | ApplicationEx.EXIT_CONFIRMED);
    }
    catch (Throwable e) {
      System.out.println("Searchable options index builder failed");
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static void startup(@NotNull final String outputPath, final boolean splitByResourcePath) throws IOException {
    Map<SearchableConfigurable, Set<OptionDescription>> options = new LinkedHashMap<>();
    try {
      for (TraverseUIHelper extension : TraverseUIHelper.helperExtensionPoint.getExtensionList()) {
        extension.beforeStart();
      }

      SearchUtil.processProjectConfigurables(ProjectManager.getInstance().getDefaultProject(), options);

      for (TraverseUIHelper extension : TraverseUIHelper.helperExtensionPoint.getExtensionList()) {
        extension.afterTraversal(options);
      }

      final Map<String, Element> roots = new HashMap<>();
      for (SearchableConfigurable option : options.keySet()) {
        SearchableConfigurable configurable = option;

        Element configurableElement = createConfigurableElement(configurable);
        Set<OptionDescription> sortedOptions = options.get(configurable);
        writeOptions(configurableElement, sortedOptions);

        if (configurable instanceof ConfigurableWrapper) {
          UnnamedConfigurable wrapped = ((ConfigurableWrapper)configurable).getConfigurable();
          if (wrapped instanceof SearchableConfigurable) {
            configurable = (SearchableConfigurable)wrapped;
          }
        }
        if (configurable instanceof KeymapPanel) {
          for (final Map.Entry<String, Set<OptionDescription>> entry : processKeymap(splitByResourcePath).entrySet()) {
            final Element entryElement = createConfigurableElement(configurable);
            writeOptions(entryElement, entry.getValue());
            addElement(roots, entryElement, entry.getKey());
          }
        }
        else if (configurable instanceof OptionsContainingConfigurable) {
          processOptionsContainingConfigurable((OptionsContainingConfigurable)configurable, configurableElement);
        }
        else if (configurable instanceof PluginManagerConfigurable) {
          for (OptionDescription description : wordsToOptionDescriptors(Collections.singleton(
            IdeBundle.message("plugin.manager.repositories")))) {
            append(null, IdeBundle.message("plugin.manager.repositories"), description.getOption(), configurableElement);
          }
        }
        else if (configurable instanceof AllFileTemplatesConfigurable) {
          for (final Map.Entry<String, Set<OptionDescription>> entry : processFileTemplates(splitByResourcePath).entrySet()) {
            final Element entryElement = createConfigurableElement(configurable);
            writeOptions(entryElement, entry.getValue());
            addElement(roots, entryElement, entry.getKey());
          }
        }

        final String module = splitByResourcePath ? getModuleByClass(configurable.getOriginalClass()) : "";
        addElement(roots, configurableElement, module);
      }

      for (final Map.Entry<String, Element> entry : roots.entrySet()) {
        final String module = entry.getKey();
        final String directory = module.isEmpty() ? "" : module + "/search/";
        final String filePrefix = module.isEmpty() ? "" : module + ".";
        final File output = new File(outputPath, directory + filePrefix + SearchableOptionsRegistrar.SEARCHABLE_OPTIONS_XML);
        FileUtil.ensureCanCreateFile(output);
        JDOMUtil.writeDocument(new Document(entry.getValue()), output, "\n");
      }

      for (TraverseUIHelper extension : TraverseUIHelper.helperExtensionPoint.getExtensionList()) {
        extension.afterResultsAreSaved();
      }

      System.out.println("Searchable options index builder completed");
    }
    finally {
      for (SearchableConfigurable configurable : options.keySet()) {
        configurable.disposeUIResources();
      }
    }
  }

  @NotNull
  private static Element createConfigurableElement(@NotNull final SearchableConfigurable configurable) {
    Element configurableElement = new Element(CONFIGURABLE);
    String id = configurable.getId();
    configurableElement.setAttribute(ID, id);
    configurableElement.setAttribute(CONFIGURABLE_NAME, configurable.getDisplayName());
    return configurableElement;
  }

  private static void addElement(@NotNull final Map<String, Element> roots, @NotNull final Element element, @NotNull final String module) {
    roots.computeIfAbsent(module, __ -> new Element(OPTIONS)).addContent(element);
  }

  private static Map<String, Set<OptionDescription>> processFileTemplates(final boolean splitByResourcePath) {
    SearchableOptionsRegistrar optionsRegistrar = SearchableOptionsRegistrar.getInstance();
    final Map<String, Set<OptionDescription>> options = new HashMap<>();

    FileTemplateManager fileTemplateManager = FileTemplateManager.getDefaultInstance();
    processTemplates(optionsRegistrar, options, fileTemplateManager.getAllTemplates(), splitByResourcePath);
    processTemplates(optionsRegistrar, options, fileTemplateManager.getAllPatterns(), splitByResourcePath);
    processTemplates(optionsRegistrar, options, fileTemplateManager.getAllCodeTemplates(), splitByResourcePath);
    processTemplates(optionsRegistrar, options, fileTemplateManager.getAllJ2eeTemplates(), splitByResourcePath);

    return options;
  }

  private static void processTemplates(SearchableOptionsRegistrar registrar, Map<String, Set<OptionDescription>> options,
                                       FileTemplate[] templates, boolean splitByResourcePath) {
    for (FileTemplate template : templates) {
      final String module =
        splitByResourcePath && template instanceof BundledFileTemplate ? getModuleByTemplate((BundledFileTemplate)template) : "";
      collectOptions(registrar, options.computeIfAbsent(module, __ -> new TreeSet<>()), template.getName(), null);
    }
  }

  @NotNull
  private static String getModuleByTemplate(@NotNull final BundledFileTemplate template) {
    final String url = template.toString();
    String path = StringUtil.substringBefore(url, "fileTemplates");
    assert path != null : "Template URL doesn't contain 'fileTemplates' directory.";
    if (path.startsWith(URLUtil.JAR_PROTOCOL)) {
      path = StringUtil.trimEnd(path, URLUtil.JAR_SEPARATOR);
    }
    return PathUtil.getFileName(path);
  }

  private static void collectOptions(SearchableOptionsRegistrar registrar, Set<? super OptionDescription> options, @NotNull String text, String path) {
    for (String word : registrar.getProcessedWordsWithoutStemming(text)) {
      options.add(new OptionDescription(word, text, path));
    }
  }

  private static void processOptionsContainingConfigurable(OptionsContainingConfigurable configurable, Element configurableElement) {
    Set<String> optionsPath = configurable.processListOptions();
    Set<OptionDescription> result = wordsToOptionDescriptors(optionsPath);
    Map<String,Set<String>> optionsWithPaths = configurable.processListOptionsWithPaths();
    for (String path : optionsWithPaths.keySet()) {
      result.addAll(wordsToOptionDescriptors(optionsWithPaths.get(path), path));
    }
    writeOptions(configurableElement, result);
  }

  private static Set<OptionDescription> wordsToOptionDescriptors(@NotNull Set<String> optionsPath) {
    return wordsToOptionDescriptors(optionsPath, null);
  }

  private static Set<OptionDescription> wordsToOptionDescriptors(@NotNull Set<String> optionsPath, @Nullable String path) {
    SearchableOptionsRegistrar registrar = SearchableOptionsRegistrar.getInstance();
    Set<OptionDescription> result = new TreeSet<>();
    for (String opt : optionsPath) {
      for (String word : registrar.getProcessedWordsWithoutStemming(opt)) {
        if (word != null) {
          result.add(new OptionDescription(word, opt, path));
        }
      }
    }
    return result;
  }

  private static @NotNull Map<String, Set<OptionDescription>> processKeymap(boolean splitByResourcePath) {
    Map<String, Set<OptionDescription>> map = new HashMap<>();
    ActionManagerImpl actionManager = (ActionManagerImpl)ActionManager.getInstance();
    Map<String, PluginId> actionToPluginId = splitByResourcePath ? getActionToPluginId() : Collections.emptyMap();
    String componentName = "ActionManager";
    SearchableOptionsRegistrar searchableOptionsRegistrar = SearchableOptionsRegistrar.getInstance();
    for (String id : actionManager.getActionIds()) {
      AnAction action = actionManager.getAction(id);
      if (action == null) {
        throw new IllegalStateException("Cannot find action by id " + id);
      }

      String module = splitByResourcePath ? getModuleByAction(action, actionToPluginId) : "";
      Set<OptionDescription> options = map.computeIfAbsent(module, __ -> new TreeSet<>());
      String text = action.getTemplatePresentation().getText();
      if (text != null) {
        collectOptions(searchableOptionsRegistrar, options, text, componentName);
      }

      String description = action.getTemplatePresentation().getDescription();
      if (description != null) {
        collectOptions(searchableOptionsRegistrar, options, description, componentName);
      }
    }
    return map;
  }

  @NotNull
  private static Map<String, PluginId> getActionToPluginId() {
    ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
    Map<String, PluginId> actionToPluginId = new HashMap<>();
    for (PluginId id : PluginId.getRegisteredIdList()) {
      for (String action : actionManager.getPluginActions(id)) {
        actionToPluginId.put(action, id);
      }
    }
    return actionToPluginId;
  }

  @NotNull
  private static String getModuleByAction(@NotNull final AnAction rootAction, @NotNull final Map<String, PluginId> actionToPluginId) {
    final Deque<AnAction> actions = new ArrayDeque<>();
    actions.add(rootAction);
    while (!actions.isEmpty()) {
      final AnAction action = actions.remove();
      final String module = getModuleByClass(action.getClass());
      if (!ROOT_ACTION_MODULE.equals(module)) {
        return module;
      }
      if (action instanceof ActionGroup) {
        Collections.addAll(actions, ((ActionGroup)action).getChildren(null));
      }
    }
    final ActionManager actionManager = ActionManager.getInstance();
    final PluginId id = actionToPluginId.get(actionManager.getId(rootAction));
    if (id != null) {
      final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(id);
      if (plugin != null && !plugin.getName().equals("IDEA CORE")) {
        return PathUtil.getFileName(plugin.getPluginPath().toString());
      }
    }
    return ROOT_ACTION_MODULE;
  }

  @NotNull
  private static String getModuleByClass(@NotNull final Class<?> aClass) {
    return PathUtil.getFileName(PathUtil.getJarPathForClass(aClass));
  }

  private static void writeOptions(Element configurableElement, Set<? extends OptionDescription> options) {
    for (OptionDescription opt : options) {
      append(opt.getPath(), opt.getHit(), opt.getOption(), configurableElement);
    }
  }

  private static void append(String path, String hit, final String word, final Element configurableElement) {
    Element optionElement = new Element(OPTION);
    optionElement.setAttribute(NAME, word);
    if (path != null) {
      optionElement.setAttribute(PATH, path);
    }
    optionElement.setAttribute(HIT, hit);
    configurableElement.addContent(optionElement);
  }
}