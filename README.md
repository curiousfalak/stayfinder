# 🏠 StayFinder

### A Property Rental Backend — Built to Handle Real-World Booking Problems

---

## 📌 What Is This Project?

StayFinder is a backend system for a **property rental platform** — similar to how Airbnb or MakeMyTrip works behind the scenes.

It handles everything from user registration and property listing to searching, booking, and sending confirmation emails.

We built this as our final year major project to understand how real-world booking applications actually work — not just theory, but actual working code.

---

## 💡 Why Did We Build This?

We noticed something interesting: apps like Airbnb look simple on the outside, but the backend is surprisingly complicated.

For example:
- What if two people try to book the same property on the same dates at the same time?
- How do you search through thousands of properties and get results in seconds?
- How do you make sure only logged-in users can book a property?

These are real problems that real companies solve every day. We wanted to build something that actually solves them — not just a CRUD app, but a **proper backend** with real features.

---

## 👥 Who Can Use This Platform?

There are three types of users:

| User Type | Who They Are | What They Can Do |
|-----------|-------------|-----------------|
| **HOST** | Property owners | List properties, upload photos, confirm or cancel bookings |
| **GUEST** | Travellers | Search properties, book stays, leave reviews |
| **ADMIN** | Platform manager | Monitor platform data through a dashboard |

---

## ✨ Features (Explained Simply)

---

### 🔐 Secure Login and Registration

**What it does:** Users can create an account and log in safely. Every user is assigned a role — either HOST or GUEST.

**Why it is useful:** Without login, anyone could book properties or delete listings. This keeps everything secure.

**Simple example:** Like how you log into Zomato before placing an order — without login, you cannot do anything important.

---

### 🏠 Property Listing (for Hosts)

**What it does:** Hosts can add their property to the platform — with a title, description, location, price per night, and how many guests it can hold.

**Why it is useful:** Before any guest can book, a host needs to list their place. This is the starting point of everything.

**Simple example:** Like a seller listing a product on Amazon before buyers can purchase it.

---

### 📸 Photo Upload for Properties

**What it does:** Hosts can upload multiple photos of their property. The photos are stored safely and anyone can view them via a link.

**Why it is useful:** Properties with good photos get more bookings. Photos cannot be stored in a regular database — they need a dedicated file storage system.

**Simple example:** Like how Instagram stores your photos separately from your account information.

---

### 🔍 Property Search

**What it does:** Guests can search for properties by city, price range, number of guests, and keywords like "pool" or "sea view". Results appear very quickly.

**Why it is useful:** If you have 10,000 properties in the database, searching through them one by one is very slow. We use a special search engine that makes this fast.

**Simple example:** Like how Google finds results instantly even though there are billions of websites.

---

### 📅 Booking a Property

**What it does:** Guests select check-in and check-out dates. The system automatically calculates the total price and creates a booking.

**Why it is useful:** The system also checks if those dates are already booked by someone else — so no two people can book the same property for the same dates.

**Simple example:** Like booking a movie ticket — once someone books a seat, it becomes unavailable for everyone else.

---

### ⏱️ 30-Second Booking Hold

**What it does:** When a guest starts the booking process, the system temporarily reserves those dates for 30 seconds — just for that guest.

**Why it is useful:** This prevents two users from booking the same dates at the exact same moment. The first person gets a fair chance to complete their booking.

**Simple example:** Like a 10-minute timer when you are buying an IRCTC train ticket — your seat is held while you fill in the payment details.

---

### ✅ Host Confirmation

**What it does:** After a guest books, the booking starts in a "Pending" state. The host reviews it and either confirms or cancels it.

**Why it is useful:** Hosts have control over who stays at their property. They are not forced to accept every booking automatically.

**Simple example:** Like a job application — you apply (guest books), then the company decides whether to accept you (host confirms).

---

### 📧 Email Notifications

**What it does:** Guests receive an email when their booking is received, and another email when the host confirms it.

**Why it is useful:** Without notifications, users have to keep refreshing the app to check their booking status. Emails keep users informed automatically.

