/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package jet.runtime;

import jet.*;

import java.util.Arrays;
import java.util.List;

/**
* @author alex.tkachman
*/
public class Ranges {
    private Ranges() {
    }

    public static ByteRange upTo(byte from, byte to) {
      if(from > to) {
        return ByteRange.empty;
      }
      else {
        return new ByteRange(from, to-from+1);
      }
    }

    public static ByteRange downTo(byte from, byte to) {
      if(from > to) {
        return new ByteRange(from, to-from-1);
      }
      else {
        return ByteRange.empty;
      }
    }

    public static ShortRange upTo(byte from, short to) {
      if(from > to) {
        return ShortRange.empty;
      }
      else {
        return new ShortRange(from, to-from+1);
      }
    }

    public static ShortRange downTo(byte from, short to) {
      if(from > to) {
        return new ShortRange(from, to-from-1);
      }
      else {
        return ShortRange.empty;
      }
    }

    public static IntRange upTo(byte from, int to) {
      if(from > to) {
        return IntRange.empty;
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static IntRange downTo(byte from, int to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return IntRange.empty;
      }
    }

    public static LongRange upTo(byte from, long to) {
      if(from > to) {
        return LongRange.empty;
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static LongRange downTo(byte from, long to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return LongRange.empty;
      }
    }

    public static FloatRange upTo(byte from, float to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange downTo(byte from, float to) {
        return new FloatRange(from, to-from);
    }

    public static DoubleRange upTo(byte from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange downTo(byte from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static CharRange upTo(byte from, char to) {
      if(from > to) {
        return CharRange.empty;
      }
      else {
        return new CharRange((char) from, to-from+1);
      }
    }

    public static CharRange downTo(byte from, char to) {
      if(from > to) {
        return new CharRange((char) from, to-from-1);
      }
      else {
        return CharRange.empty;
      }
    }

    public static ShortRange upTo(short from, byte to) {
      if(from > to) {
        return ShortRange.empty;
      }
      else {
        return new ShortRange(from, to-from+1);
      }
    }

    public static ShortRange downTo(short from, byte to) {
      if(from > to) {
        return new ShortRange(from, to-from-1);
      }
      else {
        return ShortRange.empty;
      }
    }

    public static ShortRange upTo(short from, short to) {
      if(from > to) {
        return ShortRange.empty;
      }
      else {
        return new ShortRange(from, to-from+1);
      }
    }

    public static ShortRange downTo(short from, short to) {
      if(from > to) {
        return new ShortRange(from, to-from-1);
      }
      else {
        return ShortRange.empty;
      }
    }

    public static IntRange upTo(short from, int to) {
      if(from > to) {
        return IntRange.empty;
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static IntRange downTo(short from, int to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return IntRange.empty;
      }
    }

    public static LongRange upTo(short from, long to) {
      if(from > to) {
        return LongRange.empty;
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static LongRange downTo(short from, long to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return LongRange.empty;
      }
    }

    public static FloatRange upTo(short from, float to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange downTo(short from, float to) {
        return new FloatRange(from, to-from);
    }

    public static DoubleRange upTo(short from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange downTo(short from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static ShortRange upTo(short from, char to) {
      if(from > to) {
        return ShortRange.empty;
      }
      else {
        return new ShortRange(from, to-from+1);
      }
    }

    public static ShortRange downTo(short from, char to) {
      if(from > to) {
        return new ShortRange(from, to-from-1);
      }
      else {
        return ShortRange.empty;
      }
    }

    public static IntRange upTo(int from, byte to) {
      if(from > to) {
        return IntRange.empty;
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static IntRange downTo(int from, byte to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return IntRange.empty;
      }
    }

    public static IntRange upTo(int from, short to) {
      if(from > to) {
        return IntRange.empty;
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static IntRange downTo(int from, short to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return IntRange.empty;
      }
    }

    public static IntRange upTo(int from, int to) {
      if(from > to) {
        return IntRange.empty;
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static IntRange downTo(int from, int to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return IntRange.empty;
      }
    }

    public static LongRange upTo(int from, long to) {
      if(from > to) {
        return LongRange.empty;
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static LongRange downTo(int from, long to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return LongRange.empty;
      }
    }

    public static FloatRange upTo(int from, float to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange downTo(int from, float to) {
        return new FloatRange(from, to-from);
    }

    public static DoubleRange upTo(int from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange downTo(int from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static IntRange upTo(int from, char to) {
      if(from > to) {
        return IntRange.empty;
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static IntRange downTo(int from, char to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return IntRange.empty;
      }
    }

    public static LongRange upTo(long from, byte to) {
      if(from > to) {
        return LongRange.empty;
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static LongRange downTo(long from, byte to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return LongRange.empty;
      }
    }

    public static LongRange upTo(long from, short to) {
      if(from > to) {
        return LongRange.empty;
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static LongRange downTo(long from, short to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return LongRange.empty;
      }
    }

    public static LongRange upTo(long from, int to) {
      if(from > to) {
        return LongRange.empty;
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static LongRange downTo(long from, int to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return LongRange.empty;
      }
    }

    public static LongRange upTo(long from, long to) {
      if(from > to) {
        return LongRange.empty;
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static LongRange downTo(long from, long to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return LongRange.empty;
      }
    }

    public static FloatRange upTo(long from, float to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange downTo(long from, float to) {
        return new FloatRange(from, to-from);
    }

    public static DoubleRange upTo(long from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange downTo(long from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static LongRange upTo(long from, char to) {
      if(from > to) {
        return LongRange.empty;
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static LongRange downTo(long from, char to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return LongRange.empty;
      }
    }

    public static FloatRange upTo(float from, byte to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange downTo(float from, byte to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange upTo(float from, short to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange downTo(float from, short to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange upTo(float from, int to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange downTo(float from, int to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange upTo(float from, long to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange downTo(float from, long to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange upTo(float from, float to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange downTo(float from, float to) {
        return new FloatRange(from, to-from);
    }

    public static DoubleRange upTo(float from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange downTo(float from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static FloatRange upTo(float from, char to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange downTo(float from, char to) {
        return new FloatRange(from, to-from);
    }

    public static DoubleRange upTo(double from, byte to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange downTo(double from, byte to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange upTo(double from, short to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange downTo(double from, short to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange upTo(double from, int to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange downTo(double from, int to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange upTo(double from, long to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange downTo(double from, long to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange upTo(double from, float to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange downTo(double from, float to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange upTo(double from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange downTo(double from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange upTo(double from, char to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange downTo(double from, char to) {
        return new DoubleRange(from, to-from);
    }

    public static CharRange upTo(char from, byte to) {
      if(from > to) {
        return CharRange.empty;
      }
      else {
        return new CharRange(from, to-from+1);
      }
    }

    public static CharRange downTo(char from, byte to) {
      if(from > to) {
        return new CharRange(from, to-from-1);
      }
      else {
        return CharRange.empty;
      }
    }

    public static ShortRange upTo(char from, short to) {
      if(from > to) {
        return ShortRange.empty;
      }
      else {
        return new ShortRange((short) from, to-from+1);
      }
    }

    public static ShortRange downTo(char from, short to) {
      if(from > to) {
        return new ShortRange((short) from, to-from-1);
      }
      else {
        return ShortRange.empty;
      }
    }

    public static IntRange upTo(char from, int to) {
      if(from > to) {
        return IntRange.empty;
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static IntRange downTo(char from, int to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return IntRange.empty;
      }
    }

    public static LongRange upTo(char from, long to) {
      if(from > to) {
        return LongRange.empty;
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static LongRange downTo(char from, long to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return LongRange.empty;
      }
    }

    public static FloatRange upTo(char from, float to) {
        return new FloatRange(from, to-from);
    }

    public static FloatRange downTo(char from, float to) {
        return new FloatRange(from, to-from);
    }

    public static DoubleRange upTo(char from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static DoubleRange downTo(char from, double to) {
        return new DoubleRange(from, to-from);
    }

    public static CharRange upTo(char from, char to) {
      if(from > to) {
        return CharRange.empty;
      }
      else {
        return new CharRange(from, to-from+1);
      }
    }

    public static CharRange downTo(char from, char to) {
      if(from > to) {
        return new CharRange(from, to-from-1);
      }
      else {
        return CharRange.empty;
      }
    }
    public static void main(String[] args) {
        List<String> strings = Arrays.asList("byte", "short", "int", "long", "float", "double", "char");
        for(String t1 : strings)
            for(String t2 : strings) {
                String resType;
                if(t1.equals("double") || t2.equals("double")) {
                    resType = "DoubleRange";
                }
                else if(t1.equals("float") || t2.equals("float")) {
                    resType = "FloatRange";
                }
                else if(t1.equals("long") || t2.equals("long")) {
                    resType = "LongRange";
                }
                else if(t1.equals("int") || t2.equals("int")) {
                    resType = "IntRange";
                }
                else if(t1.equals("short") || t2.equals("short")) {
                    resType = "ShortRange";
                }
                else if(t1.equals("char") || t2.equals("char")) {
                    resType = "CharRange";
                }
                else {
                    resType = "ByteRange";
                }

                if(resType.equals("FloatRange") || resType.equals("DoubleRange")) {
                    System.out.println("\npublic static " + resType + " upTo(" + t1 + " from, " + t2 + " to) {\n" +
                                       "    return new " + resType + "(from, to-from);\n" +
                                       "}");
                    System.out.println("\npublic static " + resType + " downTo(" + t1 + " from, " + t2 + " to) {\n" +
                                       "    return new " + resType + "(from, to-from);\n" +
                                       "}");
                }
                else {
                    System.out.println("\npublic static " + resType + " upTo(" + t1 + " from, " + t2 + " to) {" +
                                       "\n  if(from > to) {\n" +
                                       "    return " + resType + ".empty;\n" +
                                       "  }\n" +
                                       "  else {\n" +
                                       "    return new " + resType + "(from, to-from+1);\n" +
                                       "  }\n" +
                                       "}");
                    System.out.println("\npublic static " + resType + " downTo(" + t1 + " from, " + t2 + " to) {" +
                                       "\n  if(from > to) {\n" +
                                       "    return new " + resType + "(from, to-from-1);\n" +
                                       "  }\n" +
                                       "  else {\n" +
                                       "    return " + resType + ".empty;\n" +
                                       "  }\n" +
                                       "}");
                }
            }
    }
}
