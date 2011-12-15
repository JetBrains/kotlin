/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.impl.event.DocumentEventImpl;
import com.intellij.util.LocalTimeCounter;
import com.intellij.util.text.CharArrayCharSequence;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.text.CharSequenceBackedByArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author cdr
 */
abstract class CharArray implements CharSequenceBackedByArray {

    @SuppressWarnings("UseOfArchaicSystemPropertyAccessors")
    private static final boolean DISABLE_DEFERRED_PROCESSING = false;
    @SuppressWarnings("UseOfArchaicSystemPropertyAccessors")
    private static final boolean DEBUG_DEFERRED_PROCESSING = false;

    private static final Logger LOG = Logger.getInstance("#" + CharArray.class.getName());

    /**
     * We can't exclude possibility of situation when <code>'defer changes'</code> state is {@link #setDeferredChangeMode(boolean) entered}
     * but not exited, hence, we want to perform automatic flushing if necessary in order to avoid memory leaks. This constant holds
     * a value that defines that 'automatic flushing' criteria, i.e. every time number of stored deferred changes exceeds this value,
     * they are automatically flushed.
     */
    private static final int MAX_DEFERRED_CHANGES_NUMBER = 10000;

    private final AtomicReference<TextChangesStorage> myDeferredChangesStorage = new AtomicReference<TextChangesStorage>();

    private int myStart;
    /**
     * This class implements {@link #subSequence(int, int)} by creating object of the same class that partially shares the same
     * data as the object on which the method is called. So, this field may define interested end offset (if it's non-negative).
     */
    private int myEnd = -1;
    private int myCount = 0;

    private CharSequence myOriginalSequence;
    private char[] myArray;
    private SoftReference<String> myStringRef; // buffers String value - for not to generate it every time
    private int myBufferSize;
    private int myDeferredShift;
    private boolean myDeferredChangeMode;

    // We had a problems with bulk document text processing, hence, debug facilities were introduced. The fields group below work with them.
    // The main idea is to hold all history of bulk processing iteration in order to be able to retrieve it from client and reproduce the
    // problem.

    /**
     * Flag the identifies if current char array should debug bulk processing.
     */
    private final boolean myDebugDeferredProcessing;

    /**
     * Duplicate instance of the current char array that is used during debug processing as follows - apply every text change
     * from the bulk changes group to this instance immediately in order to be able to check if the current 'deferred change-aware'
     * instance functionally behaves at the same way as 'straightforward' one.
     */
    private CharArray myDebugArray;

    /**
     * Holds deferred changes create during the current bulk processing iteration.
     */
    private List<TextChangeImpl> myDebugDeferredChanges;

    /**
     * Document text on bulk processing start.
     */
    private String myDebugTextOnBatchUpdateStart;

    // max chars to hold, bufferSize == 0 means unbounded
    CharArray(int bufferSize) {
        this(bufferSize, new TextChangesStorage(), null, -1, -1);
    }

    private CharArray(final int bufferSize, @NotNull TextChangesStorage deferredChangesStorage, @Nullable char[] data, int start, int end) {
        this(bufferSize, deferredChangesStorage, data, start, end, DEBUG_DEFERRED_PROCESSING);
    }

    private CharArray(final int bufferSize, @NotNull TextChangesStorage deferredChangesStorage, @Nullable char[] data, int start, int end,
                      boolean debugDeferredProcessing) {
        myBufferSize = bufferSize;
        myDeferredChangesStorage.set(deferredChangesStorage);
        if (data == null) {
            myOriginalSequence = "";
        } else {
            myArray = data;
            myCount = end - start;
        }
        if (start >= 0 && end >= 0) {
            myStart = start;
            myEnd = end;
        }

        myDebugDeferredProcessing = debugDeferredProcessing;
        if (myDebugDeferredProcessing) {

            myDebugArray = new CharArray(bufferSize, new TextChangesStorage(), data == null ? null : Arrays.copyOf(data, data.length),
                    start, end, false) {
                @NotNull
                @Override
                protected DocumentEvent beforeChangedUpdate(DocumentImpl subj,
                                                            int offset,
                                                            CharSequence oldString,
                                                            CharSequence newString,
                                                            boolean wholeTextReplaced) {
                    return new DocumentEventImpl(subj, offset, oldString, newString, -1, wholeTextReplaced);
                }

                @Override
                protected void afterChangedUpdate(@NotNull DocumentEvent event, long newModificationStamp) {
                }
            };
            myDebugDeferredChanges = new ArrayList<TextChangeImpl>();
        }
    }

