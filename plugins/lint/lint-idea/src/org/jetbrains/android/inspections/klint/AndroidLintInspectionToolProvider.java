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

import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkVersionInfo;
import com.android.tools.klint.checks.*;
import com.android.tools.klint.detector.api.Issue;
import com.intellij.psi.PsiElement;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.sdk.AndroidSdkData;
import org.jetbrains.android.util.AndroidBundle;

import static com.android.tools.klint.checks.FragmentDetector.ISSUE;
import static com.android.tools.klint.checks.PluralsDetector.IMPLIED_QUANTITY;

/**
 * Registrations for all the various Lint rules as local IDE inspections, along with quickfixes for many of them
 */
public class AndroidLintInspectionToolProvider {
  public static class AndroidKLintAaptCrashInspection extends AndroidLintInspectionBase {
    public AndroidKLintAaptCrashInspection() {
      super(AndroidBundle.message("android.lint.inspections.aapt.crash"), ResourceCycleDetector.CRASH);
    }
  }
  public static class AndroidKLintInconsistentArraysInspection extends AndroidLintInspectionBase {
    public AndroidKLintInconsistentArraysInspection() {
      super(AndroidBundle.message("android.lint.inspections.inconsistent.arrays"), ArraySizeDetector.INCONSISTENT);
    }
  }

  public static class AndroidKLintInconsistentLayoutInspection extends AndroidLintInspectionBase {
    public AndroidKLintInconsistentLayoutInspection() {
      super(AndroidBundle.message("android.lint.inspections.inconsistent.layout"), LayoutConsistencyDetector.INCONSISTENT_IDS);
    }
  }

  public static class AndroidKLintDuplicateIncludedIdsInspection extends AndroidLintInspectionBase {
    public AndroidKLintDuplicateIncludedIdsInspection() {
      super(AndroidBundle.message("android.lint.inspections.duplicate.included.ids"), DuplicateIdDetector.CROSS_LAYOUT);
    }
  }

  public static class AndroidKLintIconExpectedSizeInspection extends AndroidLintInspectionBase {
    public AndroidKLintIconExpectedSizeInspection() {
      super(AndroidBundle.message("android.lint.inspections.icon.expected.size"), IconDetector.ICON_EXPECTED_SIZE);
    }
  }

  public static class AndroidKLintIconDipSizeInspection extends AndroidLintInspectionBase {
    public AndroidKLintIconDipSizeInspection() {
      super(AndroidBundle.message("android.lint.inspections.icon.dip.size"), IconDetector.ICON_DIP_SIZE);
    }
  }

  public static class AndroidKLintIconLocationInspection extends AndroidLintInspectionBase {
    public AndroidKLintIconLocationInspection() {
      super(AndroidBundle.message("android.lint.inspections.icon.location"), IconDetector.ICON_LOCATION);
    }
  }

  public static class AndroidKLintIconDensitiesInspection extends AndroidLintInspectionBase {
    public AndroidKLintIconDensitiesInspection() {
      super(AndroidBundle.message("android.lint.inspections.icon.densities"), IconDetector.ICON_DENSITIES);
    }
  }

  public static class AndroidKLintIconMissingDensityFolderInspection extends AndroidLintInspectionBase {
    public AndroidKLintIconMissingDensityFolderInspection() {
      super(AndroidBundle.message("android.lint.inspections.icon.missing.density.folder"), IconDetector.ICON_MISSING_FOLDER);
    }
  }

  public static class AndroidKLintIconMixedNinePatchInspection extends AndroidLintInspectionBase {
    public AndroidKLintIconMixedNinePatchInspection() {
      super(AndroidBundle.message("android.lint.inspections.icon.mixed.nine.patch"), IconDetector.ICON_MIX_9PNG);
    }
  }

  public static class AndroidKLintFullBackupContentInspection extends AndroidLintInspectionBase {
    public AndroidKLintFullBackupContentInspection() {
      super(AndroidBundle.message("android.lint.inspections.full.backup.content"), FullBackupContentDetector.ISSUE);
    }
  }

  public static class AndroidKLintGetInstanceInspection extends AndroidLintInspectionBase {
    public AndroidKLintGetInstanceInspection() {
      super(AndroidBundle.message("android.lint.inspections.get.instance"), CipherGetInstanceDetector.ISSUE);
    }
  }

  public static class AndroidKLintGifUsageInspection extends AndroidLintInspectionBase {
    public AndroidKLintGifUsageInspection() {
      super(AndroidBundle.message("android.lint.inspections.gif.usage"), IconDetector.GIF_USAGE);
    }
  }

  public static class AndroidKLintIconDuplicatesInspection extends AndroidLintInspectionBase {
    public AndroidKLintIconDuplicatesInspection() {
      super(AndroidBundle.message("android.lint.inspections.icon.duplicates"), IconDetector.DUPLICATES_NAMES);
    }
  }

  public static class AndroidKLintIconDuplicatesConfigInspection extends AndroidLintInspectionBase {
    public AndroidKLintIconDuplicatesConfigInspection() {
      super(AndroidBundle.message("android.lint.inspections.icon.duplicates.config"), IconDetector.DUPLICATES_CONFIGURATIONS);
    }
  }

  public static class AndroidKLintIconNoDpiInspection extends AndroidLintInspectionBase {
    public AndroidKLintIconNoDpiInspection() {
      super(AndroidBundle.message("android.lint.inspections.icon.no.dpi"), IconDetector.ICON_NODPI);
    }
  }

  public static class AndroidKLintOverdrawInspection extends AndroidLintInspectionBase {
    public AndroidKLintOverdrawInspection() {
      super(AndroidBundle.message("android.lint.inspections.overdraw"), OverdrawDetector.ISSUE);
    }
  }

  public static class AndroidKLintMissingSuperCallInspection extends AndroidLintInspectionBase {
    public AndroidKLintMissingSuperCallInspection() {
      super(AndroidBundle.message("android.lint.inspections.missing.super.call"), CallSuperDetector.ISSUE);
    }
  }

  public static class AndroidKLintMissingTranslationInspection extends AndroidLintInspectionBase {
    public AndroidKLintMissingTranslationInspection() {
      super(AndroidBundle.message("android.lint.inspections.missing.translation"), TranslationDetector.MISSING);
    }
  }

  public static class AndroidKLintExtraTranslationInspection extends AndroidLintInspectionBase {
    public AndroidKLintExtraTranslationInspection() {
      super(AndroidBundle.message("android.lint.inspections.extra.translation"), TranslationDetector.EXTRA);
    }
  }

  public static class AndroidKLintUnusedResourcesInspection extends AndroidLintInspectionBase {
    public AndroidKLintUnusedResourcesInspection() {
      super(AndroidBundle.message("android.lint.inspections.unused.resources"), UnusedResourceDetector.ISSUE);
    }
  }

  public static class AndroidKLintUnusedAttributeInspection extends AndroidLintInspectionBase {
    public AndroidKLintUnusedAttributeInspection() {
      super(AndroidBundle.message("android.lint.inspections.unused.attribute"), ApiDetector.UNUSED);
    }
  }

  public static class AndroidKLintUnusedIdsInspection extends AndroidLintInspectionBase {
    public AndroidKLintUnusedIdsInspection() {
      super(AndroidBundle.message("android.lint.inspections.unused.ids"), UnusedResourceDetector.ISSUE_IDS);
    }
  }

