/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.plugins.gradle.internal.daemon;

import org.gradle.api.internal.file.DefaultFileCollectionFactory;
import org.gradle.api.internal.file.IdentityFileResolver;
import org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory;
import org.gradle.api.internal.tasks.DefaultTaskDependencyFactory;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.api.tasks.util.internal.PatternSets;
import org.gradle.api.tasks.util.internal.PatternSpecFactory;
import org.gradle.initialization.BuildLayoutParameters;
import org.gradle.internal.Factory;
import org.gradle.internal.logging.events.OutputEvent;
import org.gradle.internal.logging.events.OutputEventListener;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.launcher.daemon.client.DaemonClientFactory;
import org.gradle.launcher.daemon.configuration.DaemonParameters;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Vladislav.Soroka
 */
public abstract class DaemonAction {
  private final String myServiceDirectoryPath;

  public DaemonAction(String serviceDirectoryPath) {
    myServiceDirectoryPath = serviceDirectoryPath;
  }

  protected ServiceRegistry getDaemonServices(DaemonClientFactory daemonClientFactory) {
    BuildLayoutParameters layout = new BuildLayoutParameters();
    if (myServiceDirectoryPath != null && !myServiceDirectoryPath.isEmpty()) {
      layout.setGradleUserHomeDir(new File(myServiceDirectoryPath));
    }
    DaemonParameters daemonParameters = getDaemonParameters(layout);
    return daemonClientFactory.createBuildClientServices(new OutputEventListener() {
      @Override
      public void onOutput(OutputEvent event) { }
    }, daemonParameters, new ByteArrayInputStream(new byte[0]));
  }

  @NotNull
  protected static DaemonParameters getDaemonParameters(BuildLayoutParameters layout) {
    DaemonParameters daemonParameters;
    boolean isGradle5Dot3OrNewer = GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("5.3")) >= 0;
    if (isGradle5Dot3OrNewer) {
      Factory<PatternSet> patternSetFactory = PatternSets.getPatternSetFactory(PatternSpecFactory.INSTANCE);
      daemonParameters = new DaemonParameters(layout, new DefaultFileCollectionFactory(new IdentityFileResolver(patternSetFactory),
                                                                                       DefaultTaskDependencyFactory
                                                                                         .withNoAssociatedProject(),
                                                                                       new DefaultDirectoryFileTreeFactory(),
                                                                                       patternSetFactory));
    }
    else {
      try {
        //noinspection JavaReflectionMemberAccess
        daemonParameters = DaemonParameters.class.getConstructor(BuildLayoutParameters.class)
          .newInstance(layout);
      }
      catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new RuntimeException("Cannot create DaemonParameters by reflection, gradle version " + GradleVersion.current(), e);
      }
    }
    return daemonParameters;
  }

  @NotNull
  protected static <T> T createCommand(Class<T> commandClass, Object id, byte[] token) {
    try {
      //noinspection unchecked
      return (T)commandClass.getConstructors()[0].newInstance(id, token);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
