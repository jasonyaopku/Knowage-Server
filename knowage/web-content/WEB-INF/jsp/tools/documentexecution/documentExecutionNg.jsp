<%--
Knowage, Open Source Business Intelligence suite
Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.

Knowage is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

Knowage is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
--%>

<%@page import="it.eng.spago.security.IEngUserProfile"%>
<%@page import="it.eng.spagobi.analiticalmodel.document.bo.BIObject"%>
<%@page import="it.eng.spagobi.commons.utilities.ObjectsAccessVerifier"%>
<%@page import="it.eng.spagobi.utilities.engines.rest.ExecutionSession"%>

<%@ page language="java" pageEncoding="utf-8" session="true"%>

<%@ include file="/WEB-INF/jsp/commons/angular/angularResource.jspf"%>

<%
BIObject obj;
Integer objId;
String objLabel;
IEngUserProfile profile;
List<String> executionRoleNames = new ArrayList();

try{
	profile = (IEngUserProfile)permanentSession.getAttribute(IEngUserProfile.ENG_USER_PROFILE);
	objId = new Integer(request.getParameter("OBJECT_ID"));
	objLabel = request.getParameter("OBJECT_LABEL");
	
	executionRoleNames = ObjectsAccessVerifier.getCorrectRolesForExecution(objId, profile);
}catch (Throwable t) {
	
}

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<%@include file="/WEB-INF/jsp/commons/angular/angularImport.jsp"%>
	
	<!-- Styles -->
	<link rel="stylesheet" type="text/css" href="/knowage/themes/commons/css/customStyle.css"> 
	<script type="text/javascript" src="<%=urlBuilder.getResourceLink(request, "js/src/angular_1.4/tools/documentbrowser/md-data-table.min.js")%>"></script>
	<script type="text/javascript" src="<%=urlBuilder.getResourceLink(request, "js/src/angular_1.4/tools/commons/document-tree/DocumentTree.js")%>"></script>
</head>

<body class="bodyStyle" ng-app="documentExecutionModule">
	<div layout="column" ng-controller="documentExecutionController as ctrl" ng-init="initSelectedRole()" ng-cloak>
			
		<md-toolbar class="miniheadimportexport">
            <div class="md-toolbar-tools" layout="row" layout-align="center center">
                <i class="fa fa-file-text-o fa-2x"></i>
                <span>&nbsp;&nbsp;</span>
                <h2 class="md-flex">
                	{{translate.load("sbi.generic.document")}}: <%= request.getParameter("OBJECT_NAME") %> - ({{translate.load("sbi.browser.defaultRole.role")}} {{selectedRole}})
                </h2>
                
                <span flex=""></span>

				<md-button class="toolbar-button-custom" aria-label="Parameters"
						title="{{translate.load('sbi.scheduler.parameters')}}"
						ng-click="toggleParametersPanel()" 
						<%-- ng-disabled="isParameterPanelDisabled()" --%>
				>
					<i class="fa fa-cog header"></i> 
				</md-button>
	
			</div>
        </md-toolbar>

 		<section layout="row" layout-fill> 
 			<md-content layout="column" layout-fill>
				<iframe ng-src="{{documentUrl}}"
				<%--
					iframe-set-dimensions-onload 
					style="overflow:hidden;height:100%;width:100%" height="100%"
				--%>
					> </iframe>
			</md-content>
		
			<md-sidenav class="md-sidenav-right" md-component-id="parametersPanelSideNav"
					ng-class="{'md-locked-open': showParametersPanel}" md-is-locked-open="$mdMedia('gt-md')" >
					
				<md-input-container class="small counter">
					<label>{{translate.load("sbi.users.roles")}}</label>
					<md-select aria-label="aria-label" ng-model="selectedRole" ng-show="showSelectRoles">
						<md-option ng-click="changeRole(role)" ng-repeat="role in roles" value="{{role}}">{{role|uppercase}}</md-option>
					</md-select>
				</md-input-container>
				
				<!-- execute button -->
				<md-button class="toolbar-button-custom md-raised" ng-disabled="isExecuteParameterDisabled()"
						title="{{translate.load('sbi.execution.parametersselection.executionbutton.message')}}"  
						ng-click="executeParameter()">
					{{translate.load("sbi.execution.parametersselection.executionbutton.message")}}
				</md-button>
				
				<md-list>
					<md-list-item layout="row" ng-repeat="param in documentParameters">
						<md-input-container class="small counter" ng-show="param.visible">
							<md-content ng-if="param.type=='STRING' && param.selectionType=='LIST'">
								param : {{param | json}}
							</md-content>
							<label>{{param.label}}</label>
							<input class="input_class" ng-model="param.parameterValue" 
									ng-required="param.mandatory" ng-if="param.type=='STRING' && param.selectionType==''">
							
							<md-select class="input_class" ng-model="param.parameterValue" ng-required="param.mandatory"
									ng-if="param.type=='STRING' && param.selectionType=='LIST'" multiple="param.multivalue">
								<md-option></md-option>
							</md-select>
						</md-input-container>
					</md-list-item>
				</md-list>
			</md-sidenav>
		</section>
	</div>
		
	<script type="text/javascript">
	//Module creation
	(function() {
		
		angular.module('documentExecutionModule', ['md.data.table', 'ngMaterial', 'ui.tree', 'sbiModule', 'document_tree']);
		
		angular.module('documentExecutionModule').factory('execProperties', function() {
			var obj = {
				roles: [<%for(Object roleObj : executionRoleNames) out.print("'" + (String)roleObj + "',");%>],
				executionInstance: {
					'OBJECT_ID' : '<%= request.getParameter("OBJECT_ID") %>', 
					'OBJECT_LABEL' : '<%= request.getParameter("OBJECT_LABEL") %>',
					'isFromCross' : false, 
					'isPossibleToComeBackToRolePage' : false
				},
				parametersData: {
					parameterList: [],
					showParametersPanel: false
				}
			};
			return obj;
		});
		
	})();
	</script>
	<script type="text/javascript" 
			src="<%=urlBuilder.getResourceLink(request, "js/src/angular_1.4/tools/documentexecution/documentExecution.js")%>"></script>
	<!--
	<script type="text/javascript"
			src="${pageContext.request.contextPath}/js/src/angular_1.4/tools/documentexecution/parametersPanel/parametersPanelController.js"></script>
	-->
</body>
</html>
