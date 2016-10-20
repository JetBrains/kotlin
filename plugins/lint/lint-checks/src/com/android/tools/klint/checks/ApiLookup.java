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

package com.android.tools.klint.checks;

import static com.android.SdkConstants.ANDROID_PKG;
import static com.android.SdkConstants.DOT_XML;
import static com.android.tools.klint.detector.api.LintUtils.assertionsEnabled;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.repository.api.LocalPackage;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.tools.klint.client.api.LintClient;
//import com.android.tools.klint.client.api.SdkWrapper;
import com.android.tools.klint.detector.api.LintUtils;
import com.android.utils.Pair;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSink;
import com.google.common.io.Files;
import com.google.common.primitives.UnsignedBytes;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Database for API checking: Allows quick lookup of a given class, method or field
 * to see which API level it was introduced in.
 * <p>
 * This class is optimized for quick bytecode lookup used in conjunction with the
 * ASM library: It has lookup methods that take internal JVM signatures, and for a method
 * call for example it processes the owner, name and description parameters separately
 * the way they are provided from ASM.
 * <p>
 * The {@link Api} class provides access to the full Android API along with version
 * information, initialized from an XML file. This lookup class adds a binary cache around
 * the API to make initialization faster and to require fewer objects. It creates
 * a binary cache data structure, which fits in a single byte array, which means that
 * to open the database you can just read in the byte array and go. On one particular
 * machine, this takes about 30-50 ms versus 600-800ms for the full parse. It also
 * helps memory by placing everything in a compact byte array instead of needing separate
 * strings (2 bytes per character in a char[] for the 25k method entries, 11k field entries
 * and 6k class entries) - and it also avoids the same number of Map.Entry objects.
 * When creating the memory data structure it performs a few other steps to help memory:
 * <ul>
 * <li> It stores the strings as single bytes, since all the JVM signatures are in ASCII
 * <li> It strips out the method return types (which takes the binary size down from
 *      about 4.7M to 4.0M)
 * <li> It strips out all APIs that have since=1, since the lookup only needs to find
 *      classes, methods and fields that have an API level *higher* than 1. This drops
 *      the memory use down from 4.0M to 1.7M.
 * </ul>
 */
public class ApiLookup {
    /** Relative path to the api-versions.xml database file within the Lint installation */
    private static final String XML_FILE_PATH = "platform-tools/api/api-versions.xml"; //$NON-NLS-1$
    private static final String FILE_HEADER = "API database used by Android lint\000";
    private static final int BINARY_FORMAT_VERSION = 8;
    private static final boolean DEBUG_SEARCH = false;
    private static final boolean WRITE_STATS = false;

    private static final int CLASS_HEADER_MEMBER_OFFSETS = 1;
    private static final int CLASS_HEADER_API = 2;
    private static final int CLASS_HEADER_DEPRECATED = 3;
    private static final int CLASS_HEADER_INTERFACES = 4;
    private static final int HAS_DEPRECATION_BYTE_FLAG = 1 << 7;
    private static final int API_MASK = ~HAS_DEPRECATION_BYTE_FLAG;

    @VisibleForTesting
    static final boolean DEBUG_FORCE_REGENERATE_BINARY = false;

    private final Api mInfo;
    private byte[] mData;
    private int[] mIndices;

    private static WeakReference<ApiLookup> sInstance = new WeakReference<ApiLookup>(null);

    private int mPackageCount;

    /**
     * Returns an instance of the API database
     *
     * @param client the client to associate with this database - used only for
     *            logging. The database object may be shared among repeated invocations,
     *            and in that case client used will be the one originally passed in.
     *            In other words, this parameter may be ignored if the client created
     *            is not new.
     * @return a (possibly shared) instance of the API database, or null
     *         if its data can't be found
     */
    @Nullable
    public static ApiLookup get(@NonNull LintClient client) {
        synchronized (ApiLookup.class) {
            ApiLookup db = sInstance.get();
            if (db == null) {
                File file = client.findResource(XML_FILE_PATH);
                if (file == null) {
                    // AOSP build environment?
                    String build = System.getenv("ANDROID_BUILD_TOP");   //$NON-NLS-1$
                    if (build != null) {
                        file = new File(build, "development/sdk/api-versions.xml" //$NON-NLS-1$
                                .replace('/', File.separatorChar));
                    }
                }

                if (file == null || !file.exists()) {
                    return null;
                } else {
                    db = get(client, file);
                }
                sInstance = new WeakReference<ApiLookup>(db);
            }

            return db;
        }
    }

    @VisibleForTesting
    @Nullable
    static String getPlatformVersion(@NonNull LintClient client) {
        AndroidSdkHandler sdk = client.getSdk();
        if (sdk != null) {
            LocalPackage pkgInfo = sdk
                    .getLocalPackage(SdkConstants.FD_PLATFORM_TOOLS, client.getRepositoryLogger());
            if (pkgInfo != null) {
                return pkgInfo.getVersion().toShortString();
            }
        }
        return null;
    }

    @VisibleForTesting
    @NonNull
    static String getCacheFileName(@NonNull String xmlFileName, @Nullable String platformVersion) {
        if (LintUtils.endsWith(xmlFileName, DOT_XML)) {
            xmlFileName = xmlFileName.substring(0, xmlFileName.length() - DOT_XML.length());
        }

        StringBuilder sb = new StringBuilder(100);
        sb.append(xmlFileName);

        // Incorporate version number in the filename to avoid upgrade filename
        // conflicts on Windows (such as issue #26663)
        sb.append('-').append(BINARY_FORMAT_VERSION);

        if (platformVersion != null) {
            sb.append('-').append(platformVersion);
        }

        sb.append(".bin"); //$NON-NLS-1$
        return sb.toString();
    }

