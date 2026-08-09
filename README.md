# QuickBill Customer Display

> **Latest Version:** 1.1.0  
> **Release Date:** August 2026

<a href="https://github.com/kartikbansode/quickbill-customer-display-app/releases/download/v1.1.0/QuickBill-Customer-Display-v1.1.0.apk">Download App (Android Apk v1.1.0)</a>

Android customer display app for QuickBill billing software.

The app connects to the QuickBill desktop application over the local network and displays the current bill, payment status, and completed sale information on a customer-facing tablet.

---

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

### Customer Billing Window

<img width="1919" height="1079" alt="image" src="https://github.com/user-attachments/assets/a7da29cc-d3e1-47d2-a949-7c3a4842206b" />

### Product Management

<img width="1919" height="1079" alt="image" src="https://github.com/user-attachments/assets/12a94658-4a30-4b03-986a-eeb3cc6795fb" />

### Invoice

### 80 mm -

<img width="347" height="826" alt="image" src="https://github.com/user-attachments/assets/5b73904e-bc3e-4373-9fc0-c13501e12a9d" />

### A4 -

<img width="671" height="842" alt="image" src="https://github.com/user-attachments/assets/b87f7430-563c-400b-b93d-4c637720cee1" />

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