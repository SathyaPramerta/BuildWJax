package com.example.buildwjax;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

// Kelas LoginActivity mewarisi (Inheritance) AppCompatActivity agar memiliki sifat-sifat layar Android.
// Ini adalah "Gerbang Utama" aplikasi BuildWJax.
public class LoginActivity extends AppCompatActivity {

    // --- DEKLARASI VARIABEL UI ---
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;

    // --- DEKLARASI FIREBASE AUTHENTICATION ---
    // Objek utama untuk mengelola sesi login, register, dan data pengguna dari Google Server
    private FirebaseAuth mAuth;

    // --- DEKLARASI KOMPONEN OVERLAY SPLASH SCREEN ---
    private RelativeLayout splashOverlay;
    private TextView tvAppName, tvJargon;
    private ImageView ivLogo;

    // Method onCreate adalah Siklus Hidup (Lifecycle) pertama yang dijalankan saat layar ini dibuka
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Menghubungkan variabel mAuth dengan layanan Firebase di Cloud
        mAuth = FirebaseAuth.getInstance();

        // --- PENGECEKAN SESI (SESSION MANAGEMENT) ---
        // Jika dosen tanya: "Kalau user sudah login sebelumnya, apakah harus login lagi?"
        // Jawab: "Tidak perlu, Pak/Bu. Kode ini mengecek jika getCurrentUser() tidak kosong,
        // maka user akan langsung dilempar (Intent) ke halaman Rekomendasi, bypass halaman login."
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, RekomendasiActivity.class));
            finish(); // Menghancurkan halaman login agar tidak bisa di-back
            return; // Menghentikan eksekusi baris kode di bawahnya
        }

        // --- INISIALISASI UI FORM LOGIN ---
        etEmail = findViewById(R.id.etEmailLogin);
        etPassword = findViewById(R.id.etPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        // --- INISIALISASI UI SPLASH OVERLAY ---
        splashOverlay = findViewById(R.id.splash_overlay);
        tvAppName = findViewById(R.id.tvSplashAppName);
        ivLogo = findViewById(R.id.ivSplashLogo);
        tvJargon = findViewById(R.id.tvSplashJargon);

        // 1. Tampilkan layar hitam overlay (yang menutupi form login)
        splashOverlay.setVisibility(View.VISIBLE);

        // 2. Panggil fungsi untuk menjalankan animasi elemen-elemen splash screen masuk ke layar
        jalankanAnimasiEntrance();

        // 3. Menggunakan Handler (Timer) untuk menahan Splash Screen agar tampil selama 2.5 detik (2500 ms).
        // Setelah waktunya habis, otomatis mengeksekusi fungsi jalankanAnimasiExit().
        new Handler().postDelayed(this::jalankanAnimasiExit, 2500);

        // --- LOGIKA TOMBOL LOGIN (EVENT LISTENER) ---
        // Menggunakan Lambda Expression (v -> {}) untuk menggantikan penulisan onClickListener konvensional yang panjang
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Validasi keamanan dasar: Tidak boleh ada kolom yang kosong
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- PROSES ASYNCHRONOUS FIREBASE LOGIN ---
            // Proses pengecekan email & password ke server Google berjalan di background (Asynchronous)
            // agar aplikasi tidak "freeze/hang" saat menunggu balasan jaringan internet.
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        // Jika server Google membalas "Sukses"
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show();
                            // Lempar user ke halaman Dashboard/Rekomendasi
                            startActivity(new Intent(LoginActivity.this, RekomendasiActivity.class));
                            finish();
                        } else {
                            // Jika gagal (misal: password salah, email tidak terdaftar, no internet)
                            Toast.makeText(this, "Login Gagal: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // --- LOGIKA PINDAH KE HALAMAN REGISTER ---
        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    // --- FUNGSI KUSTOM: ANIMASI MASUK (ENTRANCE) ---
    private void jalankanAnimasiEntrance() {
        // Efek OvershootInterpolator: Memberikan efek fisika seperti pegas (memantul sedikit saat sampai tujuan)

        // 1. Logo awalnya transparan (alpha 0) memudar jadi jelas (alpha 1) dan sedikit membesar (scale 1.1)
        ivLogo.animate().alpha(1f).scaleX(1.1f).scaleY(1.1f).setDuration(400).setInterpolator(new OvershootInterpolator()).start();

        // 2. Nama Aplikasi awalnya digeser ke atas (-50), lalu diturunkan ke posisi aslinya (0) sambil memudar masuk.
        // Diberi delay 300ms agar munculnya berurutan setelah logo.
        tvAppName.setTranslationY(-50f);
        tvAppName.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(300).setInterpolator(new OvershootInterpolator()).start();

        // 3. Jargon memudar masuk paling akhir (delay 600ms)
        tvJargon.animate().alpha(1f).setDuration(600).setStartDelay(600).start();
    }

    // --- FUNGSI KUSTOM: ANIMASI KELUAR (EXIT) ---
    private void jalankanAnimasiExit() {
        // Efek AnticipateInterpolator: Memberikan efek fisika seperti ancang-ancang sebelum bergerak mundur/menghilang

        // Animasi: Layar overlay splash memudar menjadi transparan (alpha 0) sambil perlahan melakukan zoom (scale 1.1)
        splashOverlay.animate()
                .alpha(0f)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(600)
                .setInterpolator(new AnticipateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    // Listener ini memantau kapan persisnya animasi durasi 600ms ini Selesai.
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        // SETELAH animasi selesai, HAPUS layar overlay secara fisik dari UI (GONE)
                        // Jika tidak di-GONE, form login di bawahnya tidak akan bisa diklik/disentuh.
                        splashOverlay.setVisibility(View.GONE);
                    }
                })
                .start();
    }
}