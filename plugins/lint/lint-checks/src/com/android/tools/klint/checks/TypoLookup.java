/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static com.android.SdkConstants.DOT_XML;
import static com.android.tools.lint.detector.api.LintUtils.assertionsEnabled;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.LintUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Database of common typos / misspellings.
 */
public class TypoLookup {
    private static final TypoLookup NONE = new TypoLookup();

    /** String separating misspellings and suggested replacements in the text file */
    private static final String WORD_SEPARATOR = "->";  //$NON-NLS-1$

    /** Relative path to the typos database file within the Lint installation */
    private static final String XML_FILE_PATH = "tools/support/typos-%1$s.txt"; //$NON-NLS-1$
    private static final String FILE_HEADER = "Typo database used by Android lint\000";
    private static final int BINARY_FORMAT_VERSION = 2;
    private static final boolean DEBUG_FORCE_REGENERATE_BINARY = false;
    private static final boolean DEBUG_SEARCH = false;
    private static final boolean WRITE_STATS = false;
    /** Default size to reserve for each API entry when creating byte buffer to build up data */
    private static final int BYTES_PER_ENTRY = 28;

    private byte[] mData;
    private int[] mIndices;
    private int mWordCount;

    private static final WeakHashMap<String, TypoLookup> sInstanceMap =
            new WeakHashMap<String, TypoLookup>();

    /**
     * Returns an instance of the Typo database for the given locale
     *
     * @param client the client to associate with this database - used only for
     *            logging. The database object may be shared among repeated
     *            invocations, and in that case client used will be the one
     *            originally passed in. In other words, this parameter may be
     *            ignored if the client created is not new.
     * @param locale the locale to look up a typo database for (should be a
     *            language code (ISO 639-1, two lowercase character names)
     * @param region the region to look up a typo database for (should be a two
     *            letter ISO 3166-1 alpha-2 country code in upper case) language
     *            code
     * @return a (possibly shared) instance of the typo database, or null if its
     *         data can't be found
     */
    @Nullable
    public static TypoLookup get(@NonNull LintClient client, @NonNull String locale,
            @Nullable String region) {
        synchronized (TypoLookup.class) {
            String key = locale;

            if (region != null && region.length() == 2) { // skip BCP-47 regions
                // Allow for region-specific dictionaries. See for example
                // http://en.wikipedia.org/wiki/American_and_British_English_spelling_differences
                assert region.length() == 2
                        && Character.isUpperCase(region.charAt(0))
                        && Character.isUpperCase(region.charAt(1)) : region;
                // Look for typos-en-rUS.txt etc
                key = locale + 'r' + region;
            }

            TypoLookup db = sInstanceMap.get(key);
            if (db == null) {
                String path = String.format(XML_FILE_PATH, key);
                File file = client.findResource(path);
                if (file == null) {
                    // AOSP build environment?
                    String build = System.getenv("ANDROID_BUILD_TOP");   //$NON-NLS-1$
                    if (build != null) {
                        file = new File(build, ("sdk/files/" //$NON-NLS-1$
                                    + path.substring(path.lastIndexOf('/') + 1))
                                      .replace('/', File.separatorChar));
                    }
                }

                if (file == null || !file.exists()) {
                    //noinspection VariableNotUsedInsideIf
                    if (region != null) {
                        // Fall back to the generic locale (non-region-specific) database
                        return get(client, locale, null);
                    }
                    db = NONE;
                } else {
                    db = get(client, file);
                    assert db != null : file;
                }
                sInstanceMap.put(key, db);
            }

            if (db == NONE) {
                return null;
            } else {
                return db;
            }
        }
    }

