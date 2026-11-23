# Spending Tracker

A web app to record income and expenses, group them by category, set monthly budgets and see monthly recaps. Amounts are in Indonesian rupiah, and dates follow Western Indonesia Time (WIB).

This is a Java/JSP port of our midterm project, [TsarVib/webpro-ets](https://github.com/TsarVib/webpro-ets), which was built with Laravel. It has the same features and the same database design. Built for Quiz 2 of EF234301 Web Programming (D), Informatics, Institut Teknologi Sepuluh Nopember.

**Demo login:** username `demo` (or `demo@example.com`), password `demo1234`. The app creates this account with three months of sample data on first start. Anyone can sign in to it, so do not store real data there.

## Team

| NRP        | Name                             | GitHub                                                       |
| ---------- | -------------------------------- | ------------------------------------------------------------ |
| 5025251020 | Firsto Al Kautsar Jagad Kurniaji | [TsarVib](https://github.com/TsarVib)                        |
| 5025251098 | Wistara Banyu Kayana             | [wistarabanyukayana](https://github.com/wistarabanyukayana) |

## Tech stack

| Layer    | Technology                                                       | Version |
| -------- | ---------------------------------------------------------------- | ------- |
| Language | Java                                                             | 21      |
| Server   | Apache Tomcat (Servlet 6.1, JSP 4.0)                             | 11      |
| Views    | JSP with JSTL and tag files                                      | 3.0     |
| Database | SQLite over JDBC (`sqlite-jdbc`)                                 | 3.54    |
| Styling  | Tailwind CSS with the Catppuccin palette                         | 4.3     |
| PDF      | OpenHTMLtoPDF                                                    | 1.1     |
| Build    | Maven (through the wrapper, `./mvnw`) and npm for the stylesheet | 3.10    |
| Tests    | JUnit with an embedded Tomcat                                    | 6.1     |

There is no web framework. Each page has a servlet (the controller), DAO classes run the SQL, and JSP views render the HTML.

## Features

**Authentication** (not counted toward the eight features)

- Register with username, full name, email and password.
- Log in with email or username, with a "remember me" option. After five failed attempts in a minute, the app makes the visitor wait.
- Log out. The app records the last login time on every sign-in.

**Application features**

1. **Dashboard**: current balance (initial balance + income − expense), this month's income and expense, and the latest transactions with filters.
2. **Category list**: the account's categories, grouped by income and expense.
3. **Create category**: the app rejects a duplicate active name of the same type.
4. **Rename category**: the same check. A category cannot change type while it has transactions.
5. **Delete category**: a soft delete, blocked while the category still has transactions.
6. **Create transaction**: category, amount, notes, date and time. The Income or Expense page sets the type, and the form offers only categories of that type.
7. **Transaction lists**: separate Income and Expense pages, 15 per page, newest first.
8. **Edit transaction**: the same form, filled in.
9. **Delete transaction**: after a confirmation dialog.
10. **Search and filter**: by date range, category, type and a keyword in the notes. The filters stay in the URL, so they survive a reload.
11. **Monthly recap**: one month, a range of months or a full year. Income, expense and net totals, and totals per category as bar or wheel charts.
12. **Profile settings**: change username, full name, email and initial balance. Change the password after entering the current one. Pick a theme accent and light or dark mode.
13. **Export**: CSV of the filtered income and expense lists, and the recap as CSV, a printable HTML report or a PDF.
14. **Delete account**: after entering the password. It removes the account and all its data.
15. **Budgets**: a spending limit per expense category per month. The bar turns yellow at 80% of the limit and red over it.

Income always shows in green with a `+` sign and expense in red with a `−` sign. These colors do not depend on the theme accent.

**Access control:** every query filters by the logged-in account, so one account cannot read or change another account's data. Guests go to the login page. A request for another account's record gets a 404, so the response does not reveal that the record exists. Every form carries a CSRF token. Changing the password logs out the account's other sessions.

## Database design

The data models are in [`docs/`](docs):

- Conceptual data model: [`docs/CDM.png`](docs/CDM.png)
- Physical data model: [`docs/PDM.png`](docs/PDM.png)
- DDL: [`docs/SpendingTracker_PDM_create.sql`](docs/SpendingTracker_PDM_create.sql)

| Table                | Holds                                                                         |
| -------------------- | ----------------------------------------------------------------------------- |
| `accounts`           | Users: username, full name, email, password hash, last login, initial balance |
| `default_categories` | Template categories that every new account gets a copy of                     |
| `categories`         | Income and expense categories per account (soft deleted)                      |
| `transactions`       | Income and expense entries: type, amount, notes, date, time                   |
| `budgets`            | Spending limit per account, expense category and month                        |

The database enforces the integrity rules itself, not only the app:

- **Foreign keys:** `categories`, `transactions` and `budgets` each reference `accounts.id` with `ON DELETE CASCADE`. A composite key `(categories_id, accounts_id) → categories (id, accounts_id)` on `transactions` and `budgets` makes sure a row only uses a category of the same account.
- **Checks:** `amount > 0`, `type IN ('income', 'expense')`, valid dates, times and months, and non-blank names.
- **Unique logins:** `accounts.username` and `accounts.email` each have a case-insensitive unique index.
- **Unique active category** per account, name and type (a partial index on `deleted_at IS NULL`).
- **One budget** per account, category and month.
- **Triggers:**
  - A new account gets the default categories.
  - `transactions.updated_at` updates itself.
  - A transaction's type must match its category's type.
  - A category's type is locked while it has transactions or budgets.
  - A budget must use an expense category.
  - Deleting a category also deletes its budgets.

The app creates the schema on first start and tracks its version in SQLite's `user_version`. A database from an older version gets the budgets table on the next start.

## Running locally

Requirements: Docker with Compose.

```bash
git clone https://github.com/wistarabanyukayana/webpro-quiz2.git
cd webpro-quiz2
docker compose up --build
```

Open http://localhost:8080 and log in with the demo account, or register a new one. New accounts start with the default income and expense categories. The database is in the `tracker-data` volume, so it survives a restart.

To run without Docker, you need Java 21, Node.js 24 and Tomcat 11:

1. Run `npm ci` to install Tailwind.
2. Run `npm run build:css` to build the stylesheet.
3. Run `./mvnw package -DskipTests` to build `target/spending-tracker.war`.
4. Copy the WAR to Tomcat's `webapps/ROOT.war` and start Tomcat.

The app stores its data in `data/tracker.db` under Tomcat's working directory. Set the `DB_PATH` environment variable to use another file.

## Testing

```bash
./mvnw test                               # 98 tests
scripts/smoke.sh http://localhost:8080    # against a running app
```

The tests start an embedded Tomcat with a fresh database and send real HTTP requests. They cover authentication, the dashboard, categories, transactions, filters, pagination, the recap and its exports, settings, budgets, account isolation and the database constraints and cascades. The smoke script registers a new account and goes through the main pages, forms and exports with `curl`.

GitHub Actions runs `./mvnw test` on every push to `main` and on every pull request.

## Project structure

```text
src/main/java/id/its/tracker/
  controller/       Servlets: Auth, Dashboard, Category, Transaction, Budget, Recap, Settings
  dao/              SQL for accounts, categories, transactions and budgets
  database/         Connection, schema version, seeders (default categories, demo account)
  filter/           Login check, CSRF token, security headers
  service/          Validation and business rules
src/main/resources/database/   schema.sql (the PDM export), extra.sql, budgets.sql
src/main/webapp/WEB-INF/
  views/            JSP pages
  tags/             Layout and form components as tag files
src/main/css/       Tailwind source
src/test/java/      JUnit tests
scripts/smoke.sh    Smoke test
docs/               Data models and the build plan
```
