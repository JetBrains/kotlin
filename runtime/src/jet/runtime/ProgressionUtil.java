/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

public class ProgressionUtil {
    private ProgressionUtil() {
    }

    // a mod b (in arithmetical sense)
    private static int mod(int a, int b) {
        int mod = a % b;
        return mod >= 0 ? mod : mod + b;
    }

    private static long mod(long a, long b) {
        long mod = a % b;
        return mod >= 0 ? mod : mod + b;
    }

    // (a - b) mod c
    private static int differenceModulo(int a, int b, int c) {
        return mod(mod(a, c) - mod(b, c), c);
    }

    private static long differenceModulo(long a, long b, long c) {
        return mod(mod(a, c) - mod(b, c), c);
    }


    /**
     * Calculates the final element of a bounded arithmetic progression, i.e. the last element of the progression which is in the range
     * from {@code start} to {@code end} in case of a positive {@code increment}, or from {@code end} to {@code start} in case of a negative
     * increment.
     *
     * <p>No validation on passed parameters is performed. The given parameters should satisfy the condition: either
     * {@code increment&nbsp;&gt; 0} and {@code start&nbsp;&lt;= end}, or {@code increment&nbsp;&lt; 0} and {@code start&nbsp;&gt;= end}.
     * @param start first element of the progression
     * @param end ending bound for the progression
     * @param increment increment, or difference of successive elements in the progression
     * @return the final element of the progression
     */
    public static int getProgressionFinalElement(int start, int end, int increment) {
        if (increment > 0) {
            return end - differenceModulo(end, start, increment);
        }
        else {
            return end + differenceModulo(start, end, -increment);
        }
    }

    /**
     * Calculates the final element of a bounded arithmetic progression, i.e. the last element of the progression which is in the range
     * from {@code start} to {@code end} in case of a positive {@code increment}, or from {@code end} to {@code start} in case of a negative
     * increment.
     *
     * <p>No validation on passed parameters is performed. The given parameters should satisfy the condition: either
     * {@code increment&nbsp;&gt; 0} and {@code start&nbsp;&lt;= end}, or {@code increment&nbsp;&lt; 0} and {@code start&nbsp;&gt;= end}.
     * @param start first element of the progression
     * @param end ending bound for the progression
     * @param increment increment, or difference of successive elements in the progression
     * @return the final element of the progression
     */
    public static long getProgressionFinalElement(long start, long end, long increment) {
        if (increment > 0) {
            return end - differenceModulo(end, start, increment);
        }
        else {
            return end + differenceModulo(start, end, -increment);
        }
    }
}
