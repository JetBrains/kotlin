/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.fileEditor.impl;

import com.intellij.lang.properties.charset.Native2AsciiCharset;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.BinaryFileDecompiler;
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingRegistry;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

public final class LoadTextUtil {
    private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.fileEditor.impl.LoadTextUtil");
    private static final Key<String> DETECTED_LINE_SEPARATOR_KEY = Key.create("DETECTED_LINE_SEPARATOR_KEY");

    private LoadTextUtil() {
    }

    @NotNull
    private static Pair<CharSequence, String> convertLineSeparators(@NotNull CharBuffer buffer) {
        int dst = 0;
        char prev = ' ';
        int crCount = 0;
        int lfCount = 0;
        int crlfCount = 0;

        final int length = buffer.length();
        final char[] bufferArray = CharArrayUtil.fromSequenceWithoutCopying(buffer);

        for (int src = 0; src < length; src++) {
            char c = bufferArray != null ? bufferArray[src] : buffer.charAt(src);
            switch (c) {
                case '\r':
                    buffer.put(dst++, '\n');
                    crCount++;
                    break;
                case '\n':
                    if (prev == '\r') {
                        crCount--;
                        crlfCount++;
                    } else {
                        buffer.put(dst++, '\n');
                        lfCount++;
                    }
                    break;
                default:
                    buffer.put(dst++, c);
                    break;
            }
            prev = c;
        }

        String detectedLineSeparator = null;
        if (crlfCount > crCount && crlfCount > lfCount) {
            detectedLineSeparator = "\r\n";
        } else if (crCount > lfCount) {
            detectedLineSeparator = "\r";
        } else if (lfCount > 0) {
            detectedLineSeparator = "\n";
        }

        CharSequence result = buffer.length() == dst ? buffer : buffer.subSequence(0, dst);
        return Pair.create(result, detectedLineSeparator);
    }

    private static Charset detectCharset(@NotNull VirtualFile virtualFile, @NotNull byte[] content) {
        if (virtualFile.isCharsetSet()) return virtualFile.getCharset();

        Charset charset = doDetectCharset(virtualFile, content);
        charset = charset == null ? EncodingRegistry.getInstance().getDefaultCharset() : charset;
        if (EncodingRegistry.getInstance().isNative2Ascii(virtualFile)) {
            charset = Native2AsciiCharset.wrap(charset);
        }
        virtualFile.setCharset(charset);
        return charset;
    }

    @NotNull
    public static Charset detectCharsetAndSetBOM(@NotNull VirtualFile virtualFile, @NotNull byte[] content) {
        return doDetectCharsetAndSetBOM(virtualFile, content).getFirst();
    }

    @NotNull
    private static Pair<Charset, byte[]> doDetectCharsetAndSetBOM(@NotNull VirtualFile virtualFile, @NotNull byte[] content) {
        Charset charset = detectCharset(virtualFile, content);
        Pair<Charset, byte[]> bomAndCharset = getBOMAndCharset(content, charset);
        final byte[] bom = bomAndCharset.second;
        if (bom.length != 0) {
            virtualFile.setBOM(bom);
        }
        return bomAndCharset;
    }

    private static Charset doDetectCharset(@NotNull VirtualFile virtualFile, @NotNull byte[] content) {
        Trinity<Charset, CharsetToolkit.GuessedEncoding, byte[]> guessed = guessFromContent(virtualFile, content, content.length);
        if (guessed != null && guessed.first != null) return guessed.first;

        FileType fileType = virtualFile.getFileType();
        String charsetName = fileType.getCharset(virtualFile, content);

        if (charsetName == null) {
            Charset saved = EncodingRegistry.getInstance().getEncoding(virtualFile, true);
            if (saved != null) return saved;
        }
        return CharsetToolkit.forName(charsetName);
    }

