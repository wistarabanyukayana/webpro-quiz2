<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ attribute name="action" required="true" %>
<%@ attribute name="categories" required="true" type="java.util.List" %>
<%@ attribute name="withType" type="java.lang.Boolean" %>
<%@ attribute name="filtered" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<c:set var="input" value="h-10 w-full rounded-lg border bg-surface px-3 text-sm text-ink placeholder:text-muted focus:outline-2 focus:outline-offset-2"/>
<c:set var="ok" value="border-border focus:outline-ring"/>
<c:set var="bad" value="border-error focus:outline-error"/>
<c:set var="errs" value="${requestScope.filterErrors}"/>
<form method="get" action="${action}"
      class="grid gap-4 rounded-xl border border-border bg-surface p-4 sm:grid-cols-2 ${pageScope.withType ? 'lg:grid-cols-6' : 'lg:grid-cols-5'} lg:items-end">
    <c:forEach var="f" items="${['from|From|date', 'to|To|date']}">
        <c:set var="p" value="${f.split('[|]')}"/>
        <div class="grid gap-1.5">
            <label for="field-${p[0]}" class="text-sm font-medium text-ink">${p[1]}</label>
            <input id="field-${p[0]}" name="${p[0]}" type="date" value="${fn:escapeXml(param[p[0]])}"
                   class="${input} ${empty errs[p[0]] ? ok : bad}"<c:if test="${not empty errs[p[0]]}"> aria-invalid="true" aria-describedby="field-${p[0]}-error"</c:if>>
            <c:if test="${not empty errs[p[0]]}"><p id="field-${p[0]}-error" class="text-xs text-error" role="alert"><c:out value="${errs[p[0]]}"/></p></c:if>
        </div>
    </c:forEach>
    <c:if test="${pageScope.withType}">
        <div class="grid gap-1.5">
            <label for="filter-type" class="text-sm font-medium text-ink">Type</label>
            <select id="filter-type" name="type" class="${input} ${empty errs.type ? ok : bad}">
                <option value="">All types</option>
                <option value="income"${param.type == 'income' ? ' selected' : ''}>Income</option>
                <option value="expense"${param.type == 'expense' ? ' selected' : ''}>Expense</option>
            </select>
            <c:if test="${not empty errs.type}"><p class="text-xs text-error" role="alert"><c:out value="${errs.type}"/></p></c:if>
        </div>
    </c:if>
    <div class="grid gap-1.5">
        <label for="filter-category" class="text-sm font-medium text-ink">Category</label>
        <select id="filter-category" name="category" class="${input} ${empty errs.category ? ok : bad}">
            <option value="">All categories</option>
            <c:forEach var="category" items="${categories}">
                <option value="${category.id}"${('' += category.id) == param.category ? ' selected' : ''}><c:out value="${category.name}"/></option>
            </c:forEach>
        </select>
        <c:if test="${not empty errs.category}"><p class="text-xs text-error" role="alert"><c:out value="${errs.category}"/></p></c:if>
    </div>
    <div class="grid gap-1.5">
        <label for="field-q" class="text-sm font-medium text-ink">Notes contain</label>
        <input id="field-q" name="q" type="text" placeholder="Search notes..." value="${fn:escapeXml(param.q)}"
               class="${input} ${empty errs.q ? ok : bad}"<c:if test="${not empty errs.q}"> aria-invalid="true" aria-describedby="field-q-error"</c:if>>
        <c:if test="${not empty errs.q}"><p id="field-q-error" class="text-xs text-error" role="alert"><c:out value="${errs.q}"/></p></c:if>
    </div>
    <div class="flex items-center gap-2">
        <ui:button type="submit" cls="flex-1">Apply</ui:button>
        <ui:button variant="secondary" href="${action}" disabled="${not pageScope.filtered}">Reset</ui:button>
    </div>
</form>
