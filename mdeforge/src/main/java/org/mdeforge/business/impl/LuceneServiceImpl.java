package org.mdeforge.business.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.ecl.parse.Ecl_EolParserRules.elseStatement_return;
import org.eclipse.m2m.atl.core.IReferenceModel;
import org.eclipse.m2m.atl.core.ModelFactory;
import org.eclipse.m2m.atl.core.emf.EMFModel;
import org.eclipse.m2m.atl.core.emf.EMFModelFactory;
import org.eclipse.m2m.atl.engine.parser.AtlParser;
import org.mdeforge.business.ATLTransformationService;
import org.mdeforge.business.BusinessException;
import org.mdeforge.business.CRUDArtifactService;
import org.mdeforge.business.EcoreMetamodelService;
import org.mdeforge.business.GridFileMediaService;
import org.mdeforge.business.LuceneService;
import org.mdeforge.business.ModelService;
import org.mdeforge.business.model.ATLTransformation;
import org.mdeforge.business.model.Artifact;
import org.mdeforge.business.model.CoDomainConformToRelation;
import org.mdeforge.business.model.DomainConformToRelation;
import org.mdeforge.business.model.EcoreMetamodel;
import org.mdeforge.business.model.LuceneTag;
import org.mdeforge.business.model.Model;
import org.mdeforge.business.model.Property;
import org.mdeforge.business.model.form.SearchResult;
import org.mdeforge.business.model.form.SearchResultComplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import anatlyzer.atl.model.ATLModel;
import anatlyzer.atlext.ATL.Helper;
import anatlyzer.atlext.ATL.InPatternElement;
import anatlyzer.atlext.ATL.MatchedRule;
import anatlyzer.atlext.ATL.ModuleElement;
import anatlyzer.atlext.ATL.OutPatternElement;
import anatlyzer.atlext.ATL.Rule;
import anatlyzer.atlext.ATL.SimpleInPatternElement;
import anatlyzer.atlext.ATL.SimpleOutPatternElement;
import anatlyzer.atlext.OCL.Attribute;
import anatlyzer.atlext.OCL.OclFeatureDefinition;
import anatlyzer.atlext.OCL.Operation;

@Service
public class LuceneServiceImpl<T extends Artifact> implements LuceneService{
	
	/*
	 * GENERAL TAGS
	 */
	private static final String TEXT_TAG = "text";
	private static final String TYPE_TAG = "forgeType";
	private static final String NAME_TAG = "name";
	private static final String AUTHOR_TAG = "author";
	private static final String ID_TAG = "id";
	private static final String LAST_UPDATE_TAG = "lastUpdate";
	private static final int TIKA_CHARACTERS_LIMIT = 5000000; // characters
	/*
	 * ECORE METAMODEL TAGS
	 */
	
	private static final String EPACKAGE_INDEX_CODE = "ePackage";
	private static final float EPACKAGE_BOOST_VALUE = 2.0f;

	private static final String NsURI_INDEX_CODE = "nsuri";
	private static final float NsURI_BOOST_VALUE = 1.7f;

	private static final String EANNOTATION_INDEX_CODE = "eAnnotation";
	private static final float EANNOTATION_BOOST_VALUE = 1.7f;

	private static final String ECLASS_INDEX_CODE = "eClass";
	private static final float ECLASS_BOOST_VALUE = 1.5f;

	private static final String EATTRIBUTE_INDEX_CODE = "eAttribute";
	private static final float EATTRIBUTE_BOOST_VALUE = 1.4f;

	private static final String EREFERENCE_INDEX_CODE = "eReference";
	private static final float EREFERENCE_BOOST_VALUE = 1.4f;
	
	private static final String EENUM_INDEX_CODE = "eEnum";
	private static final float EENUM_BOOST_VALUE = 1.0f;

	private static final String ELITERAL_INDEX_CODE = "eLiteral";
	private static final float ELITERAL_BOOST_VALUE = 0.7f;

	private static final String EDATATYPE_INDEX_CODE = "eDataType";
	private static final float EDATATYPE_BOOST_VALUE = 0.5f;
	
	private static String[] metamodelLuceneTags = {EPACKAGE_INDEX_CODE,NsURI_INDEX_CODE,EANNOTATION_INDEX_CODE,
			ECLASS_INDEX_CODE,EATTRIBUTE_INDEX_CODE,
			EREFERENCE_INDEX_CODE,EENUM_INDEX_CODE,
			ELITERAL_INDEX_CODE,EDATATYPE_INDEX_CODE};
	
	/*
	 * ATL Transformation TAGS
	 */
	private static final String HELPER_TAG = "helper";
	private static final float HELPER_BOOST_VALUE = 1.0f;
	private static final String FROM_METAMODEL_TAG = "fromMM";
	private static final String TO_METAMODEL_TAG = "toMM";
	private static final String RULE_NAME_TAG = "rule";
	private static final String FROM_METACLASS = "fromMC";
	private static final String FROM_TOCLASS = "toMC";
	
	private static String[] modelLuceneTags = {HELPER_TAG,FROM_METAMODEL_TAG,TO_METAMODEL_TAG,
			RULE_NAME_TAG,FROM_METACLASS,FROM_TOCLASS};
	/*
	 * MODEL TAGS
	 */
	private static final String CUSTOM_LUCENE_INDEX_SEPARATOR_CHARACTER = "_";
	private static final String CONFORM_TO_TAG = "conformToMM";
	private static String[] transformationLuceneTags = {CONFORM_TO_TAG};
	
