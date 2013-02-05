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

import jet.Function0;

import java.util.*;

@SuppressWarnings("unused")
public class Intrinsics {
    private Intrinsics() {
    }

    public static String stringPlus(String self, Object other) {
        return ((self == null) ? "null" : self) + ((other == null) ? "null" : other.toString());
    }

    public static void throwNpe() {
        throw new JetNullPointerException();
    }

    public static void checkReturnedValueIsNotNull(Object value, String className, String methodName) {
        if (value == null) {
            IllegalStateException exception =
                    new IllegalStateException("Method specified as non-null returned null: " + className + "." + methodName);
            throw sanitizeStackTrace(exception);
        }
    }

    public static void checkFieldIsNotNull(Object value, String className, String fieldName) {
        if (value == null) {
            IllegalStateException exception =
                    new IllegalStateException("Field specified as non-null contains null: " + className + "." + fieldName);
            throw sanitizeStackTrace(exception);
        }
    }

    public static void checkParameterIsNotNull(Object value, String paramName) {
        if (value == null) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

            // #0 is Thread.getStackTrace(), #1 is Intrinsics.checkParameterIsNotNull, #2 is our caller
            StackTraceElement caller = stackTraceElements[2];
            String className = caller.getClassName();
            String methodName = caller.getMethodName();

            IllegalArgumentException exception =
                    new IllegalArgumentException("Parameter specified as non-null contains null: " +
                                                 "method " + className + "." + methodName +
                                                 ", parameter " + paramName);
            throw sanitizeStackTrace(exception);
        }
    }

    public static <T> Class<T> getJavaClass(T self) {
        return (Class<T>) self.getClass();
    }

    public static int compare(long thisVal, long anotherVal) {
        return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
    }

    public static int compare(int thisVal, int anotherVal) {
        return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
    }

    public static boolean areEqual(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }

    public static <R> R stupidSync(Object lock, Function0<R> block) {
        synchronized (lock) {
            return block.invoke();
        }
    }

    private static final Set<String> METHOD_NAMES_TO_SKIP = new HashSet<String>(Arrays.asList(
            "throwNpe", "checkReturnedValueIsNotNull", "checkFieldIsNotNull", "checkParameterIsNotNull"
    ));

    private static <T extends Throwable> T sanitizeStackTrace(T throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        ArrayList<StackTraceElement> list = new ArrayList<StackTraceElement>();
        boolean skip = true;
        for(StackTraceElement ste : stackTrace) {
            if (!skip) {
                list.add(ste);
            }
            else {
                if ("jet.runtime.Intrinsics".equals(ste.getClassName())) {
                    if (METHOD_NAMES_TO_SKIP.contains(ste.getMethodName())) {
                        skip = false;
                    }
                }
            }
        }
        throwable.setStackTrace(list.toArray(new StackTraceElement[list.size()]));
        return throwable;
    }

    private static class JetNullPointerException extends NullPointerException {
        @Override
        public synchronized Throwable fillInStackTrace() {
            super.fillInStackTrace();
            return sanitizeStackTrace(this);
        }
    }

    public static class SpreadBuilder extends ArrayList {
        public void addSpread(Object array) {
            if (array != null) {
                if (array instanceof Object[]) {
                    Object[] arr = (Object[]) array;
                    if (arr.length > 0) {
                        ensureCapacity(size() + arr.length);
                        for (int i = 0; i < arr.length; i++) {
                            add(arr[i]);
                        }
                    }
                }
                else if (array instanceof Collection) {
                    addAll((Collection) array);
                }
                else if (array instanceof Iterable) {
                    for(Iterator iterator = ((Iterable) array).iterator(); iterator.hasNext(); ) {
                        add(iterator.next());
                    }
                }
                else if (array instanceof Iterator) {
                    for(Iterator iterator = ((Iterator) array); iterator.hasNext(); ) {
                        add(iterator.next());
                    }
                }
                else {
                    throw new UnsupportedOperationException("Don't know how to spread " + array.getClass());
                }
            }
        }
    }
}
