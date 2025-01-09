/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package cases.nestedClasses

private class PrivateClass {
    public object ObjPublic
    internal object ObjInternal
    protected object ObjProtected
    private object ObjPrivate

    public class NestedPublic
    internal class NestedInternal
    protected class NestedProtected
    private class NestedPrivate

    public interface NestedPublicInterface
    internal interface NestedInternalInterface
    protected interface NestedProtectedInterface
    private interface NestedPrivateInterface

    public inner class InnerPublic
    internal inner class InnerInternal
    protected inner class InnerProtected
    private inner class InnerPrivate
}