**Simple example:** Like how Swiggy sends you an order confirmation email after you place an order.

---

### ⭐ Reviews and Ratings

**What it does:** After completing a stay, guests can leave a rating (1 to 5 stars) and a written comment. The property's average rating is updated automatically.

**Why it is useful:** Reviews help future guests make better decisions. Properties with higher ratings appear more prominently in search results.

**Simple example:** Like reviewing a restaurant on Zomato after your meal.

---

### ⚡ Faster Loading with Caching

**What it does:** When someone views a popular property, the result is saved temporarily. The next person who views the same property gets the result much faster — without hitting the database again.

**Why it is useful:** Under high traffic, the same popular properties are viewed many times per minute. Without caching, the server would slow down.

**Simple example:** Like how YouTube buffers a video — once a part is loaded, it does not download again.

---

### 📊 Analytics Dashboard

**What it does:** The platform admin can see a visual dashboard showing how many properties are listed per city, price ranges, and booking trends.

**Why it is useful:** It helps the platform team understand which cities are most popular and where to focus their marketing.

**Simple example:** Like how a shop owner checks which products are selling the most to decide what to restock.

---

## 🔄 How the App Works — Step by Step

```
User opens StayFinder
         ↓
Register as HOST or GUEST
         ↓
Login → Get a secure access token
         ↓
  ┌──────────────────┐
  │                  │
HOST side        GUEST side
  │                  │
List property    Search for property
Upload photos    (city, price, guests)
  │                  │
  │            Select dates
  │                  ↓
  │            30-second hold placed
  │                  ↓
  │            Booking created (Pending)
  │                  ↓
  └──── Host confirms booking ──────┐
                                    ↓
                        Guest gets confirmation email
                                    ↓
                          Guest completes stay
                                    ↓
                          Guest leaves a review
                                    ↓
                        Rating updated on property
```

---

## 🏗️ How the System Is Built (In Simple Words)

Think of our system like a restaurant:

| Part | What It Is Like | What It Actually Does |
|------|-----------------|-----------------------|
| **Spring Boot (Backend)** | The kitchen | Receives requests, processes them, sends back responses |
| **PostgreSQL (Database)** | The filing cabinet | Stores all permanent information — users, bookings, reviews |
| **Redis (Cache)** | The notepad on the counter | Stores recent results so frequently requested data loads faster |
| **Elasticsearch (Search Engine)** | The index at the back of a book | Helps find properties quickly without reading every record |
| **MinIO (File Storage)** | The photo album | Stores all property images separately from the main database |
| **JWT (Login Token)** | Your ID card | Proves who you are with every request without logging in again |
| **Email Service** | The notification board | Sends emails to guests and hosts when something important happens |
| **Kibana (Dashboard)** | The manager's report | Shows charts and graphs about the data for the admin |
| **Docker** | A ready-to-go tiffin box | Packages everything together so the whole system starts with one command |

---

### Simple Architecture Diagram

```
[ User / Postman / App ]
           ↓
   [ Spring Boot Backend ]
    (handles all requests)
      ↓    ↓    ↓    ↓
      ↓    ↓    ↓    ↓
  [DB] [Cache] [Search] [Files]
 (PG) (Redis) (Elastic) (MinIO)
                          ↓
                    [Email Service]
                    (sends emails)
```

---

## 🛡️ How We Prevented Double Booking

This was one of the hardest parts of the project, so we want to explain it clearly.

**The problem:**
Imagine Rahul and Amit both open the same property listing at the same time. They both see the dates as available. They both click "Book" at the same moment. Without any protection, both bookings would go through — and the host now has two guests for the same dates.

**How we solved it:**

We use a **two-step protection system:**

**Step 1 — The 30-second hold:**
When Rahul clicks "Book", the system immediately places a temporary reservation on those dates — like a timer. This hold lasts for 30 seconds. Rahul now has 30 seconds to complete his booking.

When Amit clicks "Book" at almost the same time, the system sees that a hold already exists for those dates. It tells Amit: *"Sorry, someone else is in the process of booking these dates. Please try again in a few seconds."*