  public static class AndroidKLintAlwaysShowActionInspection extends AndroidLintInspectionBase {
    public AndroidKLintAlwaysShowActionInspection() {
      super(AndroidBundle.message("android.lint.inspections.always.show.action"), AlwaysShowActionDetector.ISSUE);
    }
  }

  public static class AndroidKLintAppCompatMethodInspection extends AndroidLintInspectionBase {
    public AndroidKLintAppCompatMethodInspection() {
      super(AndroidBundle.message("android.lint.inspections.app.compat.method"), AppCompatCallDetector.ISSUE);
    }
  }

  public static class AndroidKLintAppCompatResourceInspection extends AndroidLintInspectionBase {
    public AndroidKLintAppCompatResourceInspection() {
      super(AndroidBundle.message("android.lint.inspections.app.compat.resource"), AppCompatResourceDetector.ISSUE);
    }
  }

  public static class AndroidKLintAppIndexingApiErrorInspection extends AndroidLintInspectionBase {
    public AndroidKLintAppIndexingApiErrorInspection() {
      super(AndroidBundle.message("android.lint.inspections.appindexing.error"), AppIndexingApiDetector.ISSUE_ERROR);
    }
  }

  public static class AndroidKLintAppIndexingApiWarningInspection extends AndroidLintInspectionBase {
    public AndroidKLintAppIndexingApiWarningInspection() {
      super(AndroidBundle.message("android.lint.inspections.appindexing.warning"), AppIndexingApiDetector.ISSUE_WARNING);
    }
  }

  public static class AndroidKLintAssertInspection extends AndroidLintInspectionBase {
    public AndroidKLintAssertInspection() {
      super(AndroidBundle.message("android.lint.inspections.assert"), AssertDetector.ISSUE);
    }
  }

  public static class AndroidKLintStringFormatCountInspection extends AndroidLintInspectionBase {
    public AndroidKLintStringFormatCountInspection() {
      super(AndroidBundle.message("android.lint.inspections.string.format.count"), StringFormatDetector.ARG_COUNT);
    }
  }

  public static class AndroidKLintStringFormatMatchesInspection extends AndroidLintInspectionBase {
    public AndroidKLintStringFormatMatchesInspection() {
      super(AndroidBundle.message("android.lint.inspections.string.format.matches"), StringFormatDetector.ARG_TYPES);
    }
  }

  public static class AndroidKLintStringFormatInvalidInspection extends AndroidLintInspectionBase {
    public AndroidKLintStringFormatInvalidInspection() {
      super(AndroidBundle.message("android.lint.inspections.string.format.invalid"), StringFormatDetector.INVALID);
    }
  }

  public static class AndroidKLintWrongRegionInspection extends AndroidLintInspectionBase {
    public AndroidKLintWrongRegionInspection() {
      super(AndroidBundle.message("android.lint.inspections.wrong.region"), LocaleFolderDetector.WRONG_REGION);
    }
  }

  public static class AndroidKLintWrongViewCastInspection extends AndroidLintInspectionBase {
    public AndroidKLintWrongViewCastInspection() {
      super(AndroidBundle.message("android.lint.inspections.wrong.view.cast"), ViewTypeDetector.ISSUE);
    }
  }

  public static class AndroidKLintUnknownIdInspection extends AndroidLintInspectionBase {
    public AndroidKLintUnknownIdInspection() {
      super(AndroidBundle.message("android.lint.inspections.unknown.id"), WrongIdDetector.UNKNOWN_ID);
    }
  }

  public static class AndroidKLintCommitTransactionInspection extends AndroidLintInspectionBase {
    public AndroidKLintCommitTransactionInspection() {
      super(AndroidBundle.message("android.lint.inspections.commit.transaction"), CleanupDetector.COMMIT_FRAGMENT);
    }
  }

  /**
   * Local inspections processed by AndroidLintExternalAnnotator
   */
  public static class AndroidKLintContentDescriptionInspection extends AndroidLintInspectionBase {
    public AndroidKLintContentDescriptionInspection() {
      super(AndroidBundle.message("android.lint.inspections.content.description"), AccessibilityDetector.ISSUE);
    }
  }

  public static class AndroidKLintButtonOrderInspection extends AndroidLintInspectionBase {
    public AndroidKLintButtonOrderInspection() {
      super(AndroidBundle.message("android.lint.inspections.button.order"), ButtonDetector.ORDER);
    }
  }

  public static class AndroidKLintBackButtonInspection extends AndroidLintInspectionBase {
    public AndroidKLintBackButtonInspection() {
      super(AndroidBundle.message("android.lint.inspections.back.button"), ButtonDetector.BACK_BUTTON);
    }
  }

  public static class AndroidKLintButtonCaseInspection extends AndroidLintInspectionBase {
    public AndroidKLintButtonCaseInspection() {
      super(AndroidBundle.message("android.lint.inspections.button.case"), ButtonDetector.CASE);
    }
  }

  public static class AndroidKLintExtraTextInspection extends AndroidLintInspectionBase {
    public AndroidKLintExtraTextInspection() {
      super(AndroidBundle.message("android.lint.inspections.extra.text"), ExtraTextDetector.ISSUE);
    }
  }

  public static class AndroidKLintHandlerLeakInspection extends AndroidLintInspectionBase {
    public AndroidKLintHandlerLeakInspection() {
      super(AndroidBundle.message("android.lint.inspections.handler.leak"), HandlerDetector.ISSUE);
    }
  }

  public static class AndroidKLintHardcodedDebugModeInspection extends AndroidLintInspectionBase {
    public AndroidKLintHardcodedDebugModeInspection() {
      super(AndroidBundle.message("android.lint.inspections.hardcoded.debug.mode"), HardcodedDebugModeDetector.ISSUE);
    }
  }

  public static class AndroidKLintDrawAllocationInspection extends AndroidLintInspectionBase {
    public AndroidKLintDrawAllocationInspection() {
      super(AndroidBundle.message("android.lint.inspections.draw.allocation"), JavaPerformanceDetector.PAINT_ALLOC);
    }
  }

  public static class AndroidKLintUseSparseArraysInspection extends AndroidLintInspectionBase {
    public AndroidKLintUseSparseArraysInspection() {
      super(AndroidBundle.message("android.lint.inspections.use.sparse.arrays"), JavaPerformanceDetector.USE_SPARSE_ARRAY);
    }
  }

  public static class AndroidKLintUseValueOfInspection extends AndroidLintInspectionBase {
    public AndroidKLintUseValueOfInspection() {
      super(AndroidBundle.message("android.lint.inspections.use.value.of"), JavaPerformanceDetector.USE_VALUE_OF);
    }
  }

  public static class AndroidKLintLibraryCustomViewInspection extends AndroidLintInspectionBase {
    public AndroidKLintLibraryCustomViewInspection() {
      // TODO: Quickfix
      super(AndroidBundle.message("android.lint.inspections.library.custom.view"), NamespaceDetector.CUSTOM_VIEW);
    }
  }

  public static class AndroidKLintPackageManagerGetSignaturesInspection extends AndroidLintInspectionBase {
    public AndroidKLintPackageManagerGetSignaturesInspection() {
      super(AndroidBundle.message("android.lint.inspections.package.manager.get.signatures"), GetSignaturesDetector.ISSUE);
    }
  }

