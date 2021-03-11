import java.nio.charset.Charset;
import java.util.*;

class A {
    void constructors() throws Exception {
        new String();
        // TODO: new String("original");
        new String(new char[] {'a', 'b', 'c'});
        new String(new char[] {'b', 'd'}, 1, 1);
        new String(new int[] { 32, 65, 127 }, 0, 3);

        byte[] bytes = new byte[] { 32, 65, 100, 81 };
        Charset charset = Charset.forName("utf-8");
        new String(bytes);
        new String(bytes, charset);
        new String(bytes, 0, 2);
        new String(bytes, "utf-8");
        new String(bytes, 0, 2, "utf-8");
        new String(bytes, 0, 2, charset);

        new String(new StringBuilder("content"));
        new String(new StringBuffer("content"));
    }

    void normalMethods() {
        String s = "test string";
        s.length();
        s.isEmpty();
        s.charAt(1);
        s.codePointAt(2);
        s.codePointBefore(2);
        s.codePointCount(0, s.length());
        s.offsetByCodePoints(0, 4);
        s.compareTo("test 2");
        s.contains("seq");
        s.contentEquals(new StringBuilder(s));
        s.contentEquals(new StringBuffer(s));
        s.endsWith("ng");
        s.startsWith("te");
        s.startsWith("st", 2);
        s.indexOf("st");
        s.indexOf("st", 5);
        s.lastIndexOf("st");
        s.lastIndexOf("st", 4);
        s.indexOf('t');
        s.indexOf('t', 5);
        s.lastIndexOf('t');
        s.lastIndexOf('t', 5);
        s.substring(1);
        s.substring(0, 4);
        s.subSequence(0, 4);
        s.replace('e', 'i');
        s.replace("est", "oast");
        s.intern();
        s.toLowerCase();
        s.toLowerCase(Locale.FRENCH);
        s.toUpperCase();
        s.toUpperCase(Locale.FRENCH);

        s.toString();
        s.toCharArray();
   }

    void specialMethods() throws Exception {
        String s = "test string";
        s.equals("test");
        s.equalsIgnoreCase(
                "tesT"
        );
        s.compareToIgnoreCase("Test");
        s.regionMatches(
                true,
                0,
                "TE",
                0,
                2
        );
        s.regionMatches(0, "st", 1, 2);
        s.matches("\\w+");
        s.replaceAll("\\w+", "---")
              .replaceFirst("([s-t])", "A$1");
        useSplit(s.split("\\s+"));
        useSplit(s.split("\\s+", 0));
        useSplit(s.split("\\s+", -1));
        useSplit(s.split("\\s+", 2));
        int limit = 5;
        useSplit(s.split("\\s+", limit));
        s.trim();
        s.concat(" another");

        s.getBytes();
        s.getBytes(Charset.forName("utf-8"));
        s.getBytes("utf-8");

        char[] chars = new char[10];
        s.getChars(1, 11, chars, 0);
    }

    void staticMethods() {
        String.valueOf(1);
        String.valueOf(1L);
        String.valueOf('a');
        String.valueOf(true);
        String.valueOf(1.11F);
        String.valueOf(3.14);
        String.valueOf(new Object());

        String.format(
                Locale.FRENCH,
                "Je ne mange pas %d jours",
                6
        );
        String.format("Operation completed with %s", "success");

        char[] chars = {'a', 'b', 'c'};
        String.valueOf(chars);
        String.valueOf(chars, 1, 2);
        String.copyValueOf(chars);
        String.copyValueOf(chars, 1, 2);

        Comparator<String> order = String.CASE_INSENSITIVE_ORDER;
    }

    void unsupportedMethods() {
        String s = "test string";
        /* TODO:
        s.indexOf(32);
        s.indexOf(32, 2);
        s.lastIndexOf(32);
        s.lastIndexOf(32, 2);
        */
    }

    void useSplit(String[] result) {}
}