    /**
     * Returns an instance of the typo database
     *
     * @param client the client to associate with this database - used only for
     *            logging
     * @param xmlFile the XML file containing configuration data to use for this
     *            database
     * @return a (possibly shared) instance of the typo database, or null
     *         if its data can't be found
     */
    @Nullable
    private static TypoLookup get(LintClient client, File xmlFile) {
        if (!xmlFile.exists()) {
            client.log(null, "The typo database file %1$s does not exist", xmlFile);
            return null;
        }

        String name = xmlFile.getName();
        if (LintUtils.endsWith(name, DOT_XML)) {
            name = name.substring(0, name.length() - DOT_XML.length());
        }
        File cacheDir = client.getCacheDir(true/*create*/);
        if (cacheDir == null) {
            cacheDir = xmlFile.getParentFile();
        }

        File binaryData = new File(cacheDir, name
                // Incorporate version number in the filename to avoid upgrade filename
                // conflicts on Windows (such as issue #26663)
                + '-' + BINARY_FORMAT_VERSION + ".bin"); //$NON-NLS-1$

        if (DEBUG_FORCE_REGENERATE_BINARY) {
            System.err.println("\nTemporarily regenerating binary data unconditionally \nfrom "
                    + xmlFile + "\nto " + binaryData);
            if (!createCache(client, xmlFile, binaryData)) {
                return null;
            }
        } else if (!binaryData.exists() || binaryData.lastModified() < xmlFile.lastModified()) {
            if (!createCache(client, xmlFile, binaryData)) {
                return null;
            }
        }

        if (!binaryData.exists()) {
            client.log(null, "The typo database file %1$s does not exist", binaryData);
            return null;
        }

        return new TypoLookup(client, xmlFile, binaryData);
    }

    private static boolean createCache(LintClient client, File xmlFile, File binaryData) {
        long begin = 0;
        if (WRITE_STATS) {
            begin = System.currentTimeMillis();
        }

        // Read in data
        List<String> lines;
        try {
            lines = Files.readLines(xmlFile, Charsets.UTF_8);
        } catch (IOException e) {
            client.log(e, "Can't read typo database file");
            return false;
        }

        if (WRITE_STATS) {
            long end = System.currentTimeMillis();
            System.out.println("Reading data structures took " + (end - begin) + " ms)");
        }

        try {
            writeDatabase(binaryData, lines);
            return true;
        } catch (IOException ioe) {
            client.log(ioe, "Can't write typo cache file");
        }

        return false;
    }

    /** Use one of the {@link #get} factory methods instead */
    private TypoLookup(
            @NonNull LintClient client,
            @NonNull File xmlFile,
            @Nullable File binaryFile) {
        if (binaryFile != null) {
            readData(client, xmlFile, binaryFile);
        }
    }

    private TypoLookup() {
    }

    private void readData(@NonNull LintClient client, @NonNull File xmlFile,
            @NonNull File binaryFile) {
        if (!binaryFile.exists()) {
            client.log(null, "%1$s does not exist", binaryFile);
            return;
        }
        long start = System.currentTimeMillis();
        try {
            MappedByteBuffer buffer = Files.map(binaryFile, MapMode.READ_ONLY);
            assert buffer.order() == ByteOrder.BIG_ENDIAN;

            // First skip the header
            byte[] expectedHeader = FILE_HEADER.getBytes(Charsets.US_ASCII);
            buffer.rewind();
            for (int offset = 0; offset < expectedHeader.length; offset++) {
                if (expectedHeader[offset] != buffer.get()) {
                    client.log(null, "Incorrect file header: not an typo database cache " +
                            "file, or a corrupt cache file");
                    return;
                }
            }

            // Read in the format number
            if (buffer.get() != BINARY_FORMAT_VERSION) {
                // Force regeneration of new binary data with up to date format
                if (createCache(client, xmlFile, binaryFile)) {
                    readData(client, xmlFile, binaryFile); // Recurse
                }

                return;
            }

            mWordCount = buffer.getInt();

            // Read in the word table indices;
            int count = mWordCount;
            int[] offsets = new int[count];

            // Another idea: I can just store the DELTAS in the file (and add them up
            // when reading back in) such that it takes just ONE byte instead of four!

            for (int i = 0; i < count; i++) {
                offsets[i] = buffer.getInt();
            }

            // No need to read in the rest -- we'll just keep the whole byte array in memory
            // TODO: Make this code smarter/more efficient.
            int size = buffer.limit();
            byte[] b = new byte[size];
            buffer.rewind();
            buffer.get(b);
            mData = b;
            mIndices = offsets;

            // TODO: We only need to keep the data portion here since we've initialized
            // the offset array separately.
            // TODO: Investigate (profile) accessing the byte buffer directly instead of
            // accessing a byte array.
        } catch (IOException e) {
            client.log(e, null);
        }
        if (WRITE_STATS) {
            long end = System.currentTimeMillis();
            System.out.println("\nRead typo database in " + (end - start)
                    + " milliseconds.");
            System.out.println("Size of data table: " + mData.length + " bytes ("
                    + Integer.toString(mData.length/1024) + "k)\n");
        }
    }