    public void setBufferSize(int bufferSize) {
        myBufferSize = bufferSize;
    }

    @NotNull
    protected abstract DocumentEvent beforeChangedUpdate(DocumentImpl subj,
                                                         int offset,
                                                         @Nullable CharSequence oldString,
                                                         @Nullable CharSequence newString,
                                                         boolean wholeTextReplaced);

    protected abstract void afterChangedUpdate(@NotNull DocumentEvent event, long newModificationStamp);

    public void setText(@Nullable final DocumentImpl subj, final CharSequence chars) {
        myOriginalSequence = chars;
        myArray = null;
        myCount = chars.length();
        myStringRef = null;
        TextChangesStorage storage = myDeferredChangesStorage.get();
        storage.getLock().lock();
        try {
            if (isSubSequence()) {
                myDeferredChangesStorage.set(new TextChangesStorage());
                myStart = 0;
                myEnd = -1;
            } else {
                storage.clear();
            }
        } finally {
            storage.getLock().unlock();
        }

        if (subj != null) {
            trimToSize(subj);
        }

        if (myDebugDeferredProcessing) {
            myDebugArray.setText(subj, chars);
            myDebugDeferredChanges.clear();
        }
    }

    public void replace(DocumentImpl subj,
                        int startOffset, int endOffset, CharSequence toDelete, CharSequence newString, long newModificationStamp,
                        boolean wholeTextReplaced) {
        final DocumentEvent event = beforeChangedUpdate(subj, startOffset, toDelete, newString, wholeTextReplaced);
        startOffset += myStart;
        endOffset += myStart;
        doReplace(startOffset, endOffset, newString);
        afterChangedUpdate(event, newModificationStamp);
    }

    private void doReplace(int startOffset, int endOffset, CharSequence newString) {
        prepareForModification();

        if (isDeferredChangeMode()) {
            storeChange(new TextChangeImpl(newString, startOffset, endOffset));
            if (myDebugDeferredProcessing) {
                myDebugArray.doReplace(startOffset, endOffset, newString);
            }
            return;
        }

        int newLength = newString.length();
        int oldLength = endOffset - startOffset;

        CharArrayUtil.getChars(newString, myArray, startOffset, Math.min(newLength, oldLength));

        if (newLength > oldLength) {
            doInsert(newString.subSequence(oldLength, newLength), endOffset);
        } else if (newLength < oldLength) {
            doRemove(startOffset + newLength, startOffset + oldLength);
        }
    }

    public void remove(DocumentImpl subj, int startIndex, int endIndex, CharSequence toDelete) {
        DocumentEvent event = beforeChangedUpdate(subj, startIndex, toDelete, null, false);
        startIndex += myStart;
        endIndex += myStart;
        doRemove(startIndex, endIndex);
        afterChangedUpdate(event, LocalTimeCounter.currentTime());
    }

    private void doRemove(final int startIndex, final int endIndex) {
        if (startIndex == endIndex) {
            return;
        }
        prepareForModification();

        if (isDeferredChangeMode()) {
            storeChange(new TextChangeImpl("", startIndex, endIndex));
            if (myDebugDeferredProcessing) {
                myDebugArray.doRemove(startIndex, endIndex);
            }
            return;
        }

        if (endIndex < myCount) {
            System.arraycopy(myArray, endIndex, myArray, startIndex, myCount - endIndex);
        }
        myCount -= endIndex - startIndex;
    }