    /**
     * Returns an instance of the API database
     *
     * @param client the client to associate with this database - used only for
     *            logging
     * @param xmlFile the XML file containing configuration data to use for this
     *            database
     * @return a (possibly shared) instance of the API database, or null
     *         if its data can't be found
     */
    private static ApiLookup get(LintClient client, File xmlFile) {
        if (!xmlFile.exists()) {
            client.log(null, "The API database file %1$s does not exist", xmlFile);
            return null;
        }

        File cacheDir = client.getCacheDir(true/*create*/);
        if (cacheDir == null) {
            cacheDir = xmlFile.getParentFile();
        }

        String platformVersion = getPlatformVersion(client);
        File binaryData = new File(cacheDir, getCacheFileName(xmlFile.getName(), platformVersion));

        if (DEBUG_FORCE_REGENERATE_BINARY) {
            System.err.println("\nTemporarily regenerating binary data unconditionally \nfrom "
                    + xmlFile + "\nto " + binaryData);
            if (!createCache(client, xmlFile, binaryData)) {
                return null;
            }
        } else if (!binaryData.exists() || binaryData.lastModified() < xmlFile.lastModified()
               || binaryData.length() == 0) {
            if (!createCache(client, xmlFile, binaryData)) {
                return null;
            }
        }

        if (!binaryData.exists()) {
            client.log(null, "The API database file %1$s does not exist", binaryData);
            return null;
        }

        return new ApiLookup(client, xmlFile, binaryData, null);
    }

    private static boolean createCache(LintClient client, File xmlFile, File binaryData) {
        long begin = 0;
        if (WRITE_STATS) {
            begin = System.currentTimeMillis();
        }

        Api info = Api.parseApi(xmlFile);

        if (WRITE_STATS) {
            long end = System.currentTimeMillis();
            System.out.println("Reading XML data structures took " + (end - begin) + " ms)");
        }

        if (info != null) {
            try {
                writeDatabase(binaryData, info);
                return true;
            } catch (IOException ioe) {
                client.log(ioe, "Can't write API cache file");
            }
        }

        return false;
    }

    /** Use one of the {@link #get} factory methods instead */
    private ApiLookup(
            @NonNull LintClient client,
            @NonNull File xmlFile,
            @Nullable File binaryFile,
            @Nullable Api info) {
        mInfo = info;

        if (binaryFile != null) {
            readData(client, xmlFile, binaryFile);
        }
    }

    /**
     * Database format:
     * <pre>
     * (Note: all numbers are big endian; the format uses 1, 2, 3 and 4 byte integers.)
     *
     *
     * 1. A file header, which is the exact contents of {@link #FILE_HEADER} encoded
     *     as ASCII characters. The purpose of the header is to identify what the file
     *     is for, for anyone attempting to open the file.
     * 2. A file version number. If the binary file does not match the reader's expected
     *     version, it can ignore it (and regenerate the cache from XML).
     *
     * 3. The index table. When the data file is read, this is used to initialize the
     *    {@link #mIndices} array. The index table is built up like this:
     *    a. The number of index entries (e.g. number of elements in the {@link #mIndices} array)
     *        [1 4-byte int]
     *    b. The number of java/javax packages [1 4 byte int]
     *    c. Offsets to the package entries, one for each package, and each offset is 4 bytes.
     *    d. Offsets to the class entries, one for each class, and each offset is 4 bytes.
     *    e. Offsets to the member entries, one for each member, and each offset is 4 bytes.
     *
     * 4. The member entries -- one for each member. A given class entry will point to the
     *    first and last members in the index table above, and the offset of a given member
     *    is pointing to the offset of these entries.
     *    a. The name and description (except for the return value) of the member, in JVM format
     *       (e.g. for toLowerCase(char) we'd have "toLowerCase(C)". This is converted into
     *       UTF_8 representation as bytes [n bytes, the length of the byte representation of
     *       the description).
     *    b. A terminating 0 byte [1 byte].
     *    c. The API level the member was introduced in [1 byte], BUT with the
     *       top bit ({@link #HAS_DEPRECATION_BYTE_FLAG}) set if the member is deprecated.
     *    d. IF the member is deprecated, the API level the member was deprecated in [1 byte].
     *       Note that this byte does not appear if the bit indicated in (c) is not set.
     *
     * 5. The class entries -- one for each class.
     *    a. The index within this class entry where the metadata (other than the name)
     *       can be found. [1 byte]. This means that if you know a class by its number,
     *       you can quickly jump to its metadata without scanning through the string to
     *       find the end of it, by just adding this byte to the current offset and
     *       then you're at the data described below for (d).
     *    b. The name of the class (just the base name, not the package), as encoded as a
     *       UTF-8 string. [n bytes]
     *    c. A terminating 0 [1 byte].
     *    d. The index in the index table (3) of the first member in the class [a 3 byte integer.]
     *    e. The number of members in the class [a 2 byte integer].
     *    f. The API level the class was introduced in [1 byte], BUT with the
     *       top bit ({@link #HAS_DEPRECATION_BYTE_FLAG}) set if the class is deprecated.
     *    g. IF the class is deprecated, the API level the class was deprecated in [1 byte].
     *       Note that this byte does not appear if the bit indicated in (f) is not set.
     *    h. The number of new super classes and interfaces [1 byte]. This counts only
     *       super classes and interfaces added after the original API level of the class.
     *    i. For each super class or interface counted in h,
     *       I. The index of the class [a 3 byte integer]
     *       II. The API level the class/interface was added [1 byte]
     *
     * 6. The package entries -- one for each package.
     *    a. The name of the package as encoded as a UTF-8 string. [n bytes]
     *    b. A terminating 0 [1 byte].
     *    c. The index in the index table (3) of the first class in the package [a 3 byte integer.]
     *    d. The number of classes in the package [a 2 byte integer].
     * </pre>
     */
    private void readData(@NonNull LintClient client, @NonNull File xmlFile,
            @NonNull File binaryFile) {
        if (!binaryFile.exists()) {
            client.log(null, "%1$s does not exist", binaryFile);
            return;
        }
        long start = System.currentTimeMillis();
        try {
            byte[] b = Files.toByteArray(binaryFile);

            // First skip the header
            int offset = 0;
            byte[] expectedHeader = FILE_HEADER.getBytes(Charsets.US_ASCII);
            for (byte anExpectedHeader : expectedHeader) {
                if (anExpectedHeader != b[offset++]) {
                    client.log(null, "Incorrect file header: not an API database cache " +
                            "file, or a corrupt cache file");
                    return;
                }
            }

            // Read in the format number
            if (b[offset++] != BINARY_FORMAT_VERSION) {
                // Force regeneration of new binary data with up to date format
                if (createCache(client, xmlFile, binaryFile)) {
                    readData(client, xmlFile, binaryFile); // Recurse
                }

                return;
            }

            int indexCount = get4ByteInt(b, offset);
            offset += 4;
            mPackageCount = get4ByteInt(b, offset);
            offset += 4;

            mIndices = new int[indexCount];
            for (int i = 0; i < indexCount; i++) {
                // TODO: Pack the offsets: They increase by a small amount for each entry, so
                // no need to spend 4 bytes on each. These will need to be processed when read
                // back in anyway, so consider storing the offset -deltas- as single bytes and
                // adding them up cumulatively in readData().
                mIndices[i] = get4ByteInt(b, offset);
                offset += 4;
            }
            mData = b;
            // TODO: We only need to keep the data portion here since we've initialized
            // the offset array separately.
            // TODO: Investigate (profile) accessing the byte buffer directly instead of
            // accessing a byte array.
        } catch (Throwable e) {
            client.log(null, "Failure reading binary cache file %1$s", binaryFile.getPath());
            client.log(null, "Please delete the file and restart the IDE/lint: %1$s",
                    binaryFile.getPath());
            client.log(e, null);
        }
        if (WRITE_STATS) {
            long end = System.currentTimeMillis();
            System.out.println("\nRead API database in " + (end - start)
                    + " milliseconds.");
            System.out.println("Size of data table: " + mData.length + " bytes ("
                    + Integer.toString(mData.length / 1024) + "k)\n");
        }
    }

