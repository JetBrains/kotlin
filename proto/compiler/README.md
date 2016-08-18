# ProtoKot compiler

## Building 

To build Protobuf Compiler (here and below "protoc") you should have Google Protobuf libraries installed.
You can check if your system meet requirements launching "pre-build" script

    $ proto/compiler/pre-build.sh
    $ make

Script will ask you for your permission to install prerequisites automatically. 

Note that this process requires pulling official google-protobuf repository from Github, then building it from scratch (which, in turn,
can require addition packages installation, like libtool, autoreconf, etc), and then installing it into your system. Make sure that you 
have superuser permissions.

After you have finished installation of all prerequisites, you can launch Makefile that will build ProtoKot library.

    $ cd proto/compiler/pre-build.sh
    $ make

This will produce build/ folder, where you can find three artifacts: executable proto-compiler protoc, runtime library protokot-runtime.jar
and symlink to runtime sources folder (in case you want to link against source files, not jar)

    $ cd build
    $ ls
    protoc*  protokot-runtime.jar  sources@

Usage of ProtoKot consists of two main steps:
1. Compiling .proto-file in .kt-file with protoc
2. Linking your code against Protokot Runtime Library (needed by generated classes) 

## Using compiler

    $ ./protoc --kotlin_out=$(DST_DIR) $(PATH_TO_PROTO)/$(PROTO_FILE) -I $(PATH_TO_PROTO)

where $(DST_DIR) stands for path to place, where generated files should be stored, $(PATH_TO_PROTO) stands for path to .proto-file, and $(PROTO_FILE) 
is .proto-file name. Note that you should specify $(PATH_TO_PROTO) second time using "-I" directive.


## Using generated code

Example:

```java
// All messages are immutable. Use Builders for creating new messages
// Currently you have to pass default arguments to Builders constructor by yourself. 
// This will be changed in future.
val msg = Person.BuilderPerson("", 0, "")
            // all setters return "this" builder so you can chain modifiers in LINQ-style
            .setEmail("wtf@dasda.com")      
            .setId(42)
            .setName("John Doe")
            // don't forget to call build() to produce message
            .build()                            

// Messages work only with CodedStream classes, provided by ProtoKot-runtime library.
// You can create CodedStream passing reference to ByteArray.
// To get serialized size of message (in bytes) use Message.getSizeNoTag() method
val byteArray = ByteArray(msg.getSizeNoTag())
val outs = CodedOutputStream(byteArray)
msg.writeTo(outs)

// InputStreams are created in the same manner.
// WARNING! You have to pass reference to the buffer containing *ONLY* message and *NOTHING* except the message.
// That mean, trailing cells containing some trash are forbidden - 
// - you will be getting errors if you try to parse message from such buffer.
var ins = CodedInputStream(byteArray)

// Parse message from input stream
var readMsg = Person.BuilderPerson(0, "", 0).parseFrom(ins).build()

// Check is parse succeeded 
// Note that currently exceptions are not supported, errors are reported using Message.errorCode field
// You can find description of error codes in carkot/proto/compiler/error_codes.txt
assert(readMsg.errorCode == 0)
assert(msg == readMsg)
```

