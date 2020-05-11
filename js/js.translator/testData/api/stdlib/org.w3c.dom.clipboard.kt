package org.w3c.dom.clipboard

@kotlin.internal.InlineOnly public inline fun ClipboardEventInit(/*0*/ clipboardData: org.w3c.dom.DataTransfer? = ..., /*1*/ bubbles: kotlin.Boolean? = ..., /*2*/ cancelable: kotlin.Boolean? = ..., /*3*/ composed: kotlin.Boolean? = ...): org.w3c.dom.clipboard.ClipboardEventInit
@kotlin.internal.InlineOnly public inline fun ClipboardPermissionDescriptor(/*0*/ allowWithoutGesture: kotlin.Boolean? = ...): org.w3c.dom.clipboard.ClipboardPermissionDescriptor

public abstract external class Clipboard : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor Clipboard()
    public final fun read(): kotlin.js.Promise<org.w3c.dom.DataTransfer>
    public final fun readText(): kotlin.js.Promise<kotlin.String>
    public final fun write(/*0*/ data: org.w3c.dom.DataTransfer): kotlin.js.Promise<kotlin.Unit>
    public final fun writeText(/*0*/ data: kotlin.String): kotlin.js.Promise<kotlin.Unit>
}

public open external class ClipboardEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor ClipboardEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.dom.clipboard.ClipboardEventInit = ...)
    public open val clipboardData: org.w3c.dom.DataTransfer?
        public open fun <get-clipboardData>(): org.w3c.dom.DataTransfer?

    public companion object Companion {
        public final val AT_TARGET: kotlin.Short
            public final fun <get-AT_TARGET>(): kotlin.Short
        public final val BUBBLING_PHASE: kotlin.Short
            public final fun <get-BUBBLING_PHASE>(): kotlin.Short
        public final val CAPTURING_PHASE: kotlin.Short
            public final fun <get-CAPTURING_PHASE>(): kotlin.Short
        public final val NONE: kotlin.Short
            public final fun <get-NONE>(): kotlin.Short
    }
}

public external interface ClipboardEventInit : org.w3c.dom.EventInit {
    public open var clipboardData: org.w3c.dom.DataTransfer?
        public open fun <get-clipboardData>(): org.w3c.dom.DataTransfer?
        public open fun <set-clipboardData>(/*0*/ value: org.w3c.dom.DataTransfer?): kotlin.Unit
}

public external interface ClipboardPermissionDescriptor {
    public open var allowWithoutGesture: kotlin.Boolean?
        public open fun <get-allowWithoutGesture>(): kotlin.Boolean?
        public open fun <set-allowWithoutGesture>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
}