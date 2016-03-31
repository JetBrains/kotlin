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

package com.android.tools.lint.client.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.annotations.Beta;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A wrapper for an XML parser. This allows tools integrating lint to map directly
 * to builtin services, such as already-parsed data structures in XML editors.
 * <p/>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public abstract class XmlParser {
    /**
     * Parse the file pointed to by the given context and return as a Document
     *
     * @param context the context pointing to the file to be parsed, typically
     *            via {@link Context#getContents()} but the file handle (
     *            {@link Context#file} can also be used to map to an existing
     *            editor buffer in the surrounding tool, etc)
     * @return the parsed DOM document, or null if parsing fails
     */
    @Nullable
    public abstract Document parseXml(@NonNull XmlContext context);

    /**
     * Returns a {@link Location} for the given DOM node
     *
     * @param context information about the file being parsed
     * @param node the node to create a location for
     * @return a location for the given node
     */
    @NonNull
    public abstract Location getLocation(@NonNull XmlContext context, @NonNull Node node);

    /**
     * Returns a {@link Location} for the given DOM node. Like
     * {@link #getLocation(XmlContext, Node)}, but allows a position range that
     * is a subset of the node range.
     *
     * @param context information about the file being parsed
     * @param node the node to create a location for
     * @param start the starting position within the node, inclusive
     * @param end the ending position within the node, exclusive
     * @return a location for the given node
     */
    @NonNull
    public abstract Location getLocation(@NonNull XmlContext context, @NonNull Node node,
            int start, int end);

    /**
     * Returns a {@link Location} for the given DOM node
     *
     * @param context information about the file being parsed
     * @param node the node to create a location for
     * @return a location for the given node
     */
    @NonNull
    public abstract Location getNameLocation(@NonNull XmlContext context, @NonNull Node node);

    /**
     * Returns a {@link Location} for the given DOM node
     *
     * @param context information about the file being parsed
     * @param node the node to create a location for
     * @return a location for the given node
     */
    @NonNull
    public abstract Location getValueLocation(@NonNull XmlContext context, @NonNull Attr node);

    /**
     * Creates a light-weight handle to a location for the given node. It can be
     * turned into a full fledged location by
     * {@link com.android.tools.lint.detector.api.Location.Handle#resolve()}.
     *
     * @param context the context providing the node
     * @param node the node (element or attribute) to create a location handle
     *            for
     * @return a location handle
     */
    @NonNull
    public abstract Location.Handle createLocationHandle(@NonNull XmlContext context,
            @NonNull Node node);

    /**
     * Dispose any data structures held for the given context.
     * @param context information about the file previously parsed
     * @param document the document that was parsed and is now being disposed
     */
    public void dispose(@NonNull XmlContext context, @NonNull Document document) {
    }

    /**
     * Returns the start offset of the given node, or -1 if not known
     *
     * @param context the context providing the node
     * @param node the node (element or attribute) to create a location handle
     *            for
     * @return the start offset, or -1 if not known
     */
    public abstract int getNodeStartOffset(@NonNull XmlContext context, @NonNull Node node);

    /**
     * Returns the end offset of the given node, or -1 if not known
     *
     * @param context the context providing the node
     * @param node the node (element or attribute) to create a location handle
     *            for
     * @return the end offset, or -1 if not known
     */
    public abstract int getNodeEndOffset(@NonNull XmlContext context, @NonNull Node node);
}