    /** See the {@link #readData(LintClient,File,File)} for documentation on the data format. */
    private static void writeDatabase(File file, List<String> lines) throws IOException {
        /*
         * 1. A file header, which is the exact contents of {@link FILE_HEADER} encoded
         *     as ASCII characters. The purpose of the header is to identify what the file
         *     is for, for anyone attempting to open the file.
         * 2. A file version number. If the binary file does not match the reader's expected
         *     version, it can ignore it (and regenerate the cache from XML).
         */

        // Drop comments etc
        List<String> words = new ArrayList<String>(lines.size());
        for (String line : lines) {
            if (!line.isEmpty() && Character.isLetter(line.charAt(0))) {
                int end = line.indexOf(WORD_SEPARATOR);
                if (end == -1) {
                    end = line.trim().length();
                }
                String typo = line.substring(0, end).trim();
                String replacements = line.substring(end + WORD_SEPARATOR.length()).trim();
                if (replacements.isEmpty()) {
                    // We don't support empty replacements
                    continue;
                }
                String combined = typo + (char) 0 + replacements;

                words.add(combined);
            }
        }

        byte[][] wordArrays = new byte[words.size()][];
        for (int i = 0, n = words.size(); i < n; i++) {
            String word = words.get(i);
            wordArrays[i] = word.getBytes(Charsets.UTF_8);
        }
        // Sort words, using our own comparator to ensure that it matches the
        // binary search in getTypos()
        Comparator<byte[]> comparator = new Comparator<byte[]>() {
            @Override
            public int compare(byte[] o1, byte[] o2) {
                return TypoLookup.compare(o1, 0, (byte) 0, o2, 0, o2.length);
            }
        };
        Arrays.sort(wordArrays, comparator);

        byte[] headerBytes = FILE_HEADER.getBytes(Charsets.US_ASCII);
        int entryCount = wordArrays.length;
        int capacity = entryCount * BYTES_PER_ENTRY + headerBytes.length + 5;
        ByteBuffer buffer = ByteBuffer.allocate(capacity);
        buffer.order(ByteOrder.BIG_ENDIAN);
        //  1. A file header, which is the exact contents of {@link FILE_HEADER} encoded
        //      as ASCII characters. The purpose of the header is to identify what the file
        //      is for, for anyone attempting to open the file.
        buffer.put(headerBytes);

        //  2. A file version number. If the binary file does not match the reader's expected
        //      version, it can ignore it (and regenerate the cache from XML).
        buffer.put((byte) BINARY_FORMAT_VERSION);

        //  3. The number of words [1 int]
        buffer.putInt(entryCount);

        //  4. Word offset table (one integer per word, pointing to the byte offset in the
        //       file (relative to the beginning of the file) where each word begins.
        //       The words are always sorted alphabetically.
        int wordOffsetTable = buffer.position();

        // Reserve enough room for the offset table here: we will backfill it with pointers
        // as we're writing out the data structures below
        for (int i = 0, n = entryCount; i < n; i++) {
            buffer.putInt(0);
        }

        int nextEntry = buffer.position();
        int nextOffset = wordOffsetTable;

        // 7. Word entry table. Each word entry consists of the word, followed by the byte 0
        //      as a terminator, followed by a comma separated list of suggestions (which
        //      may be empty), or a final 0.
        for (int i = 0; i < entryCount; i++) {
            byte[] word = wordArrays[i];
            buffer.position(nextOffset);
            buffer.putInt(nextEntry);
            nextOffset = buffer.position();
            buffer.position(nextEntry);

            buffer.put(word); // already embeds 0 to separate typo from words
            buffer.put((byte) 0);

            nextEntry = buffer.position();
        }

        int size = buffer.position();
        assert size <= buffer.limit();
        buffer.mark();

        if (WRITE_STATS) {
            System.out.println("Wrote " + words.size() + " word entries");
            System.out.print("Actual binary size: " + size + " bytes");
            System.out.println(String.format(" (%.1fM)", size/(1024*1024.f)));

            System.out.println("Allocated size: " + (entryCount * BYTES_PER_ENTRY) + " bytes");
            System.out.println("Required bytes per entry: " + (size/ entryCount) + " bytes");
        }

        // Now dump this out as a file
        // There's probably an API to do this more efficiently; TODO: Look into this.
        byte[] b = new byte[size];
        buffer.rewind();
        buffer.get(b);
        FileOutputStream output = Files.newOutputStreamSupplier(file).getOutput();
        output.write(b);
        output.close();
    }

