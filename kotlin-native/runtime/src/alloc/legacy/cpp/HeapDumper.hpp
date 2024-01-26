/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <sstream>

#include "Graphviz.hpp"
#include "KString.h"

namespace kotlin::alloc {

template <typename Printer>
class HeapDump {
public:
    explicit HeapDump(const Printer& printer) : printer_(printer) {}

    void object(const ObjHeader* obj) {
        auto objId = id(obj);
        auto typeName = name(obj->type_info());

        std::stringstream label;
        label << objId << "[" << typeName << "]";

        graph_.node(id(obj), label.str());

        if (auto extraObj = mm::ExtraObjectData::Get(obj)) {
            std::stringstream flags;
            flags << "{";
            if (extraObj->getFlag(mm::ExtraObjectData::FLAGS_FROZEN)) flags << "FROZEN,";
            if (extraObj->getFlag(mm::ExtraObjectData::FLAGS_NEVER_FROZEN)) flags << "NEVER_FROZEN,";
            if (extraObj->getFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE)) flags << "IN_FIN_Q,";
            if (extraObj->getFlag(mm::ExtraObjectData::FLAGS_SWEEPABLE)) flags << "SWEEPABLE,";
            if (extraObj->getFlag(mm::ExtraObjectData::FLAGS_RELEASE_ON_MAIN_QUEUE)) flags << "RELEASE_ON_MAIN,";
            if (extraObj->getFlag(mm::ExtraObjectData::FLAGS_FINALIZED)) flags << "FINALIZED,";
            flags << "}";


            std::stringstream extraLabel;
            extraLabel << id(extraObj) << "[EXTRA(" << id(obj) << ")\\nflags: " << flags.str() << "]";

            graph_.node(id(extraObj), extraLabel.str());
        };
    }

    void reference(const ObjHeader* from, const ObjHeader* to) {
        graph_.edge(id(from), id(to));
    }

private:
    template <typename T>
    static std::string id(T* obj) {
        std::stringstream stream;
        stream << obj; // TOOD std::hex?
        return stream.str();
    }

    static std::string name(const TypeInfo* type) {
        if (type == nullptr) return "<unknown>";
        char* cstr = CreateCStringFromString(type->relativeName_);
        std::string str = cstr;
        std::free(cstr);
        return str;
    }

    Printer printer_;
    graphviz::Graph<Printer> graph_{printer_, "Heap", true};
};

}
