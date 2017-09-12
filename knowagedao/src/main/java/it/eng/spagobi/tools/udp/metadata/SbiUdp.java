/*
 * Knowage, Open Source Business Intelligence suite
 * Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.
 * 
 * Knowage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Knowage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.eng.spagobi.tools.udp.metadata;
// Generated 9-set-2010 10.57.32 by Hibernate Tools 3.1.0 beta3

import it.eng.spagobi.commons.metadata.SbiHibernateModel;

import java.util.HashSet;
import java.util.Set;


/**
 * SbiUdp generated by hbm2java
 */

public class SbiUdp  extends SbiHibernateModel {


    // Fields    

     private Integer udpId;
     private Integer typeId;
     private Integer familyId;
     private String label;
     private String name;
     private String description;
     private boolean isMultivalue;
     private Set sbiUdpValues = new HashSet(0);


    // Constructors

    /** default constructor */
    public SbiUdp() {
    }

	/** minimal constructor */
    public SbiUdp(Integer udpId, Integer typeId, Integer familyId, String label, String name) {
        this.udpId = udpId;
        this.typeId = typeId;
        this.familyId = familyId;
        this.label = label;
        this.name = name;
    }
    
    /** full constructor */
    public SbiUdp(Integer udpId, Integer typeId, Integer familyId, String label, String name, String description, boolean isMultivalue, Set sbiUdpValues) {
        this.udpId = udpId;
        this.typeId = typeId;
        this.familyId = familyId;
        this.label = label;
        this.name = name;
        this.description = description;
        this.isMultivalue = isMultivalue;
        this.sbiUdpValues = sbiUdpValues;
    }
    

   
    // Property accessors

    public Integer getUdpId() {
        return this.udpId;
    }
    
    public void setUdpId(Integer udpId) {
        this.udpId = udpId;
    }

    public Integer getTypeId() {
        return this.typeId;
    }
    
    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public Integer getFamilyId() {
        return this.familyId;
    }
    
    public void setFamilyId(Integer familyId) {
        this.familyId = familyId;
    }

    public String getLabel() {
        return this.label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isIsMultivalue() {
        return this.isMultivalue;
    }
    
    public void setIsMultivalue(boolean isMultivalue) {
        this.isMultivalue = isMultivalue;
    }

    public Set getSbiUdpValues() {
        return this.sbiUdpValues;
    }
    
    public void setSbiUdpValues(Set sbiUdpValues) {
        this.sbiUdpValues = sbiUdpValues;
    }
   








}