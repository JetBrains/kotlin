CXX = g++
CXXFLAGS = -O2 -std=c++11
LDFLAGS = -lprotoc -lprotobuf

EXE = protoc
SRCDIR = src
BINDIR = bin
TESTDIR = test
PROJECTS = protoc-tests

# Project for Java-backend
JVPROJECT = $(PROJECTS)/kt_java_tests
JVKTOUT = $(JVPROJECT)/src/kotlin_msg
JAVAOUT = $(JVPROJECT)/src

# Project for JS-backed
JSPROJECT = $(PROJECTS)/kt_js_tests
JSKTOUT = $(JSPROJECT)/src/kotlin_msg
JSOUT = $(JSPROJECT)/src/js_msg


# Build protoc-executable
all: $(EXE)

OBJECTS = $(patsubst $(SRCDIR)/%.cc,$(BINDIR)/%.o,$(wildcard $(SRCDIR)/*.cc))

$(EXE): $(BINDIR) $(OBJECTS)
	$(CXX) $(OBJECTS) -o $(EXE) $(LDFLAGS)

$(BINDIR)/%.o: $(SRCDIR)/%.cc
	$(CXX) $(CXXFLAGS) -c -MMD -o $@ $<

include $(wildcard $(BINDIR)/*.d)


# Create auxillary directories
$(BINDIR):
	mkdir -p $(BINDIR)

$(JVKTOUT):
	mkdir -p $(JVKTOUT)

$(JAVAOUT):
	mkdir -p $(JAVAOUT)

$(JSKTOUT):
	mkdir -p $(JSKTOUT)

$(JSOUT):
	mkdir -p $(JSOUT)

clean:
	rm -rf $(BINDIR)
	rm $(EXE)
	rm -rf $(JVKTOUT)
	rm -rf $(JAVAOUT)/java_msg
	rm -rf $(JSKTOUT)
	rm -rf $(JSOUT)

# Java-backend tests generation
$(JVKTOUT)/%.kt: $(TESTDIR)/%.proto $(EXE)
	./protoc --kotlin_out=$(JVKTOUT) $<

java: $(TESTDIR)/*.proto
	protoc --java_out=$(JAVAOUT) $^

JVKT_ARTIFACTS = $(patsubst $(TESTDIR)/%.proto,$(JVKTOUT)/%.kt,$(wildcard $(TESTDIR)/*.proto))

# JS-backend tests generation
$(JSKTOUT)/%.kt: $(TESTDIR)/%.proto $(EXE)
	./protoc --kotlin_out=$(JSKTOUT) $<

js: $(TESTDIR)/*.proto
	protoc --js_out=$(JSOUT) $^

JS_ARTIFACTS = $(patsubst $(TESTDIR)/%.proto,$(JSKTOUT)/%.kt,$(wildcard $(TESTDIR)/*.proto))

# Generate proto-messages from proto-sources
generate: $(JVKTOUT) $(JAVAOUT) $(JVKT_ARTIFACTS) $(JSKTOUT) $(JSOUT) $(JS_ARTIFACTS) js java

.PHONY: clean all generate java js
