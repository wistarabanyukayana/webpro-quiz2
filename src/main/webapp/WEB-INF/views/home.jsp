<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="tx" tagdir="/WEB-INF/tags/transactions" %>
<%@ taglib prefix="tr" uri="tracker" %>
<t:app title="Dashboard">
    <main class="grid w-full min-h-0 flex-1 content-start gap-6 overflow-y-auto p-6 md:grid-rows-[auto_auto_auto_minmax(0,1fr)] lg:p-8">
        <div class="flex flex-wrap items-start justify-between gap-4">
            <div class="grid gap-1">
                <h1 class="text-2xl font-semibold tracking-tight text-ink">Dashboard</h1>
                <p class="text-sm text-muted">Your current money overview.</p>
            </div>
            <div class="flex flex-wrap gap-2">
                <ui:button variant="secondary" href="/income">View income</ui:button>
                <ui:button href="/expense">View expenses</ui:button>
            </div>
        </div>

        <section class="grid gap-4 lg:grid-cols-[1.35fr_1fr_1fr]">
            <div class="grid gap-8 rounded-xl bg-primary p-6 text-primary-ink">
                <div class="grid gap-1">
                    <p class="text-sm opacity-80">Current balance</p>
                    <p class="break-words text-3xl font-semibold tracking-tight">${tr:money(balance)}</p>
                </div>
            </div>
            <div class="grid gap-4 rounded-xl border border-border bg-surface p-5">
                <div class="grid gap-1">
                    <p class="text-sm text-muted">Income</p>
                    <p class="text-xl font-semibold text-income">+${tr:money(monthIncome)}</p>
                </div>
                <p class="text-xs text-muted">${monthLabel}</p>
            </div>
            <div class="grid gap-4 rounded-xl border border-border bg-surface p-5">
                <div class="grid gap-1">
                    <p class="text-sm text-muted">Expenses</p>
                    <p class="text-xl font-semibold text-expense">-${tr:money(monthExpense)}</p>
                </div>
                <p class="text-xs text-muted">${monthLabel}</p>
            </div>
        </section>

        <tx:filters action="/" categories="${categories}" withType="true" filtered="${filtered}"/>

        <c:choose>
            <c:when test="${not empty rows}">
                <section class="grid min-h-0 grid-rows-[auto_minmax(0,1fr)_auto] gap-4 overflow-hidden rounded-xl border border-border bg-surface p-5">
                    <div class="grid gap-1">
                        <h2 class="text-sm font-medium text-ink">Recent transactions</h2>
                        <p class="text-xs text-muted">Your latest recorded entries.</p>
                    </div>
                    <div class="min-h-0 overflow-y-auto pr-1">
                        <div class="grid gap-2">
                            <c:forEach var="row" items="${rows}">
                                <div class="flex flex-wrap items-center justify-between gap-3 border-t border-border py-3 first:border-t-0 first:pt-0 last:pb-0">
                                    <div class="grid min-w-0 gap-1">
                                        <span class="truncate text-sm font-medium text-ink"><c:out value="${empty row.category ? 'Uncategorized' : row.category}"/></span>
                                        <span class="truncate text-xs text-muted"><c:out value="${empty row.notes ? tr:date(row.date) : row.notes}"/></span>
                                    </div>
                                    <span class="shrink-0 text-sm font-medium ${row.type == 'income' ? 'text-income' : 'text-expense'}">${(row.type == 'income' ? '+ ' : '- ') += tr:money(row.amount)}</span>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                    <ui:pagination pager="${pager}"/>
                </section>
            </c:when>
            <c:otherwise>
                <section class="grid gap-3 rounded-xl border border-border bg-surface p-6">
                    <div class="grid gap-1">
                        <h2 class="text-sm font-medium text-ink">Nothing here yet</h2>
                        <p class="text-sm text-muted">Add an income or expense entry to see your overview grow.</p>
                    </div>
                    <div class="flex flex-wrap gap-2">
                        <ui:button href="/income">Add income</ui:button>
                        <ui:button variant="secondary" href="/expense">Add expense</ui:button>
                    </div>
                </section>
            </c:otherwise>
        </c:choose>
    </main>
</t:app>