    public void insert(DocumentImpl subj, CharSequence s, int startIndex) {
        DocumentEvent event = beforeChangedUpdate(subj, startIndex, null, s, false);
        startIndex += myStart;
        doInsert(s, startIndex);

        afterChangedUpdate(event, LocalTimeCounter.currentTime());
        trimToSize(subj);
    }

    private void doInsert(final CharSequence s, final int startIndex) {
        prepareForModification();

        if (isDeferredChangeMode()) {
            storeChange(new TextChangeImpl(s, startIndex));
            if (myDebugDeferredProcessing) {
                myDebugArray.doInsert(s, startIndex);
            }
            return;
        }

        int insertLength = s.length();
        myArray = relocateArray(myArray, myCount + insertLength);
        if (startIndex < myCount) {
            System.arraycopy(myArray, startIndex, myArray, startIndex + insertLength, myCount - startIndex);
        }

        CharArrayUtil.getChars(s, myArray, startIndex);
        myCount += insertLength;
    }

    /**
     * Stores given change at collection of deferred changes (merging it with others if necessary) and updates current object
     * state ({@link #length() length} etc).
     *
     * @param change new change to store
     */
    private void storeChange(@NotNull TextChangeImpl change) {
        if (!change.isWithinBounds(length())) {
            LOG.error(String.format(
                    "Invalid change attempt detected - given change bounds are not within the current char array. Change: %d:%d-%d",
                    change.getText().length(), change.getStart(), change.getEnd()
            ), dumpState());
            return;
        }
        TextChangesStorage storage = myDeferredChangesStorage.get();
        storage.getLock().lock();
        try {
            doStoreChange(change);
        } finally {
            storage.getLock().unlock();
        }
    }

    private void doStoreChange(@NotNull TextChangeImpl change) {
        TextChangesStorage storage = myDeferredChangesStorage.get();
        if (storage.size() >= MAX_DEFERRED_CHANGES_NUMBER) {
            flushDeferredChanged(storage);
        }
        storage.store(change);
        myDeferredShift += change.getDiff();

        if (myDebugDeferredProcessing) {
            myDebugDeferredChanges.add(change);
        }
    }

    private void prepareForModification() {
        if (myOriginalSequence != null) {
            myArray = new char[myOriginalSequence.length()];
            CharArrayUtil.getChars(myOriginalSequence, myArray, 0);
            myOriginalSequence = null;
        }
        myStringRef = null;
    }

    public CharSequence getCharArray() {
        if (myOriginalSequence != null) return myOriginalSequence;
        return this;
    }

    public String toString() {
        String str = myStringRef != null ? myStringRef.get() : null;
        if (str == null) {
            if (myOriginalSequence != null) {
                str = myOriginalSequence.toString();
            } else if (!hasDeferredChanges()) {
                str = new String(myArray, myStart, myCount);
            } else {
                str = substring(0, length()).toString();
            }
            myStringRef = new SoftReference<String>(str);
        }
        if (myDebugDeferredProcessing && isDeferredChangeMode()) {
            String expected = myDebugArray.toString();
            checkStrings("toString()", expected, str);
        }
        return str;
    }

    @Override
    public final int length() {
        final int result = myCount + myDeferredShift;
        if (myDebugDeferredProcessing && isDeferredChangeMode()) {
            int expected = myDebugArray.length();
            if (expected != result) {
                dumpDebugInfo(String.format("Incorrect length() processing. Expected: '%s', actual: '%s'", expected, result));
            }
        }
        return result;
    }

