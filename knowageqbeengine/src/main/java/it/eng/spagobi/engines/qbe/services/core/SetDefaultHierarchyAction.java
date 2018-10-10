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
package it.eng.spagobi.engines.qbe.services.core;

import it.eng.qbe.model.structure.IModelEntity;
import it.eng.spago.base.SourceBean;
import it.eng.spagobi.utilities.assertion.Assert;
import it.eng.spagobi.utilities.engines.SpagoBIEngineServiceException;
import it.eng.spagobi.utilities.engines.SpagoBIEngineServiceExceptionHandler;
import it.eng.spagobi.utilities.service.JSONAcknowledge;

import java.io.IOException;

import org.apache.log4j.Logger;


public class SetDefaultHierarchyAction extends AbstractQbeEngineAction {

	private static final long serialVersionUID = 1080081357363500954L;
	public static final String SERVICE_NAME = "SET_DEFAULT_HIERARCHY_ACTION";
	@Override
	public String getActionName(){return SERVICE_NAME;}

	// INPUT PARAMETERS
	public static final String FIELD_NAME = "fieldId";
	public static final String PARENT_ENTITY_UNIQUE_NAME = "entityId";

	/** Logger component. */
    public static transient Logger logger = Logger.getLogger(SetDefaultHierarchyAction.class);


	@Override
	public void service(SourceBean request, SourceBean response)  {

		String fieldName;
		String parentEntityUniqueName;

		logger.debug("IN");

		try {
			super.service(request, response);

			fieldName = this.getAttributeAsString( FIELD_NAME );
			parentEntityUniqueName = this.getAttributeAsString( PARENT_ENTITY_UNIQUE_NAME );

			logger.debug("Parameter [" + PARENT_ENTITY_UNIQUE_NAME + "] is equals to [" + parentEntityUniqueName + "]");
			Assert.assertNotNull(getEngineInstance(), "It's not possible to execute " + this.getActionName() + " service before having properly created an instance of EngineInstance class");

			String[] peun = parentEntityUniqueName.split("::");
			String parentEntityName = peun[0];
			IModelEntity parentEntity = getDataSource().getModelStructure().getEntity(parentEntityUniqueName);
			parentEntity.getHierarchicalDimensionByEntity(parentEntityName).setDefaultHierarchy(fieldName);


			try {
				writeBackToClient( new JSONAcknowledge() );
			} catch (IOException e) {
				String message = "Impossible to write back the responce to the client";
				throw new SpagoBIEngineServiceException(getActionName(), message, e);
			}

		} catch(Throwable t) {
			throw SpagoBIEngineServiceExceptionHandler.getInstance().getWrappedException(getActionName(), getEngineInstance(), t);
		} finally {
			logger.debug("OUT");
		}
	}

}