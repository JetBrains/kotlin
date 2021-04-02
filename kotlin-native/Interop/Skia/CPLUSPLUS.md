Current status
==============

Limited C++ interop provided via plain C wrappers and cinterop mechanism.

Usage:
------

Add `Language = C++` to .def file or `compilerOpts = -x c++` to command line or def file.

C++ features
------------

* C++ class:
  + Virtual and non-virtual methods
  * Static methods mapped as companion ones
  * Fields
  * Static fields as companion ones
  * Constructors are mapped to `__init__(args)` companion methods
  * Destructor is mapped to `__destroy__(this)` companion method
* LValueReference parameters and return value internally handled as pointers
* Namespaces provided as simple mangled class name. It works but awfully ugly. Shall be fixed by mapping to packages.
* Nested C++ classes: same as namespaces, simple mangling. Shall be fixed.
* Access modifiers: only public members exposed to Kotlin. Anonymous namespaces are silently ignored.

Known issues
------------

* C++ object with nontrivial copy constructor may work incorrect when used as by value parameter or return typr. 
"Nontrivial" in this context relates to objects which bahavior depends on memory location. Most of other non-POD objects may be handled well.
In fact, "by value" return type is mapped to CValue which is immutrable movable block. Particularly, non-const methods invoked with CValue receiver
via useContents mechanism run on temporary object and therefore does not modify the original copy. TBD.

Limitations
-----------

* Operators are not supported yet (silently ignored)
* LVReference is mapped to CPointer<T>? which is incorrect (should be notNull). This may cause segmentation fault in case of null would be sent as a parameter. TBD
* const overload not supported and cause compilation error. That is, two class methods with the same signature (`const` and `non-const`) can't be compiled. The same for function parameters: if two functions differ only in `const*` modifier of parameter, this will cause "conflicting overloads" error. TBD.
* C++ lambda type is not supported yet.
* Member pointer, member reference, rvalue reference and some other types are not wupported.
* Inheritance is not implemented yet. C++-style callbacks (overriding virtual method in Kotlin) may be implemented via plain C bridge (this can be done by hand as a workaround). TBD.

