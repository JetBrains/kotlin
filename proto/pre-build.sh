#!/bin/bash

lprotoc="/usr/local/lib/libprotoc.so"
lprotobuf="/usr/local/lib/libprotobuf.so"
srcdir='google_protobuf'
ok=0
missing=""
red='\033[0;31m'
nc='\033[0m'

if [ ! -e "$lprotoc" ]; then
	ok=0
	missing="$missing $lprotoc"
fi

if [ ! -e "$lprotobuf" ]; then
	ok=0
	missing="$missing $lprotobuf"
fi

if [ "$ok" == 1 ]; then
	echo "Prerequisites met, you can build library now via Makefile"
	exit 0
fi

echo "$missing, required for building, is not found"
echo "You have to build and install latest Protobuf version from https://github.com/google/protobuf"
echo "Do you want to do it automatically?  [y/n]"
echo "(Caution: this will take decent amount of time and internet traffic)"
response="n"
read response
if [ $response = "n" ]; then
	exit 1
fi

echo -e "${red}Creating directory for Protobuf sources${nc}"
mkdir $srcdir
if [ $? -ne 0 ]; then
	echo -e "${red}Error creating directory protobuf_sources/ for cloning repository${nc}"
	exit 1
fi
cd $srcdir

echo -e "${red}Cloning repository${nc}"
git clone https://github.com/google/protobuf
if [ $? -ne 0 ]; then
	echo -e "${red}Error cloning https://github.com/google/protobuf${nc}"
	exit 1
fi

cd protobuf

echo -e "${red}Resolving prerequisites to build Google Protobuf${nc}"
sudo apt-get install autoconf automake libtool curl make g++ unzip
if [ $? -ne 0 ]; then
	echo -e "${red}Error installing tools for building${nc}"
	exit 1
fi

echo -e "${red}Generating Makefiles and etc.${nc}"
./autogen.sh
./configure
if [ $? -ne 0 ]; then
	echo -e "${red}Error generating makefiles${nc}"
	exit 1
fi

echo -e "${red}Building${nc}"
make
if [ $? -ne 0 ]; then
	echo -e "${red}Error building protobuf via Make${nc}"
	exit 1
fi

echo -e "${red}Installing${nc}"
sudo make install
if [ $? -ne 0 ]; then
	echo -e "${red}Error installing protobuf${nc}"
	exit 1
fi

echo -e "${red}Refreshing dynamic libraries bindings${nc}"
sudo ldconfig
if [ $? -ne 0 ]; then
	echo -e "${red}Error refreshing dynamic libraries${nc}"
	exit 1
fi

echo -e "${red}Installation finished succesfully. You can now build library using Makefile.${nc}"
exit 0
