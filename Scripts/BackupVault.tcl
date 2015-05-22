##############################################################################
#
#  System        : 
#  Module        : 
#  Object Name   : $RCSfile$
#  Revision      : $Revision$
#  Date          : $Date$
#  Author        : $Author$
#  Created By    : Robert Heller
#  Created       : Wed Jul 30 15:32:49 2014
#  Last Modified : <150522.1036>
#
#  Description	
#
#  Notes
#
#  History
#	
##############################################################################
#
#  Copyright (c) 2014 Deepwoods Software.
# 
#  All Rights Reserved.
# 
#  This  document  may  not, in  whole  or in  part, be  copied,  photocopied,
#  reproduced,  translated,  or  reduced to any  electronic  medium or machine
#  readable form without prior written consent from Deepwoods Software.
#
##############################################################################



package require snit
package require VaultXMLDB

snit::type BackupVault {
    component glacierVaultDB
    
    typevariable Meg256 [expr {wide(256 * 1024 * 1024)}]
    typevariable glacierTemp /home/AmazonGlacierTemp
    typevariable tempFileindex 1
    typevariable javacmd
    typeconstructor {
        set java [auto_execok java]
        set javacmd [list $java -jar $::AWS_GlacierClient_JAVA]
    }
    method run_java_GlacierClient {args} {
        set cmd "|$javacmd $args"
        #puts stderr "*** $self run_java_GlacierClient: cmd = '$cmd'"
        set pipe [open $cmd r]
        set result [read $pipe]
        close $pipe
        return $result
    }
    proc generateTempfile {} {
        while {1} {
            set filename [file join $glacierTemp [format TMP%08x $tempFileindex]]
            if {![file exists $filename]} {return $filename}
            incr tempFileindex
        }
    }
    delegate method savedb to glacierVaultDB
    delegate method findvaultbyname to glacierVaultDB
    delegate method findarchivebydescr to glacierVaultDB
    delegate method findarchivebyaid to glacierVaultDB
    method getvaultnode {} {
        return [$glacierVaultDB getElementsByTagName vaults]
    }
    constructor {dbfile args} {
        if {[file exists $dbfile]} {
            set glacierVaultDB [VaultXMLDB olddb $dbfile]
        } else {
            set glacierVaultDB [VaultXMLDB newdb]
        }
    }
    typevariable timeFormatter {%Y%m%dT%H%M%SZ}
    method CreateNewVault {vaultname} {
        #puts stderr "*** $self CreateNewVault $vaultname"
        if {[catch {$self run_java_GlacierClient CreateVault $vaultname} result]} {
            puts stderr "Failed to create Glacier vault: $result"
            return {}
        }
        foreach {name loc} $result {break}
        set date [clock format [clock scan now] -format $timeFormatter -gmt yes]
        puts stdout "New vault created on $date: Location is $loc"
        return [$glacierVaultDB addvault $loc $date]
    }
    method UploadArchive {vaultname archivefile} {
        #puts stderr "*** $self UploadArchive $vaultname $archivefile"
        if {[catch {$self run_java_GlacierClient UploadArchive $vaultname $archivefile} result]} {
            puts stderr "Failed to upload archive to vault: $vaultname $archivefile: $result"
            return {}
        }
        foreach {loc sha256treehash description} $result {break}
        set date [clock format [clock scan now] -format $timeFormatter -gmt yes]
        set size [file size $archivefile]
        #puts stderr "*** $self UploadArchive: sha256treehash is $sha256treehash"
        puts stdout "Archive ($description, $size) uploaded on $date to $loc"
        return [$glacierVaultDB addarchive $loc $date $size $sha256treehash $description]
    }
    method ListParts {vault uploadid args} {
        #puts stderr "*** $self ListParts $vault $uploadid $args"
        set vnode [$self findvaultbyname $vault]
        if {$vnode eq {}} {
            error "No such vault: $vault"
        }
        if {[catch {eval [list $self run_java_GlacierClient ListParts $vault $uploadid] $args} result]} {
            puts stderr "Failed to list parts for $vault $uploadid: $result"
            return {}
        }
        return $result
    }
    method ListMultiPartUploads {vault args} {
        #puts stderr "*** $self ListMultiPartUploads $vault $args"
        set vnode [$self findvaultbyname $vault]
        if {$vnode eq {}} {
            error "No such vault: $vault"
        }
        if {[catch {eval [list $self run_java_GlacierClient ListMultipartUploads $vault] $args} result]} {
            puts stderr "Failed to list multipart uploads for $vault: $result"
            return {}
        }
        return $result
    }
    method AbortMultipartUpload {vault uploadid} {
        #puts stderr "*** $self AbortMultipartUpload $vault $uploadid"
        set vnode [$self findvaultbyname $vault]
        if {$vnode eq {}} {
            error "No such vault: $vault" 
        }
        if {[catch {$self run_java_GlacierClient AbortMultipartUpload $vault $uploadid} result]} {
            puts stderr "Failed to abort upload ($uploadid): $result"
        }
        return {}
    }
       
    method InitiateRetrieveArchiveJob {vault archive snstopic} {
        #puts stderr "*** $self InitiateRetrieveArchiveJob $vault $archive $snstopic"
        set vnode [$self findvaultbyname $vault]
        if {$vnode eq {}} {
            error "No such vault: $vault"
        }
        set anode [$self  findarchivebydescr $vnode $archive]
        if {$anode eq {}} {
            error "No such archive in $vault: $archive"
        }
        #puts stderr "set response \[$glacierClient initiateJob $vault archive-retrieval \
        #                            -archiveid [$anode attribute archiveid] \
        #                            -snstopic $snstopic\]"
        if {[catch {$self run_java_GlacierClient InitiateJob $vault archive-retrieval [$anode attribute archiveid] -snstopic $snstopic} result]} {
            puts stderr "Failed to initiate job: $result"
            return {}
        } else {
            return $result
        }
    }
    method GetJobList {vault args} {
        #puts stderr "*** $self GetJobList $vault $args"
        set vnode [$self findvaultbyname $vault]
        if {$vnode eq {}} {
            error "No such vault: $vault"
        }
        if {[catch {eval [list $self run_java_GlacierClient ListJobs $vault] $args} result]} {
            puts stderr "Failed to list jobs: $result"
            return {}
        } else {
            return $result
        }
    }
    method GetJobDescription {vault jobid} {
        #puts stderr "*** $self GetJobDescription $vault $jobid"
        set vnode [$self findvaultbyname $vault]
        if {$vnode eq {}} {
            error "No such vault: $vault"
        }
        if {[catch {$self run_java_GlacierClient DescribeJob $vault} result]} {
            puts stderr "Failed to get job description: $result"
            return {}
        } else {
            return $result
        }
    }
    method RetrieveArchive {vault archiveid jobid filename size range treehash wholetreehash} {
        #puts stderr "*** $self RetrieveArchive $vault $archiveid $jobid $filename $size $range $treehash $wholetreehash"
        foreach {first last} [split $range -] {break}
        set partialsize [expr {(wide($last)-wide($first))+1}]
        if {wide($partialsize) == wide($size)} {
            set result [$self RetrieveWholeArchive $vault $archiveid $jobid $filename $wholetreehash $size]
        } else {
            set result [$self RetrievePartArchive $vault $archiveid $jobid $filename $treehash $partialsize]
        }
        if {$result eq {}} {
            return {}
        } else {
            return $filename
        }
    }
    method RetrieveWholeArchive {vault archiveid jobid filename wholetreehash size} {
        #puts stderr "*** $self RetrieveWholeArchiveInOnePart $vault $archiveid $jobid $filename $wholetreehash $size"
        if {wide($size) > $Meg256} {
            return [$self RetrieveWholeArchiveInParts $vault $archiveid $jobid $filename $wholetreehash $size]
        } else {
            if {[catch {$self run_java_GlacierClient GetJobOutput $vault $jobid -output $filename} result]} {
                puts stderr "Failed to download archive: $result"
                return {}
            }
            array unset response
            array set response $result
            if {[catch {$self run_java_GlacierClient TreeHash $filename} computedTreeHash]} {
                puts stderr "Failed to compute tree hash: $computedTreeHash"
                return {}
            }
            if {$computedTreeHash ne $wholetreehash ||
                $response(Checksum) ne $computedTreeHash} {
                puts stderr "Archive SHA256 Tree Hash failure: locally computed: $computedTreeHash, returned: $response(Checksum), in job descr: $wholetreehash"
                return {}
            }
            return $filename
        }
    }
    method RetrieveWholeArchiveInParts {vault archiveid jobid filename wholetreehash sizefilename wholetreehash size} {
        #puts stderr "*** $self RetrieveWholeArchiveInParts $vault $archiveid $jobid $filename $wholetreehash $sizefilename $wholetreehash $size"
        set downloadpartfile [generateTempfile]
        set fp [open $filename w]
        fconfigure $fp -translation binary
        set done no
        set pos [expr {wide(0)}]
        set remainder [expr {wide($size)}]
        while {!$done} {
            if {$remainder > $Meg256} {
                set partsize $Meg256
            } else {
                set partsize $remainder
            }
            set range [format {bytes=%d-%d} $pos [expr {(wide($pos)+wide($partsize))-1}]]
            if {[catch {$self run_java_GlacierClient GetJobOutput $vault $jobid -output $downloadpartfile -range $range} result]} {
                puts stderr "Failed to download archive part ($range): $result"
                return {}
            }
            array unset response
            array set response $result
            if {[catch {$self run_java_GlacierClient TreeHash $downloadpartfile} computedTreeHash]} {
                puts stderr "Failed to compute tree hash: $computedTreeHash"
                return {}
            }
            if {$computedTreeHash ne $response(Checksum)} {
                puts stderr "Archive part ($range) SHA256 Tree Hash failure: locally computed: $computedTreeHash, returned: $response(Checksum)"
                return {}
            }
            set dfp [open $downloadpartfile w]
            fconfigure $dfp -translation binary
            if {[catch {fcopy $dfp $fp} ps]} {
                puts stderr "Archive part ($range) copy failed: $ps"
                return {}
            } elseif {$ps < $partsize} {
                puts stderr "Archive part ($range) partial copy: $ps instead of $partsize bytes"
                return {}
            }
            close $dfp
            set pos [expr {wide(pos) + $partsize}]
            set done [expr {wide($pos) >= wide($size)}]
            set remainder [expr {$remainder - $partsize}]
        }
        close $fp
        if {[catch {$self run_java_GlacierClient TreeHash $filename} computedTreeHash]} {
            puts stderr "Failed to compute tree hash: $computedTreeHash"
            return {}
        }
        if {$computedTreeHash ne $wholetreehash} {
            puts stderr "Archive SHA256 Tree Hash failure: locally computed: $computedTreeHash, in job descr: $wholetreehash"
            return {}
        } else {
            return $filename
        }
    }
    method RetrievePartArchive {vault archiveid jobid filename treehash partialsize} {
        #puts stderr "*** $self RetrievePartArchive $vault $archiveid $jobid $filename $treehash $partialsize"
        foreach {first last} [split $range -] {break}
        if {wide($partialsize) > $Meg256Meg256} {
            return [$self RetrievePartArchivePartArchiveInParts $vault $archiveid $jobid $filename $treehash $partialsize]
        } else {
            if {[catch {$self run_java_GlacierClient GetJobOutput $vault $jobid -output $filename} result]} {
                puts stderr "Failed to download archive: $result"
                return {}
            }
            array unset response
            array set response $result
            if {[catch {$self run_java_GlacierClient TreeHash $filename} computedTreeHash]} {
                puts stderr "Failed to compute tree hash: $computedTreeHash"
                return {}
            }
            if {$computedTreeHash ne $treehash ||
                $response(Checksum) ne $computedTreeHash} {
                puts stderr "Archive SHA256 Tree Hash failure: locally computed: $computedTreeHash, returned: $response(Checksum), in job descr: $treehash"
                return {}
            }
            return $filename
        }
    }
    method RetrievePartArchivePartArchiveInParts {vault archiveid jobid filename treehash partialsize} {
        #puts stderr "*** $self RetrievePartArchivePartArchiveInParts $vault $archiveid $jobid $filename $treehash $partialsize"
        
        set downloadpartfile [generateTempfile]
        set fp [open $filename w]
        fconfigure $fp -translation binary
        set done no
        set pos [expr {wide(0)}]
        set remainder [expr {wide($partialsize)}]
        while {!$done} {
            if {$remainder > $Meg256} {
                set partsize $Meg256
            } else {
                set partsize $remainder
            }
            set range [format {bytes=%d-%d} $pos [expr {(wide($pos)+wide($partsize))-1}]]
            if {[catch {$self run_java_GlacierClient GetJobOutput $vault $jobid -output $downloadpartfile -range $range} result]} {
                puts stderr "Failed to download archive part ($range): $result"
                return {}
            }
            array unset response
            array set response $result
            set amztreehash $response(Checksum)
            if {$amztreehash ne "null"} {
                if {[catch {$self run_java_GlacierClient TreeHash $downloadpartfile} computedTreeHash]} {
                    puts stderr "Failed to compute tree hash: $computedTreeHash"
                    return {}
                }
                if {$computedTreeHash ne $amztreehash} {
                    puts stderr "Archive part ($range) SHA256 Tree Hash failure: locally computed: $computedTreeHash, returned: $amztreehash"
                    return {}
                }
            }
            set dfp [open $downloadpartfile w]
            fconfigure $dfp -translation binary
            if {[catch {fcopy $dfp $fp} ps]} {
                puts stderr "Archive part ($range) copy failed: $ps"
                return {}
            } elseif {$ps < $partsize} {
                puts stderr "Archive part ($range) partial copy: $ps instead of $partsize bytes"
                return {}
            }
            close $dfp
            set pos [expr {wide(pos) + $partsize}]
            set done [expr {wide($pos) >= wide($partialsize)}]
            set remainder [expr {$remainder - $partsize}]
        }
        close $fp
        if {$treehash ne {}} {
            if {[catch {$self run_java_GlacierClient TreeHash $filename} computedTreeHash]} {
                puts stderr "Failed to compute tree hash: $computedTreeHash"
                return {}
            }
            if {$computedTreeHash ne $treehash} {
                puts stderr "Archive SHA256 Tree Hash failure: locally computed: $computedTreeHash, in job descr: $wholetreehash"
                return {}
            } else {
                return $filename
            }
        } else {
            return $filename
        }
    }
    method deletearchive {vault archive} {
        set vnode [$self findvaultbyname $vault]
        if {$vnode eq {}} {
            error "No such vault: $vault"
        }
        set anode [$self  findarchivebydescr $vnode $archive]
        if {$anode eq {}} {
            error "No such archive in $vault: $archive"
        }
        if {[catch {$self run_java_GlacierClient DeleteArchive $vault [$anode attribute archiveid]} result]} {
            puts stderr "Failed to delete archive $vault/$archive: $result"
            return {}
        }
        $glacierVaultDB removearchive $vault [$anode attribute archiveid]
        return $result
    }
    method deletevault {vault} {
        set vnode [$self findvaultbyname $vault]
        if {$vnode eq {}} {
            error "No such vault: $vault"
        }
        if {[catch {$self run_java_GlacierClient DeleteVault $vault} result]} {
            puts stderr "Failed to delete vault $vault: $result"
            return {}
        }
        $glacierVaultDB removevault $vault
        return $result
    }
    
}

package provide BackupVault 2.0
