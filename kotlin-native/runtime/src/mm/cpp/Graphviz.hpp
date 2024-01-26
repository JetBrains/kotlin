/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <sstream>

#include "Logging.hpp"
#include "Utils.hpp"

namespace kotlin::graphviz  {

template <typename Printer>
class Graph : private Pinned {
public:
    Graph(const Printer& printer, std::string_view name, bool directed = true) : printer_(printer), directed_(directed) {
        auto kind = directed_ ? "digraph" : "graph";
        std::stringstream s;
        s << kind << " " << name << " {";
        printer_.println(s.str().data());
        incIdent();
    }

    ~Graph() {
        printer_.println("}");
    }

    void node(std::string_view id, std::string_view label) {
        std::stringstream s;
        s << ident() << id << "[label=\"" << label << "\"];";
        printer_.println(s.str().data());
    }

    void edge(std::string_view fromId, std::string_view toId) {
        std::stringstream s;
        auto edgeOp = directed_ ? "->" : "--";
        s << ident() << fromId  << edgeOp << toId << ";";
        printer_.println(s.str().data());
    }
private:
    static constexpr int kIdentStep = 4;
    void incIdent() {
        ident_ += kIdentStep;
    }
    void decIdent() {
        ident_ -= kIdentStep;
    }
    [[nodiscard]] std::string ident() const {
        return std::string(ident_, ' ');
    }

    Printer printer_;
    const bool directed_;
    int ident_ = 0;
};

// FIXME move out of graphviz namespace
template <logging::Tag kTag, logging::Level kLevel>
struct LogPrinter {
    void println(const char* str) {
        RuntimeLog(kLevel, {kTag}, "%s", str);
    }
};

}
