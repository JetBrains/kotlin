#include "include/core/SkTime.h"
#include <iostream>

using namespace std;

int main()
{
	auto nsec = SkTime::GetNSecs();
	cout << nsec << endl;
	return 0;
}