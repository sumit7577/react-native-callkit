import { TurboModuleRegistry, type TurboModule } from 'react-native';

// ---- Types ----
export type Accepted = "Accepted" | "Rejected";

export type ForwardCallParams = {
  cfi: boolean;
  phoneNumber: string;
};

export interface PhoneNumber {
  value: string;
  type: number;
  label: string;
  normalizedNumber: string;
  isPrimary: boolean;
}

export interface SimpleContact {
  rawId: number;
  contactId: number;
  name: string;
  photoUri: string;
  phoneNumbers: PhoneNumber[];
  birthdays: string[];
  anniversaries: string[];
}

export type PhoneNumberContact = {
  value: string;
  type: number;
  label: string;
  normalizedNumber: string;
  isPrimary?: boolean;
};

export type Email = { value: string; type: number; label: string };
export type Address = { value: string; type: number; label: string };
export type Event = { value: string; type: number; label: string };
export type Group = { id: number; title: string };
export type Organization = { company: string; title: string };
export type IM = { value: string; type: number; label: string };

export type Contact = {
  id: number;
  prefix?: string;
  firstName?: string;
  middleName?: string;
  surname?: string;
  suffix?: string;
  nickname?: string;
  photoUri?: string;
  thumbnailUri?: string;
  photo?: string | null;
  phoneNumbers?: PhoneNumber[];
  emails?: Email[];
  addresses?: Address[];
  events?: Event[];
  source?: string;
  starred?: number;
  contactId: number;
  notes?: string;
  groups?: Group[];
  organization?: Organization;
  websites?: string[];
  IMs?: IM[];
  mimetype?: string;
  ringtone?: string | null;
  rawId?: number;
  name?: string;
  birthdays?: string[];
  anniversaries?: string[];
};

// ---- Spec (TurboModule contract) ----
export interface Spec extends TurboModule {
  // Default dialer
  requestRole(): Promise<Accepted | string>;

  // Call handling
  makeCall(phoneNumber: string): Promise<string>;
  toggleVibration(value: boolean): Promise<string>;
  getVibrationStatus(): Promise<boolean>;

  // Call forwarding
  forwardAllCalls(cfi: boolean, phoneNumber: string): Promise<string>;

  // Replies
  getReplies(): Promise<string[]>;
  saveReplies(replies: string): Promise<void>;

  // Contacts
  getAllContacts(): Promise<SimpleContact[]>;
  createNewContact(contact: Contact): Promise<string>;
  updateContact(contact: Contact, photoStatus: number): Promise<string>;
  deleteContact(contact: Contact): Promise<string>;

  // Blocked numbers
  getBlockedNumbers(): Promise<string[]>;
  addBlockedNumber(phoneNumber: string): Promise<string>;
  removeBlockedNumber(phoneNumber: string): Promise<string>;
}

// Enforce native binding
export default TurboModuleRegistry.getEnforcing<Spec>("Callkit");
