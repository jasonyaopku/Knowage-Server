package it.eng.spagobi.metadata.metadata;

// Generated 12-apr-2016 10.43.25 by Hibernate Tools 3.4.0.CR1

import it.eng.spagobi.commons.metadata.SbiHibernateModel;

/**
 * SbiMetaDsBc generated by hbm2java
 */
public class SbiMetaDsBc extends SbiHibernateModel {

	private SbiMetaDsBcId id;

	// private SbiMetaBc sbiMetaBc;

	public SbiMetaDsBc() {
	}

	// public SbiMetaDsBc(SbiMetaDsBcId id, SbiMetaBc sbiMetaBc) {
	public SbiMetaDsBc(SbiMetaDsBcId id) {
		this.id = id;
		// this.sbiMetaBc = sbiMetaBc;
	}

	public SbiMetaDsBcId getId() {
		return this.id;
	}

	public void setId(SbiMetaDsBcId id) {
		this.id = id;
	}

	// public SbiMetaBc getSbiMetaBc() {
	// return this.sbiMetaBc;
	// }
	//
	// public void setSbiMetaBc(SbiMetaBc sbiMetaBc) {
	// this.sbiMetaBc = sbiMetaBc;
	// }

}
