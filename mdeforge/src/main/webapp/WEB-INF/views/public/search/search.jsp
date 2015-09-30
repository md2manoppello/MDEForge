<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<link
	href="${pageContext.request.contextPath}/resources/bootstrap/extend/jasny-fileupload/css/fileupload.css"
	rel="stylesheet">
<link
	href="${pageContext.request.contextPath}/resources/bootstrap/extend/bootstrap-wysihtml5/css/bootstrap-wysihtml5-0.0.2.css"
	rel="stylesheet">
<link
	href="${pageContext.request.contextPath}/resources/bootstrap/extend/bootstrap-select/bootstrap-select.css"
	rel="stylesheet" />
<link
	href="${pageContext.request.contextPath}/resources/bootstrap/extend/bootstrap-toggle-buttons/static/stylesheets/bootstrap-toggle-buttons.css"
	rel="stylesheet" />
<!-- Dropzone Plugin -->
	<link href="${pageContext.request.contextPath}/resources/theme/scripts/plugins/forms/dropzone/css/dropzone.css" rel="stylesheet" />
<!-- Dropzone -->
	<script src="${pageContext.request.contextPath}/resources/theme/scripts/plugins/forms/dropzone/dropzone4.js"></script>
<!-- Mustache -->
	<script src="${pageContext.request.contextPath}/resources/theme/scripts/plugins/forms/template/mustache.js"></script>



<ul class="breadcrumb">
	<li>You are here</li>
	<li><a
		href="index.html?lang=en&amp;layout_type=fluid&amp;menu_position=menu-left&amp;style=style-default&amp;sidebar-sticky=false&amp;top_style=full&amp;sidebar_style=full"
		class="glyphicons dashboard"><i></i> Search area</a></li>
</ul>
<!-- Breadcrumb END -->

