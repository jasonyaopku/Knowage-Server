
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


angular.module('olap_designer_toolbar')
		.service('OlapTemplateService',function(){
			
			const SCENARIO_NAME = "scenario";
			this.template = {};
			var olap={};
			/*var cubeName = "foodMart";
			var mdxQueryObj = {
					"mdxQuery":"SELECT {[Measures].[Unit Sales]} ON COLUMNS, {[Product]} ON ROWS FROM [Sales_V]",
					"clickables":[
					             {
					            	 "uniqueName":"[Product].[Product Family]",
					                 "clickParameter":{"name":"ParProdFamily","value":"{0}"}
					             },
					             {
					            	 "uniqueName":"[Product].[Product Name]",
					                 "clickParameter":{"name":"ParProdFamily","value":"{0}"}
					             }
					             
					             
					             ]
			
					
			
					
			};
			var MDXMondrianQuery = "SELECT {[Measures].[Unit Sales]} ON COLUMNS, {[Product]} ON ROWS FROM [Sales_V]";
			var scenario =  {
				     
				      "editCube": "Sales_Edit",
				      
				      "measures": [{"name":"Store Sales"},{"name":"Store Cost"}]
				       
				      ,
				      "variables": [
				        {
				          "name": "var",
				          "value": "5"
				        },
				        {
				          "name": "PD",
				          "value": "[Product].[Drink.Dairy]",
				          "type": "string"
				        }
				      ]
				    }
			
			var crossNavigation = 
		                [
		                    {
		                    "name":"family",
		                    "scope":"relative",
		                    "dimension":"Product",
		                    "hierarchy":"[Product]",
		                    "level":"[Product].[Product Family]"
		                    },
		                    {
			                    "name":"dragan",
			                    "scope":"relative",
			                    "dimension":"Product",
			                    "hierarchy":"[Product]",
			                    "level":"[Product].[Product Family]"
			                    }
		                    ]
		            ;
			
			var toolbarButtons = [
			                      {"name":"BUTTON_FATHER_MEMBERS","visible":"true","clicked":"true"},
			                      {"name":"BUTTON_HIDE_SPANS","visible":"true","clicked":"true"}
			                      ]
		    
			console.log('Hello from OlapTemplateService');
			*/
			this.getTemplateObject = function(){
				return this.template;
			}
			
			this.getTempateJson = function(){
				if(this.getTemplateObject()){
					return angular.toJson(this.getTemplateObject());
				}
			}
			
			this.setTemplateObject = function(templateObject){
				this.templateObject = templateObject;
			}
			
			this.getOlapTag = function(){
				
				if(this.getTemplateObject()){
					
					return this.getTemplateObject().olap;
			
				}else{
					
				console.log("template is undefined!!!");
				}
			}
			
			this.setOlapTag = function(olap){
				if(this.getTemplateObject()){
					
						this.getTemplateObject().olap = olap;
				
				}else{
					console.log("template is undefined!!!");
				}
			}
			
			this.deleteOlapTag = function(){
				delete this.getTemplateObject().olap;
			}
			
			
			this.getCubeTag = function(){
				if(this.getOlapTag()){	
					return this.getOlapTag().cube;
				}else{
					console.log("Olap object is undefined!!!");
				}
				
			}
			
			this.getCubeReference = function(){
				if(this.getCubeTag()){
					return this.getCubeTag().reference;
				}
			}
			
			this.setCubeTag = function(cubeName){
				if(this.getOlapTag()){
					if(!this.getCubeTag()){
						this.getOlapTag().cube = {};
					}
					if(cubeName&&cubeName!==""){
						this.getCubeTag().reference = cubeName;
						
					}else{
						console.log("Cube name is empty!!!");
						this.deleteCubeTag();
						return false;
					}
					
				}else{
					console.log("Olap object is undefined!!!");
					return false;
				}
				return true;
			}
			
			
			this.deleteCubeTag = function(){
				if(this.getOlapTag()){
					 delete this.getOlapTag().cube;
				}else{
					console.log("Olap object is undefined!!!");
				}
				
			}
			
			this.getMdxQueryTag = function(){
				if(this.getOlapTag()){	
					return this.getOlapTag().MDXQUERY;
				}else{
					console.log("Olap object is undefined!!!");
				}
				
			}
			
			this.getMdxQuery = function(){
				if(this.getMdxQueryTag()){
					return this.getMdxQueryTag().XML_TAG_TEXT_CONTENT;
				}else{
					console.log("MdxQueryTag object is undefined!!!");
				}
			}
			
			this.getMdxQueryClickables = function(){
				if(this.getMdxQueryTag()){
					return this.getMdxQueryTag().clickable;
				}else{
					console.log("MdxQueryTag object is undefined!!!");
				}
			}
			
			this.setMdxQueryTag = function(mdxQueryObj){
				if(this.getOlapTag()){
					 if(!this.getMdxQueryTag()){
						 this.getOlapTag().MDXQUERY = {};
					 }
					 
					 if(mdxQueryObj&&mdxQueryObj.mdxQuery){
						 this.getMdxQueryTag().XML_TAG_TEXT_CONTENT = mdxQueryObj.mdxQuery;
						 
						 
						 if(mdxQueryObj.clickables){
							 if(mdxQueryObj.clickables.constructor === Array&&mdxQueryObj.clickables.length>0){
								 this.getMdxQueryTag().clickable = [];
								 	for(var i= 0;i<mdxQueryObj.clickables.length;i++){
								 		var clickable = mdxQueryObj.clickables[i];
								 		if(clickable&&clickable.uniqueName&&clickable.clickParameter){
								 			if(clickable.clickParameter.name&&clickable.clickParameter.value){
								 				 this.getMdxQueryTag().clickable.push(clickable);
								 			}else{
								 				console.log("Bad format of clickParameter!!!Mandatory properties: name,value ");
								 				this.deleteMdxQueryTag();
								 				return false;
								 			}
								 		}else{
								 			console.log("Bad format of clickable!!!Mandatory properties: uniqueName,clickParameter ");
								 			this.deleteMdxQueryTag();
								 			return false;
								 		}
								 	}
							 }else{
								 console.log("Bad format of clickables!!!Mandatory clickables is array of clickable ");
								 this.deleteMdxQueryTag();
								 return false;
							 }
						 }
						 
							
					 }else{
						 console.log("Bad format of mdxQueryObj!!!Mandatory property: mdxQuery ") ;
						 this.deleteMdxQueryTag();
						 return false;
					 }
					 
					 
					 
				}else{
					console.log("Olap object is undefined!!!");
					return false;
				}
				return true;
			}
			
			this.deleteMdxQueryTag = function(){
				if(this.getOlapTag()){
					 delete this.getOlapTag().MDXQUERY;
				}else{
					console.log("Olap object is undefined!!!");
				}
			}
			
			this.getMDXMondrianQueryTag = function(){
				if(this.getOlapTag()){	
					return this.getOlapTag().MDXMondrianQuery;
				}else{
					console.log("Olap object is undefined!!!");
				}
				
			}
			
			this.getMDXMondrianQuery = function(){
				if(this.getOlapTag()){
					if(this.getMDXMondrianQueryTag()){
						return this.getMDXMondrianQueryTag().XML_TAG_TEXT_CONTENT;
					}
					
				}else{
					console.log("Olap object is undefined!!!");
				}
			}
			
			this.setMDXMondrianQueryTag = function(MDXMondrianQuery){
				if(this.getOlapTag()){
					 if(!this.getMDXMondrianQueryTag()){
						 this.getOlapTag().MDXMondrianQuery = {};
					 }
					 if(MDXMondrianQuery&&MDXMondrianQuery!==""){
						 this.getMDXMondrianQueryTag().XML_TAG_TEXT_CONTENT = MDXMondrianQuery;
						 return true;
					 }else{
						 console.log("mdxMondrianQuery is empty!!!");
						 this.deleteMDXMondrianQueryTag();
						 return false;
					 }
					 
					 
				}else{
					console.log("Olap object is undefined!!!");
				}
				
				return false;
				
			}
			
			this.deleteMDXMondrianQueryTag = function(){
				if(this.getOlapTag()){
					 delete this.getOlapTag().MDXMondrianQuery;
				}else{
					console.log("Olap object is undefined!!!");
				}
			}
			
			this.getScenarioTag = function(){
				if(this.getOlapTag()){	
					return this.getOlapTag().SCENARIO;
				}else{
					console.log("Olap object is undefined!!!");
				}
				
			}
			
			this.getScenarioObject = function(){
				var scenarioObj = {};
				
				if(this.getScenarioTag()){
					scenarioObj.name = this.getScenarioTag().name;
					scenarioObj.editCube = this.getScenarioTag().editCube;
					if(this.getScenarioTag().MEASURE){
						scenarioObj.measures = [];
						for(var i = 0;i<this.getScenarioTag().MEASURE.length;i++){
							var temp = {};
							temp.name = this.getScenarioTag().MEASURE[i].XML_TAG_TEXT_CONTENT;
							scenarioObj.measures.push (temp);
						}
						
					}
					
					if(this.getScenarioTag().VARIABLE){
						scenarioObj.variables = this.getScenarioTag().VARIABLE;
					}
					
				}
				
				return scenarioObj;
			}
			
			this.setScenarioTag = function(scenario){
				if(this.getOlapTag()){
					 if(scenario.editCube&&scenario.editCube!==""&&scenario.measures&&scenario.measures.constructor === Array&&scenario.measures.length>0){
						 
						 if(!this.getScenarioTag()){
							 this.getOlapTag().SCENARIO = {};
						 }
						 this.getScenarioTag().name = SCENARIO_NAME;
						 this.getScenarioTag().editCube = scenario.editCube;
						 
						 this.getScenarioTag().MEASURE = [];
						 for(var i = 0; i<scenario.measures.length;i++){
							 var temp = {};
							 if(scenario.measures[i].name){
								 temp.XML_TAG_TEXT_CONTENT = scenario.measures[i].name;
								 this.getScenarioTag().MEASURE.push(temp);
							 }else{
								 console.log("Bad format of measure!!!Mandatory property: name ") ;
							 }
							 
							 
						 }
						 
						 if(scenario.variables&&scenario.variables.constructor === Array&&scenario.variables.length>0){
							 this.getScenarioTag().VARIABLE = [];
							 for(var i = 0; i<scenario.variables.length;i++){
								 if(scenario.variables[i].name&&scenario.variables[i].value){
									 this.getScenarioTag().VARIABLE.push(scenario.variables[i]);
								 }else{
									 this.deleteScenarioTag ();
									 console.log("Bad format of variable!!!Mandatory properties: name and value ");
									 return false;
								 }
								 
							 }
						 }
						 
						 return true;
						 
					 }else{
						 console.log("Bad format of scenario!!!Mandatory properties: editCube and array of measures ");
						 return false;
					 }
					 
				}else{
					console.log("Olap object is undefined!!!");
				}
				
				return false;
				
			}
			
			this.deleteScenarioTag = function(){
				if(this.getOlapTag()){
					 delete this.getOlapTag().SCENARIO;
				}else{
					console.log("Olap object is undefined!!!");
				}
			}
			
			this.getCrossNavigationTag = function(){
				if(this.getOlapTag()){	
					return this.getOlapTag().CROSS_NAVIGATION;
				}else{
					console.log("Olap object is undefined!!!");
				}
				
			}
			
			this.getCrossNavigation = function(){
				var crossNavigation;
				
				if(this.getCrossNavigationTag()&&this.getCrossNavigationTag().PARAMETERS){
					if(this.getCrossNavigationTag().PARAMETERS.PARAMETER){
						crossNavigation = this.getCrossNavigationTag().PARAMETERS.PARAMETER;
					}
					
				}
				
				return crossNavigation;
			}
			
			this.setCrossNavigationTag = function(crossNavigation){
				if(this.getOlapTag()){
					 if(crossNavigation&&crossNavigation.constructor === Array&&crossNavigation.length>0){
						 
						 if(!this.getCrossNavigationTag()){ 
							 this.getOlapTag().CROSS_NAVIGATION ={}
						 }
						 
						 this.getCrossNavigationTag().PARAMETERS = {};
						 
						 this.getCrossNavigationTag().PARAMETERS.PARAMETER = [];
						 
						 for(var i = 0;i<crossNavigation.length;i++){
							 var parameter = crossNavigation[i];
							 
							 if(parameter.name&&
								parameter.scope&&
								parameter.dimension&&
								parameter.hierarchy&&
								parameter.level){
								 
								 this.getCrossNavigationTag().PARAMETERS.PARAMETER.push(parameter);
								 
							 }else{
								 console.log("Bad format of parameter!!!Mandatory properties:name,scope,dimension,hierarchy,level");
								 this.deleteCrossNavigationTag();
								 return  false;
								 
							 }
							 
							 if(i===crossNavigation.length-1&&this.getCrossNavigationTag().PARAMETERS.PARAMETER.length === 0){
								 this.deleteCrossNavigationTag();
								 console.log("Bad format of parameter!!!Array is empty");
								 return false;
							 }
						 }
						 return true;
					 }else{
						 console.log("Bad format of crossNavigation!!!Mandatory crossNavigation is array of parameters ");
					 }
					 
				}else{
					console.log("Olap object is undefined!!!");
				}
				
				return false;
				
			}
			
			this.deleteCrossNavigationTag = function(){
				if(this.getOlapTag()){
					 delete this.getOlapTag().CROSS_NAVIGATION;
				}else{
					console.log("Olap object is undefined!!!");
				}
			}
			
			this.getToolbarTag = function(){
				if(this.getOlapTag()){	
					return this.getOlapTag().TOOLBAR;
				}else{
					console.log("Olap object is undefined!!!");
				}
			}
			
			this.getToolbarButtons = function(){
				var toolbarButtons =[];
				if(this.getToolbarTag()){
					var keys = Object.keys(this.getToolbarTag());
					for(var i =0;i<keys.length;i++){
						var button = {};
						button.name = keys[i];
						button.visible = this.getToolbarTag()[keys[i]].visible;
						button.clicked = this.getToolbarTag()[keys[i]].clicked;
						toolbarButtons.push(button);
					}
				}
				return toolbarButtons;
			}
			
			this.setToolbarTag = function(toolbarButtons){
				
				if(toolbarButtons&&toolbarButtons.constructor === Array&&toolbarButtons.length>0){
					
					if(!this.getOlapTag().TOOLBAR){
						this.getOlapTag().TOOLBAR = {};
					}
					
					for(var i = 0;i<toolbarButtons.length;i++){
						
						if(toolbarButtons[i].name&&toolbarButtons[i].visible&&toolbarButtons[i].clicked){
							this.getOlapTag().TOOLBAR[toolbarButtons[i].name] = {};
							this.getOlapTag().TOOLBAR[toolbarButtons[i].name].visible = toolbarButtons[i].visible;
							this.getOlapTag().TOOLBAR[toolbarButtons[i].name].clicked = toolbarButtons[i].clicked;
						}else{
							
							console.log("Bad format of button!!!Mandatory proparties: name,visible,clicked ");
							this.deleteToolbarTag();
							return false;
						}
					}
					return true;
				}else{
					console.log("Bad format of toolbarButtons!!!Mandatory toolbarButtons is array of buttons ");
					
				}
				return false;
			}
			
			this.deleteToolbarTag = function(){
				if(this.getOlapTag()){
					 delete this.getOlapTag().TOOLBAR;
				}else{
					console.log("Olap object is undefined!!!");
				}
			}
			
			
			
			
			 this.setOlapTag(olap);
			 console.log(angular.toJson(this.template));
			 
			 /*
			 var cubeSuccess = this.setCubeTag(cubeName);
			 console.log("cubeSuccess "+ cubeSuccess);
			 console.log(angular.toJson(this.template));
			 
			 
			 var mdxQuerySuccess = this.setMdxQueryTag(mdxQueryObj);
			 console.log("mdxQuerySuccess "+ mdxQuerySuccess);
			 console.log(angular.toJson(this.template));
			 
			 var mDXMondrianQuerySuccess = this.setMDXMondrianQueryTag(MDXMondrianQuery);
			 console.log("mDXMondrianQuerySuccess "+mDXMondrianQuerySuccess);
			 console.log(angular.toJson(this.template));
			 
			 var scenarioSuccess = this.setScenarioTag(scenario);
			 console.log("scenarioSuccess "+scenarioSuccess);
			 console.log(angular.toJson(this.template));
			 
			 var crossNavigationSuccess = this.setCrossNavigationTag(crossNavigation);
			 console.log("crossNavigationSuccess "+crossNavigationSuccess);
			 console.log(angular.toJson(this.template));
			 
			 var toolbarSuccess =  this.setToolbarTag(toolbarButtons);
			 console.log("toolbarSuccess "+toolbarSuccess);
			 console.log(angular.toJson(this.template));
			 
			 console.log("Cube ");
			 console.log(this.getCubeReference());
			 console.log("Scenario ");
			 console.log(this.getScenarioObject());
			 console.log("MdxQuery ");
			 console.log(this.getMdxQuery());
			 console.log("Clickables ");
			 console.log(this.getMdxQueryClickables());
			 console.log("MdxMondrianQuery ");
			 console.log(this.getMDXMondrianQuery());
			 console.log("CrossNavigation ");
			 console.log(this.getCrossNavigation());
			 console.log("Toolbar buttons ");
			 console.log(this.getToolbarButtons());
			 
			 this.deleteCubeTag();
			 console.log(angular.toJson(this.template));
			 this.deleteMdxQueryTag();
			 console.log(angular.toJson(this.template));
			 this.deleteMDXMondrianQueryTag();
			 console.log(angular.toJson(this.template));
			 this.deleteScenarioTag();
			 console.log(angular.toJson(this.template));
			 this.deleteCrossNavigationTag();
			 console.log(angular.toJson(this.template));
			 this.deleteToolbarTag();
			 console.log(angular.toJson(this.template));
			 this.deleteOlapTag();
			 console.log(angular.toJson(this.template));
			 this.deleteCubeTag();
			 console.log(angular.toJson(this.template));
			 */
		})