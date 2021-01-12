// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.service.impl;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.plugin.JpsPluginManager;
import org.jetbrains.jps.service.JpsServiceManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class JpsServiceManagerImpl extends JpsServiceManager {
  private final ConcurrentMap<Class, Object> myServices = new ConcurrentHashMap<>(16, 0.75f, 1);
  private final ConcurrentMap<Class, List<?>> myExtensions = new ConcurrentHashMap<>(16, 0.75f, 1);
  private final AtomicInteger myModificationStamp = new AtomicInteger(0);
  private volatile JpsPluginManager myPluginManager;

  @Override
  public <T> T getService(Class<T> serviceClass) {
    //noinspection unchecked
    T service = (T)myServices.get(serviceClass);
    if (service == null) {
      // confine costly service initialization to single thread for defined startup profile
      synchronized (myServices) {
        //noinspection unchecked
        service = (T)myServices.get(serviceClass);
        if (service == null) {
          final Iterator<T> iterator = ServiceLoader.load(serviceClass, serviceClass.getClassLoader()).iterator();
          if (!iterator.hasNext()) {
            throw new ServiceConfigurationError("Implementation for " + serviceClass + " not found");
          }
          final T loadedService = iterator.next();
          if (iterator.hasNext()) {
            throw new ServiceConfigurationError("More than one implementation for " + serviceClass + " found: " + loadedService.getClass() +
              " and " + iterator.next().getClass());
          }
          //noinspection unchecked
          service = (T)myServices.putIfAbsent(serviceClass, loadedService);
          if (service == null) {
            service = loadedService;
          }
        }
      }
    }
    return service;
  }

  @Override
  public <T> Iterable<T> getExtensions(Class<T> extensionClass) {
    List<?> cached = cleanupExtensionCache()? null : myExtensions.get(extensionClass);
    if (cached == null) {
      // confine costly service initialization to single thread for defined startup profile
      synchronized (myExtensions) {
        cached = myExtensions.get(extensionClass);
        if (cached == null) {
          final List<T> extensions = new ArrayList<>(loadExtensions(extensionClass));
          cached = myExtensions.putIfAbsent(extensionClass, extensions);
          if (cached == null) {
            cached = extensions;
          }
        }
      }
    }
    //noinspection unchecked
    return (List<T>)cached;
  }

  @ApiStatus.Internal
  public boolean cleanupExtensionCache() {
    synchronized (myExtensions) {
      JpsPluginManager manager = myPluginManager;
      if (manager != null) {
        int stamp = manager.getModificationStamp();
        if (myModificationStamp.getAndSet(stamp) != stamp) {
          myExtensions.clear();
          return true;
        }
      }
      return false;
    }
  }

  @NotNull
  private <T> Collection<T> loadExtensions(Class<T> extensionClass) {
    JpsPluginManager pluginManager = myPluginManager;
    if (pluginManager == null || !pluginManager.isFullyLoaded()) {
      Iterator<JpsPluginManager> managers = ServiceLoader.load(JpsPluginManager.class, JpsPluginManager.class.getClassLoader()).iterator();
      if (managers.hasNext()) {
        try {
          pluginManager = managers.next();
        }
        catch (ServiceConfigurationError e) {
          Throwable cause = e.getCause();
          if (cause instanceof ProcessCanceledException) {
            throw (ProcessCanceledException)cause;
          }
          throw e;
        }
        if (managers.hasNext()) {
          throw new ServiceConfigurationError("More than one implementation of " + JpsPluginManager.class + " found: " + pluginManager.getClass() + " and " + managers.next().getClass());
        }
      }
      else {
        pluginManager = new SingleClassLoaderPluginManager();
      }
      myPluginManager = pluginManager;
    }
    return pluginManager.loadExtensions(extensionClass);
  }

  private static class SingleClassLoaderPluginManager extends JpsPluginManager {
    @NotNull
    @Override
    public <T> Collection<T> loadExtensions(@NotNull Class<T> extensionClass) {
      ServiceLoader<T> loader = ServiceLoader.load(extensionClass, extensionClass.getClassLoader());
      return ContainerUtil.newArrayList(loader);
    }

    @Override
    public boolean isFullyLoaded() {
      return true;
    }

    @Override
    public int getModificationStamp() {
      return 0;
    }
  }
}
