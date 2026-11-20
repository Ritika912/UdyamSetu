# UdyamSetu – Simplifying Business Growth for Small Businesses

**One platform to manage customers, enquiries, services, and business growth.**

[![Live Demo](https://img.shields.io/badge/Live%20Demo-getudyamsetu.com-blue)](https://getudyamsetu.com)

**Live Application:** https://getudyamsetu.com

---

## 1. About UdyamSetu

UdyamSetu is a business management and growth platform designed to help small business owners organize their day-to-day business activities in one place.

Small businesses often manage customer information, enquiries, follow-ups, and service requests through disconnected tools, spreadsheets, messaging applications, or manual processes. This makes it difficult to track business activities, maintain customer relationships, and understand overall business performance.

UdyamSetu aims to simplify these operations by providing a centralized platform where business owners can manage customers, track enquiries, monitor services, and gain useful insights into their business.

Our goal is simple:

**Help small businesses spend less time managing scattered information and more time growing their business.**

## 2. The Problem We Identified

Small businesses and independent entrepreneurs play an important role in the economy, but many face operational challenges as they grow.

Some common challenges include:

- **Scattered customer information:** Customer details are maintained across spreadsheets, notebooks, messages, and different applications.
- **Missed enquiries and follow-ups:** Without a structured tracking system, potential customers and business opportunities can be overlooked.
- **Lack of centralized management:** Business owners often use multiple tools to manage customers, enquiries, and services.
- **Limited business visibility:** It can be difficult to understand enquiry status, customer activity, and overall business progress.
- **Time-consuming manual work:** Repetitive administrative tasks take time away from customer engagement and business development.

These challenges can affect productivity, customer experience, and business growth.

## 3. How We Came Up With the Solution

During our problem-discovery and market-research process, we focused on understanding the operational challenges faced by small businesses, entrepreneurs, and independent service providers.

Rather than building another complex enterprise management system, we wanted to identify a practical problem that affects everyday business operations.

We identified an opportunity to simplify the way small businesses manage their customers, enquiries, and service-related activities.

We approached the solution with three principles:

1. **Simplicity:** Make business management easier without requiring complicated workflows.
2. **Centralization:** Bring important business activities together in one platform.
3. **Visibility:** Help business owners understand what is happening in their business through organized information and insights.

This led to the idea of UdyamSetu – a centralized business growth platform built around the everyday needs of small business owners.

## 4. Our Solution

UdyamSetu provides a unified workspace for business owners to manage important business operations.

Instead of switching between multiple tools, business owners can use one platform to:

- View their business dashboard.
- Manage customer information.
- Track and organize incoming enquiries.
- Monitor services and their statuses.
- Review business insights.
- Stay informed about their business activities.

The platform is designed to provide a simple and organized experience while supporting better customer management and business decisions.

## 5. Key Features

### Business Dashboard
A centralized overview of business activities that helps owners understand their current business operations.

### Customer Management
Manage customer information in one place, making it easier to maintain organized customer records and relationships.

### Enquiry Management
Track incoming business enquiries and monitor their progress so potential opportunities are easier to manage.

### Service Management
View and manage available services and monitor their respective statuses.

### Business Insights
Access business-related insights to better understand activity and make more informed decisions.

### Business Owner Authentication
A login system that provides business owners access to their management workspace.

### Public Landing Page
Introduces the platform and explains its value to small businesses and prospective users.

### Contact and Enquiry Submission
Allows visitors to submit enquiries through the platform.

---

## 6. Real-World Use Cases

UdyamSetu can support different types of small businesses and independent professionals.

| Business type | How UdyamSetu helps |
|---|---|
| Small retail businesses | Organize customer records and track customer enquiries. |
| Service providers | Manage service-related activities and monitor statuses. |
| Consultants | Keep track of prospective clients and incoming enquiries. |
| Freelancers | Organize customer information and manage business opportunities. |
| Small agencies | Maintain visibility into customers, enquiries, and services. |
| Entrepreneurs | Get a centralized view of important business activities. |

**Example scenario:**

Imagine a small service business receiving enquiries from customers through its website and other channels.

Without a centralized system, the owner may need to manually maintain customer details, remember follow-ups, and track service requests.

With UdyamSetu, the business owner can use a single workspace to manage customer information, track enquiries, monitor services, and review business activity.

This helps create a more organized business management process.

## 7. Technology Stack

The application uses the following technologies:

| Layer | Technologies |
|---|---|
| Frontend | React, JavaScript, Vite, HTML, CSS |
| Backend | Java, Spring Boot |
| Database | MySQL |
| Containerization | Docker |
| Web Server / Reverse Proxy | Nginx |
| Build and Dependency Management | Maven, npm |
| Version Control | Git and GitHub |

## 8. Application Architecture

UdyamSetu follows a frontend-backend architecture.

```text
                 Business Owner
                        |
                        v
               React Frontend
                        |
                  REST APIs
                        |
                        v
              Spring Boot Backend
                        |
                        v
                    MySQL
```

The frontend provides the user interface and communicates with the Spring Boot backend through APIs.

The backend handles application logic, authentication, and data operations, while MySQL stores application data.

Docker and Nginx are used as part of the deployment setup.

## 9. Repository Structure

```text
UdyamSetu/
│
├── backend/
│   └── Spring Boot application
│
├── frontend/
│   └── React application
│
├── deployment/
│   ├── db/
│   ├── env/
│   ├── frontend/
│   └── docker-compose.yml
│
├── .gitignore
└── README.md
```

## 10. Live Demo and Judge Access

The application is deployed and accessible online.

**Live Application:** https://getudyamsetu.com

Judges can use the following demo business-owner account to explore the application.

| Credential | Value |
|---|---|
| Login URL | https://getudyamsetu.com |
| Username / Email | `BusinessOwner` |
| Password | `BusinessOwner` |
| Login Role | Business Owner |

**Instructions for judges:**

1. Open https://getudyamsetu.com.
2. Navigate to the login page.
3. Enter the demo business-owner credentials provided above.
4. Explore the dashboard, customers, enquiries, services, and insights sections.

*Note: These credentials are intended only for evaluating the demo application. Please do not use them for personal or production activities.*

## 11. Deployment

UdyamSetu is deployed on a cloud-hosted server and is accessible through its public domain.

The deployment setup uses:

- Docker for containerized application deployment.
- Docker Compose for managing application services.
- Nginx for frontend delivery and reverse proxy configuration.
- MySQL for persistent application data.
- Environment-based configuration for sensitive deployment settings.

**Production URL:** https://getudyamsetu.com

## 12. Getting Started – Local Development

To run the application locally, you need:

- Java and Maven
- Node.js and npm
- MySQL
- Git

Clone the repository:

```bash
git clone https://github.com/Ritika912/UdyamSetu.git
cd UdyamSetu
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Backend

```bash
cd backend
```

Configure the required local database and application properties using the provided example configuration files.

Then run:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

For production deployment, refer to the Docker Compose configuration and environment examples under the `deployment/` directory.

## 13. Project Impact

UdyamSetu is built around the idea that small businesses deserve accessible and organized business management tools.

The platform aims to:

- Reduce dependence on scattered records and manual tracking.
- Improve visibility into customer enquiries.
- Make customer information easier to manage.
- Simplify service-related operations.
- Support more organized business decisions.
- Give small business owners a centralized digital workspace.

By bringing essential business activities together, UdyamSetu aims to make everyday business management simpler and more structured.

## 14. Future Scope

Some potential areas for future development include:

- Automated customer follow-up reminders.
- WhatsApp and email notifications.
- Advanced business analytics and reporting.
- AI-powered business recommendations.
- Lead prioritization and opportunity tracking.
- Multi-user access with role-based permissions.
- Integration with accounting and payment platforms.

These enhancements can help UdyamSetu evolve into a more comprehensive business growth ecosystem.

## 15. Hackathon Project

**Project Name:** UdyamSetu  
**Category:** Business Management / Business Growth Platform  
**Target Users:** Small Businesses, Entrepreneurs, Freelancers, and Service Providers  
**Live Demo:** https://getudyamsetu.com  
**GitHub Repository:** https://github.com/Ritika912/UdyamSetu

---

## Our Vision

We believe business growth should not be limited by the complexity of managing everyday operations.

UdyamSetu aims to bridge the gap between small businesses and organized digital business management.

**UdyamSetu – Connecting Small Businesses to Smarter Growth.**
