<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="prefix" required="true" %>
<%@ attribute name="values" type="java.util.Map" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<c:set var="entry" value="${prefix == 'edit' ? editing : null}"/>
<c:choose>
    <c:when test="${not empty pageScope.values}">
        <c:set var="v" value="${pageScope.values}"/>
    </c:when>
    <c:when test="${not empty entry}">
        <c:set var="v" value="${{'amount': '' += entry.amount, 'category_id': '' += entry.categoryId, 'date': entry.date,
                                 'time': empty entry.time ? '' : fn:substring(entry.time, 0, 5), 'notes': entry.notes}}"/>
    </c:when>
    <c:otherwise>
        <c:set var="v" value="${{'date': today, 'time': nowTime}}"/>
    </c:otherwise>
</c:choose>
<input type="hidden" name="_token" value="${sessionScope.csrf}">
<input type="hidden" name="back" value="${fn:escapeXml(backUrl)}">
<input type="hidden" name="type" value="${type}">
<c:if test="${not empty errors.type and not empty pageScope.values}">
    <p class="text-xs text-error" role="alert"><c:out value="${errors.type}"/></p>
</c:if>
<ui:text-field name="amount" id="${prefix}-amount" label="Amount" type="number" min="1" step="1" placeholder="0" required="true" value="${empty v.amount ? '' : v.amount}"/>
<ui:select-field name="category_id" id="${prefix}-category" label="Category" required="true">
    <option value="">Choose a category</option>
    <c:forEach var="category" items="${categories}">
        <option value="${category.id}"${('' += category.id) == v.category_id ? ' selected' : ''}><c:out value="${category.name}"/></option>
    </c:forEach>
</ui:select-field>
<div class="grid gap-4 sm:grid-cols-2">
    <ui:text-field name="date" id="${prefix}-date" label="Date" type="date" required="true" value="${empty v.date ? '' : v.date}"/>
    <ui:text-field name="time" id="${prefix}-time" label="Time" type="time" value="${empty v.time ? '' : v.time}"/>
</div>
<ui:text-area-field name="notes" id="${prefix}-notes" label="Notes" placeholder="What was this for?" value="${empty v.notes ? '' : v.notes}"/>
