<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="tr" uri="tracker" %>
<c:set var="small" value="rounded-lg px-2 py-1 text-xs font-medium text-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"/>
<c:set var="step" value="inline-flex size-10 shrink-0 items-center justify-center rounded-lg border border-border bg-surface text-muted transition-colors hover:bg-background hover:text-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"/>
<c:set var="editOpen" value="${not empty editing and openDialog == ('edit-' += editing.id)}"/>
<c:set var="editFailed" value="${editOpen and not empty errors}"/>
<c:set var="left" value="${budgeted - spent}"/>
<t:app title="Budgets">
    <main class="@container flex min-h-0 w-full flex-1 flex-col gap-6 overflow-y-auto p-6 lg:p-8">
        <div class="flex flex-wrap items-start justify-between gap-4">
            <div class="grid gap-1">
                <h1 class="text-2xl font-semibold tracking-tight text-ink">Budgets</h1>
                <p class="text-sm text-muted"><c:out value="${'Spending limits for ' += monthLabel += '.'}"/></p>
            </div>
            <div class="flex flex-wrap items-end gap-3 max-sm:w-full">
                <form method="get" action="/budgets" class="flex items-end gap-2">
                    <a href="/budgets?month=${prevMonth}" class="${step}" aria-label="Previous month">
                        <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m15 18-6-6 6-6"/></svg>
                    </a>
                    <div class="grid gap-1.5">
                        <label for="page-month" class="text-sm font-medium text-ink">Month</label>
                        <input id="page-month" name="month" type="month" value="${month}" required
                               class="h-10 w-40 rounded-lg border border-border bg-surface px-3 text-sm text-ink focus:outline-2 focus:outline-offset-2 focus:outline-ring">
                    </div>
                    <a href="/budgets?month=${nextMonth}" class="${step}" aria-label="Next month">
                        <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m9 18 6-6-6-6"/></svg>
                    </a>
                    <ui:button type="submit" variant="secondary">View</ui:button>
                </form>
                <ui:button href="/budgets?month=${month}&new=1" data-dialog-open="budget-new">Add budget</ui:button>
            </div>
        </div>

        <c:if test="${not empty monthError}">
            <div class="rounded-lg border border-error/30 bg-error/10 p-4 text-sm text-error" role="alert"><c:out value="${monthError}"/></div>
        </c:if>

        <c:choose>
            <c:when test="${empty categories}">
                <div class="grid gap-2 rounded-xl border border-border bg-surface p-6">
                    <h2 class="text-sm font-medium text-ink">No expense categories yet</h2>
                    <p class="text-sm text-muted">A budget sets a limit for one expense category. Create an expense category first.</p>
                    <ui:button href="/categories?new=1" cls="w-fit">Create category</ui:button>
                </div>
            </c:when>
            <c:when test="${empty budgets}">
                <div class="grid gap-2 rounded-xl border border-border bg-surface p-6">
                    <h2 class="text-sm font-medium text-ink"><c:out value="${'No budgets for ' += monthLabel}"/></h2>
                    <p class="text-sm text-muted">Set a monthly limit for an expense category to see how much of it is left.</p>
                    <ui:button href="/budgets?month=${month}&new=1" data-dialog-open="budget-new" cls="w-fit">Add budget</ui:button>
                </div>
            </c:when>
            <c:otherwise>
                <div class="grid gap-3 @xl:grid-cols-3">
                    <section class="grid gap-1 rounded-xl border border-border bg-surface p-4">
                        <p class="text-xs text-muted">Budgeted</p>
                        <p class="text-lg font-semibold text-ink">${tr:money(budgeted)}</p>
                    </section>
                    <section class="grid gap-1 rounded-xl border border-l-4 border-border border-l-expense bg-surface p-4">
                        <p class="text-xs text-muted">Spent</p>
                        <p class="text-lg font-semibold text-expense">${spent gt 0 ? '-' : ''}${tr:money(spent)}</p>
                    </section>
                    <section class="grid gap-1 rounded-xl border border-border bg-surface p-4">
                        <p class="text-xs text-muted">${left lt 0 ? 'Over budget by' : 'Left to spend'}</p>
                        <p class="text-lg font-semibold ${left lt 0 ? 'text-error' : 'text-ink'}">${tr:money(left lt 0 ? -left : left)}</p>
                    </section>
                </div>

                <ul class="grid gap-3 @2xl:grid-cols-2">
                    <c:forEach var="budget" items="${budgets}">
                        <c:set var="percent" value="${budget.percent()}"/>
                        <c:set var="bar" value="${budget.remaining() lt 0 ? 'bg-error' : percent ge 80 ? 'bg-warning' : 'bg-primary'}"/>
                        <li class="grid gap-3 rounded-xl border border-border bg-surface p-4">
                            <div class="flex items-start justify-between gap-3">
                                <div class="grid min-w-0 gap-0.5">
                                    <h2 class="truncate text-sm font-medium text-ink"><c:out value="${budget.category}"/></h2>
                                    <p class="text-xs text-muted">
                                        <span class="font-medium ${budget.spent gt 0 ? 'text-expense' : 'text-ink'}">${budget.spent gt 0 ? '-' : ''}${tr:money(budget.spent)}</span> of ${tr:money(budget.amount)}
                                    </p>
                                </div>
                                <span class="flex shrink-0 items-center gap-1">
                                    <a href="${fn:escapeXml('/budgets?month=' += month += '&edit=' += budget.id)}" class="${small} hover:bg-background hover:text-ink">Edit</a>
                                    <form method="post" action="/budgets/${budget.id}" data-confirm="${fn:escapeXml('Delete the ' += budget.category += ' budget for ' += monthLabel += '?')}">
                                        <input type="hidden" name="_token" value="${sessionScope.csrf}">
                                        <input type="hidden" name="_method" value="DELETE">
                                        <input type="hidden" name="back" value="${fn:escapeXml(backUrl)}">
                                        <button type="submit" class="${small} hover:bg-error/10 hover:text-error">Delete</button>
                                    </form>
                                </span>
                            </div>
                            <div class="h-2 overflow-hidden rounded-full bg-background" role="progressbar" aria-valuemin="0" aria-valuemax="100"
                                 aria-valuenow="${percent gt 100 ? 100 : percent}" aria-label="${fn:escapeXml(budget.category += ' budget used')}">
                                <div class="h-full rounded-full ${bar}" style="width: ${percent gt 100 ? 100 : percent}%"></div>
                            </div>
                            <div class="flex items-center justify-between gap-3 text-xs">
                                <span class="${budget.remaining() lt 0 ? 'font-medium text-error' : 'text-muted'}">
                                    ${budget.remaining() lt 0 ? 'Over by ' += tr:money(-budget.remaining()) : tr:money(budget.remaining()) += ' left'}
                                </span>
                                <span class="font-medium text-ink">${percent}%</span>
                            </div>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </main>

    <c:if test="${not editFailed and not empty categories}">
        <c:set var="v" value="${empty old ? {'month': month} : old}"/>
        <ui:dialog id="budget-new" title="Add budget" subtitle="Set a spending limit for one expense category in one month." open="${openDialog == 'new'}">
            <form method="post" action="/budgets" class="grid gap-4">
                <input type="hidden" name="_token" value="${sessionScope.csrf}">
                <input type="hidden" name="back" value="${fn:escapeXml(backUrl)}">
                <ui:select-field name="category_id" id="new-category" label="Category" required="true">
                    <option value="">Choose a category</option>
                    <c:forEach var="category" items="${categories}">
                        <option value="${category.id}"${('' += category.id) == v.category_id ? ' selected' : ''}><c:out value="${category.name}"/></option>
                    </c:forEach>
                </ui:select-field>
                <div class="grid gap-4 sm:grid-cols-2">
                    <ui:text-field name="month" id="new-month" label="Month" type="month" placeholder="YYYY-MM" required="true" value="${empty v.month ? '' : v.month}"/>
                    <ui:text-field name="amount" id="new-amount" label="Limit" type="number" min="1" step="1" placeholder="0" required="true" value="${empty v.amount ? '' : v.amount}"/>
                </div>
                <div class="flex items-center justify-end gap-2">
                    <ui:button variant="secondary" data-dialog-close="">Cancel</ui:button>
                    <ui:button type="submit" busy="Saving...">Add budget</ui:button>
                </div>
            </form>
        </ui:dialog>
    </c:if>

    <c:if test="${not empty editing}">
        <c:set var="v" value="${editFailed ? old : {'category_id': '' += editing.categoryId, 'month': editing.month, 'amount': '' += editing.amount}}"/>
        <ui:dialog id="budget-edit" title="Edit budget" subtitle="Set a spending limit for one expense category in one month." open="${editOpen}">
            <form method="post" action="/budgets/${editing.id}" class="grid gap-4">
                <input type="hidden" name="_token" value="${sessionScope.csrf}">
                <input type="hidden" name="_method" value="PATCH">
                <input type="hidden" name="back" value="${fn:escapeXml(backUrl)}">
                <ui:select-field name="category_id" id="edit-category" label="Category" required="true">
                    <option value="">Choose a category</option>
                    <c:forEach var="category" items="${categories}">
                        <option value="${category.id}"${('' += category.id) == v.category_id ? ' selected' : ''}><c:out value="${category.name}"/></option>
                    </c:forEach>
                </ui:select-field>
                <div class="grid gap-4 sm:grid-cols-2">
                    <ui:text-field name="month" id="edit-month" label="Month" type="month" placeholder="YYYY-MM" required="true" value="${empty v.month ? '' : v.month}"/>
                    <ui:text-field name="amount" id="edit-amount" label="Limit" type="number" min="1" step="1" placeholder="0" required="true" value="${empty v.amount ? '' : v.amount}"/>
                </div>
                <div class="flex items-center justify-end gap-2">
                    <ui:button variant="secondary" data-dialog-close="">Cancel</ui:button>
                    <ui:button type="submit" busy="Saving...">Save changes</ui:button>
                </div>
            </form>
        </ui:dialog>
    </c:if>
</t:app>