**Step 2 — Double check before confirming:**
Even after the hold, before creating the final booking, the system checks one more time: *"Has anyone else already confirmed a booking for these dates?"* This is a safety net in case anything slips through.

**Result:** Only one booking gets created. The guest who clicked first wins. The second guest is politely informed.

---

## 🧩 Challenges We Faced During Development

---

### Challenge 1 — Images Were Not Uploading Correctly

**Problem:** When we tried to upload property photos using Postman, the files were arriving at the server as empty — 0 bytes, no content.

**How we solved it:** We switched from Postman to using the `curl` command in the terminal for file uploads. We also added a backup check on the server side to detect the file type from its name (like `.jpg` or `.png`) in case it was not detected automatically.

**What we learned:** Always test file uploads with multiple tools. If one tool behaves strangely, do not spend hours debugging it — try a different tool first.

---

### Challenge 2 — Redis Was Not Saving Data Properly

**Problem:** When we tried to save a property to Redis (our cache), the system crashed with an error. It was trying to load some property information from the database — but by that time, the database connection was already closed.

**How we solved it:** We made sure all the data we wanted to cache was fully loaded before the database connection closed. We also updated the way Redis stores data so it knows what type of object it is saving and loading.

**What we learned:** Caching is not just copy-paste. You have to make sure the data is fully ready before you try to store it somewhere else.

---

### Challenge 3 — Time Zone Error on Windows

**Problem:** Our app kept crashing at startup with a strange error about timezones. Java on Windows was using an old name for the India timezone (`Asia/Calcutta`), but the database only accepts the modern name (`Asia/Kolkata`).

**How we solved it:** We added one line of code at the very beginning of our application that sets the correct timezone before anything else starts.

**What we learned:** Sometimes bugs have nothing to do with your actual code — they come from the environment you are running in. Always check your environment first when you see strange errors.

---

### Challenge 4 — Setting Up Docker for the First Time

**Problem:** We had never used Docker before this project. Getting PostgreSQL, Redis, Elasticsearch, MinIO, and Kibana all running together without conflicts was very confusing at the start.

**How we solved it:** We learned how to write a `docker-compose.yml` file — which is basically a single configuration file that starts all services together with one command. We also learned how to check if each service was running and how to restart individual ones when something went wrong.

**What we learned:** Docker feels complicated at first, but once you understand it, it makes life much easier. Instead of installing five different softwares separately, one command starts everything.

---

### Challenge 5 — Sending Emails From Our App

**Problem:** Gmail does not allow regular apps to send emails using your normal password anymore. Every time we tried, the email was blocked.

**How we solved it:** We learned about Gmail's "App Password" feature — where you generate a special 16-character password just for your app, separate from your actual Gmail password. Once we used that, emails started working.

**What we learned:** Email services have security restrictions. Always check the documentation of the email provider you are using before spending time debugging your own code.

---

## 📚 What We Learned From This Project

- **REST APIs** — how to design proper endpoints that follow conventions
- **User Authentication** — how login tokens work and why they are secure
- **Database Design** — how to structure tables and relationships properly
- **Role-Based Access** — how to restrict what different users can and cannot do
- **File Storage** — how images and files are stored separately from databases
- **Caching** — why caching matters and how it speeds up applications
- **Search** — how a search engine is different from a regular database query
- **Email Integration** — how to send automated emails from a backend application
- **Docker** — how to package and run multiple services together
- **Async Processing** — how to run tasks in the background without slowing down the main app
- **Working With Multiple Technologies Together** — the biggest learning of all

---

## 🚀 What We Would Add Next

If we had more time (or turned this into a real product), here is what we would build next:

