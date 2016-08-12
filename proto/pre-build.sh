#!/bin/bash

lprotoc="/usr/local/lib/libprotoc.so"
lprotobuf="/usr/local/lib/libprotobuf.so"
ok=1
missing=""

if [ ! -e "$lprotoc" ]; then
	ok=0
	missing="$missing $lprotoc"
fi

if [ ! -e "$lprotobuf" ]; then
	ok=0
	missing="$missing $lprotobuf"
fi

if [ "$ok" == 0 ]; then
	echo "$missing, required for building, is not found" \
"You have to build and install latest Protobuf version from https://github.com/google/protobuf" \
"Do you want to do it automatically?  [y/n]" \
"(Caution: this will take decent amount of time and internet traffic)"
	response="n"
	read response
	if [ $response = "y" ]; then
		echo "Creating directory for Protobuf sources"
		mkdir protobuf_sources
		cd protobuf_sources

		echo "Cloning repository"
		git clone https://github.com/google/protobuf
		cd protobuf

		echo "Resolving prerequisites to build Google Protobuf"
		sudo apt-get install autoconf automake libtool curl make g++ unzip

		echo "Generating Makefiles and etc."
		./autogen.sh
		./configure

		echo "Building"
		make

		echo "Installing"
		sudo make install

		echo "Cleaning"
		# cd ../..
		# rm -rf protobuf_sources
	fi
else
	echo "Prerequisites met, you can build library now via Makefile"
fi