    @Override
    public final char charAt(int i) {
        if (i < 0 || i >= length()) {
            throw new IndexOutOfBoundsException("Wrong offset: " + i + "; count:" + length());
        }
        i += myStart;
        if (myOriginalSequence != null) return myOriginalSequence.charAt(i);
        final char result;
        if (hasDeferredChanges()) {
            TextChangesStorage storage = myDeferredChangesStorage.get();
            storage.getLock().lock();
            try {
                result = storage.charAt(myArray, i);
            } finally {
                storage.getLock().unlock();
            }
        } else {
            result = myArray[i];
        }

        if (myDebugDeferredProcessing && isDeferredChangeMode()) {
            char expected = myDebugArray.charAt(i);
            if (expected != result) {
                dumpDebugInfo(
                        String.format("Incorrect charAt() processing for index %d. Expected: '%c', actual: '%c'", i, expected, result)
                );
            }
        }
        return result;
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        if (start == 0 && end == length()) return this;
        if (myOriginalSequence != null) {
            return myOriginalSequence.subSequence(start, end);
        }
        if (hasDeferredChanges()) {
            return new CharArray(myBufferSize, myDeferredChangesStorage.get(), myArray, myStart + start, myStart + end) {
                @NotNull
                @Override
                protected DocumentEvent beforeChangedUpdate(DocumentImpl subj,
                                                            int offset,
                                                            CharSequence oldString,
                                                            CharSequence newString,
                                                            boolean wholeTextReplaced) {
                    return new DocumentEventImpl(subj, offset, oldString, newString, LocalTimeCounter.currentTime(), wholeTextReplaced);
                }

                @Override
                protected void afterChangedUpdate(@NotNull DocumentEvent event, long newModificationStamp) {
                }

                @Override
                public char[] getChars() {
                    char[] chars = CharArray.this.getChars();
                    char[] result = new char[end - start];
                    System.arraycopy(chars, start, result, 0, result.length);
                    return result;
                }
            };
        } else {
            // We don't use the same approach as with 'defer changes' mode because the former is the new experimental one and this one
            // is rather mature, hence, we just minimizes the risks that something is wrong within the new approach.
            return new CharArrayCharSequence(myArray, start, end);
        }
    }

    private boolean isSubSequence() {
        return myEnd >= 0;
    }

    @Override
    public char[] getChars() {
        if (myOriginalSequence != null) {
            if (myArray == null) {
                myArray = CharArrayUtil.fromSequence(myOriginalSequence);
            }
        }
        flushDeferredChanged(myDeferredChangesStorage.get());
        if (myDebugDeferredProcessing && isDeferredChangeMode()) {
            char[] expected = myDebugArray.getChars();
            for (int i = 0, max = length(); i < max; i++) {
                if (myArray[i] != expected[i]) {
                    dumpDebugInfo(String.format("getChars(). Index: %d, expected: %c, actual: %c", i, expected[i], myArray[i]));
                    break;
                }
            }
        }
        return myArray;
    }

    @Override
    public void getChars(final char[] dst, final int dstOffset) {
        flushDeferredChanged(myDeferredChangesStorage.get());
        if (myOriginalSequence != null) {
            CharArrayUtil.getChars(myOriginalSequence, dst, dstOffset);
        } else {
            System.arraycopy(myArray, myStart, dst, dstOffset, length());
        }

        if (myDebugDeferredProcessing && isDeferredChangeMode()) {
            char[] expected = new char[dst.length];
            myDebugArray.getChars(expected, dstOffset);
            for (int i = dstOffset, j = myStart; i < dst.length && j < myArray.length; i++, j++) {
                if (expected[i] != myArray[j]) {
                    dumpDebugInfo(String.format("getChars(char[], int). Given array of length %d, offset %d. Found char '%c' at index %d, " +
                            "expected to find '%c'", dst.length, dstOffset, myArray[j], i, expected[i]));
                    break;
                }
            }
        }
    }

