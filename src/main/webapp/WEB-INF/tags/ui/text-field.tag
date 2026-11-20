<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="type" %>
<%@ attribute name="value" %>
<%@ attribute name="id" %>
<%@ attribute name="placeholder" %>
<%@ attribute name="autocomplete" %>
<%@ attribute name="required" type="java.lang.Boolean" %>
<%@ attribute name="min" %>
<%@ attribute name="max" %>
<%@ attribute name="step" %>
<%@ attribute name="cls" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="type" value="${empty pageScope.type ? 'text' : pageScope.type}"/>
<c:set var="inputId" value="${empty pageScope.id ? 'field-'.concat(name) : pageScope.id}"/>
<c:set var="error" value="${requestScope.errors[name]}"/>
<c:set var="shown" value="${pageScope.type == 'password' ? '' : (pageScope.value != null ? pageScope.value : requestScope.old[name])}"/>
<div class="grid gap-1.5 ${pageScope.cls}">
    <label for="${inputId}" class="text-sm font-medium text-ink">
        <c:out value="${label}"/>
        <c:if test="${pageScope.required}"><span class="text-neutral-500 dark:text-neutral-400" aria-hidden="true">*</span></c:if>
    </label>
    <div class="relative">
        <input id="${inputId}" name="${name}" type="${pageScope.type}" value="${fn:escapeXml(shown)}"
               <c:if test="${not empty pageScope.placeholder}">placeholder="${fn:escapeXml(pageScope.placeholder)}"</c:if>
               <c:if test="${not empty pageScope.autocomplete}">autocomplete="${pageScope.autocomplete}"</c:if>
               <c:if test="${not empty pageScope.min}">min="${pageScope.min}"</c:if>
               <c:if test="${not empty pageScope.max}">max="${pageScope.max}"</c:if>
               <c:if test="${not empty pageScope.step}">step="${pageScope.step}"</c:if>
               <c:if test="${pageScope.required}">required</c:if>
               <c:if test="${not empty error}">aria-invalid="true" aria-describedby="${inputId}-error"</c:if>
               class="h-10 w-full rounded-lg border bg-surface px-3 text-sm text-ink placeholder:text-muted ${not empty error ? 'border-error focus:outline-2 focus:outline-offset-2 focus:outline-error' : 'border-border focus:outline-2 focus:outline-offset-2 focus:outline-ring'} ${pageScope.type == 'password' ? 'pr-14' : ''}">
        <c:if test="${pageScope.type == 'password'}">
            <button type="button" data-reveal="${inputId}"
                    class="absolute inset-y-0 right-0 flex items-center px-3 text-xs font-medium text-muted underline-offset-2 hover:text-ink hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring">Show</button>
        </c:if>
    </div>
    <c:if test="${not empty error}">
        <p id="${inputId}-error" class="text-xs text-error" role="alert"><c:out value="${error}"/></p>
    </c:if>
</div>
