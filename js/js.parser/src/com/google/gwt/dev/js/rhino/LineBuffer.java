/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1997-1999 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s):
 * Mike McCabe
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the GNU Public License (the "GPL"), in which case the
 * provisions of the GPL are applicable instead of those above.
 * If you wish to allow use of your version of this file only
 * under the terms of the GPL and not to allow others to use your
 * version of this file under the NPL, indicate your decision by
 * deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL.  If you do not delete
 * the provisions above, a recipient may use your version of this
 * file under either the NPL or the GPL.
 */
// Modified by Google

package com.google.gwt.dev.js.rhino;

import java.io.Reader;
import java.io.IOException;

/**
 * An input buffer that combines fast character-based access with
 * (slower) support for retrieving the text of the current line.  It
 * also supports building strings directly out of the internal buffer
 * to support fast scanning with minimal object creation.
 *
 * Note that it is customized in several ways to support the
 * TokenStream class, and should not be considered general.
 *
 * Credits to Kipp Hickman and John Bandhauer.
 *
 * @author Mike McCabe
 */
final class LineBuffer {
    /*
     * for smooth operation of getLine(), this should be greater than
     * the length of any expected line.  Currently, 256 is 3% slower
     * than 4096 for large compiles, but seems safer given evaluateString.
     * Strings for the scanner are are built with StringBuffers
     * instead of directly out of the buffer whenever a string crosses
     * a buffer boundary, so small buffer sizes will mean that more
     * objects are created.
     */
    static final int BUFLEN = 256;

    LineBuffer(Reader in, int lineno) {
        this.in = in;
        this.lineno = lineno;
    }

    int read() throws IOException {
        for(;;) {
            if (end == offset && !fill())
                return -1;

            int c = buffer[offset];
            ++offset;

            if ((c & EOL_HINT_MASK) == 0) {
                switch (c) {
                    case '\r':
                        // if the next character is a newline, skip past it.
                        if (offset != end) {
                            if (buffer[offset] == '\n')
                                ++offset;
                        } else {
                            // set a flag for fill(), in case the first char
                            // of the next fill is a newline.
                            lastWasCR = true;
                        }
                        // NO break here!
                    case '\n': case '\u2028': case '\u2029':
                        prevStart = lineStart;
                        lineStart = offset;
                        lineno++;
                        return '\n';
                }
            }

            if (c < 128 || !formatChar(c)) {
                return c;
            }
        }
    }

    void unread() {
        // offset can only be 0 when we're asked to unread() an implicit
        // EOF_CHAR.

        // This would be wrong behavior in the general case,
        // because a peek() could map a buffer.length offset to 0
        // in the process of a fill(), and leave it there.  But
        // the scanner never calls peek() or a failed match()
        // followed by unread()... this would violate 1-character
        // lookahead.
        if (offset == 0 && !hitEOF) Context.codeBug();

        if (offset == 0) // Same as if (hitEOF)
            return;
        offset--;
        int c = buffer[offset];
        if ((c & EOL_HINT_MASK) == 0 && eolChar(c)) {
            lineStart = prevStart;
            lineno--;
        }
    }

    private void skipFormatChar() {
        if (checkSelf && !formatChar(buffer[offset])) Context.codeBug();

        // swap prev character with format one so possible call to
        // startString can assume that previous non-format char is at
        // offset - 1. Note it causes getLine to return not exactly the
        // source LineBuffer read, but it is used only in error reporting
        // and should not be a problem.
        if (offset != 0) {
            char tmp = buffer[offset];
            buffer[offset] = buffer[offset - 1];
            buffer[offset - 1] = tmp;
        }
        else if (otherEnd != 0) {
            char tmp = buffer[offset];
            buffer[offset] = otherBuffer[otherEnd - 1];
            otherBuffer[otherEnd - 1] = tmp;
        }

        ++offset;
    }

    int peek() throws IOException {
        for (;;) {
            if (end == offset && !fill()) {
                return -1;
            }

            int c = buffer[offset];
            if ((c & EOL_HINT_MASK) == 0 && eolChar(c)) {
                return '\n';
            }
            if (c < 128 || !formatChar(c)) {
                return c;
            }

            skipFormatChar();
        }
    }

    boolean match(int test) throws IOException {
        // TokenStream never looks ahead for '\n', which allows simple code
        if ((test & EOL_HINT_MASK) == 0 && eolChar(test))
            Context.codeBug();
        // Format chars are not allowed either
        if (test >= 128 && formatChar(test))
            Context.codeBug();

        for (;;) {
            if (end == offset && !fill())
                return false;

            int c = buffer[offset];
            if (test == c) {
                ++offset;
                return true;
            }
            if (c < 128 || !formatChar(c)) {
                return false;
            }
            skipFormatChar();
        }
    }

