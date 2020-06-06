/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ClipboardEventInit(clipboardData: org.w3c.dom.DataTransfer? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.dom.clipboard.ClipboardEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ClipboardPermissionDescriptor(allowWithoutGesture: kotlin.Boolean? = ...): org.w3c.dom.clipboard.ClipboardPermissionDescriptor
/*∆*/ 
/*∆*/ public abstract external class Clipboard : org.w3c.dom.events.EventTarget {
/*∆*/     public constructor Clipboard()
/*∆*/ 
/*∆*/     public final fun read(): kotlin.js.Promise<org.w3c.dom.DataTransfer>
/*∆*/ 
/*∆*/     public final fun readText(): kotlin.js.Promise<kotlin.String>
/*∆*/ 
/*∆*/     public final fun write(data: org.w3c.dom.DataTransfer): kotlin.js.Promise<kotlin.Unit>
/*∆*/ 
/*∆*/     public final fun writeText(data: kotlin.String): kotlin.js.Promise<kotlin.Unit>
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class ClipboardEvent : org.w3c.dom.events.Event {
/*∆*/     public constructor ClipboardEvent(type: kotlin.String, eventInitDict: org.w3c.dom.clipboard.ClipboardEventInit = ...)
/*∆*/ 
/*∆*/     public open val clipboardData: org.w3c.dom.DataTransfer? { get; }
/*∆*/ 
/*∆*/     public companion object of ClipboardEvent {
/*∆*/         public final val AT_TARGET: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val BUBBLING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CAPTURING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NONE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface ClipboardEventInit : org.w3c.dom.EventInit {
/*∆*/     public open var clipboardData: org.w3c.dom.DataTransfer? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface ClipboardPermissionDescriptor {
/*∆*/     public open var allowWithoutGesture: kotlin.Boolean? { get; set; }
/*∆*/ }