package com.example.sqlite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;

public class activity_login extends AppCompatActivity {

    TextInputEditText edtEmail, edtPassword;

    MaterialButton btnLogin,
            btnGoogle,
            btnFacebook;

    TextView tvRegister;

    SharedPreferences preferences;

    GoogleSignInClient googleSignInClient;
    FirebaseAuth firebaseAuth;
    CallbackManager facebookCallbackManager;

    int RC_SIGN_IN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);

        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);

        tvRegister = findViewById(R.id.tvRegister);

        preferences = getSharedPreferences("USER_FILE", MODE_PRIVATE);

        firebaseAuth = FirebaseAuth.getInstance();
        if (UserSession.hasActiveSession(this)) {
            openHome();
            return;
        }

        facebookCallbackManager = CallbackManager.Factory.create();

        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(activity_login.this, activity_register.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> loginUser());
        btnGoogle.setOnClickListener(v -> signInGoogle());

        setupFacebookLogin();
        btnFacebook.setOnClickListener(v -> signInFacebook());
    }

    private void loginUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty()) {
            edtEmail.setError("Nh\u1eadp email");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email kh\u00f4ng h\u1ee3p l\u1ec7");
            return;
        }

        if (password.isEmpty()) {
            edtPassword.setError("Nh\u1eadp m\u1eadt kh\u1ea9u");
            return;
        }

        String savedEmail = preferences.getString("email", "");
        String savedPassword = preferences.getString("password", "");

        if (email.equals(savedEmail) && password.equals(savedPassword)) {
            String savedName = preferences.getString("name", "");
            UserSession.saveCurrentUser(this, email, email, savedName);

            Toast.makeText(this, "\u0110\u0103ng nh\u1eadp th\u00e0nh c\u00f4ng", Toast.LENGTH_SHORT).show();
            openHome();
        } else {
            Toast.makeText(this, "Sai t\u00e0i kho\u1ea3n ho\u1eb7c m\u1eadt kh\u1ea9u", Toast.LENGTH_SHORT).show();
        }
    }

    private void signInGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign In Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            UserSession.saveCurrentUser(
                                    this,
                                    user.getUid(),
                                    user.getEmail(),
                                    user.getDisplayName()
                            );
                        }

                        Toast.makeText(this, "\u0110\u0103ng nh\u1eadp Google th\u00e0nh c\u00f4ng", Toast.LENGTH_SHORT).show();
                        openHome();
                    } else {
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupFacebookLogin() {
        LoginManager.getInstance().registerCallback(
                facebookCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        firebaseAuthWithFacebook(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(
                                activity_login.this,
                                "Facebook Sign In Canceled",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Toast.makeText(
                                activity_login.this,
                                "Facebook Sign In Failed",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void signInFacebook() {
        LoginManager.getInstance().logOut();
        LoginManager.getInstance().logInWithReadPermissions(
                this,
                Arrays.asList("public_profile")
        );
    }

    private void firebaseAuthWithFacebook(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            UserSession.saveCurrentUser(
                                    this,
                                    user.getUid(),
                                    user.getEmail(),
                                    user.getDisplayName()
                            );
                        }

                        Toast.makeText(this, "\u0110\u0103ng nh\u1eadp Facebook th\u00e0nh c\u00f4ng", Toast.LENGTH_SHORT).show();
                        openHome();
                    } else {
                        Toast.makeText(this, "Facebook Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openHome() {
        Intent intent = new Intent(activity_login.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