  public static class AndroidKLintParcelCreatorInspection extends AndroidLintInspectionBase {
    public AndroidKLintParcelCreatorInspection() {
      super(AndroidBundle.message("android.lint.inspections.parcel.creator"), ParcelDetector.ISSUE);
    }
  }

  public static class AndroidKLintPluralsCandidateInspection extends AndroidLintInspectionBase {
    public AndroidKLintPluralsCandidateInspection() {
      super(AndroidBundle.message("android.lint.inspections.plurals.candidate"), StringFormatDetector.POTENTIAL_PLURAL);
    }
  }

  public static class AndroidKLintPrivateResourceInspection extends AndroidLintInspectionBase {
    public AndroidKLintPrivateResourceInspection() {
      super(AndroidBundle.message("android.lint.inspections.private.resource"), PrivateResourceDetector.ISSUE);
    }
  }

  public static class AndroidKLintSdCardPathInspection extends AndroidLintInspectionBase {
    public AndroidKLintSdCardPathInspection() {
      super(AndroidBundle.message("android.lint.inspections.sd.card.path"), SdCardDetector.ISSUE);
    }
  }

  public static class AndroidKLintTextViewEditsInspection extends AndroidLintInspectionBase {
    public AndroidKLintTextViewEditsInspection() {
      super(AndroidBundle.message("android.lint.inspections.text.view.edits"), TextViewDetector.ISSUE);
    }
  }

  public static class AndroidKLintEnforceUTF8Inspection extends AndroidLintInspectionBase {
    public AndroidKLintEnforceUTF8Inspection() {
      super(AndroidBundle.message("android.lint.inspections.enforce.utf8"), Utf8Detector.ISSUE);
    }
  }

  public static class AndroidKLintUnknownIdInLayoutInspection extends AndroidLintInspectionBase {
    public AndroidKLintUnknownIdInLayoutInspection() {
      super(AndroidBundle.message("android.lint.inspections.unknown.id.in.layout"), WrongIdDetector.UNKNOWN_ID_LAYOUT);
    }
  }

  public static class AndroidKLintSuspiciousImportInspection extends AndroidLintInspectionBase {
    public AndroidKLintSuspiciousImportInspection() {
      super(AndroidBundle.message("android.lint.inspections.suspicious.import"), WrongImportDetector.ISSUE);
    }
  }

  public static class AndroidKLintAccidentalOctalInspection extends AndroidLintInspectionBase {
    public AndroidKLintAccidentalOctalInspection() {
      super(AndroidBundle.message("android.lint.inspections.accidental.octal"), GradleDetector.ACCIDENTAL_OCTAL);
    }
  }

  public static class AndroidKLintAdapterViewChildrenInspection extends AndroidLintInspectionBase {
    public AndroidKLintAdapterViewChildrenInspection() {
      super(AndroidBundle.message("android.lint.inspections.adapter.view.children"), ChildCountDetector.ADAPTER_VIEW_ISSUE);
    }
  }

  public static class AndroidKLintSQLiteStringInspection extends AndroidLintInspectionBase {
    public AndroidKLintSQLiteStringInspection() {
      super(AndroidBundle.message("android.lint.inspections.sqlite.string"), SQLiteDetector.ISSUE);
    }
  }

  public static class AndroidKLintScrollViewCountInspection extends AndroidLintInspectionBase {
    public AndroidKLintScrollViewCountInspection() {
      super(AndroidBundle.message("android.lint.inspections.scroll.view.count"), ChildCountDetector.SCROLLVIEW_ISSUE);
    }
  }

  public static class AndroidKLintMissingPrefixInspection extends AndroidLintInspectionBase {
    public AndroidKLintMissingPrefixInspection() {
      super(AndroidBundle.message("android.lint.inspections.missing.prefix"), DetectMissingPrefix.MISSING_NAMESPACE);
    }
  }

  public static class AndroidKLintMissingQuantityInspection extends AndroidLintInspectionBase {
    public AndroidKLintMissingQuantityInspection() {
      // TODO: Add fixes
      super(AndroidBundle.message("android.lint.inspections.missing.quantity"), PluralsDetector.MISSING);
    }
  }

  public static class AndroidKLintUnusedQuantityInspection extends AndroidLintInspectionBase {
    public AndroidKLintUnusedQuantityInspection() {
      // TODO: Add fixes
      super(AndroidBundle.message("android.lint.inspections.unused.quantity"), PluralsDetector.EXTRA);
    }
  }

  public static class AndroidKLintDuplicateIdsInspection extends AndroidLintInspectionBase {
    public AndroidKLintDuplicateIdsInspection() {
      super(AndroidBundle.message("android.lint.inspections.duplicate.ids"), DuplicateIdDetector.WITHIN_LAYOUT);
    }
  }

  public static class AndroidKLintGridLayoutInspection extends AndroidLintInspectionBase {
    public AndroidKLintGridLayoutInspection() {
      super(AndroidBundle.message("android.lint.inspections.grid.layout"), GridLayoutDetector.ISSUE);
    }
  }

  public static class AndroidKLintHardcodedTextInspection extends AndroidLintInspectionBase {
    public AndroidKLintHardcodedTextInspection() {
      super(AndroidBundle.message("android.lint.inspections.hardcoded.text"), HardcodedValuesDetector.ISSUE);
    }
  }

  public static class AndroidKLintInefficientWeightInspection extends AndroidLintInspectionBase {
    public AndroidKLintInefficientWeightInspection() {
      super(AndroidBundle.message("android.lint.inspections.inefficient.weight"), InefficientWeightDetector.INEFFICIENT_WEIGHT);
    }
  }

  public static class AndroidKLintNestedWeightsInspection extends AndroidLintInspectionBase {
    public AndroidKLintNestedWeightsInspection() {
      super(AndroidBundle.message("android.lint.inspections.nested.weights"), InefficientWeightDetector.NESTED_WEIGHTS);
    }
  }

  public static class AndroidKLintDeprecatedInspection extends AndroidLintInspectionBase {
    public AndroidKLintDeprecatedInspection() {
      super(AndroidBundle.message("android.lint.inspections.deprecated"), DeprecationDetector.ISSUE);
    }
  }

  public static class AndroidKLintDeviceAdminInspection extends AndroidLintInspectionBase {
    public AndroidKLintDeviceAdminInspection() {
      // TODO: Add quickfix
      super(AndroidBundle.message("android.lint.inspections.device.admin"), ManifestDetector.DEVICE_ADMIN);
    }
  }

  public static class AndroidKLintDisableBaselineAlignmentInspection extends AndroidLintInspectionBase {
    public AndroidKLintDisableBaselineAlignmentInspection() {
      super(AndroidBundle.message("android.lint.inspections.disable.baseline.alignment"), InefficientWeightDetector.BASELINE_WEIGHTS);
    }
  }

  public static class AndroidKLintManifestOrderInspection extends AndroidLintInspectionBase {
    public AndroidKLintManifestOrderInspection() {
      super(AndroidBundle.message("android.lint.inspections.manifest.order"), ManifestDetector.ORDER);
    }
  }

