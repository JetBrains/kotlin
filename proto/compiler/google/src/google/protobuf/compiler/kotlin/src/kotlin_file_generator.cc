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
#include "kotlin_name_resolver.h"

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

void FileGenerator::generateCode(io::Printer *printer, std::vector<ClassGenerator *> & classes) {
    for (int i = 0; i < classes.size(); ++i) {
        classes[i]->generateCode(printer);
        printer->Print("\n\n");
    }
}

// Extract more methods or inline generateCode() to have common style in this method
bool FileGenerator::Generate(const FileDescriptor *file, const string &parameter, GeneratorContext *context,
                             string *error) const {
    std::vector<ClassGenerator *> classes;
    string const file_name = name_resolving::getKotlinOutputByProtoName(file->name());
    google::protobuf::scoped_ptr<io::ZeroCopyOutputStream> output(context->Open(file_name));
    io::Printer printer(output.get(), '$', /* annotation_collector = */ NULL);
    NameResolver * nameResolver = new NameResolver();

    // Create Generators for all top-level messages
    int topLevelMessagesCount = file->message_type_count();
    for (int i = 0; i < topLevelMessagesCount; ++i) {
        Descriptor const * descriptor = file->message_type(i);
        // TODO: think about order of initialization and cross-branches calls. If we don't allow such things, everythign is ok atm
        nameResolver->addClass(descriptor->name(), /* parentName = */ "");
        ClassGenerator * cgen = new ClassGenerator(descriptor, nameResolver);
        nameResolver->addGeneratorForClass(descriptor->name(), cgen);
        classes.push_back(cgen);
    }

    // Generate code and clean up
    generateCode(&printer, classes);

    for (int i = 0; i < classes.size(); ++i) {
        delete classes[i];
    }
    delete nameResolver;

    return true;
}

FileGenerator::FileGenerator() { }

FileGenerator::~FileGenerator() {
}


} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google
