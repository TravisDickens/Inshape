# INSHAPE - Your Personal Fitness Tracker

Welcome to **INSHAPE** - a personalized fitness app designed to help users effectively manage and achieve their health and fitness goals. INSHAPE provides a user-friendly interface, tracking tools, and personalized notifications to support a healthier lifestyle.

## Table of Contents
1. [Introduction](#introduction)
2. [Features](#features)
3. [Design Considerations](#design-considerations)
4. [Technologies Used](#technologies-used)
5. [Getting Started](#getting-started)
6. [Usage](#usage)
7. [GitHub Actions](#github-actions)
8. [Release Notes](#release-notes)
9. [Acknowledgements](#acknowledgements)

## Introduction

INSHAPE aims to make fitness management easy and enjoyable. It caters to a wide variety of health needs including step tracking, bodyweight tracking, and meal planning. The app now supports multilingual options, offline mode, and motivational reminders, all while providing a seamless and secure user experience.

## Features

### Core Features
- **Step Tracking**: Track daily steps and visualize progress towards your daily goals.
- **Calorie Tracking**: Monitor calories burned and calories consumed with real-time insights.
- **Hydration Monitoring**: Set and track daily water intake goals.
- **Nutrition Tracking**: Log and analyze daily meals, complete with detailed nutritional insights.
- **Workout Demonstrations**: Access a library of workout images to guide exercise form and technique.
- **User Profile**: Set personal goals and manage user data for a customized experience.
- **Single Sign-On (SSO)**: Log in with Google for a streamlined, secure sign-in experience.

### New Features
- **Biometric Authentication**: Log in securely with fingerprint and facial recognition.
- **Multilingual Support**: Added support for Afrikaans and Zulu.
- **Weight Tracking with Line Chart**: Monitor body weight over time, displayed in a line chart for easy progress tracking.
- **Fitness Chatbot**: Get fitness tips and answers to workout-related questions via an in-app chatbot.
- **Offline Mode**: Track user goals and weight changes without an internet connection using Room Database.
- **Notifications**: Receive reminders for calorie intake, hydration reminders, meal times, and motivational quotes to stay on track.

## Design Considerations

- **User-Centric Design**: Focused on simplicity, clarity, and aesthetic appeal to provide an engaging user experience.
- **Responsive Layouts**: Designed with ConstraintLayout and Material Design to ensure compatibility across device types.
- **Color Scheme**: Consistent color palette to maintain a cohesive look and feel.
<div style="display: flex; justify-content: space-between; align-items: center;">
    <img src="https://github.com/user-attachments/assets/7c8e48e2-65c0-4551-8054-a713e84b7f03" alt="LoginPS" width="150" height="280"/>
    <img src="https://github.com/user-attachments/assets/902dcbf2-b8ce-460c-b06d-74ef35c88ce3" alt="Register" width="150" height="280"/>
    <img src="https://github.com/user-attachments/assets/a5769ed0-3d14-4bc9-a518-2bfd19a5713b" alt="Home" width="150" height="280"/>
    <img src="https://github.com/user-attachments/assets/048d9452-b655-48c3-9fe9-594830078f08" alt="ExerciseVideos" width="150" height="280"/> 
    <img src="https://github.com/user-attachments/assets/b373a1bd-80ec-4d6a-b8bc-d833c735b219" alt="Chatbot" width="150" height="280"/> 
    <img src="https://github.com/user-attachments/assets/af935c0a-a06c-4c54-b997-215b98838add" alt="settings" width="150" height="280"/> 
</div>

## Technologies Used

- **Android Studio**: Development environment.
- **Kotlin**: Primary language.
- **Firebase**: Backend support for authentication and real-time database storage.
- **Room Database**: Offline data storage for goals and weight tracking.
- **JSONBin**: API for nutritional data.
- **GitHub Actions**: CI/CD for automated testing and build verifications.

## Getting Started

To run this app locally:

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/VCWVL/opsc7312-poe-ST10019757.git
   ```
2. **Open in Android Studio** and sync with Gradle.
3. **Set up Firebase** by adding your configuration files.
4. **Build and Run** on an emulator or physical device.

## Usage

Once installed, log in with Google or create an account to access the main dashboard, which includes step tracking, calorie tracking, hydration tracking, and more. Notifications keep you informed, and the multilingual support ensures a broader user experience.

## GitHub Actions

Automated CI/CD processes using GitHub Actions:
- **Automated Testing**: Ensures stability of new changes.
- **Build Verification**: Automatically builds the project for stable releases.

## Release Notes

### Version 1.1.0
- **Added Multilingual Support**: Users can now choose between English, Afrikaans, and Zulu for the app interface.
- **Weight Tracking and Line Chart**: Track and visualize body weight over time.
- **In-App Fitness Chatbot**: A chatbot feature to assist users with fitness questions.
- **Offline Mode with Room Database**: Allows goal and weight tracking even when offline.
- **Notifications**:
  - Calorie intake alerts
  - Meal reminders
  - Hydration reminders
  - Motivational quotes
  - Weightloss notifications
- **Improved Security**: Strengthened permissions handling and enhanced security measures.

### Prototype Version 1.0.0
- **Initial Release** with core features, including step tracking, calorie and hydration monitoring, profile settings, and biometric authentication.

## Frequently Asked Questions (FAQs)
**Q1**: How do I reset my password?
**A**: To reset your password please contact the developer

**Q2**: Can I use INSHAPE offline?
**A**: Yes! INSHAPE offers an offline mode that allows you to track your goals and weight changes without an internet connection. Data will sync once you are back online.

**Q3**: How do I track my calories and nutrition?
**A**: You can log your meals using the nutrition tracking feature. Simply enter the food items you consumed, and INSHAPE will provide detailed nutritional insights, including calorie counts.

**Q4**: What if I encounter a bug or issue?
**A**: If you encounter any issues, please reach out to our support team through the contact details below.

**Q5**: How can I change my language preference?
**A**: You can change your language preference in the settings menu under the "Language" option. Currently, INSHAPE supports English, Afrikaans, and Zulu.

## Developer Contact Details
For any questions, feedback, or support, please contact us at:

**Email**: travisdickens246@gmail.com or Travis.Dickens@outlook.com 

## Acknowledgements
1. In (2024). Google Sign In | Firebase Authentication | Login with Google using Firebase in Android Kotlin 2024. [online] YouTube. Available at: https://youtu.be/dJS9QMapWIQ?si=jXnki8ULqFvm7dlY [Accessed 10 September. 2024].
2. Android, in (2020). How to create a Step Counter/Pedometer in Android Studio (Kotlin 2020). [online] YouTube. Available at: https://youtu.be/WSx2a99kPY4?si=Z3O9zMRgQ4xbams3 [Accessed 12 September. 2024].
3. for, C. (2023). CI/CD for Android Projects using Github Actions | Pipelines + Workflows. [online] YouTube. Available at: https://youtu.be/uBXzaaOHVzY?si=c70h7aEhfs41aE7P [Accessed 29 September. 2024].
4. Free (2022). Free API hosting! How to deploy rest API on jsonbin for free | Easy API host on a third-party server. [online] YouTube. Available at: https://youtu.be/THeD_NisH5E?si=OpANIwODINgwNyr5 [Accessed 10 September. 2024].
5. Knowledgement, A. (2024). News App in Android Studio using Kotlin | Part 2 - Retrofit & News API. [online] YouTube. Available at: https://youtu.be/rM9gYyrP5xY?si=HNH4pZTAtasvXOCb [Accessed 15 September. 2024].
6. Android, in (2023). How to play YouTube videos with custom controls in Android Studio without WebView. [online] YouTube. Available at: https://youtu.be/9K2-y1ih4j8?si=_Xe69pnybMx49AVg [Accessed 20 September. 2024].
7. Android, in (2023). The FULL Beginner Guide for Room in Android | Local Database Tutorial for Android. [online] YouTube. Available at: https://youtu.be/bOd3wO0uFr8?si=WOAo9NnUFQSx3TUL [Accessed 26 October. 2024].
8. San, S. (2020). ROOM Database - Kotlin. [online] YouTube. Available at: http://www.youtube.com/playlist?list=PLSrm9z4zp4mEPOfZNV9O-crOhoMa0G2-o [Accessed 26 October. 2024].
9. Android, in (2023a). AlarmManager in Android Studio || Notification using AlarmManager is Android Studio || 2023. [online] YouTube. Available at: https://youtu.be/5RcDWnNgkQg?si=yqNU_GCQFjr-uwDZ [Accessed 24 October. 2024].
10. Schedule Local Notifications Android Studio Kotlin Tutorial (2021). Schedule Local Notifications Android Studio Kotlin Tutorial. [online] YouTube. Available at: https://youtu.be/_Z2S63O-1HE?si=ENHJB1-Hc7d_JBS5 [Accessed 24 October. 2024].
11. with (2019). #3 Display Android notification at a particular time with Alarm Manager. [online] YouTube. Available at: https://youtu.be/nl-dheVpt8o?si=bwbuE_C9_suwY8UD [Accessed 24 October. 2024].
12. indently (2020). Android Chat Bot Tutorial 2020. [online] YouTube. Available at: http://www.youtube.com/playlist?list=PL4KX3oEgJcfcDx4VeO4R-aCBJruHpvqBs [Accessed 20 October. 2024].


