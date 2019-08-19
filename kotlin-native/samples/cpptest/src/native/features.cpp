#include "features.h"

#include <iostream>

using namespace std;

namespace ns {

int NoName::noNameMember(int& iRef) {
	cout << __PRETTY_FUNCTION__ << " invoked" << endl;
	return ++iRef;
}

int CppTest::counter;

int CppTest::s_fun() {
	static int counter = 777;
	cout << __PRETTY_FUNCTION__ << " invoked" << endl;
	return counter++;
}

int CppTest::foo(const CppTest* x) {
	int res = x == this;
	cout << "This is CppTest::foo: result is iPub + (int)(param == this): " << iPub + res << endl;
	return iPub + res;
}

CppTest::CppTest() {
	cout << ++counter << "\t" << __PRETTY_FUNCTION__ << endl;
}

CppTest::CppTest(const CppTest& c) {
    *this = c;
	cout << ++counter << "\t" << __PRETTY_FUNCTION__ << endl;
}

CppTest::CppTest(int i, double j) : iPub(i) {
	cout << ++counter << "\t" << __PRETTY_FUNCTION__ << endl;
}


CppTest::~CppTest() {
	cout << --counter << "\t" << __PRETTY_FUNCTION__ << endl;
}


CppTest bar(CppTest* s) {
	if (s)
		return *s;
	else
		return * new CppTest();
}

CppTest* create() {
	return new CppTest();
}

} // ns

CppTest* create() {
	cout << __PRETTY_FUNCTION__ << " declared in global ns" << endl;
	return nullptr;
}

::CppTest* ns2::create() {
	cout << __PRETTY_FUNCTION__ << " declared in ns2" << endl;
	return nullptr;
}

void test() {
	char buf[sizeof(ns::CppTest)];
	ns::CppTest* x = new((ns::CppTest*)buf) ns::CppTest();
}