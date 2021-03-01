#!/usr/bin/python

import sys
import os
import os.path
import shutil

if len(sys.argv) != 2:
    print('Usage: ' + sys.argv[0] + ' <kotlin_version>')
    sys.exit(0)

kotlinVersion = sys.argv[1]
print('Repacking bundles for Kotlin/Native version ' + kotlinVersion)

bundles = [f for f in os.listdir('.') if f.startswith('kotlin-native-prebuilt-') and (f.endswith('.tar.gz') or f.endswith('.zip')) and os.path.isfile(f)]
print('Found ' + str(len(bundles)) + ' bundle files to repack: ' + ', '.join(bundles))

for bundle in bundles:
	print('')
	print('Unpacking ' + bundle)
	unpackCommand = 'unzip -qq' if bundle.endswith('.zip') else 'tar -xzf'
	os.system(unpackCommand + ' ' + bundle)

	extractedDir = bundle.rstrip('.tar.gz').rstrip('.zip')
	renamedDir = extractedDir.replace('-prebuilt-', '-')
	print('Renaming ' + extractedDir + ' to ' + renamedDir)
	os.rename(extractedDir, renamedDir)

	repackedBundle = renamedDir + ('.zip' if bundle.endswith('.zip') else '.tar.gz')
	print('Packing ' + repackedBundle)
	packCommand = 'zip -qq -r' if bundle.endswith('.zip') else 'COPYFILE_DISABLE=true tar -czf'
	os.system(packCommand + ' ' + repackedBundle + ' ' + renamedDir)
	shutil.rmtree(renamedDir)

	print('Calculating SHA256')
	shaCommand = 'shasum -a 256'
	os.system(shaCommand + ' ' + repackedBundle + ' > ' + repackedBundle + '.sha256')

print('')
print('Done.')
