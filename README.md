# RM File Management Suite - Android App

Native Android Application built with Kotlin and Jetpack Compose for Relationship Officers, Administrators, and Operations Mentors.

## 🚀 GitHub Actions - অটোমেটিক APK তৈরি ও ডাউনলোড করার নিয়ম

এই রিপোজিটরিতে **GitHub Actions CI/CD Pipeline** সেটআপ করা হয়েছে। আপনি গিটহাবে পুশ করলেই নিজে নিজে সম্পূর্ণ APK তৈরি হয়ে যাবে।

### ধাপ ১: AI Studio থেকে GitHub-এ কোড পাঠান
1. AI Studio-এর উপরের মেনুতে থাকা **"Push to GitHub"** বা **"Share / Export to GitHub"** বাটনে ক্লিক করুন।
2. আপনার GitHub অ্যাকাউন্ট সিলেক্ট করে একটি নতুন রিপোজিটরি তৈরি করুন (অথবা বিদ্যমান রিপোজিটরিতে পুশ করুন)।

### ধাপ ২: GitHub-এ অটোমেটিক APK বিল্ড হওয়া
1. কোড পুশ হওয়ার সাথে সাথে GitHub Actions স্বয়ংক্রিয়ভাবে কাজ শুরু করবে।
2. আপনার GitHub রিপোজিটরির **"Actions"** ট্যাবে যান।
3. সেখানে **"Build Android APK"** নামের একটি ওয়ার্কফ্লো দেখতে পাবেন (যা ২–৩ মিনিটের মধ্যে সবুজ টিক দিয়ে সম্পন্ন হবে)।

### ধাপ ৩: মোবাইলে সরাসরি APK ডাউনলোড করুন
1. সম্পন্ন হওয়া ওয়ার্কফ্লোটিতে ক্লিক করুন।
2. নিচের দিকে **"Artifacts"** সেকশনে যান।
3. **`RM-File-Management-Suite-Debug-APK`** ফাইলের উপর ক্লিক করলেই সরাসরি `.apk` ফাইলটি আপনার মোবাইলে ডাউনলোড হয়ে যাবে!
4. ডাউনলোড হওয়া ফাইলে ট্যাপ করে ইনস্টল করে নিন।

---

## 🔑 ডেমো অ্যাকাউন্টস ও রোলস (Login Credentials)

| Role | Username / Code | Password | ক্ষমতা ও ফিচার |
| :--- | :--- | :--- | :--- |
| **Mentor (অপারেশন হেড)** | `mentor0` | `mentor123` | ফুল কন্ট্রোল, Universal App Name পরিবর্তন, নতুন RM অনুমোদন, লাইভ Google Maps ফিল্ড ট্র্যাকিং |
| **Admin (অ্যাডমিনিস্ট্রেটর)** | `admin0` | `admin123` | নতুন RM অ্যাসাইন/এন্ট্রি (পেন্ডিং স্ট্যাটাস), গ্লোবাল ডাটাবেস ও শিট সিঙ্ক |
| **RM (ফিল্ড অফিসার)** | `104393` | `password123` | CC-Number দিয়ে ফাইল এন্ট্রি, "Track My Location" দিয়ে অটো অ্যাড্রেস, ভয়েস ইনপুট, ফ্লোটেবল বাটন |

---

## 🛠️ টেকনিক্যাল আর্কিটেকচার (Technical Stack)
- **Language:** Kotlin 2.0+
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Architecture:** MVVM + Clean Architecture + StateFlow
- **Local Persistence:** Room SQLite Database
- **Location & Sensors:** Android Location API + Geocoder + Google Maps Intent
- **Speech-to-Text:** Android SpeechRecognizer + VoiceInputField on all fields
- **Notifications:** NotificationCompat with distinct audible tone generation (ToneGenerator)
