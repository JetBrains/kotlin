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

package com.android.tools.klint.checks;

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;
import com.android.tools.klint.client.api.IssueRegistry;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/** Registry which provides a list of checks to be performed on an Android project */
public class BuiltinIssueRegistry extends IssueRegistry {
    private static final List<Issue> sIssues;
    static final int INITIAL_CAPACITY = 220;

    static {
        List<Issue> issues = new ArrayList<Issue>(INITIAL_CAPACITY);

        issues.add(AddJavascriptInterfaceDetector.ISSUE);
        issues.add(AlarmDetector.ISSUE);
        issues.add(AlwaysShowActionDetector.ISSUE);
        issues.add(AnnotationDetector.ISSUE);
        issues.add(ApiDetector.INLINED);
        issues.add(ApiDetector.OVERRIDE);
        issues.add(ApiDetector.UNSUPPORTED);
        issues.add(AppCompatCallDetector.ISSUE);
        issues.add(AppCompatResourceDetector.ISSUE);
        issues.add(AssertDetector.ISSUE);
        issues.add(CallSuperDetector.ISSUE);
        issues.add(CipherGetInstanceDetector.ISSUE);
        issues.add(CleanupDetector.COMMIT_FRAGMENT);
        issues.add(CleanupDetector.RECYCLE_RESOURCE);
        issues.add(CommentDetector.EASTER_EGG);
        issues.add(CommentDetector.STOP_SHIP);
        issues.add(CustomViewDetector.ISSUE);
        issues.add(CutPasteDetector.ISSUE);
        issues.add(DateFormatDetector.DATE_FORMAT);
        issues.add(FullBackupContentDetector.ISSUE);
        issues.add(FragmentDetector.ISSUE);
        issues.add(GetSignaturesDetector.ISSUE);
        issues.add(HandlerDetector.ISSUE);
        issues.add(IconDetector.DUPLICATES_CONFIGURATIONS);
        issues.add(IconDetector.DUPLICATES_NAMES);
        issues.add(IconDetector.GIF_USAGE);
        issues.add(IconDetector.ICON_COLORS);
        issues.add(IconDetector.ICON_DENSITIES);
        issues.add(IconDetector.ICON_DIP_SIZE);
        issues.add(IconDetector.ICON_EXPECTED_SIZE);
        issues.add(IconDetector.ICON_EXTENSION);
        issues.add(IconDetector.ICON_LAUNCHER_SHAPE);
        issues.add(IconDetector.ICON_LOCATION);
        issues.add(IconDetector.ICON_MISSING_FOLDER);
        issues.add(IconDetector.ICON_MIX_9PNG);
        issues.add(IconDetector.ICON_NODPI);
        issues.add(IconDetector.ICON_XML_AND_PNG);
        issues.add(JavaPerformanceDetector.PAINT_ALLOC);
        issues.add(JavaPerformanceDetector.USE_SPARSE_ARRAY);
        issues.add(JavaPerformanceDetector.USE_VALUE_OF);
        issues.add(JavaScriptInterfaceDetector.ISSUE);
        issues.add(LayoutConsistencyDetector.INCONSISTENT_IDS);
        issues.add(LayoutInflationDetector.ISSUE);
        issues.add(LogDetector.CONDITIONAL);
        issues.add(LogDetector.LONG_TAG);
        issues.add(LogDetector.WRONG_TAG);
        issues.add(MergeRootFrameLayoutDetector.ISSUE);
        issues.add(NfcTechListDetector.ISSUE);
        issues.add(NonInternationalizedSmsDetector.ISSUE);
        issues.add(OverdrawDetector.ISSUE);
        issues.add(OverrideConcreteDetector.ISSUE);
        issues.add(ParcelDetector.ISSUE);
        issues.add(PreferenceActivityDetector.ISSUE);
        issues.add(PrivateResourceDetector.ISSUE);
        issues.add(RequiredAttributeDetector.ISSUE);
        issues.add(RtlDetector.COMPAT);
        issues.add(RtlDetector.ENABLED);
        issues.add(RtlDetector.SYMMETRY);
        issues.add(RtlDetector.USE_START);
        issues.add(SdCardDetector.ISSUE);
        issues.add(SecurityDetector.EXPORTED_PROVIDER);
        issues.add(SecurityDetector.EXPORTED_RECEIVER);
        issues.add(SecurityDetector.EXPORTED_SERVICE);
        issues.add(SecurityDetector.OPEN_PROVIDER);
        issues.add(SecurityDetector.WORLD_READABLE);
        issues.add(SecurityDetector.WORLD_WRITEABLE);
        issues.add(ServiceCastDetector.ISSUE);
        issues.add(SetJavaScriptEnabledDetector.ISSUE);
        issues.add(SharedPrefsDetector.ISSUE);
        issues.add(SQLiteDetector.ISSUE);
        issues.add(StringFormatDetector.ARG_COUNT);
        issues.add(StringFormatDetector.ARG_TYPES);
        issues.add(StringFormatDetector.INVALID);
        issues.add(StringFormatDetector.POTENTIAL_PLURAL);
        issues.add(SupportAnnotationDetector.CHECK_PERMISSION);
        issues.add(SupportAnnotationDetector.CHECK_RESULT);
        issues.add(SupportAnnotationDetector.COLOR_USAGE);
        issues.add(SupportAnnotationDetector.MISSING_PERMISSION);
        issues.add(SupportAnnotationDetector.RANGE);
        issues.add(SupportAnnotationDetector.RESOURCE_TYPE);
        issues.add(SupportAnnotationDetector.THREAD);
        issues.add(SupportAnnotationDetector.TYPE_DEF);
        issues.add(TitleDetector.ISSUE);
        issues.add(ToastDetector.ISSUE);
        issues.add(UnusedResourceDetector.ISSUE);
        issues.add(UnusedResourceDetector.ISSUE_IDS);
        issues.add(ViewConstructorDetector.ISSUE);
        issues.add(ViewHolderDetector.ISSUE);
        issues.add(ViewTypeDetector.ISSUE);
        issues.add(WrongCallDetector.ISSUE);
        issues.add(WrongImportDetector.ISSUE);

        sIssues = Collections.unmodifiableList(issues);
    }

    /**
     * Constructs a new {@link BuiltinIssueRegistry}
     */
    public BuiltinIssueRegistry() {
    }

    @NonNull
    @Override
    public List<Issue> getIssues() {
        return sIssues;
    }

    @Override
    protected int getIssueCapacity(@NonNull EnumSet<Scope> scope) {
        if (scope.equals(Scope.ALL)) {
            return getIssues().size();
        } else {
            int initialSize = 12;
            if (scope.contains(Scope.RESOURCE_FILE)) {
                initialSize += 75;
            } else if (scope.contains(Scope.ALL_RESOURCE_FILES)) {
                initialSize += 10;
            }

            if (scope.contains(Scope.SOURCE_FILE)) {
                initialSize += 55;
            } else if (scope.contains(Scope.CLASS_FILE)) {
                initialSize += 15;
            } else if (scope.contains(Scope.MANIFEST)) {
                initialSize += 30;
            } else if (scope.contains(Scope.GRADLE_FILE)) {
                initialSize += 5;
            }
            return initialSize;
        }
    }

    /**
     * Reset the registry such that it recomputes its available issues.
     * <p>
     * NOTE: This is only intended for testing purposes.
     */
    @VisibleForTesting
    public static void reset() {
        IssueRegistry.reset();
    }
}
