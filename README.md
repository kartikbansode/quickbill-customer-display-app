# QuickBill Customer Display
>
> Version: v1.0.0
> 
> 
Android customer display app for QuickBill billing software.

<a href="https://github.com/kartikbansode/quickbill-customer-display-app/releases/download/v1.0.0/QuickBill-Customer-Display-v1.0.0.apk">Download App (Andriod Apk v1.0.0)</a>

The app connects to the QuickBill desktop application over the local network and displays the current bill, payment status, and completed sale information on a customer-facing tablet.

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

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Android
- WebSocket
- OkHttp
- Gradle

## Requirements

- Android tablet
- Android 8.0 (API 26) or newer
- QuickBill Desktop
- Tablet and desktop connected to the same local network

## Connection

The customer display connects to the QuickBill desktop application using:

```text
WebSocket
Port: 8765
```

## License

This project is licensed under the MIT License.


## Contact

**LinkedIn**  
https://www.linkedin.com/in/kartikbansode

**GitHub**  
https://github.com/kartikbansode
