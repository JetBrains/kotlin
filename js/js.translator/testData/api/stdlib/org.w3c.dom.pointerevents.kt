package org.w3c.dom.pointerevents

@kotlin.internal.InlineOnly public inline fun PointerEventInit(/*0*/ pointerId: kotlin.Int? = ..., /*1*/ width: kotlin.Double? = ..., /*2*/ height: kotlin.Double? = ..., /*3*/ pressure: kotlin.Float? = ..., /*4*/ tangentialPressure: kotlin.Float? = ..., /*5*/ tiltX: kotlin.Int? = ..., /*6*/ tiltY: kotlin.Int? = ..., /*7*/ twist: kotlin.Int? = ..., /*8*/ pointerType: kotlin.String? = ..., /*9*/ isPrimary: kotlin.Boolean? = ..., /*10*/ screenX: kotlin.Int? = ..., /*11*/ screenY: kotlin.Int? = ..., /*12*/ clientX: kotlin.Int? = ..., /*13*/ clientY: kotlin.Int? = ..., /*14*/ button: kotlin.Short? = ..., /*15*/ buttons: kotlin.Short? = ..., /*16*/ relatedTarget: org.w3c.dom.events.EventTarget? = ..., /*17*/ region: kotlin.String? = ..., /*18*/ ctrlKey: kotlin.Boolean? = ..., /*19*/ shiftKey: kotlin.Boolean? = ..., /*20*/ altKey: kotlin.Boolean? = ..., /*21*/ metaKey: kotlin.Boolean? = ..., /*22*/ modifierAltGraph: kotlin.Boolean? = ..., /*23*/ modifierCapsLock: kotlin.Boolean? = ..., /*24*/ modifierFn: kotlin.Boolean? = ..., /*25*/ modifierFnLock: kotlin.Boolean? = ..., /*26*/ modifierHyper: kotlin.Boolean? = ..., /*27*/ modifierNumLock: kotlin.Boolean? = ..., /*28*/ modifierScrollLock: kotlin.Boolean? = ..., /*29*/ modifierSuper: kotlin.Boolean? = ..., /*30*/ modifierSymbol: kotlin.Boolean? = ..., /*31*/ modifierSymbolLock: kotlin.Boolean? = ..., /*32*/ view: org.w3c.dom.Window? = ..., /*33*/ detail: kotlin.Int? = ..., /*34*/ bubbles: kotlin.Boolean? = ..., /*35*/ cancelable: kotlin.Boolean? = ..., /*36*/ composed: kotlin.Boolean? = ...): org.w3c.dom.pointerevents.PointerEventInit

public open external class PointerEvent : org.w3c.dom.events.MouseEvent {
    /*primary*/ public constructor PointerEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.dom.pointerevents.PointerEventInit = ...)
    public open val height: kotlin.Double
        public open fun <get-height>(): kotlin.Double
    public open val isPrimary: kotlin.Boolean
        public open fun <get-isPrimary>(): kotlin.Boolean
    public open val pointerId: kotlin.Int
        public open fun <get-pointerId>(): kotlin.Int
    public open val pointerType: kotlin.String
        public open fun <get-pointerType>(): kotlin.String
    public open val pressure: kotlin.Float
        public open fun <get-pressure>(): kotlin.Float
    public open val tangentialPressure: kotlin.Float
        public open fun <get-tangentialPressure>(): kotlin.Float
    public open val tiltX: kotlin.Int
        public open fun <get-tiltX>(): kotlin.Int
    public open val tiltY: kotlin.Int
        public open fun <get-tiltY>(): kotlin.Int
    public open val twist: kotlin.Int
        public open fun <get-twist>(): kotlin.Int
    public open val width: kotlin.Double
        public open fun <get-width>(): kotlin.Double

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

public external interface PointerEventInit : org.w3c.dom.events.MouseEventInit {
    public open var height: kotlin.Double?
        public open fun <get-height>(): kotlin.Double?
        public open fun <set-height>(/*0*/ value: kotlin.Double?): kotlin.Unit
    public open var isPrimary: kotlin.Boolean?
        public open fun <get-isPrimary>(): kotlin.Boolean?
        public open fun <set-isPrimary>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var pointerId: kotlin.Int?
        public open fun <get-pointerId>(): kotlin.Int?
        public open fun <set-pointerId>(/*0*/ value: kotlin.Int?): kotlin.Unit
    public open var pointerType: kotlin.String?
        public open fun <get-pointerType>(): kotlin.String?
        public open fun <set-pointerType>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var pressure: kotlin.Float?
        public open fun <get-pressure>(): kotlin.Float?
        public open fun <set-pressure>(/*0*/ value: kotlin.Float?): kotlin.Unit
    public open var tangentialPressure: kotlin.Float?
        public open fun <get-tangentialPressure>(): kotlin.Float?
        public open fun <set-tangentialPressure>(/*0*/ value: kotlin.Float?): kotlin.Unit
    public open var tiltX: kotlin.Int?
        public open fun <get-tiltX>(): kotlin.Int?
        public open fun <set-tiltX>(/*0*/ value: kotlin.Int?): kotlin.Unit
    public open var tiltY: kotlin.Int?
        public open fun <get-tiltY>(): kotlin.Int?
        public open fun <set-tiltY>(/*0*/ value: kotlin.Int?): kotlin.Unit
    public open var twist: kotlin.Int?
        public open fun <get-twist>(): kotlin.Int?
        public open fun <set-twist>(/*0*/ value: kotlin.Int?): kotlin.Unit
    public open var width: kotlin.Double?
        public open fun <get-width>(): kotlin.Double?
        public open fun <set-width>(/*0*/ value: kotlin.Double?): kotlin.Unit
}