    /** See the {@link #readData(LintClient,File,File)} for documentation on the data format. */
    private static void writeDatabase(File file, Api info) throws IOException {
        Map<String, ApiClass> classMap = info.getClasses();

        List<ApiPackage> packages = Lists.newArrayList(info.getPackages().values());
        Collections.sort(packages);

        // Compute members of each class that must be included in the database; we can
        // skip those that have the same since-level as the containing class. And we
        // also need to keep those entries that are marked deprecated.
        int estimatedSize = 0;
        for (ApiPackage pkg : packages) {
            estimatedSize += 4; // offset entry
            estimatedSize += pkg.getName().length() + 20; // package entry

            if (assertionsEnabled() && !isRelevantOwner(pkg.getName() + "/") &&
                    !pkg.getName().startsWith("android/support")) {
                System.out.println("Warning: isRelevantOwner fails for " + pkg.getName() + "/");
            }

            for (ApiClass apiClass : pkg.getClasses()) {
                estimatedSize += 4; // offset entry
                estimatedSize += apiClass.getName().length() + 20; // class entry

                Set<String> allMethods = apiClass.getAllMethods(info);
                Set<String> allFields = apiClass.getAllFields(info);
                // Strip out all members that have been supported since version 1.
                // This makes the database *much* leaner (down from about 4M to about
                // 1.7M), and this just fills the table with entries that ultimately
                // don't help the API checker since it just needs to know if something
                // requires a version *higher* than the minimum. If in the future the
                // database needs to answer queries about whether a method is public
                // or not, then we'd need to put this data back in.
                int clsSince = apiClass.getSince();
                List<String> members = new ArrayList<String>(allMethods.size() + allFields.size());
                for (String member : allMethods) {
                    if (apiClass.getMethod(member, info) != clsSince
                            || apiClass.getMemberDeprecatedIn(member, info) > 0) {
                        members.add(member);
                    }
                }
                for (String member : allFields) {
                    if (apiClass.getField(member, info) != clsSince
                            || apiClass.getMemberDeprecatedIn(member, info) > 0) {
                        members.add(member);
                    }
                }

                estimatedSize += 2 + 4 * (apiClass.getInterfaces().size());
                if (apiClass.getSuperClasses().size() > 1) {
                    estimatedSize += 2 + 4 * (apiClass.getSuperClasses().size());
                }

                // Only include classes that have one or more members requiring version 2 or higher:
                Collections.sort(members);
                apiClass.members = members;
                for (String member : members) {
                    estimatedSize += member.length();
                    estimatedSize += 16;
                }
            }

            // Ensure the classes are sorted
            Collections.sort(pkg.getClasses());
        }

        // Write header
        ByteBuffer buffer = ByteBuffer.allocate(estimatedSize);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(FILE_HEADER.getBytes(Charsets.US_ASCII));
        buffer.put((byte) BINARY_FORMAT_VERSION);

        int indexCountOffset = buffer.position();
        int indexCount = 0;

        buffer.putInt(0); // placeholder

        // Write the number of packages in the package index
        buffer.putInt(packages.size());

        // Write package index
        int newIndex = buffer.position();
        for (ApiPackage pkg : packages) {
            pkg.indexOffset = newIndex;
            newIndex += 4;
            indexCount++;
        }

        // Write class index
        for (ApiPackage pkg : packages) {
            for (ApiClass cls : pkg.getClasses()) {
                cls.indexOffset = newIndex;
                cls.index = indexCount;
                newIndex += 4;
                indexCount++;
            }
        }

        // Write member indices
        for (ApiPackage pkg : packages) {
            for (ApiClass cls : pkg.getClasses()) {
                if (cls.members != null && !cls.members.isEmpty()) {
                    cls.memberOffsetBegin = newIndex;
                    cls.memberIndexStart = indexCount;
                    for (String ignored : cls.members) {
                        newIndex += 4;
                        indexCount++;
                    }
                    cls.memberOffsetEnd = newIndex;
                    cls.memberIndexLength = indexCount - cls.memberIndexStart;
                } else {
                    cls.memberOffsetBegin = -1;
                    cls.memberOffsetEnd = -1;
                    cls.memberIndexStart = -1;
                    cls.memberIndexLength = 0;
                }
            }
        }

        // Fill in the earlier index count
        buffer.position(indexCountOffset);
        buffer.putInt(indexCount);
        buffer.position(newIndex);

        // Write member entries
        for (ApiPackage pkg : packages) {
            for (ApiClass apiClass : pkg.getClasses()) {
                String clz = apiClass.getName();
                int index = apiClass.memberOffsetBegin;
                for (String member : apiClass.members) {
                    // Update member offset to point to this entry
                    int start = buffer.position();
                    buffer.position(index);
                    buffer.putInt(start);
                    index = buffer.position();
                    buffer.position(start);

                    int since;
                    if (member.indexOf('(') != -1) {
                        since = apiClass.getMethod(member, info);
                    } else {
                        since = apiClass.getField(member, info);
                    }
                    if (since == Integer.MAX_VALUE) {
                        assert false : clz + ':' + member;
                        since = 1;
                    }

                    int deprecatedIn = apiClass.getMemberDeprecatedIn(member, info);
                    if (deprecatedIn != 0) {
                        assert deprecatedIn != -1 : deprecatedIn + " for " + member;
                    }

                    byte[] signature = member.getBytes(Charsets.UTF_8);
                    for (byte b : signature) {
                        // Make sure all signatures are really just simple ASCII
                        assert b == (b & 0x7f) : member;
                        buffer.put(b);
                        // Skip types on methods
                        if (b == (byte) ')') {
                            break;
                        }
                    }
                    buffer.put((byte) 0);
                    int api = since;
                    assert api == UnsignedBytes.toInt((byte) api);
                    assert api >= 1 && api < 0xFF; // max that fits in a byte

                    boolean isDeprecated = deprecatedIn > 0;
                    if (isDeprecated) {
                        api |= HAS_DEPRECATION_BYTE_FLAG;
                    }

                    buffer.put((byte) api);

                    if (isDeprecated) {
                        assert deprecatedIn == UnsignedBytes.toInt((byte) deprecatedIn);
                        buffer.put((byte) deprecatedIn);
                    }
                }
                assert index == apiClass.memberOffsetEnd : apiClass.memberOffsetEnd;
            }
        }

        // Write class entries. These are written together, rather than
        // being spread out among the member entries, in order to have
        // reference locality (search that a binary search through the classes
        // are likely to look at entries near each other.)
        for (ApiPackage pkg : packages) {
            List<ApiClass> classes = pkg.getClasses();
            for (ApiClass cls : classes) {
                int index = buffer.position();
                buffer.position(cls.indexOffset);
                buffer.putInt(index);
                buffer.position(index);
                String name = cls.getSimpleName();

                byte[] nameBytes = name.getBytes(Charsets.UTF_8);
                assert nameBytes.length < 254 : name;
                buffer.put((byte)(nameBytes.length + 2)); // 2: terminating 0, and this byte itself
                buffer.put(nameBytes);
                buffer.put((byte) 0);

                // 3 bytes for beginning, 2 bytes for *length*
                put3ByteInt(buffer, cls.memberIndexStart);
                put2ByteInt(buffer, cls.memberIndexLength);

                ApiClass apiClass = classMap.get(cls.getName());
                assert apiClass != null : cls.getName();
                int since = apiClass.getSince();
                assert since == UnsignedBytes.toInt((byte) since) : since; // make sure it fits
                int deprecatedIn = apiClass.getDeprecatedIn();
                boolean isDeprecated = deprecatedIn > 0;
                // The first byte is deprecated in
                if (isDeprecated) {
                    since |= HAS_DEPRECATION_BYTE_FLAG;
                    assert since == UnsignedBytes.toInt((byte) since) : since; // make sure it fits
                }
                buffer.put((byte) since);
                if (isDeprecated) {
                    assert deprecatedIn == UnsignedBytes.toInt((byte) deprecatedIn) : deprecatedIn;
                    buffer.put((byte) deprecatedIn);
                }

                List<Pair<String, Integer>> interfaces = apiClass.getInterfaces();
                int count = 0;
                if (interfaces != null && !interfaces.isEmpty()) {
                    for (Pair<String, Integer> pair : interfaces) {
                        int api = pair.getSecond();
                        if (api > apiClass.getSince()) {
                            count++;
                        }
                    }
                }
                List<Pair<String, Integer>> supers = apiClass.getSuperClasses();
                if (supers != null && !supers.isEmpty()) {
                    for (Pair<String, Integer> pair : supers) {
                        int api = pair.getSecond();
                        if (api > apiClass.getSince()) {
                            count++;
                        }
                    }
                }
                buffer.put((byte)count);
                if (count > 0) {
                    if (supers != null) {
                        for (Pair<String, Integer> pair : supers) {
                            int api = pair.getSecond();
                            if (api > apiClass.getSince()) {
                                ApiClass superClass = classMap.get(pair.getFirst());
                                assert superClass != null : cls;
                                put3ByteInt(buffer, superClass.index);
                                buffer.put((byte) api);
                            }
                        }
                    }
                    if (interfaces != null) {
                        for (Pair<String, Integer> pair : interfaces) {
                            int api = pair.getSecond();
                            if (api > apiClass.getSince()) {
                                ApiClass interfaceClass = classMap.get(pair.getFirst());
                                assert interfaceClass != null : cls;
                                put3ByteInt(buffer, interfaceClass.index);
                                buffer.put((byte) api);
                            }
                        }
                    }
                }
            }
        }

        for (ApiPackage pkg : packages) {
            int index = buffer.position();
            buffer.position(pkg.indexOffset);
            buffer.putInt(index);
            buffer.position(index);

            byte[] bytes = pkg.getName().getBytes(Charsets.UTF_8);
            buffer.put(bytes);
            buffer.put((byte)0);

            List<ApiClass> classes = pkg.getClasses();
            if (classes.isEmpty()) {
                put3ByteInt(buffer, 0);
                put2ByteInt(buffer, 0);
            } else {
                // 3 bytes for beginning, 2 bytes for *length*
                int firstClassIndex = classes.get(0).index;
                int classCount = classes.get(classes.size() - 1).index - firstClassIndex + 1;
                put3ByteInt(buffer, firstClassIndex);
                put2ByteInt(buffer, classCount);
            }
        }

        int size = buffer.position();
        assert size <= buffer.limit();
        buffer.mark();

        if (WRITE_STATS) {
            System.out.print("Actual binary size: " + size + " bytes");
            System.out.println(String.format(" (%.1fM)", size/(1024*1024.f)));
        }

        // Now dump this out as a file
        // There's probably an API to do this more efficiently; TODO: Look into this.
        byte[] b = new byte[size];
        buffer.rewind();
        buffer.get(b);
        if (file.exists()) {
            boolean deleted = file.delete();
            assert deleted : file;
        }
        ByteSink sink = Files.asByteSink(file);
        sink.write(b);
    }

