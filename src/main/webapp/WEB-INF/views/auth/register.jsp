<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<t:guest title="Register">
    <form method="post" action="/register" class="grid w-full max-w-sm justify-items-stretch gap-6" novalidate>
        <input type="hidden" name="_token" value="${sessionScope.csrf}">
        <div class="grid gap-1.5">
            <h1 class="mt-1 text-2xl font-semibold tracking-tight text-ink">Register</h1>
            <p class="text-sm text-muted">Create an account to get started.</p>
        </div>

        <div class="grid gap-4 rounded-xl border border-border bg-surface p-6 shadow-sm">
            <ui:text-field name="username" label="Username" autocomplete="username"
                     placeholder="Create a username..." required="true"/>
            <ui:text-field name="full_name" label="Full name" autocomplete="name"
                     placeholder="Enter your full name..." required="true"/>
            <ui:text-field name="email" label="Email" type="email" autocomplete="email"
                     placeholder="Enter your email..." required="true"/>
            <ui:text-field name="password" label="Password" type="password" autocomplete="new-password"
                     placeholder="Create a password..." required="true"/>
            <ui:text-field name="password_confirmation" label="Confirm password" type="password" autocomplete="new-password"
                     placeholder="Confirm your password..." required="true"/>
            <p class="text-xs text-muted">At least 12 characters, with upper and lower case letters, a number and a symbol.</p>
            <button type="submit" data-busy-text="Creating account..." class="inline-flex h-10 items-center justify-center gap-2 rounded-lg px-4 text-sm font-medium transition-colors duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring disabled:cursor-not-allowed disabled:opacity-50 bg-primary text-primary-ink hover:bg-primary-hover">Create account</button>
        </div>

        <p class="text-center text-sm text-muted">
            Already have an account?
            <a href="/login" class="font-medium text-primary underline underline-offset-2 hover:text-primary-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring">Log in</a>
        </p>
    </form>
</t:guest>
