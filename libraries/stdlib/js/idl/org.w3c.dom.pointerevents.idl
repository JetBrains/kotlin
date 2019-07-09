namespace org.w3c.dom.pointerevents;


// Downloaded from http://www.w3.org/TR/pointerevents/
dictionary PointerEventInit : MouseEventInit {
    long pointerId = 0;
    double width = 1;
    double height = 1;
    float pressure = 0;
    float tangentialPressure = 0;
    long tiltX = 0;
    long tiltY = 0;
    long twist = 0;
    DOMString pointerType = "";
    boolean isPrimary = false;
};

[Constructor(DOMString type, optional PointerEventInit eventInitDict), Exposed=Window]
interface PointerEvent : MouseEvent {
    readonly attribute long pointerId;
    readonly attribute double width;
    readonly attribute double height;
    readonly attribute float pressure;
    readonly attribute float tangentialPressure;
    readonly attribute long tiltX;
    readonly attribute long tiltY;
    readonly attribute long twist;
    readonly attribute DOMString pointerType;
    readonly attribute boolean isPrimary;
};

partial interface Element {
    void setPointerCapture(long pointerId);
    void releasePointerCapture(long pointerId);
    boolean hasPointerCapture(long pointerId);
};

partial interface GlobalEventHandlers {
    attribute EventHandler ongotpointercapture;
    attribute EventHandler onlostpointercapture;
    attribute EventHandler onpointerdown;
    attribute EventHandler onpointermove;
    attribute EventHandler onpointerup;
    attribute EventHandler onpointercancel;
    attribute EventHandler onpointerover;
    attribute EventHandler onpointerout;
    attribute EventHandler onpointerenter;
    attribute EventHandler onpointerleave;
};

partial interface Navigator {
    readonly attribute long maxTouchPoints;
};