    public CharSequence substring(final int start, final int end) {
        if (start == end) return "";
        final CharSequence result;
        if (myOriginalSequence == null) {
            TextChangesStorage storage = myDeferredChangesStorage.get();
            storage.getLock().lock();
            try {
                result = storage.substring(myArray, start + myStart, end + myStart);
            } finally {
                storage.getLock().unlock();
            }
        } else {
            result = myOriginalSequence.subSequence(start, end);
        }

        if (myDebugDeferredProcessing && isDeferredChangeMode()) {
            String expected = myDebugArray.substring(start, end).toString();
            checkStrings(String.format("substring(%d, %d)", start, end), expected, result.toString());
        }
        return result;
    }

    private static char[] relocateArray(char[] array, int index) {
        if (index < array.length) {
            return array;
        }

        int newArraySize = array.length;
        if (newArraySize == 0) {
            newArraySize = 16;
        }
        while (newArraySize <= index) {
            newArraySize = newArraySize * 12 / 10 + 1;
        }
        char[] newArray = new char[newArraySize];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    private void trimToSize(DocumentImpl subj) {
        if (myBufferSize != 0 && length() > myBufferSize) {
            flushDeferredChanged(myDeferredChangesStorage.get());
            // make a copy
            remove(subj, 0, myCount - myBufferSize, getCharArray().subSequence(0, myCount - myBufferSize).toString());
        }
    }

    /**
     * @return <code>true</code> if this object is at {@link #setDeferredChangeMode(boolean) defer changes} mode;
     *         <code>false</code> otherwise
     */
    public boolean isDeferredChangeMode() {
        return !DISABLE_DEFERRED_PROCESSING && myDeferredChangeMode;
    }

    public boolean hasDeferredChanges() {
        return !myDeferredChangesStorage.get().isEmpty();
    }

    /**
     * There is a possible case that client of this class wants to perform great number of modifications in a short amount of time
     * (e.g. end-user performs formatting of the document backed by the object of the current class). It may result in significant
     * performance degradation is the changes are performed one by one (every time the change is applied tail content is shifted to
     * the left or right). So, we may want to optimize that by avoiding actual array modification until information about
     * all target changes is provided and perform array data moves only after that.
     * <p/>
     * This method allows to define that <code>'defer changes'</code> mode usages, i.e. expected usage pattern is as follows:
     * <pre>
     * <ol>
     *   <li>
     *     Client of this class enters <code>'defer changes'</code> mode (calls this method with <code>'true'</code> argument).
     *     That means that all subsequent changes will not actually modify backed array data and will be stored separately;
     *   </li>
     *   <li>
     *     Number of target changes are applied to the current object via standard API
     *     ({@link #insert(com.intellij.openapi.editor.impl.DocumentImpl, CharSequence, int) insert},
     *     {@link #remove(com.intellij.openapi.editor.impl.DocumentImpl, int, int, CharSequence) remove} and
     *     {@link #replace(com.intellij.openapi.editor.impl.DocumentImpl, int, int, CharSequence, CharSequence, long, boolean) replace});
     *   </li>
     *   <li>
     *     Client of this class indicates that <code>'massive change time'</code> is over by calling this method with <code>'false'</code>
     *     argument. That flushes all deferred changes (if any) to the backed data array and makes every subsequent change to
     *     be immediate flushed to the backed array;
     *   </li>
     * </ol>
     * </pre>
     * <p/>
     * <b>Note:</b> we can't exclude possibility that <code>'defer changes'</code> mode is started but inadvertently not ended
     * (due to programming error, unexpected exception etc). Hence, this class is free to automatically end
     * <code>'defer changes'</code> mode when necessary in order to avoid memory leak with infinite deferred changes storing.
     *
     * @param deferredChangeMode flag that defines if <code>'defer changes'</code> mode should be used by the current object
     */
    public void setDeferredChangeMode(boolean deferredChangeMode) {
        if (deferredChangeMode && myDebugDeferredProcessing) {
            myDebugArray.setText(null, myDebugTextOnBatchUpdateStart = toString());
            myDebugDeferredChanges.clear();
        }
        myDeferredChangeMode = deferredChangeMode;
        if (!deferredChangeMode) {
            flushDeferredChanged(myDeferredChangesStorage.get());
        }
    }

    private void flushDeferredChanged(@NotNull TextChangesStorage storage) {
        storage.getLock().lock();
        try {
            doFlushDeferredChanged();
        } finally {
            storage.getLock().unlock();
        }
    }

    private void doFlushDeferredChanged() {
        TextChangesStorage storage = myDeferredChangesStorage.get();
        List<TextChangeImpl> changes = storage.getChanges();
        if (changes.isEmpty()) {
            return;
        }

        char[] beforeMerge = null;
        final boolean inPlace;
        if (myDebugDeferredProcessing) {
            beforeMerge = new char[myArray.length];
            System.arraycopy(myArray, 0, beforeMerge, 0, myArray.length);
        }

        BulkChangesMerger changesMerger = BulkChangesMerger.INSTANCE;
        if (myArray.length < length()) {
            myArray = changesMerger.mergeToCharArray(myArray, myCount, changes);
            inPlace = false;
        } else {
            changesMerger.mergeInPlace(myArray, myCount, changes);
            inPlace = true;
        }

        if (myDebugDeferredProcessing) {
            for (int i = 0, max = length(); i < max; i++) {
                if (myArray[i] != myDebugArray.myArray[i]) {
                    dumpDebugInfo(String.format(
                            "flushDeferredChanged(). Index %d, expected: '%c', actual '%c'. Text before merge: '%s', merge inplace: %b",
                            i, myDebugArray.myArray[i], myArray[i], Arrays.toString(beforeMerge), inPlace));
                    break;
                }
            }
        }

        myCount += myDeferredShift;
        myDeferredShift = 0;
        storage.clear();
        myDeferredChangeMode = false;
    }

    @NotNull
    public String dumpState() {
        return String.format(
                "deferred changes mode: %b, length: %d (data array length: %d, deferred shift: %d); view offsets: [%d; %d]; deferred changes: %s",
                isDeferredChangeMode(), length(), myCount, myDeferredShift, myStart, myEnd, myDeferredChangesStorage
        );
    }

    private void checkStrings(@NotNull String operation, @NotNull String expected, @NotNull String actual) {
        if (expected.equals(actual)) {
            return;
        }
        for (int i = 0, max = Math.min(expected.length(), actual.length()); i < max; i++) {
            if (actual.charAt(i) != expected.charAt(i)) {
                dumpDebugInfo(String.format(
                        "Incorrect %s processing. Expected length: %d, actual length: %d. Unmatched symbol at %d - expected: '%c', " +
                                "actual: '%c', expected document: '%s', actual document: '%s'",
                        operation, expected.length(), actual.length(), i, expected.charAt(i), actual.charAt(i), expected, actual
                ));
                return;
            }
        }
        dumpDebugInfo(String.format(
                "Incorrect %s processing. Expected length: %d, actual length: %d, expected: '%s', actual: '%s'",
                operation, expected.length(), actual.length(), expected, actual
        ));
    }

    private void dumpDebugInfo(@NotNull String problem) {
        //LOG.error(String.format(
        //  "/***********************************************************\n" +
        //  " * Please email idea.log to Denis.Zhdanov@jetbrains.com\n" +
        //  " ***********************************************************/\n" +
        //  "Incorrect CharArray processing detected: '%s'. Start: %d, end: %d, text on batch update start: '%s', deferred changes history: %s, "
        //  + "current deferred changes: %s",
        //  problem, myStart, myEnd, myDebugTextOnBatchUpdateStart, myDebugDeferredChanges, myDeferredChangesStorage
        //));
        LOG.error(String.format(
                "Incorrect CharArray processing detected: '%s'. Start: %d, end: %d, text on batch update start: '%s', deferred changes history: %s, "
                        + "current deferred changes: %s",
                problem, myStart, myEnd, myDebugTextOnBatchUpdateStart, myDebugDeferredChanges, myDeferredChangesStorage
        ));
    }
}
