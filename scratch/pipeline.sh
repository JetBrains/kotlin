#!/bin/bash

# 1. Extract errors from HTML reports
python3 scratch/update_errors.py

# 2. Annotate errors with Gemini
python3 scratch/gemini_annotate.py jklib_test_errors.md jklib_test_errors_gemini.md

# 3. Categorize and count errors
python3 scratch/categorize_errors.py jklib_test_errors_gemini.md > errors.txt

# 4. Group similar errors using Gemini
python3 scratch/gemini_group_errors.py errors.txt > grouped_errors.txt

