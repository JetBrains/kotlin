#!/bin/bash

#
# Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

# Check if the correct number of arguments are provided
if [ "$#" -ne 4 ]; then
    echo "Usage: $0 <base_source_path> <destination_path> <version_folder_name> <replacement_string>"
    echo ""
    echo "Arguments:"
    echo "  <base_source_path>        : The root path where the script will start searching for version folders."
    echo "                              Example: /home/user/my_projects"
    echo "  <destination_path>        : The root path where files will be copied."
    echo "                              Example: /home/user/build_output"
    echo "  <version_folder_name>     : The exact name of the folder containing the module files (e.g., 'version', 'v1.0', 'release')."
    echo "                              Example: version"
    echo "  <replacement_string>      : The string that will replace all instances of '<version_folder_name>' inside the copied files."
    echo "                              Example: '1.2.3-SNAPSHOT'"
    echo ""
    echo "This script will find .module files in paths like <base_source_path>/*/version_folder_name/*.module"
    echo "and copy them to <destination_path>/<moduleName>/<moduleName>.module."
    echo "Additionally, it will replace all occurrences of the literal string matching the <version_folder_name> argument"
    echo "inside these copied files with the provided <replacement_string>."
    echo "It will also replace specific hash and size information with 'null'."
    echo "If multiple .module files are found in a 'version_folder' for the same module,"
    echo "the last one processed will overwrite previous ones."
    exit 1
fi

BASE_SOURCE_PATH="$1"
DESTINATION_PATH="$2"
VERSION_FOLDER_NAME="$3"
REPLACEMENT_STRING="$4" # New argument for the replacement string

# Check if base source path exists
if [ ! -d "$BASE_SOURCE_PATH" ]; then
    echo "Error: Base source path '$BASE_SOURCE_PATH' does not exist or is not a directory."
    exit 1
fi

# Create destination path if it doesn't exist
mkdir -p "$DESTINATION_PATH"

echo "Searching for '$VERSION_FOLDER_NAME' folders under '$BASE_SOURCE_PATH'..."

# Find all directories named VERSION_FOLDER_NAME under BASE_SOURCE_PATH
# The -print0 and while read -r -d $'\0' ensure correct handling of filenames with spaces or special characters.
find "$BASE_SOURCE_PATH" -type d -name "$VERSION_FOLDER_NAME" -print0 | while IFS= read -r -d $'\0' version_folder; do
    echo "Processing files in: $version_folder"

    # Determine the parent directory of the 'version' folder.
    # This parent directory is the one whose name we want to preserve in the destination.
    # Example: if version_folder is /path1/subfolder1/version,
    # parent_of_version_folder will be /path1/subfolder1.
    parent_of_version_folder="$(dirname "$version_folder")"

    # Extract the name of the subfolder that should be preserved in the destination.
    # This is the <moduleName> part of your desired destination path.
    # Example: if parent_of_version_folder is /path1/subfolder1,
    # preserved_subfolder_name will be subfolder1.
    preserved_subfolder_name="$(basename "$parent_of_version_folder")"

    # Find all .module files within the current version_folder
    find "$version_folder" -type f -name "*.module" -print0 | while IFS= read -r -d $'\0' file; do
        # The destination filename should always be the module name itself, with a .module extension.
        # Example: If preserved_subfolder_name is 'kotlin-assignment', the destination file will be 'kotlin-assignment.module'.
        cleaned_filename="${preserved_subfolder_name}.module"

        # Construct the full destination path for the file.
        # This combines the DESTINATION_PATH, the preserved subfolder name (as the new directory),
        # and the cleaned filename (which is the module name + .module).
        # Example: /home/user/build_output/kotlin-assignment/kotlin-assignment.module
        dest_dir="$DESTINATION_PATH/$preserved_subfolder_name"
        dest_file="$dest_dir/$cleaned_filename"

        # Create the parent directory in the destination if it doesn't exist
        mkdir -p "$dest_dir"

        # Copy the file
        cp "$file" "$dest_file"
        echo "Copied: $file -> $dest_file"

        # Escape special characters for the search pattern (VERSION_FOLDER_NAME)
        # This is crucial if VERSION_FOLDER_NAME contains characters like '.', '*', '[', ']', etc.
        ESCAPED_VERSION_FOLDER_NAME=$(echo "$VERSION_FOLDER_NAME" | sed 's/[.\[\]*^$]/\\&/g')

        # Replace all instances of VERSION_FOLDER_NAME with the specified replacement string in the copied file
        echo "Replacing '$VERSION_FOLDER_NAME' with '$REPLACEMENT_STRING' in $dest_file..."
        # Using sed -i for in-place editing. The 'g' flag ensures all occurrences on a line are replaced.
        # Using a different delimiter (e.g., '#') for sed to avoid issues if REPLACEMENT_STRING contains '/'
        # Added '' to sed -i for cross-platform compatibility (e.g., macOS requires it).
        sed -i '' "s#$ESCAPED_VERSION_FOLDER_NAME#$REPLACEMENT_STRING#g" "$dest_file"

        # Replace specific hash and size information with 'null'
        echo "Replacing hash and size information with 'null' in $dest_file..."
        # Replace "size": <number>, with "size": null,
        sed -i '' 's/"size": [0-9]*,/"size": null,/g' "$dest_file"
        # Replace "sha512": "...", with "sha512": null,
        sed -i '' 's/"sha512": "[^"]*",/"sha512": null,/g' "$dest_file"
        # Replace "sha256": "...", with "sha256": null,
        sed -i '' 's/"sha256": "[^"]*",/"sha256": null,/g' "$dest_file"
        # Replace "sha1": "...", with "sha1": null,
        sed -i '' 's/"sha1": "[^"]*",/"sha1": null,/g' "$dest_file"
        # Replace "md5": "...", with "md5": null
        sed -i '' 's/"md5": "[^"]*"/"md5": null/g' "$dest_file"
    done
done

echo "Copying and replacement complete."
