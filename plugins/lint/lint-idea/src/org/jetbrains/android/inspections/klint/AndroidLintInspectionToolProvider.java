package org.jetbrains.android.inspections.klint;

import com.android.SdkConstants;
import com.android.ide.common.repository.GradleCoordinate;
import com.android.ide.common.resources.ResourceUrl;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.VersionQualifier;
import com.android.resources.ResourceFolderType;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkVersionInfo;
import com.android.tools.idea.actions.OverrideResourceAction;
import com.android.tools.idea.templates.RepositoryUrlManager;
import com.android.tools.klint.checks.*;
import com.android.tools.klint.checks.AddJavascriptInterfaceDetector;
import com.android.tools.klint.checks.AlarmDetector;
import com.android.tools.klint.checks.AllowAllHostnameVerifierDetector;
import com.android.tools.klint.checks.AlwaysShowActionDetector;
import com.android.tools.klint.checks.AndroidAutoDetector;
import com.android.tools.klint.checks.AnnotationDetector;
import com.android.tools.klint.checks.ApiDetector;
import com.android.tools.klint.checks.AppCompatCallDetector;
import com.android.tools.klint.checks.AppIndexingApiDetector;
import com.android.tools.klint.checks.BadHostnameVerifierDetector;
import com.android.tools.klint.checks.CallSuperDetector;
import com.android.tools.klint.checks.CipherGetInstanceDetector;
import com.android.tools.klint.checks.CleanupDetector;
import com.android.tools.klint.checks.CommentDetector;
import com.android.tools.klint.checks.CustomViewDetector;
import com.android.tools.klint.checks.CutPasteDetector;
import com.android.tools.klint.checks.DateFormatDetector;
import com.android.tools.klint.checks.FragmentDetector;
import com.android.tools.klint.checks.GetSignaturesDetector;
import com.android.tools.klint.checks.HandlerDetector;
import com.android.tools.klint.checks.IconDetector;
import com.android.tools.klint.checks.JavaPerformanceDetector;
import com.android.tools.klint.checks.JavaScriptInterfaceDetector;
import com.android.tools.klint.checks.LayoutConsistencyDetector;
import com.android.tools.klint.checks.LayoutInflationDetector;
import com.android.tools.klint.checks.LocaleDetector;
import com.android.tools.klint.checks.LogDetector;
import com.android.tools.klint.checks.MathDetector;
import com.android.tools.klint.checks.MergeRootFrameLayoutDetector;
import com.android.tools.klint.checks.NonInternationalizedSmsDetector;
import com.android.tools.klint.checks.OverdrawDetector;
import com.android.tools.klint.checks.OverrideConcreteDetector;
import com.android.tools.klint.checks.ParcelDetector;
import com.android.tools.klint.checks.PreferenceActivityDetector;
import com.android.tools.klint.checks.PrivateResourceDetector;
import com.android.tools.klint.checks.ReadParcelableDetector;
import com.android.tools.klint.checks.RecyclerViewDetector;
import com.android.tools.klint.checks.RegistrationDetector;
import com.android.tools.klint.checks.RequiredAttributeDetector;
import com.android.tools.klint.checks.RtlDetector;
import com.android.tools.klint.checks.SQLiteDetector;
import com.android.tools.klint.checks.SdCardDetector;
import com.android.tools.klint.checks.SecureRandomDetector;
import com.android.tools.klint.checks.SecurityDetector;
import com.android.tools.klint.checks.ServiceCastDetector;
import com.android.tools.klint.checks.SetJavaScriptEnabledDetector;
import com.android.tools.klint.checks.SetTextDetector;
import com.android.tools.klint.checks.SslCertificateSocketFactoryDetector;
import com.android.tools.klint.checks.StringFormatDetector;
import com.android.tools.klint.checks.ToastDetector;
import com.android.tools.klint.checks.TrustAllX509TrustManagerDetector;
import com.android.tools.klint.checks.UnsafeBroadcastReceiverDetector;
import com.android.tools.klint.checks.UnsafeNativeCodeDetector;
import com.android.tools.klint.checks.ViewConstructorDetector;
import com.android.tools.klint.checks.ViewHolderDetector;
import com.android.tools.klint.checks.ViewTagDetector;
import com.android.tools.klint.checks.ViewTypeDetector;
import com.android.tools.klint.checks.WrongCallDetector;
import com.android.tools.klint.checks.WrongImportDetector;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.lint.checks.*;
import com.android.tools.lint.detector.api.TextFormat;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.sdk.AndroidSdkData;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.android.util.AndroidResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.android.SdkConstants.*;
import static com.android.tools.klint.checks.ApiDetector.REQUIRES_API_ANNOTATION;
import static com.android.tools.klint.checks.FragmentDetector.ISSUE;
import static com.android.tools.klint.detector.api.TextFormat.RAW;
import static com.android.xml.AndroidManifest.*;