	/*
	 * FRAGMENT TAGS
	 */
	private static final int MAX_NUMBER_OF_FRAGMENTS_TO_RETRIEVE = 10;
	private static final String TAG_HIGHLIGHT_OPEN = "<strong>";
	private static final String TAG_HIGHLIGHT_CLOSE = "</strong>";
	
	private IndexWriter writer;
	Logger logger = LoggerFactory.getLogger(CRUDArtifactServiceImpl.class);
	
	@Autowired
	protected GridFileMediaService gridFileMediaService;
	@Autowired
	private EcoreMetamodelService ecoreMetamodelService;
	@Autowired
	private ATLTransformationService aTLTransformationService;
	@Autowired
	private ModelService modelService;
	@Autowired
	private CRUDArtifactService<Artifact> artifactService;
	
	@Value("#{cfgproperties[basePathLucene]}")
	private String basePathLucene;
	
	
	
	@Override
	public void createLuceneIndex(Artifact artifact) {
		if(artifact instanceof EcoreMetamodel){
			 createEcoreMetamodelIndex((EcoreMetamodel) artifact);
		}else if (artifact instanceof Model) {
			createModelIndex((Model) artifact);
		}else if(artifact instanceof ATLTransformation){
			createATLTransformationIndex((ATLTransformation) artifact);
		}
		
	}
	
	
	@Override
	public List<String> getIndexTagsByArtifactType(Artifact artifact) {
		String[] result = null;
		if(artifact instanceof EcoreMetamodel){
			result = metamodelLuceneTags;
		}else if (artifact instanceof Model) {
			result = modelLuceneTags;
		}else if(artifact instanceof ATLTransformation){
			result = transformationLuceneTags;
		}
		
		return Arrays.asList(result);
	}
	
	
	@Override
	public List<String> getAllIndexTags() {
		System.out.println("---------------------------------");
		System.out.println("List of Name Fields in the index:");
		System.out.println("---------------------------------");
		
		HashSet<String> result = new HashSet<String>();
		
		File indexDirFile = new File(basePathLucene);
		try {
			FSDirectory indexDir = FSDirectory.open(indexDirFile);
			IndexReader luceneIndexReader = IndexReader.open(indexDir);
			result = (HashSet<String>) luceneIndexReader.getFieldNames(IndexReader.FieldOption.ALL);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List<String> sortedList = new ArrayList<String>(result);
		Collections.sort(sortedList);
		
		sortedList.forEach(x -> System.out.println(x));
		return sortedList;
	}
	
	
	
	
	
	/*
	 * *********************************
	 * ECORE METAMODEL INDEX
	 * *********************************
	 */
	
	private void createEcoreMetamodelIndex(EcoreMetamodel is) {
		File indexDirFile = new File(basePathLucene);
		// Set the directory in which will be created the index.
		Directory indexDir;
		try {
			indexDir = FSDirectory.open(indexDirFile);
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
			IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35, analyzer);
			// Create an index in the directory, appending new index over previously indexed documents:
			conf.setOpenMode(OpenMode.CREATE_OR_APPEND); //or CREATE
			// create the indexer
			Document document = parseMetamodelForIndex(is);
			this.writer = new IndexWriter(indexDir, conf);

			try {
				// writer.updateDocument(new Term("path", file.getPath()), document);
				writer.addDocument(document);
			} catch (CorruptIndexException e) {
				throw new BusinessException(e.getMessage());
			} catch (IOException e) {
				throw new BusinessException(e.getMessage());
			}
			writer.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			throw new BusinessException(e1.getMessage());
		}

	}
	
	private Document parseMetamodelForIndex(EcoreMetamodel ecoreMetamodel) {
		Document doc = new Document();
		
//		String textOfTheInputStream = getTextFromInputStream(gridFileMediaService.getFileInputStream(ecoreMetamodel));
		
//		InputStream is = null;
//		try {
//			is = new FileInputStream(gridFileMediaService.getFilePath(ecoreMetamodel));
//		} catch (FileNotFoundException | BusinessException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		
//		/*
//		 * FILE METADATA
//		 */
//		Metadata metadata = new Metadata();
//		// By using the BodyContentHandler, you can request that Tika return
//		// only the content of the document's body as a plain-text string.
//		ContentHandler handler = new BodyContentHandler(TIKA_CHARACTERS_LIMIT); // Parsing to
//																	// Plain
//																	// Text
//		ParseContext context = new ParseContext();
//		Parser parser = new AutoDetectParser();
//		try {
//			parser.parse(is, handler, metadata, context);
//		} catch (TikaException e) {
//			throw new BusinessException(e.getMessage());
//		} catch (SAXException e) {
//			throw new BusinessException(e.getMessage());
//		} catch (IOException e) {
//			throw new BusinessException(e.getMessage());
//		} finally {
//			try {
//				is.close();
//			} catch (IOException e) {
//				throw new BusinessException(e.getMessage());
//			}
//		}
		
		URI fileURI = URI.createFileURI(gridFileMediaService.getFilePath(ecoreMetamodel));
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new EcoreResourceFactoryImpl());
		ResourceSet resourceSet = new ResourceSetImpl();
		Resource resource = resourceSet.getResource(fileURI, true);
		if (resource.isLoaded() && resource.getErrors() != null) {
			TreeIterator<EObject> eAllContents = resource.getAllContents();
			while (eAllContents.hasNext()) {
				EObject next = eAllContents.next();
				if (next instanceof EPackage) {
					EPackage ePackage = (EPackage) next;
					doc = ePackageIndex(ePackage, doc);
				} else if (next instanceof EClass) {
					EClass eClass = (EClass) next;
					doc = eClassIndex(eClass, doc);
				} else if (next instanceof EEnum) {
					EEnum eEnum = (EEnum) next;
					doc = eEnumIndex(eEnum, doc);
				} else if (next instanceof EDataType) {
					EDataType eDataType = (EDataType) next;
					doc = eDataTypeIndex(eDataType, doc);
				} else if (next instanceof EAnnotation) {
					// GET all the EAnnotations
					EList<EAnnotation> annotations = ((EModelElement) next).getEAnnotations();
					doc = indexAnnotations(annotations, doc);
				}

			}
		}
		
