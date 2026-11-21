<%@ tag body-content="scriptless" pageEncoding="UTF-8" dynamic-attributes="attrs" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="id" %>
<%@ attribute name="required" type="java.lang.Boolean" %>
<%@ attribute name="cls" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="inputId" value="${empty pageScope.id ? 'field-'.concat(name) : pageScope.id}"/>
<c:set var="error" value="${requestScope.errors[name]}"/>
<div class="grid gap-1.5 ${pageScope.cls}">
    <label for="${inputId}" class="text-sm font-medium text-ink">
        <c:out value="${label}"/>
        <c:if test="${pageScope.required}"><span class="text-neutral-500 dark:text-neutral-400" aria-hidden="true">*</span></c:if>
    </label>
    <select id="${inputId}" name="${name}"<c:if test="${pageScope.required}"> required</c:if>
            <c:if test="${not empty error}">aria-invalid="true" aria-describedby="${inputId}-error"</c:if>
            <c:forEach items="${attrs}" var="a"> ${a.key}="${fn:escapeXml(a.value)}"</c:forEach>
            class="h-10 w-full rounded-lg border bg-surface px-3 text-sm text-ink ${not empty error ? 'border-error focus:outline-2 focus:outline-offset-2 focus:outline-error' : 'border-border focus:outline-2 focus:outline-offset-2 focus:outline-ring'}">
        <jsp:doBody/>
    </select>
    <c:if test="${not empty error}">
        <p id="${inputId}-error" class="text-xs text-error" role="alert"><c:out value="${error}"/></p>
    </c:if>
</div>
