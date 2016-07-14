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

//#define KOTLIN_GENERATED_CODE_LANGUAGE_LEVEL_LOW
int main(int argc, const char* const * argv) {
  google::protobuf::compiler::CommandLineInterface cli;

  // Support generation of C++ source and headers.
  google::protobuf::compiler::cpp::CppGenerator cpp_generator;
  cli.RegisterGenerator("--cpp_out", &cpp_generator,
    "Generate C++ source and header.");

  google::protobuf::compiler::kotlin::FileGenerator kotlinGenerator;
  cli.RegisterGenerator("--kotlin_out", &kotlinGenerator,
    "Generate Foo file.");

  return cli.Run(argc, argv);
}