		//ID
		Field idField = new Field(ID_TAG, ecoreMetamodel.getId(), Store.YES, Index.ANALYZED);
	 	doc.add(idField);
		
		//Artifact TYPE: "EcoreMetamodel"
		Field artifactType = new Field(TYPE_TAG, ecoreMetamodel.getClass().getSimpleName(), Store.YES, Index.ANALYZED);
		doc.add(artifactType);
		
//		String text = handler.toString();
		String text = getTextFromInputStream(gridFileMediaService.getFileInputStream(ecoreMetamodel));
		Field textField = new Field(TEXT_TAG, text, Store.YES, Index.ANALYZED);
		doc.add(textField);

		
		Field artName = new Field(NAME_TAG, ecoreMetamodel.getName(), Store.YES, Index.ANALYZED);
	 	doc.add(artName);
	 	
	 	Field authorField = new Field(AUTHOR_TAG, ecoreMetamodel.getAuthor().getUsername(), Store.YES, Index.ANALYZED);
	 	doc.add(authorField);
	 	
	 	Field lastUpdateField = new Field(LAST_UPDATE_TAG, ecoreMetamodel.getModified().toString(), Store.YES, Index.ANALYZED);
	 	doc.add(lastUpdateField);
	 	
	 	for (Property prop : ecoreMetamodel.getProperties()) {
			String propName = prop.getName();
			String propValue = prop.getValue();
			if(propName != null && propValue != null) {
				Field propField = new Field(propName, propValue, Store.YES, Index.ANALYZED);
				doc.add(propField);
			}
		}
	 	
//		System.out.println(handler.toString());	 	

