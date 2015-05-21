<%@taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>



<!-- Breadcrumb START -->
<ul class="breadcrumb">
		<li><spring:message code="mdeforge.public.back.browse.breadcrumbs.you_are_here"/></li>
		<li><a href="#" class="glyphicons dashboard"><i></i> <spring:message code="mdeforge.public.back.browse.breadcrumbs.public_area"/></a></li>
		<li class="divider"></li>
		<li><spring:message code="mdeforge.public.back.browse"/></li>
		<li class="divider"></li>
		<li><spring:message code="mdeforge.public.back.browse.list"/></li>
</ul>
<!-- Breadcrumb END -->


<h3><spring:message code="mdeforge.public.back.browse.list"/></h3>


<div class="innerLR">

	
	<div class="widget widget-heading-simple widget-body-gray">
		<div class="widget-body">
		
			<!-- Table -->
			<table class="dynamicTable tableTools table table-striped table-bordered table-condensed table-white">
			
				<!-- Table heading -->
				<thead>
					<tr>						
						<th>Name</th>
						<th>Description</th>
						<th>Open</th>
						<th>Created</th>
						<th>Modified</th>
						<th>Action</th>
					</tr>
				</thead>
				<!-- // Table heading END -->
				
				<!-- Table body -->
				<tbody>
					<c:forEach items="${ecoreMetamodelsList}" var="ecoreMetamodel">
					<!-- Table row -->
					<tr class="gradeX">						
						<td>${ecoreMetamodel.getName()}</td>
						<td>
							<c:forEach items="${ecoreMetamodel.properties}" var="property">
								<c:if test="${property.getName() == 'Description�'}">
										${property.getValue()}
								</c:if>
							</c:forEach>
						</td>
						<td class="center">${ecoreMetamodel.getOpen()}</td>
						<td class="center">${ecoreMetamodel.getCreated()}</td>
						<td class="center">${ecoreMetamodel.getModified()}</td>
						<td class="center actions">
								<a href="${pageContext.request.contextPath}/public/browse/metamodel_details?metamodel_id=${ecoreMetamodel.getId()}" class="btn-action glyphicons eye_open btn-default"><i></i></a>																	
						</td>
					</tr>
					<!-- // Table row END -->
					</c:forEach>
					
					
				</tbody>
				<!-- // Table body END -->
				
			</table>
			<!-- // Table END -->
			
		</div>
	</div>
	
</div>	
	