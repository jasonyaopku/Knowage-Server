package it.eng.spagobi.tools.glossary.dao.criterion;

import it.eng.spagobi.commons.dao.Criterion;
import it.eng.spagobi.tools.glossary.metadata.SbiGlWlist;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

public class SearchWlistByContentId implements Criterion<SbiGlWlist> {

	private final Integer contentId;

	public SearchWlistByContentId(Integer contentId) {
		this.contentId = contentId;
	}

	@Override
	public Criteria evaluete(Session session) {
		Criteria c = session.createCriteria(SbiGlWlist.class);
		c.add(Restrictions.eq("content.contentId", contentId));
		return c;
	}

}