	 	return doc;
	}
	
	private String getTextFromInputStream(InputStream is){      
        String str = "";
        StringBuffer buf = new StringBuffer();            
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            if (is != null) {                            
                while ((str = reader.readLine()) != null) {    
                    buf.append(str + "\n" );
                }                
            }
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            try { is.close(); } catch (Throwable ignore) {}
        }
        return buf.toString();
    }
	
	private Document ePackageIndex(EPackage ePackage, Document doc){
		Field ePackageField = new Field(EPACKAGE_INDEX_CODE, ePackage.getName(), Store.YES, Index.ANALYZED);
		ePackageField.setBoost(EPACKAGE_BOOST_VALUE);
		// System.out.println("Package: " + ePackage.getName());
		doc.add(ePackageField);
		// GET NsURI
		if (ePackage.getNsURI() != null && !ePackage.getNsURI().isEmpty()) {
			Field EPackageNsURIField = new Field(NsURI_INDEX_CODE, ePackage.getNsURI(), Store.YES, Index.ANALYZED);
			ePackageField.setBoost(NsURI_BOOST_VALUE);
//			System.out.println("NsURI : " + ePackage.getNsURI());
			doc.add(EPackageNsURIField);
		}
		// GET EAnnotation
		EList<EAnnotation> annotations = ePackage.getEAnnotations();
		if (annotations != null && !annotations.isEmpty()) {
			doc = indexAnnotations(annotations, doc);
		}
		
		return doc;
	}
	

	
	private Document eClassIndex(EClass eClass, Document doc){
		try {
			Field eClassField = new Field(ECLASS_INDEX_CODE, eClass.getName(), Store.YES, Index.ANALYZED);
			eClassField.setBoost(ECLASS_BOOST_VALUE);
			// System.out.println("Class: " + eClass.getName());
			doc.add(eClassField);

			// GET EAnnotation
			EList<EAnnotation> annotations = eClass.getEAnnotations();
			if (annotations != null && !annotations.isEmpty()) {
				doc = indexAnnotations(annotations, doc);
			}

			// Index EClass Attributes
			for (EAttribute attribute : eClass.getEAttributes()) {
				Field eClassAttributeField = new Field(EATTRIBUTE_INDEX_CODE, attribute.getName(), Store.YES, Index.ANALYZED);
				eClassAttributeField.setBoost(EATTRIBUTE_BOOST_VALUE);
				// System.out.println("Attribute: " + attribute.getName());
				doc.add(eClassAttributeField);
			}
			// Index EClass References
			for (EReference reference : eClass.getEReferences()) {
				Field eClassReferenceField = new Field(EREFERENCE_INDEX_CODE, reference.getName(), Store.YES, Index.ANALYZED);
				eClassReferenceField.setBoost(EREFERENCE_BOOST_VALUE);
				// System.out.println("Reference: " + reference.getName());
				doc.add(eClassReferenceField);
			}
		
		} catch (Exception e) {
			System.err.println("ERROR");
		}
		
		return doc;
	}
	
	
	private Document eEnumIndex(EEnum eEnum, Document doc){
		Field eEnumField = new Field(EENUM_INDEX_CODE, eEnum.getName(), Store.YES, Index.ANALYZED);
		eEnumField.setBoost(EENUM_BOOST_VALUE);
		doc.add(eEnumField);
		return doc;
	}
	
	private Document eDataTypeIndex(EDataType eDataType, Document doc){
		Field eDataTypeField = new Field(EDATATYPE_INDEX_CODE, eDataType.getName(), Store.YES, Index.ANALYZED);
		eDataTypeField.setBoost(EDATATYPE_BOOST_VALUE);
		doc.add(eDataTypeField);
		return doc;
	}
	
	
	/**
	 * Index the annotation list provided as input.
	 * @param annotations
	 * @param doc
	 * @return Document
	 */
	private Document indexAnnotations(List<EAnnotation> annotations, Document doc){
		if (annotations != null && !annotations.isEmpty()) {
			for (EAnnotation eAnnotation : annotations) {
				if(getAnnotationKey(eAnnotation) != null && getAnnotationKey(eAnnotation).equals("weight")){
					if(getAnnotationValue(eAnnotation) != null){
						Field EPackageEAnnotationField = new Field(EANNOTATION_INDEX_CODE, getAnnotationValue(eAnnotation), Store.YES, Index.ANALYZED);
						doc.add(EPackageEAnnotationField);
					}
				}
			}
		}
		return doc;
	}
	
	
	/**
	 * Get the annotation key from an EAnnotation
	 * @param eAnnotation
	 * @return String
	 */
	private String getAnnotationKey(EAnnotation eAnnotation){
		String result = null;
		if (eAnnotation != null) {
				EMap<String, String> annotationDetails = eAnnotation.getDetails();
				for (Entry<String, String> entry : annotationDetails) {
					if(entry.getKey() != null && entry.getValue() != null){
						result = entry.getKey();
				}
			}
		}
		return result;
	}
	
	/**
	 * Get the annotation value from an EAnnotation
	 * @param eAnnotation
	 * @return String
	 */
	private String getAnnotationValue(EAnnotation eAnnotation){
		String result = null;
		if (eAnnotation != null) {
			EMap<String, String> annotationDetails = eAnnotation.getDetails();
			for (Entry<String, String> entry : annotationDetails) {
				if(entry.getKey() != null && entry.getValue() != null){
					result = entry.getValue();
				}
			}
		}
		return result;
	}


	
	
	
	/*
	 * *********************************
	 * ATL TRANSFORMATION INDEX
	 * *********************************
	 */

	private void createATLTransformationIndex(ATLTransformation art) {
		Document doc = new Document();
		AtlParser atlParser = new AtlParser();
		ModelFactory modelFactory = new EMFModelFactory();
		IReferenceModel atlMetamodel;
		try {
			File indexDirFile = new File(basePathLucene);
			// Set the directory in which will be created the index.
			Directory indexDir;
			indexDir = FSDirectory.open(indexDirFile);
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
			IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35, analyzer);
			conf.setOpenMode(OpenMode.CREATE_OR_APPEND); //or CREATE
			IndexWriter writer = new IndexWriter(indexDir, conf);
			try {
			atlMetamodel = modelFactory.getBuiltInResource("ATL.ecore");
			String filePath = gridFileMediaService.getFilePath(art);
			EMFModel atlDynModel = (EMFModel) modelFactory
					.newModel(atlMetamodel);
			atlParser.inject(atlDynModel, filePath);
			Resource originalTrafo = atlDynModel.getResource();
			ATLModel atlModel = new ATLModel(originalTrafo, originalTrafo
					.getURI().toFileString(), true);
//			atlModel.getModule().get
			EList<ModuleElement> eAllContents = atlModel.getModule().getElements();
			for (ModuleElement moduleElement : eAllContents) {
				if (moduleElement instanceof Helper) {
					Helper h = (Helper) moduleElement;
					if (h.getDefinition()!=null) {
						OclFeatureDefinition def = h.getDefinition();
						if(def.getFeature()!= null && def.getFeature() instanceof Attribute) {
							Attribute attr = (Attribute) def.getFeature();
							Field attribute = new Field(HELPER_TAG, attr.getName(), Store.YES, Index.ANALYZED);
							attribute.setBoost(HELPER_BOOST_VALUE);
							Field text = new Field(TEXT_TAG, attr.getName(), Store.YES, Index.ANALYZED);
							doc.add(text);
							doc.add(attribute);
						}
						if(def.getFeature()!= null && def.getFeature() instanceof Operation) {
							Operation attr = (Operation) def.getFeature();
							Field attribute = new Field(HELPER_TAG, attr.getName(), Store.YES, Index.ANALYZED);
							attribute.setBoost(HELPER_BOOST_VALUE);
							Field text = new Field(TEXT_TAG, attr.getName(), Store.YES, Index.ANALYZED);
							doc.add(text);
							doc.add(attribute);
						}
					}
				}
				if (moduleElement instanceof Rule)
				{
					Rule r = (Rule) moduleElement;
					Field ruleName = new Field(RULE_NAME_TAG,r.getName(),
							Store.YES,Index.ANALYZED);
					if(r instanceof MatchedRule) {
						MatchedRule mr = (MatchedRule) r;
						EList<InPatternElement> si = mr.getInPattern().getElements();
						for (InPatternElement inPatternElement : si) {
							if(inPatternElement instanceof SimpleInPatternElement) {
								SimpleInPatternElement sipe = (SimpleInPatternElement) inPatternElement;
								Field fromMC = new Field(FROM_METACLASS, sipe.getType().getName(), Store.YES, Index.ANALYZED);
								doc.add(fromMC);
								Field text = new Field(TEXT_TAG, sipe.getType().getName(), Store.YES, Index.ANALYZED);
								doc.add(text);
								
							}
							
						}
						EList<OutPatternElement> so = mr.getOutPattern().getElements();
						for (OutPatternElement outPatternElement : so) {
							if(outPatternElement instanceof SimpleOutPatternElement) {
								SimpleOutPatternElement sope = (SimpleOutPatternElement) outPatternElement;
								Field toMC = new Field(FROM_TOCLASS, sope.getType().getName(), Store.YES, Index.ANALYZED);
								doc.add(toMC);
								Field text = new Field(TEXT_TAG, sope.getType().getName(), Store.YES, Index.ANALYZED);
								doc.add(text);
							}
							
						}
					}
					doc.add(ruleName);
				}
				
			}
			Field type = new Field(TYPE_TAG, art.getClass().getSimpleName(), 
					Store.YES, Index.ANALYZED);
			doc.add(type);
			
			String artifactName = art.getName();
		 	Field artName = new Field(NAME_TAG, artifactName, Store.YES, Index.ANALYZED);
		 	
		 	for (DomainConformToRelation dctr : art.getDomainConformToRelation()) {
		 		Artifact temp_art = ecoreMetamodelService.findOne(dctr.getToArtifact().getId());
		 		Field fromMMName = new Field(FROM_METAMODEL_TAG, temp_art.getName(), 
		 				Store.YES, Index.ANALYZED);
		 		Field fromMMID = new Field(FROM_METAMODEL_TAG, temp_art.getId(), 
		 				Store.YES, Index.ANALYZED);
				doc.add(fromMMID);
				doc.add(fromMMName);
			}
		 	for (CoDomainConformToRelation dctr : art.getCoDomainConformToRelation()) {
		 		Artifact temp_art = ecoreMetamodelService.findOne(dctr.getToArtifact().getId());
		 		Field fromMMName = new Field(TO_METAMODEL_TAG, temp_art.getName(), 
		 				Store.YES, Index.ANALYZED);
		 		Field fromMMID = new Field(TO_METAMODEL_TAG, temp_art.getId(), 
		 				Store.YES, Index.ANALYZED);
				doc.add(fromMMID);
				doc.add(fromMMName);
			}
		 	String author = art.getAuthor().getUsername();
		 	Field authorField = new Field(AUTHOR_TAG, author, Store.YES, Index.ANALYZED);
		 	Date lastUpdate = art.getModified();
		 	Field lastUpdateField = new Field(LAST_UPDATE_TAG, lastUpdate.toString(), 
		 			Store.YES, Index.ANALYZED);
		 	for (Property prop : art.getProperties()) {
		 		
				String propName = prop.getName();
				String propValue = prop.getValue();
				if(propName != null && propValue != null){
					Field propField = new Field(propName, propValue, 
						Store.YES, Index.ANALYZED);
					doc.add(propField);
				}
			}
		 	Field idField = new Field(ID_TAG, art.getId(), 
		 			Store.YES, Index.ANALYZED);
		 	doc.add(artName);
		 	doc.add(authorField);
		 	doc.add(lastUpdateField);
			doc.add(idField);
			writer.addDocument(doc);
			}catch (Exception e) {
				logger.error(e.getMessage());e.printStackTrace();
			}
			writer.close();
		}
		  catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	/*
	 * *********************************
	 * MODEL INDEX
	 * *********************************
	 */
	
	private void createModelIndex(Model is) {
		File indexDirFile = new File(basePathLucene);
		// set the directory for the index
		Directory indexDir;
		try {
			indexDir = FSDirectory.open(indexDirFile);
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
			IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35, analyzer);
			// Create a new index in the directory, removing any
			// previously indexed documents:
			conf.setOpenMode(OpenMode.CREATE_OR_APPEND);
			// indexWriterConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
			// create the indexer
			this.writer = new IndexWriter(indexDir, conf);

			Document document = parseModelForIndex(is);
			writer.addDocument(document);
			
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new BusinessException(e1.getMessage());
		}

	}
	
	/**
	 * Perform the effective Lucene index.
	 * @param ecoreMetamodel
	 * @return
	 */
	private Document parseModelForIndex(Model model) {
		
		LuceneTag luceneTag = new LuceneTag(); 
		
		Document doc = new Document();
//		Metadata metadata = new Metadata();
//		//By using the BodyContentHandler, you can request that Tika return only the content of the document's body as a plain-text string.
//		ContentHandler handler = new BodyContentHandler(TIKA_CHARACTERS_LIMIT); //Parsing to Plain Text
//		ParseContext context = new ParseContext();
//		Parser parser = new AutoDetectParser();
//		InputStream stream = gridFileMediaService.getFileInputStream(model);
//		try {
//			parser.parse(stream, handler, metadata, context);
//		}
//		catch (TikaException e) {
//			e.printStackTrace();
//		} catch (SAXException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		finally {
//			try {
//				stream.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		try{
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		EcoreMetamodel emm = ((EcoreMetamodel)model.getMetamodel().getToArtifact());
		ecoreMetamodelService.loadArtifact(emm);
		ResourceSet load_resourceSet = new ResourceSetImpl();

		load_resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		Resource load_resource = load_resourceSet.getResource(URI.createURI(gridFileMediaService.getFilePath(model)), true);
		
		TreeIterator<EObject> eAllContents = load_resource.getAllContents();
		while (eAllContents.hasNext()) {
			EObject next = eAllContents.next();
			
			EClass eClass = next.eClass();
			if(eClass instanceof EClass)  {
				//CLASS ANNOTATIONS
				EList<EAnnotation> annotations = next.eClass().getEAnnotations();
				//TODO index also the annotations
				
				//CLASS ATTRIBUTES
				for (EAttribute attribute : eClass.getEAllAttributes()) {
					// EAnnotation ann = attribute.getEAnnotation("searchindex");
					// String key = eClass.getName() + "#" +attribute.getName();
					// System.out.println(key);
					// Object value = next.eGet(attribute);
					String attributeValue = next.eGet(eClass.getEStructuralFeature(attribute.getName())).toString();

					/*
					 * Index className:classAttributeValue
					 */
					Field eClassWithAttributeField = new Field(eClass.getName(), attributeValue, Store.YES, Index.ANALYZED);
					// eClassWithAttributeField(1.5f);
					doc.add(eClassWithAttributeField);

					/*
					 * Index className::classAttribute:attributeValue
					 */
					Field eClassWithAttributeAndAttributeValueField = new Field(eClass.getName() + CUSTOM_LUCENE_INDEX_SEPARATOR_CHARACTER + attribute.getName(), attributeValue, Store.YES, Index.ANALYZED);
					// eClassWithAttributeAndAttributeValueField(1.5f);
					doc.add(eClassWithAttributeAndAttributeValueField);
				}
				
				// EClass References
				for (EReference reference : eClass.getEAllReferences()) {
					EObject value = (EObject) next.eGet(reference);
					String key = reference.getName();
					EClass referenceTo = (EClass) value.eClass();
					Field eClassReferenceField = new Field(key, referenceTo.getName(), Store.YES, Index.ANALYZED);
					// eClassReferenceField.setBoost(1.5f);
					doc.add(eClassReferenceField);
					}
			}
			
		}
		}catch(Exception e) { 
			logger.error("Some error when try to parse EMF index");
		}
		//Artifact TYPE: "Model"
		Field artifactType = new Field(TYPE_TAG, model.getClass().getSimpleName(), Store.YES, Index.ANALYZED);
		doc.add(artifactType);

