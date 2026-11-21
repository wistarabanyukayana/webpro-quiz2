<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ attribute name="pager" required="true" type="id.its.tracker.util.Pager" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:if test="${pager.total() > 0}">
    <nav class="flex flex-wrap items-center justify-between gap-3 border-t border-border px-4 py-3" aria-label="Pagination">
        <p class="text-xs text-muted">Showing ${pager.from()}–${pager.to()} of ${pager.total()}</p>
        <div class="flex flex-wrap items-center gap-1">
            <c:forEach items="${pager.links()}" var="link">
                <c:choose>
                    <c:when test="${not empty link.url()}">
                        <a href="${fn:escapeXml(link.url())}"
                           class="grid h-8 min-w-8 place-items-center rounded-lg px-2 text-xs font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring ${link.active() ? 'bg-primary/10 text-primary' : 'text-muted hover:bg-background hover:text-ink'}"<c:if test="${link.active()}"> aria-current="page"</c:if>>${link.label()}</a>
                    </c:when>
                    <c:otherwise>
                        <span class="grid h-8 min-w-8 place-items-center px-2 text-xs text-muted">${link.label()}</span>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </div>
    </nav>
</c:if>
