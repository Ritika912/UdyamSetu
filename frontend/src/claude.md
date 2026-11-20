# UdyamSetu – Frontend Development Instructions

## 1. Project Overview

UdyamSetu is a Business Growth Platform designed for small business owners.

Tagline: "Every enquiry tells a story. Turn it into an opportunity."

The platform helps small businesses:
- Capture and manage customer enquiries.
- Maintain customer records.
- Manage offered services.
- Identify customer demand and unfulfilled service requests.
- Discover business growth opportunities through actionable insights.

The application should be simple enough for non-technical business owners.

## 2. Existing Technology Stack

Frontend:
- React 19
- Vite
- JavaScript
- CSS
- Lucide React
- React Router
- Native Fetch API

Backend:
- Java 21
- Spring Boot
- Spring Data JPA
- MySQL

The backend is maintained in a separate project.

## 3. Core Frontend Sections

The application has four main sections:

1. Dashboard
2. Enquiry Management
3. Customers & Services
4. Business Insights & Opportunities

Expected routes:
- `/` – Public landing page
- `/login` – Owner sign-in (public)
- `/dashboard` – Dashboard (protected)
- `/enquiries` – Enquiries (protected)
- `/customers` – Customers (protected)
- `/services` – Services (protected)
- `/insights` – Business Insights & Opportunities (protected)

## 4. Architecture Guidelines

- Use reusable React components.
- Keep page components separate from shared layout components.
- Keep API calls inside the `src/api/` layer.
- Keep shared formatting utilities inside `src/utils/`.
- Avoid unnecessarily large components.
- Use React state and hooks appropriately.
- Keep frontend and backend responsibilities separate.
- Prefer simple architecture over unnecessary abstraction.

## 5. Backend Integration Rules

- Use the existing backend REST APIs.
- Use the centralized API client in `src/api/client.js`.
- Use the Vite `/api` proxy during development.
- Do not hardcode backend response data.
- Do not invent endpoint URLs, request fields or response structures.
- Inspect the actual backend controllers and DTOs before integration.
- Handle loading, empty, success and error states.
- Display backend validation errors clearly.
- Preserve backend enum values when sending requests.

Backend API base path during development:
`/api`

## 6. Business Rules

Enquiry statuses:

- NEW → New
- IN_PROGRESS → In Progress
- PENDING → Pending
- CONVERTED → Converted
- UNFULFILLED → Service unavailable
- LOST → Customer did not proceed

Important distinctions:
- UNFULFILLED means the business could not provide the requested service.
- LOST means the customer did not proceed for another reason.

Enquiry requirements:
- Customer name and contact number are mandatory.
- Other enquiry fields are optional unless backend validation specifies otherwise.
- A customer can be associated with multiple enquiries.
- An enquiry can use an existing catalogue service or free-text service.
- Free-text services must not automatically create catalogue entries.

Service requirements:
- Inactive services must not be selectable for new enquiries.
- Service deactivation is soft deletion.
- Historical enquiry relationships must remain intact.

## 7. UI/UX Guidelines

- Keep the existing dashboard design and visual identity.
- Maintain consistent styling across all pages.
- Make the application responsive for desktop and mobile.
- Use clear labels and simple language.
- Avoid unnecessary CRM terminology.
- Provide useful empty states.
- Provide clear loading and error states.
- Use accessible form labels and validation messages.
- Avoid unnecessary animations and decorative elements.

## 8. Coding Standards

- Write readable and maintainable code.
- Use meaningful component, variable and function names.
- Avoid duplicating code.
- Extract reusable components when appropriate.
- Avoid unnecessary dependencies.
- Do not introduce Redux, React Query, Axios or additional UI libraries without approval.
- Use native Fetch API through the existing API client.
- Do not suppress ESLint warnings just to make checks pass.

## 9. Critical Safety Rules

- Never delete or overwrite existing work without approval.
- Never rebuild an existing feature without inspecting it first.
- Never modify backend files unless explicitly approved.
- Never modify `application.yaml` or database configuration.
- Never start the backend against `udyamsetu_db` without explicit approval and schema safety verification.
- Never execute destructive SQL.
- Do not change existing database records.
- Do not introduce unnecessary backend changes to solve frontend problems.

## 10. Development Workflow

For every task:

1. Read this CLAUDE.md.
2. Inspect the relevant existing files.
3. Understand the existing implementation.
4. Identify dependencies and potential risks.
5. Make only the changes required for the requested task.
6. Preserve existing functionality.
7. Run ESLint.
8. Run the Vite production build.
9. Report created, modified and deleted files.
10. Report test results and remaining issues.

Work incrementally. Do not implement multiple unrelated features in one task.

If a requirement is unclear or a backend contract is missing, investigate and report it before making assumptions.

## 11. Current Development Priority

The immediate goal is to complete frontend development and integrate the existing tested backend.

Prioritize:

1. Application routing and shared layout.
2. Reusable frontend components.
3. Dashboard API integration.
4. Enquiry management.
5. Customer management.
6. Service management.
7. Business Insights and Opportunities.
8. End-to-end testing.
9. Final UI polish and hackathon demo preparation.

Do not add unrelated features before completing the core user journey.

## 12. Product Quality Goal

The final application must support this complete workflow:

1. Business owner opens the dashboard.
2. Owner captures a customer enquiry.
3. Enquiry is saved through the backend.
4. Owner tracks the enquiry and updates its status.
5. Unfulfilled service requests are recorded.
6. Business Insights identifies demand patterns.
7. Opportunity Insights presents explainable business recommendations.

The objective is a working, reliable Business Growth Platform, not merely a visually attractive dashboard.