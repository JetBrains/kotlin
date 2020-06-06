/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun CompositionEventInit(data: kotlin.String? = ..., view: org.w3c.dom.Window? = ..., detail: kotlin.Int? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.dom.events.CompositionEventInit
/*∆*/ 
/*∆*/ public fun EventListener(handler: (org.w3c.dom.events.Event) -> kotlin.Unit): org.w3c.dom.events.EventListener
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun EventModifierInit(ctrlKey: kotlin.Boolean? = ..., shiftKey: kotlin.Boolean? = ..., altKey: kotlin.Boolean? = ..., metaKey: kotlin.Boolean? = ..., modifierAltGraph: kotlin.Boolean? = ..., modifierCapsLock: kotlin.Boolean? = ..., modifierFn: kotlin.Boolean? = ..., modifierFnLock: kotlin.Boolean? = ..., modifierHyper: kotlin.Boolean? = ..., modifierNumLock: kotlin.Boolean? = ..., modifierScrollLock: kotlin.Boolean? = ..., modifierSuper: kotlin.Boolean? = ..., modifierSymbol: kotlin.Boolean? = ..., modifierSymbolLock: kotlin.Boolean? = ..., view: org.w3c.dom.Window? = ..., detail: kotlin.Int? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.dom.events.EventModifierInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun FocusEventInit(relatedTarget: org.w3c.dom.events.EventTarget? = ..., view: org.w3c.dom.Window? = ..., detail: kotlin.Int? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.dom.events.FocusEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun InputEventInit(data: kotlin.String? = ..., isComposing: kotlin.Boolean? = ..., view: org.w3c.dom.Window? = ..., detail: kotlin.Int? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.dom.events.InputEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun KeyboardEventInit(key: kotlin.String? = ..., code: kotlin.String? = ..., location: kotlin.Int? = ..., repeat: kotlin.Boolean? = ..., isComposing: kotlin.Boolean? = ..., ctrlKey: kotlin.Boolean? = ..., shiftKey: kotlin.Boolean? = ..., altKey: kotlin.Boolean? = ..., metaKey: kotlin.Boolean? = ..., modifierAltGraph: kotlin.Boolean? = ..., modifierCapsLock: kotlin.Boolean? = ..., modifierFn: kotlin.Boolean? = ..., modifierFnLock: kotlin.Boolean? = ..., modifierHyper: kotlin.Boolean? = ..., modifierNumLock: kotlin.Boolean? = ..., modifierScrollLock: kotlin.Boolean? = ..., modifierSuper: kotlin.Boolean? = ..., modifierSymbol: kotlin.Boolean? = ..., modifierSymbolLock: kotlin.Boolean? = ..., view: org.w3c.dom.Window? = ..., detail: kotlin.Int? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.dom.events.KeyboardEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun MouseEventInit(screenX: kotlin.Int? = ..., screenY: kotlin.Int? = ..., clientX: kotlin.Int? = ..., clientY: kotlin.Int? = ..., button: kotlin.Short? = ..., buttons: kotlin.Short? = ..., relatedTarget: org.w3c.dom.events.EventTarget? = ..., region: kotlin.String? = ..., ctrlKey: kotlin.Boolean? = ..., shiftKey: kotlin.Boolean? = ..., altKey: kotlin.Boolean? = ..., metaKey: kotlin.Boolean? = ..., modifierAltGraph: kotlin.Boolean? = ..., modifierCapsLock: kotlin.Boolean? = ..., modifierFn: kotlin.Boolean? = ..., modifierFnLock: kotlin.Boolean? = ..., modifierHyper: kotlin.Boolean? = ..., modifierNumLock: kotlin.Boolean? = ..., modifierScrollLock: kotlin.Boolean? = ..., modifierSuper: kotlin.Boolean? = ..., modifierSymbol: kotlin.Boolean? = ..., modifierSymbolLock: kotlin.Boolean? = ..., view: org.w3c.dom.Window? = ..., detail: kotlin.Int? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.dom.events.MouseEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun UIEventInit(view: org.w3c.dom.Window? = ..., detail: kotlin.Int? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.dom.events.UIEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun WheelEventInit(deltaX: kotlin.Double? = ..., deltaY: kotlin.Double? = ..., deltaZ: kotlin.Double? = ..., deltaMode: kotlin.Int? = ..., screenX: kotlin.Int? = ..., screenY: kotlin.Int? = ..., clientX: kotlin.Int? = ..., clientY: kotlin.Int? = ..., button: kotlin.Short? = ..., buttons: kotlin.Short? = ..., relatedTarget: org.w3c.dom.events.EventTarget? = ..., region: kotlin.String? = ..., ctrlKey: kotlin.Boolean? = ..., shiftKey: kotlin.Boolean? = ..., altKey: kotlin.Boolean? = ..., metaKey: kotlin.Boolean? = ..., modifierAltGraph: kotlin.Boolean? = ..., modifierCapsLock: kotlin.Boolean? = ..., modifierFn: kotlin.Boolean? = ..., modifierFnLock: kotlin.Boolean? = ..., modifierHyper: kotlin.Boolean? = ..., modifierNumLock: kotlin.Boolean? = ..., modifierScrollLock: kotlin.Boolean? = ..., modifierSuper: kotlin.Boolean? = ..., modifierSymbol: kotlin.Boolean? = ..., modifierSymbolLock: kotlin.Boolean? = ..., view: org.w3c.dom.Window? = ..., detail: kotlin.Int? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.dom.events.WheelEventInit
/*∆*/ 
/*∆*/ public open external class CompositionEvent : org.w3c.dom.events.UIEvent {
/*∆*/     public constructor CompositionEvent(type: kotlin.String, eventInitDict: org.w3c.dom.events.CompositionEventInit = ...)
/*∆*/ 
/*∆*/     public open val data: kotlin.String { get; }
/*∆*/ 
/*∆*/     public companion object of CompositionEvent {
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
/*∆*/ public external interface CompositionEventInit : org.w3c.dom.events.UIEventInit {
/*∆*/     public open var data: kotlin.String? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class Event {
/*∆*/     public constructor Event(type: kotlin.String, eventInitDict: org.w3c.dom.EventInit = ...)
/*∆*/ 
/*∆*/     public open val bubbles: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val cancelable: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val composed: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val currentTarget: org.w3c.dom.events.EventTarget? { get; }
/*∆*/ 
/*∆*/     public open val defaultPrevented: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val eventPhase: kotlin.Short { get; }
/*∆*/ 
/*∆*/     public open val isTrusted: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val target: org.w3c.dom.events.EventTarget? { get; }
/*∆*/ 
/*∆*/     public open val timeStamp: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val type: kotlin.String { get; }
/*∆*/ 
/*∆*/     public final fun composedPath(): kotlin.Array<org.w3c.dom.events.EventTarget>
/*∆*/ 
/*∆*/     public final fun initEvent(type: kotlin.String, bubbles: kotlin.Boolean, cancelable: kotlin.Boolean): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun preventDefault(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun stopImmediatePropagation(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun stopPropagation(): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of Event {
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
/*∆*/ public external interface EventListener {
/*∆*/     public abstract fun handleEvent(event: org.w3c.dom.events.Event): kotlin.Unit
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface EventModifierInit : org.w3c.dom.events.UIEventInit {
/*∆*/     public open var altKey: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var ctrlKey: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var metaKey: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var modifierAltGraph: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var modifierCapsLock: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var modifierFn: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var modifierFnLock: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var modifierHyper: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var modifierNumLock: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var modifierScrollLock: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var modifierSuper: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var modifierSymbol: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var modifierSymbolLock: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var shiftKey: kotlin.Boolean? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class EventTarget {
/*∆*/     public constructor EventTarget()
/*∆*/ 
/*∆*/     public final fun addEventListener(type: kotlin.String, callback: ((org.w3c.dom.events.Event) -> kotlin.Unit)?, options: dynamic = ...): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun addEventListener(type: kotlin.String, callback: org.w3c.dom.events.EventListener?, options: dynamic = ...): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun dispatchEvent(event: org.w3c.dom.events.Event): kotlin.Boolean
/*∆*/ 
/*∆*/     public final fun removeEventListener(type: kotlin.String, callback: ((org.w3c.dom.events.Event) -> kotlin.Unit)?, options: dynamic = ...): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun removeEventListener(type: kotlin.String, callback: org.w3c.dom.events.EventListener?, options: dynamic = ...): kotlin.Unit
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class FocusEvent : org.w3c.dom.events.UIEvent {
/*∆*/     public constructor FocusEvent(type: kotlin.String, eventInitDict: org.w3c.dom.events.FocusEventInit = ...)
/*∆*/ 
/*∆*/     public open val relatedTarget: org.w3c.dom.events.EventTarget? { get; }
/*∆*/ 
/*∆*/     public companion object of FocusEvent {
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
/*∆*/ public external interface FocusEventInit : org.w3c.dom.events.UIEventInit {
/*∆*/     public open var relatedTarget: org.w3c.dom.events.EventTarget? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class InputEvent : org.w3c.dom.events.UIEvent {
/*∆*/     public constructor InputEvent(type: kotlin.String, eventInitDict: org.w3c.dom.events.InputEventInit = ...)
/*∆*/ 
/*∆*/     public open val data: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val isComposing: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public companion object of InputEvent {
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
/*∆*/ public external interface InputEventInit : org.w3c.dom.events.UIEventInit {
/*∆*/     public open var data: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var isComposing: kotlin.Boolean? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class KeyboardEvent : org.w3c.dom.events.UIEvent {
/*∆*/     public constructor KeyboardEvent(type: kotlin.String, eventInitDict: org.w3c.dom.events.KeyboardEventInit = ...)
/*∆*/ 
/*∆*/     public open val altKey: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val charCode: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val code: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val ctrlKey: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val isComposing: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val key: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val keyCode: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val location: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val metaKey: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val repeat: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val shiftKey: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val which: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public final fun getModifierState(keyArg: kotlin.String): kotlin.Boolean
/*∆*/ 
/*∆*/     public companion object of KeyboardEvent {
/*∆*/         public final val AT_TARGET: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val BUBBLING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CAPTURING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOM_KEY_LOCATION_LEFT: kotlin.Int { get; }
/*∆*/ 
/*∆*/         public final val DOM_KEY_LOCATION_NUMPAD: kotlin.Int { get; }
/*∆*/ 
/*∆*/         public final val DOM_KEY_LOCATION_RIGHT: kotlin.Int { get; }
/*∆*/ 
/*∆*/         public final val DOM_KEY_LOCATION_STANDARD: kotlin.Int { get; }
/*∆*/ 
/*∆*/         public final val NONE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface KeyboardEventInit : org.w3c.dom.events.EventModifierInit {
/*∆*/     public open var code: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var isComposing: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var key: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var location: kotlin.Int? { get; set; }
/*∆*/ 
/*∆*/     public open var repeat: kotlin.Boolean? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class MouseEvent : org.w3c.dom.events.UIEvent, org.w3c.dom.UnionElementOrMouseEvent {
/*∆*/     public constructor MouseEvent(type: kotlin.String, eventInitDict: org.w3c.dom.events.MouseEventInit = ...)
/*∆*/ 
/*∆*/     public open val altKey: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val button: kotlin.Short { get; }
/*∆*/ 
/*∆*/     public open val buttons: kotlin.Short { get; }
/*∆*/ 
/*∆*/     public open val clientX: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val clientY: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val ctrlKey: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val metaKey: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val offsetX: kotlin.Double { get; }
/*∆*/ 
/*∆*/     public open val offsetY: kotlin.Double { get; }
/*∆*/ 
/*∆*/     public open val pageX: kotlin.Double { get; }
/*∆*/ 
/*∆*/     public open val pageY: kotlin.Double { get; }
/*∆*/ 
/*∆*/     public open val region: kotlin.String? { get; }
/*∆*/ 
/*∆*/     public open val relatedTarget: org.w3c.dom.events.EventTarget? { get; }
/*∆*/ 
/*∆*/     public open val screenX: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val screenY: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val shiftKey: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val x: kotlin.Double { get; }
/*∆*/ 
/*∆*/     public open val y: kotlin.Double { get; }
/*∆*/ 
/*∆*/     public final fun getModifierState(keyArg: kotlin.String): kotlin.Boolean
/*∆*/ 
/*∆*/     public companion object of MouseEvent {
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
/*∆*/ public external interface MouseEventInit : org.w3c.dom.events.EventModifierInit {
/*∆*/     public open var button: kotlin.Short? { get; set; }
/*∆*/ 
/*∆*/     public open var buttons: kotlin.Short? { get; set; }
/*∆*/ 
/*∆*/     public open var clientX: kotlin.Int? { get; set; }
/*∆*/ 
/*∆*/     public open var clientY: kotlin.Int? { get; set; }
/*∆*/ 
/*∆*/     public open var region: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var relatedTarget: org.w3c.dom.events.EventTarget? { get; set; }
/*∆*/ 
/*∆*/     public open var screenX: kotlin.Int? { get; set; }
/*∆*/ 
/*∆*/     public open var screenY: kotlin.Int? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class UIEvent : org.w3c.dom.events.Event {
/*∆*/     public constructor UIEvent(type: kotlin.String, eventInitDict: org.w3c.dom.events.UIEventInit = ...)
/*∆*/ 
/*∆*/     public open val detail: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val view: org.w3c.dom.Window? { get; }
/*∆*/ 
/*∆*/     public companion object of UIEvent {
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
/*∆*/ public external interface UIEventInit : org.w3c.dom.EventInit {
/*∆*/     public open var detail: kotlin.Int? { get; set; }
/*∆*/ 
/*∆*/     public open var view: org.w3c.dom.Window? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class WheelEvent : org.w3c.dom.events.MouseEvent {
/*∆*/     public constructor WheelEvent(type: kotlin.String, eventInitDict: org.w3c.dom.events.WheelEventInit = ...)
/*∆*/ 
/*∆*/     public open val deltaMode: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val deltaX: kotlin.Double { get; }
/*∆*/ 
/*∆*/     public open val deltaY: kotlin.Double { get; }
/*∆*/ 
/*∆*/     public open val deltaZ: kotlin.Double { get; }
/*∆*/ 
/*∆*/     public companion object of WheelEvent {
/*∆*/         public final val AT_TARGET: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val BUBBLING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CAPTURING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val DOM_DELTA_LINE: kotlin.Int { get; }
/*∆*/ 
/*∆*/         public final val DOM_DELTA_PAGE: kotlin.Int { get; }
/*∆*/ 
/*∆*/         public final val DOM_DELTA_PIXEL: kotlin.Int { get; }
/*∆*/ 
/*∆*/         public final val NONE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface WheelEventInit : org.w3c.dom.events.MouseEventInit {
/*∆*/     public open var deltaMode: kotlin.Int? { get; set; }
/*∆*/ 
/*∆*/     public open var deltaX: kotlin.Double? { get; set; }
/*∆*/ 
/*∆*/     public open var deltaY: kotlin.Double? { get; set; }
/*∆*/ 
/*∆*/     public open var deltaZ: kotlin.Double? { get; set; }
/*∆*/ }