    @Nullable("null means no luck, otherwise it's tuple(guessed encoding, hint about content if was unable to guess, BOM)")
    public static Trinity<Charset, CharsetToolkit.GuessedEncoding, byte[]> guessFromContent(VirtualFile virtualFile, byte[] content, int length) {
        EncodingRegistry settings = EncodingRegistry.getInstance();
        boolean shouldGuess = settings != null && settings.isUseUTFGuessing(virtualFile);
        CharsetToolkit toolkit = shouldGuess ? new CharsetToolkit(content, EncodingRegistry.getInstance().getDefaultCharset()) : null;
        setCharsetWasDetectedFromBytes(virtualFile, false);
        if (shouldGuess) {
            toolkit.setEnforce8Bit(true);
            Charset charset = toolkit.guessFromBOM();
            if (charset != null) {
                setCharsetWasDetectedFromBytes(virtualFile, true);
                byte[] bom = CharsetToolkit.getBom(charset);
                if (bom == null) bom = CharsetToolkit.UTF8_BOM;
                return Trinity.create(charset, null, bom);
            }
            CharsetToolkit.GuessedEncoding guessed = toolkit.guessFromContent(length);
            if (guessed == CharsetToolkit.GuessedEncoding.VALID_UTF8) {
                setCharsetWasDetectedFromBytes(virtualFile, true);
                return Trinity.create(CharsetToolkit.UTF8_CHARSET, null, null); //UTF detected, ignore all directives
            }
            return Trinity.create(null, guessed, null);
        }
        return null;
    }

    @NotNull
    private static Pair<Charset, byte[]> getBOMAndCharset(@NotNull byte[] content, final Charset charset) {
        if (charset != null && charset.name().contains(CharsetToolkit.UTF8) && CharsetToolkit.hasUTF8Bom(content)) {
            return Pair.create(charset, CharsetToolkit.UTF8_BOM);
        }
        try {
            if (CharsetToolkit.hasUTF16LEBom(content)) {
                return Pair.create(CharsetToolkit.UTF_16LE_CHARSET, CharsetToolkit.UTF16LE_BOM);
            }
            if (CharsetToolkit.hasUTF16BEBom(content)) {
                return Pair.create(CharsetToolkit.UTF_16BE_CHARSET, CharsetToolkit.UTF16BE_BOM);
            }
        } catch (UnsupportedCharsetException ignore) {
        }
        if (charset == null) {
            return Pair.create(Charset.defaultCharset(), CharsetToolkit.UTF8_BOM);
        }
        return Pair.create(charset, ArrayUtil.EMPTY_BYTE_ARRAY);
    }

    /**
     * Gets the <code>Writer</code> for this file and sets modification stamp and time stamp to the specified values
     * after closing the Writer.<p>
     * <p/>
     * Normally you should not use this method.
     *
     * @param project
     * @param virtualFile
     * @param requestor            any object to control who called this method. Note that
     *                             it is considered to be an external change if <code>requestor</code> is <code>null</code>.
     *                             See {@link com.intellij.openapi.vfs.VirtualFileEvent#getRequestor}
     * @param text
     * @param newModificationStamp new modification stamp or -1 if no special value should be set @return <code>Writer</code>
     * @throws java.io.IOException if an I/O error occurs
     * @see com.intellij.openapi.vfs.VirtualFile#getModificationStamp()
     */
    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    public static Writer getWriter(@Nullable Project project, @NotNull VirtualFile virtualFile, Object requestor, @NotNull String text, final long newModificationStamp)
            throws IOException {
        Charset existing = virtualFile.getCharset();
        Charset specified = extractCharsetFromFileContent(project, virtualFile, text);
        Charset charset = chooseMostlyHarmlessCharset(existing, specified, text);
        if (charset != null) {
            if (!charset.equals(existing)) {
                virtualFile.setCharset(charset);
            }
            setDetectedFromBytesFlagBack(virtualFile, charset, text);
        }

        // in c ase of "UTF-16", OutputStreamWriter sometimes adds BOM on it's own.
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6800103
        byte[] bom = virtualFile.getBOM();
        Charset fromBom = bom == null ? null : CharsetToolkit.guessFromBOM(bom);
        if (fromBom != null) charset = fromBom;

        OutputStream outputStream = virtualFile.getOutputStream(requestor, newModificationStamp, -1);
        OutputStreamWriter writer = charset == null ? new OutputStreamWriter(outputStream) : new OutputStreamWriter(outputStream, charset);
        // no need to buffer ByteArrayOutputStream
        return outputStream instanceof ByteArrayOutputStream ? writer : new BufferedWriter(writer);
    }