    // For debugging only
    private String dumpEntry(int offset) {
        if (DEBUG_SEARCH) {
            StringBuilder sb = new StringBuilder(200);
            for (int i = offset; i < mData.length; i++) {
                if (mData[i] == 0) {
                    break;
                }
                char c = (char) UnsignedBytes.toInt(mData[i]);
                sb.append(c);
            }

            return sb.toString();
        } else {
            return "<disabled>"; //$NON-NLS-1$
        }
    }

    private static int compare(byte[] data, int offset, byte terminator, String s, int sOffset,
            int max) {
        int i = offset;
        int j = sOffset;
        for (; j < max; i++, j++) {
            byte b = data[i];
            char c = s.charAt(j);
            // TODO: Check somewhere that the strings are purely in the ASCII range; if not
            // they're not a match in the database
            byte cb = (byte) c;
            int delta = b - cb;
            if (delta != 0) {
                return delta;
            }
        }

        return data[i] - terminator;
    }

    /**
     * Returns the API version required by the given class reference,
     * or -1 if this is not a known API class. Note that it may return -1
     * for classes introduced in version 1; internally the database only
     * stores version data for version 2 and up.
     *
     * @param className the internal name of the class, e.g. its
     *            fully qualified name (as returned by Class.getName(), but with
     *            '.' replaced by '/'.
     * @return the minimum API version the method is supported for, or -1 if
     *         it's unknown <b>or version 1</b>.
     */
    public int getClassVersion(@NonNull String className) {
        //noinspection VariableNotUsedInsideIf
        if (mData != null) {
            return getClassVersion(findClass(className));
        }  else {
            assert mInfo != null;
            ApiClass clz = mInfo.getClass(className);
            if (clz != null) {
                int since = clz.getSince();
                if (since == Integer.MAX_VALUE) {
                    since = -1;
                }
                return since;
            }
        }

        return -1;
    }

