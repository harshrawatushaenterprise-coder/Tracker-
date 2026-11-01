# Prince Tracker — Android starter source

Package: `com.princetracker.app`

## Is project mein kya hai
- Firebase Phone OTP login
- Phone number se app user lookup aur location request
- Receiver ke liye pending request screen; explicit Accept/Reject
- Accept hone ke baad foreground location service (visible ongoing notification)
- Stop sharing par service band aur current location document delete
- Google Maps SDK dependency and map screen placeholder

## Important: yeh abhi first source-code build hai, production-ready release nahi
- Request/accept flow aur location sender scaffold implemented hain.
- Request bhejne wale ko receiver ka live marker dikhane ka map listener abhi add nahi hai.
- Push notification delivery while app closed is not implemented; pending requests app kholne par listener/screen mein aayengi. Iske liye FCM + trusted Cloud Function backend add karna hoga.
- `firestore.rules` mein location read rule ko production mein per accepted request validate karna hoga. Is initial starter mein client-side rules deliberately permissive/limited hain; public launch se pehle harden karein.
- Background location on Android 14+ ke liye Play policy, runtime permission and foreground-service start restrictions follow karna zaroori hai. User ne app kholkar sharing start karni hogi.
- Google Maps API key placeholder hai; real key add karni hogi.
- Firebase phone auth needs Firebase project, enabled Phone provider, SMS region policy, SHA-1/SHA-256, and `google-services.json`.

## Firebase setup
1. https://console.firebase.google.com/ par project banayein.
2. Android app add karein with exact package `com.princetracker.app`.
3. Download `google-services.json` and put it in `app/google-services.json`.
4. Authentication > Sign-in method > Phone enable karein; SMS region policy mein India allow karein.
5. Project settings mein debug SHA-1 and SHA-256 add karein.
6. Firestore Database create karein; `firestore/firestore.rules` review karke deploy karein.

## Google Maps setup
1. Google Cloud Console mein billing-enabled project banayein.
2. Maps SDK for Android enable karein.
3. API key banayein, Android app restriction mein package + SHA-1 set karein.
4. Manifest mein `YOUR_GOOGLE_MAPS_API_KEY` replace karein.

## Build
Android Studio mein folder open karein, Gradle sync karein, phone connect karke Run karein. Debug APK usually `app/build/outputs/apk/debug/app-debug.apk` mein aata hai.

## GitHub
Repository mein project ke andar ke saare files/folders upload karein. `google-services.json` aur API keys ko public repo mein commit na karein. GitHub par public repository banane se pehle secrets/config file exclude karein.