    // For debugging only
    private String dumpEntry(int offset) {
        if (DEBUG_SEARCH) {
            int end = offset;
            while (mData[end] != 0) {
                end++;
            }
            return new String(mData, offset, end - offset, Charsets.UTF_8);
        } else {
            return "<disabled>"; //$NON-NLS-1$
        }
    }

    /** Comparison function: *only* used for ASCII strings */
    @VisibleForTesting
    static int compare(byte[] data, int offset, byte terminator, CharSequence s,
            int begin, int end) {
        int i = offset;
        int j = begin;
        for (; ; i++, j++) {
            byte b = data[i];
            if (b == ' ') {
                // We've matched up to the space in a split-word typo, such as
                // in German all zu=>allzu; here we've matched just past "all".
                // Rather than terminating, attempt to continue in the buffer.
                if (j == end) {
                    int max = s.length();
                    if (end < max && s.charAt(end) == ' ') {
                        // Find next word
                        for (; end < max; end++) {
                            char c = s.charAt(end);
                            if (!Character.isLetter(c)) {
                                if (c == ' ' && end == j) {
                                    continue;
                                }
                                break;
                            }
                        }
                    }
                }
            }

            if (j == end) {
                break;
            }

            if (b == '*') {
                // Glob match (only supported at the end)
                return 0;
            }
            char c = s.charAt(j);
            byte cb = (byte) c;
            int delta = b - cb;
            if (delta != 0) {
                cb = (byte) Character.toLowerCase(c);
                if (b != cb) {
                    // Ensure that it has the right sign
                    b = (byte) Character.toLowerCase(b);
                    delta = b - cb;
                    if (delta != 0) {
                        return delta;
                    }
                }
            }
        }

        return data[i] - terminator;
    }

    /** Comparison function used for general UTF-8 encoded strings */
    @VisibleForTesting
    static int compare(byte[] data, int offset, byte terminator, byte[] s,
            int begin, int end) {
        int i = offset;
        int j = begin;
        for (; ; i++, j++) {
            byte b = data[i];
            if (b == ' ') {
                // We've matched up to the space in a split-word typo, such as
                // in German all zu=>allzu; here we've matched just past "all".
                // Rather than terminating, attempt to continue in the buffer.
                // We've matched up to the space in a split-word typo, such as
                // in German all zu=>allzu; here we've matched just past "all".
                // Rather than terminating, attempt to continue in the buffer.
                if (j == end) {
                    int max = s.length;
                    if (end < max && s[end] == ' ') {
                        // Find next word
                        for (; end < max; end++) {
                            byte cb = s[end];
                            if (!isLetter(cb)) {
                                if (cb == ' ' && end == j) {
                                    continue;
                                }
                                break;
                            }
                        }
                    }
                }
            }

            if (j == end) {
                break;
            }
            if (b == '*') {
                // Glob match (only supported at the end)
                return 0;
            }
            byte cb = s[j];
            int delta = b - cb;
            if (delta != 0) {
                cb = toLowerCase(cb);
                b = toLowerCase(b);
                delta = b - cb;
                if (delta != 0) {
                    return delta;
                }
            }

            if (b == terminator || cb == terminator) {
                return delta;
            }
        }

        return data[i] - terminator;
    }