    // Reconstruct a source line from the buffers.  This can be slow...
    String getLine() {
        // Look for line end in the unprocessed buffer
        int i = offset;
        while(true) {
            if (i == end) {
                // if we're out of buffer, let's just expand it.  We do
                // this instead of reading into a StringBuffer to
                // preserve the stream for later reads.
                if (end == buffer.length) {
                    char[] tmp = new char[buffer.length * 2];
                    System.arraycopy(buffer, 0, tmp, 0, end);
                    buffer = tmp;
                }
                int charsRead = 0;
                try {
                    charsRead = in.read(buffer, end, buffer.length - end);
                } catch (IOException ioe) {
                    // ignore it, we're already displaying an error...
                    break;
                }
                if (charsRead < 0)
                    break;
                end += charsRead;
            }
            int c = buffer[i];
            if ((c & EOL_HINT_MASK) == 0 && eolChar(c))
                break;
            i++;
        }

        int start = lineStart;
        if (lineStart < 0) {
            // the line begins somewhere in the other buffer; get that first.
            StringBuffer sb = new StringBuffer(otherEnd - otherStart + i);
            sb.append(otherBuffer, otherStart, otherEnd - otherStart);
            sb.append(buffer, 0, i);
            return sb.toString();
        } else {
            return new String(buffer, lineStart, i - lineStart);
        }
    }

    // Get the offset of the current character, relative to
    // the line that getLine() returns.
    int getOffset() {
        if (lineStart < 0)
            // The line begins somewhere in the other buffer.
            return offset + (otherEnd - otherStart);
        else
            return offset - lineStart;
    }

    private boolean fill() throws IOException {
        // fill should be caled only for emty buffer
        if (checkSelf && !(end == offset)) Context.codeBug();

        // swap buffers
        char[] tempBuffer = buffer;
        buffer = otherBuffer;
        otherBuffer = tempBuffer;

        // allocate the buffers lazily, in case we're handed a short string.
        if (buffer == null) {
            buffer = new char[BUFLEN];
        }

        // buffers have switched, so move the newline marker.
        if (lineStart >= 0) {
            otherStart = lineStart;
        } else {
            // discard beging of the old line
            otherStart = 0;
        }

        otherEnd = end;

        // set lineStart to a sentinel value, unless this is the first
        // time around.
        prevStart = lineStart = (otherBuffer == null) ? 0 : -1;

        offset = 0;
        end = in.read(buffer, 0, buffer.length);
        if (end < 0) {
            end = 0;

            // can't null buffers here, because a string might be retrieved
            // out of the other buffer, and a 0-length string might be
            // retrieved out of this one.

            hitEOF = true;
            return false;
        }

        // If the last character of the previous fill was a carriage return,
        // then ignore a newline.

        // There's another bizzare special case here.  If lastWasCR is
        // true, and we see a newline, and the buffer length is
        // 1... then we probably just read the last character of the
        // file, and returning after advancing offset is not the right
        // thing to do.  Instead, we try to ignore the newline (and
        // likely get to EOF for real) by doing yet another fill().
        if (lastWasCR) {
            if (buffer[0] == '\n') {
              offset++;
              if (end == 1)
                  return fill();
            }
            lineStart = offset;
            lastWasCR = false;
        }
        return true;
    }

    int getLineno() { return lineno; }
    boolean eof() { return hitEOF; }

    private static boolean formatChar(int c) {
        return Character.getType((char)c) == Character.FORMAT;
    }

    private static boolean eolChar(int c) {
        return c == '\r' || c == '\n' || c == '\u2028' || c == '\u2029';
    }

    // Optimization for faster check for eol character: eolChar(c) returns
    // true only when (c & EOL_HINT_MASK) == 0
    private static final int EOL_HINT_MASK = 0xdfd0;

    private Reader in;
    private char[] otherBuffer = null;
    private char[] buffer = null;

    // Yes, there are too too many of these.
    private int offset = 0;
    private int end = 0;
    private int otherEnd;
    private int lineno;

    private int lineStart = 0;
    private int otherStart = 0;
    private int prevStart = 0;

    private boolean lastWasCR = false;
    private boolean hitEOF = false;

// Rudimentary support for Design-by-Contract
    private static final boolean checkSelf = true;
}
