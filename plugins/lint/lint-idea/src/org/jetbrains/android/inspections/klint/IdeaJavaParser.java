/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.android.inspections.klint;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.client.api.JavaEvaluator;
import com.android.tools.klint.client.api.JavaParser;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Severity;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import lombok.ast.Node;
import lombok.ast.Position;
import org.jetbrains.uast.UastContext;

import java.io.File;
import java.util.List;

public class IdeaJavaParser extends JavaParser {
    private final IntellijLintClient myClient;
    private final Project myProject;
    private final UastContext myContext;
    private final JavaEvaluator myEvaluator;

    public IdeaJavaParser(IntellijLintClient client, Project myProject) {
        this.myClient = client;
        this.myProject = myProject;
        this.myEvaluator = new MyJavaEvaluator(myProject);

        myContext = ServiceManager.getService(myProject, UastContext.class);
    }

    @Override
    public UastContext getUastContext() {
        return myContext;
    }

    @Override
    public void prepareJavaParse(@NonNull List<JavaContext> contexts) {
        
    }

    @Override
    public PsiJavaFile parseJavaToPsi(@NonNull JavaContext context) {
        PsiFile psiFile = IntellijLintUtils.getPsiFile(context);
        if (!(psiFile instanceof PsiJavaFile)) {
            return null;
        }
        return (PsiJavaFile)psiFile;
    }

    @Override
    public JavaEvaluator getEvaluator() {
        return myEvaluator;
    }

    @Override
    public Project getIdeaProject() {
        return myProject;
    }

    @Override
    public Location getRangeLocation(
            @NonNull JavaContext context, @NonNull Node from, int fromDelta, @NonNull Node to, int toDelta
    ) {
        Position position1 = from.getPosition();
        Position position2 = to.getPosition();
        if (position1 == null) {
            return getLocation(context, to);
        }
        else if (position2 == null) {
            return getLocation(context, from);
        }

        int start = Math.max(0, from.getPosition().getStart() + fromDelta);
        int end = to.getPosition().getEnd() + toDelta;
        return Location.create(context.file, null, start, end);
    }

    @Override
    public Location.Handle createLocationHandle(@NonNull JavaContext context, @NonNull Node node) {
        return new LocationHandle(context.file, node);
    }

    @Override
    public void runReadAction(@NonNull Runnable runnable) {
        ApplicationManager.getApplication().runReadAction(runnable);
    }

    /* Handle for creating positions cheaply and returning full fledged locations later */
    private class LocationHandle implements Location.Handle {
        private final File myFile;
        private final Node myNode;
        private Object mClientData;

        public LocationHandle(File file, Node node) {
            myFile = file;
            myNode = node;
        }

        @NonNull
        @Override
        public Location resolve() {
            Position pos = myNode.getPosition();
            if (pos == null) {
                myClient.log(Severity.WARNING, null, "No position data found for node %1$s", myNode);
                return Location.create(myFile);
            }
            return Location.create(myFile, null /*contents*/, pos.getStart(), pos.getEnd());
        }

        @Override
        public void setClientData(@Nullable Object clientData) {
            mClientData = clientData;
        }

        @Override
        @Nullable
        public Object getClientData() {
            return mClientData;
        }
    }
    
    private static class MyJavaEvaluator extends JavaEvaluator {
        private final Project myProject;

        public MyJavaEvaluator(Project project) {
            myProject = project;
        }

        @Nullable
        @Override
        public PsiClass findClass(@NonNull String qualifiedName) {
            return JavaPsiFacade.getInstance(myProject).findClass(qualifiedName, GlobalSearchScope.allScope(myProject));
        }

        @Nullable
        @Override
        public PsiClassType getClassType(@Nullable PsiClass cls) {
            return cls != null ? JavaPsiFacade.getElementFactory(myProject).createType(cls) : null;
        }

        @NonNull
        @Override
        public PsiAnnotation[] getAllAnnotations(@NonNull PsiModifierListOwner owner) {
            return AnnotationUtil.getAllAnnotations(owner, true, null, true);
        }

        @Nullable
        @Override
        public PsiAnnotation findAnnotationInHierarchy(@NonNull PsiModifierListOwner listOwner, @NonNull String... annotationNames) {
            return AnnotationUtil.findAnnotationInHierarchy(listOwner, Sets.newHashSet(annotationNames));
        }

        @Nullable
        @Override
        public PsiAnnotation findAnnotation(@Nullable PsiModifierListOwner listOwner, @NonNull String... annotationNames) {
            return AnnotationUtil.findAnnotation(listOwner, false, annotationNames);
        }

        @Nullable
        @Override
        public File getFile(@NonNull PsiFile file) {
            VirtualFile virtualFile = file.getVirtualFile();
            return virtualFile != null ? VfsUtilCore.virtualToIoFile(virtualFile) : null;
        }
    }
}
