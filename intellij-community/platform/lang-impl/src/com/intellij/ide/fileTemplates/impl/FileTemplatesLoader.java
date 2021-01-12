// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.fileTemplates.impl;

import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ClearableLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.objectTree.ThrowableInterner;
import com.intellij.project.ProjectKt;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.UriUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.lang.UrlClassLoader;
import gnu.trove.THashSet;
import org.apache.velocity.runtime.ParserPool;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.directive.Stop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;

/**
 * Serves as a container for all existing template manager types and loads corresponding templates lazily.
 * Reloads templates on plugins change.
 */
class FileTemplatesLoader implements Disposable {
  private static final Logger LOG = Logger.getInstance(FileTemplatesLoader.class);

  static final String TEMPLATES_DIR = "fileTemplates";
  private static final String DEFAULT_TEMPLATES_ROOT = TEMPLATES_DIR;
  private static final String DESCRIPTION_FILE_EXTENSION = "html";
  private static final String DESCRIPTION_EXTENSION_SUFFIX = "." + DESCRIPTION_FILE_EXTENSION;

  private static final Map<String, String> MANAGER_TO_DIR = ContainerUtil.newHashMap(
    Pair.create(FileTemplateManager.DEFAULT_TEMPLATES_CATEGORY, ""),
    Pair.create(FileTemplateManager.INTERNAL_TEMPLATES_CATEGORY, "internal"),
    Pair.create(FileTemplateManager.INCLUDES_TEMPLATES_CATEGORY, "includes"),
    Pair.create(FileTemplateManager.CODE_TEMPLATES_CATEGORY, "code"),
    Pair.create(FileTemplateManager.J2EE_TEMPLATES_CATEGORY, "j2ee")
  );

  private final ClearableLazyValue<LoadedConfiguration> myManagers;

