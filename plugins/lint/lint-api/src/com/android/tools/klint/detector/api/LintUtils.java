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

package com.android.tools.klint.detector.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.ApiVersion;
import com.android.ide.common.rendering.api.ItemResourceValue;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.StyleResourceValue;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceItem;
import com.android.resources.ResourceUrl;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.LocaleQualifier;
import com.android.resources.FolderTypeRelationship;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkVersionInfo;
import com.android.tools.klint.client.api.LintClient;
import com.android.utils.PositionXmlParser;
import com.android.utils.SdkUtils;
import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.psi.*;
import lombok.ast.ImportDeclaration;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.FieldNode;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UParenthesizedExpression;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.android.SdkConstants.*;
import static com.android.ide.common.resources.configuration.FolderConfiguration.QUALIFIER_SPLITTER;
import static com.android.ide.common.resources.configuration.LocaleQualifier.BCP_47_PREFIX;
import static com.android.tools.klint.client.api.JavaParser.*;


/**
 * Useful utility methods related to lint.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class LintUtils {
    // Utility class, do not instantiate
    private LintUtils() {
    }

    /**
     * Format a list of strings, and cut of the list at {@code maxItems} if the
     * number of items are greater.
     *
     * @param strings the list of strings to print out as a comma separated list
     * @param maxItems the maximum number of items to print
     * @return a comma separated list
     */
    @NonNull
    public static String formatList(@NonNull List<String> strings, int maxItems) {
        StringBuilder sb = new StringBuilder(20 * strings.size());

        for (int i = 0, n = strings.size(); i < n; i++) {
            if (sb.length() > 0) {
                sb.append(", "); //$NON-NLS-1$
            }
            sb.append(strings.get(i));

            if (maxItems > 0 && i == maxItems - 1 && n > maxItems) {
                sb.append(String.format("... (%1$d more)", n - i - 1));
                break;
            }
        }

        return sb.toString();
    }

    /**
     * Determine if the given type corresponds to a resource that has a unique
     * file
     *
     * @param type the resource type to check
     * @return true if the given type corresponds to a file-type resource
     */
    public static boolean isFileBasedResourceType(@NonNull ResourceType type) {
        List<ResourceFolderType> folderTypes = FolderTypeRelationship.getRelatedFolders(type);
        for (ResourceFolderType folderType : folderTypes) {
            if (folderType != ResourceFolderType.VALUES) {
                return type != ResourceType.ID;
            }
        }
        return false;
    }

    /**
     * Returns true if the given file represents an XML file
     *
     * @param file the file to be checked
     * @return true if the given file is an xml file
     */
    public static boolean isXmlFile(@NonNull File file) {
        return SdkUtils.endsWithIgnoreCase(file.getPath(), DOT_XML);
    }

    /**
     * Returns true if the given file represents a bitmap drawable file
     *
     * @param file the file to be checked
     * @return true if the given file is an xml file
     */
    public static boolean isBitmapFile(@NonNull File file) {
        String path = file.getPath();
        // endsWith(name, DOT_PNG) is also true for endsWith(name, DOT_9PNG)
        return endsWith(path, DOT_PNG)
                || endsWith(path, DOT_JPG)
                || endsWith(path, DOT_GIF)
                || endsWith(path, DOT_JPEG)
                || endsWith(path, DOT_WEBP);
    }

    /**
     * Case insensitive ends with
     *
     * @param string the string to be tested whether it ends with the given
     *            suffix
     * @param suffix the suffix to check
     * @return true if {@code string} ends with {@code suffix},
     *         case-insensitively.
     */
    public static boolean endsWith(@NonNull String string, @NonNull String suffix) {
        return string.regionMatches(true /* ignoreCase */, string.length() - suffix.length(),
                suffix, 0, suffix.length());
    }

    /**
     * Case insensitive starts with
     *
     * @param string the string to be tested whether it starts with the given prefix
     * @param prefix the prefix to check
     * @param offset the offset to start checking with
     * @return true if {@code string} starts with {@code prefix},
     *         case-insensitively.
     */
    public static boolean startsWith(@NonNull String string, @NonNull String prefix, int offset) {
        return string.regionMatches(true /* ignoreCase */, offset, prefix, 0, prefix.length());
    }

    /**
     * Returns the basename of the given filename, unless it's a dot-file such as ".svn".
     *
     * @param fileName the file name to extract the basename from
     * @return the basename (the filename without the file extension)
     */
    public static String getBaseName(@NonNull String fileName) {
        int extension = fileName.indexOf('.');
        if (extension > 0) {
            return fileName.substring(0, extension);
        } else {
            return fileName;
        }
    }

    /**
     * Returns the children elements of the given node
     *
     * @param node the parent node
     * @return a list of element children, never null
     */
    @NonNull
    public static List<Element> getChildren(@NonNull Node node) {
        NodeList childNodes = node.getChildNodes();
        List<Element> children = new ArrayList<Element>(childNodes.getLength());
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                children.add((Element) child);
            }
        }

        return children;
    }

    /**
     * Returns the <b>number</b> of children of the given node
     *
     * @param node the parent node
     * @return the count of element children
     */
    public static int getChildCount(@NonNull Node node) {
        NodeList childNodes = node.getChildNodes();
        int childCount = 0;
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                childCount++;
            }
        }

        return childCount;
    }

    /**
     * Returns true if the given element is the root element of its document
     *
     * @param element the element to test
     * @return true if the element is the root element
     */
    public static boolean isRootElement(Element element) {
        return element == element.getOwnerDocument().getDocumentElement();
    }

    /**
     * Returns the corresponding R field name for the given XML resource name
     * @param styleName the XML name
     * @return the corresponding R field name
     */
    public static String getFieldName(@NonNull String styleName) {
        for (int i = 0, n = styleName.length(); i < n; i++) {
            char c = styleName.charAt(i);
            if (c == '.' || c == '-' || c == ':') {
                return styleName.replace('.', '_').replace('-', '_').replace(':', '_');
            }
        }

        return styleName;
    }

    /**
     * Returns the given id without an {@code @id/} or {@code @+id} prefix
     *
     * @param id the id to strip
     * @return the stripped id, never null
     */
    @NonNull
    public static String stripIdPrefix(@Nullable String id) {
        if (id == null) {
            return "";
        } else if (id.startsWith(NEW_ID_PREFIX)) {
            return id.substring(NEW_ID_PREFIX.length());
        } else if (id.startsWith(ID_PREFIX)) {
            return id.substring(ID_PREFIX.length());
        }

        return id;
    }

    /**
     * Returns true if the given two id references match. This is similar to
     * String equality, but it also considers "{@code @+id/foo == @id/foo}.
     *
     * @param id1 the first id to compare
     * @param id2 the second id to compare
     * @return true if the two id references refer to the same id
     */
    public static boolean idReferencesMatch(@Nullable String id1, @Nullable String id2) {
        if (id1 == null || id2 == null || id1.isEmpty() || id2.isEmpty()) {
            return false;
        }
        if (id1.startsWith(NEW_ID_PREFIX)) {
            if (id2.startsWith(NEW_ID_PREFIX)) {
                return id1.equals(id2);
            } else {
                assert id2.startsWith(ID_PREFIX) : id2;
                return ((id1.length() - id2.length())
                            == (NEW_ID_PREFIX.length() - ID_PREFIX.length()))
                        && id1.regionMatches(NEW_ID_PREFIX.length(), id2,
                                ID_PREFIX.length(),
                                id2.length() - ID_PREFIX.length());
            }
        } else {
            assert id1.startsWith(ID_PREFIX) : id1;
            if (id2.startsWith(ID_PREFIX)) {
                return id1.equals(id2);
            } else {
                assert id2.startsWith(NEW_ID_PREFIX);
                return (id2.length() - id1.length()
                            == (NEW_ID_PREFIX.length() - ID_PREFIX.length()))
                        && id2.regionMatches(NEW_ID_PREFIX.length(), id1,
                                ID_PREFIX.length(),
                                id1.length() - ID_PREFIX.length());
            }
        }
    }

    /**
     * Computes the edit distance (number of insertions, deletions or substitutions
     * to edit one string into the other) between two strings. In particular,
     * this will compute the Levenshtein distance.
     * <p>
     * See http://en.wikipedia.org/wiki/Levenshtein_distance for details.
     *
     * @param s the first string to compare
     * @param t the second string to compare
     * @return the edit distance between the two strings
     */
    public static int editDistance(@NonNull String s, @NonNull String t) {
        int m = s.length();
        int n = t.length();
        int[][] d = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            d[0][j] = j;
        }
        for (int j = 1; j <= n; j++) {
            for (int i = 1; i <= m; i++) {
                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    d[i][j] = d[i - 1][j - 1];
                } else {
                    int deletion = d[i - 1][j] + 1;
                    int insertion = d[i][j - 1] + 1;
                    int substitution = d[i - 1][j - 1] + 1;
                    d[i][j] = Math.min(deletion, Math.min(insertion, substitution));
                }
            }
        }

        return d[m][n];
    }

    /**
     * Returns true if assertions are enabled
     *
     * @return true if assertions are enabled
     */
    @SuppressWarnings("all")
    public static boolean assertionsEnabled() {
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true; // Intentional side-effect
        return assertionsEnabled;
    }

    /**
     * Returns the layout resource name for the given layout file
     *
     * @param layoutFile the file pointing to the layout
     * @return the layout resource name, not including the {@code @layout}
     *         prefix
     */
    public static String getLayoutName(File layoutFile) {
        String name = layoutFile.getName();
        int dotIndex = name.indexOf('.');
        if (dotIndex != -1) {
            name = name.substring(0, dotIndex);
        }
        return name;
    }

    /**
     * Splits the given path into its individual parts, attempting to be
     * tolerant about path separators (: or ;). It can handle possibly ambiguous
     * paths, such as {@code c:\foo\bar:\other}, though of course these are to
     * be avoided if possible.
     *
     * @param path the path variable to split, which can use both : and ; as
     *            path separators.
     * @return the individual path components as an Iterable of strings
     */
    public static Iterable<String> splitPath(@NonNull String path) {
        if (path.indexOf(';') != -1) {
            return Splitter.on(';').omitEmptyStrings().trimResults().split(path);
        }

        List<String> combined = new ArrayList<String>();
        Iterables.addAll(combined, Splitter.on(':').omitEmptyStrings().trimResults().split(path));
        for (int i = 0, n = combined.size(); i < n; i++) {
            String p = combined.get(i);
            if (p.length() == 1 && i < n - 1 && Character.isLetter(p.charAt(0))
                    // Technically, Windows paths do not have to have a \ after the :,
                    // which means it would be using the current directory on that drive,
                    // but that's unlikely to be the case in a path since it would have
                    // unpredictable results
                    && !combined.get(i+1).isEmpty() && combined.get(i+1).charAt(0) == '\\') {
                combined.set(i, p + ':' + combined.get(i+1));
                combined.remove(i+1);
                n--;
                continue;
            }
        }

        return combined;
    }

    /**
     * Computes the shared parent among a set of files (which may be null).
     *
     * @param files the set of files to be checked
     * @return the closest common ancestor file, or null if none was found
     */
    @Nullable
    public static File getCommonParent(@NonNull List<File> files) {
        int fileCount = files.size();
        if (fileCount == 0) {
            return null;
        } else if (fileCount == 1) {
            return files.get(0);
        } else if (fileCount == 2) {
            return getCommonParent(files.get(0), files.get(1));
        } else {
            File common = files.get(0);
            for (int i = 1; i < fileCount; i++) {
                common = getCommonParent(common, files.get(i));
                if (common == null) {
                    return null;
                }
            }

            return common;
        }
    }

    /**
     * Computes the closest common parent path between two files.
     *
     * @param file1 the first file to be compared
     * @param file2 the second file to be compared
     * @return the closest common ancestor file, or null if the two files have
     *         no common parent
     */
    @Nullable
    public static File getCommonParent(@NonNull File file1, @NonNull File file2) {
        if (file1.equals(file2)) {
            return file1;
        } else if (file1.getPath().startsWith(file2.getPath())) {
            return file2;
        } else if (file2.getPath().startsWith(file1.getPath())) {
            return file1;
        } else {
            // Dumb and simple implementation
            File first = file1.getParentFile();
            while (first != null) {
                File second = file2.getParentFile();
                while (second != null) {
                    if (first.equals(second)) {
                        return first;
                    }
                    second = second.getParentFile();
                }

                first = first.getParentFile();
            }
        }
        return null;
    }

    private static final String UTF_16 = "UTF_16";               //$NON-NLS-1$
    private static final String UTF_16LE = "UTF_16LE";           //$NON-NLS-1$

    /**
     * Returns the encoded String for the given file. This is usually the
     * same as {@code Files.toString(file, Charsets.UTF8}, but if there's a UTF byte order mark
     * (for UTF8, UTF_16 or UTF_16LE), use that instead.
     *
     * @param client the client to use for I/O operations
     * @param file the file to read from
     * @return the string
     * @throws IOException if the file cannot be read properly
     */
    @NonNull
    public static String getEncodedString(
            @NonNull LintClient client,
            @NonNull File file) throws IOException {
        byte[] bytes = client.readBytes(file);
        if (endsWith(file.getName(), DOT_XML)) {
            return PositionXmlParser.getXmlString(bytes);
        }

        return getEncodedString(bytes);
    }

    /**
     * Returns the String corresponding to the given data. This is usually the
     * same as {@code new String(data)}, but if there's a UTF byte order mark
     * (for UTF8, UTF_16 or UTF_16LE), use that instead.
     * <p>
     * NOTE: For XML files, there is the additional complication that there
     * could be a {@code encoding=} attribute in the prologue. For those files,
     * use {@link PositionXmlParser#getXmlString(byte[])} instead.
     *
     * @param data the byte array to construct the string from
     * @return the string
     */
    @NonNull
    public static String getEncodedString(@Nullable byte[] data) {
        if (data == null) {
            return "";
        }

        int offset = 0;
        String defaultCharset = UTF_8;
        String charset = null;
        // Look for the byte order mark, to see if we need to remove bytes from
        // the input stream (and to determine whether files are big endian or little endian) etc
        // for files which do not specify the encoding.
        // See http://unicode.org/faq/utf_bom.html#BOM for more.
        if (data.length > 4) {
            if (data[0] == (byte)0xef && data[1] == (byte)0xbb && data[2] == (byte)0xbf) {
                // UTF-8
                defaultCharset = charset = UTF_8;
                offset += 3;
            } else if (data[0] == (byte)0xfe && data[1] == (byte)0xff) {
                //  UTF-16, big-endian
                defaultCharset = charset = UTF_16;
                offset += 2;
            } else if (data[0] == (byte)0x0 && data[1] == (byte)0x0
                    && data[2] == (byte)0xfe && data[3] == (byte)0xff) {
                // UTF-32, big-endian
                defaultCharset = charset = "UTF_32";    //$NON-NLS-1$
                offset += 4;
            } else if (data[0] == (byte)0xff && data[1] == (byte)0xfe
                    && data[2] == (byte)0x0 && data[3] == (byte)0x0) {
                // UTF-32, little-endian. We must check for this *before* looking for
                // UTF_16LE since UTF_32LE has the same prefix!
                defaultCharset = charset = "UTF_32LE";  //$NON-NLS-1$
                offset += 4;
            } else if (data[0] == (byte)0xff && data[1] == (byte)0xfe) {
                //  UTF-16, little-endian
                defaultCharset = charset = UTF_16LE;
                offset += 2;
            }
        }
        int length = data.length - offset;

        // Guess encoding by searching for an encoding= entry in the first line.
        boolean seenOddZero = false;
        boolean seenEvenZero = false;
        for (int lineEnd = offset; lineEnd < data.length; lineEnd++) {
            if (data[lineEnd] == 0) {
                if ((lineEnd - offset) % 2 == 0) {
                    seenEvenZero = true;
                } else {
                    seenOddZero = true;
                }
            } else if (data[lineEnd] == '\n' || data[lineEnd] == '\r') {
                break;
            }
        }

        if (charset == null) {
            charset = seenOddZero ? UTF_16LE : seenEvenZero ? UTF_16 : UTF_8;
        }

        String text = null;
        try {
            text = new String(data, offset, length, charset);
        } catch (UnsupportedEncodingException e) {
            try {
                if (!charset.equals(defaultCharset)) {
                    text = new String(data, offset, length, defaultCharset);
                }
            } catch (UnsupportedEncodingException u) {
                // Just use the default encoding below
            }
        }
        if (text == null) {
            text = new String(data, offset, length);
        }
        return text;
    }

    /**
     * Returns true if the given class node represents a static inner class.
     *
     * @param classNode the inner class to be checked
     * @return true if the class node represents an inner class that is static
     */
    public static boolean isStaticInnerClass(@NonNull ClassNode classNode) {
        // Note: We can't just filter out static inner classes like this:
        //     (classNode.access & Opcodes.ACC_STATIC) != 0
        // because the static flag only appears on methods and fields in the class
        // file. Instead, look for the synthetic this pointer.

        @SuppressWarnings("rawtypes") // ASM API
        List fieldList = classNode.fields;
        for (Object f : fieldList) {
            FieldNode field = (FieldNode) f;
            if (field.name.startsWith("this$") && (field.access & Opcodes.ACC_SYNTHETIC) != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if the given class node represents an anonymous inner class
     *
     * @param classNode the class to be checked
     * @return true if the class appears to be an anonymous class
     */
    public static boolean isAnonymousClass(@NonNull ClassNode classNode) {
        if (classNode.outerClass == null) {
            return false;
        }

        String name = classNode.name;
        int index = name.lastIndexOf('$');
        if (index == -1 || index == name.length() - 1) {
            return false;
        }

        return Character.isDigit(name.charAt(index + 1));
    }

    /**
     * Returns the previous opcode prior to the given node, ignoring label and
     * line number nodes
     *
     * @param node the node to look up the previous opcode for
     * @return the previous opcode, or {@link Opcodes#NOP} if no previous node
     *         was found
     */
    public static int getPrevOpcode(@NonNull AbstractInsnNode node) {
        AbstractInsnNode prev = getPrevInstruction(node);
        if (prev != null) {
            return prev.getOpcode();
        } else {
            return Opcodes.NOP;
        }
    }

    /**
     * Returns the previous instruction prior to the given node, ignoring label
     * and line number nodes.
     *
     * @param node the node to look up the previous instruction for
     * @return the previous instruction, or null if no previous node was found
     */
    @Nullable
    public static AbstractInsnNode getPrevInstruction(@NonNull AbstractInsnNode node) {
        AbstractInsnNode prev = node;
        while (true) {
            prev = prev.getPrevious();
            if (prev == null) {
                return null;
            } else {
                int type = prev.getType();
                if (type != AbstractInsnNode.LINE && type != AbstractInsnNode.LABEL
                        && type != AbstractInsnNode.FRAME) {
                    return prev;
                }
            }
        }
    }

    /**
     * Returns the next opcode after to the given node, ignoring label and line
     * number nodes
     *
     * @param node the node to look up the next opcode for
     * @return the next opcode, or {@link Opcodes#NOP} if no next node was found
     */
    public static int getNextOpcode(@NonNull AbstractInsnNode node) {
        AbstractInsnNode next = getNextInstruction(node);
        if (next != null) {
            return next.getOpcode();
        } else {
            return Opcodes.NOP;
        }
    }

    /**
     * Returns the next instruction after to the given node, ignoring label and
     * line number nodes.
     *
     * @param node the node to look up the next node for
     * @return the next instruction, or null if no next node was found
     */
    @Nullable
    public static AbstractInsnNode getNextInstruction(@NonNull AbstractInsnNode node) {
        AbstractInsnNode next = node;
        while (true) {
            next = next.getNext();
            if (next == null) {
                return null;
            } else {
                int type = next.getType();
                if (type != AbstractInsnNode.LINE && type != AbstractInsnNode.LABEL
                        && type != AbstractInsnNode.FRAME) {
                    return next;
                }
            }
        }
    }

    /**
     * Returns true if the given directory is a lint manifest file directory.
     *
     * @param dir the directory to check
     * @return true if the directory contains a manifest file
     */
    public static boolean isManifestFolder(File dir) {
        boolean hasManifest = new File(dir, ANDROID_MANIFEST_XML).exists();
        if (hasManifest) {
            // Special case: the bin/ folder can also contain a copy of the
            // manifest file, but this is *not* a project directory
            if (dir.getName().equals(BIN_FOLDER)) {
                // ...unless of course it just *happens* to be a project named bin, in
                // which case we peek at its parent to see if this is the case
                dir = dir.getParentFile();
                //noinspection ConstantConditions
                if (dir != null && isManifestFolder(dir)) {
                    // Yes, it's a bin/ directory inside a real project: ignore this dir
                    return false;
                }
            }
        }

        return hasManifest;
    }

    /**
     * Look up the locale and region from the given parent folder name and
     * return it as a combined string, such as "en", "en-rUS", b+eng-US, etc, or null if
     * no language is specified.
     *
     * @param folderName the folder name
     * @return the locale+region string or null
     */
    @Nullable
    public static String getLocaleAndRegion(@NonNull String folderName) {
        if (folderName.indexOf('-') == -1) {
            return null;
        }

        String locale = null;

        for (String qualifier : QUALIFIER_SPLITTER.split(folderName)) {
            int qualifierLength = qualifier.length();
            if (qualifierLength == 2) {
                char first = qualifier.charAt(0);
                char second = qualifier.charAt(1);
                if (first >= 'a' && first <= 'z' && second >= 'a' && second <= 'z') {
                    locale = qualifier;
                }
            } else if (qualifierLength == 3 && qualifier.charAt(0) == 'r' && locale != null) {
                char first = qualifier.charAt(1);
                char second = qualifier.charAt(2);
                if (first >= 'A' && first <= 'Z' && second >= 'A' && second <= 'Z') {
                    return locale + '-' + qualifier;
                }
                break;
            } else if (qualifier.startsWith(BCP_47_PREFIX)) {
                return qualifier;
            }
        }

        return locale;
    }

    /**
     * Returns true if the given class (specified by a fully qualified class
     * name) name is imported in the given compilation unit either through a fully qualified
     * import or by a wildcard import.
     *
     * @param compilationUnit the compilation unit
     * @param fullyQualifiedName the fully qualified class name
     * @return true if the given imported name refers to the given fully
     *         qualified name
     * @deprecated Use PSI element hierarchies instead where type resolution is more directly
     *  available (call {@link PsiImportStatement#resolve()})
     */
    @Deprecated
    public static boolean isImported(
            @Nullable lombok.ast.Node compilationUnit,
            @NonNull String fullyQualifiedName) {
        if (compilationUnit == null) {
            return false;
        }
        int dotIndex = fullyQualifiedName.lastIndexOf('.');
        int dotLength = fullyQualifiedName.length() - dotIndex;

        boolean imported = false;
        for (lombok.ast.Node rootNode : compilationUnit.getChildren()) {
            if (rootNode instanceof ImportDeclaration) {
                ImportDeclaration importDeclaration = (ImportDeclaration) rootNode;
                String fqn = importDeclaration.asFullyQualifiedName();
                if (fqn.equals(fullyQualifiedName)) {
                    return true;
                } else if (fullyQualifiedName.regionMatches(dotIndex, fqn,
                        fqn.length() - dotLength, dotLength)) {
                    // This import is importing the class name using some other prefix, so there
                    // fully qualified class name cannot be imported under that name
                    return false;
                } else if (importDeclaration.astStarImport()
                        && fqn.regionMatches(0, fqn, 0, dotIndex + 1)) {
                    imported = true;
                    // but don't break -- keep searching in case there's a non-wildcard
                    // import of the specific class name, e.g. if we're looking for
                    // android.content.SharedPreferences.Editor, don't match on the following:
                    //   import android.content.SharedPreferences.*;
                    //   import foo.bar.Editor;
                }
            }
        }

        return imported;
    }

    /**
     * Looks up the resource values for the given attribute given a style. Note that
     * this only looks project-level style values, it does not resume into the framework
     * styles.
     */
    @Nullable
    public static List<ResourceValue> getStyleAttributes(
            @NonNull Project project, @NonNull LintClient client,
            @NonNull String styleUrl, @NonNull String namespace, @NonNull String attribute) {
        if (!client.supportsProjectResources()) {
            return null;
        }

        AbstractResourceRepository resources = client.getProjectResources(project, true);
        if (resources == null) {
            return null;
        }

        ResourceUrl style = ResourceUrl.parse(styleUrl);
        if (style == null || style.framework) {
            return null;
        }

        List<ResourceValue> result = null;

        Queue<ResourceValue> queue = new ArrayDeque<ResourceValue>();
        queue.add(new ResourceValue(ResourceUrl.create(style.type, style.name, false), null));
        Set<String> seen = Sets.newHashSet();
        int count = 0;
        boolean isFrameworkAttribute = ANDROID_URI.equals(namespace);
        while (count < 30 && !queue.isEmpty()) {
            ResourceValue front = queue.remove();
            String name = front.getName();
            seen.add(name);
            List<ResourceItem> items = resources.getResourceItem(front.getResourceType(), name);
            if (items != null) {
                for (ResourceItem item : items) {
                    ResourceValue rv = item.getResourceValue(false);
                    if (rv instanceof StyleResourceValue) {
                        StyleResourceValue srv = (StyleResourceValue) rv;
                        ItemResourceValue value = srv.getItem(attribute, isFrameworkAttribute);
                        if (value != null) {
                            if (result == null) {
                                result = Lists.newArrayList();
                            }
                            if (!result.contains(value)) {
                                result.add(value);
                            }
                        }

                        String parent = srv.getParentStyle();
                        if (parent != null && !parent.startsWith(ANDROID_PREFIX)) {
                            ResourceUrl p = ResourceUrl.parse(parent);
                            if (p != null && !p.framework && !seen.contains(p.name)) {
                                seen.add(p.name);
                                queue.add(new ResourceValue(ResourceUrl.create(ResourceType.STYLE, p.name,
                                        false), null));
                            }
                        }

                        int index = name.lastIndexOf('.');
                        if (index > 0) {
                            String parentName = name.substring(0, index);
                            if (!seen.contains(parentName)) {
                                seen.add(parentName);
                                queue.add(new ResourceValue(ResourceUrl.create(ResourceType.STYLE, parentName,
                                        false), null));
                            }
                        }
                    }
                }
            }

            count++;
        }

        return result;
    }

    @Nullable
    public static List<StyleResourceValue> getInheritedStyles(
            @NonNull Project project, @NonNull LintClient client,
            @NonNull String styleUrl) {
        if (!client.supportsProjectResources()) {
            return null;
        }

        AbstractResourceRepository resources = client.getProjectResources(project, true);
        if (resources == null) {
            return null;
        }

        ResourceUrl style = ResourceUrl.parse(styleUrl);
        if (style == null || style.framework) {
            return null;
        }

        List<StyleResourceValue> result = null;

        Queue<ResourceValue> queue = new ArrayDeque<ResourceValue>();
        queue.add(new ResourceValue(ResourceUrl.create(style.type, style.name, false), null));
        Set<String> seen = Sets.newHashSet();
        int count = 0;
        while (count < 30 && !queue.isEmpty()) {
            ResourceValue front = queue.remove();
            String name = front.getName();
            seen.add(name);
            List<ResourceItem> items = resources.getResourceItem(front.getResourceType(), name);
            if (items != null) {
                for (ResourceItem item : items) {
                    ResourceValue rv = item.getResourceValue(false);
                    if (rv instanceof StyleResourceValue) {
                        StyleResourceValue srv = (StyleResourceValue) rv;
                        if (result == null) {
                            result = Lists.newArrayList();
                        }
                        result.add(srv);

                        String parent = srv.getParentStyle();
                        if (parent != null && !parent.startsWith(ANDROID_PREFIX)) {
                            ResourceUrl p = ResourceUrl.parse(parent);
                            if (p != null && !p.framework && !seen.contains(p.name)) {
                                seen.add(p.name);
                                queue.add(new ResourceValue(ResourceUrl.create(ResourceType.STYLE, p.name,
                                        false), null));
                            }
                        }

                        int index = name.lastIndexOf('.');
                        if (index > 0) {
                            String parentName = name.substring(0, index);
                            if (!seen.contains(parentName)) {
                                seen.add(parentName);
                                queue.add(new ResourceValue(ResourceUrl.create(ResourceType.STYLE, parentName,
                                        false), null));
                            }
                        }
                    }
                }
            }

            count++;
        }

        return result;
    }

    /** Returns true if the given two paths point to the same logical resource file within
     * a source set. This means that it only checks the parent folder name and individual
     * file name, not the path outside the parent folder.
     *
     * @param file1 the first file to compare
     * @param file2 the second file to compare
     * @return true if the two files have the same parent and file names
     */
    public static boolean isSameResourceFile(@Nullable File file1, @Nullable File file2) {
        if (file1 != null && file2 != null
                && file1.getName().equals(file2.getName())) {
            File parent1 = file1.getParentFile();
            File parent2 = file2.getParentFile();
            if (parent1 != null && parent2 != null &&
                    parent1.getName().equals(parent2.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Whether we should attempt to look up the prefix from the model. Set to false
     * if we encounter a model which is too old.
     * <p>
     * This is public such that code which for example syncs to a new gradle model
     * can reset it.
     */
    public static boolean sTryPrefixLookup = true;

    /** Looks up the resource prefix for the given Gradle project, if possible */
    @Nullable
    public static String computeResourcePrefix(@Nullable AndroidProject project) {
        try {
            if (sTryPrefixLookup && project != null) {
                return project.getResourcePrefix();
            }
        } catch (Exception e) {
            // This happens if we're talking to an older model than 0.10
            // Ignore; fall through to normal handling and never try again.
            //noinspection AssignmentToStaticFieldFromInstanceMethod
            sTryPrefixLookup = false;
        }

        return null;
    }

    /** Computes a suggested name given a resource prefix and resource name */
    public static String computeResourceName(@NonNull String prefix, @NonNull String name) {
        if (prefix.isEmpty()) {
            return name;
        } else if (name.isEmpty()) {
            return prefix;
        } else if (prefix.endsWith("_")) {
            return prefix + name;
        } else {
            return prefix + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }


    /**
     * Convert an {@link com.android.builder.model.ApiVersion} to a {@link
     * com.android.sdklib.AndroidVersion}. The chief problem here is that the {@link
     * com.android.builder.model.ApiVersion}, when using a codename, will not encode the
     * corresponding API level (it just reflects the string entered by the user in the gradle file)
     * so we perform a search here (since lint really wants to know the actual numeric API level)
     *
     * @param api     the api version to convert
     * @param targets if known, the installed targets (used to resolve platform codenames, only
     *                needed to resolve platforms newer than the tools since {@link
     *                com.android.sdklib.SdkVersionInfo} knows the rest)
     * @return the corresponding version
     */
    @NonNull
    public static AndroidVersion convertVersion(
            @NonNull ApiVersion api,
            @Nullable IAndroidTarget[] targets) {
        String codename = api.getCodename();
        if (codename != null) {
            AndroidVersion version = SdkVersionInfo.getVersion(codename, targets);
            if (version != null) {
                return version;
            }
            return new AndroidVersion(api.getApiLevel(), codename);
        }
        return new AndroidVersion(api.getApiLevel(), null);
    }
    
    /**
     * Looks for a certain string within a larger string, which should immediately follow
     * the given prefix and immediately precede the given suffix.
     *
     * @param string the full string to search
     * @param prefix the optional prefix to follow
     * @param suffix the optional suffix to precede
     * @return the corresponding substring, if present
     */
    @Nullable
    public static String findSubstring(@NonNull String string, @Nullable String prefix,
            @Nullable String suffix) {
        int start = 0;
        if (prefix != null) {
            start = string.indexOf(prefix);
            if (start == -1) {
                return null;
            }
            start += prefix.length();
        }

        if (suffix != null) {
            int end = string.indexOf(suffix, start);
            if (end == -1) {
                return null;
            }
            return string.substring(start, end);
        }

        return string.substring(start);
    }

    /**
     * Splits up the given message coming from a given string format (where the string
     * format follows the very specific convention of having only strings formatted exactly
     * with the format %n$s where n is between 1 and 9 inclusive, and each formatting parameter
     * appears exactly once, and in increasing order.
     *
     * @param format the format string responsible for creating the error message
     * @param errorMessage an error message formatted with the format string
     * @return the specific values inserted into the format
     */
    @NonNull
    public static List<String> getFormattedParameters(
            @NonNull String format,
            @NonNull String errorMessage) {
        StringBuilder pattern = new StringBuilder(format.length());
        int parameter = 1;
        for (int i = 0, n = format.length(); i < n; i++) {
            char c = format.charAt(i);
            if (c == '%') {
                // Only support formats of the form %n$s where n is 1 <= n <=9
                assert i < format.length() - 4 : format;
                assert format.charAt(i + 1) == ('0' + parameter) : format;
                assert Character.isDigit(format.charAt(i + 1)) : format;
                assert format.charAt(i + 2) == '$' : format;
                assert format.charAt(i + 3) == 's' : format;
                parameter++;
                i += 3;
                pattern.append("(.*)");
            } else {
                pattern.append(c);
            }
        }
        try {
            Pattern compile = Pattern.compile(pattern.toString());
            Matcher matcher = compile.matcher(errorMessage);
            if (matcher.find()) {
                int groupCount = matcher.groupCount();
                List<String> parameters = Lists.newArrayListWithExpectedSize(groupCount);
                for (int i = 1; i <= groupCount; i++) {
                    parameters.add(matcher.group(i));
                }

                return parameters;
            }

        } catch (PatternSyntaxException pse) {
            // Internal error: string format is not valid. Should be caught by unit tests
            // as a failure to return the formatted parameters.
        }
        return Collections.emptyList();
    }

    /**
     * Returns the locale for the given parent folder.
     *
     * @param parent the name of the parent folder
     * @return null if the locale is not known, or a locale qualifier providing the language
     *    and possibly region
     */
    @Nullable
    public static LocaleQualifier getLocale(@NonNull String parent) {
        if (parent.indexOf('-') != -1) {
            FolderConfiguration config = FolderConfiguration.getConfigForFolder(parent);
            if (config != null) {
                return config.getLocaleQualifier();
            }
        }
        return null;
    }

    /**
     * Returns the locale for the given context.
     *
     * @param context the context to look up the locale for
     * @return null if the locale is not known, or a locale qualifier providing the language
     *    and possibly region
     */
    @Nullable
    public static LocaleQualifier getLocale(@NonNull XmlContext context) {
        Element root = context.document.getDocumentElement();
        if (root != null) {
            String locale = root.getAttributeNS(TOOLS_URI, ATTR_LOCALE);
            if (locale != null && !locale.isEmpty()) {
                return getLocale(locale);
            }
        }

        return getLocale(context.file.getParentFile().getName());
    }

    /**
     * Check whether the given resource file is in an English locale
     * @param context the XML context for the resource file
     * @param assumeForBase whether the base folder (e.g. no locale specified) should be
     *                      treated as English
     */
    public static boolean isEnglishResource(@NonNull XmlContext context, boolean assumeForBase) {
        LocaleQualifier locale = LintUtils.getLocale(context);
        if (locale == null) {
            return assumeForBase;
        } else {
            return "en".equals(locale.getLanguage());  //$NON-NLS-1$
        }
    }

    /**
     * Create a {@link Location} for an error in the top level build.gradle file.
     * This is necessary when we're doing an analysis based on the Gradle interpreted model,
     * not from parsing Gradle files - and the model doesn't provide source positions.
     * @param project the project containing the gradle file being analyzed
     * @return location for the top level gradle file if it exists, otherwise fall back to
     *     the project directory.
     */
    public static Location guessGradleLocation(@NonNull Project project) {
        File dir = project.getDir();
        Location location;
        File topLevel = new File(dir, FN_BUILD_GRADLE);
        if (topLevel.exists()) {
            location = Location.create(topLevel);
        } else {
            location = Location.create(dir);
        }
        return location;
    }

    /**
     * Returns true if the given element is the null literal
     *
     * @param element the element to check
     * @return true if the element is "null"
     */
    public static boolean isNullLiteral(@Nullable PsiElement element) {
        return element instanceof PsiLiteral && "null".equals(element.getText());
    }

    public static boolean isTrueLiteral(@Nullable PsiElement element) {
        return element instanceof PsiLiteral && "true".equals(element.getText());
    }

    public static boolean isFalseLiteral(@Nullable PsiElement element) {
        return element instanceof PsiLiteral && "false".equals(element.getText());
    }

    @Nullable
    public static PsiElement skipParentheses(@Nullable PsiElement element) {
        while (element instanceof PsiParenthesizedExpression) {
            element = element.getParent();
        }

        return element;
    }

    @Nullable
    public static UElement skipParentheses(@Nullable UElement element) {
        while (element instanceof UParenthesizedExpression) {
            element = element.getUastParent();
        }

        return element;
    }

    @Nullable
    public static PsiElement nextNonWhitespace(@Nullable PsiElement element) {
        if (element != null) {
            element = element.getNextSibling();
            while (element instanceof PsiWhiteSpace) {
                element = element.getNextSibling();
            }
        }

        return element;
    }

    @Nullable
    public static PsiElement prevNonWhitespace(@Nullable PsiElement element) {
        if (element != null) {
            element = element.getPrevSibling();
            while (element instanceof PsiWhiteSpace) {
                element = element.getPrevSibling();
            }
        }

        return element;
    }

    public static boolean isString(@NonNull PsiType type) {
        if (type instanceof PsiClassType) {
            final String shortName = ((PsiClassType)type).getClassName();
            if (!Objects.equal(shortName, CommonClassNames.JAVA_LANG_STRING_SHORT)) {
                return false;
            }
        }
        return CommonClassNames.JAVA_LANG_STRING.equals(type.getCanonicalText());
    }

    @Nullable
    public static String getAutoBoxedType(@NonNull String primitive) {
        if (TYPE_INT.equals(primitive)) {
            return TYPE_INTEGER_WRAPPER;
        } else if (TYPE_LONG.equals(primitive)) {
            return TYPE_LONG_WRAPPER;
        } else if (TYPE_CHAR.equals(primitive)) {
            return TYPE_CHARACTER_WRAPPER;
        } else if (TYPE_FLOAT.equals(primitive)) {
            return TYPE_FLOAT_WRAPPER;
        } else if (TYPE_DOUBLE.equals(primitive)) {
            return TYPE_DOUBLE_WRAPPER;
        } else if (TYPE_BOOLEAN.equals(primitive)) {
            return TYPE_BOOLEAN_WRAPPER;
        } else if (TYPE_SHORT.equals(primitive)) {
            return TYPE_SHORT_WRAPPER;
        } else if (TYPE_BYTE.equals(primitive)) {
            return TYPE_BYTE_WRAPPER;
        }

        return null;
    }

    @Nullable
    public static String getPrimitiveType(@NonNull String autoBoxedType) {
        if (TYPE_INTEGER_WRAPPER.equals(autoBoxedType)) {
            return TYPE_INT;
        } else if (TYPE_LONG_WRAPPER.equals(autoBoxedType)) {
            return TYPE_LONG;
        } else if (TYPE_CHARACTER_WRAPPER.equals(autoBoxedType)) {
            return TYPE_CHAR;
        } else if (TYPE_FLOAT_WRAPPER.equals(autoBoxedType)) {
            return TYPE_FLOAT;
        } else if (TYPE_DOUBLE_WRAPPER.equals(autoBoxedType)) {
            return TYPE_DOUBLE;
        } else if (TYPE_BOOLEAN_WRAPPER.equals(autoBoxedType)) {
            return TYPE_BOOLEAN;
        } else if (TYPE_SHORT_WRAPPER.equals(autoBoxedType)) {
            return TYPE_SHORT;
        } else if (TYPE_BYTE_WRAPPER.equals(autoBoxedType)) {
            return TYPE_BYTE;
        }

        return null;
    }
}
