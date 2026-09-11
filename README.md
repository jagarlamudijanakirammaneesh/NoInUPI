# NoInUPI

NoInUPI is an Android application that enables UPI payments without requiring an internet connection.

It uses India's `*99#` USSD banking service to communicate with the user's bank through the mobile network.

## Features

- Send money using a UPI ID
- No internet connection required
- `*99#` USSD-based payments
- QR code scanning
- Receive UPI ID
- Transaction history
- Offline balance checking
- Simple and lightweight interface
- Manual UPI PIN entry through the bank/carrier interface

## How It Works

NoInUPI communicates with the bank through the `*99#` USSD service instead of using an internet-based UPI API.

Basic payment flow:

1. Enter or scan the recipient's UPI ID
2. Enter the amount
3. NoInUPI starts the `*99#` session
4. The required USSD options are handled automatically
5. The user enters their UPI PIN manually
6. The bank processes the transaction
7. The transaction result is displayed in the app

## Requirements

- Android device
- Active SIM card
- Bank account linked to UPI
- UPI-enabled bank account
- `*99#` USSD support from the mobile network
- No internet connection is required for the payment itself

## Security

NoInUPI does not collect or store your UPI PIN.

The UPI PIN is entered manually through the bank/carrier interface and is never stored by the application.

Transaction history is stored locally on the device.i don't know backend btw.

## Important

`*99#` availability and supported functionality depend on your bank and mobile network operator.

Some banks or operators may not support every `*99#` feature.

NoInUPI is an independent open-source project and is not affiliated with NPCI, BHIM, any bank, or any UPI service provider.

## Tech Stack

- Kotlin
- Android
- Jetpack Compose
- Android Accessibility Service
- USSD / `*99#`
- CameraX
- ML Kit
- ZXing

## Project Structure

```text
NoInUPI/
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           └── res/
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts