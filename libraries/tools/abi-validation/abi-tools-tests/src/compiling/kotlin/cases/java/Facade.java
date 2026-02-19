/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package cases.java;

class Part1 {
    public static void publicMethod(int param) { }

    public static class Part2 extends Part1 {
        public static void publicMethod(String param) { }
    }
}


public class Facade extends Part1.Part2 { }
