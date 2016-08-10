#include <google/protobuf/compiler/command_line_interface.h>
#include <google/protobuf/compiler/cpp/cpp_generator.h>
#include <google/protobuf/compiler/python/python_generator.h>
#include <google/protobuf/compiler/java/java_generator.h>
#include <google/protobuf/compiler/javanano/javanano_generator.h>
#include <google/protobuf/compiler/ruby/ruby_generator.h>
#include <google/protobuf/compiler/csharp/csharp_generator.h>
#include <google/protobuf/compiler/objectivec/objectivec_generator.h>
#include <google/protobuf/compiler/js/js_generator.h>
#include "kotlin_file_generator.h"
#include <iostream>

// TODO attacch Kotlin generator to protoc as plugin.
// Don't rewrite protoc main.
int main(int argc, const char* const * argv) {
  google::protobuf::compiler::CommandLineInterface cli;
  cli.AllowPlugins("protoc-");

  // Proto2 C++
  google::protobuf::compiler::cpp::CppGenerator cpp_generator;
  cli.RegisterGenerator("--cpp_out", "--cpp_opt", &cpp_generator,
                        "Generate C++ header and source.");

  // Proto2 Java
  google::protobuf::compiler::java::JavaGenerator java_generator;
  cli.RegisterGenerator("--java_out", &java_generator,
                        "Generate Java source file.");


  // Proto2 Python
  google::protobuf::compiler::python::Generator py_generator;
  cli.RegisterGenerator("--python_out", &py_generator,
                        "Generate Python source file.");

  // Java Nano
  google::protobuf::compiler::javanano::JavaNanoGenerator javanano_generator;
  cli.RegisterGenerator("--javanano_out", &javanano_generator,
                        "Generate Java Nano source file.");

  // TODO(teboring): Add it back when php implementation is ready
  // PHP
  // google::protobuf::compiler::php::Generator php_generator;
  // cli.RegisterGenerator("--php_out", &php_generator,
  //                      "Generate PHP source file.");

  // Ruby
  google::protobuf::compiler::ruby::Generator rb_generator;
  cli.RegisterGenerator("--ruby_out", &rb_generator,
                        "Generate Ruby source file.");

  // CSharp
  google::protobuf::compiler::csharp::Generator csharp_generator;
  cli.RegisterGenerator("--csharp_out", "--csharp_opt", &csharp_generator,
                        "Generate C# source file.");

  // Objective C
  google::protobuf::compiler::objectivec::ObjectiveCGenerator objc_generator;
  cli.RegisterGenerator("--objc_out", "--objc_opt", &objc_generator,
                        "Generate Objective C header and source.");

  // JavaScript
  google::protobuf::compiler::js::Generator js_generator;
  cli.RegisterGenerator("--js_out", &js_generator,
                        "Generate JavaScript source.");

  // Kotlin
  google::protobuf::compiler::kotlin::FileGenerator kotlinGenerator;
  cli.RegisterGenerator("--kotlin_out", &kotlinGenerator,
                        "Generate Kotlin source file.");

  return cli.Run(argc, argv);

}