  public static class AndroidKLintMockLocationInspection extends AndroidLintInspectionBase {
    public AndroidKLintMockLocationInspection() {
      super(AndroidBundle.message("android.lint.inspections.mock.location"), ManifestDetector.MOCK_LOCATION);
    }
  }

  public static class AndroidKLintMultipleUsesSdkInspection extends AndroidLintInspectionBase {
    public AndroidKLintMultipleUsesSdkInspection() {
      super(AndroidBundle.message("android.lint.inspections.multiple.uses.sdk"), ManifestDetector.MULTIPLE_USES_SDK);
    }
  }

  public static class AndroidKLintUsesMinSdkAttributesInspection extends AndroidLintInspectionBase {
    public AndroidKLintUsesMinSdkAttributesInspection() {
      super(AndroidBundle.message("android.lint.inspections.uses.min.sdk.attributes"), ManifestDetector.USES_SDK);
    }
  }

  public static class AndroidKLintUsingHttpInspection extends AndroidLintInspectionBase {
    public AndroidKLintUsingHttpInspection() {
      super(AndroidBundle.message("android.lint.inspections.using.http"), PropertyFileDetector.HTTP);
    }
  }

  public static class AndroidKLintValidFragmentInspection extends AndroidLintInspectionBase {
    public AndroidKLintValidFragmentInspection() {
      super(AndroidBundle.message("android.lint.inspections.valid.fragment"), ISSUE);
    }
  }

  public static class AndroidKLintViewConstructorInspection extends AndroidLintInspectionBase {
    public AndroidKLintViewConstructorInspection() {
      super(AndroidBundle.message("android.lint.inspections.view.constructor"), ViewConstructorDetector.ISSUE);
    }
  }

  public static class AndroidKLintViewHolderInspection extends AndroidLintInspectionBase {
    public AndroidKLintViewHolderInspection() {
      super(AndroidBundle.message("android.lint.inspections.view.holder"), ViewHolderDetector.ISSUE);
    }
  }

  public static class AndroidKLintWebViewLayoutInspection extends AndroidLintInspectionBase {
    public AndroidKLintWebViewLayoutInspection() {
      super(AndroidBundle.message("android.lint.inspections.web.view.layout"), WebViewDetector.ISSUE);
    }
  }

  public static class AndroidKLintMergeRootFrameInspection extends AndroidLintInspectionBase {
    public AndroidKLintMergeRootFrameInspection() {
      super(AndroidBundle.message("android.lint.inspections.merge.root.frame"), MergeRootFrameLayoutDetector.ISSUE);
    }
  }

  public static class AndroidKLintNegativeMarginInspection extends AndroidLintInspectionBase {
    public AndroidKLintNegativeMarginInspection() {
      super(AndroidBundle.message("android.lint.inspections.negative.margin"), NegativeMarginDetector.ISSUE);
    }
  }

  public static class AndroidKLintNestedScrollingInspection extends AndroidLintInspectionBase {
    public AndroidKLintNestedScrollingInspection() {
      super(AndroidBundle.message("android.lint.inspections.nested.scrolling"), NestedScrollingWidgetDetector.ISSUE);
    }
  }

  public static class AndroidKLintNewerVersionAvailableInspection extends AndroidLintInspectionBase {
    public AndroidKLintNewerVersionAvailableInspection() {
      super(AndroidBundle.message("android.lint.inspections.newer.version.available"), GradleDetector.REMOTE_VERSION);
    }
  }

  public static class AndroidKLintNfcTechWhitespaceInspection extends AndroidLintInspectionBase {
    public AndroidKLintNfcTechWhitespaceInspection() {
      super(AndroidBundle.message("android.lint.inspections.nfc.tech.whitespace"), NfcTechListDetector.ISSUE);
    }
  }

  public static class AndroidKLintNotSiblingInspection extends AndroidLintInspectionBase {
    public AndroidKLintNotSiblingInspection() {
      super(AndroidBundle.message("android.lint.inspections.not.sibling"), WrongIdDetector.NOT_SIBLING);
    }
  }

  public static class AndroidKLintObsoleteLayoutParamInspection extends AndroidLintInspectionBase {
    public AndroidKLintObsoleteLayoutParamInspection() {
      super(AndroidBundle.message("android.lint.inspections.obsolete.layout.param"), ObsoleteLayoutParamsDetector.ISSUE);
    }
  }

  public static class AndroidKLintProguardInspection extends AndroidLintInspectionBase {
    public AndroidKLintProguardInspection() {
      super(AndroidBundle.message("android.lint.inspections.proguard"), ProguardDetector.WRONG_KEEP);
    }
  }

  public static class AndroidKLintProguardSplitInspection extends AndroidLintInspectionBase {
    public AndroidKLintProguardSplitInspection() {
      super(AndroidBundle.message("android.lint.inspections.proguard.split"), ProguardDetector.SPLIT_CONFIG);
    }
  }

  public static class AndroidKLintPxUsageInspection extends AndroidLintInspectionBase {
    public AndroidKLintPxUsageInspection() {
      super(AndroidBundle.message("android.lint.inspections.px.usage"), PxUsageDetector.PX_ISSUE);
    }
  }

  public static class AndroidKLintScrollViewSizeInspection extends AndroidLintInspectionBase {
    public AndroidKLintScrollViewSizeInspection() {
      super(AndroidBundle.message("android.lint.inspections.scroll.view.size"), ScrollViewChildDetector.ISSUE);
    }
  }

  public static class AndroidKLintExportedServiceInspection extends AndroidLintInspectionBase {
    public AndroidKLintExportedServiceInspection() {
      super(AndroidBundle.message("android.lint.inspections.exported.service"), SecurityDetector.EXPORTED_SERVICE);
    }
  }

  public static class AndroidKLintGradleCompatibleInspection extends AndroidLintInspectionBase {
    public AndroidKLintGradleCompatibleInspection() {
      super(AndroidBundle.message("android.lint.inspections.gradle.compatible"), GradleDetector.COMPATIBILITY);
    }
  }

  public static class AndroidKLintGradleCompatiblePluginInspection extends AndroidLintInspectionBase {
    public AndroidKLintGradleCompatiblePluginInspection() {
      super(AndroidBundle.message("android.lint.inspections.gradle.plugin.compatible"), GradleDetector.GRADLE_PLUGIN_COMPATIBILITY);
    }
  }

  public static class AndroidKLintGradleDependencyInspection extends AndroidLintInspectionBase {
    public AndroidKLintGradleDependencyInspection() {
      super(AndroidBundle.message("android.lint.inspections.gradle.dependency"), GradleDetector.DEPENDENCY);
    }
  }

  public static class AndroidKLintGradleDeprecatedInspection extends AndroidLintInspectionBase {
    public AndroidKLintGradleDeprecatedInspection() {
      super(AndroidBundle.message("android.lint.inspections.gradle.deprecated"), GradleDetector.DEPRECATED);
    }
  }

  public static class AndroidKLintGradleDynamicVersionInspection extends AndroidLintInspectionBase {
    public AndroidKLintGradleDynamicVersionInspection() {
      super(AndroidBundle.message("android.lint.inspections.gradle.dynamic.version"), GradleDetector.PLUS);
    }
  }

  public static class AndroidKLintGradleGetterInspection extends AndroidLintInspectionBase {
    public AndroidKLintGradleGetterInspection() {
      super(AndroidBundle.message("android.lint.inspections.gradle.getter"), GradleDetector.GRADLE_GETTER);
    }
  }

