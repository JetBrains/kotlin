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
import com.android.annotations.Nullable;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceFile;
import com.android.ide.common.res2.ResourceItem;
import com.android.resources.ResourceUrl;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.tools.klint.client.api.LintClient;
import com.android.tools.klint.detector.api.*;
import com.android.utils.XmlUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.*;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import org.jetbrains.uast.*;
import org.jetbrains.uast.visitor.UastVisitor;
import org.w3c.dom.*;

import java.io.File;
import java.util.*;

import static com.android.SdkConstants.*;

/** Detector for finding inconsistent usage of views and casts
 * <p>
 * TODO: Check findFragmentById
 * <pre>
 * ((ItemListFragment) getSupportFragmentManager()
 *   .findFragmentById(R.id.item_list))
 *   .setActivateOnItemClick(true);
 * </pre>
 * Here we should check the {@code <fragment>} tag pointed to by the id, and
 * check its name or class attributes to make sure the cast is compatible with
 * the named fragment class!
 */
public class ViewTypeDetector extends ResourceXmlDetector implements Detector.UastScanner {
    /** Mismatched view types */
    @SuppressWarnings("unchecked")
    public static final Issue ISSUE = Issue.create(
            "WrongViewCast", //$NON-NLS-1$
            "Mismatched view type",
            "Keeps track of the view types associated with ids and if it finds a usage of " +
            "the id in the Java code it ensures that it is treated as the same type.",
            Category.CORRECTNESS,
            9,
            Severity.FATAL,
            new Implementation(
                    ViewTypeDetector.class,
                    EnumSet.of(Scope.ALL_RESOURCE_FILES, Scope.ALL_JAVA_FILES),
                    Scope.JAVA_FILE_SCOPE));

    /** Flag used to do no work if we're running in incremental mode in a .java file without
     * a client supporting project resources */
    private Boolean mIgnore = null;

    private final Map<String, Object> mIdToViewTag = new HashMap<String, Object>(50);

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.SLOW;
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.LAYOUT;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singletonList(ATTR_ID);
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        String view = attribute.getOwnerElement().getTagName();
        String value = attribute.getValue();
        String id = null;
        if (value.startsWith(ID_PREFIX)) {
            id = value.substring(ID_PREFIX.length());
        } else if (value.startsWith(NEW_ID_PREFIX)) {
            id = value.substring(NEW_ID_PREFIX.length());
        } // else: could be @android id

