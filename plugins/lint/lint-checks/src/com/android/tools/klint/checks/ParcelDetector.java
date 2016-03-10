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
package com.android.tools.klint.checks;

import com.android.annotations.NonNull;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.Speed;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.kinds.UastClassKind;
import org.jetbrains.uast.visitor.UastVisitor;

/**
 * Looks for Parcelable classes that are missing a CREATOR field
 */
public class ParcelDetector extends Detector implements UastScanner {

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

    /** Constructs a new {@link ParcelDetector} check */
    public ParcelDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements UastScanner ----

    @Override
    public UastVisitor createUastVisitor(UastAndroidContext context) {
        return new ParcelVisitor(context);
    }

    private static class ParcelVisitor extends UastVisitor {
        private final UastAndroidContext mContext;

        public ParcelVisitor(UastAndroidContext context) {
            mContext = context;
        }

        @Override
        public boolean visitClass(@NotNull UClass node) {
            // Only applies to concrete classes
            if (node.getKind() != UastClassKind.CLASS || node.hasModifier(UastModifier.ABSTRACT)) {
                return true;
            }

            for (UType reference : node.getSuperTypes()) {
                String name = reference.getName();
                if (name.equals("Parcelable")) {
                    UVariable field = UastUtils.findStaticMemberOfType(node, "CREATOR", UVariable.class);
                    if (field == null) {
                        // Make doubly sure that we're really implementing
                        // android.os.Parcelable
                        UClass parcelable = reference.resolve(mContext);
                        if (parcelable != null) {
                            if (!parcelable.isSubclassOf("android.os.Parcelable")) {
                                return true;
                            }
                        }
                        Location location = UastAndroidUtils.getLocation(node.getNameElement());
                        mContext.report(ISSUE, node, location,
                                        "This class implements `Parcelable` but does not "
                                        + "provide a `CREATOR` field");
                    }
                }
            }

            return true;
        }

    }
}