  FileTemplatesLoader(@Nullable Project project) {
    myManagers = ClearableLazyValue.createAtomic(() -> {
      return loadConfiguration(project);
    });
    ApplicationManager.getApplication().getMessageBus().connect(this).
      subscribe(DynamicPluginListener.TOPIC, new DynamicPluginListener() {
        @Override
        public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
          // this shouldn't be necessary once we update to a new Velocity Engine with this leak fixed (IDEA-240449, IDEABKL-7932)
          clearClassLeakViaStaticExceptionTrace();
          resetParserPool();
        }

        private void clearClassLeakViaStaticExceptionTrace() {
          Field field = ReflectionUtil.getDeclaredField(Stop.class, "STOP_ALL");
          if (field != null) {
            try {
              ThrowableInterner.clearBacktrace((Throwable)field.get(null));
            }
            catch (Throwable e) {
              LOG.info(e);
            }
          }
        }

        private void resetParserPool() {
          try {
            RuntimeServices ri = RuntimeSingleton.getRuntimeServices();
            Field ppField = ReflectionUtil.getDeclaredField(ri.getClass(), "parserPool");
            if (ppField != null) {
              Object pp = ppField.get(ri);
              if (pp instanceof ParserPool) {
                ((ParserPool)pp).initialize(ri);
              }
            }
          }
          catch (Throwable e) {
            LOG.info(e);
          }
        }

        @Override
        public void pluginLoaded(@NotNull IdeaPluginDescriptor pluginDescriptor) {
          myManagers.drop();
        }
        @Override
        public void pluginUnloaded(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
          myManagers.drop();
        }
      });
  }

  @Override
  public void dispose() {}

  @NotNull Collection<@NotNull FTManager> getAllManagers() {
    return myManagers.getValue().getManagers();
  }

  @NotNull
  FTManager getDefaultTemplatesManager() {
    return new FTManager(myManagers.getValue().getManager(FileTemplateManager.DEFAULT_TEMPLATES_CATEGORY));
  }

  @NotNull
  FTManager getInternalTemplatesManager() {
    return new FTManager(myManagers.getValue().getManager(FileTemplateManager.INTERNAL_TEMPLATES_CATEGORY));
  }

  @NotNull
  FTManager getPatternsManager() {
    return new FTManager(myManagers.getValue().getManager(FileTemplateManager.INCLUDES_TEMPLATES_CATEGORY));
  }

  @NotNull
  FTManager getCodeTemplatesManager() {
    return new FTManager(myManagers.getValue().getManager(FileTemplateManager.CODE_TEMPLATES_CATEGORY));
  }

  @NotNull
  FTManager getJ2eeTemplatesManager() {
    return new FTManager(myManagers.getValue().getManager(FileTemplateManager.J2EE_TEMPLATES_CATEGORY));
  }

  URL getDefaultTemplateDescription() {
    return myManagers.getValue().defaultTemplateDescription;
  }

  URL getDefaultIncludeDescription() {
    return myManagers.getValue().defaultIncludeDescription;
  }

  private static LoadedConfiguration loadConfiguration(@Nullable Project project) {
    Path configDir;
    if (project == null || project.isDefault()) {
      configDir = PathManager.getConfigDir().resolve(TEMPLATES_DIR);
    }
    else {
      String storeDirPath = Objects.requireNonNull(ProjectKt.getStateStore(project).getDirectoryStorePath(true));
      configDir = Paths.get(storeDirPath, TEMPLATES_DIR);
    }

    FileTemplateLoadResult result = loadDefaultTemplates(new ArrayList<>(MANAGER_TO_DIR.values()));
    Map<String, FTManager> managers = new HashMap<>();
    for (Map.Entry<String, String> entry: MANAGER_TO_DIR.entrySet()) {
      String name = entry.getKey();
      String pathPrefix = entry.getValue();
      FTManager manager = new FTManager(name, configDir.resolve(pathPrefix),
                                        name.equals(FileTemplateManager.INTERNAL_TEMPLATES_CATEGORY));
      manager.setDefaultTemplates(result.getResult().get(pathPrefix));
      manager.loadCustomizedContent();
      managers.put(name, manager);
    }

    return new LoadedConfiguration(managers, result.getDefaultTemplateDescription(), result.getDefaultIncludeDescription());
  }

  private static @NotNull FileTemplateLoadResult loadDefaultTemplates(@NotNull List<String> prefixes) {
    FileTemplateLoadResult result = new FileTemplateLoadResult(new MultiMap<>());
    Set<URL> processedUrls = new THashSet<>();
    Set<ClassLoader> processedLoaders = new HashSet<>();
    IdeaPluginDescriptor[] plugins = PluginManagerCore.getPlugins();
    for (PluginDescriptor plugin : plugins) {
      if (plugin instanceof IdeaPluginDescriptorImpl && plugin.isEnabled()) {
        final ClassLoader loader = plugin.getPluginClassLoader();
        if (loader instanceof PluginClassLoader && ((PluginClassLoader)loader).getUrls().isEmpty() ||
            !processedLoaders.add(loader)) {
          continue; // test or development mode, when IDEA_CORE's loader contains all the classpath
        }
        try {
          final Enumeration<URL> systemResources = loader.getResources(DEFAULT_TEMPLATES_ROOT);
          if (systemResources.hasMoreElements()) {
            while (systemResources.hasMoreElements()) {
              final URL url = systemResources.nextElement();
              if (!processedUrls.add(url)) {
                continue;
              }
              loadDefaultsFromRoot(url, prefixes, result);
            }
          }
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    }
    return result;
  }

  private static void loadDefaultsFromRoot(@NotNull URL root, @NotNull List<String> prefixes, @NotNull FileTemplateLoadResult result)
    throws IOException {
    final List<String> children = UrlUtil.getChildrenRelativePaths(root);
    if (children.isEmpty()) {
      return;
    }

    final Set<String> descriptionPaths = new HashSet<>();
    for (String path : children) {
      if (path.equals("default.html")) {
        result.setDefaultTemplateDescription(
          UrlClassLoader.internProtocol(new URL(UriUtil.trimTrailingSlashes(root.toExternalForm()) + "/" + path)));
      }
      else if (path.equals("includes/default.html")) {
        result.setDefaultIncludeDescription(
          UrlClassLoader.internProtocol(new URL(UriUtil.trimTrailingSlashes(root.toExternalForm()) + "/" + path)));
      }
      else if (path.endsWith(DESCRIPTION_EXTENSION_SUFFIX)) {
        descriptionPaths.add(path);
      }
    }

    for (final String path : children) {
      if (!path.endsWith(FTManager.TEMPLATE_EXTENSION_SUFFIX)) {
        continue;
      }

      for (String prefix : prefixes) {
        if (!matchesPrefix(path, prefix)) {
          continue;
        }

        String filename = path.substring(prefix.isEmpty() ? 0 : prefix.length() + 1, path.length() - FTManager.TEMPLATE_EXTENSION_SUFFIX.length());
        String extension = FileUtilRt.getExtension(filename);
        String templateName = filename.substring(0, filename.length() - extension.length() - 1);
        URL templateUrl = UrlClassLoader.internProtocol(new URL(UriUtil.trimTrailingSlashes(root.toExternalForm()) + "/" + path));
        String descriptionPath = getDescriptionPath(prefix, templateName, extension, descriptionPaths);
        URL descriptionUrl = descriptionPath == null ? null :
                             UrlClassLoader
                               .internProtocol(new URL(UriUtil.trimTrailingSlashes(root.toExternalForm()) + "/" + descriptionPath));
        assert templateUrl != null;
        result.getResult().putValue(prefix, new DefaultTemplate(templateName, extension, templateUrl, descriptionUrl));
        // FTManagers loop
        break;
      }
    }
  }

  private static boolean matchesPrefix(@NotNull String path, @NotNull String prefix) {
    if (prefix.isEmpty()) {
      return path.indexOf('/') == -1;
    }
    return FileUtil.startsWith(path, prefix) && path.indexOf('/', prefix.length() + 1) == -1;
  }

  //Example: templateName="NewClass"   templateExtension="java"
  private static @Nullable String getDescriptionPath(@NotNull String pathPrefix,
                                                     @NotNull String templateName,
                                                     @NotNull String templateExtension,
                                                     @NotNull Set<String> descriptionPaths) {
    final Locale locale = Locale.getDefault();

    String descName = MessageFormat
      .format("{0}.{1}_{2}_{3}" + DESCRIPTION_EXTENSION_SUFFIX, templateName, templateExtension,
              locale.getLanguage(), locale.getCountry());
    String descPath = pathPrefix.isEmpty() ? descName : pathPrefix + "/" + descName;
    if (descriptionPaths.contains(descPath)) {
      return descPath;
    }

    descName = MessageFormat.format("{0}.{1}_{2}" + DESCRIPTION_EXTENSION_SUFFIX, templateName, templateExtension, locale.getLanguage());
    descPath = pathPrefix.isEmpty() ? descName : pathPrefix + descName;
    if (descriptionPaths.contains(descPath)) {
      return descPath;
    }

    descName = templateName + "." + templateExtension + DESCRIPTION_EXTENSION_SUFFIX;
    descPath = pathPrefix.isEmpty() ? descName : pathPrefix + descName;
    if (descriptionPaths.contains(descPath)) {
      return descPath;
    }
    return null;
  }

  private static class LoadedConfiguration {
    public final URL defaultTemplateDescription;
    public final URL defaultIncludeDescription;

    private final Map<String, FTManager> myManagers;

    LoadedConfiguration(@NotNull Map<String, FTManager> managers,
                        URL defaultTemplateDescription,
                        URL defaultIncludeDescription) {

      myManagers = Collections.unmodifiableMap(managers);
      this.defaultTemplateDescription = defaultTemplateDescription;
      this.defaultIncludeDescription = defaultIncludeDescription;
    }

    public FTManager getManager(@NotNull String kind) {
      return myManagers.get(kind);
    }

    public Collection<FTManager> getManagers() {
      return myManagers.values();
    }
  }
}
