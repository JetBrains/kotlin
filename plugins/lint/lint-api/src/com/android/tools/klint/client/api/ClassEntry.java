/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static com.android.SdkConstants.DOT_CLASS;
import static com.android.SdkConstants.DOT_JAR;
import static org.objectweb.asm.Opcodes.ASM5;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** A class, present either as a .class file on disk, or inside a .jar file. */
@VisibleForTesting
class ClassEntry implements Comparable<ClassEntry> {
    public final File file;
    public final File jarFile;
    public final File binDir;
    public final byte[] bytes;

    @VisibleForTesting
    ClassEntry(
            @NonNull File file,
            @Nullable File jarFile,
            @NonNull File binDir,
            @NonNull byte[] bytes) {
        super();
        this.file = file;
        this.jarFile = jarFile;
        this.binDir = binDir;
        this.bytes = bytes;
    }

    @NonNull
    public String path() {
        if (jarFile != null) {
            return jarFile.getPath() + ':' + file.getPath();
        } else {
            return file.getPath();
        }
    }

    @Override
    public int compareTo(@NonNull ClassEntry other) {
        String p1 = file.getPath();
        String p2 = other.file.getPath();
        int m1 = p1.length();
        int m2 = p2.length();
        if (m1 == m2 && p1.equals(p2)) {
            return 0;
        }
        int m = Math.min(m1, m2);

        for (int i = 0; i < m; i++) {
            char c1 = p1.charAt(i);
            char c2 = p2.charAt(i);
            if (c1 != c2) {
                // Sort Foo$Bar.class *after* Foo.class, even though $ < .
                if (c1 == '.' && c2 == '$') {
                    return -1;
                }
                if (c1 == '$' && c2 == '.') {
                    return 1;
                }
                return c1 - c2;
            }
        }

        return (m == m1) ? -1 : 1;
    }

    @Override
    public String toString() {
        return file.getPath();
    }

