<%-- has to stay valid XHTML for the PDF --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="tr" uri="tracker" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <title><c:out value="${'Recap report: ' += periodLabel}"/></title>
    <style>
        body { color: #303446; font-family: Arial, Helvetica, sans-serif; font-size: 12px; margin: 32px; }
        h1 { margin-bottom: 4px; }
        h2 { border-bottom: 1px solid #bcc0cc; margin-top: 24px; padding-bottom: 6px; }
        .muted { color: #5c5f77; }
        .summary { display: table; margin: 18px 0; width: 100%; }
        .summary div { display: table-cell; padding: 10px; width: 33%; }
        .income { color: #2b7a1e; }
        .expense { color: #b00c30; }
        .summary .income-box { border-left: 4px solid #2b7a1e; }
        .summary .expense-box { border-left: 4px solid #b00c30; }
        h2.income { border-bottom-color: #2b7a1e; }
        h2.expense { border-bottom-color: #b00c30; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border-bottom: 1px solid #d5d8e0; padding: 7px; text-align: left; }
        th:last-child, td:last-child { text-align: right; }
        .category-grid { display: table; width: 100%; }
        .category-list { display: table-cell; padding-right: 24px; vertical-align: top; width: 50%; }
    </style>
</head>
<body>
    <h1>Financial recap</h1>
    <p class="muted"><c:out value="${periodLabel}"/></p>

    <div class="summary">
        <div class="income-box"><strong>Income</strong><br/><span class="income">+${tr:money(income)}</span></div>
        <div class="expense-box"><strong>Expense</strong><br/><span class="expense">-${tr:money(expense)}</span></div>
        <div><strong>Net change</strong><br/><span class="${net lt 0 ? 'expense' : 'income'}">${net gt 0 ? '+' : ''}${tr:money(net)}</span></div>
    </div>

    <div class="category-grid">
        <c:forEach var="group" items="${['income|Income by category', 'expense|Expense by category']}">
            <c:set var="parts" value="${group.split('[|]')}"/>
            <div class="category-list">
                <h2 class="${parts[0]}">${parts[1]}</h2>
                <table>
                    <c:forEach var="item" items="${parts[0] == 'income' ? incomeTotals : expenseTotals}">
                        <tr>
                            <td><c:out value="${item.category}"/></td>
                            <td class="${parts[0]}">${parts[0] == 'income' ? '+' : '-'}${tr:money(item.total)}</td>
                        </tr>
                    </c:forEach>
                </table>
            </div>
        </c:forEach>
    </div>

    <h2>Transactions</h2>
    <table>
        <thead>
            <tr><th>Date</th><th>Time</th><th>Type</th><th>Category</th><th>Amount</th><th>Notes</th></tr>
        </thead>
        <tbody>
            <c:forEach var="row" items="${rows}">
                <tr>
                    <td>${row.date}</td>
                    <td>${empty row.time ? '-' : row.time}</td>
                    <td class="${row.type}">${row.type == 'income' ? 'Income' : 'Expense'}</td>
                    <td><c:out value="${empty row.category ? 'Uncategorized' : row.category}"/></td>
                    <td class="${row.type}">${row.type == 'income' ? '+' : '-'}${tr:money(row.amount)}</td>
                    <td><c:out value="${empty row.notes ? '-' : row.notes}"/></td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</body>
</html>
