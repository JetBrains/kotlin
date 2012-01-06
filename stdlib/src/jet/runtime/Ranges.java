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

    public static ByteRange rangeTo(byte from, byte to) {
      if(from > to) {
        return new ByteRange(from, to-from-1);
      }
      else {
        return new ByteRange(from, to-from+1);
      }
    }

    public static ShortRange rangeTo(byte from, short to) {
      if(from > to) {
        return new ShortRange(from, to-from-1);
      }
      else {
        return new ShortRange(from, to-from+1);
      }
    }

    public static IntRange rangeTo(byte from, int to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static LongRange rangeTo(byte from, long to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static CharRange rangeTo(byte from, char to) {
      if(from > to) {
        return new CharRange((char) from, to-from-1);
      }
      else {
        return new CharRange((char) from, to-from+1);
      }
    }

    public static ShortRange rangeTo(short from, byte to) {
      if(from > to) {
        return new ShortRange(from, to-from-1);
      }
      else {
        return new ShortRange(from, to-from+1);
      }
    }

    public static ShortRange rangeTo(short from, short to) {
      if(from > to) {
        return new ShortRange(from, to-from-1);
      }
      else {
        return new ShortRange(from, to-from+1);
      }
    }

    public static IntRange rangeTo(short from, int to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static LongRange rangeTo(short from, long to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static ShortRange rangeTo(short from, char to) {
      if(from > to) {
        return new ShortRange(from, to-from-1);
      }
      else {
        return new ShortRange(from, to-from+1);
      }
    }

    public static IntRange rangeTo(int from, byte to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static IntRange rangeTo(int from, short to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static IntRange rangeTo(int from, int to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static LongRange rangeTo(int from, long to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static IntRange rangeTo(int from, char to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static LongRange rangeTo(long from, byte to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static LongRange rangeTo(long from, short to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static LongRange rangeTo(long from, int to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static LongRange rangeTo(long from, long to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static LongRange rangeTo(long from, char to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static CharRange rangeTo(char from, byte to) {
      if(from > to) {
        return new CharRange(from, to-from-1);
      }
      else {
        return new CharRange(from, to-from+1);
      }
    }

    public static ShortRange rangeTo(char from, short to) {
      if(from > to) {
        return new ShortRange((short) from, to-from-1);
      }
      else {
        return new ShortRange((short) from, to-from+1);
      }
    }

    public static IntRange rangeTo(char from, int to) {
      if(from > to) {
        return new IntRange(from, to-from-1);
      }
      else {
        return new IntRange(from, to-from+1);
      }
    }

    public static LongRange rangeTo(char from, long to) {
      if(from > to) {
        return new LongRange(from, to-from-1);
      }
      else {
        return new LongRange(from, to-from+1);
      }
    }

    public static CharRange rangeTo(char from, char to) {
      if(from > to) {
        return new CharRange(from, to-from-1);
      }
      else {
        return new CharRange(from, to-from+1);
      }
    }

    public static void main(String[] args) {
        List<String> strings = Arrays.asList("byte", "short", "int", "long", /*"float", "double",*/ "char");
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

                System.out.println("\npublic static " + resType + " rangeTo(" + t1 + " from, " + t2 + " to) {" +
                                   "\n  if(from > to) {\n" +
                                   "    return new " + resType + "(from, to-from-1);\n" +
                                   "  }\n" +
                                   "  else {\n" +
                                   "    return new " + resType + "(from, to-from+1);\n" +
                                   "  }\n" +
                                   "}");
            }
    }
}
