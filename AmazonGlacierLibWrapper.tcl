##############################################################################
#
#  System        : 
#  Module        : 
#  Object Name   : $RCSfile$
#  Revision      : $Revision$
#  Date          : $Date$
#  Author        : $Author$
#  Created By    : Robert Heller
#  Created       : Tue May 19 15:17:37 2015
#  Last Modified : <150519.1654>
#
#  Description	
#
#  Notes
#
#  History
#	
##############################################################################
#
#    Copyright (C) 2015  Robert Heller D/B/A Deepwoods Software
#			51 Locke Hill Road
#			Wendell, MA 01379-9728
#
#    This program is free software; you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation; either version 2 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program; if not, write to the Free Software
#    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
#
# 
#
##############################################################################


if {$::starkit::mode eq "sourced"} {
    starkit::autoextend [file join $starkit::topdir lib] 
    global AWS_GlacierClient_JAVA
    set AWS_GlacierClient_JAVA [glob \
                                -nocomplain \
                                [file join \
                                 [file dirname $starkit::topdir] \
                                 JavaAWSGlacier-*.jar]]
    package require BackupVault 2.0
    
    package provide AmazonGlacierLib_Wrapper 1.0
} elseif {$argc > 0} {
    switch [lindex $argv 0] {
        install {
            if {$argc < 2} {
                puts stderr "$argv0 install: missing destdir\n"
                puts stderr "usage: $argv0 install destdir\n"
                puts stderr "Install the libraries in this Kit under destdir."
                exit 1
            }
            set destdir [lindex $argv 1]
            if {![file isdirectory $destdir] && ![file writable $destdir]} {
                puts stderr "$argv0 install: not a writable directory: $destdir"
                exit 2
            }
            set libfiles [glob -directory [file join $starkit::topdir lib] *]
            #puts stderr "*** $argv0 install: libfiles = $libfiles"
            set appindx [lsearch -glob $libfiles */app-AmazonGlacierLibWrapper]
            #puts stderr "*** $argv0 install: appindx = $appindx"
            if {$appindx >= 0} {
                set libfiles [lreplace $libfiles $appindx $appindx]
            }
            #puts stderr "*** $argv0 install(2): libfiles = $libfiles"
            eval [list file copy] $libfiles [list $destdir]
        }
        copyright {
            set copyingFp [open [file join \
                                 [file dirname [file dirname \
                                                [file dirname \
                                                 [info script]]]] \
                                 Copyright COPYING] r]
            set copying [read $copyingFp]
            close $copyingFp
            puts $copying
        }
        warrantry {
            set copyingFp [open [file join \
                                 [file dirname [file dirname \
                                                [file dirname \
                                                 [info script]]]] \
                                 Copyright COPYING] r]
            set copying [read $copyingFp]
            close $copyingFp
            if {[regexp -indices -line {^[[:space:]]+NO WARRANTY$} $copying startline] > 0} {
                set start [lindex $startline 0]
                if {[regexp -indices -line {^[[:space:]]+END OF TERMS AND CONDITIONS} $copying endline] > 0} {
                    set end [lindex $endline 0]
                    set warrantry [string range $copying $start $end]
                    puts "$warrantry"
                }
            }
        }
        help {
            if {$argc == 1} {
                puts stderr "usage: $argv0 command ...\n"
                puts stderr "Command is one of:"
                puts stderr "   install -- install the library files"
                puts stderr "   copyright -- display the copyright"
                puts stderr "   warrantry -- display the warrantry"
                puts stderr "   help    -- help on using this kit\n"
                puts stderr "This kit can also be directly sourced into a tclkit"
                puts stderr "script to directly make use of this library."
                exit 3
            } else {
                switch [lindex $argv 1] {
                    install {
                        puts stderr "usage: $argv0 install destdir\n"
                        puts stderr "Install the library files under destdir, which can be a VFS dir of an unwraped"
                        puts stderr "kit. The library directories in this kit will be recursively copied under the"
                        puts stderr "destdir directory."
                        exit 3
                    }
                    default {
                        puts stderr "usage: $argv0 help \[command\]\n"
                        puts stderr "Provide help on using this kit."
                        exit 3
                    }
                }
            }
        }
        default {
            puts stderr "$argv0: unknown command: [lindex $argv 0]\n"
            puts stderr "usage: $argv0 command ..."
            puts stderr "or 'source $argv0' to use this library in a tclkit"
            puts stderr "To get a list of defined commands, type '$argv0 help'"
            exit 1
        }
    }
    exit
} else {            
    puts stderr {usage: source this kit file into a tclkit to add in the Amazon Glacier Client}
    puts stderr {code.  Then you can use AWS::GlacierClientWrapper and its various support libraries.}
    puts stderr "Enter '$argv0 help' for more help."
    exit 2    
}


