//
// Created by user on 7/11/16.
//

#include <google/protobuf/descriptor.h>
#include <google/protobuf/stubs/shared_ptr.h>
#include <google/protobuf/io/zero_copy_stream.h>
#ifndef _SHARED_PTR_H
#include <google/protobuf/stubs/shared_ptr.h>
#endif
#include "kotlin_file_generator.h"
#include <iostream>

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

void FileGenerator::generateCode(io::Printer *printer, std::vector<ClassGenerator *> & classes) const {
    for (int i = 0; i < classes.size(); ++i) {
        classes[i]->generateCode(printer);
        printer->Print("\n\n");
    }
}

bool FileGenerator::Generate(const FileDescriptor *file, const string &parameter, GeneratorContext *context,
                             string *error) const {
    std::vector<ClassGenerator *> classes;

    //TODO: maybe wrap work with class names and stuff in separate class (like in Java implementation)
    // Get output file name
    string const proto_file_name = file->name();
    size_t file_extension_index = proto_file_name.find(".proto");
    string const file_name = proto_file_name.substr(0, file_extension_index) + ".kt";


    google::protobuf::scoped_ptr<io::ZeroCopyOutputStream> output(context->Open(file_name));
    io::Printer printer(output.get(), '$', /* annotation_collector = */ NULL);

    // Create Generators for all top-level messages
    int topLevelMessagesCount = file->message_type_count();
    for (int i = 0; i < topLevelMessagesCount; ++i) {
        Descriptor const * descriptor = file->message_type(i);
        ClassGenerator * cgen = new ClassGenerator(descriptor);
        classes.push_back(cgen);
    }

    // Generate code and clean up
    generateCode(&printer, classes);
    for (int i = 0; i < classes.size(); ++i) {
        delete classes[i];
    }
    return true;
}

FileGenerator::FileGenerator() { }

FileGenerator::~FileGenerator() {
}


} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google
