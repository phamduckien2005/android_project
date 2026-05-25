package com.example.sqlite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class activity_register extends AppCompatActivity {

    TextInputEditText edtName,
            edtEmail,
            edtPassword,
            edtConfirmPassword;

    MaterialButton btnRegister;
    TextView tvLogin;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword =
                findViewById(R.id.edtConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        preferences = getSharedPreferences(
                "USER_FILE",
                MODE_PRIVATE
        );

        editor = preferences.edit();

        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(activity_register.this, activity_login.class));
            finish();
        });
    }

    private void registerUser() {

        String name =
                edtName.getText().toString().trim();

        String email =
                edtEmail.getText().toString().trim();

        String password =
                edtPassword.getText().toString().trim();

        String confirm =
                edtConfirmPassword.getText().toString().trim();

        if(name.isEmpty()){
            edtName.setError("Nhập họ tên");
            return;
        }

        if(email.isEmpty()){
            edtEmail.setError("Nhập email");
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            edtEmail.setError("Email không hợp lệ");
            return;
        }

        if(password.length() < 6){
            edtPassword.setError("Ít nhất 6 ký tự");
            return;
        }

        if(!password.equals(confirm)){
            edtConfirmPassword.setError(
                    "Mật khẩu không khớp"
            );
            return;
        }

        editor.putString("name", name);
        editor.putString("email", email);
        editor.putString("password", password);

        editor.apply();

        Toast.makeText(this,
                "Đăng ký thành công",
                Toast.LENGTH_SHORT).show();

        startActivity(new Intent(
                activity_register.this,
                activity_login.class
        ));

        finish();
    }
}
