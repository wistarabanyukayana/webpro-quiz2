<%@ tag body-content="scriptless" pageEncoding="UTF-8" %>
<%@ attribute name="title" required="true" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:html title="${title}">
<div class="relative flex min-h-dvh items-center justify-center px-4 py-8">
    <div class="absolute top-5 right-5">
        <t:theme-toggle/>
    </div>
    <jsp:doBody/>
</div>
</t:html>
