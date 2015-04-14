package org.mdeforge.business;

import java.util.List;

import org.mdeforge.business.model.ATLTransformation;

public interface ATLTransformationService extends ArtifactService<ATLTransformation>, MetricProvider{	
	void execute(ATLTransformation transformation);
	List<ATLTransformation> findTransformationsBySourceMetamodels(ATLTransformation metamodel);
	List<ATLTransformation> findTransformationsByTargetMetamodels(ATLTransformation metamodel);
	List<ATLTransformation> findAllTransformations() throws BusinessException;
	ResponseGrid<ATLTransformation> findAllPaginated(RequestGrid requestGrid)  throws BusinessException;
}
