package com.example.smscontact



import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class ContactManagerActivity : AppCompatActivity() {

    private lateinit var contactListAdapter: ContactListAdapter
    private val PREFS_NAME = "com.example.rescue_mate.prefs"
    private val PREFS_KEY_CONTACTS = "contacts"
    private val PICK_CONTACT_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_manager)

        val contacts = loadContacts()

        contactListAdapter = ContactListAdapter(this, contacts) { position ->
            contactListAdapter.removeContact(position)
            saveContacts(contactListAdapter.getContacts())
        }

        val listView: ListView = findViewById(R.id.contact_list_view)
        listView.adapter = contactListAdapter

        val addButton: Button = findViewById(R.id.add_contact_button)
        addButton.setOnClickListener {
            pickContact()
        }
    }

    private fun pickContact() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(intent, PICK_CONTACT_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
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
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
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
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val contactsJson = prefs.getString(PREFS_KEY_CONTACTS, "[]")
        return Contact.fromJson(contactsJson)
    }

    private fun saveContacts(contacts: List<Contact>) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(PREFS_KEY_CONTACTS, Contact.toJson(contacts))
        }
    }
}
