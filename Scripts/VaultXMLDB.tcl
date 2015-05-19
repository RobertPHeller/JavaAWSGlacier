##############################################################################
#
#  System        : 
#  Module        : 
#  Object Name   : $RCSfile$
#  Revision      : $Revision$
#  Date          : $Date$
#  Author        : $Author$
#  Created By    : Robert Heller
#  Created       : Wed Jun 25 18:38:42 2014
#  Last Modified : <150519.1553>
#
#  Description	
#
#  Notes
#
#  History
#	
##############################################################################
#
#    TclAWSGlacier
#    Copyright (C) 2014  Robert Heller D/B/A Deepwoods Software
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
##############################################################################


package require snit
package require ParseXML

snit::type VaultXMLDB {
    delegate option * to db
    delegate method * to db
    component db
    typemethod newdb {{name %%AUTO%%}} {
        return [$type create $name {<vaults/>}]
    }
    typemethod olddb {filename {name %%AUTO%%}} {
        if {[catch {open $filename r} fp]} {
            error "$type olddb: could not open $filename: $fp"
        }
        set xml [read $fp]
        close $fp
        return [$type create $name $xml]
    }
    method savedb {filename} {
        if {[file exists $filename]} {
            catch {file rename -force $filename ${filename}.bak}
        }
        if {[catch {open $filename w} fp]} {
            error "$type olddb: could not open $filename: $fp"
        }
        $db displayTree $fp
        close $fp
    }
    constructor {xml args} {
        install db using ParseXML %%AUTO%% $xml
        #$self configurelist $args
    }
    method addvault {location date {id {}}} {
        if {[regexp {^/([[:digit:]]+)/vaults/(.+)$} $location => accountid vaultname] < 1} {
            error "$self addvault: Illformed Location: $location"
        }
        set vaultsnode [$self getElementsByTagName vaults]
        set attrs {}
        if {$id ne {}} {
            lappend attrs id $id
        }
        lappend attrs location $location name $vaultname accountid $accountid \
              date $date
            
        set newvault [[$vaultsnode info type] create %%AUTO%% -tag vault \
                      -attributes $attrs]
        $vaultsnode addchild $newvault
        return $newvault
    }
    method removevault {vname} {
        set vaultsnode [$self getElementsByTagName vaults]
        foreach vault [$vaultsnode  getElementsByTagName vault] {
            if {[$vault attribute name] eq $vname} {
                $vaultsnode removeChild $vault
                return
            }
        }
    }
    method findvaultbyname {vname} {
        set vaultsnode [$self getElementsByTagName vaults]
        foreach vault [$vaultsnode  getElementsByTagName vault] {
            if {[$vault attribute name] eq $vname} {
                return $vault
            }
        }
        return {}
    }
    method findarchivebydescr {vault archivedescr} {
        foreach a [$vault getElementsByTagName archive] {
            set adescrnodes [$a getElementsByTagName description]
            if {[llength $adescrnodes] < 1} {continue}
            set adescrnode [lindex $adescrnodes 0]
            set adesc [$adescrnode data]
            if {$adesc eq $archivedescr} {return $a}
        }
        return {}
    }
    method findarchivebyaid {vault archiveid} {
        if {$vault == ""} {return {}}
        foreach a [$vault getElementsByTagName archive] {
            if {[$a attribute archiveid] eq $archiveid} {return $a}
        }
        return {}
    }
    method addarchive {location date size sha256treehash {description {}} {id {}}} {
        if {[regexp {^/([[:digit:]]+)/vaults/([^/]+)/archives/(.+)$} \
             $location => accountid vaultname archiveid] < 1} {
            error "$selfns addarchive: Illformed location: $location"
        }
        set vaults [$self getElementsByTagName vault]
        set vaultnode {}
        foreach v $vaults {
            if {[$v attribute name] eq $vaultname} {
                set vaultnode $v
                break
            }
        }
        if {$vaultnode eq {}} {
            error "No such vault: $vaultname"
        }
        set attrs {}
        if {$id ne {}} {
            lappend attrs id $id
        }
        lappend attrs location $location archiveid $archiveid date $date
        set newarchive [[$vaultnode info type] create %%AUTO%% -tag archive \
                        -attributes $attrs]
        set snode [[$newarchive info type] create %%AUTO%% -tag size]
        $snode setdata $size
        $newarchive addchild $snode
        set sha256node [[$newarchive info type] create %%AUTO%% -tag sha256treehash]
        $sha256node setdata $sha256treehash
        $newarchive addchild $sha256node
        if {$description ne {}} {
            set descrnode [[$newarchive info type] create %%AUTO%% -tag description]
            $descrnode setdata $description
            $newarchive addchild $descrnode
        }
        $vaultnode addchild $newarchive
        return $newarchive
    }
    method removearchive {vname aid} {
        #puts stderr "*** $self removearchive $vname $aid"
        set vnode [$self findvaultbyname $vname]
        foreach a [$vnode getElementsByTagName archive] {
            #puts stderr "*** $self removearchive: a = $a, archiveid is [$a attribute archiveid]"
            if {[$a attribute archiveid] eq $aid} {
                $vnode removeChild $a
            }
        }
        return {}
    }
    
}

package provide VaultXMLDB 1.0