    /**
     * Look up whether this word is a typo, and if so, return the typo itself
     * and one or more likely meanings
     *
     * @param text the string containing the word
     * @param begin the index of the first character in the word
     * @param end the index of the first character after the word. Note that the
     *            search may extend <b>beyond</b> this index, if for example the
     *            word matches a multi-word typo in the dictionary
     * @return a list of the typo itself followed by the replacement strings if
     *         the word represents a typo, and null otherwise
     */
    @Nullable
    public List<String> getTypos(@NonNull CharSequence text, int begin, int end) {
        assert end <= text.length();

        if (assertionsEnabled()) {
            for (int i = begin; i < end; i++) {
                char c = text.charAt(i);
                if (c >= 128) {
                    assert false : "Call the UTF-8 version of this method instead";
                    return null;
                }
            }
        }

        int low = 0;
        int high = mWordCount - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int offset = mIndices[middle];

            if (DEBUG_SEARCH) {
                System.out.println("Comparing string " + text +" with entry at " + offset
                        + ": " + dumpEntry(offset));
            }

            // Compare the word at the given index.
            int compare = compare(mData, offset, (byte) 0, text, begin, end);

            if (compare == 0) {
                offset = mIndices[middle];

                // Don't allow matching uncapitalized words, such as "enlish", when
                // the dictionary word is capitalized, "Enlish".
                if (mData[offset] != text.charAt(begin)
                        && Character.isLowerCase(text.charAt(begin))) {
                    return null;
                }

                // Make sure there is a case match; we only want to allow
                // matching capitalized words to capitalized typos or uncapitalized typos
                //  (e.g. "Teh" and "teh" to "the"), but not uncapitalized words to capitalized
                // typos (e.g. "enlish" to "Enlish").
                String glob = null;
                for (int i = begin; ; i++) {
                    byte b = mData[offset++];
                    if (b == 0) {
                        offset--;
                        break;
                    } else if (b == '*') {
                        int globEnd = i;
                        while (globEnd < text.length()
                                && Character.isLetter(text.charAt(globEnd))) {
                            globEnd++;
                        }
                        glob = text.subSequence(i, globEnd).toString();
                        break;
                    }
                    char c = text.charAt(i);
                    byte cb = (byte) c;
                    if (b != cb && i > begin) {
                        return null;
                    }
                }

                return computeSuggestions(mIndices[middle], offset, glob);
            }

            if (compare < 0) {
                low = middle + 1;
            } else if (compare > 0) {
                high = middle - 1;
            } else {
                assert false; // compare == 0 already handled above
                return null;
            }
        }