    public static class AndroidKLintGradleIdeErrorInspection extends AndroidLintInspectionBase {
      public AndroidKLintGradleIdeErrorInspection() {
        super(AndroidBundle.message("android.lint.inspections.gradle.ide.error"), GradleDetector.IDE_SUPPORT);
      }

  }

  public static class AndroidKLintGradleOverridesInspection extends AndroidLintInspectionBase {
    public AndroidKLintGradleOverridesInspection() {
      super(AndroidBundle.message("android.lint.inspections.gradle.overrides"), ManifestDetector.GRADLE_OVERRIDES);
    }
  }

  public static class AndroidKLintGradlePathInspection extends AndroidLintInspectionBase {
    public AndroidKLintGradlePathInspection() {
      super(AndroidBundle.message("android.lint.inspections.gradle.path"), GradleDetector.PATH);
    }
  }

  public static class AndroidKLintGrantAllUrisInspection extends AndroidLintInspectionBase {
    public AndroidKLintGrantAllUrisInspection() {
      super(AndroidBundle.message("android.lint.inspections.grant.all.uris"), SecurityDetector.OPEN_PROVIDER);
    }
  }

  public static class AndroidKLintWorldWriteableFilesInspection extends AndroidLintInspectionBase {
    public AndroidKLintWorldWriteableFilesInspection() {
      super(AndroidBundle.message("android.lint.inspections.world.writeable.files"), SecurityDetector.WORLD_WRITEABLE);
    }
  }

  public static class AndroidKLintStateListReachableInspection extends AndroidLintInspectionBase {
    public AndroidKLintStateListReachableInspection() {
      super(AndroidBundle.message("android.lint.inspections.state.list.reachable"), StateListDetector.ISSUE);
    }
  }

  public static class AndroidKLintTextFieldsInspection extends AndroidLintInspectionBase {
    public AndroidKLintTextFieldsInspection() {
      super(AndroidBundle.message("android.lint.inspections.text.fields"), TextFieldDetector.ISSUE);
    }
  }

  public static class AndroidKLintTooManyViewsInspection extends AndroidLintInspectionBase {
    public AndroidKLintTooManyViewsInspection() {
      super(AndroidBundle.message("android.lint.inspections.too.many.views"), TooManyViewsDetector.TOO_MANY);
    }
  }

  public static class AndroidKLintTooDeepLayoutInspection extends AndroidLintInspectionBase {
    public AndroidKLintTooDeepLayoutInspection() {
      super(AndroidBundle.message("android.lint.inspections.too.deep.layout"), TooManyViewsDetector.TOO_DEEP);
    }
  }

  public static class AndroidKLintTypographyDashesInspection extends AndroidKLintTypographyInspectionBase {
    public AndroidKLintTypographyDashesInspection() {
      super(AndroidBundle.message("android.lint.inspections.typography.dashes"), TypographyDetector.DASHES);
    }
  }

  public static class AndroidKLintTypographyQuotesInspection extends AndroidKLintTypographyInspectionBase {
    public AndroidKLintTypographyQuotesInspection() {
      super(AndroidBundle.message("android.lint.inspections.typography.quotes"), TypographyDetector.QUOTES);
    }
  }

  public static class AndroidKLintTypographyFractionsInspection extends AndroidKLintTypographyInspectionBase {
    public AndroidKLintTypographyFractionsInspection() {
      super(AndroidBundle.message("android.lint.inspections.typography.fractions"), TypographyDetector.FRACTIONS);
    }
  }

  public static class AndroidKLintTypographyEllipsisInspection extends AndroidKLintTypographyInspectionBase {
    public AndroidKLintTypographyEllipsisInspection() {
      super(AndroidBundle.message("android.lint.inspections.typography.ellipsis"), TypographyDetector.ELLIPSIS);
    }
  }

  public static class AndroidKLintTypographyOtherInspection extends AndroidKLintTypographyInspectionBase {
    public AndroidKLintTypographyOtherInspection() {
      super(AndroidBundle.message("android.lint.inspections.typography.other"), TypographyDetector.OTHER);
    }
  }

  public static class AndroidKLintUseAlpha2Inspection extends AndroidLintInspectionBase {
    public AndroidKLintUseAlpha2Inspection() {
      super(AndroidBundle.message("android.lint.inspections.use.alpha2"), LocaleFolderDetector.USE_ALPHA_2);
    }
  }

  public static class AndroidKLintUseCompoundDrawablesInspection extends AndroidLintInspectionBase {
    public AndroidKLintUseCompoundDrawablesInspection() {
      super(AndroidBundle.message("android.lint.inspections.use.compound.drawables"), UseCompoundDrawableDetector.ISSUE);
    }

    // TODO: implement quickfix
  }

  public static class AndroidKLintUselessParentInspection extends AndroidLintInspectionBase {
    public AndroidKLintUselessParentInspection() {
      super(AndroidBundle.message("android.lint.inspections.useless.parent"), UselessViewDetector.USELESS_PARENT);
    }

    // TODO: implement quickfix
  }

  public static class AndroidKLintUselessLeafInspection extends AndroidLintInspectionBase {
    public AndroidKLintUselessLeafInspection() {
      super(AndroidBundle.message("android.lint.inspections.useless.leaf"), UselessViewDetector.USELESS_LEAF);
    }
  }

  private abstract static class AndroidKLintTypographyInspectionBase extends AndroidLintInspectionBase {
    public AndroidKLintTypographyInspectionBase(String displayName, Issue issue) {
      super(displayName, issue);
    }
  }

  public static class AndroidKLintNewApiInspection extends AndroidLintInspectionBase {
    public AndroidKLintNewApiInspection() {
      super(AndroidBundle.message("android.lint.inspections.new.api"), ApiDetector.UNSUPPORTED);
    }
  }

  public static class AndroidKLintInlinedApiInspection extends AndroidLintInspectionBase {
    public AndroidKLintInlinedApiInspection() {
      super(AndroidBundle.message("android.lint.inspections.inlined.api"), ApiDetector.INLINED);
    }
  }

  public static class AndroidKLintOverrideInspection extends AndroidLintInspectionBase {
    public AndroidKLintOverrideInspection() {
      super(AndroidBundle.message("android.lint.inspections.override"), ApiDetector.OVERRIDE);
    }
  }

  public static class AndroidKLintDuplicateUsesFeatureInspection extends AndroidLintInspectionBase {
    public AndroidKLintDuplicateUsesFeatureInspection() {
      super(AndroidBundle.message("android.lint.inspections.duplicate.uses.feature"), ManifestDetector.DUPLICATE_USES_FEATURE);
    }
  }

  public static class AndroidKLintMipmapIconsInspection extends AndroidLintInspectionBase {
    public AndroidKLintMipmapIconsInspection() {
      super(AndroidBundle.message("android.lint.inspections.mipmap.icons"), ManifestDetector.MIPMAP);
    }
  }

  public static class AndroidKLintMissingApplicationIconInspection extends AndroidLintInspectionBase {
    public AndroidKLintMissingApplicationIconInspection() {
      super(AndroidBundle.message("android.lint.inspections.missing.application.icon"), ManifestDetector.APPLICATION_ICON);
    }
  }