//		String text = handler.toString();
		String text = getTextFromInputStream(gridFileMediaService.getFileInputStream(model));
		Field textField = new Field(TEXT_TAG, text, Store.YES, Index.ANALYZED);
//		textField.setBoost(2.0f);
		
//		System.out.println(identifyLanguage(text));
		
		String artifactName = model.getName();
	 	Field artName = new Field(NAME_TAG, artifactName, Store.YES, Index.ANALYZED);
//		filenameField.setBoost(0.5f);
		
	 	String author = model.getAuthor().getUsername();
	 	Field authorField = new Field(AUTHOR_TAG, author, Store.YES, Index.ANALYZED);
	 	
	 	Date lastUpdate = model.getModified();
	 	Field lastUpdateField = new Field(LAST_UPDATE_TAG, lastUpdate.toString(), Store.YES, Index.ANALYZED);
//		filetypeField.setBoost(1.4f);
		
	 	for (Property prop : model.getProperties()) {
			String propName = prop.getName();
			String propValue = prop.getValue();
			Field propField = new Field(propName, propValue, Store.YES, Index.ANALYZED);
			doc.add(propField);
		}
	 	Field idField = new Field(ID_TAG, model.getId(), Store.YES, Index.ANALYZED);
	 	
	 	
	 	Field conformToFieldName = new Field(CONFORM_TO_TAG, model.getMetamodel().getToArtifact().getName(), Store.YES, Index.ANALYZED);
	 	doc.add(conformToFieldName);
	 	Field conformToFieldId = new Field(CONFORM_TO_TAG, model.getMetamodel().getToArtifact().getId(), Store.YES, Index.ANALYZED);
	 	doc.add(conformToFieldId);
	 	doc.add(textField);
	 	doc.add(artName);
	 	doc.add(authorField);
	 	doc.add(lastUpdateField);
		doc.add(idField);
		
	
		return doc;
	}



	@Override
	public void createLuceneIndexFromConfigurationPath() {
		File directoryToIndex = new File(basePathLucene);
		if (directoryToIndex.isDirectory() && directoryToIndex.exists()) {
		
			float duration = 0;
			long startTime = System.nanoTime();
			
			System.out.println("Start Metamodel indexing...");
			metamodelIndex();
			System.out.println("End Metamodel indexing!");
			System.out.println("Start Model indexing...");
			atlIndex();
			modelIndex();
			long endTime = System.nanoTime();
			duration = (endTime - startTime) / 1000000; // milliseconds(1000000) - seconds (1000000000)
			
			System.out.println("End Model indexing in " + duration +" ms.");
		}else{
			System.out.println("The provided folder doesn't exists. Check the configuration file.");
		}
	}
	
	private void metamodelIndex() {
		List<EcoreMetamodel> ecoreMMlist = ecoreMetamodelService.findAll();
		for (EcoreMetamodel ecoreMetamodel : ecoreMMlist) {
				System.out.println("Indexing: " + ecoreMetamodel.getName());
//				ecoreMetamodelService.createIndex(ecoreMetamodel);
				createLuceneIndex(ecoreMetamodel);
		}
		System.out.println("------------------------------------------------");
		System.out.println("Index of " + ecoreMMlist.size() + " metamodels done!");
		// ecoreMetamodelService.search("eClass:Family");
	}

	
	private void atlIndex() {
		List<ATLTransformation> atlTList = aTLTransformationService.findAll();
		for (ATLTransformation atl : atlTList) {
				System.out.println("Indexing: " + atl.getName());
				createLuceneIndex(atl);
		}
		System.out.println("------------------------------------------------");
		System.out.println("Index of " + atlTList.size() + " metamodels done!");
		// ecoreMetamodelService.search("eClass:Family");
	}
	
	private void modelIndex(){
		List<Model> modelList = modelService.findAll();
		for (Model model : modelList) {
//			if(model.isValid()){
				System.out.println("Indexing: " + model.getName());
				createLuceneIndex(model);
//			}else{
//				System.out.println(model.getName() + " not a valid model!");
//			}
			
		}
		System.out.println("------------------------------------------------");
		System.out.println("Index of " + modelList.size() + " models done!");
	}
	
	
	
	
	/*
	 * SEARCHING
	 */
	@Override
	public SearchResultComplete searchForm(String queryString) throws BusinessException {
		
		SearchResultComplete searchResultComplete = new SearchResultComplete();
		List<SearchResult> searchResults = new ArrayList<SearchResult>();
		
		long duration = 0;
		long startTime = System.nanoTime();
		
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
		File indexDir = new File(basePathLucene);
		Directory directory;
		try {
			directory = FSDirectory.open(indexDir);

			IndexReader reader = IndexReader.open(directory);
			IndexSearcher searcher = new IndexSearcher(reader);
//			QueryParser queryParser = new QueryParser(Version.LUCENE_35, "text", analyzer);
			
			//Get all indexed fields
//			String[] fields = indexFieldNames().toArray(new String[0]);
			String[] fields = getAllIndexTags().toArray(new String[0]);
			//Query Parse over multiple Indexed Fields
			MultiFieldQueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_35, fields, analyzer);
			org.apache.lucene.search.Query query = queryParser.parse(queryString);
			TopDocs hits = searcher.search(query, Integer.MAX_VALUE);
			System.out.println("Total hits: " + hits.totalHits);

			SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter(TAG_HIGHLIGHT_OPEN, TAG_HIGHLIGHT_CLOSE);
			Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
			for (int i = 0; i < hits.totalHits; i++) {
				int id = hits.scoreDocs[i].doc;
				Document doc = searcher.doc(id);
				
				String text = doc.get("text");
				TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), hits.scoreDocs[i].doc, "text", analyzer);
				
				TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, text, true, MAX_NUMBER_OF_FRAGMENTS_TO_RETRIEVE);

				String[] fragments = new String[frag.length];
				for (int j = 0; j < frag.length; j++) {
					if ((frag[j] != null) && (frag[j].getScore() > 0)) {
						fragments[j] = frag[j].toString();
//						System.out.println(frag[j].toString());
					}
				}
				T art = (T) artifactService.findOne(doc.get("id"));
				
				SearchResult sr = new SearchResult();
				sr.setArtifact(art);
				sr.setScore(hits.scoreDocs[i].score);
				sr.setFragments(fragments);
				
				searchResults.add(sr);
			}
			
			searcher.close();
			
		} catch (IOException e) {
			throw new BusinessException(e.getMessage());
		} catch (InvalidTokenOffsetsException e) {
			throw new BusinessException(e.getMessage());
		} catch (org.apache.lucene.queryParser.ParseException e) {
			throw new BusinessException(e.getMessage());
		}
		
		
		long endTime = System.nanoTime();
		duration = (endTime - startTime)/1000000; //milliseconds(1000000) - seconds (1000000000)