    /**
     * Returns true if the given owner class is known in the API database.
     *
     * @param className the internal name of the class, e.g. its fully qualified name (as returned
     *                  by Class.getName(), but with '.' replaced by '/' (and '$' for inner
     *                  classes)
     * @return true if this is a class found in the API database
     */
    public boolean isKnownClass(@NonNull String className) {
        return findClass(className) != -1;
    }

    private int getClassVersion(int classNumber) {
        if (classNumber != -1) {
            int offset = seekClassData(classNumber, CLASS_HEADER_API);
            int api = UnsignedBytes.toInt(mData[offset]) & API_MASK;
            return api > 1 ? api : -1;
        }
        return -1;
    }

    /**
     * Returns the API version required to perform the given cast, or -1 if this is valid for all
     * versions of the class (or, if these are not known classes or if the cast is not valid at
     * all.) <p> Note also that this method should only be called for interfaces that are actually
     * implemented by this class or extending the given super class (check elsewhere); it doesn't
     * distinguish between interfaces implemented in the initial version of the class and interfaces
     * not implemented at all.
     *
     * @param sourceClass      the internal name of the class, e.g. its fully qualified name (as
     *                         returned by Class.getName(), but with '.' replaced by '/'.
     * @param destinationClass the class to cast the sourceClass to
     * @return the minimum API version the method is supported for, or 1 or -1 if it's unknown.
     */
    public int getValidCastVersion(@NonNull String sourceClass,
            @NonNull String destinationClass) {
        if (mData != null) {
            int classNumber = findClass(sourceClass);
            if (classNumber != -1) {
                int interfaceNumber = findClass(destinationClass);
                if (interfaceNumber != -1) {
                    int offset = seekClassData(classNumber, CLASS_HEADER_INTERFACES);
                    int interfaceCount = mData[offset++];
                    for (int i = 0; i < interfaceCount; i++) {
                        int clsNumber = get3ByteInt(mData, offset);
                        offset += 3;
                        int api = mData[offset++];
                        if (clsNumber == interfaceNumber) {
                           return api;
                        }
                    }
                    return getClassVersion(classNumber);
                }
            }
        }  else {
            assert mInfo != null;
            ApiClass clz = mInfo.getClass(sourceClass);
            if (clz != null) {
                List<Pair<String, Integer>> interfaces = clz.getInterfaces();
                for (Pair<String,Integer> pair : interfaces) {
                    String interfaceName = pair.getFirst();
                    if (interfaceName.equals(destinationClass)) {
                        return pair.getSecond();
                    }
                }
            }
        }

        return -1;
    }
    /**
     * Returns the API version the given class was deprecated in, or -1 if the class
     * is not deprecated.
     *
     * @param className the internal name of the method's owner class, e.g. its
     *            fully qualified name (as returned by Class.getName(), but with
     *            '.' replaced by '/'.
     * @return the API version the API was deprecated in, or -1 if
     *         it's unknown <b>or version 0</b>.
     */
    public int getClassDeprecatedIn(@NonNull String className) {
        if (mData != null) {
            int classNumber = findClass(className);
            if (classNumber != -1) {
                int offset = seekClassData(classNumber, CLASS_HEADER_DEPRECATED);
                if (offset == -1) {
                    // Not deprecated
                    return -1;
                }
                int deprecatedIn = UnsignedBytes.toInt(mData[offset]);
                return deprecatedIn != 0 ? deprecatedIn : -1;
            }
        }  else {
            assert mInfo != null;
            ApiClass clz = mInfo.getClass(className);
            if (clz != null) {
                int deprecatedIn = clz.getDeprecatedIn();
                if (deprecatedIn == Integer.MAX_VALUE) {
                    deprecatedIn = -1;
                }
                return deprecatedIn;
            }
        }

        return -1;
    }

