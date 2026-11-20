<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<t:guest title="Log in">
    <form method="post" action="/login" class="grid w-full max-w-sm justify-items-stretch gap-6" novalidate>
        <input type="hidden" name="_token" value="${sessionScope.csrf}">
        <div class="grid gap-1.5">
            <h1 class="mt-1 text-2xl font-semibold tracking-tight text-ink">Log in</h1>
            <p class="text-sm text-muted">Enter your credentials to access your account.</p>
        </div>

        <div class="grid gap-4 rounded-xl border border-border bg-surface p-6 shadow-sm">
            <ui:text-field name="login" label="Email or username" autocomplete="username"
                     placeholder="Enter your email or username..." required="true"/>
            <ui:text-field name="password" label="Password" type="password" autocomplete="current-password"
                     placeholder="Enter your password..." required="true"/>
            <label class="flex items-center gap-2 text-sm text-muted">
                <input type="checkbox" name="remember" value="1"
                       class="size-4 rounded border-border text-primary accent-primary focus:ring-2 focus:ring-ring">
                Remember me
            </label>
            <button type="submit" data-busy-text="Logging in..." class="inline-flex h-10 items-center justify-center gap-2 rounded-lg px-4 text-sm font-medium transition-colors duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring disabled:cursor-not-allowed disabled:opacity-50 bg-primary text-primary-ink hover:bg-primary-hover">Log in</button>
        </div>

        <p class="text-center text-sm text-muted">
            Don't have an account yet?
            <a href="/register" class="font-medium text-primary underline underline-offset-2 hover:text-primary-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring">Create one</a>
        </p>
    </form>
</t:guest>