  public static class AndroidKLintResourceCycleInspection extends AndroidLintInspectionBase {
    public AndroidKLintResourceCycleInspection() {
      super(AndroidBundle.message("android.lint.inspections.resource.cycle"), ResourceCycleDetector.CYCLE);
    }
  }
  public static class AndroidKLintResourceNameInspection extends AndroidLintInspectionBase {
    public AndroidKLintResourceNameInspection() {
      super(AndroidBundle.message("android.lint.inspections.resource.name"), ResourcePrefixDetector.ISSUE);
    }
  }
  public static class AndroidKLintRtlCompatInspection extends AndroidLintInspectionBase {
    public AndroidKLintRtlCompatInspection() {
      super(AndroidBundle.message("android.lint.inspections.rtl.compat"), RtlDetector.COMPAT);
    }
  }
  public static class AndroidKLintRtlEnabledInspection extends AndroidLintInspectionBase {
    public AndroidKLintRtlEnabledInspection() {
      super(AndroidBundle.message("android.lint.inspections.rtl.enabled"), RtlDetector.ENABLED);
    }
  }
  public static class AndroidKLintRtlHardcodedInspection extends AndroidLintInspectionBase {
    public AndroidKLintRtlHardcodedInspection() {
      super(AndroidBundle.message("android.lint.inspections.rtl.hardcoded"), RtlDetector.USE_START);
    }
  }
  public static class AndroidKLintRtlSymmetryInspection extends AndroidLintInspectionBase {
    public AndroidKLintRtlSymmetryInspection() {
      super(AndroidBundle.message("android.lint.inspections.rtl.symmetry"), RtlDetector.SYMMETRY);
    }
  }

  // Missing the following issues, because they require classfile analysis:
  // FloatMath, FieldGetter, Override, OnClick, ViewTag, DefaultLocale, SimpleDateFormat,
  // Registered, MissingRegistered, Instantiatable, HandlerLeak, ValidFragment, SecureRandom,
  // ViewConstructor, Wakelock, Recycle, CommitTransaction, WrongCall, DalvikOverride

  // I think DefaultLocale is already handled by a regular IDEA code check.

  public static class AndroidKLintAddJavascriptInterfaceInspection extends AndroidLintInspectionBase {
    public AndroidKLintAddJavascriptInterfaceInspection() {
      super(AndroidBundle.message("android.lint.inspections.add.javascript.interface"), AddJavascriptInterfaceDetector.ISSUE);
    }
  }

  public static class AndroidKLintAllowBackupInspection extends AndroidLintInspectionBase {
    public AndroidKLintAllowBackupInspection() {
      super(AndroidBundle.message("android.lint.inspections.allow.backup"), ManifestDetector.ALLOW_BACKUP);
    }
  }

  public static class AndroidKLintButtonStyleInspection extends AndroidLintInspectionBase {
    public AndroidKLintButtonStyleInspection() {
      super(AndroidBundle.message("android.lint.inspections.button.style"), ButtonDetector.STYLE);
    }
  }

  public static class AndroidKLintByteOrderMarkInspection extends AndroidLintInspectionBase {
    public AndroidKLintByteOrderMarkInspection() {
      super(AndroidBundle.message("android.lint.inspections.byte.order.mark"), ByteOrderMarkDetector.BOM);
    }
  }

  public static class AndroidKLintCommitPrefEditsInspection extends AndroidLintInspectionBase {
    public AndroidKLintCommitPrefEditsInspection() {
      super(AndroidBundle.message("android.lint.inspections.commit.pref.edits"), SharedPrefsDetector.ISSUE);
    }
  }

  public static class AndroidKLintCustomViewStyleableInspection extends AndroidLintInspectionBase {
    public AndroidKLintCustomViewStyleableInspection() {
      super(AndroidBundle.message("android.lint.inspections.custom.view.styleable"), CustomViewDetector.ISSUE);
    }
  }

  public static class AndroidKLintCutPasteIdInspection extends AndroidLintInspectionBase {
    public AndroidKLintCutPasteIdInspection() {
      super(AndroidBundle.message("android.lint.inspections.cut.paste.id"), CutPasteDetector.ISSUE);
    }
  }
  public static class AndroidKLintDuplicateActivityInspection extends AndroidLintInspectionBase {
    public AndroidKLintDuplicateActivityInspection() {
      super(AndroidBundle.message("android.lint.inspections.duplicate.activity"), ManifestDetector.DUPLICATE_ACTIVITY);
    }
  }
  public static class AndroidKLintDuplicateDefinitionInspection extends AndroidLintInspectionBase {
    public AndroidKLintDuplicateDefinitionInspection() {
      super(AndroidBundle.message("android.lint.inspections.duplicate.definition"), DuplicateResourceDetector.ISSUE);
    }
  }
  public static class AndroidKLintEasterEggInspection extends AndroidLintInspectionBase {
    public AndroidKLintEasterEggInspection() {
      super(AndroidBundle.message("android.lint.inspections.easter.egg"), CommentDetector.EASTER_EGG);
    }
  }
  public static class AndroidKLintExportedContentProviderInspection extends AndroidLintInspectionBase {
    public AndroidKLintExportedContentProviderInspection() {
      super(AndroidBundle.message("android.lint.inspections.exported.content.provider"), SecurityDetector.EXPORTED_PROVIDER);
    }
  }
  public static class AndroidKLintExportedPreferenceActivityInspection extends AndroidLintInspectionBase {
    public AndroidKLintExportedPreferenceActivityInspection() {
      super(AndroidBundle.message("android.lint.inspections.exported.preference.activity"), PreferenceActivityDetector.ISSUE);
    }
  }
  public static class AndroidKLintExportedReceiverInspection extends AndroidLintInspectionBase {
    public AndroidKLintExportedReceiverInspection() {
      super(AndroidBundle.message("android.lint.inspections.exported.receiver"), SecurityDetector.EXPORTED_RECEIVER);
    }
  }
  public static class AndroidKLintIconColorsInspection extends AndroidLintInspectionBase {
    public AndroidKLintIconColorsInspection() {
      super(AndroidBundle.message("android.lint.inspections.icon.colors"), IconDetector.ICON_COLORS);
    }
  }
  public static class AndroidKLintIconExtensionInspection extends AndroidLintInspectionBase {
    public AndroidKLintIconExtensionInspection() {
      super(AndroidBundle.message("android.lint.inspections.icon.extension"), IconDetector.ICON_EXTENSION);
    }
  }
  public static class AndroidKLintIconLauncherShapeInspection extends AndroidLintInspectionBase {
    public AndroidKLintIconLauncherShapeInspection() {
      super(AndroidBundle.message("android.lint.inspections.icon.launcher.shape"), IconDetector.ICON_LAUNCHER_SHAPE);
    }
  }
  public static class AndroidKLintIconXmlAndPngInspection extends AndroidLintInspectionBase {
    public AndroidKLintIconXmlAndPngInspection() {
      super(AndroidBundle.message("android.lint.inspections.icon.xml.and.png"), IconDetector.ICON_XML_AND_PNG);
    }
  }
  public static class AndroidKLintIllegalResourceRefInspection extends AndroidLintInspectionBase {
    public AndroidKLintIllegalResourceRefInspection() {
      super(AndroidBundle.message("android.lint.inspections.illegal.resource.ref"), ManifestDetector.ILLEGAL_REFERENCE);
    }
  }