    /**
     * Returns the API version required by the given method call. The method is
     * referred to by its {@code owner}, {@code name} and {@code desc} fields.
     * If the method is unknown it returns -1. Note that it may return -1 for
     * classes introduced in version 1; internally the database only stores
     * version data for version 2 and up.
     *
     * @param owner the internal name of the method's owner class, e.g. its
     *            fully qualified name (as returned by Class.getName(), but with
     *            '.' replaced by '/'.
     * @param name the method's name
     * @param desc the method's descriptor - see {@link Type}
     * @return the minimum API version the method is supported for, or 1 or -1 if
     *         it's unknown.
     */
    public int getCallVersion(
            @NonNull String owner,
            @NonNull String name,
            @NonNull String desc) {
        //noinspection VariableNotUsedInsideIf
        if (mData != null) {
            int classNumber = findClass(owner);
            if (classNumber != -1) {
                int api = findMember(classNumber, name, desc);
                if (api == -1) {
                    return getClassVersion(classNumber);
                }
                return api;
            }
        }  else {
            assert mInfo != null;
            ApiClass clz = mInfo.getClass(owner);
            if (clz != null) {
                String signature = name + desc;
                int since = clz.getMethod(signature, mInfo);
                if (since == Integer.MAX_VALUE) {
                    since = -1;
                }
                return since;
            }
        }

        return -1;
    }

    /**
     * Returns the API version the given call was deprecated in, or -1 if the method
     * is not deprecated.
     *
     * @param owner the internal name of the method's owner class, e.g. its
     *            fully qualified name (as returned by Class.getName(), but with
     *            '.' replaced by '/'.
     * @param name the method's name
     * @param desc the method's descriptor - see {@link Type}
     * @return the API version the API was deprecated in, or 1 or -1 if
     *         it's unknown.
     */
    public int getCallDeprecatedIn(
            @NonNull String owner,
            @NonNull String name,
            @NonNull String desc) {
        //noinspection VariableNotUsedInsideIf
        if (mData != null) {
            int classNumber = findClass(owner);
            if (classNumber != -1) {
                int deprecatedIn = findMemberDeprecatedIn(classNumber, name, desc);
                return deprecatedIn != 0 ? deprecatedIn : -1;
            }
        }  else {
            assert mInfo != null;
            ApiClass clz = mInfo.getClass(owner);
            if (clz != null) {
                String signature = name + desc;
                int deprecatedIn = clz.getMemberDeprecatedIn(signature, mInfo);
                if (deprecatedIn == Integer.MAX_VALUE) {
                    deprecatedIn = -1;
                }
                return deprecatedIn;
            }
        }

        return -1;
    }

    /**
     * Returns the API version required to access the given field, or -1 if this
     * is not a known API method. Note that it may return -1 for classes
     * introduced in version 1; internally the database only stores version data
     * for version 2 and up.
     *
     * @param owner the internal name of the method's owner class, e.g. its
     *            fully qualified name (as returned by Class.getName(), but with
     *            '.' replaced by '/'.
     * @param name the method's name
     * @return the minimum API version the method is supported for, or 1 or -1 if
     *         it's unknown.
     */
    public int getFieldVersion(
            @NonNull String owner,
            @NonNull String name) {
        //noinspection VariableNotUsedInsideIf
        if (mData != null) {
            int classNumber = findClass(owner);
            if (classNumber != -1) {
                int api = findMember(classNumber, name, null);
                if (api == -1) {
                    return getClassVersion(classNumber);
                }
                return api;
            }
        }  else {
            assert mInfo != null;
            ApiClass clz = mInfo.getClass(owner);
            if (clz != null) {
                int since = clz.getField(name, mInfo);
                if (since == Integer.MAX_VALUE) {
                    since = -1;
                }
                return since;
            }
        }

        return -1;
    }