        if (id != null) {
            if (view.equals(VIEW_TAG)) {
                view = attribute.getOwnerElement().getAttribute(ATTR_CLASS);
            }

            Object existing = mIdToViewTag.get(id);
            if (existing == null) {
                mIdToViewTag.put(id, view);
            } else if (existing instanceof String) {
                String existingString = (String) existing;
                if (!existingString.equals(view)) {
                    // Convert to list
                    List<String> list = new ArrayList<String>(2);
                    list.add((String) existing);
                    list.add(view);
                    mIdToViewTag.put(id, list);
                }
            } else if (existing instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) existing;
                if (!list.contains(view)) {
                    list.add(view);
                }
            }
        }
    }

    // ---- Implements Detector.JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("findViewById"); //$NON-NLS-1$
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UCallExpression call, @NonNull UMethod method) {
        LintClient client = context.getClient();
        if (mIgnore == Boolean.TRUE) {
            return;
        } else if (mIgnore == null) {
            mIgnore = !context.getScope().contains(Scope.ALL_RESOURCE_FILES) &&
                    !client.supportsProjectResources();
            if (mIgnore) {
                return;
            }
        }
        assert method.getName().equals("findViewById");
        UElement node = LintUtils.skipParentheses(call);
        while (node != null && node.getUastParent() instanceof UParenthesizedExpression) {
            node = node.getUastParent();
        }
        if (node.getUastParent() instanceof UBinaryExpressionWithType) {
            UBinaryExpressionWithType cast = (UBinaryExpressionWithType) node.getUastParent();
            PsiType type = cast.getType();
            String castType = null;
            if (type instanceof PsiClassType) {
                castType = type.getCanonicalText();
            }
            if (castType == null) {
                return;
            }

            List<UExpression> args = call.getValueArguments();
            if (args.size() == 1) {
                UExpression first = args.get(0);
                ResourceUrl resourceUrl = ResourceEvaluator.getResource(context, first);
                if (resourceUrl != null && resourceUrl.type == ResourceType.ID &&
                        !resourceUrl.framework) {
                    String id = resourceUrl.name;

                    if (client.supportsProjectResources()) {
                        AbstractResourceRepository resources = client
                                .getProjectResources(context.getMainProject(), true);
                        if (resources == null) {
                            return;
                        }

                        List<ResourceItem> items = resources.getResourceItem(ResourceType.ID,
                                id);
                        if (items != null && !items.isEmpty()) {
                            Set<String> compatible = Sets.newHashSet();
                            for (ResourceItem item : items) {
                                Collection<String> tags = getViewTags(context, item);
                                if (tags != null) {
                                    compatible.addAll(tags);
                                }
                            }
                            if (!compatible.isEmpty()) {
                                ArrayList<String> layoutTypes = Lists.newArrayList(compatible);
                                checkCompatible(context, castType, null, layoutTypes, cast);
                            }
                        }
                    } else {
                        Object types = mIdToViewTag.get(id);
                        if (types instanceof String) {
                            String layoutType = (String) types;
                            checkCompatible(context, castType, layoutType, null, cast);
                        } else if (types instanceof List<?>) {
                            @SuppressWarnings("unchecked")
                            List<String> layoutTypes = (List<String>) types;
                            checkCompatible(context, castType, null, layoutTypes, cast);
                        }
                    }
                }
            }
        }
    }

    @Nullable
    protected Collection<String> getViewTags(
            @NonNull Context context,
            @NonNull ResourceItem item) {
        // Check view tag in this file. Can I do it cheaply? Try with
        // an XML pull parser. Or DOM if we have multiple resources looked
        // up?
        ResourceFile source = item.getSource();
        if (source != null) {
            File file = source.getFile();
            Multimap<String,String> map = getIdToTagsIn(context, file);
            if (map != null) {
                return map.get(item.getName());
            }
        }

        return null;
    }


    private Map<File, Multimap<String, String>> mFileIdMap;

    @Nullable
    private Multimap<String, String> getIdToTagsIn(@NonNull Context context, @NonNull File file) {
        if (!file.getPath().endsWith(DOT_XML)) {
            return null;
        }
        if (mFileIdMap == null) {
            mFileIdMap = Maps.newHashMap();
        }
        Multimap<String, String> map = mFileIdMap.get(file);
        if (map == null) {
            map = ArrayListMultimap.create();
            mFileIdMap.put(file, map);

            String xml = context.getClient().readFile(file);
            // TODO: Use pull parser instead for better performance!
            Document document = XmlUtils.parseDocumentSilently(xml, true);
            if (document != null && document.getDocumentElement() != null) {
                addViewTags(map, document.getDocumentElement());
            }
        }
        return map;
    }

    private static void addViewTags(Multimap<String, String> map, Element element) {
        String id = element.getAttributeNS(ANDROID_URI, ATTR_ID);
        if (id != null && !id.isEmpty()) {
            id = LintUtils.stripIdPrefix(id);
            if (!map.containsEntry(id, element.getTagName())) {
                map.put(id, element.getTagName());
            }
        }

        NodeList children = element.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                addViewTags(map, (Element) child);
            }
        }
    }

    /** Check if the view and cast type are compatible */
    private static void checkCompatible(JavaContext context, String castType, String layoutType,
            List<String> layoutTypes, UBinaryExpressionWithType node) {
        assert layoutType == null || layoutTypes == null; // Should only specify one or the other
        boolean compatible = true;
        if (layoutType != null) {
            if (!layoutType.equals(castType)
                    && !context.getSdkInfo().isSubViewOf(castType, layoutType)) {
                compatible = false;
            }
        } else {
            compatible = false;
            assert layoutTypes != null;
            for (String type : layoutTypes) {
                if (type.equals(castType)
                        || context.getSdkInfo().isSubViewOf(castType, type)) {
                    compatible = true;
                    break;
                }
            }
        }

        if (!compatible) {
            if (layoutType == null) {
                layoutType = Joiner.on("|").join(layoutTypes);
            }
            String message = String.format(
                    "Unexpected cast to `%1$s`: layout tag was `%2$s`",
                    castType.substring(castType.lastIndexOf('.') + 1), layoutType);
            context.report(ISSUE, node, context.getUastLocation(node), message);
        }
    }
}
