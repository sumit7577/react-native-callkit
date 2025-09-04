# 📞 react-native-caller

[![npm version](https://img.shields.io/npm/v/react-native-caller.svg?color=brightgreen&label=npm)](https://www.npmjs.com/package/react-native-caller)
[![npm downloads](https://img.shields.io/npm/dm/react-native-caller.svg)](https://www.npmjs.com/package/react-native-caller)
[![license](https://img.shields.io/npm/l/react-native-caller.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-android-blue.svg)](https://reactnative.dev/)

A **React Native Android module** that integrates with the native telecom APIs.  
It allows your app to:

- 📱 Request **default dialer role**  
- 📞 Make and manage calls  
- 🔁 Enable call forwarding  
- 💬 Manage quick replies  
- 👥 Access and modify contacts  
- 🚫 Manage blocked numbers  

---

## 🚀 Installation

```sh
npm install react-native-caller
# or
yarn add react-native-caller
```

---

## ⚙️ Android Setup

If you want your app to request becoming the **default dialer**:

```xml
<intent-filter>
  <action android:name="android.intent.action.DIAL" />
  <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

---

## 📖 Usage

```tsx
import Caller from "react-native-caller";

// Request default dialer role
await Caller.requestRole();

// Make a call
await Caller.makeCall("+15551234567");

// Get contacts
const contacts = await Caller.getAllContacts();

// Block a number
await Caller.addBlockedNumber("+15551234567");
```

---

## 📚 API

### 📱 Default Dialer
- `requestRole(): Promise<string>` → Request your app to become default dialer  

### 📞 Call Handling
- `makeCall(phoneNumber: string): Promise<string>` → Start a call  
- `toggleVibration(value: boolean): Promise<string>` → Enable/disable vibration  
- `getVibrationStatus(): Promise<boolean>` → Get vibration status  

### 🔁 Call Forwarding
- `forwardAllCalls(cfi: boolean, phoneNumber: string): Promise<string>`  

### 💬 Replies
- `getReplies(): Promise<string[]>`  
- `saveReplies(replies: string): Promise<void>`  

### 👥 Contacts
- `getAllContacts(): Promise<SimpleContact[]>`  
- `createNewContact(contact: Contact): Promise<string>`  
- `updateContact(contact: Contact, photoStatus: number): Promise<string>`  
- `deleteContact(contact: Contact): Promise<string>`  

### 🚫 Blocked Numbers
- `getBlockedNumbers(): Promise<string[]>`  
- `addBlockedNumber(phoneNumber: string): Promise<string>`  
- `removeBlockedNumber(phoneNumber: string): Promise<string>`  

---

## 🔒 Permissions

Request runtime permissions in your app:

```ts
import { PermissionsAndroid } from "react-native";

await PermissionsAndroid.requestMultiple([
  PermissionsAndroid.PERMISSIONS.CALL_PHONE,
  PermissionsAndroid.PERMISSIONS.READ_CONTACTS,
  PermissionsAndroid.PERMISSIONS.WRITE_CONTACTS,
]);
```

---

## 🧑‍💻 Contributing

We welcome contributions! Please check out the following before submitting a PR:

- [Development workflow](CONTRIBUTING.md#development-workflow)  
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)  
- [Code of conduct](CODE_OF_CONDUCT.md)  

---

## 📄 License

MIT © 2025 [sumit7577]  

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