    /**
     * Returns the API version the given field was deprecated in, or -1 if the field
     * is not deprecated.
     *
     * @param owner the internal name of the method's owner class, e.g. its
     *            fully qualified name (as returned by Class.getName(), but with
     *            '.' replaced by '/'.
     * @param name the method's name
     * @return the API version the API was deprecated in, or 1 or -1 if
     *         it's unknown.
     */
    public int getFieldDeprecatedIn(
            @NonNull String owner,
            @NonNull String name) {
        //noinspection VariableNotUsedInsideIf
        if (mData != null) {
            int classNumber = findClass(owner);
            if (classNumber != -1) {
                int deprecatedIn = findMemberDeprecatedIn(classNumber, name, null);
                return deprecatedIn != 0 ? deprecatedIn : -1;
            }
        }  else {
            assert mInfo != null;
            ApiClass clz = mInfo.getClass(owner);
            if (clz != null) {
                int deprecatedIn = clz.getMemberDeprecatedIn(name, mInfo);
                if (deprecatedIn == Integer.MAX_VALUE) {
                    deprecatedIn = -1;
                }
                return deprecatedIn;
            }
        }

        return -1;
    }

    /**
     * Returns true if the given owner (in VM format) is relevant to the database.
     * This allows quick filtering out of owners that won't return any data
     * for the various {@code #getFieldVersion} etc methods.
     *
     * @param owner the owner to look up
     * @return true if the owner might be relevant to the API database
     */
    public static boolean isRelevantOwner(@NonNull String owner) {
        if (owner.startsWith("java")) {                   //$NON-NLS-1$ // includes javax/
            return true;
        }
        if (owner.startsWith(ANDROID_PKG)) {
            return !owner.startsWith("/support/", 7);
        } else if (owner.startsWith("org/")) {            //$NON-NLS-1$
            if (owner.startsWith("xml", 4)                //$NON-NLS-1$
                    || owner.startsWith("w3c/", 4)        //$NON-NLS-1$
                    || owner.startsWith("json/", 4)       //$NON-NLS-1$
                    || owner.startsWith("apache/", 4)) {  //$NON-NLS-1$
                return true;
            }
        } else if (owner.startsWith("com/")) {            //$NON-NLS-1$
            if (owner.startsWith("google/", 4)            //$NON-NLS-1$
                    || owner.startsWith("android/", 4)) { //$NON-NLS-1$
                return true;
            }
        } else if (owner.startsWith("junit")              //$NON-NLS-1$
                    || owner.startsWith("dalvik")) {      //$NON-NLS-1$
            return true;
        }

        return false;
    }

    /**
     * Returns true if the given owner (in VM format) is a valid Java package supported
     * in any version of Android.
     *
     * @param owner the package, in VM format
     * @return true if the package is included in one or more versions of Android
     */
    public boolean isValidJavaPackage(@NonNull String owner) {
        return findPackage(owner) != -1;
    }

    /** Returns the package index of the given class, or -1 if it is unknown */
    private int findPackage(@NonNull String owner) {
        assert owner.indexOf('.') == -1 : "Should use / instead of . in owner: " + owner;

        // The index array contains class indexes from 0 to classCount and
        //   member indices from classCount to mIndices.length.
        int low = 0;
        int high = mPackageCount - 1;
        // Compare the api info at the given index.
        int classNameLength = owner.lastIndexOf('/');
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int offset = mIndices[middle];

            if (DEBUG_SEARCH) {
                System.out.println("Comparing string " + owner.substring(0, classNameLength)
                        + " with entry at " + offset + ": " + dumpEntry(offset));
            }

            int compare = compare(mData, offset, (byte) 0, owner, 0, classNameLength);
            if (compare == 0) {
                if (DEBUG_SEARCH) {
                    System.out.println("Found " + dumpEntry(offset));
                }
                return middle;
            }

            if (compare < 0) {
                low = middle + 1;
            } else if (compare > 0) {
                high = middle - 1;
            } else {
                assert false; // compare == 0 already handled above
                return -1;
            }
        }