  public static class AndroidKLintImpliedQuantityInspection extends AndroidLintInspectionBase {
    public AndroidKLintImpliedQuantityInspection() {
      super(AndroidBundle.message("android.lint.inspections.implied.quantity"), IMPLIED_QUANTITY);
    }
  }

  public static class AndroidKLintIncludeLayoutParamInspection extends AndroidLintInspectionBase {
    public AndroidKLintIncludeLayoutParamInspection() {
      super(AndroidBundle.message("android.lint.inspections.include.layout.param"), IncludeDetector.ISSUE);
    }
  }

  public static class AndroidKLintInflateParamsInspection extends AndroidLintInspectionBase {
    public AndroidKLintInflateParamsInspection() {
      super(AndroidBundle.message("android.lint.inspections.inflate.params"), LayoutInflationDetector.ISSUE);
    }
  }
  public static class AndroidKLintInOrMmUsageInspection extends AndroidLintInspectionBase {
    public AndroidKLintInOrMmUsageInspection() {
      super(AndroidBundle.message("android.lint.inspections.in.or.mm.usage"), PxUsageDetector.IN_MM_ISSUE);
    }
  }
  public static class AndroidKLintInnerclassSeparatorInspection extends AndroidLintInspectionBase {
    public AndroidKLintInnerclassSeparatorInspection() {
      super(AndroidBundle.message("android.lint.inspections.innerclass.separator"), MissingClassDetector.INNERCLASS);
    }
  }

  public static class AndroidKLintInvalidIdInspection extends AndroidLintInspectionBase {
    public AndroidKLintInvalidIdInspection() {
      super(AndroidBundle.message("android.lint.inspections.invalid.id"), WrongIdDetector.INVALID);
    }
  }

  public static class AndroidKLintInvalidResourceFolderInspection extends AndroidLintInspectionBase {
    public AndroidKLintInvalidResourceFolderInspection() {
      super(AndroidBundle.message("android.lint.inspections.invalid.resource.folder"), LocaleFolderDetector.INVALID_FOLDER);
    }
  }

  public static class AndroidKLintJavascriptInterfaceInspection extends AndroidLintInspectionBase {
    public AndroidKLintJavascriptInterfaceInspection() {
      super(AndroidBundle.message("android.lint.inspections.javascript.interface"), JavaScriptInterfaceDetector.ISSUE);
    }
  }

  public static class AndroidKLintLabelForInspection extends AndroidLintInspectionBase {
    public AndroidKLintLabelForInspection() {
      super(AndroidBundle.message("android.lint.inspections.label.for"), LabelForDetector.ISSUE);
    }
  }
  public static class AndroidKLintLocaleFolderInspection extends AndroidLintInspectionBase {
    public AndroidKLintLocaleFolderInspection() {
      super(AndroidBundle.message("android.lint.inspections.locale.folder"), LocaleFolderDetector.DEPRECATED_CODE);
    }
  }
  public static class AndroidKLintLocalSuppressInspection extends AndroidLintInspectionBase {
    public AndroidKLintLocalSuppressInspection() {
      super(AndroidBundle.message("android.lint.inspections.local.suppress"), AnnotationDetector.ISSUE);
    }
  }

  public static class AndroidKLintLogConditionalInspection extends AndroidLintInspectionBase {
    public AndroidKLintLogConditionalInspection() {
      super(AndroidBundle.message("android.lint.inspections.log.conditional"), LogDetector.CONDITIONAL);
    }
  }

  public static class AndroidKLintLogTagMismatchInspection extends AndroidLintInspectionBase {
    public AndroidKLintLogTagMismatchInspection() {
      super(AndroidBundle.message("android.lint.inspections.log.tag.mismatch"), LogDetector.WRONG_TAG);
    }
  }

  public static class AndroidKLintLongLogTagInspection extends AndroidLintInspectionBase {
    public AndroidKLintLongLogTagInspection() {
      super(AndroidBundle.message("android.lint.inspections.long.log.tag"), LogDetector.LONG_TAG);
    }
  }

  // THIS ISSUE IS PROBABLY NOT NEEDED HERE!
  public static class AndroidKLintMangledCRLFInspection extends AndroidLintInspectionBase {
    public AndroidKLintMangledCRLFInspection() {
      super(AndroidBundle.message("android.lint.inspections.mangled.crlf"), DosLineEndingDetector.ISSUE);
    }
  }
  public static class AndroidKLintMenuTitleInspection extends AndroidLintInspectionBase {
    public AndroidKLintMenuTitleInspection() {
      super(AndroidBundle.message("android.lint.inspections.menu.title"), TitleDetector.ISSUE);
    }
  }

  public static class AndroidKLintMissingIdInspection extends AndroidLintInspectionBase {
    public AndroidKLintMissingIdInspection() {
      super(AndroidBundle.message("android.lint.inspections.missing.id"), MissingIdDetector.ISSUE);
    }
  }

  public static class AndroidKLintMissingVersionInspection extends AndroidLintInspectionBase {
    public AndroidKLintMissingVersionInspection() {
      super(AndroidBundle.message("android.lint.inspections.missing.version"), ManifestDetector.SET_VERSION);
    }
  }
  public static class AndroidKLintOldTargetApiInspection extends AndroidLintInspectionBase {
    public AndroidKLintOldTargetApiInspection() {
      super(AndroidBundle.message("android.lint.inspections.old.target.api"), ManifestDetector.TARGET_NEWER);
    }

    private static int getHighestApi(PsiElement element) {
      int max = SdkVersionInfo.HIGHEST_KNOWN_STABLE_API;
      AndroidFacet instance = AndroidFacet.getInstance(element);
      if (instance != null) {
        AndroidSdkData sdkData = instance.getSdkData();
        if (sdkData != null) {
          for (IAndroidTarget target : sdkData.getTargets()) {
            if (target.isPlatform()) {
              AndroidVersion version = target.getVersion();
              if (version.getApiLevel() > max && !version.isPreview()) {
                max = version.getApiLevel();
              }
            }
          }
        }
      }
      return max;
    }
  }

  public static class AndroidKLintOrientationInspection extends AndroidLintInspectionBase {
    public AndroidKLintOrientationInspection() {
      super(AndroidBundle.message("android.lint.inspections.orientation"), InefficientWeightDetector.ORIENTATION);
    }
  }

  public static class AndroidKLintOverrideAbstractInspection extends AndroidLintInspectionBase {
    public AndroidKLintOverrideAbstractInspection() {
      super(AndroidBundle.message("android.lint.inspections.override.abstract"), OverrideConcreteDetector.ISSUE);
    }
  }

  public static class AndroidKLintPackagedPrivateKeyInspection extends AndroidLintInspectionBase {
    public AndroidKLintPackagedPrivateKeyInspection() {
      super(AndroidBundle.message("android.lint.inspections.packaged.private.key"), PrivateKeyDetector.ISSUE);
    }
  }
  public static class AndroidKLintPropertyEscapeInspection extends AndroidLintInspectionBase {
    public AndroidKLintPropertyEscapeInspection() {
      super(AndroidBundle.message("android.lint.inspections.property.escape"), PropertyFileDetector.ESCAPE);
    }
  }

