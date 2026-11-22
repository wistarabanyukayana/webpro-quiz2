<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<c:set var="card" value="grid gap-4 rounded-xl border border-border bg-surface p-6"/>
<t:app title="Settings">
    <main class="grid w-full flex-1 content-start gap-6 overflow-y-auto p-6 lg:p-8">
        <div class="grid gap-1">
            <h1 class="text-2xl font-semibold tracking-tight text-ink">Settings</h1>
            <p class="text-sm text-muted">Account details, appearance, and password.</p>
        </div>

        <form method="post" action="/settings" class="${card}">
            <input type="hidden" name="_token" value="${sessionScope.csrf}">
            <input type="hidden" name="_method" value="PATCH">
            <div class="grid gap-1">
                <h2 class="text-sm font-medium text-ink">Profile</h2>
                <p class="text-xs text-muted">Used for signing in and shown in the app.</p>
            </div>
            <div class="grid gap-4 sm:grid-cols-2">
                <ui:text-field name="username" label="Username" autocomplete="username" required="true"/>
                <ui:text-field name="full_name" label="Full name" autocomplete="name" required="true"/>
                <ui:text-field name="email" label="Email" type="email" autocomplete="email" required="true"/>
                <ui:text-field name="initial_balance" label="Initial balance" placeholder="0" required="true"/>
            </div>
            <div class="flex items-center gap-3">
                <ui:button type="submit" busy="Saving...">Save profile</ui:button>
                <c:if test="${saved == 'profile'}"><span class="text-xs text-primary" role="status">Saved</span></c:if>
            </div>
        </form>

        <section class="${card}">
            <div class="grid gap-1">
                <h2 class="text-sm font-medium text-ink">Accent color</h2>
                <p class="text-xs text-muted">Catppuccin palette, saved in this browser. Current: <span data-accent-label>Green</span></p>
            </div>
            <div class="flex flex-wrap gap-3" role="radiogroup" aria-label="Accent color">
                <c:forEach var="accent" items="${['rosewater', 'flamingo', 'pink', 'mauve', 'red', 'maroon', 'peach', 'yellow', 'green', 'teal', 'sky', 'sapphire', 'blue', 'lavender']}">
                    <c:set var="accentLabel" value="${accent.substring(0, 1).toUpperCase() += accent.substring(1)}"/>
                    <label class="relative" title="${accentLabel}">
                        <input type="radio" name="accent" value="${accent}" data-accent-option data-label="${accentLabel}"
                               aria-label="${'Use ' += accentLabel += ' accent'}" class="peer sr-only"${accent == 'green' ? ' checked' : ''}>
                        <span class="block size-8 cursor-pointer rounded-full border border-black/10 transition-transform hover:scale-110 peer-checked:ring-2 peer-checked:ring-ink peer-checked:ring-offset-2 peer-checked:ring-offset-surface peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-ring dark:border-white/10"
                              style="background-color: var(--swatch-${accent})" aria-hidden="true"></span>
                    </label>
                </c:forEach>
            </div>
        </section>

        <form method="post" action="/settings/password" class="${card}">
            <input type="hidden" name="_token" value="${sessionScope.csrf}">
            <input type="hidden" name="_method" value="PUT">
            <div class="grid gap-1">
                <h2 class="text-sm font-medium text-ink">Password</h2>
                <p class="text-xs text-muted">Requires your current password.</p>
            </div>
            <div class="grid gap-4 sm:grid-cols-2">
                <ui:text-field name="current_password" label="Current password" type="password" autocomplete="current-password" required="true"/>
                <div class="hidden sm:block"></div>
                <ui:text-field name="password" label="New password" type="password" autocomplete="new-password" required="true"/>
                <ui:text-field name="password_confirmation" label="Confirm new password" type="password" autocomplete="new-password" required="true"/>
            </div>
            <div class="flex items-center gap-3">
                <ui:button type="submit" busy="Updating...">Update password</ui:button>
                <c:if test="${saved == 'password'}"><span class="text-xs text-primary" role="status">Updated</span></c:if>
            </div>
        </form>
    </main>
</t:app>
