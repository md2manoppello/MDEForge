package org.mdeforge.business.impl;

import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.mdeforge.business.BusinessException;
import org.mdeforge.business.ModelService;
import org.mdeforge.business.RequestGrid;
import org.mdeforge.business.ResponseGrid;
import org.mdeforge.business.model.Metamodel;
import org.mdeforge.business.model.Model;
import org.mdeforge.integration.ModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;


@Service
public class ModelServiceImpl implements ModelService {

	@Autowired
	private ModelRepository modelRepository;

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

	



}