//		System.out.println("Search done in " + duration + " milliseconds");
		
		searchResultComplete.setResults(searchResults);
		searchResultComplete.setSearchTime(duration);
		
		
		return searchResultComplete;
	}
	
	@Override
	public List<T> search(String queryString, int maxSearchResult) throws BusinessException {
		List<T> listArtifact = new ArrayList<T>();
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
		File indexDir = new File(basePathLucene);
		Directory directory;
		try {
			directory = FSDirectory.open(indexDir);
			IndexReader reader = IndexReader.open(directory);
			IndexSearcher searcher = new IndexSearcher(reader);
//			QueryParser queryParser = new QueryParser(Version.LUCENE_35, "text", analyzer);
			//Get all indexed fields
			String[] fields = getAllIndexTags().toArray(new String[0]);
			//Query Parse over multiple Indexed Fields
			MultiFieldQueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_35, fields, analyzer);
			org.apache.lucene.search.Query query = queryParser.parse(queryString);
			TopDocs hits = searcher.search(query, maxSearchResult);
			SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter(TAG_HIGHLIGHT_OPEN, TAG_HIGHLIGHT_CLOSE);
			Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
			int max = (maxSearchResult > hits.totalHits)?hits.totalHits:maxSearchResult;
			for (int i = 0; i < max; i++) {
				try {
					int id = hits.scoreDocs[i].doc;
					Document doc = searcher.doc(id);
					String text = doc.get("text");
					TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), hits.scoreDocs[i].doc, "text", analyzer);
					TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, text, true, MAX_NUMBER_OF_FRAGMENTS_TO_RETRIEVE);
					String[] fragments = new String[frag.length];
					for (int j = 0; j < frag.length; j++) {
						if ((frag[j] != null) && (frag[j].getScore() > 0)) {
							fragments[j] = frag[j].toString();
						}
					}
					T art = (T) artifactService.findOne(doc.get("id"));
					listArtifact.add(art);
				} catch (Exception e){}
			}
			searcher.close();
		} catch (IOException e) {
			throw new BusinessException(e.getMessage());
		} catch (org.apache.lucene.queryParser.ParseException e) {
			throw new BusinessException(e.getMessage());
		}
