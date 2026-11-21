<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="id" %>
<%@ attribute name="value" %>
<%@ attribute name="placeholder" %>
<%@ attribute name="rows" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="inputId" value="${empty pageScope.id ? 'field-'.concat(name) : pageScope.id}"/>
<c:set var="error" value="${requestScope.errors[name]}"/>
<div class="grid gap-1.5">
    <label for="${inputId}" class="text-sm font-medium text-ink"><c:out value="${label}"/></label>
    <textarea id="${inputId}" name="${name}" rows="${empty pageScope.rows ? 3 : pageScope.rows}"
              <c:if test="${not empty pageScope.placeholder}">placeholder="${fn:escapeXml(pageScope.placeholder)}"</c:if>
              <c:if test="${not empty error}">aria-invalid="true" aria-describedby="${inputId}-error"</c:if>
              class="w-full resize-y rounded-lg border bg-surface px-3 py-2 text-sm text-ink placeholder:text-muted ${not empty error ? 'border-error focus:outline-2 focus:outline-offset-2 focus:outline-error' : 'border-border focus:outline-2 focus:outline-offset-2 focus:outline-ring'}"><c:out value="${pageScope.value != null ? pageScope.value : requestScope.old[name]}"/></textarea>
    <c:if test="${not empty error}">
        <p id="${inputId}-error" class="text-xs text-error" role="alert"><c:out value="${error}"/></p>
    </c:if>
</div>
