# 💰 SmartLedger AI — Personal Finance Manager

A production-ready full-stack FinTech application built with Java Spring Boot, React TypeScript, and AI-powered financial insights using Llama 3.3.

## 🚀 Tech Stack

**Backend**
- Java 17 + Spring Boot 3.2
- Spring Security + JWT Authentication
- Spring Data JPA + Hibernate + MySQL
- Groq AI (Llama 3.3) Integration
- iText7 PDF Report Generation
- RESTful APIs

**Frontend**
- React 18 + TypeScript
- React Router v6
- Recharts (Data Visualization)
- Axios (HTTP Client)

## ✨ Features

- 🔐 JWT Authentication — Secure login/register with BCrypt encryption
- 💸 Transaction Tracking — Add income/expense with categories and filters
- 📊 Dashboard Charts — Visual spending insights with pie and bar charts
- 🎯 Budget Management — Set monthly limits per category with progress tracking
- 🤖 AI Financial Advisor — Chat with Llama 3.3 about your finances
- 📄 PDF Reports — Download monthly financial summary reports
- 🔒 Secure APIs — All endpoints protected with JWT

## 📡 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/register | Register new user |
| POST | /api/auth/login | Login user |
| GET | /api/transactions | Get all transactions |
| POST | /api/transactions | Add transaction |
| DELETE | /api/transactions/{id} | Delete transaction |
| GET | /api/transactions/summary | Get financial summary |
| POST | /api/budgets | Set budget |
| GET | /api/budgets/status | Get budget status |
| POST | /api/ai/chat | AI financial advice |
| GET | /api/reports/pdf | Download PDF report |

## 🏃 Quick Start

### Prerequisites
- Java 17+
- Node 18+
- MySQL 8+
- Groq API Key (free at console.groq.com)

### Backend Setup
```bash
git clone https://github.com/adwitiya584/smartledger-ai.git
cd smartledger-ai
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Fill in your DB password and API keys
mvn spring-boot:run
```

### Frontend Setup
```bash
git clone https://github.com/adwitiya584/smartledger-ai-frontend.git
cd smartledger-ai-frontend
npm install
npm start
```

## 🔧 Environment Variables

Copy `application.properties.example` to `application.properties` and fill:

```properties
spring.datasource.password=YOUR_MYSQL_PASSWORD
app.jwt.secret=YOUR_SECRET_KEY
app.groq.api-key=YOUR_GROQ_API_KEY
```

## 👨‍💻 Author

**Mohit Pandey**
- 📧 mohitppandey098@gmail.com
- 💼 Ex-Mentor at Coding Ninjas | Ex-Developer at LuckPay Solutions
- 🔗 [GitHub](https://github.com/adwitiya584)

## 📝 License

MIT License — feel free to use this project for learning and portfolio purposes.
