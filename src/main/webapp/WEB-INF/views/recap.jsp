<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="tr" uri="tracker" %>
<c:set var="exportLink" value="inline-flex h-8 items-center justify-center rounded-md border border-border bg-surface px-2.5 text-xs font-medium text-ink transition-colors hover:bg-background focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"/>
<c:set var="groups" value="${['income|Income by category|text-income|border-t-income|+', 'expense|Expense by category|text-expense|border-t-expense|-']}"/>
<t:app title="Recap">
    <main class="flex min-h-0 w-full flex-1 flex-col gap-6 overflow-y-auto p-6 lg:p-8">
        <div class="flex flex-wrap items-start justify-between gap-4">
            <div class="grid gap-1">
                <h1 class="text-2xl font-semibold tracking-tight text-ink">Recap</h1>
                <p class="text-sm text-muted"><c:out value="${'Income and expense totals for ' += periodLabel += '.'}"/></p>
            </div>

            <form method="get" action="/recap" class="grid gap-3 sm:flex sm:items-end">
                <ui:select-field name="mode" id="report-mode" label="Report">
                    <option value="month"${mode == 'month' ? ' selected' : ''}>Single month</option>
                    <option value="range"${mode == 'range' ? ' selected' : ''}>Month range</option>
                    <option value="year"${mode == 'year' ? ' selected' : ''}>Full year</option>
                </ui:select-field>
                <div data-show-when="report-mode:month"${mode == 'month' ? '' : ' hidden'}>
                    <ui:text-field name="month" label="Month" type="month" value="${month}"/>
                </div>
                <div class="grid gap-3 sm:flex sm:items-end" data-show-when="report-mode:range"${mode == 'range' ? '' : ' hidden'}>
                    <ui:text-field name="from" label="From" type="month" value="${from}"/>
                    <ui:text-field name="to" label="To" type="month" value="${to}"/>
                </div>
                <div data-show-when="report-mode:year"${mode == 'year' ? '' : ' hidden'}>
                    <ui:text-field name="year" label="Year" type="number" value="${year}" min="2000" max="2100"/>
                </div>
                <ui:button type="submit">View report</ui:button>
            </form>
        </div>

        <div class="grid gap-3 sm:grid-cols-[repeat(3,minmax(0,1fr))_auto]">
            <section class="grid gap-1 rounded-xl border border-l-4 border-border border-l-income bg-surface p-4">
                <p class="text-xs text-muted">Income</p>
                <p class="text-lg font-semibold text-income">+${tr:money(income)}</p>
            </section>
            <section class="grid gap-1 rounded-xl border border-l-4 border-border border-l-expense bg-surface p-4">
                <p class="text-xs text-muted">Expense</p>
                <p class="text-lg font-semibold text-expense">-${tr:money(expense)}</p>
            </section>
            <section class="grid gap-1 rounded-xl border border-border bg-surface p-4">
                <p class="text-xs text-muted">Net change</p>
                <p class="text-lg font-semibold ${net lt 0 ? 'text-expense' : 'text-income'}">${net gt 0 ? '+' : ''}${tr:money(net)}</p>
            </section>
            <section class="flex w-fit flex-wrap items-center gap-2 rounded-xl border border-border bg-surface px-3 py-2">
                <p class="mr-1 text-xs font-medium text-muted">Export</p>
                <div class="flex flex-wrap gap-1.5">
                    <a href="${fn:escapeXml('/recap/csv?' += exportQuery)}" class="${exportLink}">CSV</a>
                    <a href="${fn:escapeXml('/recap/report?' += exportQuery)}" target="_blank" rel="noreferrer" class="${exportLink}">Print</a>
                    <a href="${fn:escapeXml('/recap/pdf?' += exportQuery)}" class="inline-flex h-8 items-center justify-center rounded-md bg-primary px-2.5 text-xs font-medium text-primary-ink transition-colors hover:bg-primary-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring">PDF</a>
                </div>
            </section>
        </div>

        <c:choose>
            <c:when test="${income == 0 and expense == 0}">
                <div class="grid gap-2 rounded-xl border border-border bg-surface p-6">
                    <h2 class="text-sm font-medium text-ink">No data for this month</h2>
                    <p class="text-sm text-muted">Record an income or expense to see the breakdown here.</p>
                </div>
            </c:when>
            <c:otherwise>
                <section class="grid gap-4 rounded-xl border border-border bg-surface p-4">
                    <div class="flex flex-wrap items-center justify-between gap-2">
                        <div class="grid gap-1">
                            <h2 class="text-sm font-medium text-ink">Period comparison</h2>
                            <p class="text-xs text-muted"><c:out value="${'Compare categories for ' += periodLabel += '.'}"/></p>
                        </div>
                        <div class="flex items-center gap-2">
                            <label for="chart-mode" class="sr-only">Chart view</label>
                            <select id="chart-mode" class="h-9 rounded-lg border border-border bg-surface px-2.5 text-xs font-medium text-ink focus:outline-2 focus:outline-offset-2 focus:outline-ring">
                                <option value="bar">Bar chart</option>
                                <option value="wheel">Wheel chart</option>
                            </select>
                        </div>
                    </div>

                    <div class="min-h-64">
                        <div class="grid gap-5 sm:h-64 sm:grid-cols-2" aria-label="Category bar charts" data-show-when="chart-mode:bar">
                            <c:forEach var="group" items="${groups}">
                                <c:set var="g" value="${group.split('[|]')}"/>
                                <c:set var="items" value="${g[0] == 'income' ? incomeTotals : expenseTotals}"/>
                                <div class="grid min-w-0 content-start gap-2">
                                    <h3 class="flex items-center gap-2 text-sm font-medium text-ink">
                                        <span class="size-2.5 rounded-full" style="background: var(--${g[0]})" aria-hidden="true"></span>
                                        ${g[1]}
                                    </h3>
                                    <c:choose>
                                        <c:when test="${empty items}"><p class="text-sm text-muted">No categories recorded.</p></c:when>
                                        <c:otherwise>
                                            <div class="flex min-h-48 items-end gap-3 overflow-x-auto px-2 pt-4">
                                                <c:forEach var="item" items="${items}">
                                                    <c:set var="height" value="${item.total * 100.0 / chartMax}"/>
                                                    <div class="grid min-w-20 flex-1 justify-items-center gap-2">
                                                        <span class="text-center text-xs font-medium ${g[2]}">${g[4]}${tr:money(item.total)}</span>
                                                        <div class="flex h-32 w-full items-end rounded-t-lg bg-background">
                                                            <div class="w-full rounded-t-lg" style="height: ${height lt 2 ? 2 : height}%; background: var(--${g[0]})" aria-hidden="true"></div>
                                                        </div>
                                                        <span class="max-w-24 truncate text-center text-xs text-ink" title="${fn:escapeXml(item.category)}"><c:out value="${item.category}"/></span>
                                                    </div>
                                                </c:forEach>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </c:forEach>
                        </div>

                        <div class="grid gap-5 sm:grid-cols-2" aria-label="Category wheel charts" data-show-when="chart-mode:wheel" hidden>
                            <c:forEach var="group" items="${groups}">
                                <c:set var="g" value="${group.split('[|]')}"/>
                                <c:set var="items" value="${g[0] == 'income' ? incomeTotals : expenseTotals}"/>
                                <c:set var="groupTotal" value="${g[0] == 'income' ? income : expense}"/>
                                <div class="grid min-w-0 content-start gap-2">
                                    <h3 class="flex items-center gap-2 text-sm font-medium text-ink">
                                        <span class="size-2.5 rounded-full" style="background: var(--${g[0]})" aria-hidden="true"></span>
                                        ${g[1]}
                                        <span class="text-xs font-normal text-muted">(share of total)</span>
                                    </h3>
                                    <c:choose>
                                        <c:when test="${empty items}"><p class="text-sm text-muted">No categories recorded.</p></c:when>
                                        <c:otherwise>
                                            <div class="flex flex-wrap items-center justify-center gap-5">
                                                <div class="grid size-48 shrink-0 place-items-center rounded-full" role="img"
                                                     style="background: ${g[0] == 'income' ? incomeWheel : expenseWheel}"
                                                     aria-label="${g[1] += ': ' += tr:money(groupTotal)}">
                                                    <div class="grid size-36 place-content-center rounded-full bg-surface text-center">
                                                        <span class="text-xs text-muted">Total</span>
                                                        <span class="text-sm font-semibold whitespace-nowrap ${g[2]}">${g[4]}${tr:money(groupTotal)}</span>
                                                    </div>
                                                </div>
                                                <div class="grid max-w-full gap-2">
                                                    <c:forEach var="item" items="${items}" varStatus="s">
                                                        <div class="flex min-w-0 items-center gap-2 text-xs text-ink">
                                                            <span class="size-2.5 shrink-0 rounded-full" style="background: var(${chartColors[s.index % 8]})" aria-hidden="true"></span>
                                                            <span class="max-w-32 truncate" title="${fn:escapeXml(item.category)}"><c:out value="${item.category}"/></span>
                                                            <strong>${tr:money(item.total)}</strong>
                                                        </div>
                                                    </c:forEach>
                                                </div>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                </section>

                <div class="grid min-h-64 flex-1 gap-6 sm:grid-cols-2">
                    <c:forEach var="group" items="${groups}">
                        <c:set var="g" value="${group.split('[|]')}"/>
                        <c:set var="items" value="${g[0] == 'income' ? incomeTotals : expenseTotals}"/>
                        <section class="flex min-h-0 flex-col gap-3 overflow-hidden rounded-xl border border-t-4 border-border bg-surface p-5 ${g[3]}">
                            <h2 class="text-sm font-semibold ${g[2]}">${g[1]}</h2>
                            <c:choose>
                                <c:when test="${empty items}"><p class="text-sm text-muted">${'No ' += fn:toLowerCase(g[1]) += ' recorded.'}</p></c:when>
                                <c:otherwise>
                                    <ul class="min-h-0 flex-1 overflow-y-auto">
                                        <c:forEach var="item" items="${items}">
                                            <li class="mb-2 flex items-center justify-between gap-3 rounded-lg border border-border px-3 py-2 last:mb-0">
                                                <span class="min-w-0 truncate text-sm text-ink"><c:out value="${item.category}"/></span>
                                                <span class="shrink-0 text-sm font-medium ${g[2]}">${g[4]}${tr:money(item.total)}</span>
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
</t:app>
