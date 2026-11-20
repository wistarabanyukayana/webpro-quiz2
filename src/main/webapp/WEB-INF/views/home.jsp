<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:app title="Dashboard">
    <main class="grid w-full min-h-0 flex-1 content-start gap-6 overflow-y-auto p-6 lg:p-8">
        <header class="grid gap-1.5">
            <h1 class="text-2xl font-semibold tracking-tight text-ink">Dashboard</h1>
            <p class="text-sm text-muted">Your current money overview.</p>
        </header>
        <section class="grid place-items-center gap-2 rounded-xl border border-border bg-surface p-10 text-center">
            <p class="font-medium text-ink">Signed in as <c:out value="${account.username}"/></p>
            <p class="text-sm text-muted">Nothing here yet.</p>
        </section>
    </main>
</t:app>