    /**
     * Creates a list of class entries from the given class path.
     *
     * @param client the client to report errors to and to use to read files
     * @param classPath the class path (directories and jar files) to scan
     * @param sort if true, sort the results
     * @return the list of class entries, never null.
     */
    @NonNull
    public static List<ClassEntry> fromClassPath(
            @NonNull LintClient client,
            @NonNull List<File> classPath,
            boolean sort) {
        if (!classPath.isEmpty()) {
            List<ClassEntry> libraryEntries = new ArrayList<ClassEntry>(64);
            addEntries(client, libraryEntries, classPath);
            if (sort) {
                Collections.sort(libraryEntries);
            }
            return libraryEntries;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Creates a list of class entries from the given class path and specific set of
     * files within it.
     *
     * @param client the client to report errors to and to use to read files
     * @param classFiles the specific set of class files to look for
     * @param classFolders the list of class folders to look in (to determine the
     *                     package root)
     * @param sort if true, sort the results
     * @return the list of class entries, never null.
     */
    @NonNull
    public static List<ClassEntry> fromClassFiles(
            @NonNull LintClient client,
            @NonNull List<File> classFiles, @NonNull List<File> classFolders,
            boolean sort) {
        List<ClassEntry> entries = new ArrayList<ClassEntry>(classFiles.size());

        if (!classFolders.isEmpty()) {
            for (File file : classFiles) {
                String path = file.getPath();
                if (file.isFile() && path.endsWith(DOT_CLASS)) {
                    try {
                        byte[] bytes = client.readBytes(file);
                        for (File dir : classFolders) {
                            if (path.startsWith(dir.getPath())) {
                                entries.add(new ClassEntry(file, null /* jarFile*/, dir,
                                        bytes));
                                break;
                            }
                        }
                    } catch (IOException e) {
                        client.log(e, null);
                    }
                }
            }

            if (sort && !entries.isEmpty()) {
                Collections.sort(entries);
            }
        }

        return entries;
    }

    /**
     * Given a classpath, add all the class files found within the directories and inside jar files
     */
    private static void addEntries(
            @NonNull LintClient client,
            @NonNull List<ClassEntry> entries,
            @NonNull List<File> classPath) {
        for (File classPathEntry : classPath) {
            if (classPathEntry.getName().endsWith(DOT_JAR)) {
                //noinspection UnnecessaryLocalVariable
                File jarFile = classPathEntry;
                if (!jarFile.exists()) {
                    continue;
                }
                ZipInputStream zis = null;
                try {
                    FileInputStream fis = new FileInputStream(jarFile);
                    try {
                        zis = new ZipInputStream(fis);
                        ZipEntry entry = zis.getNextEntry();
                        while (entry != null) {
                            String name = entry.getName();
                            if (name.endsWith(DOT_CLASS)) {
                                try {
                                    byte[] bytes = ByteStreams.toByteArray(zis);
                                    if (bytes != null) {
                                        File file = new File(entry.getName());
                                        entries.add(new ClassEntry(file, jarFile, jarFile, bytes));
                                    }
                                } catch (Exception e) {
                                    client.log(e, null);
                                    continue;
                                }
                            }

                            entry = zis.getNextEntry();
                        }
                    } finally {
                        Closeables.close(fis, true);
                    }
                } catch (IOException e) {
                    client.log(e, "Could not read jar file contents from %1$s", jarFile);
                } finally {
                    try {
                        Closeables.close(zis, true);
                    } catch (IOException e) {
                        // cannot happen
                    }
                }
            } else if (classPathEntry.isDirectory()) {
                //noinspection UnnecessaryLocalVariable
                File binDir = classPathEntry;
                List<File> classFiles = new ArrayList<File>();
                addClassFiles(binDir, classFiles);

                for (File file : classFiles) {
                    try {
                        byte[] bytes = client.readBytes(file);
                        entries.add(new ClassEntry(file, null /* jarFile*/, binDir, bytes));
                    } catch (IOException e) {
                        client.log(e, null);
                    }
                }
            } else {
                client.log(null, "Ignoring class path entry %1$s", classPathEntry);
            }
        }
    }

    /** Adds in all the .class files found recursively in the given directory */
    private static void addClassFiles(@NonNull File dir, @NonNull List<File> classFiles) {
        // Process the resource folder
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(DOT_CLASS)) {
                    classFiles.add(file);
                } else if (file.isDirectory()) {
                    // Recurse
                    addClassFiles(file, classFiles);
                }
            }
        }
    }

    /**
     * Creates a super class map (from class to its super class) for the given set of entries
     *
     * @param client the client to report errors to and to use to access files
     * @param libraryEntries the set of library entries to consult
     * @param classEntries the set of class entries to consult
     * @return a map from name to super class internal names
     */
    @NonNull
    public static Map<String, String> createSuperClassMap(
            @NonNull LintClient client,
            @NonNull List<ClassEntry> libraryEntries,
            @NonNull List<ClassEntry> classEntries) {
        int size = libraryEntries.size() + classEntries.size();
        Map<String, String> map = Maps.newHashMapWithExpectedSize(size);
        SuperclassVisitor visitor = new SuperclassVisitor(map);
        addSuperClasses(client, visitor, libraryEntries);
        addSuperClasses(client, visitor, classEntries);
        return map;
    }

    /**
     * Creates a super class map (from class to its super class) for the given set of entries
     *
     * @param client the client to report errors to and to use to access files
     * @param entries the set of library entries to consult
     * @return a map from name to super class internal names
     */
    @NonNull
    public static Map<String, String> createSuperClassMap(
            @NonNull LintClient client,
            @NonNull List<ClassEntry> entries) {
        Map<String, String> map = Maps.newHashMapWithExpectedSize(entries.size());
        SuperclassVisitor visitor = new SuperclassVisitor(map);
        addSuperClasses(client, visitor, entries);
        return map;
    }

    /** Adds in all the super classes found for the given class entries into the given map */
    private static void addSuperClasses(
            @NonNull LintClient client,
            @NonNull SuperclassVisitor visitor,
            @NonNull List<ClassEntry> entries) {
        for (ClassEntry entry : entries) {
            try {
                ClassReader reader = new ClassReader(entry.bytes);
                int flags = ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG
                        | ClassReader.SKIP_FRAMES;
                reader.accept(visitor, flags);
            } catch (Throwable t) {
                client.log(null, "Error processing %1$s: broken class file?", entry.path());
            }
        }
    }

    /** Visitor skimming classes and initializing a map of super classes */
    private static class SuperclassVisitor extends ClassVisitor {
        private final Map<String, String> mMap;

        public SuperclassVisitor(Map<String, String> map) {
            super(ASM5);
            mMap = map;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            // Record super class in the map (but don't waste space on java.lang.Object)
            if (superName != null && !"java/lang/Object".equals(superName)) {
                mMap.put(name, superName);
            }
        }
    }
}
