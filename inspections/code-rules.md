# Keep the inspection list in `README.md` in sync

Applies to:
```
*.inspection.kts
README.md
```

Each inspection lives in its own `<id>.inspection.kts` file, where `<id>` is the `id` of the `InspectionKts` it defines.
Each inspection has a row in the "Inspections" table of `README.md` with its id and a short summary of what it reports.
When adding, renaming or removing an inspection, update the table accordingly.
