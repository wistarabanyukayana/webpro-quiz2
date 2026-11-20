<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="message" required="true" %>
<%@ attribute name="href" %>
<%@ attribute name="linkText" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:guest title="${title}">
    <div class="grid max-w-sm justify-items-stretch gap-2">
        <h1 class="mt-1 text-2xl font-semibold tracking-tight text-ink"><c:out value="${title}"/></h1>
        <p class="text-sm text-muted"><c:out value="${message}"/></p>
        <a href="${empty pageScope.href ? '/' : pageScope.href}" class="mt-3 w-fit font-medium text-primary underline underline-offset-2 hover:text-primary-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"><c:out value="${empty pageScope.linkText ? 'Back to the homepage' : pageScope.linkText}"/></a>
    </div>
</t:guest>
