package com.example.firebasepractice

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log.d
import android.util.Log.e
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.firebasepractice.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val REQUEST_SIGN_IN_CODE = 1
    private lateinit var mGoogleSignIn: ActivityResultLauncher<Intent>
    private lateinit var mAuth: FirebaseAuth
    private var isFieldEmpty: Boolean = false
    private val mStorage = FirebaseStorage.getInstance().reference
    private val mFireStore = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uploadImageToServer = registerForActivityResult(ActivityResultContracts.GetContent(),
            ActivityResultCallback {
                binding.imageView.setImageURI(it)
                try {

                    CoroutineScope(Dispatchers.IO).launch {
                        mStorage.child("Images/0").putFile(it).await()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                applicationContext,
                                "Upload Successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        applicationContext,
                        "Not Upload Successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            })

        val downloadImageFromServer = {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    val maxDownloadSize = 5L * 1024 * 1024
                    val byte = mStorage.child("Images/0").getBytes(maxDownloadSize).await()
                    val decodeBytes = BitmapFactory.decodeByteArray(byte, 0, byte.size)
                    withContext(Dispatchers.Main) {
                        binding.imageView.setImageBitmap(decodeBytes)
                        Toast.makeText(
                            applicationContext,
                            "Download Successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext,
                    "Not Download Successfully ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val deleteFromServer = {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    mStorage.child("Images/0").delete().await()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            applicationContext,
                            "Delete Successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext,
                    "Not Delete Successfully ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        mGoogleSignIn = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback { it ->
                it?.apply {

                    val intent = it.data
                    val account = GoogleSignIn.getSignedInAccountFromIntent(intent).result
                    account?.let { it1 ->
                        authFromFirebase(it1)

                    }
                }
            })

        with(binding) {
            btnUpload.setOnClickListener {

                uploadImageToServer.launch("image/*")

            }
            btnDownload.setOnClickListener {
                downloadImageFromServer()
            }
            btnDelete.setOnClickListener {
                deleteFromServer()
            }
            signInButton.setOnClickListener {
                signIn()
            }

            btnSavetofirestore.setOnClickListener {
                saveToFirestore()
            }
            btnReadfromfirestore.setOnClickListener {
                readFromFirestore()
            }
            btnUpdatetofirestore.setOnClickListener {
                updateToFirestore(getField1Data(),getField2Data())
            }
            btnDeletefromfirestore.setOnClickListener {
                deleteFromFirestore(getField1Data())
            }
        }
    }

    private fun deleteFromFirestore(user: User) {

        CoroutineScope(Dispatchers.IO).launch {
            try {

                d("MainActivity: ",user.toString())
                val querySnapshot = mFireStore.collection("Doc").whereEqualTo("name", user.name)
                    .whereEqualTo("age", user.age)
                    .whereEqualTo("salary",user.salary)
                    .get().await()
                if(querySnapshot.isEmpty){
                    withContext(Dispatchers.Main) {

                        Toast.makeText(
                            applicationContext,
                            "User Not Exist! ",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    for(document in querySnapshot.documents){

                        mFireStore.collection("Doc").document(document.id).delete().await()
                        withContext(Dispatchers.Main) {

                            Toast.makeText(
                                applicationContext,
                                "Delete Successfully ",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }



            } catch (e: Exception) {
                d("MainActivity: ", "Error ${e.message}")

            }
        }

    }


    private fun updateToFirestore(data: User,newData: User) {
        CoroutineScope(Dispatchers.IO).launch {
            try {

                d("MainActivity: ",data.toString())
                val querySnapshot = mFireStore.collection("Doc").whereEqualTo("name", data.name)
                    .whereEqualTo("age", data.age)
                    .whereEqualTo("salary",data.salary)
                    .get().await()
                querySnapshot?.let {
                    if(querySnapshot.isEmpty) {
                        withContext(Dispatchers.Main) {

                            Toast.makeText(
                                applicationContext,
                                "User Not Exist! ",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    else {
                        for (document in it.documents) {
                            d("MainActivity: ", document.id + newData.toString())

                            mFireStore.collection("Doc").document(document.id)
                                .set(newData, SetOptions.merge()).await()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    applicationContext,
                                    "Update Successfully ",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                d("MainActivity: ", "Error ${e.message}")

            }
        }
    }

    private fun readFromFirestore() {
        CoroutineScope(Dispatchers.IO).launch {
            var user: User?
            try {
                val querySnapshot = mFireStore.collection("Doc").get().await()
                for (document in querySnapshot.documents) {
                    user = document.toObject<User>()

                    withContext(Dispatchers.Main) {
                        showData(user!!)

                    }

                }


            } catch (e: Exception) {
                e("MainActivity: ", "Error ${e.message}")
            }
        }
    }

    private fun showData(user: User) {
        binding.etFirstname2.setText(user.name)
        binding.etAge2.setText(user.age.toString())
        binding.etSalary2.setText(user.salary.toString())

    }

    private fun saveToFirestore() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = getField1Data()

                    mFireStore.collection("Doc").document(data.name!!).set(data).await()
                    withContext(Dispatchers.Main) {
                        d("MainActivity: ", "Success")
                        binding.etFirstname.text.clear()
                        binding.etAge.text.clear()
                        binding.etSalary.text.clear()
                    }



            } catch (e: Exception) {
                d("MainActivity: ", "Error ${e.message}")
                Toast.makeText(applicationContext, "Fill Field First!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getField1Data(): User {
        with(binding) {
            val name = etFirstname.text?.toString()
            val age = etAge.text?.toString()
            val salary = etSalary.text?.toString()
            val user =  User(name.let { if(it?.isNotEmpty() == true)it else null }, age.let { if(it?.isNotEmpty() == true)it.toDouble() else null }, salary.let { if(it?.isNotEmpty() == true)it.toDouble() else null })

            isFieldEmpty = name?.isNotEmpty() == true && age?.isNotEmpty() == true && salary?.isNotEmpty() == true
            return user
        }

    }
    private fun getField2Data(): User {
        with(binding) {
            val name = etFirstname2.text?.toString()
            val age = etAge2.text?.toString()
            val salary = etSalary2.text?.toString()
            return User(name.let { if(it?.isNotEmpty() == true)it else null }, age.let { if(it?.isNotEmpty() == true)it.toDouble() else null }, salary.let { if(it?.isNotEmpty() == true)it.toDouble() else null })

        }

    }

    private fun authFromFirebase(account: GoogleSignInAccount) {
        try {
            mAuth = FirebaseAuth.getInstance()
            CoroutineScope(Dispatchers.IO).launch {


                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                mAuth.signInWithCredential(credential).await()

                val user = mAuth.currentUser

                d("From MainActivity: ", "Login Successfully ${user?.displayName}")


                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        applicationContext,
                        "Login Successfully \n ${user?.displayName}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
        } catch (e: Exception) {


            Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()

        }


    }

    private fun signIn() {
        // Configure Google Sign In

        //Create Option what you want!!
        val option = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_apikey))
            .requestEmail()
            .requestProfile()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(applicationContext, option)

        //Show the List of Account
        mGoogleSignIn.launch(googleSignInClient.signInIntent)

    }

}