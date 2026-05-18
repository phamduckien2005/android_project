package com.example.sqlite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FirebaseUser;

public class activity_login extends AppCompatActivity {

    TextInputEditText edtEmail, edtPassword;

    MaterialButton btnLogin,
            btnGoogle,
            btnFacebook;

    TextView tvRegister;

    SharedPreferences preferences;

    // GOOGLE SIGN IN
    GoogleSignInClient googleSignInClient;
    FirebaseAuth firebaseAuth;

    int RC_SIGN_IN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ánh xạ
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);

        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);

        tvRegister = findViewById(R.id.tvRegister);

        preferences = getSharedPreferences(
                "USER_FILE",
                MODE_PRIVATE
        );

        // FIREBASE AUTH
        firebaseAuth = FirebaseAuth.getInstance();

        // GOOGLE CONFIG
        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(
                        GoogleSignInOptions.DEFAULT_SIGN_IN
                )
                        .requestIdToken(
                                getString(R.string.default_web_client_id)
                        )
                        .requestEmail()
                        .build();

        googleSignInClient =
                GoogleSignIn.getClient(this, gso);

        // CHUYỂN ĐĂNG KÝ
        tvRegister.setOnClickListener(v -> {

            Intent intent =
                    new Intent(activity_login.this,
                            activity_register.class);

            startActivity(intent);
        });

        // LOGIN THƯỜNG
        btnLogin.setOnClickListener(v -> loginUser());

        // LOGIN GOOGLE
        btnGoogle.setOnClickListener(v -> signInGoogle());

        // LOGIN FACEBOOK DEMO
        btnFacebook.setOnClickListener(v -> {

            UserSession.saveCurrentUser(this, "facebook_demo", "facebook_demo");

            Toast.makeText(this,
                    "Đăng nhập Facebook thành công",
                    Toast.LENGTH_SHORT).show();

            openHome();
        });
    }

    // ==========================
    // LOGIN THƯỜNG
    // ==========================
    private void loginUser() {

        String email =
                edtEmail.getText().toString().trim();

        String password =
                edtPassword.getText().toString().trim();

        // CHECK EMAIL
        if(email.isEmpty()){

            edtEmail.setError("Nhập email");
            return;
        }

        // EMAIL HỢP LỆ
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){

            edtEmail.setError("Email không hợp lệ");
            return;
        }

        // CHECK PASSWORD
        if(password.isEmpty()){

            edtPassword.setError("Nhập mật khẩu");
            return;
        }

        // LẤY DỮ LIỆU ĐÃ LƯU
        String savedEmail =
                preferences.getString("email","");

        String savedPassword =
                preferences.getString("password","");

        // KIỂM TRA
        if(email.equals(savedEmail)
                && password.equals(savedPassword)){

            String savedName = preferences.getString("name", "");
            UserSession.saveCurrentUser(this, email, email, savedName);

            Toast.makeText(this,
                    "Đăng nhập thành công",
                    Toast.LENGTH_SHORT).show();

            openHome();

        }else{

            Toast.makeText(this,
                    "Sai tài khoản hoặc mật khẩu",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ==========================
    // GOOGLE SIGN IN
    // ==========================
    private void signInGoogle(){
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent =
                    googleSignInClient.getSignInIntent();

            startActivityForResult(signInIntent,
                    RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    @Nullable Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        // KẾT QUẢ GOOGLE
        if(requestCode == RC_SIGN_IN){

            Task<GoogleSignInAccount> task =
                    GoogleSignIn
                            .getSignedInAccountFromIntent(data);

            try {

                GoogleSignInAccount account =
                        task.getResult(ApiException.class);

                firebaseAuthWithGoogle(
                        account.getIdToken()
                );

            } catch (ApiException e) {

                Toast.makeText(this,
                        "Google Sign In Failed",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // FIREBASE AUTH
    private void firebaseAuthWithGoogle(String idToken){

        AuthCredential credential =
                GoogleAuthProvider.getCredential(
                        idToken,
                        null
                );

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {

                    if(task.isSuccessful()){

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            UserSession.saveCurrentUser(
                                    this,
                                    user.getUid(),
                                    user.getEmail(),
                                    user.getDisplayName()
                            );
                        }

                        Toast.makeText(this,
                                "Đăng nhập Google thành công",
                                Toast.LENGTH_SHORT).show();

                        openHome();

                    }else{

                        Toast.makeText(this,
                                "Authentication Failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ==========================
    // OPEN HOME
    // ==========================
    private void openHome(){

        Intent intent =
                new Intent(activity_login.this,
                        MainActivity.class);

        startActivity(intent);

        finish();
    }
}
