// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps.builders.java.dependencyView;

import com.intellij.util.SmartList;
import org.jetbrains.jps.dependency.Usage;
import org.jetbrains.org.objectweb.asm.ClassReader;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Future;

public final class Callbacks {

    public interface ConstantRef {
        String getOwner();
        String getName();
        String getDescriptor();
    }

    public interface Backend {
        default void associate(String classFileName, String sourceFileName, ClassReader cr) {
            associate(classFileName, Collections.singleton(sourceFileName), cr);
        }
        default void associate(String classFileName, Collection<String> sources, ClassReader cr) {
            associate(classFileName, sources, cr, false);
        }
        void associate(String classFileName, Collection<String> sources, ClassReader cr, boolean isGenerated);
        void registerImports(String className, Collection<String> classImports, Collection<String> staticImports);
        void registerConstantReferences(String className, Collection<ConstantRef> cRefs);
        default void registerUsage(String className, Usage usage) {
        }
        default void registerUsage(Path source, Usage usage) {
        }
    }

    public static ConstantRef createConstantReference(String ownerClass, String fieldName, String descriptor) {
        return new ConstantRef() {
            @Override
            public String getOwner() {
                return ownerClass;
            }

            @Override
            public String getName() {
                return fieldName;
            }

            @Override
            public String getDescriptor() {
                return descriptor;
            }
        };
    }

    public static final class ConstantAffection {
        public static final ConstantAffection EMPTY = new ConstantAffection();
        private final boolean myKnown;
        private final Collection<File> myAffectedFiles;

        public ConstantAffection(final Collection<File> affectedFiles) {
            myAffectedFiles = affectedFiles;
            myKnown = true;
        }

        public ConstantAffection() {
            myKnown = false;
            myAffectedFiles = null;
        }

        public boolean isKnown(){
            return myKnown;
        }

        public Collection<File> getAffectedFiles (){
            return myAffectedFiles;
        }

        public static ConstantAffection compose(final Collection<? extends ConstantAffection> affections) {
            if (affections.isEmpty()) {
                return new ConstantAffection(Collections.emptyList()); // return a 'known' affection here
            }
            if (affections.size() == 1) {
                return affections.iterator().next();
            }
            for (ConstantAffection a : affections) {
                if (!a.isKnown()) {
                    return EMPTY;
                }
            }
            final Collection<File> affected = new SmartList<>();
            for (ConstantAffection affection : affections) {
                affected.addAll(affection.getAffectedFiles());
            }
            return new ConstantAffection(affected);
        }
    }
}
