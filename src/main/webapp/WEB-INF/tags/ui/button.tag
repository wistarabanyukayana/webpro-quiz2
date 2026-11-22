<%@ tag body-content="scriptless" pageEncoding="UTF-8" dynamic-attributes="attrs" %>
<%@ attribute name="variant" %>
<%@ attribute name="type" %>
<%@ attribute name="href" %>
<%@ attribute name="cls" %>
<%@ attribute name="disabled" type="java.lang.Boolean" %>
<%@ attribute name="busy" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="classes" value="inline-flex h-10 items-center justify-center gap-2 rounded-lg px-4 text-sm font-medium transition-colors duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring disabled:cursor-not-allowed disabled:opacity-50 ${pageScope.variant == 'secondary' ? 'border border-border bg-surface text-ink hover:bg-background' : pageScope.variant == 'danger' ? 'bg-error text-primary-ink hover:opacity-90' : 'bg-primary text-primary-ink hover:bg-primary-hover'} ${pageScope.cls}"/>
<c:choose>
    <c:when test="${not empty pageScope.href and not pageScope.disabled}">
        <a href="${fn:escapeXml(pageScope.href)}" class="${classes}"<c:forEach items="${attrs}" var="a"> ${a.key}="${fn:escapeXml(a.value)}"</c:forEach>><jsp:doBody/></a>
    </c:when>
    <c:otherwise>
        <button type="${empty pageScope.type ? 'button' : pageScope.type}" class="${classes}"<c:if test="${pageScope.disabled}"> disabled</c:if><c:if test="${not empty pageScope.busy}"> data-busy-text="${fn:escapeXml(pageScope.busy)}"</c:if><c:forEach items="${attrs}" var="a"> ${a.key}="${fn:escapeXml(a.value)}"</c:forEach>><jsp:doBody/></button>
    </c:otherwise>
</c:choose>
