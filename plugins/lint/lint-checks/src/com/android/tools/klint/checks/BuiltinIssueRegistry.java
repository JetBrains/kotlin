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
import com.android.annotations.VisibleForTesting;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;

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

        issues.add(AccessibilityDetector.ISSUE);
        issues.add(AddJavascriptInterfaceDetector.ISSUE);
        issues.add(AlarmDetector.ISSUE);
        issues.add(AlwaysShowActionDetector.ISSUE);
        issues.add(AnnotationDetector.ISSUE);
        issues.add(ApiDetector.INLINED);
        issues.add(ApiDetector.OVERRIDE);
        issues.add(ApiDetector.UNSUPPORTED);
        issues.add(ApiDetector.UNUSED);
        issues.add(AppCompatCallDetector.ISSUE);
        issues.add(AppCompatResourceDetector.ISSUE);
        issues.add(AppIndexingApiDetector.ISSUE_ERROR);
        issues.add(AppIndexingApiDetector.ISSUE_WARNING);
        issues.add(ArraySizeDetector.INCONSISTENT);
        issues.add(AssertDetector.ISSUE);
        issues.add(ButtonDetector.BACK_BUTTON);
        issues.add(ButtonDetector.CASE);
        issues.add(ButtonDetector.ORDER);
        issues.add(ButtonDetector.STYLE);
        issues.add(ByteOrderMarkDetector.BOM);
        issues.add(CallSuperDetector.ISSUE);
        issues.add(ChildCountDetector.ADAPTER_VIEW_ISSUE);
        issues.add(ChildCountDetector.SCROLLVIEW_ISSUE);
        issues.add(CipherGetInstanceDetector.ISSUE);
        issues.add(CleanupDetector.COMMIT_FRAGMENT);
        issues.add(CleanupDetector.RECYCLE_RESOURCE);
        issues.add(ClickableViewAccessibilityDetector.ISSUE);
        issues.add(CommentDetector.EASTER_EGG);
        issues.add(CommentDetector.STOP_SHIP);
        issues.add(CustomViewDetector.ISSUE);
        issues.add(CutPasteDetector.ISSUE);
        issues.add(DateFormatDetector.DATE_FORMAT);
        issues.add(DeprecationDetector.ISSUE);
        issues.add(DetectMissingPrefix.MISSING_NAMESPACE);
        issues.add(DosLineEndingDetector.ISSUE);
        issues.add(DuplicateIdDetector.CROSS_LAYOUT);
        issues.add(DuplicateIdDetector.WITHIN_LAYOUT);
        issues.add(DuplicateResourceDetector.ISSUE);
        issues.add(DuplicateResourceDetector.TYPE_MISMATCH);
        issues.add(ExtraTextDetector.ISSUE);
        issues.add(FieldGetterDetector.ISSUE);
        issues.add(FullBackupContentDetector.ISSUE);
        issues.add(FragmentDetector.ISSUE);
        issues.add(GetSignaturesDetector.ISSUE);
        issues.add(GradleDetector.COMPATIBILITY);
        issues.add(GradleDetector.GRADLE_PLUGIN_COMPATIBILITY);
        issues.add(GradleDetector.DEPENDENCY);
        issues.add(GradleDetector.DEPRECATED);
        issues.add(GradleDetector.GRADLE_GETTER);
        issues.add(GradleDetector.IDE_SUPPORT);
        issues.add(GradleDetector.PATH);
        issues.add(GradleDetector.PLUS);
        issues.add(GradleDetector.STRING_INTEGER);
        issues.add(GradleDetector.REMOTE_VERSION);
        issues.add(GradleDetector.ACCIDENTAL_OCTAL);
        issues.add(GridLayoutDetector.ISSUE);
        issues.add(HandlerDetector.ISSUE);
        issues.add(HardcodedDebugModeDetector.ISSUE);
        issues.add(HardcodedValuesDetector.ISSUE);
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
        issues.add(IncludeDetector.ISSUE);
        issues.add(InefficientWeightDetector.BASELINE_WEIGHTS);
        issues.add(InefficientWeightDetector.INEFFICIENT_WEIGHT);
        issues.add(InefficientWeightDetector.NESTED_WEIGHTS);
        issues.add(InefficientWeightDetector.ORIENTATION);
        issues.add(InefficientWeightDetector.WRONG_0DP);
        issues.add(InvalidPackageDetector.ISSUE);
        issues.add(JavaPerformanceDetector.PAINT_ALLOC);
        issues.add(JavaPerformanceDetector.USE_SPARSE_ARRAY);
        issues.add(JavaPerformanceDetector.USE_VALUE_OF);
        issues.add(JavaScriptInterfaceDetector.ISSUE);
        issues.add(LabelForDetector.ISSUE);
        issues.add(LayoutConsistencyDetector.INCONSISTENT_IDS);
        issues.add(LayoutInflationDetector.ISSUE);
        issues.add(LocaleDetector.STRING_LOCALE);
        issues.add(LocaleFolderDetector.DEPRECATED_CODE);
        issues.add(LocaleFolderDetector.INVALID_FOLDER);
        issues.add(LocaleFolderDetector.WRONG_REGION);
        issues.add(LocaleFolderDetector.USE_ALPHA_2);
        issues.add(LogDetector.CONDITIONAL);
        issues.add(LogDetector.LONG_TAG);
        issues.add(LogDetector.WRONG_TAG);
        issues.add(ManifestDetector.ALLOW_BACKUP);
        issues.add(ManifestDetector.APPLICATION_ICON);
        issues.add(ManifestDetector.DEVICE_ADMIN);
        issues.add(ManifestDetector.DUPLICATE_ACTIVITY);
        issues.add(ManifestDetector.DUPLICATE_USES_FEATURE);
        issues.add(ManifestDetector.GRADLE_OVERRIDES);
        issues.add(ManifestDetector.ILLEGAL_REFERENCE);
        issues.add(ManifestDetector.MIPMAP);
        issues.add(ManifestDetector.MOCK_LOCATION);
        issues.add(ManifestDetector.MULTIPLE_USES_SDK);
        issues.add(ManifestDetector.ORDER);
        issues.add(ManifestDetector.SET_VERSION);
        issues.add(ManifestDetector.TARGET_NEWER);
        issues.add(ManifestDetector.UNIQUE_PERMISSION);
        issues.add(ManifestDetector.USES_SDK);
        issues.add(ManifestDetector.WRONG_PARENT);
        issues.add(ManifestTypoDetector.ISSUE);
        issues.add(MathDetector.ISSUE);
        issues.add(MergeRootFrameLayoutDetector.ISSUE);
        issues.add(MissingClassDetector.INNERCLASS);
        issues.add(MissingClassDetector.INSTANTIATABLE);
        issues.add(MissingClassDetector.MISSING);
        issues.add(MissingIdDetector.ISSUE);
        issues.add(NamespaceDetector.CUSTOM_VIEW);
        issues.add(NamespaceDetector.RES_AUTO);
        issues.add(NamespaceDetector.TYPO);
        issues.add(NamespaceDetector.UNUSED);
        issues.add(NegativeMarginDetector.ISSUE);
        issues.add(NestedScrollingWidgetDetector.ISSUE);
        issues.add(NfcTechListDetector.ISSUE);
        issues.add(NonInternationalizedSmsDetector.ISSUE);
        issues.add(ObsoleteLayoutParamsDetector.ISSUE);
        issues.add(OnClickDetector.ISSUE);
        issues.add(OverdrawDetector.ISSUE);
        issues.add(OverrideDetector.ISSUE);
        issues.add(OverrideConcreteDetector.ISSUE);
        issues.add(ParcelDetector.ISSUE);
        issues.add(PluralsDetector.EXTRA);
        issues.add(PluralsDetector.MISSING);
        issues.add(PluralsDetector.IMPLIED_QUANTITY);
        issues.add(PreferenceActivityDetector.ISSUE);
        issues.add(PrivateKeyDetector.ISSUE);
        issues.add(PrivateResourceDetector.ISSUE);
        issues.add(ProguardDetector.SPLIT_CONFIG);
        issues.add(ProguardDetector.WRONG_KEEP);
        issues.add(PropertyFileDetector.ESCAPE);
        issues.add(PropertyFileDetector.HTTP);
        issues.add(PxUsageDetector.DP_ISSUE);
        issues.add(PxUsageDetector.IN_MM_ISSUE);
        issues.add(PxUsageDetector.PX_ISSUE);
        issues.add(PxUsageDetector.SMALL_SP_ISSUE);
        issues.add(RegistrationDetector.ISSUE);
        issues.add(RelativeOverlapDetector.ISSUE);
        issues.add(RequiredAttributeDetector.ISSUE);
        issues.add(ResourceCycleDetector.CRASH);
        issues.add(ResourceCycleDetector.CYCLE);
        issues.add(ResourcePrefixDetector.ISSUE);
        issues.add(RtlDetector.COMPAT);
        issues.add(RtlDetector.ENABLED);
        issues.add(RtlDetector.SYMMETRY);
        issues.add(RtlDetector.USE_START);
        issues.add(ScrollViewChildDetector.ISSUE);
        issues.add(SdCardDetector.ISSUE);
        issues.add(SecureRandomDetector.ISSUE);
        issues.add(SecureRandomGeneratorDetector.ISSUE);
        issues.add(SecurityDetector.EXPORTED_PROVIDER);
        issues.add(SecurityDetector.EXPORTED_RECEIVER);
        issues.add(SecurityDetector.EXPORTED_SERVICE);
        issues.add(SecurityDetector.OPEN_PROVIDER);
        issues.add(SecurityDetector.WORLD_READABLE);
        issues.add(SecurityDetector.WORLD_WRITEABLE);
        issues.add(ServiceCastDetector.ISSUE);
        issues.add(SetJavaScriptEnabledDetector.ISSUE);
        issues.add(SharedPrefsDetector.ISSUE);
        issues.add(SignatureOrSystemDetector.ISSUE);
        issues.add(SQLiteDetector.ISSUE);
        issues.add(StateListDetector.ISSUE);
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
        issues.add(SystemPermissionsDetector.ISSUE);
        issues.add(TextFieldDetector.ISSUE);
        issues.add(TextViewDetector.ISSUE);
        issues.add(TextViewDetector.SELECTABLE);
        issues.add(TitleDetector.ISSUE);
        issues.add(ToastDetector.ISSUE);
        issues.add(TooManyViewsDetector.TOO_DEEP);
        issues.add(TooManyViewsDetector.TOO_MANY);
        issues.add(TranslationDetector.EXTRA);
        issues.add(TranslationDetector.MISSING);
        issues.add(TypoDetector.ISSUE);
        issues.add(TypographyDetector.DASHES);
        issues.add(TypographyDetector.ELLIPSIS);
        issues.add(TypographyDetector.FRACTIONS);
        issues.add(TypographyDetector.OTHER);
        issues.add(TypographyDetector.QUOTES);
        issues.add(UnusedResourceDetector.ISSUE);
        issues.add(UnusedResourceDetector.ISSUE_IDS);
        issues.add(UseCompoundDrawableDetector.ISSUE);
        issues.add(UselessViewDetector.USELESS_LEAF);
        issues.add(UselessViewDetector.USELESS_PARENT);
        issues.add(Utf8Detector.ISSUE);
        issues.add(ViewConstructorDetector.ISSUE);
        issues.add(ViewHolderDetector.ISSUE);
        issues.add(ViewTagDetector.ISSUE);
        issues.add(ViewTypeDetector.ISSUE);
        issues.add(WakelockDetector.ISSUE);
        issues.add(WebViewDetector.ISSUE);
        issues.add(WrongCallDetector.ISSUE);
        issues.add(WrongCaseDetector.WRONG_CASE);
        issues.add(WrongIdDetector.INVALID);
        issues.add(WrongIdDetector.NOT_SIBLING);
        issues.add(WrongIdDetector.UNKNOWN_ID);
        issues.add(WrongIdDetector.UNKNOWN_ID_LAYOUT);
        issues.add(WrongImportDetector.ISSUE);
        issues.add(WrongLocationDetector.ISSUE);

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

            if (scope.contains(Scope.JAVA_FILE)) {
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
