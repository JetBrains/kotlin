/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.tools.lint.checks.ViewHolderDetector.INFLATE;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceFile;
import com.android.ide.common.res2.ResourceItem;
import com.android.resources.ResourceType;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.Pair;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.kxml2.io.KXmlParser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import lombok.ast.AstVisitor;
import lombok.ast.Expression;
import lombok.ast.MethodInvocation;
import lombok.ast.NullLiteral;
import lombok.ast.Select;
import lombok.ast.StrictListAccessor;

/**
 * Looks for layout inflation calls passing null as the view root
 */
public class LayoutInflationDetector extends LayoutDetector implements Detector.JavaScanner {

    @SuppressWarnings("unchecked")
    private static final Implementation IMPLEMENTATION = new Implementation(
            LayoutInflationDetector.class,
            Scope.JAVA_AND_RESOURCE_FILES,
            Scope.JAVA_FILE_SCOPE);

    /** Passing in a null parent to a layout inflater */
    public static final Issue ISSUE = Issue.create(
            "InflateParams", //$NON-NLS-1$
            "Layout Inflation without a Parent",

            "When inflating a layout, avoid passing in null as the parent view, since " +
            "otherwise any layout parameters on the root of the inflated layout will be ignored.",

            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            IMPLEMENTATION)
            .addMoreInfo("http://www.doubleencore.com/2013/05/layout-inflation-as-intended");

    private static final String ERROR_MESSAGE =
            "Avoid passing `null` as the view root (needed to resolve "
            + "layout parameters on the inflated layout's root element)";

    /** Constructs a new {@link LayoutInflationDetector} check */
    public LayoutInflationDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mPendingErrors != null) {
            for (Pair<String,Location> pair : mPendingErrors) {
                String inflatedLayout = pair.getFirst();
                if (mLayoutsWithRootLayoutParams == null ||
                        !mLayoutsWithRootLayoutParams.contains(inflatedLayout)) {
                    // No root layout parameters on the inflated layout: no need to complain
                    continue;
                }
                Location location = pair.getSecond();
                context.report(ISSUE, location, ERROR_MESSAGE);
            }
        }
    }

    // ---- Implements XmlScanner ----

    private Set<String> mLayoutsWithRootLayoutParams;
    private List<Pair<String,Location>> mPendingErrors;

    @Override
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        Element root = document.getDocumentElement();
        if (root != null) {
            NamedNodeMap attributes = root.getAttributes();
            for (int i = 0, n = attributes.getLength(); i < n; i++) {
                Attr attribute = (Attr) attributes.item(i);
                if (attribute.getLocalName() != null
                        && attribute.getLocalName().startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
                    if (mLayoutsWithRootLayoutParams == null) {
                        mLayoutsWithRootLayoutParams = Sets.newHashSetWithExpectedSize(20);
                    }
                    mLayoutsWithRootLayoutParams.add(LintUtils.getBaseName(context.file.getName()));
                    break;
                }
            }
        }
    }

    // ---- Implements JavaScanner ----

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(INFLATE);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        assert node.astName().astValue().equals(INFLATE);
        if (node.astOperand() == null) {
            return;
        }
        StrictListAccessor<Expression, MethodInvocation> arguments = node.astArguments();
        if (arguments.size() < 2) {
            return;
        }
        Iterator<Expression> iterator = arguments.iterator();
        Expression first = iterator.next();
        Expression second = iterator.next();
        if (!(second instanceof NullLiteral) || !(first instanceof Select)) {
            return;
        }
        Select select = (Select) first;
        Expression operand = select.astOperand();
        if (operand instanceof Select) {
            Select rLayout = (Select) operand;
            if (rLayout.astIdentifier().astValue().equals(ResourceType.LAYOUT.getName()) &&
                    rLayout.astOperand().toString().endsWith(SdkConstants.R_CLASS)) {
                String layoutName = select.astIdentifier().astValue();
                if (context.getScope().contains(Scope.RESOURCE_FILE)) {
                    // We're doing a full analysis run: we can gather this information
                    // incrementally
                    if (!context.getDriver().isSuppressed(context, ISSUE, node)) {
                        if (mPendingErrors == null) {
                            mPendingErrors = Lists.newArrayList();
                        }
                        Location location = context.getLocation(second);
                        mPendingErrors.add(Pair.of(layoutName, location));
                    }
                } else if (hasLayoutParams(context, layoutName)) {
                    context.report(ISSUE, node, context.getLocation(second), ERROR_MESSAGE);
                }
            }
        }

        super.visitMethod(context, visitor, node);
    }

    private static boolean hasLayoutParams(@NonNull JavaContext context, String name) {
        LintClient client = context.getClient();
        if (!client.supportsProjectResources()) {
            return true; // not certain
        }

        Project project = context.getProject();
        AbstractResourceRepository resources = client.getProjectResources(project, true);
        if (resources == null) {
            return true; // not certain
        }

        List<ResourceItem> items = resources.getResourceItem(ResourceType.LAYOUT, name);
        if (items == null || items.isEmpty()) {
            return false;
        }

        for (ResourceItem item : items) {
            ResourceFile source = item.getSource();
            if (source == null) {
                return true; // not certain
            }
            File file = source.getFile();
            if (file.exists()) {
                try {
                    String s = context.getClient().readFile(file);
                    if (hasLayoutParams(new StringReader(s))) {
                        return true;
                    }
                } catch (Exception e) {
                    context.log(e, "Could not read/parse inflated layout");
                    return true; // not certain
                }
            }
        }

        return false;
    }

    @VisibleForTesting
    static boolean hasLayoutParams(@NonNull Reader reader)
            throws XmlPullParserException, IOException {
        KXmlParser parser = new KXmlParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(reader);

        while (true) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    if (parser.getAttributeName(i).startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
                        String prefix = parser.getAttributePrefix(i);
                        if (prefix != null && !prefix.isEmpty() &&
                                ANDROID_URI.equals(parser.getNamespace(prefix))) {
                            return true;
                        }
                    }
                }

                return false;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                return false;
            }
        }
    }
}
