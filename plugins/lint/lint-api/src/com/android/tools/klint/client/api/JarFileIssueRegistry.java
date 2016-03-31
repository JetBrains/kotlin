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
package com.android.tools.lint.client.api;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Severity;
import com.android.utils.SdkUtils;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

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
    private static final String MF_LINT_REGISTRY = "Lint-Registry"; //$NON-NLS-1$

    private static Map<File, SoftReference<JarFileIssueRegistry>> sCache;

    private final List<Issue> myIssues;

    @NonNull
    static IssueRegistry get(@NonNull LintClient client, @NonNull File jarFile) throws IOException,
            ClassNotFoundException, IllegalAccessException, InstantiationException {
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
            jarFile = new JarFile(file);
            Manifest manifest = jarFile.getManifest();
            Attributes attrs = manifest.getMainAttributes();
            Object object = attrs.get(new Attributes.Name(MF_LINT_REGISTRY));
            if (object instanceof String) {
                String className = (String) object;
                // Make a class loader for this jar
                URL url = SdkUtils.fileToUrl(file);
                URLClassLoader loader = new URLClassLoader(new URL[]{url},
                        JarFileIssueRegistry.class.getClassLoader());
                Class<?> registryClass = Class.forName(className, true, loader);
                IssueRegistry registry = (IssueRegistry) registryClass.newInstance();
                myIssues.addAll(registry.getIssues());
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

    @NonNull
    @Override
    public List<Issue> getIssues() {
        return myIssues;
    }
}