<h3>Search Page</h3>
<div class="innerLR">
	<div class="row-fluid">
		<div class="span12">
			<div class="widget widget-tabs">
				<div class="widget-head">
					<ul>
						<c:choose>
							<c:when
								test="${artifactList.size() > 0 || (artifactList == null && artifactListByExample.size() == null)}">
								<li class="active">
							</c:when>
							<c:otherwise>
								<li>
							</c:otherwise>
						</c:choose>
						<a href="#search-simple" data-toggle="tab">Simple search</a>
						</li>

						<c:choose>
							<c:when test="${artifactListByExample.size() > 0}">
								<li class="active">
							</c:when>
							<c:otherwise>
								<li>
							</c:otherwise>
						</c:choose>
						<a href="#search-by-example" data-toggle="tab">Metamodel
							Search By Example</a>
						</li>
					</ul>
				</div>
				<div class="widget-body">
					<div class="tab-content">
						<c:choose>
							<c:when
								test="${artifactList.size() > 0 || (artifactList == null && artifactListByExample.size() == null)}">
								<div class="tab-pane active" id="search-simple">
							</c:when>
							<c:otherwise>
								<div class="tab-pane" id="search-simple">
							</c:otherwise>
						</c:choose>

						<div class="widget widget-heading-simple widget-body-white">

							<div class="widget-body">

								<div class="row-fluid">
									<form action="${pageContext.request.contextPath}/public/search"
										method="get" class="form-search">
										<div class="span6 center">
											<div
												class="widget widget-heading-simple widget-body-simple margin-none">


												<div class="uniformjs">
													<label class="checkbox" style="display: inline-block;">
														<div class="checker" id="uniform-undefined">
															<span><input type="checkbox" class="checkbox"
																name="artifactType" value="models" style="opacity: 0;"></span>
														</div> Models
													</label> <label class="checkbox"
														style="display: inline-block; margin-left: 10px;">
														<div class="checker" id="uniform-undefined">
															<span class="checked"><input type="checkbox"
																class="checkbox" name="artifactType" value="metamodels"
																checked="checked" style="opacity: 0;"></span>
														</div> Metamodels
													</label> <label class="checkbox"
														style="display: inline-block; margin-left: 10px;">
														<div class="checker" id="uniform-undefined">
															<span class="checked"><input type="checkbox"
																class="checkbox" name="artifactType"
																value="transformations" style="opacity: 0;"></span>
														</div> Transformations
													</label>
												</div>
											</div>

										</div>

										<div class="span6 center">
											<div
												class="widget widget-heading-simple widget-body-simple text-right">

												<input type="text" class="input-large"
													placeholder="Type your keywords .. " name="search_string"
													id="searchField">
												<button type="submit" class="btn btn-inverse">
													<spring:message code="mdeforge.common.action.search" />
												</button>

											</div>
									</form>





								</div>
							</div>
						</div>
					</div>



					<c:choose>
						<c:when test="${artifactList.size() > 0}">
							<div
								class="widget widget-heading-simple widget-body-white margin-none">
								<div class="widget-body">
									<h5 class="text-uppercase strong separator bottom">${artifactList.size()}
										Search results</h5>

									<c:forEach items="${artifactList}" var="artifact">
										<div class="row-fluid">

											<div class="span12">
												<h5 class="strong text-uppercase">
													<c:choose>
														<c:when
															test="${artifact.getClass().name == 'org.mdeforge.business.model.ATLTransformation'}">
															<a
																href="${pageContext.request.contextPath}/public/browse/transformation_details?transformation_id=${artifact.getId()}">${artifact.getName()}</a>
														</c:when>
														<c:when
															test="${artifact.getClass().name == 'org.mdeforge.business.model.EcoreMetamodel'}">
															<a
																href="${pageContext.request.contextPath}/public/browse/metamodel_details?metamodel_id=${artifact.getId()}">${artifact.getName()}</a>
														</c:when>
														<c:when
															test="${artifact.getClass().name == 'org.mdeforge.business.model.Model'}">
															<a href="#">${artifact.getName()}</a>
														</c:when>
													</c:choose>
												</h5>
												<span class="badge badge-success">Score: <fmt:formatNumber
														type="number" maxFractionDigits="3"
														value="${artifact.getScore()}" /></span>
												<p>
													<c:set var="existDescription" value="false" />
													<c:set var="description" value="" />
													<c:forEach items="${artifact.properties}" var="property">

														<c:if test="${property.getName() == 'Description '}">
															<c:set var="existDescription" value="true" />
															<c:set var="description" value="${property.getValue()}" />

														</c:if>
													</c:forEach>

													<c:choose>
														<c:when test="${existDescription == 'true'}">
															<c:out value="${description}" />
														</c:when>
														<c:otherwise>
															<i>No description</i>
														</c:otherwise>
													</c:choose>


												</p>


												<c:choose>
													<c:when
														test="${artifact.getClass().name.equals('org.mdeforge.business.model.ATLTransformation')}">
														<span class="badge badge-important">ATL
															Transformation</span>
													</c:when>
													<c:when
														test="${artifact.getClass().name.equals('org.mdeforge.business.model.EcoreMetamodel')}">
														<span class="badge badge-warning">Ecore Metamodel</span>
													</c:when>
													<c:when
														test="${artifact.getClass().name.equals('org.mdeforge.business.model.Model')}">
														<span class="badge badge-info">Model</span>
													</c:when>
												</c:choose>
												<span class="label">Last update: <fmt:formatDate
														type="date" value="${artifact.getModified()}" /></span>


												<p class="margin-none strong">
													<a
														href="${pageContext.request.contextPath}/public/browse/metamodel_download?metamodel_id=${artifact.getId()}"
														title="Metamodel Download"
														class="glyphicons single download_alt"><i></i>Download</a>
												</p>
											</div>
										</div>
										<hr class="separator">
									</c:forEach>


									<!-- 												<div class="pagination margin-none"> -->
									<!-- 												    <ul> -->
									<!-- 												        <li class="disabled"><a href="#">&lt;</a></li> -->
									<!-- 												        <li class="active"><a href="#">1</a></li> -->
									<!-- 												        <li><a href="#">2</a></li> -->
									<!-- 												        <li><a href="#">3</a></li> -->
									<!-- 												        <li><a href="#">&gt;</a></li> -->
									<!-- 												    </ul> -->
									<!-- 												</div> -->

								</div>
							</div>
						</c:when>
						<c:otherwise>
							<!-- <div class="widget widget-heading-simple widget-body-white margin-none">
											<div class="widget-body">
												<h5 class="text-uppercase strong separator bottom">No Results</h5>
											</div>
									</div> -->
						</c:otherwise>
					</c:choose>

				</div>


				<c:choose>
					<c:when test="${artifactListByExample.size() > 0}">
						<div class="tab-pane active" id="search-by-example">
					</c:when>
					<c:otherwise>
						<div class="tab-pane" id="search-by-example">
					</c:otherwise>
				</c:choose>

				<script>
				
				Dropzone.options.myAwesomeDropzone = {
						  paramName: "metamodelfile",
						  autoProcessQueue : false,
						  init: function() {
							  	var myAwesomeDropzone = this;
							  	$('#searchEmoreMetamodelByExampleButton').on("click", function(e){
									e.preventDefault();
								    e.stopPropagation();
								    console.log(myAwesomeDropzone);
								    this.processQueue();
								});
							    this.on("success", function(file) {
							    	var v = new Object();
							    	console.log(file)
							    	v.response = $.parseJSON(file.xhr.response);
							    	console.log(v);
							    	$.get('${pageContext.request.contextPath}/resources/theme/scripts/plugins/forms/template/resultEcoreSearchByExampleTemplate.html', function(template) {
							    	    var rendered = Mustache.render(template, v);
							    	    $('#resultEcoreMetamodelList').append(rendered);
							    	  });
							    });
							    
							  }
						};
				</script>

				<div class="widget widget-heading-simple widget-body-white">
					<div class="widget-body">
						<h4 class="separator bottom">Drop Metamodel Upload</h4>
							
						<form:form cssClass="dropzone form-horizontal" modelAttribute="metamodelfile"
							action="${pageContext.request.contextPath}/public/search_metamodel_by_example/result"
							role="form" method="POST" id="my-awesome-dropzone" enctype="multipart/form-data">
								<div class="fallback">
				    				<input name="file" type="file" multiple />
				  				</div>
				  				<div class="dropzone-previews"></div>
				  				<button type="submit" class="btn btn-inverse" id="searchEmoreMetamodelByExampleButton">
											<spring:message code="mdeforge.common.action.search" /></button>
						</form:form>
					
					
						
						
							
					</div>
				</div>

				<c:choose>
					<c:when test="${artifactListByExample.size() > 0}">
						<div
							class="widget widget-heading-simple widget-body-white margin-none">
							<div class="widget-body" id="resultEcoreMetamodelList">
								<h5 class="text-uppercase strong separator bottom">${artifactListByExample.size()}
									Search results</h5>
							</div>
						</div>
					</c:when>
					<c:otherwise>
						<div
							class="widget widget-heading-simple widget-body-white margin-none">
							<div class="widget-body" id="resultEcoreMetamodelList">
							</div>
						</div>
					</c:otherwise>
				</c:choose>
			</div>

		</div>
	</div>
</div>

</div>

</div>
</div>

