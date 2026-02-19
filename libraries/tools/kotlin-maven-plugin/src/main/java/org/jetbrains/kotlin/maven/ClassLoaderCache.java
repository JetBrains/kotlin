/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A static holder for a thread-safe cache of class loaders.
 * Persists the cache between builds and different mojos using the same build process unless a different classloader is used for loading the Maven plugin.
 * In such cases, the remaining cache is garbage collected with the related class loader.
 * The cache utilizes a combination of time-based eviction and soft references for efficient resource usage.
 * Almost copied "as is" from KGP's `ClassLoadersCacheHolder`
 */
class ClassLoaderCache {
    static class ClassLoaderCacheKey {
        private final List<File> classpath;
        private final ParentClassLoaderProvider parentClassLoaderProvider;

        ClassLoaderCacheKey(List<File> classpath, ParentClassLoaderProvider parentClassLoaderProvider) {
            this.classpath = classpath;
            this.parentClassLoaderProvider = parentClassLoaderProvider;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ClassLoaderCacheKey key = (ClassLoaderCacheKey) o;
            return Objects.equals(classpath, key.classpath) &&
                   Objects.equals(parentClassLoaderProvider, key.parentClassLoaderProvider);
        }

        @Override
        public int hashCode() {
            return Objects.hash(classpath, parentClassLoaderProvider);
        }

        public List<File> getClasspath() {
            return classpath;
        }

        public ParentClassLoaderProvider getParentClassLoaderProvider() {
            return parentClassLoaderProvider;
        }
    }

    /**
     * The cache can be accessed by multiple compilations concurrently, hence the cache must be thread-safe.
     * As per `com.google.common.cache.Cache` documentation, implementations are expected to be thread-safe.
     * Utilizes the double-check locking singleton pattern, so @Volatile is crucial for correctness.
     */
    private static volatile Cache<@NotNull ClassLoaderCacheKey, @NotNull ClassLoader> classLoaders;
    private static final Object lock = new Object();

    static Cache<@NotNull ClassLoaderCacheKey, @NotNull ClassLoader> getCache(Long classLoaderCacheTimeoutInSeconds) {
        if (classLoaders == null) {
            synchronized(lock) {
                if (classLoaders == null) {
                    classLoaders = CacheBuilder.newBuilder()
                                           .expireAfterAccess(classLoaderCacheTimeoutInSeconds, TimeUnit.SECONDS)
                                           .softValues()
                                           .build();
                }
            }
        }
        return classLoaders;
    }
}

/**
 * A provider of the parent [ClassLoader] for newly created ClassLoader instances.
 * <p>
 * An implementation must override `equals` and `hashCode`! It's used as a part of a {@link Cache} key
 */
interface ParentClassLoaderProvider {
    ClassLoader getClassLoader();
}

class SharedBuildToolsApiClassesClassLoaderProvider implements ParentClassLoaderProvider {
    @Override
    public ClassLoader getClassLoader() {
        return SharedApiClassesClassLoader.newInstance();
    }

    @Override
    public int hashCode() {
        return SharedBuildToolsApiClassesClassLoaderProvider.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SharedBuildToolsApiClassesClassLoaderProvider;
    }
}