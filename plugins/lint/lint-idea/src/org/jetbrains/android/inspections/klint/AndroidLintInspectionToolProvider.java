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

import com.android.tools.klint.checks.*;
import org.jetbrains.android.util.AndroidBundle;

import static com.android.tools.klint.checks.FragmentDetector.ISSUE;

/**
 * Registrations for all the various Lint rules as local IDE inspections, along with quickfixes for many of them
 */
public class AndroidLintInspectionToolProvider {
  public static class AndroidKLintInconsistentLayoutInspection extends AndroidLintInspectionBase {
    public AndroidKLintInconsistentLayoutInspection() {
      super(AndroidBundle.message("android.lint.inspections.inconsistent.layout"), LayoutConsistencyDetector.INCONSISTENT_IDS);
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

  public static class AndroidKLintWrongViewCastInspection extends AndroidLintInspectionBase {
    public AndroidKLintWrongViewCastInspection() {
      super(AndroidBundle.message("android.lint.inspections.wrong.view.cast"), ViewTypeDetector.ISSUE);
    }
  }

  public static class AndroidKLintCommitTransactionInspection extends AndroidLintInspectionBase {
    public AndroidKLintCommitTransactionInspection() {
      super(AndroidBundle.message("android.lint.inspections.commit.transaction"), CleanupDetector.COMMIT_FRAGMENT);
    }
  }

  public static class AndroidKLintHandlerLeakInspection extends AndroidLintInspectionBase {
    public AndroidKLintHandlerLeakInspection() {
      super(AndroidBundle.message("android.lint.inspections.handler.leak"), HandlerDetector.ISSUE);
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

  public static class AndroidKLintSuspiciousImportInspection extends AndroidLintInspectionBase {
    public AndroidKLintSuspiciousImportInspection() {
      super(AndroidBundle.message("android.lint.inspections.suspicious.import"), WrongImportDetector.ISSUE);
    }
  }

  public static class AndroidKLintSQLiteStringInspection extends AndroidLintInspectionBase {
    public AndroidKLintSQLiteStringInspection() {
      super(AndroidBundle.message("android.lint.inspections.sqlite.string"), SQLiteDetector.ISSUE);
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

  public static class AndroidKLintMergeRootFrameInspection extends AndroidLintInspectionBase {
    public AndroidKLintMergeRootFrameInspection() {
      super(AndroidBundle.message("android.lint.inspections.merge.root.frame"), MergeRootFrameLayoutDetector.ISSUE);
    }
  }

  public static class AndroidKLintNfcTechWhitespaceInspection extends AndroidLintInspectionBase {
    public AndroidKLintNfcTechWhitespaceInspection() {
      super(AndroidBundle.message("android.lint.inspections.nfc.tech.whitespace"), NfcTechListDetector.ISSUE);
    }
  }

  public static class AndroidKLintExportedServiceInspection extends AndroidLintInspectionBase {
    public AndroidKLintExportedServiceInspection() {
      super(AndroidBundle.message("android.lint.inspections.exported.service"), SecurityDetector.EXPORTED_SERVICE);
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

  public static class AndroidKLintInflateParamsInspection extends AndroidLintInspectionBase {
    public AndroidKLintInflateParamsInspection() {
      super(AndroidBundle.message("android.lint.inspections.inflate.params"), LayoutInflationDetector.ISSUE);
    }
  }

  public static class AndroidKLintJavascriptInterfaceInspection extends AndroidLintInspectionBase {
    public AndroidKLintJavascriptInterfaceInspection() {
      super(AndroidBundle.message("android.lint.inspections.javascript.interface"), JavaScriptInterfaceDetector.ISSUE);
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

  public static class AndroidKLintMenuTitleInspection extends AndroidLintInspectionBase {
    public AndroidKLintMenuTitleInspection() {
      super(AndroidBundle.message("android.lint.inspections.menu.title"), TitleDetector.ISSUE);
    }
  }

  public static class AndroidKLintOverrideAbstractInspection extends AndroidLintInspectionBase {
    public AndroidKLintOverrideAbstractInspection() {
      super(AndroidBundle.message("android.lint.inspections.override.abstract"), OverrideConcreteDetector.ISSUE);
    }
  }

  public static class AndroidKLintRecycleInspection extends AndroidLintInspectionBase {
    public AndroidKLintRecycleInspection() {
      super(AndroidBundle.message("android.lint.inspections.recycle"), CleanupDetector.RECYCLE_RESOURCE);
    }
  }

  public static class AndroidKLintRegisteredInspection extends AndroidLintInspectionBase {
    public AndroidKLintRegisteredInspection() {
      super(AndroidBundle.message("android.lint.inspections.registered"), RegistrationDetector.ISSUE);
    }
  }

  public static class AndroidKLintRequiredSizeInspection extends AndroidLintInspectionBase {
    public AndroidKLintRequiredSizeInspection() {
      super(AndroidBundle.message("android.lint.inspections.required.size"), RequiredAttributeDetector.ISSUE);
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

  public static class AndroidKLintSimpleDateFormatInspection extends AndroidLintInspectionBase {
    public AndroidKLintSimpleDateFormatInspection() {
      super(AndroidBundle.message("android.lint.inspections.simple.date.format"), DateFormatDetector.DATE_FORMAT);
    }
  }

  // Maybe not relevant
  public static class AndroidKLintStopShipInspection extends AndroidLintInspectionBase {
    public AndroidKLintStopShipInspection() {
      super(AndroidBundle.message("android.lint.inspections.stop.ship"), CommentDetector.STOP_SHIP);
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
}