        return null;
    }

    /**
     * Look up whether this word is a typo, and if so, return the typo itself
     * and one or more likely meanings
     *
     * @param utf8Text the string containing the word, encoded as UTF-8
     * @param begin the index of the first character in the word
     * @param end the index of the first character after the word. Note that the
     *            search may extend <b>beyond</b> this index, if for example the
     *            word matches a multi-word typo in the dictionary
     * @return a list of the typo itself followed by the replacement strings if
     *         the word represents a typo, and null otherwise
     */
    @Nullable
    public List<String> getTypos(@NonNull byte[] utf8Text, int begin, int end) {
        assert end <= utf8Text.length;

        int low = 0;
        int high = mWordCount - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int offset = mIndices[middle];

            if (DEBUG_SEARCH) {
                String s = new String(Arrays.copyOfRange(utf8Text, begin, end), Charsets.UTF_8);
                System.out.println("Comparing string " + s +" with entry at " + offset
                        + ": " + dumpEntry(offset));
                System.out.println("   middle=" + middle + ", low=" + low + ", high=" + high);
            }

            // Compare the word at the given index.
            int compare = compare(mData, offset, (byte) 0, utf8Text, begin, end);

            if (DEBUG_SEARCH) {
                System.out.println(" signum=" + (int)Math.signum(compare) + ", delta=" + compare);
            }

            if (compare == 0) {
                offset = mIndices[middle];

                // Don't allow matching uncapitalized words, such as "enlish", when
                // the dictionary word is capitalized, "Enlish".
                if (mData[offset] != utf8Text[begin] && isUpperCase(mData[offset])) {
                    return null;
                }

                // Make sure there is a case match; we only want to allow
                // matching capitalized words to capitalized typos or uncapitalized typos
                //  (e.g. "Teh" and "teh" to "the"), but not uncapitalized words to capitalized
                // typos (e.g. "enlish" to "Enlish").
                String glob = null;
                for (int i = begin; ; i++) {
                    byte b = mData[offset++];
                    if (b == 0) {
                        offset--;
                        break;
                    } else if (b == '*') {
                        int globEnd = i;
                        while (globEnd < utf8Text.length && isLetter(utf8Text[globEnd])) {
                            globEnd++;
                        }
                        glob = new String(utf8Text, i, globEnd - i, Charsets.UTF_8);
                        break;
                    }
                    byte cb = utf8Text[i];
                    if (b != cb && i > begin) {
                        return null;
                    }
                }

                return computeSuggestions(mIndices[middle], offset, glob);
            }

            if (compare < 0) {
                low = middle + 1;
            } else if (compare > 0) {
                high = middle - 1;
            } else {
                assert false; // compare == 0 already handled above
                return null;
            }
        }

        return null;
    }

    private List<String> computeSuggestions(int begin, int offset, String glob) {
        String typo = new String(mData, begin, offset - begin, Charsets.UTF_8);

        if (glob != null) {
            typo = typo.replaceAll("\\*", glob); //$NON-NLS-1$
        }

        assert mData[offset] == 0;
        offset++;
        int replacementEnd = offset;
        while (mData[replacementEnd] != 0) {
            replacementEnd++;
        }
        String replacements = new String(mData, offset, replacementEnd - offset, Charsets.UTF_8);
        List<String> words = new ArrayList<String>();
        words.add(typo);

        // The first entry should be the typo itself. We need to pass this back since due
        // to multi-match words and globbing it could extend beyond the initial word range

        for (String s : Splitter.on(',').omitEmptyStrings().trimResults().split(replacements)) {
            if (glob != null) {
                // Need to append the glob string to each result
                words.add(s.replaceAll("\\*", glob)); //$NON-NLS-1$
            } else {
                words.add(s);
            }
        }

        return words;
    }

    // "Character" handling for bytes. This assumes that the bytes correspond to Unicode
    // characters in the ISO 8859-1 range, which is are encoded the same way in UTF-8.
    // This obviously won't work to for example uppercase to lowercase conversions for
    // multi byte characters, which means we simply won't catch typos if the dictionaries
    // contain these. None of the currently included dictionaries do. However, it does
    // help us properly deal with punctuation and spacing characters.

    static boolean isUpperCase(byte b) {
        return Character.isUpperCase((char) b);
    }

    static byte toLowerCase(byte b) {
        return (byte) Character.toLowerCase((char) b);
    }

    static boolean isSpace(byte b) {
        return Character.isWhitespace((char) b);
    }

    static boolean isLetter(byte b) {
        // Assume that multi byte characters represent letters in other languages.
        // Obviously, it could be unusual punctuation etc but letters are more likely
        // in this context.
        return Character.isLetter((char) b) || (b & 0x80) != 0;
    }
}
