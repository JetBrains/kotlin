#!/usr/bin/env bash
EXCLUDE=()
BUILD_SCRIPT="build.sh"

function isExcluded() {
    CHECKED="${1}"
    for VALUE in $EXCLUDE; do
	if [ "x$CHECKED" == "x$VALUE" ]; then
	    return 0
	fi
    done
    return -1
}

for SAMPLE_DIR in *; do
    if [ -d "$SAMPLE_DIR" ] && [ -e "$SAMPLE_DIR/$BUILD_SCRIPT" ]; then
	echo
	echo "======================================================"
	date
	echo "Building a sample: $SAMPLE_DIR."
	if ! isExcluded "$SAMPLE_DIR"; then
	    bash "$SAMPLE_DIR/$BUILD_SCRIPT" || (echo "Cannot build a sample: $SAMPLE_DIR. See log for details." && exit 1)
	else
	    echo "The sample excluded."
	fi
    fi
done