//		System.out.println("Search done in " + duration + " milliseconds");
		return listArtifact;
	}
	
	
	@Override
	public SearchResultComplete searchWithPagination(String queryString, int hitsPerPage, int pageNumber) throws BusinessException {
		long duration = 0;
		long startTime = System.nanoTime();
		
		int totalNumberOfHits;
		
		SearchResultComplete searchResultComplete = new SearchResultComplete();
		List<SearchResult> searchResults = new ArrayList<SearchResult>();
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
		File indexDir = new File(basePathLucene);
		Directory directory;
		try {
			directory = FSDirectory.open(indexDir);
			IndexReader reader = IndexReader.open(directory);
			IndexSearcher searcher = new IndexSearcher(reader);
			
//			QueryParser queryParser = new QueryParser(Version.LUCENE_35, "text", analyzer);
			//Get all indexed fields
			String[] fields = getAllIndexTags().toArray(new String[0]);
			//Query Parse over multiple Indexed Fields
			MultiFieldQueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_35, fields, analyzer);
			org.apache.lucene.search.Query query = queryParser.parse(queryString);
			
			TopScoreDocCollector collector = TopScoreDocCollector.create(3000, true);  // maxSearchResult is just an int limiting the total number of hits 

			int startIndex = (pageNumber-1) * hitsPerPage;  // our page is 1 based - so we need to convert to zero based
			
	        searcher.search(query, collector);

	        totalNumberOfHits = collector.getTotalHits();

	        TopDocs hits = collector.topDocs(startIndex, hitsPerPage);
			
			SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter(TAG_HIGHLIGHT_OPEN, TAG_HIGHLIGHT_CLOSE);
			Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
//			int max = (maxSearchResult > hits.totalHits)?hits.totalHits:maxSearchResult;
			int iterator = (hitsPerPage > hits.scoreDocs.length)?hits.scoreDocs.length:hitsPerPage;
			for (int i = 0; i < iterator; i++) {
				int id = hits.scoreDocs[i].doc;
				Document doc = searcher.doc(id);
				String text = doc.get("text");
				TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(),hits.scoreDocs[i].doc, "text", analyzer);
				TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, text, true, MAX_NUMBER_OF_FRAGMENTS_TO_RETRIEVE);
				String[] fragments = new String[frag.length];
				for (int j = 0; j < frag.length; j++) {
					if ((frag[j] != null) && (frag[j].getScore() > 0)) {
						fragments[j] = frag[j].toString();
//						System.out.println(frag[j].toString());
					}
				}
				T art = (T) artifactService.findOne(doc.get("id"));
				
				//TODO fare ricerca solo di artefatti PUBBLCI o SOLO PRIVATI o SOLO SHARATI CON ME
				SearchResult sr = new SearchResult();
				sr.setArtifact(art);
				sr.setScore(hits.scoreDocs[i].doc);
				sr.setFragments(fragments);
				
				searchResults.add(sr);
			}
			searcher.close();
		} catch (IOException e) {
			throw new BusinessException(e.getMessage());
		} catch (InvalidTokenOffsetsException e) {
			throw new BusinessException(e.getMessage());
		} catch (org.apache.lucene.queryParser.ParseException e) {
			throw new BusinessException(e.getMessage());
		}
		
		long endTime = System.nanoTime();
		duration = (endTime - startTime)/1000000; //milliseconds(1000000) - seconds (1000000000)
		
		searchResultComplete.setResults(searchResults);
		searchResultComplete.setSearchTime(duration);
		searchResultComplete.setTotalHits(totalNumberOfHits);
		searchResultComplete.setHitsPerPage(hitsPerPage);
		searchResultComplete.setPageNumber(pageNumber);
		searchResultComplete.setQueryString(queryString);
		int numberOfPages = 0;
		if(totalNumberOfHits % hitsPerPage == 0){
			numberOfPages = totalNumberOfHits/hitsPerPage;
		}else{
			numberOfPages = (totalNumberOfHits/hitsPerPage) + 1;
		}
		searchResultComplete.setPages(numberOfPages);
		
		return searchResultComplete;
	}
	
	
	@Override
	/**
	 * Delete a Term from Lucene index. It take as input the FIELD_NAME and the FILE_PATH of the file we want to delete.
	 */
	public boolean deleteTermFromIndex(String fieldName, String filePath){
		int numberDeleteTerms = 0;
		boolean result = false;

		File indexDirFile = new File(basePathLucene);
		FSDirectory indexDir;
		IndexReader luceneIndexReader = null;
		try {
			indexDir = FSDirectory.open(indexDirFile);
			luceneIndexReader = IndexReader.open(indexDir);
			
			Term termToDelete = new Term(fieldName, filePath); 
			
			numberDeleteTerms = luceneIndexReader.deleteDocuments(termToDelete);
			luceneIndexReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(numberDeleteTerms > 0){
			result = true;
		}
		
		return result;
	}


	@Override
	public List<String> indexFieldNamesForMM() {
		
		return Arrays.asList(metamodelLuceneTags);
	}


	@Override
	public List<String> indexFieldNamesForT() {
		return Arrays.asList(transformationLuceneTags);
	}


	@Override
	public List<String> indexFieldNamesForM() {
		// TODO Auto-generated method stub
		return Arrays.asList(modelLuceneTags);
	}


}
