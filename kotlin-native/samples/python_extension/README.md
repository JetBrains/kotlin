# C to Kotlin/Native interoperability example

This example shows how to use Kotlin/Native programs from other execution environments, such as Python.
Python has C native interface, which could be used to organize a bridge with the
Kotlin/Native application. File `kotlin_bridge.c` contains translation code between Kotlin and Python
lands. This demo works on Linux, Windows (64-bit) or macOS hosts.

To build and run the sample do the following:

*   Install Python with development headers, i.e. on Linux
    ```
    sudo apt install python-dev
    ```

*   Run `build.sh`, it will ask for superuser password to install Python extension.
    Use build.bat on Windows.
*   On macOS copy Kotlin binary to extension's directory and change install name with
    `install_name_tool` tool, i.e.
    ```
    sudo cp ./build/libserver.dylib  /Library/Python/2.7/site-packages/
    sudo install_name_tool /Library/Python/2.7/site-packages/kotlin_bridge.so \
          -change libserver.dylib @loader_path/libserver.dylib
    ```
*   On Linux copy Kotlin binary in some place where libraries could be loaded from, i.e.
    ```
    cp ./build/libserver.so /usr/local/lib/
    ldconfig
    ```
    or modify dynamic loader search path with
    ```
    export LD_LIBRARY_PATH=`pwd`
    ```

*   run Python code using Kotlin functionality with
    ```
    python src/main/python/main.py
    ```
*   it will show you result of using several Kotlin/Native APIs, accepting and returning both objects and
    primitive types

 The example works as following. Kotlin/Native API is implemented in `Server.kt`, and we run Kotlin/Native compiler
 with `-produce dynamic` option. Compiler produces two artifacts: `server_api.h` which is C language API
 to all public functions and classes available in the application. `libserver.dylib` or `libserver.so` or `server.dll`
 shared object contains C bridge to all above APIs.

  This C bridge looks like a C struct, reflecting all scopes in program, with operations available. For example,
  for class Server
```c_cpp
   class Server(val prefix: String) {
          fun greet(session: Session) = "$prefix: Hello from Kotlin/Native in ${session}"
          fun concat(session: Session, a: String, b: String) = "$prefix: $a $b in ${session}"
          fun add(session: Session, a: Int, b: Int) = a + b + session.number
   }
```
   following C API is produced
```c_cpp
   typedef struct {
     server_KNativePtr pinned;
   } server_kref_demo_Session;
   typedef struct {
     server_KNativePtr pinned;
   } server_kref_demo_Server;

   typedef struct {
      /* Service functions. */
      void (*DisposeStablePointer)(server_KNativePtr ptr);
      void (*DisposeString)(const char* string);
      server_KBoolean (*IsInstance)(server_KNativePtr ref, const server_KType* type);

      /* User functions. */
      struct {
        struct {
          struct {
            server_KType* (*_type)(void);
            server_kref_demo_Session (*Session)(const char* name, server_KInt number);
          } Session;
          struct {
            server_KType* (*_type)(void);
            server_kref_demo_Server (*Server)(const char* prefix);
            const char* (*greet)(server_kref_demo_Server thiz, server_kref_demo_Session session);
            const char* (*concat)(server_kref_demo_Server thiz, server_kref_demo_Session session, const char* a, const char* b);
            server_KInt (*add)(server_kref_demo_Server thiz, server_kref_demo_Session session, server_KInt a, server_KInt b);
          } Server;
        } demo;
      } kotlin;
   } server_ExportedSymbols;
   extern server_ExportedSymbols* server_symbols(void);
```

 So every class instance is represented with a single element structure, encapsulating stable pointer to an instance.
 Once no longer needed, `DisposeStablePointer()` with that stable pointer shall be called, and if value is not stored
 somewhere else - it is disposed. For primitive types and `kotlin.String` smart bridges converting to C primitive types
 or to C strings (which has to be manually freed with `DisposeString()`) are implemented.

 For example, running constructor of class Server taking a string will look like

    server_kref_demo_Server server = server_symbols()->kotlin.demo.Server.Server("the server");

 And disposing no longer needed instance will look like

    server_symbols()->DisposeStablePointer(server.pinned);

 To make code easier readable, macro definitions like

    #define T_(name) server_kref_demo_ ## name
    #define __ server_symbols()->

 will transform above, overly verbose lines to more readable

    T_(Server) server = __ kotlin.demo.Server.Server("the server");

 `_type()` function will return opaque type pointer, which could be checked with `IsInstance()` operation, like

    __ IsInstance(ref.pinned, __ kotlin.demo.Server._type())
