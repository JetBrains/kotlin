// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.script;

import com.intellij.ide.extensionResources.ExtensionsRootType;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionNotApplicableException;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.concurrency.NonUrgentExecutor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

final class IdeStartupScripts implements ProjectManagerListener {
  private static final Logger LOG = Logger.getInstance(IdeStartupScripts.class);

  private static final String SCRIPT_DIR = "startup";

  private final AtomicBoolean isActive = new AtomicBoolean(true);

  IdeStartupScripts() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      throw ExtensionNotApplicableException.INSTANCE;
    }
  }

  @Override
  public void projectOpened(@NotNull Project project) {
    if (!isActive.compareAndSet(true, false)) {
      return;
    }

    CompletableFuture<List<Pair<File, IdeScriptEngine>>> future = CompletableFuture.supplyAsync(IdeStartupScripts::prepareScriptsAndEngines, NonUrgentExecutor.getInstance());
    WeakReference<Project> projectRef = new WeakReference<>(project);
    StartupManager.getInstance(project).runAfterOpened(() -> future.thenAcceptAsync(it -> {
      Project project1 = projectRef.get();
      if (project1 != null) {
        runAllScriptsImpl(project1, it);
      }
    }, NonUrgentExecutor.getInstance()));
  }

  private static @NotNull List<Pair<File, IdeScriptEngine>> prepareScriptsAndEngines() {
    List<File> scripts = getScripts();
    LOG.info(scripts.size() + " startup script(s) found");
    if (scripts.isEmpty()) return Collections.emptyList();

    IdeScriptEngineManager scriptEngineManager = IdeScriptEngineManager.getInstance();
    List<Pair<File, IdeScriptEngine>> result = new ArrayList<>();
    for (File script : scripts) {
      String extension = FileUtilRt.getExtension(script.getName());
      IdeScriptEngine engine = extension.isEmpty() ? null : scriptEngineManager.getEngineByFileExtension(extension, null);
      result.add(Pair.create(script, engine));
    }
    return result;
  }

  private static void runImpl(@NotNull Project project,
                              @NotNull File script,
                              @NotNull IdeScriptEngine engine) throws IOException, IdeScriptException {
    String scriptText = FileUtil.loadFile(script);
    IdeConsoleScriptBindings.ensureIdeIsBound(project, engine);

    LOG.info(script.getPath());
    long start = System.currentTimeMillis();
    try {
      engine.eval(scriptText);
    }
    finally {
      LOG.info("... completed in " + StringUtil.formatDuration(System.currentTimeMillis() - start));
    }
  }

  private static @NotNull List<File> getScripts() {
    File directory = getScriptsRootDirectory();
    List<File> scripts = JBIterable.of(directory == null ? null : directory.listFiles())
      .filter(ExtensionsRootType.regularFileFilter())
      .toList();

    ContainerUtil.sort(scripts, (f1, f2) -> {
      String f1Name = f1 != null ? f1.getName() : null;
      String f2Name = f2 != null ? f2.getName() : null;
      return StringUtil.compare(f1Name, f2Name, false);
    });
    return scripts;
  }

  private static @Nullable File getScriptsRootDirectory() {
    try {
      return ExtensionsRootType.getInstance().findResourceDirectory(PluginManagerCore.CORE_ID, SCRIPT_DIR, false);
    }
    catch (IOException ignore) {
    }
    return null;
  }

  private static void runAllScriptsImpl(@NotNull Project project, @NotNull List<Pair<File, IdeScriptEngine>> result) {
    try {
      for (Pair<File, IdeScriptEngine> pair : result) {
        try {
          if (pair.second == null) {
            LOG.warn(pair.first.getPath() + " not supported (no script engine)");
          }
          else {
            runImpl(project, pair.first, pair.second);
          }
        }
        catch (Exception e) {
          LOG.warn(e);
        }
      }
    }
    catch (ProcessCanceledException e) {
      LOG.warn("... cancelled");
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }
}
