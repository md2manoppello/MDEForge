package org.mdeforge.business.impl;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.mdeforge.business.BusinessException;
import org.mdeforge.business.GridFileMediaService;
import org.mdeforge.business.ModelService;
import org.mdeforge.business.ProjectService;
import org.mdeforge.business.RequestGrid;
import org.mdeforge.business.ResponseGrid;
import org.mdeforge.business.UserService;
import org.mdeforge.business.WorkspaceService;
import org.mdeforge.business.model.ConformToRelation;
import org.mdeforge.business.model.EcoreMetamodel;
import org.mdeforge.business.model.Metamodel;
import org.mdeforge.business.model.Model;
import org.mdeforge.business.model.Project;
import org.mdeforge.business.model.User;
import org.mdeforge.business.model.Workspace;
import org.mdeforge.integration.EcoreMetamodelRepository;
import org.mdeforge.integration.ModelRepository;
import org.mdeforge.integration.ProjectRepository;
import org.mdeforge.integration.RelationRepository;
import org.mdeforge.integration.UserRepository;
import org.mdeforge.integration.WorkspaceRepository;
import org.mdeforge.search.jsonMongoUtils.EmfjsonMongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.stereotype.Service;

import com.mongodb.Mongo;


@Service
public class ModelServiceImpl implements ModelService {

	@Autowired
	private Mongo mongo;
	@Autowired
	private SimpleMongoDbFactory mongoDbFactory;
	@Autowired
	private ModelRepository modelRepository;
	@Autowired
	private EcoreMetamodelRepository ecoreMetamodelRepository;
	@Autowired
	private RelationRepository relationRepository;
	@Autowired
	private ProjectService projectService;
	@Autowired
	private ProjectRepository projectRepository;
	@Autowired
	private WorkspaceService workspaceService;
	@Autowired
	private WorkspaceRepository workspaceRepository;
	@Autowired
	private GridFileMediaService gridFileMediaService; 
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private UserService userService;
	@Value("#{cfgproperties[mongoPrefix]}")
	private String mongoPrefix;
	@Value("#{cfgproperties[jsonArtifactCollection]}")
	private String jsonArtifactCollection;
	
	@Override
	public void upload(Model model) {
		modelRepository.save(model);
	}

	@Override
	public void download(Model model) {
		// TODO Auto-generated method stub
	}

	@Override
	public void delete(Model model) {
		modelRepository.delete(model);

	}

	@Override
	public List<Model> findModelsByMetamodel(Metamodel metamodel) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public ResponseGrid<Model> findAllPaginated(RequestGrid requestGrid) throws BusinessException {
		Page<Model>  rows = null;
		if (requestGrid.getSortDir().compareTo("asc")==0){
			rows = modelRepository.findAll(new PageRequest(requestGrid.getiDisplayStart(), requestGrid.getiDisplayLength(),Direction.ASC, requestGrid.getSortCol()));
		}else{
			rows = modelRepository.findAll(new PageRequest(requestGrid.getiDisplayStart(), requestGrid.getiDisplayLength(),Direction.DESC, requestGrid.getSortCol()));
		}
		return new ResponseGrid<Model>(requestGrid.getsEcho(), rows.getNumberOfElements(), rows.getTotalElements(), rows.getContent());
	}
	
	@Override
	public Model findByName(String name) throws BusinessException {
		return modelRepository.findByName(name);
	}

	@Override
	public void update(Model model) {
		modelRepository.save(model);
	}
	

	@Override
	public List<Model> findAll() {
		return modelRepository.findAll();
	}

//	@Override
//	public boolean isValid(EmfModel model) {
//		
//	}

//	@Override
//	public List<String> getMetamodelsURIs() {
//		// TODO Auto-generated method stub
//		return null;
//	}

//	@Override
//	public List<URI> getMetamodelFileUris(EmfModel model) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	
	public String create(Model model) throws BusinessException {
		// check workspace Auth
		try {
			// GetUser
			if (model.getId() != null)
				throw new BusinessException();

//			// File handler
//			GridFileMedia fileMedia = new GridFileMedia();
//			fileMedia.setFileName(model.getName());
//			fileMedia.setByteArray(Base64.decode(model.getFile()
//					.getContent().getBytes()));
//			model.setFile(fileMedia);

			// check workspace Auth
			for (Workspace ws : model.getWorkspaces()) {
				workspaceService.findById(ws.getId(), model.getAuthor());
			}
			// check project Auth
			for (Project p : model.getProjects()) {
				projectService.findById(p.getId(), model.getAuthor());
			}
//			if (model.getFile() != null) {
//				gridFileMediaService.store(model.getFile());
//			}
			model.setCreated(new Date());
			model.setModified(new Date());
			
			User user = userRepository.findOne(model.getAuthor().getId());
			model.setAuthor(user);
			model.getShared().add(user);
			
//			ConformToRelation rel = new ConformToRelation();
//			rel.setFromArtifact(model);
//			rel.setToArtifact(metamodel);
//			model.getRelations().add(rel);
//			relationRepository.save(rel);
			
			model.setId((new ObjectId()).toString());
			
			ConformToRelation rel = (ConformToRelation) model.getRelations().get(0);
			rel.setFromArtifact(model);
			relationRepository.save(rel);
			
			// Update bi-directional reference
			modelRepository.save(model);
			
			for (Workspace ws : model.getWorkspaces()) {
				Workspace w = workspaceService.findOne(ws.getId());
				if (w == null)
					throw new BusinessException();
				w.getArtifacts().add(model);
				workspaceRepository.save(w);
			}
			for (Project ps : model.getProjects()) {
				Project p = projectService.findById(ps.getId(), model.getAuthor());
				p.getArtifacts().add(model);
				projectRepository.save(p);
			}
			for (User us : model.getShared()) {
				User u = userService.findOne(us.getId());
				if (u == null)
					throw new BusinessException();
				u.getSharedArtifact().add(model);
				userRepository.save(u);
			}
			
			String jsonMongoUriBase = mongoPrefix+mongo.getAddress().toString()+"/"+mongoDbFactory.getDb().getName()+"/"+jsonArtifactCollection+"/";
			
			String mmID = ((ConformToRelation) model.getRelations().get(0)).getToArtifact().getId();
			
			model.setExtractedContents(EmfjsonMongo.getInstance().saveModel(jsonMongoUriBase+mmID, model.getNsuri(), jsonMongoUriBase+model.getId()));
			
			return model.getId();
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessException();
		}
	}
}
