/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static com.android.SdkConstants.ATTR_SHOW_AS_ACTION;
import static com.android.SdkConstants.VALUE_ALWAYS;
import static com.android.SdkConstants.VALUE_IF_ROOM;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.tools.klint.client.api.JavaEvaluator;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.ResourceXmlDetector;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.XmlContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;

import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.visitor.UastVisitor;
import org.w3c.dom.Attr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Check which looks for usage of showAsAction="always" in menus (or
 * MenuItem.SHOW_AS_ACTION_ALWAYS in code), which is usually a style guide violation.
 * (Use ifRoom instead).
 */
public class AlwaysShowActionDetector extends ResourceXmlDetector implements
        Detector.UastScanner {

    /** The main issue discovered by this detector */
    @SuppressWarnings("unchecked")
    public static final Issue ISSUE = Issue.create(
            "AlwaysShowAction", //$NON-NLS-1$
            "Usage of `showAsAction=always`",

            "Using `showAsAction=\"always\"` in menu XML, or `MenuItem.SHOW_AS_ACTION_ALWAYS` in " +
            "Java code is usually a deviation from the user interface style guide." +
            "Use `ifRoom` or the corresponding `MenuItem.SHOW_AS_ACTION_IF_ROOM` instead.\n" +
            "\n" +
            "If `always` is used sparingly there are usually no problems and behavior is " +
            "roughly equivalent to `ifRoom` but with preference over other `ifRoom` " +
            "items. Using it more than twice in the same menu is a bad idea.\n" +
            "\n" +
            "This check looks for menu XML files that contain more than two `always` " +
            "actions, or some `always` actions and no `ifRoom` actions. In Java code, " +
            "it looks for projects that contain references to `MenuItem.SHOW_AS_ACTION_ALWAYS` " +
            "and no references to `MenuItem.SHOW_AS_ACTION_IF_ROOM`.",

            Category.USABILITY,
            3,
            Severity.WARNING,
            new Implementation(
                    AlwaysShowActionDetector.class,
                    Scope.JAVA_AND_RESOURCE_FILES,
                    Scope.RESOURCE_FILE_SCOPE))
            .addMoreInfo("http://developer.android.com/design/patterns/actionbar.html"); //$NON-NLS-1$

    /** List of showAsAction attributes appearing in the current menu XML file */
    private List<Attr> mFileAttributes;
    /** If at least one location has been marked ignore in this file, ignore all */
    private boolean mIgnoreFile;
    /** List of locations of MenuItem.SHOW_AS_ACTION_ALWAYS references in Java code */
    private List<Location> mAlwaysFields;
    /** True if references to MenuItem.SHOW_AS_ACTION_IF_ROOM were found */
    private boolean mHasIfRoomRefs;

    /** Constructs a new {@link AlwaysShowActionDetector} */
    public AlwaysShowActionDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.MENU;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singletonList(ATTR_SHOW_AS_ACTION);
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        mFileAttributes = null;
    }

    @Override
    public void afterCheckFile(@NonNull Context context) {
        if (mIgnoreFile) {
            mFileAttributes = null;
            return;
        }
        if (mFileAttributes != null) {
            assert context instanceof XmlContext; // mFileAttributes is only set in XML files

            List<Attr> always = new ArrayList<Attr>();
            List<Attr> ifRoom = new ArrayList<Attr>();
            for (Attr attribute : mFileAttributes) {
                String value = attribute.getValue();
                if (value.equals(VALUE_ALWAYS)) {
                    always.add(attribute);
                } else if (value.equals(VALUE_IF_ROOM)) {
                    ifRoom.add(attribute);
                } else if (value.indexOf('|') != -1) {
                    String[] flags = value.split("\\|"); //$NON-NLS-1$
                    for (String flag : flags) {
                        if (flag.equals(VALUE_ALWAYS)) {
                            always.add(attribute);
                            break;
                        } else if (flag.equals(VALUE_IF_ROOM)) {
                            ifRoom.add(attribute);
                            break;
                        }
                    }
                }
            }

            if (!always.isEmpty() && mFileAttributes.size() > 1) {
                // Complain if you're using more than one "always", or if you're
                // using "always" and aren't using "ifRoom" at all (and provided you
                // have more than a single item)
                if (always.size() > 2 || ifRoom.isEmpty()) {
                    XmlContext xmlContext = (XmlContext) context;
                    Location location = null;
                    for (int i = always.size() - 1; i >= 0; i--) {
                        Location next = location;
                        location = xmlContext.getLocation(always.get(i));
                        if (next != null) {
                            location.setSecondary(next);
                        }
                    }
                    if (location != null) {
                        context.report(ISSUE, location,
                                "Prefer \"`ifRoom`\" instead of \"`always`\"");
                    }
                }
            }
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mAlwaysFields != null && !mHasIfRoomRefs) {
            for (Location location : mAlwaysFields) {
                context.report(ISSUE, location,
                    "Prefer \"`SHOW_AS_ACTION_IF_ROOM`\" instead of \"`SHOW_AS_ACTION_ALWAYS`\"");
            }
        }
    }

    // ---- Implements XmlScanner ----

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        if (context.getDriver().isSuppressed(context, ISSUE, attribute)) {
            mIgnoreFile = true;
            return;
        }

        if (mFileAttributes == null) {
            mFileAttributes = new ArrayList<Attr>();
        }
        mFileAttributes.add(attribute);
    }

    // ---- Implements UastScanner ----

    @Nullable
    @Override
    public List<String> getApplicableReferenceNames() {
        return Arrays.asList("SHOW_AS_ACTION_IF_ROOM", "SHOW_AS_ACTION_ALWAYS");
    }

    @Override
    public void visitReference(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UReferenceExpression reference, @NonNull PsiElement resolved) {
        if (resolved instanceof PsiField
                && JavaEvaluator.isMemberInClass((PsiField) resolved,
                "android.view.MenuItem")) {
            if ("SHOW_AS_ACTION_ALWAYS".equals(((PsiField) resolved).getName())) {
                if (context.getDriver().isSuppressed(context, ISSUE, reference)) {
                    return;
                }
                if (mAlwaysFields == null) {
                    mAlwaysFields = new ArrayList<Location>();
                }
                mAlwaysFields.add(context.getUastLocation(reference));
            } else {
                mHasIfRoomRefs = true;
            }
        }
    }
}
