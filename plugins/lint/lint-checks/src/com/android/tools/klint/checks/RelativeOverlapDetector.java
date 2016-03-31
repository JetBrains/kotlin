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
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_ABOVE;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_BASELINE;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_END;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_END;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_START;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_START;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_BELOW;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_END_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_LEFT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_RIGHT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_START_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_TEXT;
import static com.android.SdkConstants.ATTR_VISIBILITY;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.PREFIX_THEME_REF;
import static com.android.SdkConstants.RELATIVE_LAYOUT;
import static com.android.SdkConstants.VALUE_TRUE;
import static com.android.SdkConstants.VALUE_WRAP_CONTENT;
import static com.android.SdkConstants.VIEW;
import static com.android.SdkConstants.VIEW_INCLUDE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Check for potential item overlaps in a RelativeLayout when left- and
 * right-aligned text items are used.
 */
public class RelativeOverlapDetector extends LayoutDetector {
    public static final Issue ISSUE = Issue.create(
            "RelativeOverlap",
            "Overlapping items in RelativeLayout",
            "If relative layout has text or button items aligned to left and right " +
            "sides they can overlap each other due to localized text expansion " +
            "unless they have mutual constraints like `toEndOf`/`toStartOf`.",
            Category.I18N, 3, Severity.WARNING,
            new Implementation(RelativeOverlapDetector.class, Scope.RESOURCE_FILE_SCOPE));

    private static class LayoutNode {
        private enum Bucket {
            TOP, BOTTOM, SKIP
        }

        private int mIndex;
        private boolean mProcessed;
        private Element mNode;
        private Bucket mBucket;
        private LayoutNode mToLeft;
        private LayoutNode mToRight;
        private boolean mLastLeft;
        private boolean mLastRight;

        public LayoutNode(@NonNull Element node, int index) {
            mNode = node;
            mIndex = index;
            mProcessed = false;
            mLastLeft = true;
            mLastRight = true;
        }

        @NonNull
        public String getNodeId() {
            String nodeid = mNode.getAttributeNS(ANDROID_URI, ATTR_ID);
            if (nodeid.isEmpty()) {
                return String.format("%1$s-%2$d", mNode.getTagName(), mIndex);
            } else {
                return uniformId(nodeid);
            }
        }

        @NonNull
        public String getNodeTextId() {
            String text = mNode.getAttributeNS(ANDROID_URI, ATTR_TEXT);
            if (text.isEmpty()) {
                return getNodeId();
            } else {
                return uniformId(text);
            }
        }

        @NonNull
        @Override
        public String toString() {
            return getNodeTextId();
        }

        public boolean isInvisible() {
            String visibility = mNode.getAttributeNS(ANDROID_URI,
                    ATTR_VISIBILITY);
            return visibility.equals("gone") || visibility.equals("invisible");
        }

        /**
         * Determine if not can grow due to localization or not.
         */
        public boolean fixedWidth() {
            String width = mNode.getAttributeNS(ANDROID_URI, ATTR_LAYOUT_WIDTH);
            if (width.equals(VALUE_WRAP_CONTENT)) {
                // First check child nodes. If at least one of them is not
                // fixed-width,
                // treat whole layout as non-fixed-width
                NodeList childNodes = mNode.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node child = childNodes.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        LayoutNode childLayout = new LayoutNode((Element) child,
                                i);
                        if (!childLayout.fixedWidth()) {
                            return false;
                        }
                    }
                }
                // If node contains text attribute, consider it fixed-width if
                // text is hard-coded, otherwise it is not fixed-width.
                String text = mNode.getAttributeNS(ANDROID_URI, ATTR_TEXT);
                if (!text.isEmpty()) {
                    return !text.startsWith(PREFIX_RESOURCE_REF)
                        && !text.startsWith(PREFIX_THEME_REF);
                }