  public static class AndroidKLintProtectedPermissionsInspection extends AndroidLintInspectionBase {
    public AndroidKLintProtectedPermissionsInspection() {
      super(AndroidBundle.message("android.lint.inspections.protected.permissions"), SystemPermissionsDetector.ISSUE);
    }
  }

  public static class AndroidKLintRecycleInspection extends AndroidLintInspectionBase {
    public AndroidKLintRecycleInspection() {
      super(AndroidBundle.message("android.lint.inspections.recycle"), CleanupDetector.RECYCLE_RESOURCE);
    }
  }

  public static class AndroidKLintReferenceTypeInspection extends AndroidLintInspectionBase {
    public AndroidKLintReferenceTypeInspection() {
      super(AndroidBundle.message("android.lint.inspections.reference.type"), DuplicateResourceDetector.TYPE_MISMATCH);
    }
  }

  public static class AndroidKLintRegisteredInspection extends AndroidLintInspectionBase {
    public AndroidKLintRegisteredInspection() {
      super(AndroidBundle.message("android.lint.inspections.registered"), RegistrationDetector.ISSUE);
    }
  }

  public static class AndroidKLintRelativeOverlapInspection extends AndroidLintInspectionBase {
    public AndroidKLintRelativeOverlapInspection() {
      super(AndroidBundle.message("android.lint.inspections.relative.overlap"), RelativeOverlapDetector.ISSUE);
    }
  }

  public static class AndroidKLintRequiredSizeInspection extends AndroidLintInspectionBase {
    public AndroidKLintRequiredSizeInspection() {
      super(AndroidBundle.message("android.lint.inspections.required.size"), RequiredAttributeDetector.ISSUE);
    }
  }
  public static class AndroidKLintResAutoInspection extends AndroidLintInspectionBase {
    public AndroidKLintResAutoInspection() {
      super(AndroidBundle.message("android.lint.inspections.res.auto"), NamespaceDetector.RES_AUTO);
    }
  }
  public static class AndroidKLintSelectableTextInspection extends AndroidLintInspectionBase {
    public AndroidKLintSelectableTextInspection() {
      super(AndroidBundle.message("android.lint.inspections.selectable.text"), TextViewDetector.SELECTABLE);
    }
  }

  public static class AndroidKLintServiceCastInspection extends AndroidLintInspectionBase {
    public AndroidKLintServiceCastInspection() {
      super(AndroidBundle.message("android.lint.inspections.service.cast"), ServiceCastDetector.ISSUE);
    }
  }
  public static class AndroidKLintSetJavaScriptEnabledInspection extends AndroidLintInspectionBase {
    public AndroidKLintSetJavaScriptEnabledInspection() {
      super(AndroidBundle.message("android.lint.inspections.set.java.script.enabled"), SetJavaScriptEnabledDetector.ISSUE);
    }
  }

  public static class AndroidKLintShortAlarmInspection extends AndroidLintInspectionBase {
    public AndroidKLintShortAlarmInspection() {
      super(AndroidBundle.message("android.lint.inspections.short.alarm"), AlarmDetector.ISSUE);
    }
  }

  public static class AndroidKLintShowToastInspection extends AndroidLintInspectionBase {
    public AndroidKLintShowToastInspection() {
      super(AndroidBundle.message("android.lint.inspections.show.toast"), ToastDetector.ISSUE);
    }
  }
  public static class AndroidKLintSignatureOrSystemPermissionsInspection extends AndroidLintInspectionBase {
    public AndroidKLintSignatureOrSystemPermissionsInspection() {
      super(AndroidBundle.message("android.lint.inspections.signature.or.system.permissions"), SignatureOrSystemDetector.ISSUE);
    }
  }

  public static class AndroidKLintSimpleDateFormatInspection extends AndroidLintInspectionBase {
    public AndroidKLintSimpleDateFormatInspection() {
      super(AndroidBundle.message("android.lint.inspections.simple.date.format"), DateFormatDetector.DATE_FORMAT);
    }
  }

  public static class AndroidKLintSmallSpInspection extends AndroidLintInspectionBase {
    public AndroidKLintSmallSpInspection() {
      super(AndroidBundle.message("android.lint.inspections.small.sp"), PxUsageDetector.SMALL_SP_ISSUE);
    }
  }

  public static class AndroidKLintSpUsageInspection extends AndroidLintInspectionBase {
    public AndroidKLintSpUsageInspection() {
      super(AndroidBundle.message("android.lint.inspections.sp.usage"), PxUsageDetector.DP_ISSUE);
    }
  }

  // Maybe not relevant
  public static class AndroidKLintStopShipInspection extends AndroidLintInspectionBase {
    public AndroidKLintStopShipInspection() {
      super(AndroidBundle.message("android.lint.inspections.stop.ship"), CommentDetector.STOP_SHIP);
    }
  }

  public static class AndroidKLintStringShouldBeIntInspection extends AndroidLintInspectionBase {
    public AndroidKLintStringShouldBeIntInspection() {
      super(AndroidBundle.message("android.lint.inspections.string.should.be.int"), GradleDetector.STRING_INTEGER);
    }
  }

  public static class AndroidKLintSuspicious0dpInspection extends AndroidLintInspectionBase {
    public AndroidKLintSuspicious0dpInspection() {
      super(AndroidBundle.message("android.lint.inspections.suspicious0dp"), InefficientWeightDetector.WRONG_0DP);
    }
  }

  public static class AndroidKLintTyposInspection extends AndroidLintInspectionBase {
    public AndroidKLintTyposInspection() {
      super(AndroidBundle.message("android.lint.inspections.typos"), TypoDetector.ISSUE);
    }
  }

  public static class AndroidKLintUniquePermissionInspection extends AndroidLintInspectionBase {
    public AndroidKLintUniquePermissionInspection() {
      super(AndroidBundle.message("android.lint.inspections.unique.permission"), ManifestDetector.UNIQUE_PERMISSION);
    }
  }
  public static class AndroidKLintUnlocalizedSmsInspection extends AndroidLintInspectionBase {
    public AndroidKLintUnlocalizedSmsInspection() {
      super(AndroidBundle.message("android.lint.inspections.unlocalized.sms"), NonInternationalizedSmsDetector.ISSUE);
    }
  }
  public static class AndroidKLintWorldReadableFilesInspection extends AndroidLintInspectionBase {
    public AndroidKLintWorldReadableFilesInspection() {
      super(AndroidBundle.message("android.lint.inspections.world.readable.files"), SecurityDetector.WORLD_READABLE);
    }
  }
  public static class AndroidKLintWrongCallInspection extends AndroidLintInspectionBase {
    public AndroidKLintWrongCallInspection() {
      super(AndroidBundle.message("android.lint.inspections.wrong.call"), WrongCallDetector.ISSUE);
    }
  }

  public static class AndroidKLintWrongCaseInspection extends AndroidLintInspectionBase {
    public AndroidKLintWrongCaseInspection() {
      super(AndroidBundle.message("android.lint.inspections.wrong.case"), WrongCaseDetector.WRONG_CASE);
    }
  }

  public static class AndroidKLintWrongFolderInspection extends AndroidLintInspectionBase {
    public AndroidKLintWrongFolderInspection() {
      super(AndroidBundle.message("android.lint.inspections.wrong.folder"), WrongLocationDetector.ISSUE);
    }
  }
}
