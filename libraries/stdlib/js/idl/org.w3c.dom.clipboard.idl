namespace org.w3c.dom.clipboard;


// Downloaded from https://w3c.github.io/clipboard-apis
dictionary ClipboardEventInit : EventInit {
  DataTransfer? clipboardData = null;
};
[Constructor(DOMString type, optional ClipboardEventInit eventInitDict), Exposed=Window]
interface ClipboardEvent : Event {
  readonly attribute DataTransfer? clipboardData;
};
partial interface Navigator {
  [SecureContext, SameObject] readonly attribute Clipboard clipboard;
};
[SecureContext, Exposed=Window] interface Clipboard : EventTarget {
  Promise<DataTransfer> read();
  Promise<DOMString> readText();
  Promise<void> write(DataTransfer data);
  Promise<void> writeText(DOMString data);
};
dictionary ClipboardPermissionDescriptor : PermissionDescriptor {
  boolean allowWithoutGesture = false;
};
dictionary ClipboardEventInit : EventInit {
  DataTransfer? clipboardData = null;
};

[Constructor(DOMString type, optional ClipboardEventInit eventInitDict), Exposed=Window]
interface ClipboardEvent : Event {
  readonly attribute DataTransfer? clipboardData;
};

partial interface Navigator {
  [SecureContext, SameObject] readonly attribute Clipboard clipboard;
};

[SecureContext, Exposed=Window] interface Clipboard : EventTarget {
  Promise<DataTransfer> read();
  Promise<DOMString> readText();
  Promise<void> write(DataTransfer data);
  Promise<void> writeText(DOMString data);
};

dictionary ClipboardPermissionDescriptor : PermissionDescriptor {
  boolean allowWithoutGesture = false;
};

