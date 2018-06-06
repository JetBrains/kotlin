namespace org.w3c.dom.events;


// Downloaded from http://www.w3.org/TR/uievents/
[Constructor(DOMString type, optional UIEventInit eventInitDict)]
interface UIEvent : Event {
  readonly attribute Window? view;
  readonly attribute long detail;
};
dictionary UIEventInit : EventInit {
  Window? view = null;
  long detail = 0;
};
[Constructor(DOMString type, optional FocusEventInit eventInitDict)]
interface FocusEvent : UIEvent {
  readonly attribute EventTarget? relatedTarget;
};
dictionary FocusEventInit : UIEventInit {
  EventTarget? relatedTarget = null;
};
[Constructor(DOMString type, optional MouseEventInit eventInitDict)]
interface MouseEvent : UIEvent {
  readonly attribute long screenX;
  readonly attribute long screenY;
  readonly attribute long clientX;
  readonly attribute long clientY;

  readonly attribute boolean ctrlKey;
  readonly attribute boolean shiftKey;
  readonly attribute boolean altKey;
  readonly attribute boolean metaKey;

  readonly attribute short button;
  readonly attribute unsigned short buttons;

  readonly attribute EventTarget? relatedTarget;

  boolean getModifierState(DOMString keyArg);
};
dictionary MouseEventInit : EventModifierInit {
  long screenX = 0;
  long screenY = 0;
  long clientX = 0;
  long clientY = 0;

  short button = 0;
  unsigned short buttons = 0;
  EventTarget? relatedTarget = null;
};
dictionary EventModifierInit : UIEventInit {
  boolean ctrlKey = false;
  boolean shiftKey = false;
  boolean altKey = false;
  boolean metaKey = false;

  boolean modifierAltGraph = false;
  boolean modifierCapsLock = false;
  boolean modifierFn = false;
  boolean modifierFnLock = false;
  boolean modifierHyper = false;
  boolean modifierNumLock = false;
  boolean modifierScrollLock = false;
  boolean modifierSuper = false;
  boolean modifierSymbol = false;
  boolean modifierSymbolLock = false;
};
[Constructor(DOMString type, optional WheelEventInit eventInitDict)]
interface WheelEvent : MouseEvent {
  // DeltaModeCode
  const unsigned long DOM_DELTA_PIXEL = 0x00;
  const unsigned long DOM_DELTA_LINE  = 0x01;
  const unsigned long DOM_DELTA_PAGE  = 0x02;

  readonly attribute double deltaX;
  readonly attribute double deltaY;
  readonly attribute double deltaZ;
  readonly attribute unsigned long deltaMode;
};
dictionary WheelEventInit : MouseEventInit {
  double deltaX = 0.0;
  double deltaY = 0.0;
  double deltaZ = 0.0;
  unsigned long deltaMode = 0;
};
[Constructor(DOMString type, optional InputEventInit eventInitDict)]
interface InputEvent : UIEvent {
  readonly attribute DOMString data;
  readonly attribute boolean isComposing;
};
dictionary InputEventInit : UIEventInit {
  DOMString data = "";
  boolean isComposing = false;
};
[Constructor(DOMString type, optional KeyboardEventInit eventInitDict)]
interface KeyboardEvent : UIEvent {
  // KeyLocationCode
  const unsigned long DOM_KEY_LOCATION_STANDARD = 0x00;
  const unsigned long DOM_KEY_LOCATION_LEFT = 0x01;
  const unsigned long DOM_KEY_LOCATION_RIGHT = 0x02;
  const unsigned long DOM_KEY_LOCATION_NUMPAD = 0x03;

  readonly attribute DOMString key;
  readonly attribute DOMString code;
  readonly attribute unsigned long location;

  readonly attribute boolean ctrlKey;
  readonly attribute boolean shiftKey;
  readonly attribute boolean altKey;
  readonly attribute boolean metaKey;

  readonly attribute boolean repeat;
  readonly attribute boolean isComposing;

  boolean getModifierState(DOMString keyArg);
};
dictionary KeyboardEventInit : EventModifierInit {
  DOMString key = "";
  DOMString code = "";
  unsigned long location = 0;
  boolean repeat = false;
  boolean isComposing = false;
};
[Constructor(DOMString type, optional CompositionEventInit eventInitDict)]
interface CompositionEvent : UIEvent {
  readonly attribute DOMString data;
};
dictionary CompositionEventInit : UIEventInit {
  DOMString data = "";
};
partial interface KeyboardEvent {
  // The following support legacy user agents
  readonly attribute unsigned long charCode;
  readonly attribute unsigned long keyCode;
  readonly attribute unsigned long which;
};
[Constructor(DOMString type, optional UIEventInit eventInitDict)]
interface UIEvent : Event {
  readonly attribute Window? view;
  readonly attribute long detail;
};

