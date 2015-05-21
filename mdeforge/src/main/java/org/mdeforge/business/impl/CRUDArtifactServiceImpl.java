package org.mdeforge.business.impl;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mdeforge.business.BusinessException;
import org.mdeforge.business.CRUDArtifactService;
import org.mdeforge.business.GridFileMediaService;
import org.mdeforge.business.ProjectService;
import org.mdeforge.business.UserService;
import org.mdeforge.business.WorkspaceService;
import org.mdeforge.business.model.Artifact;
import org.mdeforge.business.model.GridFileMedia;
import org.mdeforge.business.model.Metric;
import org.mdeforge.business.model.Project;
import org.mdeforge.business.model.Relation;
import org.mdeforge.business.model.User;
import org.mdeforge.business.model.Workspace;
import org.mdeforge.integration.ArtifactRepository;
import org.mdeforge.integration.ProjectRepository;
import org.mdeforge.integration.RelationRepository;
import org.mdeforge.integration.UserRepository;
import org.mdeforge.integration.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.codec.Base64;

public abstract class CRUDArtifactServiceImpl<T extends Artifact> implements
		CRUDArtifactService<T> {

	@Autowired
	protected SimpleMongoDbFactory mongoDbFactory;
	@Autowired
	protected RelationRepository relationRepository;
	@Autowired
	protected ArtifactRepository artifactRepository;
	@Autowired
	protected ProjectService projectService;
	@Autowired
	protected ProjectRepository projectRepository;
	@Autowired
	protected WorkspaceService workspaceService;
	@Autowired
	protected WorkspaceRepository workspaceRepository;
	@Autowired
	protected UserService userService;
	@Autowired
	protected UserRepository userRepository;
	@Autowired
	protected GridFileMediaService gridFileMediaService;
	@Value("#{cfgproperties[basePath]}")
	protected String basePath;

	protected Class<T> persistentClass;

	@SuppressWarnings("unchecked")
	public CRUDArtifactServiceImpl() {
		this.persistentClass = (Class<T>) ((ParameterizedType) getClass()
				.getGenericSuperclass()).getActualTypeArguments()[0];
	}

	@Override
	public T findOneById(String idArtifact, User user) throws BusinessException {
		MongoOperations operations = new MongoTemplate(mongoDbFactory);
		Query query = new Query();
		Criteria c1 = Criteria.where("users").in(user.getId());
		Criteria c3 = Criteria.where("_id").is(idArtifact);
		Criteria c4 = Criteria.where("open").is(true);
		if (persistentClass != Artifact.class) {
			Criteria c2 = Criteria.where("_class").is(
					persistentClass.getCanonicalName());
			query.addCriteria(c3.andOperator(c2.orOperator(c1, c4)));
		} else
			query.addCriteria(c3.orOperator(c1, c4));

		T artifact = operations.findOne(query, persistentClass);
		if (artifact == null)
			throw new BusinessException();
		return artifact;
	}

	@Override
	public List<T> findAll() {
		MongoOperations n = new MongoTemplate(mongoDbFactory);

		Query query = new Query();
		if (persistentClass != Artifact.class) {
			Criteria c = Criteria.where("_class").is(
					persistentClass.getCanonicalName());
			query.addCriteria(c);
			return n.find(query, persistentClass);
		} else
			return n.findAll(persistentClass);

	}

	@Override
	public void delete(String idArtifact, User user) {
		Artifact artifact = findOneByOwner(idArtifact, user);
		for (Project project : artifact.getProjects())
			for (Artifact art : project.getArtifacts())
				if (art.getId().equals(idArtifact)) {
					project.getArtifacts().remove(art);
					projectRepository.save(project);
				}
		for (Workspace workspace : artifact.getWorkspaces())
			for (Artifact art : workspace.getArtifacts())
				if (art.getId().equals(idArtifact)) {
					workspace.getArtifacts().remove(art);
					workspaceRepository.save(workspace);
				}

		for (User us : artifact.getShared())
			for (Artifact art : us.getSharedArtifact())
				if (art.getId().equals(idArtifact)) {
					us.getSharedArtifact().remove(art);
					userRepository.save(user);
				}
		// TODO delete Relation
		gridFileMediaService.delete(artifact.getFile().getId());
		artifactRepository.delete(artifact);

	}

	@Override
	public void delete(T artifact, User user) {
		artifact = findOneById(artifact.getId(), user);
		for (Project project : artifact.getProjects())
			for (Artifact art : project.getArtifacts())
				if (art.getId().equals(artifact.getId())) {
					project.getArtifacts().remove(art);
					projectRepository.save(project);
				}
		for (Workspace workspace : artifact.getWorkspaces())
			for (Artifact art : workspace.getArtifacts())
				if (art.getId().equals(artifact.getId())) {
					workspace.getArtifacts().remove(art);
					workspaceRepository.save(workspace);
				}

		for (User us : artifact.getShared())
			for (Artifact art : us.getSharedArtifact())
				if (art.getId().equals(artifact.getId())) {
					us.getSharedArtifact().remove(art);
					userRepository.save(user);
				}
		// TODO delete Relation
		gridFileMediaService.delete(artifact.getFile().getId());
		artifactRepository.delete(artifact);

	}

	@Override
	public void update(T artifact) {
		try {
			if (artifact.getId() == null)
				throw new BusinessException();
			// verify metamodel owner
			findOneByOwner(artifact.getId(), artifact.getAuthor());

			// UploadFile
			GridFileMedia fileMedia = new GridFileMedia();
			fileMedia.setFileName("");
			fileMedia.setByteArray(Base64.decode(artifact.getFile()
					.getContent().getBytes()));
			artifact.setFile(fileMedia);

			for (Workspace ws : artifact.getWorkspaces()) {
				workspaceService.findById(ws.getId(), artifact.getAuthor());
			}
			// check project Auth
			for (Project p : artifact.getProjects()) {
				projectService.findById(p.getId(), artifact.getAuthor());
			}
			Artifact transTemp = artifactRepository.findOne(artifact.getId());
			gridFileMediaService.delete(transTemp.getFile().getId());
			if (artifact.getFile() != null) {
				gridFileMediaService.store(artifact.getFile());
			}

			List<Relation> relationTemp = artifact.getRelations();
			artifact.setRelations(new ArrayList<Relation>());
			artifactRepository.save(artifact);
			// check relation
			for (Relation rel : relationTemp) {
				Artifact toArtifact = findOneById(rel.getToArtifact().getId(),
						artifact.getAuthor());
				if (existRelation(toArtifact.getId(), artifact.getId())) {
					rel.setFromArtifact(artifact);
					artifact.getRelations().add(rel);
					relationRepository.save(rel);
					artifactRepository.save(artifact);
					Artifact temp = artifactRepository.findOne(rel
							.getToArtifact().getId());
					if (temp.getRelations() == null)
						temp.setRelations(new ArrayList<Relation>());
					temp.getRelations().add(rel);
					artifactRepository.save(temp);
				}
			}

			for (Workspace ws : artifact.getWorkspaces()) {
				Workspace w = workspaceService.findOne(ws.getId());
				if (!isArtifactInWorkspace(w.getId(), artifact.getId())) {
					w.getArtifacts().add(artifact);
					workspaceRepository.save(w);
				}
			}
			for (Project ps : artifact.getProjects()) {
				Project p = projectService.findById(ps.getId(),
						artifact.getAuthor());
				if (!isArtifactInProject(p.getId(), artifact.getId())) {
					p.getArtifacts().add(artifact);
					projectRepository.save(p);
				}
			}
			for (User us : artifact.getShared()) {
				User u = userService.findOne(us.getId());
				if (u == null)
					throw new BusinessException();
				if (!isArtifactInUser(u, artifact.getId())) {
					u.getSharedArtifact().add(artifact);
					userRepository.save(u);
				}
			}
		} catch (Exception e) {
			throw new BusinessException();
		}
	}
	@Override
	public void updateSimple(T artifact) {
		try {
			artifactRepository.save(artifact);
		} catch (Exception e) {
			throw new BusinessException();
		}
	}
	@Override
	public T create(T artifact) throws BusinessException {
		// check workspace Auth
		try {
			// GetUser
			if (artifact.getId() != null)
				throw new BusinessException();
			// File handler
			GridFileMedia fileMedia = new GridFileMedia();
			fileMedia.setFileName(artifact.getFile().getFileName());
			fileMedia.setByteArray(Base64.decode(artifact.getFile()
					.getContent().getBytes()));
			artifact.setFile(fileMedia);
			// check workspace Auth
			for (Workspace ws : artifact.getWorkspaces())
				workspaceService.findById(ws.getId(), artifact.getAuthor());
			// check project Auth
			for (Project p : artifact.getProjects())
				projectService.findById(p.getId(), artifact.getAuthor());
			if (artifact.getFile() != null)
				gridFileMediaService.store(artifact.getFile());
			artifact.setCreated(new Date());
			artifact.setModified(new Date());
			User user = userRepository.findOne(artifact.getAuthor().getId());
			artifact.setAuthor(user);
			artifact.getShared().add(user);
			List<Relation> relationTemp = artifact.getRelations();
			artifact.setRelations(new ArrayList<Relation>());
			artifactRepository.save(artifact);
			// check relation
			for (Relation rel : relationTemp) {
				Artifact toArtifact = findOneById(rel.getToArtifact().getId(),
						artifact.getAuthor());
				if (!existRelation(toArtifact.getId(), artifact.getId())) {
					rel.setFromArtifact(artifact);
					artifact.getRelations().add(rel);
					relationRepository.save(rel);
					artifactRepository.save(artifact);
					Artifact temp = artifactRepository.findOne(rel
							.getToArtifact().getId());
					if (temp.getRelations() == null)
						temp.setRelations(new ArrayList<Relation>());
					temp.getRelations().add(rel);
					artifactRepository.save(temp);
				}
			}
			// Update bi-directional reference
			artifact.setRelations(relationTemp);
			artifactRepository.save(artifact);
			for (Workspace ws : artifact.getWorkspaces()) {
				Workspace w = workspaceService.findOne(ws.getId());
				if (w == null)
					throw new BusinessException();
				w.getArtifacts().add(artifact);
				workspaceRepository.save(w);
			}
			for (Project ps : artifact.getProjects()) {
				Project p = projectService.findById(ps.getId(),
						artifact.getAuthor());
				p.getArtifacts().add(artifact);
				projectRepository.save(p);
			}
			for (User us : artifact.getShared()) {
				User u = userService.findOne(us.getId());
				if (u == null)
					throw new BusinessException();
				u.getSharedArtifact().add(artifact);
				userRepository.save(u);
			}
			return artifact;
		} catch (Exception e) {
			throw new BusinessException();
		}
	}

	@Override
	public List<T> findAllWithPublicByUser(User user) throws BusinessException {
		MongoOperations n = new MongoTemplate(mongoDbFactory);
		Query query = new Query();
		Criteria c1 = Criteria.where("shared.id").is(user.getId());
		Criteria c2 = Criteria.where("open").is(true);
		if (persistentClass != Artifact.class) {
			Criteria c3 = Criteria.where("_class").is(
					persistentClass.getCanonicalName());
			query.addCriteria(c1.andOperator(c2, c3));
		} else
			query.addCriteria(c1.andOperator(c2));
		return n.find(query, persistentClass);
	}

	@Override
	public List<T> findAllPublic() {
		MongoOperations n = new MongoTemplate(mongoDbFactory);
		Query query = new Query();
		Criteria c2 = Criteria.where("open").is(true);
		if (persistentClass != Artifact.class) {
			Criteria c1 = Criteria.where("_class").is(
					persistentClass.getCanonicalName());
			query.addCriteria(c1.andOperator(c2));
		} else
			query.addCriteria(c2);
		return n.find(query, persistentClass);
	}

	@Override
	public T findOneByOwner(String idArtifact, User user)
			throws BusinessException {
		// TODO change method
		MongoOperations n = new MongoTemplate(mongoDbFactory);
		Query query = new Query();
		Criteria c2 = Criteria.where("author.$id").is(user.getId());
		if (persistentClass != Artifact.class) {
			Criteria c1 = Criteria.where("_class").is(
					persistentClass.getCanonicalName());
			query.addCriteria(c2.andOperator(c1));
		}
		query.addCriteria(c2);
		return n.findOne(query, persistentClass);
	}

	@Override
	public T findOne(String id) throws BusinessException {
		MongoOperations n = new MongoTemplate(mongoDbFactory);
		Query query = new Query();
		Criteria c2 = Criteria.where("id").is(id);
		if (persistentClass != Artifact.class) {
			Criteria c1 = Criteria.where("_class").is(
					persistentClass.getCanonicalName());
			query.addCriteria(c2.andOperator(c1));
		} else
			query.addCriteria(c2);
		return n.findOne(query, persistentClass);
	}
	@Override
	public T findOnePublic(String id) throws BusinessException {
		MongoOperations n = new MongoTemplate(mongoDbFactory);
		Query query = new Query();
		Criteria c2 = Criteria.where("id").is(id);
		Criteria c3 = Criteria.where("open").is(true);
		if (persistentClass != Artifact.class) {
			Criteria c1 = Criteria.where("_class").is(
					persistentClass.getCanonicalName());
			query.addCriteria(new Criteria().andOperator(c1, c2, c3));
		} else
			query.addCriteria(new Criteria().andOperator(c2, c3));
		return n.findOne(query, persistentClass);
	}

	@Override
	public boolean isArtifactInWorkspace(String idWorkspace, String idArtfact)
			throws BusinessException {
		Artifact artifact = findOne(idArtfact);
		for (Workspace workspace : artifact.getWorkspaces()) {
			if (workspace.getId().equals(idWorkspace))
				return true;
		}
		return false;
	}

	@Override
	public boolean isArtifactInProject(String idProject, String idArtfact)
			throws BusinessException {
		Artifact artifact = findOne(idArtfact);
		for (Project workspace : artifact.getProjects()) {
			if (workspace.getId().equals(idProject))
				return true;
		}
		return false;
	}

	@Override
	public boolean isArtifactInUser(User idUser, String idArtfact)
			throws BusinessException {
		Artifact artifact = findOne(idArtfact);
		for (User user : artifact.getShared()) {
			if (user.getId().equals(idUser.getId()))
				return true;
		}
		return false;
	}

	@Override
	public boolean existRelation(String idTo, String idFrom)
			throws BusinessException {
		List<Relation> r = relationRepository
				.findByFromArtifactIdOrToArtifactId(idFrom, idTo);
		return (r.size() == 0) ? false : true;
	}

	@Override
	public List<T> findArtifactInProject(String idProject, User user) {
		projectService.findById(idProject, user);
		MongoOperations operations = new MongoTemplate(mongoDbFactory);
		Query query = new Query();
		Criteria c1 = Criteria.where("projects.$id").is(idProject);
		if (persistentClass != Artifact.class) {
			Criteria c2 = Criteria.where("_class").is(
					persistentClass.getCanonicalName());
			query.addCriteria(new Criteria().andOperator(c1, c2));
		} else
			query.addCriteria(c1);
		return operations.find(query, persistentClass);
	}

	@Override
	public List<T> findArtifactInWorkspace(String idWorkspace, User user) {
		workspaceService.findById(idWorkspace, user);
		MongoOperations operations = new MongoTemplate(mongoDbFactory);
		Query query = new Query();
		Criteria c1 = Criteria.where("workspaces.$id").is(idWorkspace);
		if (persistentClass != Artifact.class) {
			Criteria c2 = Criteria.where("_class").is(
					persistentClass.getCanonicalName());
			query.addCriteria(new Criteria().andOperator(c1, c2));
		} else
			query.addCriteria(c1);
		return operations.find(query, persistentClass);
	}

	protected T findOneByName(String name, User user, Class<T> c)
			throws BusinessException {
		MongoOperations operations = new MongoTemplate(mongoDbFactory);
		Query query = new Query();
		Criteria c1 = Criteria.where("users.$id").is(user.getId());
		Criteria c3 = Criteria.where("name").is(name);

		if (c != Artifact.class) {
			Criteria c2 = Criteria.where("_class").is(c.getCanonicalName());
			query.addCriteria(c1.andOperator(c2).andOperator(c3));
		}
		query.addCriteria(c1.andOperator(c3));
		T project = operations.findOne(query, c);
		if (project == null)
			throw new BusinessException();
		return project;
	}

	@Override
	public T findOneByName(String name) throws BusinessException {
		MongoOperations operations = new MongoTemplate(mongoDbFactory);
		Query query = new Query();

		Criteria c3 = Criteria.where("name").is(name);

		if (persistentClass != Artifact.class) {
			Criteria c2 = Criteria.where("_class").is(
					persistentClass.getCanonicalName());
			query.addCriteria(new Criteria().andOperator(c2, c3));
		}
		query.addCriteria(c3);
		T project = operations.findOne(query, persistentClass);
		if (project == null)
			throw new BusinessException();
		return project;
	}

	@Override
	public List<Metric> findMetricForArtifact(Artifact a) {
		MongoOperations operations = new MongoTemplate(mongoDbFactory);
		Query query = new Query();
		Criteria c1 = Criteria.where("artifact.$id").is(a.getId());
		query.addCriteria(c1);
		return operations.find(query, Metric.class);

	}
}