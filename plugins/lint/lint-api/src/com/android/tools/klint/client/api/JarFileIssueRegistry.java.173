/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.klint.client.api;

import static com.android.SdkConstants.DOT_CLASS;

import com.android.annotations.NonNull;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.utils.SdkUtils;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * <p> An {@link IssueRegistry} for a custom lint rule jar file. The rule jar should provide a
 * manifest entry with the key {@code Lint-Registry} and the value of the fully qualified name of an
 * implementation of {@link IssueRegistry} (with a default constructor). </p>
 *
 * <p> NOTE: The custom issue registry should not extend this file; it should be a plain
 * IssueRegistry! This file is used internally to wrap the given issue registry.</p>
 */
class JarFileIssueRegistry extends IssueRegistry {
    /**
     * Manifest constant for declaring an issue provider. Example: Lint-Registry:
     * foo.bar.CustomIssueRegistry
     */
    private static final String MF_LINT_REGISTRY_OLD = "Lint-Registry"; //$NON-NLS-1$
    private static final String MF_LINT_REGISTRY = "Lint-Registry-v2"; //$NON-NLS-1$

    private static Map<File, SoftReference<JarFileIssueRegistry>> sCache;

    private final List<Issue> myIssues;

    private boolean mHasLegacyDetectors;

    /** True if one or more java detectors were found that use the old Lombok-based API */
    public boolean hasLegacyDetectors() {
        return mHasLegacyDetectors;
    }

    @NonNull
    static JarFileIssueRegistry get(@NonNull LintClient client, @NonNull File jarFile)
            throws IOException, ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        if (sCache == null) {
           sCache = new HashMap<File, SoftReference<JarFileIssueRegistry>>();
        } else {
            SoftReference<JarFileIssueRegistry> reference = sCache.get(jarFile);
            if (reference != null) {
                JarFileIssueRegistry registry = reference.get();
                if (registry != null) {
                    return registry;
                }
            }
        }

        // Ensure that the scope-to-detector map doesn't return stale results
        IssueRegistry.reset();

        JarFileIssueRegistry registry = new JarFileIssueRegistry(client, jarFile);
        sCache.put(jarFile, new SoftReference<JarFileIssueRegistry>(registry));
        return registry;
    }

    private JarFileIssueRegistry(@NonNull LintClient client, @NonNull File file)
            throws IOException, ClassNotFoundException, IllegalAccessException,
                    InstantiationException {
        myIssues = Lists.newArrayList();
        JarFile jarFile = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            jarFile = new JarFile(file);
            Manifest manifest = jarFile.getManifest();
            Attributes attrs = manifest.getMainAttributes();
            Object object = attrs.get(new Attributes.Name(MF_LINT_REGISTRY));
            boolean isLegacy = false;
            if (object == null) {
                object = attrs.get(new Attributes.Name(MF_LINT_REGISTRY_OLD));
                //noinspection VariableNotUsedInsideIf
                if (object != null) {
                    // It's an old rule. We don't yet conclude that
                    //   mHasLegacyDetectors=true
                    // because the lint checks may not be Java related.
                    isLegacy = true;
                }
            }
            if (object instanceof String) {
                String className = (String) object;
                // Make a class loader for this jar
                URL url = SdkUtils.fileToUrl(file);
                ClassLoader loader = client.createUrlClassLoader(new URL[]{url},
                        JarFileIssueRegistry.class.getClassLoader());
                Class<?> registryClass = Class.forName(className, true, loader);
                IssueRegistry registry = (IssueRegistry) registryClass.newInstance();
                myIssues.addAll(registry.getIssues());

                if (isLegacy) {
                    // If it's an old registry, look through the issues to see if it
                    // provides Java scanning and if so create the old style visitors
                    for (Issue issue : myIssues) {
                        EnumSet<Scope> scope = issue.getImplementation().getScope();
                        if (scope.contains(Scope.JAVA_FILE) || scope.contains(Scope.JAVA_LIBRARIES)
                                || scope.contains(Scope.ALL_JAVA_FILES)) {
                            mHasLegacyDetectors = true;
                            break;
                        }
                    }
                }

                if (loader instanceof URLClassLoader) {
                    loadAndCloseURLClassLoader(client, file, (URLClassLoader)loader);
                }
            } else {
                client.log(Severity.ERROR, null,
                    "Custom lint rule jar %1$s does not contain a valid registry manifest key " +
                    "(%2$s).\n" +
                    "Either the custom jar is invalid, or it uses an outdated API not supported " +
                    "this lint client", file.getPath(), MF_LINT_REGISTRY);
            }
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
        }
    }

    /**
     * Work around http://bugs.java.com/bugdatabase/view_bug.do?bug_id=5041014 :
     * URLClassLoader, on Windows, locks the .jar file forever.
     * As of Java 7, there's a workaround: you can call close() when you're "done"
     * with the file. We'll do that here. However, the whole point of the
     * {@linkplain JarFileIssueRegistry} is that when lint is run over and over again
     * as the user is editing in the IDE and we're background checking the code, we
     * don't to keep loading the custom view classes over and over again: we want to
     * cache them. Therefore, just closing the URLClassLoader right away isn't great
     * either. However, it turns out it's safe to close the URLClassLoader once you've
     * loaded the classes you need, since the URLClassLoader will continue to serve
     * those classes even after its close() methods has been called.
     * <p>
     * Therefore, if we can call close() on this URLClassLoader, we'll proactively load
     * all class files we find in the .jar file, then close it.
     *
     * @param client the client to report errors to
     * @param file the .jar file
     * @param loader the URLClassLoader we should close
     */
    private static void loadAndCloseURLClassLoader(
            @NonNull LintClient client,
            @NonNull File file,
            @NonNull URLClassLoader loader) {
        try {
            // Proactively close out the .jar file. This is only available on Java 7.
            Method closeMethod = loader.getClass().getDeclaredMethod("close");

            // But first, proactively load all classes:
            try {
                InputStream inputStream = new FileInputStream(file);
                try {
                    JarInputStream jarInputStream = new JarInputStream(inputStream);
                    try {
                        ZipEntry entry = jarInputStream.getNextEntry();
                        while (entry != null) {
                            String name = entry.getName();
                            // Load non-inner-classes
                            if (name.endsWith(DOT_CLASS)) {
                                // Strip .class suffix and change .jar file path (/)
                                // to class name (.'s).
                                name = name.substring(0,
                                        name.length() - DOT_CLASS.length());
                                name = name.replace('/', '.');
                                try {
                                    Class.forName(name, true, loader);
                                } catch (Throwable e) {
                                    client.log(Severity.ERROR, e,
                                            "Failed to prefetch " + name + " from " + file);
                                }
                            }
                            entry = jarInputStream.getNextEntry();
                        }
                    } finally {
                        jarInputStream.close();
                    }
                } finally {
                    inputStream.close();
                }
            } catch (Throwable ignore) {
            } finally {
                // Finally close the URL class loader
                try {
                    closeMethod.invoke(loader);
                } catch (Throwable ignore) {
                    // Couldn't close. This is unlikely.
                }
            }
        } catch (NoSuchMethodException ignore) {
            // No close method - we're on 1.6
        }
    }

    @NonNull
    @Override
    public List<Issue> getIssues() {
        return myIssues;
    }
}
