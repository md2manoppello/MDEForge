package org.mdeforge.business.impl;

import org.mdeforge.business.TransformationService;
import org.mdeforge.business.model.Transformation;
import org.springframework.stereotype.Service;

@Service(value="Transformation")
public class TransformationServiceImpl extends ArtifactServiceImpl<Transformation> implements TransformationService {

}
