CXX = g++
CXXFLAGS = -O2 -std=c++11
LDFLAGS = -lprotoc -lprotobuf

EXE = protoc
SRCDIR = src
BINDIR = bin
TESTDIR = test

OBJECTS = $(patsubst $(SRCDIR)/%.cc,$(BINDIR)/%.o,$(wildcard $(SRCDIR)/*.cc))

all: $(EXE)

$(EXE): $(BINDIR) $(OBJECTS)
	$(CXX) $(OBJECTS) -o $(EXE) $(LDFLAGS)
	
$(BINDIR)/%.o: $(SRCDIR)/%.cc
	$(CXX) $(CXXFLAGS) -c -MMD -o $@ $< -lprotoc

include $(wildcard $(BINDIR)/*.d)

$(BINDIR):
	mkdir -p $(BINDIR)

clean:
	rm -rf $(BINDIR) $(EXE)

generate:
	./protoc --kotlin_out=$$HOME/Downloads/carkot/proto/compiler/src ./test/addressbook.proto 
	./protoc --kotlin_out=$$HOME/Downloads/carkot/proto/compiler/src ./test/nested-msg.proto
.PHONY: clean all generate
