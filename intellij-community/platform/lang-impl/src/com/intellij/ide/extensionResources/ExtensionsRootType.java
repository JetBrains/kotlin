// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.extensionResources;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.scratch.RootType;
import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PlatformUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.DigestUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p> Extensions root type provide a common interface for plugins to access resources that are modifiable by the user. </p>
 * <p>Plugin's resources are stored in a directory named %plugin-id% under extensions root.</p>
 * <p>
 * Plugins can bundle default resources. Bundled resources are searched via {@link ClassLoader#getResources(String)}
 * call to plugin's class loader passing {@link ExtensionsRootType#EXTENSIONS_PATH} concatenated with relative resource path as an argument.
 * </p>
 * <p> Bundled resources are updated automatically upon plugin version change. For bundled plugins, application version is used. </p>
 */
public final class ExtensionsRootType extends RootType {
  static final Logger LOG = Logger.getInstance(ExtensionsRootType.class);

  private static final String EXTENSIONS_PATH = "extensions";
  private static final String BACKUP_FILE_EXTENSION = "old";

  ExtensionsRootType() {
    super(EXTENSIONS_PATH, "Extensions");
  }

  @NotNull
  public static ExtensionsRootType getInstance() {
    return findByClass(ExtensionsRootType.class);
  }

  @NotNull
  public static Condition<File> regularFileFilter() {
    return new Condition<File>() {
      private final ExtensionsRootType myRootType = getInstance();
      @Override
      public boolean value(File file) {
        if (file.isDirectory() || file.isHidden()) return false;
        String name = file.getName();
        String extension = FileUtilRt.getExtension(name);
        return !extension.isEmpty() &&
               !FileUtilRt.extensionEquals(name, "txt") &&
               !FileUtilRt.extensionEquals(name, "properties") &&
               !extension.startsWith(BACKUP_FILE_EXTENSION) &&
               !myRootType.isResourceFile(file);
      }
    };
  }

  @Nullable
  public PluginId getOwner(@Nullable VirtualFile resource) {
    VirtualFile file = getPluginResourcesDirectoryFor(resource);
    return file != null ? PluginId.findId(file.getName()) : null;
  }

  @Nullable
  public File findResource(@NotNull PluginId pluginId, @NotNull String path) throws IOException {
    extractBundledExtensionsIfNeeded(pluginId);
    return findExtensionImpl(pluginId, path);
  }

  @Nullable
  public File findResourceDirectory(@NotNull PluginId pluginId, @NotNull String path, boolean createIfMissing) throws IOException {
    extractBundledExtensionsIfNeeded(pluginId);
    return findExtensionsDirectoryImpl(pluginId, path, createIfMissing);
  }

  public void extractBundledResources(@NotNull PluginId pluginId, @NotNull String path) throws IOException {
    List<URL> bundledResources = getBundledResourceUrls(pluginId, path);
    if (bundledResources.isEmpty()) return;

    File resourcesDirectory = findExtensionsDirectoryImpl(pluginId, path, true);
    if (resourcesDirectory == null) return;

    for (URL bundledResourceDirUrl : bundledResources) {
      VirtualFile bundledResourcesDir = VfsUtil.findFileByURL(bundledResourceDirUrl);
      if (bundledResourcesDir == null || !bundledResourcesDir.isDirectory()) continue;
      extractResources(bundledResourcesDir, resourcesDirectory);
    }
  }

  @Nullable
  @Override
  public String substituteName(@NotNull Project project, @NotNull VirtualFile file) {
    VirtualFile resourcesDir = getPluginResourcesDirectoryFor(file);
    if (file.equals(resourcesDir)) {
      String name = getPluginResourcesRootName(resourcesDir);
      if (name != null) {
        return name;
      }
    }
    return super.substituteName(project, file);
  }

  /** @noinspection unused*/
  public boolean isResourceFile(@NotNull File file) {
    return false;
  }

  @Nullable
  String getPath(@Nullable VirtualFile resource) {
    VirtualFile pluginResourcesDir = getPluginResourcesDirectoryFor(resource);
    PluginId pluginId = getOwner(pluginResourcesDir);
    return pluginResourcesDir != null && pluginId != null ? VfsUtilCore.getRelativePath(resource, pluginResourcesDir) : null;
  }

  @Nullable
  private File findExtensionImpl(@NotNull PluginId pluginId, @NotNull String path) throws IOException {
    File dir = findExtensionsDirectoryImpl(pluginId, "", false);
    File file = dir == null ? null : new File(dir, path);
    return file != null && file.exists() && file.isFile() ? file : null;
  }

  @Nullable
  private File findExtensionsDirectoryImpl(@NotNull PluginId pluginId, @NotNull String path, boolean createIfMissing) throws IOException {
    String fullPath = getPath(pluginId, path);
    File dir = new File(FileUtil.toSystemDependentName(fullPath));
    if (createIfMissing && !dir.exists() && !dir.mkdirs()) {
      throw new IOException("Failed to create directory: " + dir.getPath());
    }
    if (!dir.exists()) return null;
    if (!dir.isDirectory()) throw new IOException("Not a directory: " + dir.getPath());
    return dir;
  }

  @Nullable
  private String getPluginResourcesRootName(VirtualFile resourcesDir) {
    PluginId ownerPluginId = getOwner(resourcesDir);
    if (ownerPluginId == null) return null;

    if (PluginManagerCore.CORE_ID == ownerPluginId) {
      return PlatformUtils.getPlatformPrefix();
    }

    IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(ownerPluginId);
    if (plugin != null) {
      return plugin.getName();
    }

    return null;
  }

  @Contract("null->null")
  private VirtualFile getPluginResourcesDirectoryFor(@Nullable VirtualFile resource) {
    if (resource == null) return null;
    String rootPath = ScratchFileService.getInstance().getRootPath(this);
    VirtualFile root = LocalFileSystem.getInstance().findFileByPath(rootPath);
    if (root == null) return null;

    VirtualFile parent = resource;
    VirtualFile file = resource;
    while (parent != null && !root.equals(parent)) {
      file = parent;
      parent = file.getParent();
    }
    return parent != null && file.isDirectory() ? file : null;
  }

  @NotNull
  private String getPath(@NotNull PluginId pluginId, @NotNull String path) {
    return ScratchFileService.getInstance().getRootPath(this) + "/" + pluginId.getIdString() + (StringUtil.isEmpty(path) ? "" : "/" + path);
  }

  @NotNull
  private static List<URL> getBundledResourceUrls(@NotNull PluginId pluginId, @NotNull String path) throws IOException {
    String resourcesPath = EXTENSIONS_PATH + "/" + path;
    IdeaPluginDescriptorImpl plugin = null;
    // search in enabled plugins only
    for (IdeaPluginDescriptorImpl descriptor : PluginManagerCore.getLoadedPlugins(null)) {
      if (descriptor.getPluginId() == pluginId) {
        plugin = descriptor;
        break;
      }
    }

    if (plugin == null) {
      return ContainerUtil.emptyList();
    }

    ClassLoader pluginClassLoader = plugin.getPluginClassLoader();
    final Enumeration<URL> resources = pluginClassLoader.getResources(resourcesPath);
    if (resources == null) {
      return ContainerUtil.emptyList();
    }

    if (plugin.getUseIdeaClassLoader()) {
      return ContainerUtil.toList(resources);
    }

    Set<URL> urls = new LinkedHashSet<>(ContainerUtil.toList(resources));
    // exclude parent classloader resources from list
    List<ClassLoader> dependentPluginClassLoaders = StreamEx.of(plugin.getDependentPluginIds())
      .map(id -> PluginManagerCore.getPlugin(id))
      .nonNull()
      .map(PluginDescriptor::getPluginClassLoader)
      .without(pluginClassLoader)
      .collect(Collectors.toList());

    for (ClassLoader classLoader : dependentPluginClassLoaders) {
      urls.removeAll(ContainerUtil.toList(classLoader.getResources(resourcesPath)));
    }
    return new ArrayList<>(urls);
  }

  private static void extractResources(@NotNull VirtualFile from, @NotNull File to) throws IOException {
    VfsUtilCore.visitChildrenRecursively(from, new VirtualFileVisitor<Void>(VirtualFileVisitor.NO_FOLLOW_SYMLINKS) {
      @NotNull
      @Override
      public Result visitFileEx(@NotNull VirtualFile file) {
        try {
          return visitImpl(file);
        }
        catch (IOException e) {
          throw new VisitorException(e);
        }
      }

      Result visitImpl(@NotNull VirtualFile file) throws IOException {
        File child = new File(to, FileUtil.toSystemDependentName(ObjectUtils.notNull(VfsUtilCore.getRelativePath(file, from))));
        if (child.exists() && child.isDirectory() != file.isDirectory()) {
          renameToBackupCopy(child);
        }
        File dir = file.isDirectory() ? child : child.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
          LOG.warn("Failed to create dir: " + dir.getPath());
          return SKIP_CHILDREN;
        }
        if (file.isDirectory()) return CONTINUE;
        if (file.getFileType().isBinary()) return CONTINUE;
        if (file.getLength() > FileUtilRt.LARGE_FOR_CONTENT_LOADING) return CONTINUE;

        String newText = FileUtil.loadTextAndClose(file.getInputStream());
        String oldText = child.exists() ? FileUtil.loadFile(child) : "";
        String newHash = hash(newText);
        String oldHash = hash(oldText);
        boolean upToDate = StringUtil.equals(oldHash, newHash);
        if (upToDate) return CONTINUE;
        if (child.exists()) {
          renameToBackupCopy(child);
        }
        FileUtil.writeToFile(child, newText);
        return CONTINUE;
      }
    }, IOException.class);
  }

  @NotNull
  private static String hash(@NotNull String s) {
    MessageDigest md5 = DigestUtil.md5();
    StringBuilder sb = new StringBuilder();
    byte[] digest = md5.digest(s.getBytes(StandardCharsets.UTF_8));
    for (byte b : digest) {
      sb.append(Integer.toHexString(b));
    }
    return sb.toString();
  }

  private static void renameToBackupCopy(@NotNull File file) throws IOException {
    File parent = file.getParentFile();
    int i = 0;
    String newName = file.getName() + "." + BACKUP_FILE_EXTENSION;
    while (new File(parent, newName).exists()) {
      newName = file.getName() + "." + BACKUP_FILE_EXTENSION + "_" + i;
      i++;
    }
    FileUtil.rename(file, newName);
  }

  private void extractBundledExtensionsIfNeeded(@NotNull PluginId pluginId) throws IOException {
    if (!ApplicationManager.getApplication().isDispatchThread()) return;

    IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(pluginId);
    if (plugin == null || !ResourceVersions.getInstance().shouldUpdateResourcesOf(plugin)) return;

    extractBundledResources(pluginId, "");
    ResourceVersions.getInstance().resourcesUpdated(plugin);
  }
}
