package org.mdeforge.business;

import org.mdeforge.business.model.Artifact;
import org.mdeforge.business.model.GridFileMedia;

public interface GridFileMediaService {
	void store(GridFileMedia gridFileMedia) throws BusinessException;
	
	public String getFilePath(Artifact artifact) throws BusinessException;
	
	GridFileMedia getGridFileMedia(GridFileMedia id) throws BusinessException;

	void delete(String idGridFileMedia) throws BusinessException;

	byte[] getFileByte(Artifact artifact) throws BusinessException;
	
}
