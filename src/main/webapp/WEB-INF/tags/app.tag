<%@ tag body-content="scriptless" pageEncoding="UTF-8" %>
<%@ attribute name="title" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<c:set var="item" value="flex h-10 shrink-0 items-center gap-2.5 rounded-lg px-3 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"/>
<c:set var="idle" value="text-muted hover:bg-background hover:text-ink"/>
<c:set var="active" value="bg-primary/10 text-primary"/>
<t:html title="${title}">
<div class="flex h-dvh flex-col overflow-hidden md:flex-row">
    <aside class="flex w-full shrink-0 flex-col gap-4 border-b border-border bg-surface p-3 md:h-dvh md:w-60 md:justify-between md:gap-6 md:border-r md:border-b-0 md:p-4">
        <nav class="flex gap-1 overflow-x-auto md:grid md:gap-1" aria-label="Main">
            <c:forEach var="link" items="${['/|Dashboard|dashboard', '/income|Income|income', '/expense|Expense|expense', '/categories|Categories|categories', '/recap|Recap|recap']}">
                <c:set var="parts" value="${link.split('[|]')}"/>
                <a href="${parts[0]}" class="${item} ${currentPath == parts[0] ? active : idle}"${currentPath == parts[0] ? ' aria-current="page"' : ''}>
                    <ui:nav-icon name="${parts[2]}" cls="size-4 shrink-0"/>
                    ${parts[1]}
                </a>
            </c:forEach>
        </nav>

        <div class="flex items-center gap-2 md:grid md:gap-1">
            <div class="flex items-center justify-between gap-2">
                <a href="/settings" class="${item} flex-1 ${currentPath == '/settings' ? active : idle}"${currentPath == '/settings' ? ' aria-current="page"' : ''}>
                    <ui:nav-icon name="settings" cls="size-4 shrink-0"/>
                    Settings
                </a>
                <t:theme-toggle/>
            </div>
            <form method="post" action="/logout" class="contents" data-logout>
                <input type="hidden" name="_token" value="${sessionScope.csrf}">
                <button type="submit" class="${item} text-muted hover:bg-background hover:text-error data-[armed]:bg-error/10 data-[armed]:text-error data-[armed]:hover:bg-error/20">
                    <ui:nav-icon name="logout" cls="size-4 shrink-0"/>
                    <span data-logout-label>Log out</span>
                </button>
            </form>
        </div>
    </aside>
    <jsp:doBody/>
</div>
</t:html>
