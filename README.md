# AI Resume Builder – Simple 📄🤖

AI Resume Builder – Simple is an Android application designed to help users create professional resumes easily using Artificial Intelligence. The app provides a clean and user-friendly interface where users can enter their details, generate resume content with AI, and download or share resumes in PDF format.

---

## 🚀 Features

- **Google Sign-In**
  - Secure and fast authentication using Google Sign-In.

- **Resume Dashboard**
  - View all created resumes in one place.
  - Edit resume names or delete resumes anytime.

- **Easy Resume Creation**
  - Add personal details such as:
    - Name
    - Email
    - Phone Number
    - GitHub Profile
    - LinkedIn Profile
    - Portfolio Link

- **Professional Sections**
  - Education
  - Skills
  - Work Experience
  - Projects
  - Achievements
  - Courses & Certifications

- **AI-Powered Resume Generation**
  - Generate structured and professional resume content instantly using AI (Gemini API).

- **Edit Anytime**
  - Modify resume content even after AI generation.

- **PDF Download & Share**
  - Download resumes in PDF format.
  - Share resumes easily with recruiters or friends.

- **Usage Limit**
  - Free users can generate up to **3 AI resumes per month**.
  - The app tracks remaining days and resume usage.

- **User-Friendly Design**
  - Clean UI with a floating add button for quick resume creation.

---

## 🔐 Data & Security

- Uses **Google Firebase Authentication** and **Firestore**.
- User data is used **only for resume creation and management**.
- No user data is sold or misused.
- Ads are served using **Google AdMob**.

---

## 🎯 Who Is This App For?

- Students  
- Job Seekers  
- Freshers  
- Professionals looking to create resumes quickly  

---

## 🛠️ Tech Stack

- **Android:** Java, XML  
- **Authentication:** Firebase Google Sign-In  
- **Database:** Firebase Firestore  
- **AI:** Gemini AI API  
- **Ads:** Google AdMob  
- **PDF:** Android PDF generation  

---

## 📲 Project Setup Instructions

### 1️⃣ Clone the Repository
    
    git clone https://github.com/your-username/ai-resume-builder.git

### 2️⃣ Open in Android Studio
Open Android Studio
Click Open
Select the cloned project folder

### 3️⃣ Firebase Setup (Required)
1. Go to Firebase Console
2. Create a new project
3. Add an Android App
4. Download google-services.json
5. Place it inside: app/google-services.json

### 4️⃣ Enable Firebase Services
In Firebase Console:
Enable Authentication → Google Sign-In
Enable Firestore Database

### Edit Constants.java
    
    public static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE";

### Edit strings.xml:
    
     <!-- Dummy AdMob Test IDs -->
     <string name="app_id">ca-app-pub-3940256099942544~3347511713</string>
     <string name="banner_ad_unit_id">ca-app-pub-3940256099942544/6300978111</string>
     <string name="interstitial_ad_unit_id">ca-app-pub-3940256099942544/1033173712</string>
     <!--Google Sign-In Client ID-->
     <string name="default_web_client_id">ADD_YOUR_WEB_CLIENT_ID_HERE</string>
     <string name="privacy_policy_login_text">
     By signing in, you agree to our
     <a href="ADD_YOUR_PRIVACY_POLICY_URL">Privacy Policy</a>.
     </string>

### Privacy Policy
This app includes a Privacy Policy page hosted separately.

# 👨‍💻 Developer
- Pravas D S
- Independent Android Developer
- [play store](https://play.google.com/store/apps/developer?id=PASS+FAMILY)