/**
 * Registrations for all the various Lint rules as local IDE inspections, along with quickfixes for many of them
 */
public class AndroidLintInspectionToolProvider {
  public static class AndroidKLintCustomErrorInspection extends AndroidLintInspectionBase {
    public AndroidKLintCustomErrorInspection() {
      super("Error from Custom Lint Check", IntellijLintIssueRegistry.CUSTOM_ERROR);
    }
  }

  public static class AndroidKLintCustomWarningInspection extends AndroidLintInspectionBase {
    public AndroidKLintCustomWarningInspection() {
      super("Warning from Custom Lint Check", IntellijLintIssueRegistry.CUSTOM_WARNING);
    }

  }

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

  public static class AndroidKLintFloatMathInspection extends AndroidLintInspectionBase {
    public AndroidKLintFloatMathInspection() {
      super("Using FloatMath instead of Math", MathDetector.ISSUE);
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

  public static class AndroidKLintUnprotectedSMSBroadcastReceiverInspection extends AndroidLintInspectionBase {
    public AndroidKLintUnprotectedSMSBroadcastReceiverInspection() {
      super(AndroidBundle.message("android.lint.inspections.unprotected.smsbroadcast.receiver"), UnsafeBroadcastReceiverDetector.BROADCAST_SMS);
    }

  }

  public static class AndroidKLintUnusedAttributeInspection extends AndroidLintInspectionBase {
    public AndroidKLintUnusedAttributeInspection() {
      super(AndroidBundle.message("android.lint.inspections.unused.attribute"), ApiDetector.UNUSED);
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
  
  public static class AndroidKLintGoogleAppIndexingUrlErrorInspection extends AndroidLintInspectionBase {
    public AndroidKLintGoogleAppIndexingUrlErrorInspection() {
      super("URL not supported by app for Google App Indexing", AppIndexingApiDetector.ISSUE_URL_ERROR);
    }

  }

  public static class AndroidKLintGoogleAppIndexingWarningInspection extends AndroidLintInspectionBase {
    public AndroidKLintGoogleAppIndexingWarningInspection() {
      super("Missing support for Google App Indexing", AppIndexingApiDetector.ISSUE_APP_INDEXING);
    }

  }

  public static class AndroidKLintGoogleAppIndexingApiWarningInspection extends AndroidLintInspectionBase {
    public AndroidKLintGoogleAppIndexingApiWarningInspection() {
      super("Missing support for Google App Indexing Api", AppIndexingApiDetector.ISSUE_APP_INDEXING_API);
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
  
  public static class AndroidKLintBadHostnameVerifierInspection extends AndroidLintInspectionBase {
    public AndroidKLintBadHostnameVerifierInspection() {
      super("Insecure HostnameVerifier", BadHostnameVerifierDetector.ISSUE);
    }
  }

  public static class AndroidKLintBatteryLifeInspection extends AndroidLintInspectionBase {
    public AndroidKLintBatteryLifeInspection() {
      super("Battery Life Issues", BatteryDetector.ISSUE);
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

  public static class AndroidKLintParcelClassLoaderInspection extends AndroidLintInspectionBase {
    public AndroidKLintParcelClassLoaderInspection() {
      super("Default Parcel Class Loader", ReadParcelableDetector.ISSUE);
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

  public static class AndroidKLintDefaultLocaleInspection extends AndroidLintInspectionBase {
    public AndroidKLintDefaultLocaleInspection() {
      super("Implied default locale in case conversion", LocaleDetector.STRING_LOCALE);
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

  public static class AndroidKLintViewTagInspection extends AndroidLintInspectionBase {
    public AndroidKLintViewTagInspection() {
      super("Tagged object leaks", ViewTagDetector.ISSUE);
    }
  }

  public static class AndroidKLintMergeRootFrameInspection extends AndroidLintInspectionBase {
    public AndroidKLintMergeRootFrameInspection() {
      super(AndroidBundle.message("android.lint.inspections.merge.root.frame"), MergeRootFrameLayoutDetector.ISSUE);
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

  public static class AndroidKLintSSLCertificateSocketFactoryCreateSocketInspection extends AndroidLintInspectionBase {
    public AndroidKLintSSLCertificateSocketFactoryCreateSocketInspection() {
      super(AndroidBundle.message("android.lint.inspections.sslcertificate.socket.factory.create.socket"), SslCertificateSocketFactoryDetector.CREATE_SOCKET);
    }
  }

  public static class AndroidKLintSSLCertificateSocketFactoryGetInsecureInspection extends AndroidLintInspectionBase {
    public AndroidKLintSSLCertificateSocketFactoryGetInsecureInspection() {
      super(AndroidBundle.message("android.lint.inspections.sslcertificate.socket.factory.get.insecure"), SslCertificateSocketFactoryDetector.GET_INSECURE);
    }
  }

  public static class AndroidKLintSwitchIntDefInspection extends AndroidLintInspectionBase {
    public AndroidKLintSwitchIntDefInspection() {
      super("Missing @IntDef in Switch", AnnotationDetector.SWITCH_TYPE_DEF);
    }

  }

  public static class AndroidKLintTrustAllX509TrustManagerInspection extends AndroidLintInspectionBase {
    public AndroidKLintTrustAllX509TrustManagerInspection() {
      super("Insecure TLS/SSL trust manager", TrustAllX509TrustManagerDetector.ISSUE);
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

    @NotNull
    @Override
    public AndroidLintQuickFix[] getQuickFixes(@NotNull PsiElement startElement, @NotNull PsiElement endElement, @NotNull String message) {
      int api = ApiDetector.getRequiredVersion(TextFormat.RAW.toText(message));
      if (api == -1) {
        return AndroidLintQuickFix.EMPTY_ARRAY;
      }

      Project project = startElement.getProject();
      if (JavaPsiFacade.getInstance(project).findClass(REQUIRES_API_ANNOTATION, GlobalSearchScope.allScope(project)) != null) {
        return new AndroidLintQuickFix[] {
                new AddTargetApiQuickFix(api, true),
                new AddTargetApiQuickFix(api, false),
                new AddTargetVersionCheckQuickFix(api)
        };
      }

      return new AndroidLintQuickFix[] {
              new AddTargetApiQuickFix(api, false),
              new AddTargetVersionCheckQuickFix(api)
      };
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

  private static final Pattern QUOTED_PARAMETER = Pattern.compile("`.+:(.+)=\"(.*)\"`");

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

  public static class AndroidKLintAllowAllHostnameVerifierInspection extends AndroidLintInspectionBase {
    public AndroidKLintAllowAllHostnameVerifierInspection() {
      super("Insecure HostnameVerifier", AllowAllHostnameVerifierDetector.ISSUE);
    }
  }

  public static class AndroidKLintCommitPrefEditsInspection extends AndroidLintInspectionBase {
    public AndroidKLintCommitPrefEditsInspection() {
      super(AndroidBundle.message("android.lint.inspections.commit.pref.edits"), CleanupDetector.SHARED_PREF);
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
  
  public static class AndroidKLintAuthLeakInspection extends AndroidLintInspectionBase {
    public AndroidKLintAuthLeakInspection() {
      super("Code could contain an credential leak", StringAuthLeakDetector.AUTH_LEAK);
    }
  }

  public static class AndroidKLintInflateParamsInspection extends AndroidLintInspectionBase {
    public AndroidKLintInflateParamsInspection() {
      super(AndroidBundle.message("android.lint.inspections.inflate.params"), LayoutInflationDetector.ISSUE);
    }
  }
  
  public static class AndroidKLintInvalidUsesTagAttributeInspection extends AndroidLintInspectionBase {
    public AndroidKLintInvalidUsesTagAttributeInspection() {
      super(AndroidBundle.message("android.lint.inspections.invalid.uses.tag.attribute"), AndroidAutoDetector.INVALID_USES_TAG_ISSUE);
    }
  }

  public static class AndroidKLintJavascriptInterfaceInspection extends AndroidLintInspectionBase {
    public AndroidKLintJavascriptInterfaceInspection() {
      super(AndroidBundle.message("android.lint.inspections.javascript.interface"), JavaScriptInterfaceDetector.ISSUE);
    }
  }

  public static class AndroidKLintLocalSuppressInspection extends AndroidLintInspectionBase {
    public AndroidKLintLocalSuppressInspection() {
      super(AndroidBundle.message("android.lint.inspections.local.suppress"), AnnotationDetector.INSIDE_METHOD);
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

  public static class AndroidKLintMissingIntentFilterForMediaSearchInspection extends AndroidLintInspectionBase {
    public AndroidKLintMissingIntentFilterForMediaSearchInspection() {
      super(AndroidBundle.message("android.lint.inspections.missing.intent.filter.for.media.search"),
            AndroidAutoDetector.MISSING_INTENT_FILTER_FOR_MEDIA_SEARCH);
    }
  }

  public static class AndroidKLintMissingMediaBrowserServiceIntentFilterInspection extends AndroidLintInspectionBase {
    public AndroidKLintMissingMediaBrowserServiceIntentFilterInspection() {
      super(AndroidBundle.message("android.lint.inspections.missing.media.browser.service.intent.filter"),
            AndroidAutoDetector.MISSING_MEDIA_BROWSER_SERVICE_ACTION_ISSUE);
    }
  }

  public static class AndroidKLintMissingOnPlayFromSearchInspection extends AndroidLintInspectionBase {
    public AndroidKLintMissingOnPlayFromSearchInspection() {
      super(AndroidBundle.message("android.lint.inspections.missing.on.play.from.search"),
            AndroidAutoDetector.MISSING_ON_PLAY_FROM_SEARCH);
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

  public static class AndroidKLintRecyclerViewInspection extends AndroidLintInspectionBase {
    public AndroidKLintRecyclerViewInspection() {
      super("RecyclerView Problems", RecyclerViewDetector.FIXED_POSITION);
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
  public static class AndroidKLintSecureRandomInspection extends AndroidLintInspectionBase {
    public AndroidKLintSecureRandomInspection() {
      super("Using a fixed seed with SecureRandom", SecureRandomDetector.ISSUE);
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

  public static class AndroidKLintSetTextI18nInspection extends AndroidLintInspectionBase {
    public AndroidKLintSetTextI18nInspection() {
      super(AndroidBundle.message("android.lint.inspections.set.text.i18n"), SetTextDetector.SET_TEXT_I18N);
    }
  }

  public static class AndroidKLintSetWorldReadableInspection extends AndroidLintInspectionBase {
    public AndroidKLintSetWorldReadableInspection() {
      super(AndroidBundle.message("android.lint.inspections.set.world.readable"), SecurityDetector.SET_READABLE);
    }
  }

  public static class AndroidKLintSetWorldWritableInspection extends AndroidLintInspectionBase {
    public AndroidKLintSetWorldWritableInspection() {
      super(AndroidBundle.message("android.lint.inspections.set.world.writable"), SecurityDetector.SET_WRITABLE);
    }
  }

  public static class AndroidKLintShiftFlagsInspection extends AndroidLintInspectionBase {
    public AndroidKLintShiftFlagsInspection() {
      super(AndroidBundle.message("android.lint.inspections.shift.flags"), AnnotationDetector.FLAG_STYLE);
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

  public static class AndroidKLintSupportAnnotationUsageInspection extends AndroidLintInspectionBase {
    public AndroidKLintSupportAnnotationUsageInspection() {
      super("Incorrect support annotation usage", AnnotationDetector.ANNOTATION_USAGE);
    }
  }

  public static class AndroidKLintUniqueConstantsInspection extends AndroidLintInspectionBase {
    public AndroidKLintUniqueConstantsInspection() {
      super(AndroidBundle.message("android.lint.inspections.unique.constants"), AnnotationDetector.UNIQUE);
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

  public static class AndroidKLintPendingBindingsInspection extends AndroidLintInspectionBase {
    public AndroidKLintPendingBindingsInspection() {
      super("Missing Pending Bindings", RecyclerViewDetector.DATA_BINDER);
    }
  }

  public static class AndroidKLintUnsafeDynamicallyLoadedCodeInspection extends AndroidLintInspectionBase {
    public AndroidKLintUnsafeDynamicallyLoadedCodeInspection() {
      super("load used to dynamically load code", UnsafeNativeCodeDetector.LOAD);
    }
  }

  public static class AndroidKLintUnsafeNativeCodeLocationInspection extends AndroidLintInspectionBase {
    public AndroidKLintUnsafeNativeCodeLocationInspection() {
      super("Native code outside library directory", UnsafeNativeCodeDetector.UNSAFE_NATIVE_CODE_LOCATION);
    }
  }

  public static class AndroidKLintUnsafeProtectedBroadcastReceiverInspection extends AndroidLintInspectionBase {
    public AndroidKLintUnsafeProtectedBroadcastReceiverInspection() {
      super("Unsafe Protected BroadcastReceiver", UnsafeBroadcastReceiverDetector.ACTION_STRING);
    }
  }
}
