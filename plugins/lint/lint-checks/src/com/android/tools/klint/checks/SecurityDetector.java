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

import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_EXPORTED;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_PATH;
import static com.android.SdkConstants.ATTR_PATH_PATTERN;
import static com.android.SdkConstants.ATTR_PATH_PREFIX;
import static com.android.SdkConstants.ATTR_PERMISSION;
import static com.android.SdkConstants.ATTR_READ_PERMISSION;
import static com.android.SdkConstants.ATTR_WRITE_PERMISSION;
import static com.android.SdkConstants.TAG_ACTIVITY;
import static com.android.SdkConstants.TAG_APPLICATION;
import static com.android.SdkConstants.TAG_GRANT_PERMISSION;
import static com.android.SdkConstants.TAG_INTENT_FILTER;
import static com.android.SdkConstants.TAG_PATH_PERMISSION;
import static com.android.SdkConstants.TAG_PROVIDER;
import static com.android.SdkConstants.TAG_RECEIVER;
import static com.android.SdkConstants.TAG_SERVICE;
import static com.android.xml.AndroidManifest.NODE_ACTION;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.Expression;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Identifier;
import lombok.ast.MethodInvocation;
import lombok.ast.StrictListAccessor;

/**
 * Checks that exported services request a permission.
 */
public class SecurityDetector extends Detector implements Detector.XmlScanner,
        Detector.JavaScanner {

    private static final Implementation IMPLEMENTATION_MANIFEST = new Implementation(
            SecurityDetector.class,
            Scope.MANIFEST_SCOPE);

    private static final Implementation IMPLEMENTATION_JAVA = new Implementation(
            SecurityDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Exported services */
    public static final Issue EXPORTED_SERVICE = Issue.create(
            "ExportedService", //$NON-NLS-1$
            "Exported service does not require permission",
            "Exported services (services which either set `exported=true` or contain " +
            "an intent-filter and do not specify `exported=false`) should define a " +
            "permission that an entity must have in order to launch the service " +
            "or bind to it. Without this, any application can use this service.",
            Category.SECURITY,
            5,
            Severity.WARNING,
            IMPLEMENTATION_MANIFEST);

    /** Exported content providers */
    public static final Issue EXPORTED_PROVIDER = Issue.create(
            "ExportedContentProvider", //$NON-NLS-1$
            "Content provider does not require permission",
            "Content providers are exported by default and any application on the " +
            "system can potentially use them to read and write data. If the content " +
            "provider provides access to sensitive data, it should be protected by " +
            "specifying `export=false` in the manifest or by protecting it with a " +
            "permission that can be granted to other applications.",
            Category.SECURITY,
            5,
            Severity.WARNING,
            IMPLEMENTATION_MANIFEST);

    /** Exported receivers */
    public static final Issue EXPORTED_RECEIVER = Issue.create(
            "ExportedReceiver", //$NON-NLS-1$
            "Receiver does not require permission",
            "Exported receivers (receivers which either set `exported=true` or contain " +
            "an intent-filter and do not specify `exported=false`) should define a " +
            "permission that an entity must have in order to launch the receiver " +
            "or bind to it. Without this, any application can use this receiver.",
            Category.SECURITY,
            5,
            Severity.WARNING,
            IMPLEMENTATION_MANIFEST);

    /** Content provides which grant all URIs access */
    public static final Issue OPEN_PROVIDER = Issue.create(
            "GrantAllUris", //$NON-NLS-1$
            "Content provider shares everything",
            "The `<grant-uri-permission>` element allows specific paths to be shared. " +
            "This detector checks for a path URL of just '/' (everything), which is " +
            "probably not what you want; you should limit access to a subset.",
            Category.SECURITY,
            7,
            Severity.WARNING,
            IMPLEMENTATION_MANIFEST);

    /** Using the world-writable flag */
    public static final Issue WORLD_WRITEABLE = Issue.create(
            "WorldWriteableFiles", //$NON-NLS-1$
            "`openFileOutput()` call passing `MODE_WORLD_WRITEABLE`",
            "There are cases where it is appropriate for an application to write " +
            "world writeable files, but these should be reviewed carefully to " +
            "ensure that they contain no private data, and that if the file is " +
            "modified by a malicious application it does not trick or compromise " +
            "your application.",
            Category.SECURITY,
            4,
            Severity.WARNING,
            IMPLEMENTATION_JAVA);


    /** Using the world-readable flag */
    public static final Issue WORLD_READABLE = Issue.create(
            "WorldReadableFiles", //$NON-NLS-1$
            "`openFileOutput()` call passing `MODE_WORLD_READABLE`",
            "There are cases where it is appropriate for an application to write " +
            "world readable files, but these should be reviewed carefully to " +
            "ensure that they contain no private data that is leaked to other " +
            "applications.",
            Category.SECURITY,
            4,
            Severity.WARNING,
            IMPLEMENTATION_JAVA);

    /** Constructs a new {@link SecurityDetector} check */
    public SecurityDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return file.getName().equals(ANDROID_MANIFEST_XML);
    }

    // ---- Implements Detector.XmlScanner ----

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
            TAG_SERVICE,
            TAG_GRANT_PERMISSION,
            TAG_PROVIDER,
            TAG_ACTIVITY,
            TAG_RECEIVER
        );
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String tag = element.getTagName();
        if (tag.equals(TAG_SERVICE)) {
            checkService(context, element);
        } else if (tag.equals(TAG_GRANT_PERMISSION)) {
            checkGrantPermission(context, element);
        } else if (tag.equals(TAG_PROVIDER)) {
            checkProvider(context, element);
        } else if (tag.equals(TAG_RECEIVER)) {
            checkReceiver(context, element);
        }
    }

    public static boolean getExported(Element element) {
        // Used to check whether an activity, service or broadcast receiver is exported.
        String exportValue = element.getAttributeNS(ANDROID_URI, ATTR_EXPORTED);
        if (exportValue != null && !exportValue.isEmpty()) {
            return Boolean.valueOf(exportValue);
        } else {
            for (Element child : LintUtils.getChildren(element)) {
                if (child.getTagName().equals(TAG_INTENT_FILTER)) {
                    return true;
                }
            }
        }

      return false;
    }

    private static boolean isUnprotectedByPermission(Element element) {
        // Used to check whether an activity, service or broadcast receiver are
        // protected by a permission.
        String permission = element.getAttributeNS(ANDROID_URI, ATTR_PERMISSION);
        if (permission == null || permission.isEmpty()) {
            Node parent = element.getParentNode();
            if (parent.getNodeType() == Node.ELEMENT_NODE
                    && parent.getNodeName().equals(TAG_APPLICATION)) {
                Element application = (Element) parent;
                permission = application.getAttributeNS(ANDROID_URI, ATTR_PERMISSION);
                return permission == null || permission.isEmpty();
            }
        }

        return false;
    }

    private static boolean isWearableBindListener(@NonNull Element element) {
        // Checks whether a service has an Android Wear bind listener
        for (Element child : LintUtils.getChildren(element)) {
            if (child.getTagName().equals(TAG_INTENT_FILTER)) {
                for (Element innerChild : LintUtils.getChildren(child)) {
                    if (innerChild.getTagName().equals(NODE_ACTION)) {
                        String name = innerChild.getAttributeNS(ANDROID_URI, ATTR_NAME);
                        if ("com.google.android.gms.wearable.BIND_LISTENER".equals(name)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean isStandardReceiver(Element element) {
        // Play Services also the following receiver which we'll consider standard
        // in the sense that it doesn't require a separate permission
        String name = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
        if ("com.google.android.gms.tagmanager.InstallReferrerReceiver".equals(name)) {
            return true;
        }

        // Checks whether a broadcast receiver receives a standard Android action
        for (Element child : LintUtils.getChildren(element)) {
            if (child.getTagName().equals(TAG_INTENT_FILTER)) {
                for (Element innerChild : LintUtils.getChildren(child)) {
                    if (innerChild.getTagName().equals(NODE_ACTION)) {
                        String categoryString = innerChild.getAttributeNS(ANDROID_URI, ATTR_NAME);
                        return categoryString.startsWith("android."); //$NON-NLS-1$
                    }
                }
            }
        }

        return false;
    }

    private static void checkReceiver(XmlContext context, Element element) {
        if (getExported(element) && isUnprotectedByPermission(element) &&
                !isStandardReceiver(element)) {
            // No declared permission for this exported receiver: complain
            context.report(EXPORTED_RECEIVER, element, context.getLocation(element),
                           "Exported receiver does not require permission");
        }
    }

    private static void checkService(XmlContext context, Element element) {
        if (getExported(element) && isUnprotectedByPermission(element)
                && !isWearableBindListener(element)) {
            // No declared permission for this exported service: complain
            context.report(EXPORTED_SERVICE, element, context.getLocation(element),
                           "Exported service does not require permission");
        }
    }

    private static void checkGrantPermission(XmlContext context, Element element) {
        Attr path = element.getAttributeNodeNS(ANDROID_URI, ATTR_PATH);
        Attr prefix = element.getAttributeNodeNS(ANDROID_URI, ATTR_PATH_PREFIX);
        Attr pattern = element.getAttributeNodeNS(ANDROID_URI, ATTR_PATH_PATTERN);

        String msg = "Content provider shares everything; this is potentially dangerous.";
        if (path != null && path.getValue().equals("/")) { //$NON-NLS-1$
            context.report(OPEN_PROVIDER, path, context.getLocation(path), msg);
        }
        if (prefix != null && prefix.getValue().equals("/")) { //$NON-NLS-1$
            context.report(OPEN_PROVIDER, prefix, context.getLocation(prefix), msg);
        }
        if (pattern != null && (pattern.getValue().equals("/")  //$NON-NLS-1$
               /* || pattern.getValue().equals(".*")*/)) {
            context.report(OPEN_PROVIDER, pattern, context.getLocation(pattern), msg);
        }
    }

    private static void checkProvider(XmlContext context, Element element) {
        String exportValue = element.getAttributeNS(ANDROID_URI, ATTR_EXPORTED);
        // Content providers are exported by default
        boolean exported = true;
        if (exportValue != null && !exportValue.isEmpty()) {
            exported = Boolean.valueOf(exportValue);
        }

        if (exported) {
            // Just check for some use of permissions. Other Lint checks can check the saneness
            // of the permissions. We'll accept the permission, readPermission, or writePermission
            // attributes on the provider element, or a path-permission element.
            String permission = element.getAttributeNS(ANDROID_URI, ATTR_READ_PERMISSION);
            if (permission == null || permission.isEmpty()) {
                permission = element.getAttributeNS(ANDROID_URI, ATTR_WRITE_PERMISSION);
                if (permission == null || permission.isEmpty()) {
                    permission = element.getAttributeNS(ANDROID_URI, ATTR_PERMISSION);
                    if (permission == null || permission.isEmpty()) {
                        // No permission attributes? Check for path-permission.

                        // TODO: Add a Lint check to ensure the path-permission is good, similar to
                        // the grant-uri-permission check.
                        boolean hasPermission = false;
                        for (Element child : LintUtils.getChildren(element)) {
                            String tag = child.getTagName();
                            if (tag.equals(TAG_PATH_PERMISSION)) {
                                hasPermission = true;
                                break;
                            }
                        }

                        if (!hasPermission) {
                            context.report(EXPORTED_PROVIDER, element,
                                    context.getLocation(element),
                                    "Exported content providers can provide access to " +
                                            "potentially sensitive data");
                        }
                    }
                }
            }
        }
    }

    // ---- Implements Detector.JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        // These are the API calls that can accept a MODE_WORLD_READABLE/MODE_WORLD_WRITABLE
        // argument.
        List<String> values = new ArrayList<String>(2);
        values.add("openFileOutput"); //$NON-NLS-1$
        values.add("getSharedPreferences"); //$NON-NLS-1$
        return values;
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        StrictListAccessor<Expression,MethodInvocation> args = node.astArguments();
        for (Expression arg : args) {
            arg.accept(visitor);
        }
    }

    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        return new IdentifierVisitor(context);
    }

    private static class IdentifierVisitor extends ForwardingAstVisitor {
        private final JavaContext mContext;

        public IdentifierVisitor(JavaContext context) {
            super();
            mContext = context;
        }

        @Override
        public boolean visitIdentifier(Identifier node) {
            if ("MODE_WORLD_WRITEABLE".equals(node.astValue())) { //$NON-NLS-1$
                Location location = mContext.getLocation(node);
                mContext.report(WORLD_WRITEABLE, node, location,
                        "Using `MODE_WORLD_WRITEABLE` when creating files can be " +
                                "risky, review carefully");
            } else if ("MODE_WORLD_READABLE".equals(node.astValue())) { //$NON-NLS-1$
                Location location = mContext.getLocation(node);
                mContext.report(WORLD_READABLE, node, location,
                        "Using `MODE_WORLD_READABLE` when creating files can be " +
                                "risky, review carefully");
            }

            return false;
        }
    }
}
