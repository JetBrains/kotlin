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

import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_END;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_START;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_TOP;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.DIMEN_PREFIX;
import static com.android.SdkConstants.PREFIX_ANDROID;
import static com.android.SdkConstants.TAG_DIMEN;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_STYLE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceFile;
import com.android.ide.common.res2.ResourceItem;
import com.android.ide.common.resources.ResourceUrl;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Checks for negative margins in the following scenarios:
 * <ul>
 *     <li>In direct layout attribute usages, e.g. {@code <Button android:layoutMargin="-5dp"}</li>
 *     <li>In theme styles, e.g. {@code <item name="android:layoutMargin">-5dp</item>}</li>
 *     <li>In dimension usages, e.g. {@code <Button android:layoutMargin="@dimen/foo"} along
 *      with {@code <dimen name="foo">-5dp</dimen>}</li>
 * </ul>
 */
public class NegativeMarginDetector extends LayoutDetector {
    private static final Implementation IMPLEMENTATION = new Implementation(
            NegativeMarginDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** Negative margins */
    public static final Issue ISSUE = Issue.create(
            "NegativeMargin", //$NON-NLS-1$
            "Negative Margins",

            "Margin values should be positive. Negative values are generally a sign that " +
            "you are making assumptions about views surrounding the current one, or may be "+
            "tempted to turn off child clipping to allow a view to escape its parent. " +
            "Turning off child clipping to do this not only leads to poor graphical " +
            "performance, it also results in wrong touch event handling since touch events " +
            "are based strictly on a chain of parent-rect hit tests. Finally, making " +
            "assumptions about the size of strings can lead to localization problems.",
            Category.USABILITY,
            4,
            Severity.WARNING,
            IMPLEMENTATION).setEnabledByDefault(false);

    private HashMap<String, Location.Handle> mDimenUsage;


    /** Constructs a new {@link NegativeMarginDetector} */
    public NegativeMarginDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        // Look in both layouts (at attribute values) and in value files (style and dimension
        // definitions)
        return folderType == ResourceFolderType.LAYOUT || folderType == ResourceFolderType.VALUES;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return Arrays.asList(
                ATTR_LAYOUT_MARGIN,
                ATTR_LAYOUT_MARGIN_LEFT,
                ATTR_LAYOUT_MARGIN_TOP,
                ATTR_LAYOUT_MARGIN_RIGHT,
                ATTR_LAYOUT_MARGIN_BOTTOM,
                ATTR_LAYOUT_MARGIN_START,
                ATTR_LAYOUT_MARGIN_END
        );
    }

    @Override
    @Nullable
    public Collection<String> getApplicableElements() {
        return Arrays.asList(TAG_DIMEN, TAG_STYLE);
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        String value = attribute.getValue();
        checkMarginValue(context, value, attribute, null);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (context.getResourceFolderType() != ResourceFolderType.VALUES) {
            return;
        }

        String tag = element.getTagName();
        if (TAG_DIMEN.equals(tag)) {
            NodeList itemNodes = element.getChildNodes();
            String name = element.getAttribute(ATTR_NAME);
            Location.Handle handle = mDimenUsage != null ? mDimenUsage.get(name) : null;
            if (handle != null) {
                for (int j = 0, nodeCount = itemNodes.getLength(); j < nodeCount; j++) {
                    Node item = itemNodes.item(j);
                    if (item.getNodeType() == Node.TEXT_NODE) {
                        String text = item.getNodeValue().trim();
                        checkMarginValue(context, text, null, handle);
                    }
                }
            }
        } else {
            assert TAG_STYLE.equals(tag) : tag;
            NodeList itemNodes = element.getChildNodes();
            for (int j = 0, nodeCount = itemNodes.getLength(); j < nodeCount; j++) {
                Node item = itemNodes.item(j);
                if (item.getNodeType() == Node.ELEMENT_NODE &&
                        TAG_ITEM.equals(item.getNodeName())) {
                    Element itemElement = (Element) item;
                    String name = itemElement.getAttribute(ATTR_NAME);
                    if (name.startsWith(PREFIX_ANDROID) &&
                            name.startsWith(ATTR_LAYOUT_MARGIN, PREFIX_ANDROID.length())) {
                        NodeList childNodes = item.getChildNodes();
                        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                            Node child = childNodes.item(i);
                            if (child.getNodeType() != Node.TEXT_NODE) {
                                return;
                            }

                            checkMarginValue(context, child.getNodeValue(), child, null);
                        }
                    }
                }

            }
        }
    }

    private static boolean isNegativeDimension(@NonNull String value) {
        return value.trim().startsWith("-");
    }

    private void checkMarginValue(
            @NonNull XmlContext context,
            @NonNull String value,
            @Nullable Node scope,
            @Nullable Location.Handle handle) {
        if (isNegativeDimension(value)) {
            String message = "Margin values should not be negative";
            if (scope != null) {
                context.report(ISSUE, scope, context.getLocation(scope), message);
            } else {
                assert handle != null;
                context.report(ISSUE, handle.resolve(), message);
            }
        } else if (value.startsWith(DIMEN_PREFIX) && scope != null) {
            ResourceUrl url = ResourceUrl.parse(value);
            if (url == null) {
                return;
            }
            if (context.getClient().supportsProjectResources()) {
                // Typically interactive IDE usage, where we are only analyzing a single file,
                // but we can use the IDE to resolve resource URLs
                LintClient client = context.getClient();
                Project project = context.getProject();
                AbstractResourceRepository resources = client.getProjectResources(project, true);
                if (resources != null) {
                    List<ResourceItem> items = resources.getResourceItem(url.type, url.name);
                    if (items != null) {
                        for (ResourceItem item : items) {
                            ResourceValue resourceValue = item.getResourceValue(false);
                            if (resourceValue != null) {
                                String dimenValue = resourceValue.getValue();
                                if (dimenValue != null && isNegativeDimension(dimenValue)) {
                                    ResourceFile sourceFile = item.getSource();
                                    assert sourceFile != null;
                                    String message = String.format(
                                            "Margin values should not be negative "
                                                    + "(`%1$s` is defined as `%2$s` in `%3$s`",
                                            value, dimenValue, sourceFile.getFile());
                                    context.report(ISSUE, scope,
                                            context.getLocation(scope),
                                            message);
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if (!context.getDriver().isSuppressed(context, ISSUE, scope)) {
                // Batch mode where we process layouts then values in order
                if (mDimenUsage == null) {
                    mDimenUsage = new HashMap<String, Location.Handle>();
                }
                mDimenUsage.put(url.name, context.createLocationHandle(scope));
            }
        }
    }
}
