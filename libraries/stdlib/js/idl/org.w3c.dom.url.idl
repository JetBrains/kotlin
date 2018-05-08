namespace org.w3c.dom.url;


// Downloaded from https://raw.githubusercontent.com/whatwg/url/master/url.html
[Constructor(USVString url, optional USVString base),
 Exposed=(Window,Worker)]
interface URL {
  static USVString domainToASCII(USVString domain);
  static USVString domainToUnicode(USVString domain);

  stringifier attribute USVString href;
  readonly attribute USVString origin;
           attribute USVString protocol;
           attribute USVString username;
           attribute USVString password;
           attribute USVString host;
           attribute USVString hostname;
           attribute USVString port;
           attribute USVString pathname;
           attribute USVString search;
  readonly attribute URLSearchParams searchParams;
           attribute USVString hash;
};
[Constructor(optional (USVString or URLSearchParams) init = ""),
 Exposed=(Window,Worker)]
interface URLSearchParams {
  void append(USVString name, USVString value);
  void delete(USVString name);
  USVString? get(USVString name);
  sequence<USVString> getAll(USVString name);
  boolean has(USVString name);
  void set(USVString name, USVString value);
  iterable<USVString, USVString>;
  stringifier;
};
[Constructor(USVString url, optional USVString base),
 Exposed=(Window,Worker)]
interface URL {
  static USVString domainToASCII(USVString domain);
  static USVString domainToUnicode(USVString domain);

  stringifier attribute USVString href;
  readonly attribute USVString origin;
           attribute USVString protocol;
           attribute USVString username;
           attribute USVString password;
           attribute USVString host;
           attribute USVString hostname;
           attribute USVString port;
           attribute USVString pathname;
           attribute USVString search;
  readonly attribute URLSearchParams searchParams;
           attribute USVString hash;
};

[Constructor(optional (USVString or URLSearchParams) init = ""),
 Exposed=(Window,Worker)]
interface URLSearchParams {
  void append(USVString name, USVString value);
  void delete(USVString name);
  USVString? get(USVString name);
  sequence<USVString> getAll(USVString name);
  boolean has(USVString name);
  void set(USVString name, USVString value);
  iterable<USVString, USVString>;
  stringifier;
};

