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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.ATTR_SHOW_AS_ACTION;
import static com.android.SdkConstants.VALUE_ALWAYS;
import static com.android.SdkConstants.VALUE_IF_ROOM;

import com.android.annotations.NonNull;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector.JavaScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Node;
import lombok.ast.Select;

/**
 * Check which looks for usage of showAsAction="always" in menus (or
 * MenuItem.SHOW_AS_ACTION_ALWAYS in code), which is usually a style guide violation.
 * (Use ifRoom instead).
 */
public class AlwaysShowActionDetector extends ResourceXmlDetector implements JavaScanner {

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

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
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
                    context.report(ISSUE, location,
                            "Prefer \"`ifRoom`\" instead of \"`always`\"");
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

    // ---- Implements JavaScanner ----

    @Override
    public
    List<Class<? extends Node>> getApplicableNodeTypes() {
        return Collections.<Class<? extends Node>>singletonList(Select.class);
    }

    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        return new FieldAccessChecker(context);
    }

    private class FieldAccessChecker extends ForwardingAstVisitor {
        private final JavaContext mContext;

        public FieldAccessChecker(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitSelect(Select node) {
            String description = node.astIdentifier().astValue();
            boolean isIfRoom = description.equals("SHOW_AS_ACTION_IF_ROOM"); //$NON-NLS-1$
            boolean isAlways = description.equals("SHOW_AS_ACTION_ALWAYS");  //$NON-NLS-1$
            if ((isIfRoom || isAlways)
                    && node.astOperand().toString().equals("MenuItem")) { //$NON-NLS-1$
                if (isAlways) {
                    if (mContext.getDriver().isSuppressed(mContext, ISSUE, node)) {
                        return super.visitSelect(node);
                    }
                    if (mAlwaysFields == null) {
                        mAlwaysFields = new ArrayList<Location>();
                    }
                    mAlwaysFields.add(mContext.getLocation(node));
                } else {
                    mHasIfRoomRefs = true;
                }
            }

            return super.visitSelect(node);
        }
    }
}
