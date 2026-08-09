<p align="center">
  <a href="https://github.com/kartikbansode/quickbill-customer-display-app">
    <img width="80" height="80" alt="logo" src="https://github.com/user-attachments/assets/cc80dfee-89d8-4fc7-905d-7dfbaa3b480b" />
  </a>
</p>

<h1 align="center">
  QuickBill Customer Display
</h1>

<p align="center">
  Real-Time Android Customer Display for QuickBill
</p>

<p align="center">
  <a href="https://github.com/kartikbansode/quickbill">
    QuickBill Desktop
  </a>
  &bull;
  <a href="https://github.com/kartikbansode/quickbill-customer-display-app/releases">
    Releases
  </a>
  &bull;
  <a href="https://github.com/kartikbansode/quickbill-customer-display-app">
    Repository
  </a>
</p>

<p align="center">
  <a href="https://github.com/kartikbansode/quickbill-customer-display-app/releases/download/v1.1.0/QuickBill-Customer-Display-v1.1.0.apk">
    <strong>Download Android App v1.1.0</strong>
  </a>
</p>

<p align="center">
  <a href="https://github.com/kartikbansode/quickbill-customer-display-app/releases">
    <img
      src="https://img.shields.io/badge/Version-1.1.0-blue"
      alt="Version 1.1.0"
    />
  </a>

  <a href="https://developer.android.com/about/versions/oreo">
    <img
      src="https://img.shields.io/badge/Android-8.0%2B-green"
      alt="Android 8.0+"
    />
  </a>

  <a href="https://kotlinlang.org/">
    <img
      src="https://img.shields.io/badge/Kotlin-Android-purple"
      alt="Kotlin"
    />
  </a>

  <a href="https://developer.android.com/develop/ui/compose">
    <img
      src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-orange"
      alt="Jetpack Compose"
    />
  </a>
</p>

<p align="center">
  QuickBill Customer Display is an Android customer-facing display application
  designed to work with the QuickBill desktop billing system over a local network.
  It provides real-time bill, payment, QR and transaction status information
  on a dedicated customer display.
</p>

<p align="center">
  <strong>Companion app for
    <a href="https://github.com/kartikbansode/quickbill">QuickBill Desktop</a>
  </strong>
  · Real-time customer-facing billing display
</p>



## What's Changed in v1.1.0

- Added a more stable full-screen customer display experience
- Improved payment screen handling for cash, UPI, card, and credit
- Added dynamic UPI QR display support
- Improved success screen behavior after completed transactions
- Added better reconnect handling for LAN connection drops
- Improved synchronization with the desktop billing application
- Better handling when the desktop app is opened before or after the tablet app
- General UI polish and stability improvements

---

## Features

- Real-time bill updates
- Customer and cashier information
- Itemized billing display
- Quantity, rate and amount display
- Subtotal, tax, discount and total
- Cash payment display
- UPI payment display
- Card payment display
- Credit payment display
- Payment pending screen
- Payment success screen
- Automatic return to welcome screen
- Full-screen tablet interface
- Automatic reconnection to QuickBill desktop
- LAN WebSocket communication
- Desktop remains fully functional if the customer display is disconnected

---

## Screenshots

### Welcome Screen

<img width="2000" height="1200" alt="Screenshot_20260809-203824" src="https://github.com/user-attachments/assets/5366f565-fe6c-4b3c-b173-7996ca1368f9" />


### Billing Screen

<img width="2000" height="1200" alt="Screenshot_20260809-203851" src="https://github.com/user-attachments/assets/9e008f2c-4c78-45d8-8ac5-241d179b5a9c" />


### Payment Screen

<img width="2000" height="1200" alt="Screenshot_20260809-203917" src="https://github.com/user-attachments/assets/d6d30b66-5447-4ffd-b81c-e2e0fed87ffb" />

### Payment Successful Screen

<img width="2000" height="1200" alt="Screenshot_20260809-203922" src="https://github.com/user-attachments/assets/8a71e918-8447-4a89-9ffc-4592c6aec477" />


---

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Android
- WebSocket
- OkHttp
- Gradle

---

## Requirements

- Android tablet
- Android 8.0 (API 26) or newer
- QuickBill Desktop
- Tablet and desktop connected to the same local network

---

## Connection

The customer display connects to the QuickBill desktop application using:

```text
WebSocket
Port: 8765
```

---

## License

This project is licensed under the MIT License.

---

## Contact

**LinkedIn**  
https://www.linkedin.com/in/kartikbansode

**GitHub**  
https://github.com/kartikbansode

---
