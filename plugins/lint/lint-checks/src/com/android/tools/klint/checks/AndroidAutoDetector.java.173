/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.TAG_INTENT_FILTER;
import static com.android.SdkConstants.TAG_SERVICE;
import static com.android.tools.klint.client.api.JavaParser.TYPE_STRING;
import static com.android.xml.AndroidManifest.NODE_ACTION;
import static com.android.xml.AndroidManifest.NODE_APPLICATION;
import static com.android.xml.AndroidManifest.NODE_METADATA;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Detector.XmlScanner;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.LintUtils;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.ResourceXmlDetector;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.XmlContext;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiMethod;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Detector for Android Auto issues.
 * <p> Uses a {@code <meta-data>} tag with a {@code name="com.google.android.gms.car.application"}
 * as a trigger for validating Automotive specific issues.
 */
public class AndroidAutoDetector extends ResourceXmlDetector
        implements XmlScanner, Detector.UastScanner {

    @SuppressWarnings("unchecked")
    public static final Implementation IMPL = new Implementation(
            AndroidAutoDetector.class,
            EnumSet.of(Scope.RESOURCE_FILE, Scope.MANIFEST, Scope.JAVA_FILE),
            Scope.RESOURCE_FILE_SCOPE);

    /** Invalid attribute for uses tag.*/
    public static final Issue INVALID_USES_TAG_ISSUE = Issue.create(
            "InvalidUsesTagAttribute", //$NON-NLS-1$
            "Invalid `name` attribute for `uses` element.",
            "The <uses> element in `<automotiveApp>` should contain a " +
            "valid value for the `name` attribute.\n" +
            "Valid values are `media` or `notification`.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPL).addMoreInfo(
            "https://developer.android.com/training/auto/start/index.html#auto-metadata");

    /** Missing MediaBrowserService action */
    public static final Issue MISSING_MEDIA_BROWSER_SERVICE_ACTION_ISSUE = Issue.create(
            "MissingMediaBrowserServiceIntentFilter", //$NON-NLS-1$
            "Missing intent-filter with action `android.media.browse.MediaBrowserService`.",
            "An Automotive Media App requires an exported service that extends " +
            "`android.service.media.MediaBrowserService` with an " +
            "`intent-filter` for the action `android.media.browse.MediaBrowserService` " +
            "to be able to browse and play media.\n" +
            "To do this, add\n" +
            "`<intent-filter>`\n" +
            "    `<action android:name=\"android.media.browse.MediaBrowserService\" />`\n" +
            "`</intent-filter>`\n to the service that extends " +
            "`android.service.media.MediaBrowserService`",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPL).addMoreInfo(
            "https://developer.android.com/training/auto/audio/index.html#config_manifest");

    /** Missing intent-filter for Media Search. */
    public static final Issue MISSING_INTENT_FILTER_FOR_MEDIA_SEARCH = Issue.create(
            "MissingIntentFilterForMediaSearch", //$NON-NLS-1$
            "Missing intent-filter with action `android.media.action.MEDIA_PLAY_FROM_SEARCH`",
            "To support voice searches on Android Auto, you should also register an " +
            "`intent-filter` for the action `android.media.action.MEDIA_PLAY_FROM_SEARCH`" +
            ".\nTo do this, add\n" +
            "`<intent-filter>`\n" +
            "    `<action android:name=\"android.media.action.MEDIA_PLAY_FROM_SEARCH\" />`\n" +
            "`</intent-filter>`\n" +
            "to your `<activity>` or `<service>`.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPL).addMoreInfo(
            "https://developer.android.com/training/auto/audio/index.html#support_voice");

    /** Missing implementation of MediaSession.Callback#onPlayFromSearch*/
    public static final Issue MISSING_ON_PLAY_FROM_SEARCH = Issue.create(
            "MissingOnPlayFromSearch", //$NON-NLS-1$
            "Missing `onPlayFromSearch`.",
            "To support voice searches on Android Auto, in addition to adding an " +
            "`intent-filter` for the action `onPlayFromSearch`," +
            " you also need to override and implement " +
            "`onPlayFromSearch(String query, Bundle bundle)`",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPL).addMoreInfo(
            "https://developer.android.com/training/auto/audio/index.html#support_voice");

    private static final String CAR_APPLICATION_METADATA_NAME =
            "com.google.android.gms.car.application"; //$NON-NLS-1$
    private static final String VAL_NAME_MEDIA = "media"; //$NON-NLS-1$
    private static final String VAL_NAME_NOTIFICATION = "notification"; //$NON-NLS-1$
    private static final String TAG_AUTOMOTIVE_APP = "automotiveApp"; //$NON-NLS-1$
    private static final String ATTR_RESOURCE = "resource"; //$NON-NLS-1$
    private static final String TAG_USES = "uses"; //$NON-NLS-1$
    private static final String ACTION_MEDIA_BROWSER_SERVICE =
            "android.media.browse.MediaBrowserService"; //$NON-NLS-1$
    private static final String ACTION_MEDIA_PLAY_FROM_SEARCH =
            "android.media.action.MEDIA_PLAY_FROM_SEARCH"; //$NON-NLS-1$
    private static final String CLASS_MEDIA_SESSION_CALLBACK =
            "android.media.session.MediaSession.Callback"; //$NON-NLS-1$
    private static final String CLASS_V4MEDIA_SESSION_COMPAT_CALLBACK =
            "android.support.v4.media.session.MediaSessionCompat.Callback"; //$NON-NLS-1$
    private static final String METHOD_MEDIA_SESSION_PLAY_FROM_SEARCH =
            "onPlayFromSearch"; //$NON-NLS-1$
    private static final String BUNDLE_ARG = "android.os.Bundle"; //$NON-NLS-1$

    /**
     * Indicates whether we identified that the current app is an automotive app and
     * that we should validate all the automotive specific issues.
     */
    private boolean mDoAutomotiveAppCheck;

    /** Indicates that a {@link #ACTION_MEDIA_BROWSER_SERVICE} intent-filter action was found. */
    private boolean mMediaIntentFilterFound;

    /** Indicates that a {@link #ACTION_MEDIA_PLAY_FROM_SEARCH} intent-filter action was found. */
    private boolean mMediaSearchIntentFilterFound;

    /** The resource file name deduced by the meta-data resource value */
    private String mAutomotiveResourceFileName;

    /** Indicates whether this app is an automotive Media App. */
    private boolean mIsAutomotiveMediaApp;

    /** {@link Location.Handle} to the application element */
    private Location.Handle mMainApplicationHandle;

    /** Constructs a new {@link AndroidAutoDetector} check */
    public AndroidAutoDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        // We only need to check the meta data resource file in res/xml if any.
        return folderType == ResourceFolderType.XML;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                TAG_AUTOMOTIVE_APP, // Root element of a declared automotive descriptor.
                NODE_METADATA,      // meta-data from AndroidManifest.xml
                TAG_SERVICE,        // service from AndroidManifest.xml
                TAG_INTENT_FILTER,  // Any declared intent-filter from AndroidManifest.xml
                NODE_APPLICATION    // Used for storing the application element/location.
        );
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        mIsAutomotiveMediaApp = false;
        mAutomotiveResourceFileName = null;
        mMediaIntentFilterFound = false;
        mMediaSearchIntentFilterFound = false;
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String tagName = element.getTagName();
        if (NODE_METADATA.equals(tagName) && !mDoAutomotiveAppCheck) {
            checkAutoMetadataTag(element);
        } else if (TAG_AUTOMOTIVE_APP.equals(tagName)) {
            checkAutomotiveAppElement(context, element);
        } else if (NODE_APPLICATION.equals(tagName)) {
            // Disable reporting the error if the Issue was suppressed at
            // the application level.
            if (context.getMainProject() == context.getProject()
                    && !context.getProject().isLibrary()) {
                mMainApplicationHandle = context.createLocationHandle(element);
                mMainApplicationHandle.setClientData(element);
            }
        } else if (TAG_SERVICE.equals(tagName)) {
            checkServiceForBrowserServiceIntentFilter(element);
        } else if (TAG_INTENT_FILTER.equals(tagName)) {
            checkForMediaSearchIntentFilter(element);
        }
    }

    private void checkAutoMetadataTag(Element element) {
        String name = element.getAttributeNS(ANDROID_URI, ATTR_NAME);

        if (CAR_APPLICATION_METADATA_NAME.equals(name)) {
            String autoFileName = element.getAttributeNS(ANDROID_URI, ATTR_RESOURCE);

            if (autoFileName != null && autoFileName.startsWith("@xml/")) { //$NON-NLS-1$
                // Store the fact that we need to check all the auto issues.
                mDoAutomotiveAppCheck = true;
                mAutomotiveResourceFileName =
                        autoFileName.substring("@xml/".length()) + DOT_XML; //$NON-NLS-1$
            }
        }
    }

    private void checkAutomotiveAppElement(XmlContext context, Element element) {
        // Indicates whether the current file matches the resource that was registered
        // in AndroidManifest.xml.
        boolean isMetadataResource =
                mAutomotiveResourceFileName != null
                && mAutomotiveResourceFileName.equals(context.file.getName());

        for (Element child : LintUtils.getChildren(element)) {

            if (TAG_USES.equals(child.getTagName())) {
                String attrValue = child.getAttribute(ATTR_NAME);
                if (VAL_NAME_MEDIA.equals(attrValue)) {
                    mIsAutomotiveMediaApp |= isMetadataResource;
                } else if (!VAL_NAME_NOTIFICATION.equals(attrValue)
                        && context.isEnabled(INVALID_USES_TAG_ISSUE)) {
                    // Error invalid value for attribute.
                    Attr node = child.getAttributeNode(ATTR_NAME);
                    if (node == null) {
                        // no name specified
                        continue;
                    }
                    context.report(INVALID_USES_TAG_ISSUE, node,
                            context.getLocation(node),
                            "Expecting one of `" + VAL_NAME_MEDIA + "` or `" +
                            VAL_NAME_NOTIFICATION + "` for the name " +
                            "attribute in " + TAG_USES + " tag.");
                }
            }
        }
        // Report any errors that we have collected that can be shown to the user
        // once we determine that this is an Automotive Media App.
        if (mIsAutomotiveMediaApp
                && !context.getProject().isLibrary()
                && mMainApplicationHandle != null
                && mDoAutomotiveAppCheck) {

            Element node = (Element) mMainApplicationHandle.getClientData();

            if (!mMediaIntentFilterFound
                    && context.isEnabled(MISSING_MEDIA_BROWSER_SERVICE_ACTION_ISSUE)) {
                context.report(MISSING_MEDIA_BROWSER_SERVICE_ACTION_ISSUE, node,
                        mMainApplicationHandle.resolve(),
                        "Missing `intent-filter` for action " +
                                "`android.media.browse.MediaBrowserService` that is required for " +
                                "android auto support");
            }
            if (!mMediaSearchIntentFilterFound
                    && context.isEnabled(MISSING_INTENT_FILTER_FOR_MEDIA_SEARCH)) {
                context.report(MISSING_INTENT_FILTER_FOR_MEDIA_SEARCH, node,
                        mMainApplicationHandle.resolve(),
                        "Missing `intent-filter` for action " +
                                "`android.media.action.MEDIA_PLAY_FROM_SEARCH`.");
            }
        }
    }

    private void checkServiceForBrowserServiceIntentFilter(Element element) {
        if (TAG_SERVICE.equals(element.getTagName())
                && !mMediaIntentFilterFound) {

            for (Element child : LintUtils.getChildren(element)) {
                String tagName = child.getTagName();
                if (TAG_INTENT_FILTER.equals(tagName)) {
                    for (Element filterChild : LintUtils.getChildren(child)) {
                        if (NODE_ACTION.equals(filterChild.getTagName())) {
                            String actionValue = filterChild.getAttributeNS(ANDROID_URI, ATTR_NAME);
                            if (ACTION_MEDIA_BROWSER_SERVICE.equals(actionValue)) {
                                mMediaIntentFilterFound = true;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkForMediaSearchIntentFilter(Element element) {
        if (!mMediaSearchIntentFilterFound) {

            for (Element filterChild : LintUtils.getChildren(element)) {
                if (NODE_ACTION.equals(filterChild.getTagName())) {
                    String actionValue = filterChild.getAttributeNS(ANDROID_URI, ATTR_NAME);
                    if (ACTION_MEDIA_PLAY_FROM_SEARCH.equals(actionValue)) {
                        mMediaSearchIntentFilterFound = true;
                        break;
                    }
                }
            }
        }
    }

    // Implementation of the UastScanner

    @Override
    @Nullable
    public List<String> applicableSuperClasses() {
        // We currently enable scanning only for media apps.
        return mIsAutomotiveMediaApp ?
                Arrays.asList(CLASS_MEDIA_SESSION_CALLBACK,
                        CLASS_V4MEDIA_SESSION_COMPAT_CALLBACK)
                : null;
    }

    @Override
    public void checkClass(@NonNull JavaContext context, @NonNull UClass declaration) {
        // Only check classes that are not declared abstract.
        if (!context.getEvaluator().isAbstract(declaration)) {
            MediaSessionCallbackVisitor visitor = new MediaSessionCallbackVisitor(context);
            declaration.accept(visitor);
            if (!visitor.isPlayFromSearchMethodFound()
                    && context.isEnabled(MISSING_ON_PLAY_FROM_SEARCH)) {

                context.reportUast(MISSING_ON_PLAY_FROM_SEARCH, declaration,
                        context.getUastNameLocation(declaration),
                        "This class does not override `" +
                                METHOD_MEDIA_SESSION_PLAY_FROM_SEARCH + "` from `MediaSession.Callback`" +
                                " The method should be overridden and implemented to support " +
                                "Voice search on Android Auto.");
            }
        }
    }

    /**
     * A Visitor class to search for {@code MediaSession.Callback#onPlayFromSearch(..)}
     * method declaration.
     */
    private static class MediaSessionCallbackVisitor extends JavaRecursiveElementVisitor {

        private final JavaContext mContext;

        private boolean mOnPlayFromSearchFound;

        public MediaSessionCallbackVisitor(JavaContext context) {
            this.mContext = context;
        }

        public boolean isPlayFromSearchMethodFound() {
            return mOnPlayFromSearchFound;
        }

        @Override
        public void visitMethod(PsiMethod method) {
            super.visitMethod(method);
            if (METHOD_MEDIA_SESSION_PLAY_FROM_SEARCH.equals(method.getName())
                    && mContext.getEvaluator().parametersMatch(method, TYPE_STRING,
                    BUNDLE_ARG)) {
                mOnPlayFromSearchFound = true;
            }
        }
    }

    // Used by the IDE to show errors.
    @SuppressWarnings("unused")
    @NonNull
    public static String[] getAllowedAutomotiveAppTypes() {
        return new String[]{VAL_NAME_MEDIA, VAL_NAME_NOTIFICATION};
    }
}
