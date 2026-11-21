<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="tx" tagdir="/WEB-INF/tags/transactions" %>
<%@ taglib prefix="tr" uri="tracker" %>
<c:set var="label" value="${type == 'income' ? 'Income' : 'Expense'}"/>
<c:set var="small" value="rounded-lg px-2 py-1 text-xs font-medium text-muted transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"/>
<c:set var="editOpen" value="${not empty editing and openDialog == ('edit-' += editing.id)}"/>
<c:set var="editFailed" value="${editOpen and not empty errors}"/>
<c:set var="newOpen" value="${openDialog == 'new'}"/>
<t:app title="${label}">
    <main class="flex min-h-0 w-full flex-1 flex-col gap-6 overflow-y-auto p-6 lg:p-8">
        <div class="flex flex-wrap items-start justify-between gap-4">
            <div class="grid gap-1">
                <h1 class="text-2xl font-semibold tracking-tight text-ink">${label}</h1>
                <p class="text-sm text-muted">
                    ${pager.total += (pager.total == 1 ? ' entry ' : ' entries ') += (filtered ? 'matching these filters' : 'recorded')} · total ${tr:money(sum)}
                </p>
            </div>
            <div class="flex flex-wrap items-center gap-2">
                <ui:button variant="secondary" href="${exportUrl}">Export CSV</ui:button>
                <ui:button href="/${type}?new=1" data-dialog-open="transaction-new" disabled="${empty categories}">New ${type}</ui:button>
            </div>
        </div>

        <c:if test="${empty categories}">
            <div class="grid gap-1 rounded-xl border border-border bg-surface p-6">
                <h2 class="text-sm font-medium text-ink">No categories yet</h2>
                <p class="text-sm text-muted">
                    Add a ${type} category before recording an entry.
                    <a href="/categories" class="font-medium text-primary underline underline-offset-2 hover:text-primary-hover">Go to categories</a>
                </p>
            </div>
        </c:if>

        <tx:filters action="/${type}" categories="${categories}" filtered="${filtered}"/>

        <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-border bg-surface max-md:flex-none">
            <c:choose>
                <c:when test="${empty rows}">
                    <div class="grid gap-2 p-6">
                        <h2 class="text-sm font-medium text-ink">${filtered ? 'No entries match these filters' : 'No '.concat(type).concat(' entries yet')}</h2>
                        <p class="text-sm text-muted">${filtered ? 'Try a wider date range or clear the filters.' : 'Once you record '.concat(type).concat(', it will appear in this list.')}</p>
                        <c:choose>
                            <c:when test="${filtered}"><ui:button variant="secondary" href="/${type}" cls="w-fit">Clear filters</ui:button></c:when>
                            <c:when test="${not empty categories}"><ui:button href="/${type}?new=1" data-dialog-open="transaction-new" cls="w-fit">New ${type}</ui:button></c:when>
                        </c:choose>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="relative min-h-0 flex-1 overflow-auto">
                        <table class="w-full min-w-3xl text-left text-sm">
                            <thead class="text-xs text-muted">
                            <tr>
                                <th class="px-4 py-3 font-medium">Date</th>
                                <th class="px-4 py-3 font-medium">Time</th>
                                <th class="px-4 py-3 font-medium">Category</th>
                                <th class="px-4 py-3 font-medium">Notes</th>
                                <th class="px-4 py-3 text-right font-medium">Amount</th>
                                <th class="px-4 py-3 text-right font-medium"><span class="sr-only">Actions</span></th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="row" items="${rows}">
                                <tr class="border-t border-border">
                                    <td class="px-4 py-3 whitespace-nowrap text-ink">${row.date}</td>
                                    <td class="px-4 py-3 whitespace-nowrap text-muted">${empty row.time ? '-' : row.time}</td>
                                    <td class="px-4 py-3 whitespace-nowrap text-ink"><c:out value="${empty row.category ? 'Uncategorized' : row.category}"/></td>
                                    <td class="max-w-xs truncate px-4 py-3 text-muted" title="${fn:escapeXml(row.notes)}"><c:out value="${empty row.notes ? '-' : row.notes}"/></td>
                                    <td class="px-4 py-3 text-right font-medium whitespace-nowrap ${row.type == 'income' ? 'text-income' : 'text-expense'}">${(row.type == 'income' ? '+ ' : '- ') += tr:money(row.amount)}</td>
                                    <td class="px-4 py-3">
                                        <div class="flex justify-end gap-1">
                                            <a href="${fn:escapeXml(backUrl.contains('?') ? backUrl.concat('&edit=') : backUrl.concat('?edit='))}${row.id}" aria-label="Edit ${type} entry" class="${small} hover:bg-background hover:text-ink">Edit</a>
                                            <form method="post" action="/transactions/${row.id}" data-confirm="Delete this ${type} entry?">
                                                <input type="hidden" name="_token" value="${sessionScope.csrf}">
                                                <input type="hidden" name="_method" value="DELETE">
                                                <button type="submit" aria-label="Delete ${type} entry" class="${small} hover:bg-error/10 hover:text-error">Delete</button>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    <ui:pagination pager="${pager}"/>
                </c:otherwise>
            </c:choose>
        </div>
    </main>

    <c:if test="${not empty categories and not editFailed}">
        <ui:dialog id="transaction-new" title="New ${type}" subtitle="${type == 'income' ? 'Record money that came in.' : 'Record money that went out.'}" open="${newOpen}">
            <form method="post" action="/transactions" class="grid gap-4">
                <tx:form prefix="new" values="${newOpen and not empty errors ? old : null}"/>
                <div class="flex items-center justify-end gap-2 pt-1">
                    <ui:button variant="secondary" data-dialog-close="">Cancel</ui:button>
                    <ui:button type="submit" busy="Saving...">Add ${type}</ui:button>
                </div>
            </form>
        </ui:dialog>
    </c:if>
    <c:if test="${not empty editing}">
        <ui:dialog id="transaction-edit" title="Edit ${type}" subtitle="Update the details of this entry." open="${editOpen}">
            <form method="post" action="/transactions/${editing.id}" class="grid gap-4">
                <input type="hidden" name="_method" value="PATCH">
                <tx:form prefix="edit" values="${editFailed ? old : null}"/>
                <div class="flex items-center justify-end gap-2 pt-1">
                    <ui:button variant="secondary" data-dialog-close="">Cancel</ui:button>
                    <ui:button type="submit" busy="Saving...">Save changes</ui:button>
                </div>
            </form>
        </ui:dialog>
    </c:if>
</t:app>