                String nodeName = mNode.getTagName();
                if (nodeName.contains("Image") || nodeName.contains("Progress")
                        || nodeName.contains("Radio")) {
                    return true;
                } else if (nodeName.contains("Button")
                        || nodeName.contains("Text")) {
                    return false;
                }
            }
            return true;
        }

        @NonNull
        public Element getNode() {
            return mNode;
        }

        /**
         * Process a node of a layout. Put it into one of three processing
         * units and determine its right and left neighbours.
         */
        public void processNode(@NonNull Map<String, LayoutNode> nodes) {
            if (mProcessed) {
                return;
            }
            mProcessed = true;

            if (isInvisible() ||
                hasAttr(ATTR_LAYOUT_ALIGN_RIGHT) ||
                hasAttr(ATTR_LAYOUT_ALIGN_END) ||
                hasAttr(ATTR_LAYOUT_ALIGN_LEFT) ||
                hasAttr(ATTR_LAYOUT_ALIGN_START)) {
                mBucket = Bucket.SKIP;
            } else if (hasTrueAttr(ATTR_LAYOUT_ALIGN_PARENT_TOP)) {
                mBucket = Bucket.TOP;
            } else if (hasTrueAttr(ATTR_LAYOUT_ALIGN_PARENT_BOTTOM)) {
                mBucket = Bucket.BOTTOM;
            } else {
                if (hasAttr(ATTR_LAYOUT_ABOVE) || hasAttr(ATTR_LAYOUT_BELOW)) {
                    mBucket = Bucket.SKIP;
                } else {
                    String[] checkAlignment = { ATTR_LAYOUT_ALIGN_TOP,
                            ATTR_LAYOUT_ALIGN_BOTTOM,
                            ATTR_LAYOUT_ALIGN_BASELINE };
                    for (String alignment : checkAlignment) {
                        String value = mNode.getAttributeNS(ANDROID_URI,
                                alignment);
                        if (!value.isEmpty()) {
                            LayoutNode otherNode = nodes.get(uniformId(value));
                            if (otherNode != null) {
                                otherNode.processNode(nodes);
                                mBucket = otherNode.mBucket;
                            }
                        }
                    }
                }
            }
            if (mBucket == null) {
                mBucket = Bucket.TOP;
            }

            // Check relative placement
            boolean positioned = false;
            mToLeft = findNodeByAttr(nodes, ATTR_LAYOUT_TO_START_OF);
            if (mToLeft == null) {
                mToLeft = findNodeByAttr(nodes, ATTR_LAYOUT_TO_LEFT_OF);
            }
            // Avoid circular dependency
            for (LayoutNode n = mToLeft; n != null; n = n.mToLeft) {
              if (n.equals(this)) {
                mToLeft = null;
                mBucket = Bucket.SKIP;
                break;
              }
            }
            if (mToLeft != null) {
                mToLeft.mLastLeft = false;
                mLastRight = false;
                positioned = true;
            }
            mToRight = findNodeByAttr(nodes, ATTR_LAYOUT_TO_END_OF);
            if (mToRight == null) {
                mToRight = findNodeByAttr(nodes, ATTR_LAYOUT_TO_RIGHT_OF);
            }
            // Avoid circular dependency
            for (LayoutNode n = mToRight; n != null; n = n.mToRight) {
              if (n.equals(this)) {
                mToRight = null;
                mBucket = Bucket.SKIP;
                break;
              }
            }
            if (mToRight != null) {
                mToRight.mLastRight = false;
                mLastLeft = false;
                positioned = true;
            }

            if (hasTrueAttr(ATTR_LAYOUT_ALIGN_PARENT_END)
                    || hasTrueAttr(ATTR_LAYOUT_ALIGN_PARENT_RIGHT)) {
                mLastRight = false;
                positioned = true;
            }
            if (hasTrueAttr(ATTR_LAYOUT_ALIGN_PARENT_START)
                    || hasTrueAttr(ATTR_LAYOUT_ALIGN_PARENT_LEFT)) {
                mLastLeft = false;
                positioned = true;
            }
            // Treat any node that does not have explicit relative placement
            // same as if it has layout_alignParentStart = true;
            if (!positioned) {
                mLastLeft = false;
            }
        }

        @NonNull
        public Set<LayoutNode> canGrowLeft() {
            Set<LayoutNode> nodes;
            if (mToRight != null) {
                nodes = mToRight.canGrowLeft();
            } else {
                nodes = new LinkedHashSet<LayoutNode>();
            }
            if (!fixedWidth()) {
                nodes.add(this);
            }
            return nodes;
        }

        @NonNull
        public Set<LayoutNode> canGrowRight() {
            Set<LayoutNode> nodes;
            if (mToLeft != null) {
                nodes = mToLeft.canGrowRight();
            } else {
                nodes = new LinkedHashSet<LayoutNode>();
            }
            if (!fixedWidth()) {
                nodes.add(this);
            }
            return nodes;
        }

        /**
         * Determines if not should be skipped from checking.
         */
        public boolean skip() {
            if (mBucket == Bucket.SKIP) {
                return true;
            }

            // Skip all includes and Views
            return mNode.getTagName().equals(VIEW_INCLUDE)
                    || mNode.getTagName().equals(VIEW);
        }

        public boolean sameBucket(@NonNull LayoutNode node) {
            return mBucket == node.mBucket;
        }

        @Nullable
        private LayoutNode findNodeByAttr(
                @NonNull Map<String, LayoutNode> nodes,
                @NonNull String attrName) {
            String value = mNode.getAttributeNS(ANDROID_URI, attrName);
            if (!value.isEmpty()) {
                return nodes.get(uniformId(value));
            } else {
                return null;
            }
        }

        private boolean hasAttr(@NonNull String key) {
            return mNode.hasAttributeNS(ANDROID_URI, key);
        }

        private boolean hasTrueAttr(@NonNull String key) {
            return mNode.getAttributeNS(ANDROID_URI, key).equals(VALUE_TRUE);
        }

        @NonNull
        private static String uniformId(@NonNull String value) {
            return value.replaceFirst("@\\+", "@");
        }
    }

    public RelativeOverlapDetector() {
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(RELATIVE_LAYOUT);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        // Traverse all child elements
        NodeList childNodes = element.getChildNodes();
        int count = childNodes.getLength();
        Map<String, LayoutNode> nodes = Maps.newHashMap();
        for (int i = 0; i < count; i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                LayoutNode ln = new LayoutNode((Element) node, i);
                nodes.put(ln.getNodeId(), ln);
            }
        }

        // Node map is populated, recalculate nodes sizes
        for (LayoutNode ln : nodes.values()) {
            ln.processNode(nodes);
        }
        for (LayoutNode right : nodes.values()) {
            if (!right.mLastLeft || right.skip()) {
                continue;
            }
            Set<LayoutNode> canGrowLeft = right.canGrowLeft();
            for (LayoutNode left : nodes.values()) {
                if (left == right || !left.mLastRight || left.skip()
                        || !left.sameBucket(right)) {
                    continue;
                }
                Set<LayoutNode> canGrowRight = left.canGrowRight();
                if (!canGrowLeft.isEmpty() || !canGrowRight.isEmpty()) {
                    canGrowRight.addAll(canGrowLeft);
                    LayoutNode nodeToBlame = right;
                    LayoutNode otherNode = left;
                    if (!canGrowRight.contains(right)
                            && canGrowRight.contains(left)) {
                        nodeToBlame = left;
                        otherNode = right;
                    }
                    context.report(ISSUE, nodeToBlame.getNode(),
                            context.getLocation(nodeToBlame.getNode()),
                            String.format(
                                    "`%1$s` can overlap `%2$s` if %3$s %4$s due to localized text expansion",
                                    nodeToBlame.getNodeId(), otherNode.getNodeId(),
                                    Joiner.on(", ").join(canGrowRight),
                                    canGrowRight.size() > 1 ? "grow" : "grows"));
                }
            }
        }
    }
}
