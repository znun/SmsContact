package com.example.smscontact


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView

class ContactListAdapter(context: Context, private val contacts: MutableList<Contact>, private val onDeleteContact: (Int) -> Unit) :
    ArrayAdapter<Contact>(context, 0, contacts) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.contact_list_item, parent, false)
        val contact = getItem(position)
        val nameTextView: TextView = view.findViewById(R.id.contact_name)
        val numberTextView: TextView = view.findViewById(R.id.contact_number)
        val deleteButton: Button = view.findViewById(R.id.delete_contact_button)

        nameTextView.text = contact?.name
        numberTextView.text = contact?.number

        deleteButton.setOnClickListener {
            onDeleteContact(position)
        }

        return view
    }

    fun addContact(contact: Contact) {
        contacts.add(contact)
        notifyDataSetChanged()
    }

    fun removeContact(position: Int) {
        contacts.removeAt(position)
        notifyDataSetChanged()
    }

    fun getContacts(): List<Contact> {
        return contacts
    }
}