dictionary UIEventInit : EventInit {
  Window? view = null;
  long detail = 0;
};

[Constructor(DOMString type, optional FocusEventInit eventInitDict)]
interface FocusEvent : UIEvent {
  readonly attribute EventTarget? relatedTarget;
};

dictionary FocusEventInit : UIEventInit {
  EventTarget? relatedTarget = null;
};

[Constructor(DOMString type, optional MouseEventInit eventInitDict)]
interface MouseEvent : UIEvent {
  readonly attribute long screenX;
  readonly attribute long screenY;
  readonly attribute long clientX;
  readonly attribute long clientY;

  readonly attribute boolean ctrlKey;
  readonly attribute boolean shiftKey;
  readonly attribute boolean altKey;
  readonly attribute boolean metaKey;

  readonly attribute short button;
  readonly attribute unsigned short buttons;

  readonly attribute EventTarget? relatedTarget;

  boolean getModifierState(DOMString keyArg);
};

dictionary MouseEventInit : EventModifierInit {
  long screenX = 0;
  long screenY = 0;
  long clientX = 0;
  long clientY = 0;

  short button = 0;
  unsigned short buttons = 0;
  EventTarget? relatedTarget = null;
};

dictionary EventModifierInit : UIEventInit {
  boolean ctrlKey = false;
  boolean shiftKey = false;
  boolean altKey = false;
  boolean metaKey = false;

  boolean modifierAltGraph = false;
  boolean modifierCapsLock = false;
  boolean modifierFn = false;
  boolean modifierFnLock = false;
  boolean modifierHyper = false;
  boolean modifierNumLock = false;
  boolean modifierScrollLock = false;
  boolean modifierSuper = false;
  boolean modifierSymbol = false;
  boolean modifierSymbolLock = false;
};

[Constructor(DOMString type, optional WheelEventInit eventInitDict)]
interface WheelEvent : MouseEvent {
  // DeltaModeCode
  const unsigned long DOM_DELTA_PIXEL = 0x00;
  const unsigned long DOM_DELTA_LINE  = 0x01;
  const unsigned long DOM_DELTA_PAGE  = 0x02;

  readonly attribute double deltaX;
  readonly attribute double deltaY;
  readonly attribute double deltaZ;
  readonly attribute unsigned long deltaMode;
};

dictionary WheelEventInit : MouseEventInit {
  double deltaX = 0.0;
  double deltaY = 0.0;
  double deltaZ = 0.0;
  unsigned long deltaMode = 0;
};

[Constructor(DOMString type, optional InputEventInit eventInitDict)]
interface InputEvent : UIEvent {
  readonly attribute DOMString data;
  readonly attribute boolean isComposing;
};

dictionary InputEventInit : UIEventInit {
  DOMString data = "";
  boolean isComposing = false;
};

[Constructor(DOMString type, optional KeyboardEventInit eventInitDict)]
interface KeyboardEvent : UIEvent {
  // KeyLocationCode
  const unsigned long DOM_KEY_LOCATION_STANDARD = 0x00;
  const unsigned long DOM_KEY_LOCATION_LEFT = 0x01;
  const unsigned long DOM_KEY_LOCATION_RIGHT = 0x02;
  const unsigned long DOM_KEY_LOCATION_NUMPAD = 0x03;

  readonly attribute DOMString key;
  readonly attribute DOMString code;
  readonly attribute unsigned long location;

  readonly attribute boolean ctrlKey;
  readonly attribute boolean shiftKey;
  readonly attribute boolean altKey;
  readonly attribute boolean metaKey;

  readonly attribute boolean repeat;
  readonly attribute boolean isComposing;

  boolean getModifierState(DOMString keyArg);
};

dictionary KeyboardEventInit : EventModifierInit {
  DOMString key = "";
  DOMString code = "";
  unsigned long location = 0;
  boolean repeat = false;
  boolean isComposing = false;
};

[Constructor(DOMString type, optional CompositionEventInit eventInitDict)]
interface CompositionEvent : UIEvent {
  readonly attribute DOMString data;
};

dictionary CompositionEventInit : UIEventInit {
  DOMString data = "";
};

partial interface KeyboardEvent {
  // The following support legacy user agents
  readonly attribute unsigned long charCode;
  readonly attribute unsigned long keyCode;
  readonly attribute unsigned long which;
};

