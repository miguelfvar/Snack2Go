package com.klanify.snack2go.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.klanify.snack2go.R

class LoginActivity : AppCompatActivity() {
    private val GOOGLESIGNIN = 100
    private val callbackmanager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {

        Thread.sleep(2000)
        setTheme(R.style.Base_Theme_FirebaseTutorial)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setup()
        sesion()

    }

    override fun onStart(){
        super.onStart()
        var layout = findViewById<LinearLayout>(R.id.loginLayout).visibility
        layout = View.VISIBLE

    }

    private fun sesion(){
        lateinit var prefs : SharedPreferences
        prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email :String? = prefs.getString("email", null)
        val provider :String? = prefs.getString("provider", null)

        if(email != null && provider != null ){
            showHome(email, ProviderType.valueOf(provider))
        }
    }

    private fun setup(){
        val signinbutton = findViewById<Button>(R.id.signinButton)
        val googlebutton = findViewById<Button>(R.id.googlebutton)
        val facebookbutton = findViewById<Button>(R.id.fbbutton)
        val email = findViewById<EditText>(R.id.TextEmailAddress)
        val password = findViewById<EditText>(R.id.TextPassword)
        val label = findViewById<TextView>(R.id.signupLabel)

        title = "Authentication"
        signinbutton.setOnClickListener{
            if(email.text.isNotEmpty() && password.text.isNotEmpty()) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(
                    email.text.toString(), password.text.toString()
                ).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this,"Inicio Exitoso", Toast.LENGTH_SHORT).show()
                        showHome(it.result?.user?.email ?:"", ProviderType.BASIC)
                    }
                    else{
                        showAlert()
                    }
                }
            }
            else{
                Toast.makeText(this,"Ingrese su email o contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        googlebutton.setOnClickListener{
            val googleConf :GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, googleConf)
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent,GOOGLESIGNIN)

        }

        facebookbutton.setOnClickListener{
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))
            LoginManager.getInstance().registerCallback(callbackmanager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        result?.let{
                            val token = it.accessToken
                            val credential :AuthCredential = FacebookAuthProvider.getCredential(token.token)
                            FirebaseAuth.getInstance().signInWithCredential(credential)
                                .addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        showHome(it.result?.user?.email ?: "", ProviderType.FACEBOOK)
                                    } else {
                                        showAlert()
                                    }
                                }
                        }
                    }

                    override fun onCancel() {

                    }

                    override fun onError(error: FacebookException) {
                        showAlert()
                    }
                })
        }

        label.setOnClickListener{
            startActivity(Intent(this, RegisterActivity::class.java))
        }

    }

    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Error al autenticar usuario")
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email:String, provider: ProviderType){
        val homeIntent : Intent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email",email)
            putExtra("provider",provider.name)
        }
        startActivity(homeIntent)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackmanager.onActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == GOOGLESIGNIN){
            val task :Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account :GoogleSignInAccount? = task.getResult(ApiException::class.java)
                if (account != null) {
                    val credential :AuthCredential = GoogleAuthProvider.getCredential(account.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                showHome(account.email ?: "", ProviderType.GOOGLE)
                            } else {
                                showAlert()
                            }
                        }
                }
            }catch (e: ApiException){
                showAlert()
            }
        }
    }
}