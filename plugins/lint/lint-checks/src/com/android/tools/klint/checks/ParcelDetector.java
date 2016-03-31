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
package com.android.tools.lint.checks;

import static com.android.tools.lint.client.api.JavaParser.ResolvedClass;
import static com.android.tools.lint.client.api.JavaParser.ResolvedField;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.ClassDeclaration;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Node;
import lombok.ast.TypeReference;

/**
 * Looks for Parcelable classes that are missing a CREATOR field
 */
public class ParcelDetector extends Detector implements Detector.JavaScanner {

    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "ParcelCreator", //$NON-NLS-1$
            "Missing Parcelable `CREATOR` field",

            "According to the `Parcelable` interface documentation, " +
            "\"Classes implementing the Parcelable interface must also have a " +
            "static field called `CREATOR`, which is an object implementing the " +
            "`Parcelable.Creator` interface.",

            Category.USABILITY,
            3,
            Severity.ERROR,
            new Implementation(
                    ParcelDetector.class,
                    Scope.JAVA_FILE_SCOPE))
            .addMoreInfo("http://developer.android.com/reference/android/os/Parcelable.html");

    /** Constructs a new {@link com.android.tools.lint.checks.ParcelDetector} check */
    public ParcelDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements JavaScanner ----

    @Nullable
    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes() {
        return Collections.<Class<? extends Node>>singletonList(ClassDeclaration.class);
    }

    @Nullable
    @Override
    public AstVisitor createJavaVisitor(@NonNull final JavaContext context) {
        return new ParcelVisitor(context);
    }

    private static class ParcelVisitor extends ForwardingAstVisitor {
        private final JavaContext mContext;

        public ParcelVisitor(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitClassDeclaration(ClassDeclaration node) {
            // Only applies to concrete classes
            int flags = node.astModifiers().getExplicitModifierFlags();
            if ((flags & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)) != 0) {
                return true;
            }

            if (node.astImplementing() != null)
                for (TypeReference reference : node.astImplementing()) {
                    String name = reference.astParts().last().astIdentifier().astValue();
                    if (name.equals("Parcelable")) {
                        JavaParser.ResolvedNode resolved = mContext.resolve(node);
                        if (resolved instanceof ResolvedClass) {
                            ResolvedClass cls = (ResolvedClass) resolved;
                            ResolvedField field = cls.getField("CREATOR", false);
                            if (field == null) {
                                // Make doubly sure that we're really implementing
                                // android.os.Parcelable
                                JavaParser.ResolvedNode r = mContext.resolve(reference);
                                if (r instanceof ResolvedClass) {
                                    ResolvedClass parcelable = (ResolvedClass) r;
                                    if (!parcelable.isSubclassOf("android.os.Parcelable", false)) {
                                        return true;
                                    }
                                }
                                Location location = mContext.getLocation(node.astName());
                                mContext.report(ISSUE, node, location,
                                        "This class implements `Parcelable` but does not "
                                                + "provide a `CREATOR` field");
                            }
                        }
                        break;
                    }
                }

            return true;
        }
    }
}
