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
import com.android.ide.common.blame.SourcePosition;
import com.android.ide.common.res2.ResourceFile;
import com.android.ide.common.res2.ResourceItem;
import com.google.common.annotations.Beta;

import java.io.File;

/**
 * Location information for a warning
 * <p/>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class Location {
    private static final String SUPER_KEYWORD = "super"; //$NON-NLS-1$

    private final File mFile;
    private final Position mStart;
    private final Position mEnd;
    private String mMessage;
    private Location mSecondary;
    private Object mClientData;

    /**
     * (Private constructor, use one of the factory methods
     * {@link Location#create(File)},
     * {@link Location#create(File, Position, Position)}, or
     * {@link Location#create(File, String, int, int)}.
     * <p>
     * Constructs a new location range for the given file, from start to end. If
     * the length of the range is not known, end may be null.
     *
     * @param file the associated file (but see the documentation for
     *            {@link #getFile()} for more information on what the file
     *            represents)
     * @param start the starting position, or null
     * @param end the ending position, or null
     */
    protected Location(@NonNull File file, @Nullable Position start, @Nullable Position end) {
        super();
        mFile = file;
        mStart = start;
        mEnd = end;
    }

    /**
     * Returns the file containing the warning. Note that the file *itself* may
     * not yet contain the error. When editing a file in the IDE for example,
     * the tool could generate warnings in the background even before the
     * document is saved. However, the file is used as a identifying token for
     * the document being edited, and the IDE integration can map this back to
     * error locations in the editor source code.
     *
     * @return the file handle for the location
     */
    @NonNull
    public File getFile() {
        return mFile;
    }

    /**
     * The start position of the range
     *
     * @return the start position of the range, or null
     */
    @Nullable
    public Position getStart() {
        return mStart;
    }

    /**
     * The end position of the range
     *
     * @return the start position of the range, may be null for an empty range
     */
    @Nullable
    public Position getEnd() {
        return mEnd;
    }

    /**
     * Returns a secondary location associated with this location (if
     * applicable), or null.
     *
     * @return a secondary location or null
     */
    @Nullable
    public Location getSecondary() {
        return mSecondary;
    }

    /**
     * Sets a secondary location for this location.
     *
     * @param secondary a secondary location associated with this location
     */
    public void setSecondary(@Nullable Location secondary) {
        mSecondary = secondary;
    }

    /**
     * Sets a custom message for this location. This is typically used for
     * secondary locations, to describe the significance of this alternate
     * location. For example, for a duplicate id warning, the primary location
     * might say "This is a duplicate id", pointing to the second occurrence of
     * id declaration, and then the secondary location could point to the
     * original declaration with the custom message "Originally defined here".
     *
     * @param message the message to apply to this location
     */
    public void setMessage(@NonNull String message) {
        mMessage = message;
    }

    /**
     * Returns the custom message for this location, if any. This is typically
     * used for secondary locations, to describe the significance of this
     * alternate location. For example, for a duplicate id warning, the primary
     * location might say "This is a duplicate id", pointing to the second
     * occurrence of id declaration, and then the secondary location could point
     * to the original declaration with the custom message
     * "Originally defined here".
     *
     * @return the custom message for this location, or null
     */
    @Nullable
    public String getMessage() {
        return mMessage;
    }

    /**
     * Sets the client data associated with this location. This is an optional
     * field which can be used by the creator of the {@link Location} to store
     * temporary state associated with the location.
     *
     * @param clientData the data to store with this location
     */
    public void setClientData(@Nullable Object clientData) {
        mClientData = clientData;
    }

    /**
     * Returns the client data associated with this location - an optional field
     * which can be used by the creator of the {@link Location} to store
     * temporary state associated with the location.
     *
     * @return the data associated with this location
     */
    @Nullable
    public Object getClientData() {
        return mClientData;
    }

    @Override
    public String toString() {
        return "Location [file=" + mFile + ", start=" + mStart + ", end=" + mEnd + ", message="
                + mMessage + ']';
    }

    /**
     * Creates a new location for the given file
     *
     * @param file the file to create a location for
     * @return a new location
     */
    @NonNull
    public static Location create(@NonNull File file) {
        return new Location(file, null /*start*/, null /*end*/);
    }

    /**
     * Creates a new location for the given file and SourcePosition.
     *
     * @param file the file containing the positions
     * @param position the source position
     * @return a new location
     */
    @NonNull
    public static Location create(
            @NonNull File file,
            @NonNull SourcePosition position) {
        if (position.equals(SourcePosition.UNKNOWN)) {
            return new Location(file, null /*start*/, null /*end*/);
        }
        return new Location(file,
                new DefaultPosition(
                        position.getStartLine(),
                        position.getStartColumn(),
                        position.getStartOffset()),
                new DefaultPosition(
                        position.getEndLine(),
                        position.getEndColumn(),
                        position.getEndOffset()));
    }

    /**
     * Creates a new location for the given file and starting and ending
     * positions.
     *
     * @param file the file containing the positions
     * @param start the starting position
     * @param end the ending position
     * @return a new location
     */
    @NonNull
    public static Location create(
            @NonNull File file,
            @NonNull Position start,
            @Nullable Position end) {
        return new Location(file, start, end);
    }

    /**
     * Creates a new location for the given file, with the given contents, for
     * the given offset range.
     *
     * @param file the file containing the location
     * @param contents the current contents of the file
     * @param startOffset the starting offset
     * @param endOffset the ending offset
     * @return a new location
     */
    @NonNull
    public static Location create(
            @NonNull File file,
            @Nullable String contents,
            int startOffset,
            int endOffset) {
        if (startOffset < 0 || endOffset < startOffset) {
            throw new IllegalArgumentException("Invalid offsets");
        }

        if (contents == null) {
            return new Location(file,
                    new DefaultPosition(-1, -1, startOffset),
                    new DefaultPosition(-1, -1, endOffset));
        }

        int size = contents.length();
        endOffset = Math.min(endOffset, size);
        startOffset = Math.min(startOffset, endOffset);
        Position start = null;
        int line = 0;
        int lineOffset = 0;
        char prev = 0;
        for (int offset = 0; offset <= size; offset++) {
            if (offset == startOffset) {
                start = new DefaultPosition(line, offset - lineOffset, offset);
            }
            if (offset == endOffset) {
                Position end = new DefaultPosition(line, offset - lineOffset, offset);
                return new Location(file, start, end);
            }
            char c = contents.charAt(offset);
            if (c == '\n') {
                lineOffset = offset + 1;
                if (prev != '\r') {
                    line++;
                }
            } else if (c == '\r') {
                line++;
                lineOffset = offset + 1;
            }
            prev = c;
        }
        return create(file);
    }

    /**
     * Creates a new location for the given file, with the given contents, for
     * the given line number.
     *
     * @param file the file containing the location
     * @param contents the current contents of the file
     * @param line the line number (0-based) for the position
     * @return a new location
     */
    @NonNull
    public static Location create(@NonNull File file, @NonNull String contents, int line) {
        return create(file, contents, line, null, null, null);
    }

    /**
     * Creates a new location for the given file, with the given contents, for
     * the given line number.
     *
     * @param file the file containing the location
     * @param contents the current contents of the file
     * @param line the line number (0-based) for the position
     * @param patternStart an optional pattern to search for from the line
     *            match; if found, adjust the column and offsets to begin at the
     *            pattern start
     * @param patternEnd an optional pattern to search for behind the start
     *            pattern; if found, adjust the end offset to match the end of
     *            the pattern
     * @param hints optional additional information regarding the pattern search
     * @return a new location
     */
    @NonNull
    public static Location create(@NonNull File file, @NonNull String contents, int line,
            @Nullable String patternStart, @Nullable String patternEnd,
            @Nullable SearchHints hints) {
        int currentLine = 0;
        int offset = 0;
        while (currentLine < line) {
            offset = contents.indexOf('\n', offset);
            if (offset == -1) {
                return create(file);
            }
            currentLine++;
            offset++;
        }

        if (line == currentLine) {
            if (patternStart != null) {
                SearchDirection direction = SearchDirection.NEAREST;
                if (hints != null) {
                    direction = hints.mDirection;
                }

                int index;
                if (direction == SearchDirection.BACKWARD) {
                    index = findPreviousMatch(contents, offset, patternStart, hints);
                    line = adjustLine(contents, line, offset, index);
                } else if (direction == SearchDirection.EOL_BACKWARD) {
                    int lineEnd = contents.indexOf('\n', offset);
                    if (lineEnd == -1) {
                        lineEnd = contents.length();
                    }

                    index = findPreviousMatch(contents, lineEnd, patternStart, hints);
                    line = adjustLine(contents, line, offset, index);
                } else if (direction == SearchDirection.FORWARD) {
                    index = findNextMatch(contents, offset, patternStart, hints);
                    line = adjustLine(contents, line, offset, index);
                } else {
                    assert direction == SearchDirection.NEAREST;

                    int before = findPreviousMatch(contents, offset, patternStart, hints);
                    int after = findNextMatch(contents, offset, patternStart, hints);

                    if (before == -1) {
                        index = after;
                        line = adjustLine(contents, line, offset, index);
                    } else if (after == -1) {
                        index = before;
                        line = adjustLine(contents, line, offset, index);
                    } else if (offset - before < after - offset) {
                        index = before;
                        line = adjustLine(contents, line, offset, index);
                    } else {
                        index = after;
                        line = adjustLine(contents, line, offset, index);
                    }
                }

                if (index != -1) {
                    int lineStart = contents.lastIndexOf('\n', index);
                    if (lineStart == -1) {
                        lineStart = 0;
                    } else {
                        lineStart++; // was pointing to the previous line's CR, not line start
                    }
                    int column = index - lineStart;
                    if (patternEnd != null) {
                        int end = contents.indexOf(patternEnd, offset + patternStart.length());
                        if (end != -1) {
                            return new Location(file, new DefaultPosition(line, column, index),
                                    new DefaultPosition(line, -1, end + patternEnd.length()));
                        }
                    } else if (hints != null && (hints.isJavaSymbol() || hints.isWholeWord())) {
                        if (hints.isConstructor() && contents.startsWith(SUPER_KEYWORD, index)) {
                            patternStart = SUPER_KEYWORD;
                        }
                        return new Location(file, new DefaultPosition(line, column, index),
                                new DefaultPosition(line, column + patternStart.length(),
                                        index + patternStart.length()));
                    }
                    return new Location(file, new DefaultPosition(line, column, index),
                            new DefaultPosition(line, column, index + patternStart.length()));
                }
            }

            Position position = new DefaultPosition(line, -1, offset);
            return new Location(file, position, position);
        }

        return create(file);
    }

    private static int findPreviousMatch(@NonNull String contents, int offset, String pattern,
            @Nullable SearchHints hints) {
        while (true) {
            int index = contents.lastIndexOf(pattern, offset);
            if (index == -1) {
                return -1;
            } else {
                if (isMatch(contents, index, pattern, hints)) {
                    return index;
                } else {
                    offset = index - pattern.length();
                }
            }
        }
    }

    private static int findNextMatch(@NonNull String contents, int offset, String pattern,
            @Nullable SearchHints hints) {
        int constructorIndex = -1;
        if (hints != null && hints.isConstructor()) {
            // Special condition: See if the call is referenced as "super" instead.
            assert hints.isWholeWord();
            int index = contents.indexOf(SUPER_KEYWORD, offset);
            if (index != -1 && isMatch(contents, index, SUPER_KEYWORD, hints)) {
                constructorIndex = index;
            }
        }

        while (true) {
            int index = contents.indexOf(pattern, offset);
            if (index == -1) {
                return constructorIndex;
            } else {
                if (isMatch(contents, index, pattern, hints)) {
                    if (constructorIndex != -1) {
                        return Math.min(constructorIndex, index);
                    }
                    return index;
                } else {
                    offset = index + pattern.length();
                }
            }
        }
    }

    private static boolean isMatch(@NonNull String contents, int offset, String pattern,
            @Nullable SearchHints hints) {
        if (!contents.startsWith(pattern, offset)) {
            return false;
        }

        if (hints != null) {
            char prevChar = offset > 0 ? contents.charAt(offset - 1) : 0;
            int lastIndex = offset + pattern.length() - 1;
            char nextChar = lastIndex < contents.length() - 1 ? contents.charAt(lastIndex + 1) : 0;

            if (hints.isWholeWord() && (Character.isLetter(prevChar)
                    || Character.isLetter(nextChar))) {
                return false;

            }

            if (hints.isJavaSymbol()) {
                if (Character.isJavaIdentifierPart(prevChar)
                        || Character.isJavaIdentifierPart(nextChar)) {
                    return false;
                }

                if (prevChar == '"') {
                    return false;
                }

                // TODO: Additional validation to see if we're in a comment, string, etc.
                // This will require lexing from the beginning of the buffer.
            }

            if (hints.isConstructor() && SUPER_KEYWORD.equals(pattern)) {
                // Only looking for super(), not super.x, so assert that the next
                // non-space character is (
                int index = lastIndex + 1;
                while (index < contents.length() - 1) {
                    char c = contents.charAt(index);
                    if (c == '(') {
                        break;
                    } else if (!Character.isWhitespace(c)) {
                        return false;
                    }
                    index++;
                }
            }
        }

        return true;
    }

    private static int adjustLine(String doc, int line, int offset, int newOffset) {
        if (newOffset == -1) {
            return line;
        }

        if (newOffset < offset) {
            return line - countLines(doc, newOffset, offset);
        } else {
            return line + countLines(doc, offset, newOffset);
        }
    }

    private static int countLines(String doc, int start, int end) {
        int lines = 0;
        for (int offset = start; offset < end; offset++) {
            char c = doc.charAt(offset);
            if (c == '\n') {
                lines++;
            }
        }

        return lines;
    }

    /**
     * Reverses the secondary location list initiated by the given location
     *
     * @param location the first location in the list
     * @return the first location in the reversed list
     */
    public static Location reverse(@NonNull Location location) {
        Location next = location.getSecondary();
        location.setSecondary(null);
        while (next != null) {
            Location nextNext = next.getSecondary();
            next.setSecondary(location);
            location = next;
            next = nextNext;
        }

        return location;
    }

    /**
     * A {@link Handle} is a reference to a location. The point of a location
     * handle is to be able to create them cheaply, and then resolve them into
     * actual locations later (if needed). This makes it possible to for example
     * delay looking up line numbers, for locations that are offset based.
     */
    public interface Handle {
        /**
         * Compute a full location for the given handle
         *
         * @return create a location for this handle
         */
        @NonNull
        Location resolve();

        /**
         * Sets the client data associated with this location. This is an optional
         * field which can be used by the creator of the {@link Location} to store
         * temporary state associated with the location.
         *
         * @param clientData the data to store with this location
         */
        void setClientData(@Nullable Object clientData);

        /**
         * Returns the client data associated with this location - an optional field
         * which can be used by the creator of the {@link Location} to store
         * temporary state associated with the location.
         *
         * @return the data associated with this location
         */
        @Nullable
        Object getClientData();
    }

    /** A default {@link Handle} implementation for simple file offsets */
    public static class DefaultLocationHandle implements Handle {
        private final File mFile;
        private final String mContents;
        private final int mStartOffset;
        private final int mEndOffset;
        private Object mClientData;

        /**
         * Constructs a new {@link DefaultLocationHandle}
         *
         * @param context the context pointing to the file and its contents
         * @param startOffset the start offset within the file
         * @param endOffset the end offset within the file
         */
        public DefaultLocationHandle(@NonNull Context context, int startOffset, int endOffset) {
            mFile = context.file;
            mContents = context.getContents();
            mStartOffset = startOffset;
            mEndOffset = endOffset;
        }

        @Override
        @NonNull
        public Location resolve() {
            return create(mFile, mContents, mStartOffset, mEndOffset);
        }

        @Override
        public void setClientData(@Nullable Object clientData) {
            mClientData = clientData;
        }

        @Override
        @Nullable
        public Object getClientData() {
            return mClientData;
        }
    }

    public static class ResourceItemHandle implements Handle {
        private final ResourceItem mItem;

        public ResourceItemHandle(@NonNull ResourceItem item) {
            mItem = item;
        }
        @NonNull
        @Override
        public Location resolve() {
            // TODO: Look up the exact item location more
            // closely
            ResourceFile source = mItem.getSource();
            assert source != null : mItem;
            return create(source.getFile());
        }

        @Override
        public void setClientData(@Nullable Object clientData) {
        }

        @Nullable
        @Override
        public Object getClientData() {
            return null;
        }
    }

    /**
     * Whether to look forwards, or backwards, or in both directions, when
     * searching for a pattern in the source code to determine the right
     * position range for a given symbol.
     * <p>
     * When dealing with bytecode for example, there are only line number entries
     * within method bodies, so when searching for the method declaration, we should only
     * search backwards from the first line entry in the method.
     */
    public enum SearchDirection {
        /** Only search forwards */
        FORWARD,

        /** Only search backwards */
        BACKWARD,

        /** Search backwards from the current end of line (normally it's the beginning of
         * the current line) */
        EOL_BACKWARD,

        /**
         * Search both forwards and backwards from the given line, and prefer
         * the match that is closest
         */
        NEAREST,
    }

    /**
     * Extra information pertaining to finding a symbol in a source buffer,
     * used by {@link Location#create(File, String, int, String, String, SearchHints)}
     */
    public static class SearchHints {
        /**
         * the direction to search for the nearest match in (provided
         * {@code patternStart} is non null)
         */
        @NonNull
        private final SearchDirection mDirection;

        /** Whether the matched pattern should be a whole word */
        private boolean mWholeWord;

        /**
         * Whether the matched pattern should be a Java symbol (so for example,
         * a match inside a comment or string literal should not be used)
         */
        private boolean mJavaSymbol;

        /**
         * Whether the matched pattern corresponds to a constructor; if so, look for
         * some other possible source aliases too, such as "super".
         */
        private boolean mConstructor;

        private SearchHints(@NonNull SearchDirection direction) {
            super();
            mDirection = direction;
        }

        /**
         * Constructs a new {@link SearchHints} object
         *
         * @param direction the direction to search in for the pattern
         * @return a new @link SearchHints} object
         */
        @NonNull
        public static SearchHints create(@NonNull SearchDirection direction) {
            return new SearchHints(direction);
        }

        /**
         * Indicates that pattern matches should apply to whole words only

         * @return this, for constructor chaining
         */
        @NonNull
        public SearchHints matchWholeWord() {
            mWholeWord = true;

            return this;
        }

        /** @return true if the pattern match should be for whole words only */
        public boolean isWholeWord() {
            return mWholeWord;
        }

        /**
         * Indicates that pattern matches should apply to Java symbols only
         *
         * @return this, for constructor chaining
         */
        @NonNull
        public SearchHints matchJavaSymbol() {
            mJavaSymbol = true;
            mWholeWord = true;

            return this;
        }

        /** @return true if the pattern match should be for Java symbols only */
        public boolean isJavaSymbol() {
            return mJavaSymbol;
        }

        /**
         * Indicates that pattern matches should apply to constructors. If so, look for
         * some other possible source aliases too, such as "super".
         *
         * @return this, for constructor chaining
         */
        @NonNull
        public SearchHints matchConstructor() {
            mConstructor = true;
            mWholeWord = true;
            mJavaSymbol = true;

            return this;
        }

        /** @return true if the pattern match should be for a constructor */
        public boolean isConstructor() {
            return mConstructor;
        }
    }
}
