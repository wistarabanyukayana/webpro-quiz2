<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:error-page title="Too many attempts" message="You tried to log in too many times. Wait a minute, then try again." href="/login" linkText="Back to log in"/>
