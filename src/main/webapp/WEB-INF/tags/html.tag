<%@ tag body-content="scriptless" pageEncoding="UTF-8" %>
<%@ attribute name="title" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${title}"/></title>
    <link rel="icon" href="/assets/favicon.svg" type="image/svg+xml">
    <script src="/assets/theme.js"></script>
    <link rel="stylesheet" href="/assets/app.css">
    <script src="/assets/app.js" defer></script>
</head>
<body>
<jsp:doBody/>
<dialog id="confirm-dialog" class="m-auto w-[calc(100%-2rem)] max-w-sm rounded-xl border border-border bg-surface p-6 text-ink shadow-xl">
    <form method="dialog" class="grid gap-5">
        <p class="text-sm" data-confirm-message></p>
        <div class="flex justify-end gap-2">
            <button value="cancel" class="inline-flex h-10 items-center justify-center rounded-lg border border-border bg-surface px-4 text-sm font-medium text-ink transition-colors hover:bg-background focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring">Cancel</button>
            <button value="ok" class="inline-flex h-10 items-center justify-center rounded-lg bg-error px-4 text-sm font-medium text-primary-ink transition-colors hover:opacity-90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring">Delete</button>
        </div>
    </form>
</dialog>
</body>
</html>
