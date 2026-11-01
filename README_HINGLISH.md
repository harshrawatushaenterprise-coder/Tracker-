# Prince Tracker — Premium Android starter (v0.2)

## Tere selected settings
- Android app
- Phone number login (OTP planned)
- Google Maps (map launch placeholder in this starter)
- Location history OFF
- Premium dark-blue and white interface

## Abhi is version mein kya hai?
- Premium Android screen
- Mobile number entry field
- Request button (demo message only)
- Google Maps open button
- Consent/privacy information
- GitHub Actions workflow jo repository mein code upload hone ke baad debug APK build kar sakta hai

## Important: live tracking abhi connected nahi hai
Abhi phone number par OTP, actual request notification, accept/reject workflow, Firebase backend aur real-time location map implement/configure nahi hue hain. Isliye ye demo interface hai, ready-to-use tracker nahi.

## Phone se APK banane ka next process
1. GitHub par free account banao.
2. New repository `prince-tracker` create karo.
3. ZIP ko extract karke uske andar ke files repository mein upload karo. `.github` folder bhi upload hona chahiye.
4. GitHub repository > Actions mein `Build Prince Tracker APK` workflow run hoga.
5. Build successful hone par workflow ke Artifacts section se `prince-tracker-debug-apk` download karna.

## Production features ke liye agla development
- Firebase Authentication phone OTP
- Firestore consent requests and sharing permissions
- Firebase Cloud Messaging notifications
- Location updates only after explicit acceptance
- Google Maps SDK/API key and live authorized markers
- Revoke/stop sharing, account deletion, security rules and privacy disclosures

Never use phone number alone to infer or retrieve someone's location. The invited person must install the app, verify their number, accept the request and grant Android location permission.
