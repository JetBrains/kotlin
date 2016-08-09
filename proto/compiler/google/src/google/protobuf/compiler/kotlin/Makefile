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
	$(CXX) $(CXXFLAGS) -c -MMD -o $@ $<

include $(wildcard $(BINDIR)/*.d)

$(BINDIR):
	mkdir -p $(BINDIR)

clean:
	rm -rf $(BINDIR)
	rm $(EXE)

generate:
	./protoc --kotlin_out=$$HOME/Downloads/carkot/proto/compiler/protoc-artifacts ./test/connect.proto
	./protoc --kotlin_out=$$HOME/Downloads/carkot/proto/compiler/protoc-artifacts ./test/direction.proto
	./protoc --kotlin_out=$$HOME/Downloads/carkot/proto/compiler/protoc-artifacts ./test/location.proto
	./protoc --kotlin_out=$$HOME/Downloads/carkot/proto/compiler/protoc-artifacts ./test/route_done.proto
	./protoc --kotlin_out=$$HOME/Downloads/carkot/proto/compiler/protoc-artifacts ./test/route.proto
	./protoc --kotlin_out=$$HOME/Downloads/carkot/proto/compiler/protoc-artifacts ./test/rc_session.proto

.PHONY: clean all generate
