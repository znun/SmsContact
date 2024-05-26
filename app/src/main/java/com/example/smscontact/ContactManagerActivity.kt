package com.example.smscontact

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ContactManagerActivity : AppCompatActivity() {

    private lateinit var contactListAdapter: ContactListAdapter
    private val PICK_CONTACT_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_manager)

        val contacts = loadContacts()
        contactListAdapter = ContactListAdapter(this, contacts) { position ->
            contactListAdapter.removeContact(position)
            saveContacts(contactListAdapter.getContacts())
        }

        val contactListView: ListView = findViewById(R.id.contact_list_view)
        contactListView.adapter = contactListAdapter

        val addContactButton: Button = findViewById(R.id.add_contact_button)
        addContactButton.setOnClickListener {
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { contactUri ->
                val cursor = contentResolver.query(contactUri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val contactIdIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val hasPhoneNumberIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    if (contactIdIndex >= 0 && hasPhoneNumberIndex >= 0) {
                        val contactId = cursor.getString(contactIdIndex)
                        val hasPhoneNumber = cursor.getInt(hasPhoneNumberIndex)
                        if (hasPhoneNumber > 0) {
                            val phones = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )
                            if (phones != null && phones.moveToFirst()) {
                                val phoneNumberIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                val contactNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                                if (phoneNumberIndex >= 0 && contactNameIndex >= 0) {
                                    val contactName = cursor.getString(contactNameIndex)
                                    val contactNumber = phones.getString(phoneNumberIndex)
                                    val contact = Contact(contactName, contactNumber)
                                    contactListAdapter.addContact(contact)
                                    saveContacts(contactListAdapter.getContacts())
                                    Log.d("ContactManager", "Contact saved: $contact")
                                }
                                phones.close()
                            }
                        }
                    }
                    cursor.close()
                }
            }
        }
    }

    private fun loadContacts(): MutableList<Contact> {
        val prefs = getSharedPreferences("com.example.smscontact.prefs", Context.MODE_PRIVATE)
        val contactsJson = prefs.getString("contacts", "[]")
        Log.d("ContactManager", "Loaded contacts: $contactsJson")
        return Contact.fromJson(contactsJson)
    }

    private fun saveContacts(contacts: List<Contact>) {
        val prefs = getSharedPreferences("com.example.smscontact.prefs", Context.MODE_PRIVATE)
        val contactsJson = Contact.toJson(contacts)
        Log.d("ContactManager", "Saving contacts: $contactsJson")
        prefs.edit().putString("contacts", contactsJson).apply()
    }
}