        return -1;
    }

    private static int get4ByteInt(@NonNull byte[] data, int offset) {
        byte b1 = data[offset++];
        byte b2 = data[offset++];
        byte b3 = data[offset++];
        byte b4 = data[offset];
        // The byte data is always big endian.
        return (b1 & 0xFF) << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8 | (b4 & 0xFF);
    }

    private static void put3ByteInt(@NonNull ByteBuffer buffer, int value) {
        // Big endian
        byte b3 = (byte) (value & 0xFF);
        value >>>= 8;
        byte b2 = (byte) (value & 0xFF);
        value >>>= 8;
        byte b1 = (byte) (value & 0xFF);
        buffer.put(b1);
        buffer.put(b2);
        buffer.put(b3);
    }

    private static void put2ByteInt(@NonNull ByteBuffer buffer, int value) {
        // Big endian
        byte b2 = (byte) (value & 0xFF);
        value >>>= 8;
        byte b1 = (byte) (value & 0xFF);
        buffer.put(b1);
        buffer.put(b2);
    }

    private static int get3ByteInt(@NonNull byte[] mData, int offset) {
        byte b1 = mData[offset++];
        byte b2 = mData[offset++];
        byte b3 = mData[offset];
        // The byte data is always big endian.
        return (b1 & 0xFF) << 16 | (b2 & 0xFF) << 8 | (b3 & 0xFF);
    }

    private static int get2ByteInt(@NonNull byte[] data, int offset) {
        byte b1 = data[offset++];
        byte b2 = data[offset];
        // The byte data is always big endian.
        return (b1 & 0xFF) << 8 | (b2 & 0xFF);
    }

    /** Returns the class number of the given class, or -1 if it is unknown */
    private int findClass(@NonNull String owner) {
        assert owner.indexOf('.') == -1 : "Should use / instead of . in owner: " + owner;

        int packageNumber = findPackage(owner);
        if (packageNumber == -1) {
            return -1;
        }
        int curr = mIndices[packageNumber];
        while (mData[curr] != 0) {
            curr++;
        }
        curr++;

        // 3 bytes for first offset
        int low = get3ByteInt(mData, curr);
        curr += 3;

        int length = get2ByteInt(mData, curr);
        if (length == 0) {
            return -1;
        }
        int high = low + length - 1;
        int index = owner.lastIndexOf('/');
        int classNameLength = owner.length();
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int offset = mIndices[middle];
            offset++; // skip the byte which points to the metadata after the name

            if (DEBUG_SEARCH) {
                System.out.println("Comparing string " + owner.substring(0, classNameLength)
                        + " with entry at " + offset + ": " + dumpEntry(offset));
            }

            int compare = compare(mData, offset, (byte) 0, owner, index + 1, classNameLength);
            if (compare == 0) {
                if (DEBUG_SEARCH) {
                    System.out.println("Found " + dumpEntry(offset));
                }
                return middle;
            }

            if (compare < 0) {
                low = middle + 1;
            } else if (compare > 0) {
                high = middle - 1;
            } else {
                assert false; // compare == 0 already handled above
                return -1;
            }
        }

        return -1;
    }

    private int findMember(int classNumber, @NonNull String name, @Nullable String desc) {
        return findMember(classNumber, name, desc, false);
    }

    private int findMemberDeprecatedIn(int classNumber, @NonNull String name,
            @Nullable String desc) {
        return findMember(classNumber, name, desc, true);
    }

    private int seekClassData(int classNumber, int field) {
        int offset = mIndices[classNumber];
        offset += mData[offset] & 0xFF;
        if (field == CLASS_HEADER_MEMBER_OFFSETS) {
            return offset;
        }
        offset += 5; // 3 bytes for start, 2 bytes for length
        if (field == CLASS_HEADER_API) {
            return offset;
        }
        boolean hasDeprecation = (mData[offset] & HAS_DEPRECATION_BYTE_FLAG) != 0;
        offset++;
        if (field == CLASS_HEADER_DEPRECATED) {
            return hasDeprecation ? offset : -1;
        } else if (hasDeprecation) {
            offset++;
        }
        assert field == CLASS_HEADER_INTERFACES;
        return offset;
    }

    private int findMember(int classNumber, @NonNull String name, @Nullable String desc,
            boolean deprecation) {
        int curr = seekClassData(classNumber, CLASS_HEADER_MEMBER_OFFSETS);

        // 3 bytes for first offset
        int low = get3ByteInt(mData, curr);
        curr += 3;

        int length = get2ByteInt(mData, curr);
        if (length == 0) {
            return -1;
        }
        int high = low + length - 1;

        while (low <= high) {
            int middle = (low + high) >>> 1;
            int offset = mIndices[middle];

            if (DEBUG_SEARCH) {
                System.out.println("Comparing string " + (name + ';' + desc) +
                        " with entry at " + offset + ": " + dumpEntry(offset));
            }

            int compare;
            if (desc != null) {
                // Method
                int nameLength = name.length();
                compare = compare(mData, offset, (byte) '(', name, 0, nameLength);
                if (compare == 0) {
                    offset += nameLength;
                    int argsEnd = desc.indexOf(')');
                    // Only compare up to the ) -- after that we have a return value in the
                    // input description, which isn't there in the database
                    compare = compare(mData, offset, (byte) ')', desc, 0, argsEnd);
                    if (compare == 0) {
                        if (DEBUG_SEARCH) {
                            System.out.println("Found " + dumpEntry(offset));
                        }

                        offset += argsEnd + 1;

                        if (mData[offset++] == 0) {
                            // Yes, terminated argument list: get the API level
                            int api = UnsignedBytes.toInt(mData[offset]);
                            if (deprecation) {
                                if ((api & HAS_DEPRECATION_BYTE_FLAG) != 0) {
                                    return UnsignedBytes.toInt(mData[offset + 1]);
                                } else {
                                    return -1;
                                }
                            } else {
                                return api & API_MASK;
                            }
                        }
                    }
                }
            } else {
                // Field
                int nameLength = name.length();
                compare = compare(mData, offset, (byte) 0, name, 0, nameLength);
                if (compare == 0) {
                    offset += nameLength;
                    if (mData[offset++] == 0) {
                        // Yes, terminated argument list: get the API level
                        int api = UnsignedBytes.toInt(mData[offset]);
                        if (deprecation) {
                            if ((api & HAS_DEPRECATION_BYTE_FLAG) != 0) {
                                return UnsignedBytes.toInt(mData[offset + 1]);
                            } else {
                                return -1;
                            }
                        } else {
                            return api & API_MASK;
                        }
                    }
                }
            }

            if (compare < 0) {
                low = middle + 1;
            } else if (compare > 0) {
                high = middle - 1;
            } else {
                assert false; // compare == 0 already handled above
                return -1;
            }
        }

        return -1;
    }

    /** Clears out any existing lookup instances */
    @VisibleForTesting
    static void dispose() {
        sInstance.clear();
    }
}
