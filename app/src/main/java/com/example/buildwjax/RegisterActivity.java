package com.example.buildwjax;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

// Kelas RegisterActivity bertugas menyediakan halaman bagi pengguna baru
// untuk mendaftarkan email dan password mereka ke sistem Firebase.
public class RegisterActivity extends AppCompatActivity {

    // --- DEKLARASI VARIABEL UI ---
    private EditText etEmail, etPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;

    // --- DEKLARASI FIREBASE ---
    // Variabel kunci untuk memanggil layanan autentikasi dari Google
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Menghubungkan variabel lokal dengan instance Firebase di cloud
        mAuth = FirebaseAuth.getInstance();

        // Mengaitkan variabel Java dengan komponen UI di XML
        etEmail = findViewById(R.id.etEmailReg);
        etPassword = findViewById(R.id.etPasswordReg);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // --- LOGIKA TOMBOL DAFTAR (REGISTER) ---
        btnRegister.setOnClickListener(v -> {
            // Mengambil teks yang diketik user dan menghapus spasi kosong di awal/akhir dengan trim()
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // --- 1. VALIDASI KEAMANAN DASAR ---
            // Mencegah aplikasi crash karena mengirim data kosong ke server
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show();
                return; // Hentikan proses, jangan lanjut ke server
            }

            // Mencegah error dari Firebase, karena Firebase Auth MENANGWIJIBKAN password minimal 6 karakter
            if (password.length() < 6) {
                Toast.makeText(this, "Password minimal 6 karakter!", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- 2. PROSES BUAT AKUN KE FIREBASE (ASYNCHRONOUS) ---
            // Mengirim data email dan password ke server Google secara background process
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        // Jika server Google mengonfirmasi pendaftaran sukses
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Registrasi Berhasil! Silakan Login", Toast.LENGTH_SHORT).show();

                            // finish() akan menghancurkan halaman Register ini dari memori (Stack).
                            // Karena sebelumnya kita berasal dari LoginActivity, maka saat dihancurkan,
                            // layar akan otomatis mundur (kembali) ke halaman Login.
                            finish();
                        } else {
                            // Jika gagal (misal: format email salah, atau email sudah pernah didaftarkan)
                            // Tampilkan pesan error langsung dari server Google (getException)
                            Toast.makeText(this, "Gagal: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // --- LOGIKA TOMBOL KEMBALI KE LOGIN ---
        // Jika user membatalkan niat mendaftar, langsung hancurkan halaman ini agar kembali ke layar Login
        tvGoToLogin.setOnClickListener(v -> finish());
    }
}