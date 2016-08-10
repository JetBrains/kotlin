//
// Created by user on 7/11/16.
//

#ifndef SRC_KOTLIN_FILE_GENERATOR_H
#define SRC_KOTLIN_FILE_GENERATOR_H

#include "kotlin_class_generator.h"
#include <vector>
#include <google/protobuf/io/printer.h>
#include <google/protobuf/compiler/code_generator.h>

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {


class FileGenerator : public CodeGenerator {
public:
    FileGenerator();
    ~FileGenerator();

    // implements CodeGenerator ----------------------------------------
    bool Generate(const FileDescriptor* file,
                  const string& parameter,
                  GeneratorContext* context,
                  string* error) const;
private:
	// TODO check code style
    static void generateCode(io::Printer *, std::vector<ClassGenerator *> &);
};

} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google
#endif //SRC_KOTLIN_FILE_GENERATOR_H
