package org.dieschnittstelle.mobile.android.skeleton.viewmodel;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.view.inputmethod.EditorInfo;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.dieschnittstelle.mobile.android.skeleton.model.Task;
import org.dieschnittstelle.mobile.android.skeleton.util.DateConverter;

import java.util.ArrayList;
import java.util.List;

public class TaskDetailViewModel extends ViewModel {
    private Task task;
    private final MutableLiveData<Boolean> isTaskOnSave = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isTaskOnDelete = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isContactOnDelete = new MutableLiveData<>(false);
    private final MutableLiveData<String> nameInputError = new MutableLiveData<>();
    private final List<Contact> availableContacts = new ArrayList<>();

    private final Task.LatLng defaultLatLng = new Task.LatLng();

    public Task getTask() { return task; }

    public void setTask(Task task) {
        this.task = task;
    }

    public void saveTask() {
        isTaskOnSave.setValue(true);
    }

    public MutableLiveData<Boolean> isTaskOnSave() {
        return isTaskOnSave;
    }

    public void deleteTask() { isTaskOnDelete.setValue(true); }

    public MutableLiveData<Boolean> isTaskOnDelete() {
        return isTaskOnDelete;
    }

    public void deleteContact(String contactName) {
        Contact toBeDeleted = availableContacts.stream()
                .filter(contact -> contact.getName().equals(contactName))
                .findFirst().orElse(null);

        if (toBeDeleted == null) { return; }

        task.getContacts().remove(toBeDeleted.getId());
        isContactOnDelete.setValue(true);
    }

    public MutableLiveData<Boolean> isContactOnDelete() {
        return isContactOnDelete;
    }

    public List<Contact> getAvailableContacts() {
        return availableContacts;
    }

    public Task.LatLng getDefaultLatLng() {
        return defaultLatLng;
    }

    public boolean checkNameInputOnEnterKey(int keyId) {
        if (keyId == EditorInfo.IME_ACTION_NEXT || keyId == EditorInfo.IME_ACTION_DONE) {
            if (task.getName().length() <= 2) {
                this.nameInputError.setValue("Mission name is too short!");
                return true;
            }
        }
        return false;
    }

    public MutableLiveData<String> getNameInputError() {
        return nameInputError;
    }

    public boolean onNameInputChanged() {
        this.nameInputError.setValue(null);
        return false;
    }

    public String toDueDateString() {
        String dateTime = DateConverter.toDateString(task.getExpiry());
        if (dateTime.isBlank()) {
            return dateTime;
        }
        return dateTime.split(" ")[0];
    }

    public String toTimeLimitString() {
        String dateTime = DateConverter.toDateString(task.getExpiry());
        if (dateTime.isBlank()) {
            return dateTime;
        }
        return dateTime.split(" ")[1];
    }

    public void setupDefaultLatLng(double lat, double lng) {
        defaultLatLng.setLat(lat);
        defaultLatLng.setLng(lng);
    }

    public void setupAvailableContactList(ContentResolver contentResolver) {
        List<Contact> contactList = queryContacts(contentResolver);
        availableContacts.addAll(contactList);
    }

    //suppress warning when column index = -1
    @SuppressLint("Range")
    private List<Contact> queryContacts(ContentResolver contentResolver) {
        // https://stackoverflow.com/a/12562234
        List<Contact> contactList = new ArrayList<>();
        Cursor contactCursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        while (contactCursor != null && contactCursor.moveToNext()) {
            String id = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts._ID));
            String name = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            Contact contact = new Contact(id);
            contact.setName(name);

            List<String> phoneNumbers = collectPhoneNumbers(contentResolver, contactCursor, id);
            contact.setPhoneNumbers(phoneNumbers);

            List<String> emails = collectEmails(contentResolver, id);
            contact.setEmails(emails);

            contactList.add(contact);
        }

        if (contactCursor != null) {
            contactCursor.close();
        }
        return contactList;
    }

    //suppress warning when column index = -1
    @SuppressLint("Range")
    private static List<String> collectPhoneNumbers(ContentResolver contentResolver, Cursor contactCursor, String id) {
        List<String> phoneList = new ArrayList<>();

        if (contactCursor.getInt(contactCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
            Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);

            while (phoneCursor != null && phoneCursor.moveToNext()) {
                String phoneNo = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                phoneList.add(phoneNo);
            }

            if (phoneCursor != null) {
                phoneCursor.close();
            }
        }
        return phoneList;
    }

    //suppress warning when column index = -1
    @SuppressLint("Range")
    private List<String> collectEmails(ContentResolver contentResolver, String id) {
        List<String> emailList = new ArrayList<>();
        Cursor emailCursor = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[]{id}, null);

        while (emailCursor != null && emailCursor.moveToNext()) {
            String email = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
            emailList.add(email);
        }

        if (emailCursor != null) {
            emailCursor.close();
        }
        return emailList;
    }

    public static class Contact {
        String id;
        String name;
        List<String> phoneNumbers;
        List<String> emails;

        public Contact(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public List<String> getEmails() {
            return emails;
        }

        public void setEmails(List<String> emails) {
            this.emails = emails;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getPhoneNumbers() {
            return phoneNumbers;
        }

        public void setPhoneNumbers(List<String> phoneNumbers) {
            this.phoneNumbers = phoneNumbers;
        }
    }
}