    private static void setDetectedFromBytesFlagBack(@NotNull VirtualFile virtualFile, @NotNull Charset charset, @NotNull String text) {
        if (virtualFile.getBOM() != null) {
            // prevent file to be reloaded in other encoding after save with BOM
            setCharsetWasDetectedFromBytes(virtualFile, true);
            return;
        }

        byte[] content = text.getBytes(charset);
        CharsetToolkit.GuessedEncoding guessedEncoding = new CharsetToolkit(content).guessFromContent(content.length);
        if (guessedEncoding == CharsetToolkit.GuessedEncoding.VALID_UTF8) {
            setCharsetWasDetectedFromBytes(virtualFile, true);
        }
    }

    private static Charset chooseMostlyHarmlessCharset(Charset existing, Charset specified, String text) {
        if (existing == null) return specified;
        if (specified == null) return existing;
        if (specified.equals(existing)) return specified;
        if (isSupported(specified, text)) return specified; //if explicitly specified encoding is safe, return it
        if (isSupported(existing, text)) return existing;   //otherwise stick to the old encoding if it's ok
        return specified;                                   //if both are bad there is no difference
    }

    private static boolean isSupported(@NotNull Charset charset, @NotNull String str) {
        if (!charset.canEncode()) return false;
        ByteBuffer out = charset.encode(str);
        CharBuffer buffer = charset.decode(out);
        return str.equals(buffer.toString());
    }

    public static Charset extractCharsetFromFileContent(@Nullable Project project, @NotNull VirtualFile virtualFile, @NotNull String text) {
        Charset charset = charsetFromContentOrNull(project, virtualFile, text);
        if (charset == null) charset = virtualFile.getCharset();
        return charset;
    }

    @Nullable("returns null if cannot determine from content")
    public static Charset charsetFromContentOrNull(@Nullable Project project, @NotNull VirtualFile virtualFile, @NotNull String text) {
        FileType fileType = virtualFile.getFileType();
        if (fileType instanceof LanguageFileType) {
            return ((LanguageFileType) fileType).extractCharsetFromFileContent(project, virtualFile, text);
        }
        return null;
    }

    public static CharSequence loadText(@NotNull VirtualFile file) {
        if (file instanceof LightVirtualFile) {
            CharSequence content = ((LightVirtualFile) file).getContent();
            if (StringUtil.indexOf(content, '\r') == -1) return content;

            CharBuffer buffer = CharBuffer.allocate(content.length());
            buffer.append(content);
            buffer.rewind();
            return convertLineSeparators(buffer).first;
        }

        assert !file.isDirectory() : "'" + file.getPresentableUrl() + "' is directory";
        final FileType fileType = file.getFileType();

        if (fileType.isBinary()) {
            final BinaryFileDecompiler decompiler = BinaryFileTypeDecompilers.INSTANCE.forFileType(fileType);
            if (decompiler != null) {
                CharSequence text = decompiler.decompile(file);
                StringUtil.assertValidSeparators(text);
                return text;
            }

            throw new IllegalArgumentException("Attempt to load text for binary file, that doesn't have decompiler plugged in: " + file.getPresentableUrl());
        }

        try {
            byte[] bytes = file.contentsToByteArray();
            return getTextByBinaryPresentation(bytes, file);
        } catch (IOException e) {
            return ArrayUtil.EMPTY_CHAR_SEQUENCE;
        }
    }

    @NotNull
    public static CharSequence getTextByBinaryPresentation(@NotNull final byte[] bytes, @NotNull VirtualFile virtualFile) {
        return getTextByBinaryPresentation(bytes, virtualFile, true);
    }

    @NotNull
    public static CharSequence getTextByBinaryPresentation(@NotNull byte[] bytes, @NotNull VirtualFile virtualFile, final boolean rememberDetectedSeparators) {
        Pair<Charset, byte[]> pair = doDetectCharsetAndSetBOM(virtualFile, bytes);
        final Charset charset = pair.getFirst();
        byte[] bom = pair.getSecond();
        int offset = bom == null ? 0 : bom.length;

        final Pair<CharSequence, String> result = convertBytes(bytes, charset, offset);
        if (rememberDetectedSeparators) {
            virtualFile.putUserData(DETECTED_LINE_SEPARATOR_KEY, result.getSecond());
        }
        return result.getFirst();
    }

