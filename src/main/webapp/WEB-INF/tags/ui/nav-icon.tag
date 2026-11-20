<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="cls" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<svg class="${pageScope.cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
<c:choose>
    <c:when test="${name == 'dashboard'}">
        <rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/>
        <rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>
    </c:when>
    <c:when test="${name == 'income'}"><path d="M12 3v12"/><path d="m7 10 5 5 5-5"/><path d="M4 19h16"/></c:when>
    <c:when test="${name == 'expense'}"><path d="M12 21V9"/><path d="m7 14 5-5 5 5"/><path d="M4 3h16"/></c:when>
    <c:when test="${name == 'categories'}"><path d="M4 11V5a1 1 0 0 1 1-1h6l9 9-7 7-9-9z"/><circle cx="8" cy="8" r="1"/></c:when>
    <c:when test="${name == 'recap'}">
        <path d="M4 20h16"/><rect x="5" y="11" width="3.5" height="6" rx="1"/>
        <rect x="10.25" y="7" width="3.5" height="10" rx="1"/><rect x="15.5" y="3.5" width="3.5" height="13.5" rx="1"/>
    </c:when>
    <c:when test="${name == 'settings'}">
        <circle cx="12" cy="12" r="3"/>
        <path d="M19.4 15a1.7 1.7 0 0 0 .34 1.87l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.7 1.7 0 0 0-1.87-.34 1.7 1.7 0 0 0-1 1.56V21a2 2 0 1 1-4 0v-.11a1.7 1.7 0 0 0-1.11-1.56 1.7 1.7 0 0 0-1.87.34l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-1.56-1H3a2 2 0 1 1 0-4h.11A1.7 1.7 0 0 0 4.67 8.9a1.7 1.7 0 0 0-.34-1.87l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.7 1.7 0 0 0 1.87.34H9a1.7 1.7 0 0 0 1-1.56V3a2 2 0 1 1 4 0v.11a1.7 1.7 0 0 0 1 1.56 1.7 1.7 0 0 0 1.87-.34l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.7 1.7 0 0 0-.34 1.87V9a1.7 1.7 0 0 0 1.56 1H21a2 2 0 1 1 0 4h-.11a1.7 1.7 0 0 0-1.49 1z"/>
    </c:when>
    <c:otherwise><path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4"/><path d="M10 8l4 4-4 4"/><path d="M14 12H4"/></c:otherwise>
</c:choose>
</svg>
