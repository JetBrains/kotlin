/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.tools.lint.checks;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import java.util.Collections;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.ImportDeclaration;
import lombok.ast.Node;

/**
 * Checks for "import android.R", which seems to be a common source of confusion
 * (see for example http://stackoverflow.com/questions/885009/r-cannot-be-resolved-android-error
 * and many other forums).
 * <p>
 * The root cause is probably this (from http://source.android.com/source/using-eclipse.html) :
 * <blockquote> Note: Eclipse sometimes likes to add an import android.R
 * statement at the top of your files that use resources, especially when you
 * ask eclipse to sort or otherwise manage imports. This will cause your make to
 * break. Look out for these erroneous import statements and delete them.
 * </blockquote>
 */
public class WrongImportDetector extends Detector implements Detector.JavaScanner {
    /** Is android.R being imported? */
    public static final Issue ISSUE = Issue.create(
            "SuspiciousImport", //$NON-NLS-1$
            "'`import android.R`' statement",
            "Importing `android.R` is usually not intentional; it sometimes happens when " +
            "you use an IDE and ask it to automatically add imports at a time when your " +
            "project's R class it not present.\n" +
            "\n" +
            "Once the import is there you might get a lot of \"confusing\" error messages " +
            "because of course the fields available on `android.R` are not the ones you'd " +
            "expect from just looking at your own `R` class.",
            Category.CORRECTNESS,
            9,
            Severity.WARNING,
            new Implementation(
                    WrongImportDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    /** Constructs a new {@link WrongImportDetector} check */
    public WrongImportDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements Detector.JavaScanner ----

    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes() {
        return Collections.<Class<? extends Node>> singletonList(
                ImportDeclaration.class);
    }

    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        return new ImportVisitor(context);
    }

    private static class ImportVisitor extends ForwardingAstVisitor {
        private final JavaContext mContext;

        public ImportVisitor(JavaContext context) {
            super();
            mContext = context;
        }

        @Override
        public boolean visitImportDeclaration(ImportDeclaration node) {
            String fqn = node.asFullyQualifiedName();
            if (fqn.equals("android.R")) { //$NON-NLS-1$
                Location location = mContext.getLocation(node);
                mContext.report(ISSUE, node, location,
                    "Don't include `android.R` here; use a fully qualified name for "
                            + "each usage instead");
            }
            return false;
        }
    }
}
