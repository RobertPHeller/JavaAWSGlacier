#!/bin/sh
sdx qwrap AmazonGlacierLibWrapper.tcl
sdx unwrap AmazonGlacierLibWrapper.kit
ln -s /usr/share/tcl8.4/tcllib-1.11.1/snit AmazonGlacierLibWrapper.vfs/lib/
ln -s /usr/share/tcl8.4/tcllib-1.11.1/uri  AmazonGlacierLibWrapper.vfs/lib/
unzip -qq -d AmazonGlacierLibWrapper.vfs/lib BuildData/Tclxml3.1-Linux64.zip
mkdir AmazonGlacierLibWrapper.vfs/lib/AGLWrap
cp Scripts/*.tcl AmazonGlacierLibWrapper.vfs/lib/AGLWrap/
echo "pkg_mkIndex AmazonGlacierLibWrapper.vfs/lib/AGLWrap/ *.tcl"|tclkit 
mkdir AmazonGlacierLibWrapper.vfs/Copyright
cp COPYING AmazonGlacierLibWrapper.vfs/Copyright/
sdx wrap AmazonGlacierLibWrapper.kit
rm -rf AmazonGlacierLibWrapper.vfs
mv AmazonGlacierLibWrapper.kit dist/lib/
