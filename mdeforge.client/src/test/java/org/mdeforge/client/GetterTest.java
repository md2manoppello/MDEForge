package org.mdeforge.client;

import static org.junit.Assert.assertNotNull;

import java.io.PrintWriter;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mdeforge.business.model.ATLTransformation;
import org.mdeforge.business.model.Artifact;
import org.mdeforge.business.model.Cluster;
import org.mdeforge.business.model.ETLTransformation;
import org.mdeforge.business.model.EcoreMetamodel;
import org.mdeforge.business.model.Model;
import org.mdeforge.business.model.Project;
import org.mdeforge.business.model.ValuedRelation;

public class GetterTest {

	private static ModelService modelService;
	private static ATLTransformationService atlTransformationService; 
	private static ETLTransformationService etlTransformationService; 
	private static EcoreMetamodelService ecoreMetamodelService; 
	private static WorkspaceService workspaceService; 
	@BeforeClass
	public static void setup() throws Exception {
		modelService = new ModelService("http://localhost:8080/mdeforge/", "maja", "majacdg");
		atlTransformationService = new ATLTransformationService("http://localhost:8080/mdeforge/", "maja", "majacdg");
		etlTransformationService = new ETLTransformationService("http://localhost:8080/mdeforge/", "maja", "majacdg");
		ecoreMetamodelService = new EcoreMetamodelService("http://localhost:8080/mdeforge/", "maja", "majacdg");
		workspaceService = new WorkspaceService("http://localhost:8080/mdeforge/", "maja", "majacdg");
	}
	
	@Ignore
	@Test
	public void getModelsTest() throws Exception {
		System.out.println("###Model");
		for (Model iterable_element : modelService.getModels()) {
			System.out.println(iterable_element.getName());
		}
		assertNotNull(modelService);
	}
	@Ignore
	@Test
	public void getEcoreMetamodelsTest() throws Exception {
		System.out.println("###Ecore");
		for (EcoreMetamodel iterable_element : ecoreMetamodelService.getEcoreMetamodels()) {
			System.out.println(iterable_element.getName());
		}
		assertNotNull(ecoreMetamodelService);
	}
	@Ignore
	@Test
	public void getETLTransformationsTest() throws Exception {
		System.out.println("###ETL");
		for (ETLTransformation iterable_element : etlTransformationService.getETLTransformations()) {
			System.out.println(iterable_element.getName());
		}
		assertNotNull(etlTransformationService);
	}
	@Ignore
	@Test
	public void getATLTransformationsTest() throws Exception {
		System.out.println("###ATL");
		for (ATLTransformation iterable_element : atlTransformationService.getATLTransformations()) {
			System.out.println(iterable_element.getName());
		}
		assertNotNull(atlTransformationService);
	}
	@Ignore
	@Test
	public void getEcoreMetanmodelInWorkspaceTest() throws Exception {
		System.out.println("###EcoreWorkspace");
		for (EcoreMetamodel iterable_element : workspaceService.getEcoreMetamodelsInWorkspace("5514aa53d4c67eee3e2c1b12")) {
			System.out.println(iterable_element.getName());
		}
		assertNotNull(workspaceService);
	}
	@Ignore
	@Test
	public void getEcoreMetamodel() {
		try {
			EcoreMetamodel emm = ecoreMetamodelService.getEcoreMetamodel("552bbd07d4c659da8e19ec99");
			System.out.println(emm);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}
	
	
	
	@Ignore
	@Test
	public void getEcoreMetamodelsPublicTest() throws Exception {
		System.out.println("###START");
		List<EcoreMetamodel> ecoreMMlist = ecoreMetamodelService.getEcoreMetamodelsPublic();
		EcoreMetamodel [] ecoreMMArray = ecoreMMlist.toArray(new EcoreMetamodel[ecoreMMlist.size()]);
		String [][] matrix = new String[ecoreMMArray.length][ecoreMMArray.length];
		for (int i = 0; i < ecoreMMArray.length-1; i++) {
			for (int j = i+1; j <ecoreMMArray.length; j++){
				String s = ecoreMetamodelService.getEcoreMetamodelSimilarity(ecoreMMArray[i].getId(), ecoreMMArray[j].getId());
				matrix[i][j] = s; 
				double d = Double.parseDouble(s);
				if(d>0.5)
					System.out.println("#######Iteration: " + i + "_" + ecoreMMArray[i].getName() + " : " + j + "_" + ecoreMMArray[j].getName() + " = " + d);
				else
					System.out.println("Iteration: " + i + "_" + ecoreMMArray[i].getName() + " : " + j + "_" + ecoreMMArray[j].getName() + " = " + d);
				
			}
		}
		String printString = "";
		PrintWriter p = new PrintWriter("/Users/juridirocco/Desktop/result.txt");
		for (int i = 86; i < matrix.length; i++){
			for (int j = 0; j <matrix[i].length; j++)
				printString +=(matrix[i][j] == null)?";":";";
			printString+="\n";
		}
		p.println(printString);
		p.close();
		System.out.println("FINITO!!!");
		assertNotNull(ecoreMetamodelService);
	}
	@Ignore
	@Test
	public void getCluster() {
		try {
			List<Cluster> clusterList = ecoreMetamodelService.getEcoreMetamodelCluster(1, 0.3);
			for (Cluster cluster : clusterList) {
				System.out.println("=======");
				
				for (Artifact art : cluster.getArtifacts()) {
					System.out.println(art.getName());
				}
				System.out.println("°°°");
				for (ValuedRelation rel  : cluster.getRelations()) {
					System.out.println(rel.getValue());
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Ignore
	@Test
	public void getProjectInAWorkspace() {
		try {
			List<Project> resutl = workspaceService.getProjectsInWorkspace("5514aa53d4c67eee3e2c1b12");
			for (Project project : resutl) {
				System.out.println(project.getName());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
