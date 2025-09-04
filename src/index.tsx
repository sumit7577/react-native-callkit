import Callkit, { type Contact, type ForwardCallParams, type SimpleContact } from './NativeCallkit';


/**
 * Native function to set your app to default caller app
 * After User agree to make the app as default dialer your app will able to pick and receive calls
 * This function will be mostly called inside useEffect when component get mounted
 * @example
 * CallKit.setDefaultDialer()
 */
export function setDefaultDialer(): Promise<"Accepted" | "Rejected" | string> {
  return Callkit.requestRole();
}

/**
 * Make a phone call to the given number.
 */
export function callUser(phoneNumber: string): Promise<string> {
  return Callkit.makeCall(phoneNumber);
}

/**
 * Toggle vibration for incoming calls.
 */
export function toggleVibration(value: boolean): Promise<string> {
  return Callkit.toggleVibration(value);
}

/**
 * Get the current vibration status.
 */
export function getVibrationStatus(): Promise<boolean> {
  return Callkit.getVibrationStatus();
}

/**
 * Enable/disable call forwarding via USSD.
 */
export function forwardAllCalls(params: ForwardCallParams): Promise<string> {
  return Callkit.forwardAllCalls(params.cfi, params.phoneNumber);
}

/**
 * Get saved quick replies for calls.
 */
export function getCallReplies(): Promise<string[]> {
  return Callkit.getReplies();
}

/**
 * Save quick replies for calls.
 */
export function saveCallReplies(replies: string): Promise<void> {
  return Callkit.saveReplies(replies);
}

/**
 * Fetch all contacts from the device.
 */
export function getAllContacts(): Promise<SimpleContact[]> {
  return Callkit.getAllContacts();
}

/**
 * Create a new contact in the device address book.
 */
export function createNewContact(contact: Contact): Promise<string> {
  return Callkit.createNewContact(contact);
}

/**
 * Update an existing contact.
 * 
 * @param photoStatus - 1 = Add, 2 = Remove, 3 = Change, 4 = Unchanged
 */
export function updateContact(contact: Contact, photoStatus: number): Promise<string> {
  return Callkit.updateContact(contact, photoStatus);
}

/**
 * Delete a contact from the device address book.
 */
export function deleteContact(contact: Contact): Promise<string> {
  return Callkit.deleteContact(contact);
}

/**
 * Get all blocked numbers.
 */
export function getBlockedNumbers(): Promise<string[]> {
  return Callkit.getBlockedNumbers();
}

/**
 * Block a number.
 */
export function blockNumber(phoneNumber: string): Promise<string> {
  return Callkit.addBlockedNumber(phoneNumber);
}

/**
 * Unblock a previously blocked number.
 */
export function unblockNumber(phoneNumber: string): Promise<string> {
  return Callkit.removeBlockedNumber(phoneNumber);
}