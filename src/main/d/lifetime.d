/**
 * The D language investigations:
 * here life events except perhaps allocator stage ()
 * # dmd -v -vgc -I/opt/local//libexec/dmd-bootstrap/include/druntime/import -I/opt/local/libexec/dmd-bootstrap/include/phobos -L-L/opt/local/libexec/dmd-bootstrap/lib lifetime.d
 * Expected output for 
 * bash-3.2$ dmd --version
 * DMD64 D Compiler v2.070-devel
 * Copyright (c) 1999-2015 by Digital Mars written by Walter Bright
 * 

 * bash-3.2$ ./lifetime
A::static_this: start
A::static_this: end
main:start
(4557316096)lifetime.A::this: start
(4557316096)lifetime.A::this: end
(4557316112)lifetime.A::this: start
(4557316112)lifetime.A::this: end
inner scope started
(140734540366608)lifetime.A::this: start
(140734540366608)lifetime.A::this: end
(140734540366608)lifetime.A::~this: start
(140734540366608)lifetime.A::~this: end
inner scope ended
(4557316096)lifetime.A::~this: start
(4557316096)lifetime.A::~this: end
scopedAllocations::called
scopedAllocations::start
(140734540366432)lifetime.A::this: start
(140734540366432)lifetime.A::this: end
(140734540366464)lifetime.A::this: start
(140734540366464)lifetime.A::this: end
(140734540366496)lifetime.A::this: start
(140734540366496)lifetime.A::this: end
scopedAllocations::end
(140734540366496)lifetime.A::~this: start
(140734540366496)lifetime.A::~this: end
(140734540366464)lifetime.A::~this: start
(140734540366464)lifetime.A::~this: end
(140734540366432)lifetime.A::~this: start
(140734540366432)lifetime.A::~this: end
scopedAllocations::returned
main:end
A::~static this: start
A::~static this: end
(4557316112)lifetime.A::~this: start
(4557316112)lifetime.A::~this: end
 */
import object;
import std.string;
import std.stdio;
import std.typecons;

class A {
  static this() {
    writeln("A::static_this: start");
    writeln("A::static_this: end");
  }
  this() {
    trace("this: start", this);
    trace("this: end", this);
  }
  
  ~this() {
    trace("~this: start", this);
    trace("~this: end", this);
  }

  static ~this() {
    writeln("A::~static this: start");
    writeln("A::~static this: end");
  }
}

void trace(string msg, Object obj) {
  write("(");
  write(obj.toHash());
  write(")");
  write(obj.classinfo.toString());
  write("::");
  writeln(msg);
}


void scopedAllocations() {
  writeln("scopedAllocations::start");
  auto a = scoped!A();
  auto b = scoped!A();
  auto c = scoped!A();
    writeln("scopedAllocations::end");
}

void main() {
     writeln("main:start");
     auto a = new A();
     auto c = new A();
     auto d = Object.factory("A");
     {
       writeln("inner scope started");
       auto b = scoped!A();
     }
     writeln("inner scope ended");
     delete a;
     writeln("scopedAllocations::called");
     scopedAllocations();
     writeln("scopedAllocations::returned");
     writeln("main:end");
}

extern(C) {
  /**
   * ABI compatibility, known bug in 2.70 :)
   */
  void __dmd_personality_v0(){}
  void _d_throwdwarf(){}
  void __dmd_begin_catch(){}
}