| Feature | Why It Would Be Useful |
|---------|------------------------|
| 💳 Payment Gateway (Razorpay / Stripe) | Guests need to actually pay for their booking |
| 📍 Location-Based Search | "Show me properties within 10 km of me" using maps |
| 📱 Mobile App | Most users are on phones — a proper Android/iOS app would help |
| 🔔 Real-Time Notifications | Instead of emails, instant pop-up alerts inside the app |
| 🗺️ Google Maps Integration | Show exact property location on a map |
| 🌍 Multi-Currency Support | Allow prices in USD, EUR, and other currencies |
| 🤖 Property Recommendations | Suggest similar properties based on what you have previously viewed |
| 🔁 Cancellation and Refund Policy | Allow partial refunds based on how early a guest cancels |
| 📊 Admin Web Dashboard | A proper website for admins instead of the Kibana tool |

---

## 🛠️ Tech Stack

| Technology | Why We Used It |
|-----------|---------------|
| **Java 21** | The main programming language — widely used in backend development |
| **Spring Boot** | Makes building REST APIs much faster — handles a lot of setup automatically |
| **PostgreSQL** | Our main database — reliable and widely used in real companies |
| **Redis** | Makes the app faster by storing frequently used data in memory |
| **Elasticsearch** | Helps search properties quickly with filters |
| **MinIO** | Stores property images — works like a personal version of Amazon S3 |
| **JWT (JSON Web Token)** | Keeps users logged in securely without storing sessions on the server |
| **Spring Mail + Thymeleaf** | Sends nicely formatted HTML emails to users |
| **Kibana** | Visualises data from Elasticsearch in charts and graphs |
| **Docker + Docker Compose** | Runs all services together with a single command |
| **Swagger (OpenAPI)** | Auto-generates API documentation so others can test our endpoints easily |
| **Maven** | Manages all the libraries and dependencies our project uses |
| **Lombok** | Reduces repetitive Java code like getters and setters |
| **Bucket4j** | Limits how many requests one user can make per minute to prevent abuse |

---

## ▶️ How to Run This Project

### What You Need First

- Java 21 installed
- Maven installed
- Docker Desktop installed and running

### Steps

```bash
# Step 1 — Clone the project
git clone https://github.com/your-username/stayfinder.git
cd stayfinder

# Step 2 — Start all services (database, cache, search, storage)
docker-compose up -d

# Step 3 — Set up MinIO (image storage)
# Open http://localhost:9001
# Login: minioadmin / minioadmin
# Create a bucket named "stayfinder" and set it to Public

# Step 4 — Add your Gmail App Password in application.properties

# Step 5 — Run the application
./mvnw spring-boot:run

# Step 6 — Open Swagger UI to test all endpoints
# http://localhost:8080/swagger-ui/index.html
```

---

## 📡 API Endpoints — Quick Reference

| Feature | Request Type | Endpoint | Who Can Use |
|---------|-------------|----------|-------------|
| Register | POST | `/api/users/register` | Everyone |
| Login | POST | `/api/auth/login` | Everyone |
| Create Property | POST | `/api/properties` | HOST only |
| View Property | GET | `/api/properties/{id}` | Logged in |
| Upload Images | POST | `/api/properties/{id}/images` | HOST only |
| Search Properties | GET | `/api/search` | Logged in |
| Place Hold | POST | `/api/bookings/hold` | GUEST only |
| Create Booking | POST | `/api/bookings` | GUEST only |
| Confirm Booking | PATCH | `/api/bookings/{id}/confirm` | HOST only |
| Cancel Booking | PATCH | `/api/bookings/{id}/cancel` | HOST or GUEST |
| Leave Review | POST | `/api/reviews` | GUEST only |
| View Rating | GET | `/api/reviews/property/{id}/rating` | Logged in |

---

## 👩‍💻 Developed By

**Falak Khan**
Final Year B.Tech — Computer Science Engineering
[GitHub](#) • [LinkedIn](#) • falak@email.com

**Fiza Khan**
Final Year B.Tech — Computer Science Engineering
[GitHub](#) • [LinkedIn](#) • fiza@email.com

---

> *We built this project from scratch to understand how real backend systems work.
> Every feature was implemented by us, tested by us, and debugged (the hard way) by us.*
>
> *If you found this helpful or want to discuss it, feel free to reach out!* 😊

---

⭐ If this project helped you understand backend development, please give it a star!
