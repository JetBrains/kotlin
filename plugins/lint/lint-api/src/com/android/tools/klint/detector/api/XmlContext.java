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

package com.android.tools.lint.detector.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.XmlParser;
import com.google.common.annotations.Beta;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;

/**
 * A {@link Context} used when checking XML files.
 * <p/>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class XmlContext extends ResourceContext {
    static final String SUPPRESS_COMMENT_PREFIX = "<!--suppress "; //$NON-NLS-1$

    /** The XML parser */
    private final XmlParser mParser;
    /** The XML document */
    public Document document;

    /**
     * Construct a new {@link XmlContext}
     *
     * @param driver the driver running through the checks
     * @param project the project containing the file being checked
     * @param main the main project if this project is a library project, or
     *            null if this is not a library project. The main project is
     *            the root project of all library projects, not necessarily the
     *            directly including project.
     * @param file the file being checked
     * @param folderType the {@link ResourceFolderType} of this file, if any
     */
    public XmlContext(
            @NonNull LintDriver driver,
            @NonNull Project project,
            @Nullable Project main,
            @NonNull File file,
            @Nullable ResourceFolderType folderType,
            @NonNull XmlParser parser) {
        super(driver, project, main, file, folderType);
        mParser = parser;
    }

    /**
     * Returns the location for the given node, which may be an element or an attribute.
     *
     * @param node the node to look up the location for
     * @return the location for the node
     */
    @NonNull
    public Location getLocation(@NonNull Node node) {
        return mParser.getLocation(this, node);
    }

    /**
     * Returns the location for name-portion of the given element or attribute.
     *
     * @param node the node to look up the location for
     * @return the location for the node
     */
    @NonNull
    public Location getNameLocation(@NonNull Node node) {
        return mParser.getNameLocation(this, node);
    }

    /**
     * Returns the location for value-portion of the given attribute
     *
     * @param node the node to look up the location for
     * @return the location for the node
     */
    @NonNull
    public Location getValueLocation(@NonNull Attr node) {
        return mParser.getValueLocation(this, node);
    }

    /**
     * Creates a new location within an XML text node
     *
     * @param textNode the text node
     * @param begin the start offset within the text node (inclusive)
     * @param end the end offset within the text node (exclusive)
     * @return a new location
     */
    @NonNull
    public Location getLocation(@NonNull Node textNode, int begin, int end) {
        assert textNode.getNodeType() == Node.TEXT_NODE;
        return mParser.getLocation(this, textNode, begin, end);
    }

    @NonNull
    public XmlParser getParser() {
        return mParser;
    }

    /**
     * Reports an issue applicable to a given DOM node. The DOM node is used as the
     * scope to check for suppress lint annotations.
     *
     * @param issue the issue to report
     * @param scope the DOM node scope the error applies to. The lint infrastructure
     *    will check whether there are suppress directives on this node (or its enclosing
     *    nodes) and if so suppress the warning without involving the client.
     * @param location the location of the issue, or null if not known
     * @param message the message for this warning
     */
    public void report(
            @NonNull Issue issue,
            @Nullable Node scope,
            @Nullable Location location,
            @NonNull String message) {
        if (scope != null && mDriver.isSuppressed(this, issue, scope)) {
            return;
        }
        super.report(issue, location, message);
    }

    /**
     * Report an error.
     * Like {@link #report(Issue, org.w3c.dom.Node, Location, String)} but with
     * a now-unused data parameter at the end.
     *
     * @deprecated Use {@link #report(Issue, org.w3c.dom.Node, Location, String)} instead;
     *    this method is here for custom rule compatibility
     */
    @SuppressWarnings("UnusedDeclaration") // Potentially used by external existing custom rules
    @Deprecated
    public void report(
            @NonNull Issue issue,
            @Nullable Node scope,
            @Nullable Location location,
            @NonNull String message,
            @SuppressWarnings("UnusedParameters") @Nullable Object data) {
        report(issue, scope, location, message);
    }

    @Override
    public void report(
            @NonNull Issue issue,
            @Nullable Location location,
            @NonNull String message) {
        // Warn if clients use the non-scoped form? No, there are cases where an
        //  XML detector's error isn't applicable to one particular location (or it's
        //  not feasible to compute it cheaply)
        //mDriver.getClient().log(null, "Warning: Issue " + issue
        //        + " was reported without a scope node: Can't be suppressed.");

        // For now just check the document root itself
        if (document != null && mDriver.isSuppressed(this, issue, document)) {
            return;
        }

        super.report(issue, location, message);
    }

    @Override
    @Nullable
    protected String getSuppressCommentPrefix() {
        return SUPPRESS_COMMENT_PREFIX;
    }

    public boolean isSuppressedWithComment(@NonNull Node node, @NonNull Issue issue) {
        // Check whether there is a comment marker
        String contents = getContents();
        assert contents != null; // otherwise we wouldn't be here

        int start = mParser.getNodeStartOffset(this, node);
        if (start != -1) {
            return isSuppressedWithComment(start, issue);
        }

        return false;
    }

    @NonNull
    public Location.Handle createLocationHandle(@NonNull Node node) {
        return mParser.createLocationHandle(this, node);
    }
}
