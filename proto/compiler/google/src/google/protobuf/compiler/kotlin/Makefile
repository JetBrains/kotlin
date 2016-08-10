CXX = g++
CXXFLAGS = -O2 -std=c++11
LDFLAGS = -lprotoc -lprotobuf

EXE = protoc
SRCDIR = src
BINDIR = bin
TESTDIR = test
KTPROJECT = protoc-test-project
KTOUT = $(KTPROJECT)/src

OBJECTS = $(patsubst $(SRCDIR)/%.cc,$(BINDIR)/%.o,$(wildcard $(SRCDIR)/*.cc))
KTARTIFACTS = $(patsubst $(TESTDIR)/%.proto,$(KTOUT)/%.kt,$(wildcard $(TESTDIR)/*.proto))

all: $(EXE)

$(EXE): $(BINDIR) $(OBJECTS)
	$(CXX) $(OBJECTS) -o $(EXE) $(LDFLAGS)

$(BINDIR)/%.o: $(SRCDIR)/%.cc
	$(CXX) $(CXXFLAGS) -c -MMD -o $@ $<

include $(wildcard $(BINDIR)/*.d)

$(BINDIR):
	mkdir -p $(BINDIR)

$(KTOUT):
	mkdir -p $(KTOUT)

clean:
	rm -rf $(BINDIR)
	rm $(EXE)
	rm -rf $(KTPROJECT)

$(KTOUT)/%.kt: $(TESTDIR)/%.proto $(EXE)
	./protoc --kotlin_out=$(KTOUT) $<

generate: $(KTOUT) $(KTARTIFACTS)


.PHONY: clean all generate
