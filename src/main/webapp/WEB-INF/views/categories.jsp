<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<c:set var="small" value="rounded-lg px-2 py-1 text-xs font-medium text-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"/>
<c:set var="editOpen" value="${not empty editing and openDialog == ('edit-' += editing.id)}"/>
<c:set var="editFailed" value="${editOpen and not empty errors}"/>
<t:app title="Categories">
    <main class="flex min-h-0 w-full flex-1 flex-col gap-6 overflow-y-auto p-6 lg:p-8">
        <div class="flex flex-wrap items-start justify-between gap-4">
            <div class="grid gap-1">
                <h1 class="text-2xl font-semibold tracking-tight text-ink">Categories</h1>
                <p class="text-sm text-muted">Organize income and expense entries for this account.</p>
            </div>
            <ui:button href="/categories?new=1" data-dialog-open="category-new">Add category</ui:button>
        </div>

        <c:if test="${not empty errors.category}">
            <div class="rounded-lg border border-error/30 bg-error/10 p-4 text-sm text-error" role="alert"><c:out value="${errors.category}"/></div>
        </c:if>

        <c:choose>
            <c:when test="${noCategories}">
                <div class="grid gap-2 rounded-xl border border-border bg-surface p-6">
                    <h2 class="text-sm font-medium text-ink">No categories yet</h2>
                    <p class="text-sm text-muted">Create your first category to use it when recording an entry.</p>
                    <ui:button href="/categories?new=1" data-dialog-open="category-new" cls="w-fit">Create category</ui:button>
                </div>
            </c:when>
            <c:otherwise>
                <div class="grid min-h-0 flex-1 gap-6 sm:grid-cols-2">
                    <c:forEach var="group" items="${['Income', 'Expense']}">
                        <c:set var="items" value="${group == 'Income' ? income : expense}"/>
                        <section class="flex min-h-0 flex-col gap-3 overflow-hidden rounded-xl border border-border bg-surface p-5">
                            <h2 class="text-sm font-medium text-ink">${group}</h2>
                            <c:choose>
                                <c:when test="${empty items}">
                                    <p class="text-sm text-muted">No ${fn:toLowerCase(group)} categories.</p>
                                </c:when>
                                <c:otherwise>
                                    <ul class="min-h-0 flex-1 overflow-y-auto">
                                        <c:forEach var="category" items="${items}">
                                            <li class="mb-2 flex items-center justify-between gap-3 rounded-lg border border-border px-3 py-2 last:mb-0">
                                                <span class="grid min-w-0">
                                                    <span class="truncate text-sm text-ink"><c:out value="${category.name}"/></span>
                                                    <span class="text-xs text-muted">${category.transactionCount == 0 ? 'No entries' : category.transactionCount == 1 ? '1 entry' : (category.transactionCount += ' entries')}</span>
                                                </span>
                                                <span class="flex shrink-0 items-center gap-1">
                                                    <a href="/categories?edit=${category.id}" class="${small} hover:bg-background hover:text-ink">Edit</a>
                                                    <form method="post" action="/categories/${category.id}" data-confirm="${fn:escapeXml('Delete the ' += category.name += ' category?')}">
                                                        <input type="hidden" name="_token" value="${sessionScope.csrf}">
                                                        <input type="hidden" name="_method" value="DELETE">
                                                        <input type="hidden" name="back" value="${fn:escapeXml(backUrl)}">
                                                        <button type="submit" class="${small} hover:bg-error/10 hover:text-error">Delete</button>
                                                    </form>
                                                </span>
                                            </li>
                                        </c:forEach>
                                    </ul>
                                </c:otherwise>
                            </c:choose>
                        </section>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </main>

    <c:if test="${not editFailed}">
        <ui:dialog id="category-new" title="Add category" subtitle="Choose a name and the type of entries it will organize." open="${openDialog == 'new'}">
            <form method="post" action="/categories" class="grid gap-4">
                <input type="hidden" name="_token" value="${sessionScope.csrf}">
                <input type="hidden" name="back" value="${fn:escapeXml(backUrl)}">
                <ui:text-field name="name" label="Name" placeholder="For example, Rent" required="true"/>
                <c:set var="type" value="${empty old.type ? 'expense' : old.type}"/>
                <ui:select-field name="type" label="Type">
                    <option value="income"${type == 'income' ? ' selected' : ''}>Income</option>
                    <option value="expense"${type == 'expense' ? ' selected' : ''}>Expense</option>
                </ui:select-field>
                <div class="flex items-center justify-end gap-2">
                    <ui:button variant="secondary" data-dialog-close="">Cancel</ui:button>
                    <ui:button type="submit" busy="Saving...">Add category</ui:button>
                </div>
            </form>
        </ui:dialog>
    </c:if>

    <c:if test="${not empty editing}">
        <ui:dialog id="category-edit" title="Edit category" subtitle="Choose a name and the type of entries it will organize." open="${editOpen}">
            <form method="post" action="/categories/${editing.id}" class="grid gap-4">
                <input type="hidden" name="_token" value="${sessionScope.csrf}">
                <input type="hidden" name="_method" value="PATCH">
                <input type="hidden" name="back" value="${fn:escapeXml(backUrl)}">
                <ui:text-field name="name" id="edit-name" label="Name" placeholder="For example, Rent" required="true"
                         value="${editFailed ? old.name : editing.name}"/>
                <c:set var="type" value="${editFailed ? old.type : editing.type}"/>
                <ui:select-field name="type" id="edit-type" label="Type">
                    <option value="income"${type == 'income' ? ' selected' : ''}>Income</option>
                    <option value="expense"${type == 'expense' ? ' selected' : ''}>Expense</option>
                </ui:select-field>
                <div class="flex items-center justify-end gap-2">
                    <ui:button variant="secondary" data-dialog-close="">Cancel</ui:button>
                    <ui:button type="submit" busy="Saving...">Save changes</ui:button>
                </div>
            </form>
        </ui:dialog>
    </c:if>
</t:app>
