<%@ tag body-content="scriptless" pageEncoding="UTF-8" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="subtitle" %>
<%@ attribute name="open" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- no display class here, it would override display:none when closed --%>
<dialog id="${id}" aria-labelledby="${id}-title" closedby="any"<c:if test="${pageScope.open}"> data-open</c:if>
        class="m-auto w-[calc(100%-2rem)] max-w-lg rounded-xl border border-border bg-surface p-6 text-ink shadow-xl">
    <div class="grid gap-5">
        <div class="flex items-start justify-between gap-4">
            <div class="grid gap-1">
                <h2 id="${id}-title" class="text-lg font-semibold tracking-tight text-ink"><c:out value="${title}"/></h2>
                <c:if test="${not empty pageScope.subtitle}"><p class="text-xs text-muted"><c:out value="${pageScope.subtitle}"/></p></c:if>
            </div>
            <button type="button" data-dialog-close
                    class="rounded-lg px-2 py-1 text-xs font-medium text-muted hover:bg-background hover:text-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring">Close</button>
        </div>
        <jsp:doBody/>
    </div>
</dialog>
