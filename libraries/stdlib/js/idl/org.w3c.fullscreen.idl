namespace org.w3c.fullscreen;


// Downloaded from https://raw.githubusercontent.com/whatwg/fullscreen/master/fullscreen.html
partial interface Element {
  Promise<void> requestFullscreen();
};

partial interface Document {
  [LenientSetter] readonly attribute boolean fullscreenEnabled;
  [LenientSetter] readonly attribute boolean fullscreen; // historical

  Promise<void> exitFullscreen();

  attribute EventHandler onfullscreenchange;
  attribute EventHandler onfullscreenerror;
};

partial interface DocumentOrShadowRoot {
  [LenientSetter] readonly attribute Element? fullscreenElement;
};