    /**
     * Get detected line separator, if the file never been loaded, is loaded if checkFile parameter is specified.
     *
     * @param file      the file to check
     * @param checkFile if the line separator was not detected before, try to detect it
     * @return the detected line separator or null
     */
    @Nullable
    public static String detectLineSeparator(@NotNull VirtualFile file, boolean checkFile) {
        String lineSeparator = getDetectedLineSeparator(file);
        if (lineSeparator == null && checkFile) {
            try {
                getTextByBinaryPresentation(file.contentsToByteArray(), file);
                lineSeparator = getDetectedLineSeparator(file);
            } catch (IOException e) {
                // null will be returned
            }
        }
        return lineSeparator;
    }

    static String getDetectedLineSeparator(@NotNull VirtualFile file) {
        return file.getUserData(DETECTED_LINE_SEPARATOR_KEY);
    }

    /**
     * Change line separator for the file to the specified value (assumes that the documents were saved)
     *
     * @param project          the project instance
     * @param requestor        the requestor for the operation
     * @param file             the file to convert
     * @param newLineSeparator the new line separator for the file
     * @throws java.io.IOException in the case of IO problem
     */
    public static void changeLineSeparator(@Nullable Project project,
                                           @Nullable Object requestor,
                                           @NotNull VirtualFile file,
                                           @NotNull String newLineSeparator) throws IOException {
        String lineSeparator = getDetectedLineSeparator(file);
        if (lineSeparator != null && lineSeparator.equals(newLineSeparator)) {
            return;
        }
        CharSequence cs = getTextByBinaryPresentation(file.contentsToByteArray(), file);
        lineSeparator = getDetectedLineSeparator(file);
        if (lineSeparator == null || lineSeparator.equals(newLineSeparator)) {
            return;
        }
        if (!newLineSeparator.equals("\n")) {
            cs = StringUtil.convertLineSeparators(cs.toString(), newLineSeparator);
        }
        String text = cs.toString();
        file.putUserData(DETECTED_LINE_SEPARATOR_KEY, newLineSeparator);
        Writer w = getWriter(project, file, requestor, text, System.currentTimeMillis());
        try {
            w.write(text);
        } finally {
            w.close();
        }
    }

    @NotNull
    public static CharSequence getTextByBinaryPresentation(@NotNull byte[] bytes, Charset charset) {
        Pair<Charset, byte[]> pair = getBOMAndCharset(bytes, charset);
        byte[] bom = pair.getSecond();
        int offset = bom == null ? 0 : bom.length;

        final Pair<CharSequence, String> result = convertBytes(bytes, charset, offset);
        return result.getFirst();
    }

    // do not need to think about BOM here. it is processed outside
    @NotNull
    private static Pair<CharSequence, String> convertBytes(@NotNull byte[] bytes, Charset charset, final int startOffset) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, startOffset, bytes.length - startOffset);

        if (charset == null) {
            charset = CharsetToolkit.getDefaultSystemCharset();
        }
        if (charset == null) {
            //noinspection HardCodedStringLiteral
            charset = Charset.forName("ISO-8859-1");
        }
        CharBuffer charBuffer = charset.decode(byteBuffer);
        return convertLineSeparators(charBuffer);
    }

    private static final Key<Boolean> CHARSET_WAS_DETECTED_FROM_BYTES = new Key<Boolean>("CHARSET_WAS_DETECTED_FROM_BYTES");

    public static boolean wasCharsetDetectedFromBytes(@NotNull VirtualFile virtualFile) {
        return virtualFile.getUserData(CHARSET_WAS_DETECTED_FROM_BYTES) != null;
    }

    public static void setCharsetWasDetectedFromBytes(@NotNull VirtualFile virtualFile, boolean flag) {
        virtualFile.putUserData(CHARSET_WAS_DETECTED_FROM_BYTES, flag ? Boolean.TRUE : null);
    }
}
