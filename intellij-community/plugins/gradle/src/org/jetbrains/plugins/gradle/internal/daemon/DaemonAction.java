// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.internal.daemon;

import org.gradle.api.internal.file.DefaultFileCollectionFactory;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.api.internal.file.IdentityFileResolver;
import org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory;
import org.gradle.api.internal.file.collections.DirectoryFileTreeFactory;
import org.gradle.api.internal.tasks.DefaultTaskDependencyFactory;
import org.gradle.api.internal.tasks.TaskDependencyFactory;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.api.tasks.util.internal.PatternSets;
import org.gradle.api.tasks.util.internal.PatternSpecFactory;
import org.gradle.initialization.BuildLayoutParameters;
import org.gradle.internal.Factory;
import org.gradle.internal.file.PathToFileResolver;
import org.gradle.internal.logging.events.OutputEvent;
import org.gradle.internal.logging.events.OutputEventListener;
import org.gradle.internal.nativeintegration.filesystem.FileSystem;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.launcher.daemon.client.DaemonClientFactory;
import org.gradle.launcher.daemon.configuration.DaemonParameters;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Constructor;
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
    // Constructors have changed for different versions of Gradle, need to use the correct version by reflection
    GradleVersion gradleBaseVersion = GradleVersion.current().getBaseVersion();
    if (gradleBaseVersion.compareTo(GradleVersion.version("6.4")) >= 0) {
      // DaemonParameters(BuildLayoutParameters, FileCollectionFactory) with DefaultFileCollectionFactory using
      // DefaultFileCollectionFactory(PathToFileResolver, TaskDependencyFactory, DirectoryFileTreeFactory, Factory<PatternSet>,
      //   PropertyHost, FileSystem) using IdentityFileResolver()
      return daemonParameters6Dot4(layout);
    }
    else if (gradleBaseVersion.compareTo(GradleVersion.version("6.3")) >= 0) {
      // DaemonParameters(BuildLayoutParameters, FileCollectionFactory) with DefaultFileCollectionFactory using
      // DefaultFileCollectionFactory(PathToFileResolver, TaskDependencyFactory, DirectoryFileTreeFactory, Factory<PatternSet>,
      //   PropertyHost, FileSystem)
      return daemonParameters6Dot3(layout);
    }
    else if (gradleBaseVersion.compareTo(GradleVersion.version("6.0")) >= 0) {
      // DaemonParameters(BuildLayoutParameters, FileCollectionFactory) with DefaultFileCollectionFactory using
      // DefaultFileCollectionFactory(PathToFileResolver, TaskDependencyFactory, DirectoryFileTreeFactory, Factory<PatternSet>)
      return daemonParameters6Dot0(layout);
    }
    else if (gradleBaseVersion.compareTo(GradleVersion.version("5.3")) >= 0) {
      // DaemonParameters(BuildLayoutParameters, FileCollectionFactory) with DefaultFileCollectionFactory constructor with no parameters
      return daemonParameters5Dot3(layout);
    }
    else {
      // DaemonParameters(BuildLayoutParameters)
      return daemonParametersPre5Dot3(layout);
    }
  }

  private static DaemonParameters daemonParameters6Dot4(BuildLayoutParameters layout) {
    try {
      Factory<PatternSet> patternSetFactory = PatternSets.getPatternSetFactory(PatternSpecFactory.INSTANCE);
      IdentityFileResolver identityFileResolver = IdentityFileResolver.class.getConstructor().newInstance();
      DefaultFileCollectionFactory collectionFactory = createCollectionFactory6Dot3(identityFileResolver, patternSetFactory);
      return new DaemonParameters(layout, collectionFactory);
    }
    catch (ClassNotFoundException | NoSuchFieldException | InstantiationException | IllegalAccessException | InvocationTargetException |
      NoSuchMethodException e) {
      throw new RuntimeException("Cannot create DaemonParameters by reflection, gradle version " + GradleVersion.current(), e);
    }
  }

  private static DaemonParameters daemonParameters6Dot3(BuildLayoutParameters layout) {
    try {
      Factory<PatternSet> patternSetFactory = PatternSets.getPatternSetFactory(PatternSpecFactory.INSTANCE);
      //noinspection JavaReflectionMemberAccess
      IdentityFileResolver identityFileResolver = IdentityFileResolver.class.getConstructor(Factory.class).newInstance(patternSetFactory);
      DefaultFileCollectionFactory collectionFactory = createCollectionFactory6Dot3(identityFileResolver, patternSetFactory);
      return new DaemonParameters(layout, collectionFactory);
    }
    catch (ClassNotFoundException | NoSuchFieldException | InstantiationException | IllegalAccessException | InvocationTargetException |
      NoSuchMethodException e) {
      throw new RuntimeException("Cannot create DaemonParameters by reflection, gradle version " + GradleVersion.current(), e);
    }
  }

  private static DefaultFileCollectionFactory createCollectionFactory6Dot3(IdentityFileResolver fileResolver,
                                                                           Factory<PatternSet> patternFactory)
    throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
           InstantiationException {
    ClassLoader classLoader = DaemonAction.class.getClassLoader();
    //noinspection rawtypes
    Class propertyHostClass = classLoader.loadClass("org.gradle.api.internal.provider.PropertyHost");
    Object propertyHostNoOp = propertyHostClass.getField("NO_OP").get(null);
    Constructor<DefaultFileCollectionFactory> collectionFactoryConstructor = DefaultFileCollectionFactory.class.getConstructor(
      PathToFileResolver.class, TaskDependencyFactory.class, DirectoryFileTreeFactory.class, Factory.class, propertyHostClass,
      FileSystem.class);
    return collectionFactoryConstructor.newInstance(fileResolver, DefaultTaskDependencyFactory.withNoAssociatedProject(),
                                                    new DefaultDirectoryFileTreeFactory(), patternFactory, propertyHostNoOp, null);
  }

  private static DaemonParameters daemonParameters6Dot0(BuildLayoutParameters layout) {
    try {
      Factory<PatternSet> patternSetFactory = PatternSets.getPatternSetFactory(PatternSpecFactory.INSTANCE);
      //noinspection JavaReflectionMemberAccess
      IdentityFileResolver identityFileResolver = IdentityFileResolver.class.getConstructor(Factory.class).newInstance(patternSetFactory);
      //noinspection JavaReflectionMemberAccess
      Constructor<DefaultFileCollectionFactory> collectionFactoryConstructor = DefaultFileCollectionFactory.class.getConstructor(
        PathToFileResolver.class, TaskDependencyFactory.class, DirectoryFileTreeFactory.class, Factory.class);
      DefaultFileCollectionFactory factory = collectionFactoryConstructor.newInstance(
        identityFileResolver, DefaultTaskDependencyFactory.withNoAssociatedProject(),
        new DefaultDirectoryFileTreeFactory(), patternSetFactory);
      return new DaemonParameters(layout, factory);
    }
    catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new RuntimeException("Cannot create DaemonParameters by reflection, gradle version " + GradleVersion.current(), e);
    }
  }

  private static DaemonParameters daemonParameters5Dot3(BuildLayoutParameters layout) {
    try {
      //noinspection JavaReflectionMemberAccess
      return DaemonParameters.class.getConstructor(BuildLayoutParameters.class, FileCollectionFactory.class).newInstance(
        layout, DefaultFileCollectionFactory.class.getConstructor().newInstance());
    }
    catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new RuntimeException("Cannot create DaemonParameters by reflection, gradle version " + GradleVersion.current(), e);
    }
  }

  private static DaemonParameters daemonParametersPre5Dot3(BuildLayoutParameters layout) {
    try {
      //noinspection JavaReflectionMemberAccess
      return DaemonParameters.class.getConstructor(BuildLayoutParameters.class).newInstance(layout);
    }
    catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new RuntimeException("Cannot create DaemonParameters by reflection, gradle version " + GradleVersion.current(), e);
    }
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
