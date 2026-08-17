package com.example.buildwjax;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// Kelas RekomendasiActivity berfungsi sebagai halaman "Katalog Cepat" atau Template.
// Di sini user bisa langsung memilih paket rakitan PC jadi (Low, Mid, High) tanpa harus memilih satu-satu.
public class RekomendasiActivity extends AppCompatActivity {

    // Variabel penunjuk jalur (referensi) ke database Firebase
    private DatabaseReference dbRakitan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekomendasi);

        // --- 1. LOGIKA TOMBOL LOGOUT ---
        // Tombol logout ditempatkan di halaman pertama setelah login agar user mudah keluar sesi
        TextView btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            // Perintah ke Firebase untuk menghancurkan token login aktif di perangkat ini
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(RekomendasiActivity.this, LoginActivity.class));
            finish(); // Hancurkan halaman ini agar user tidak bisa kembali lewat tombol 'Back'
        });

        // --- 2. PERSIAPAN DATABASE & ISOLASI DATA (MULTI-USER) ---
        // Mengambil ID unik (UID) dari user yang berhasil masuk
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Mengarahkan "panah" database ke folder khusus milik user tersebut: "Rakitan" -> "UID"
        dbRakitan = FirebaseDatabase.getInstance().getReference("Rakitan").child(uid);

        // --- 3. INISIALISASI TOMBOL PAKET RAKITAN ---
        Button btnLow = findViewById(R.id.btnLow);
        Button btnMid = findViewById(R.id.btnMid);
        Button btnHigh = findViewById(R.id.btnHigh);
        Button btnCustom = findViewById(R.id.btnCustom);

        // --- 4. AKSI TOMBOL PAKET LOW-END ---
        // Saat diklik, aplikasi akan langsung melakukan INSTANSIASI (membuat objek baru)
        // KomponenModel khusus "Template Low" dan mengirimkannya ke fungsi simpanRekomendasi.
        btnLow.setOnClickListener(v -> {
            simpanRekomendasi(new KomponenModel(
                    "Entry-Level Starter", "BuildWJax Recs", "Gaming", "5570000",
                    "AMD Ryzen 5 4600G", "Gigabyte A520M K V2", "Pilih Kartu Grafis (Rp 0)", "Kingston FURY Beast 16GB",
                    "ADATA XPG SX8200 Pro 512GB", "Pilih HDD (Rp 0)", "Deepcool AK400", "FSP HV PRO 85+ 550W",
                    "Cube Gaming Byron (mATX)", "Arctic P12 PWM PST 120mm"
            ));
        });

        // --- 5. AKSI TOMBOL PAKET MID-RANGE ---
        btnMid.setOnClickListener(v -> {
            simpanRekomendasi(new KomponenModel(
                    "Sweet Spot Gaming", "BuildWJax Recs", "Gaming/Editing", "16350000",
                    "Intel Core i5-12400F", "MSI PRO B760M-A WIFI", "ASUS DUAL GeForce RTX 4060", "TeamGroup T-Force Delta RGB 16GB",
                    "Samsung 980 PRO 1TB", "Pilih HDD (Rp 0)", "Thermalright Peerless Assassin 120 SE", "Corsair RM750e 750W",
                    "Montech AIR 100 ARGB", "Deepcool FC120 ARGB"
            ));
        });

        // --- 6. AKSI TOMBOL PAKET HIGH-END ---
        btnHigh.setOnClickListener(v -> {
            simpanRekomendasi(new KomponenModel(
                    "Ultimate Beast PC", "BuildWJax Recs", "Hardcore Gaming", "49000000",
                    "AMD Ryzen 7 7800X3D", "Gigabyte X670E AORUS MASTER", "ASUS ROG Strix GeForce RTX 4080", "Corsair Vengeance 32GB",
                    "WD Black SN850X 2TB", "Pilih HDD (Rp 0)", "NZXT Kraken 240 RGB", "Seasonic Focus GX-850 850W",
                    "Lian Li O11 Dynamic EVO", "Lian Li UNI FAN SL-INFINITY 120"
            ));
        });

        // --- 7. AKSI TOMBOL CUSTOM PC ---
        // Jika user tidak mau memakai template rakitan dan ingin meracik sendiri,
        // langsung arahkan ke Dashboard (MainActivity) tanpa menyimpan data apapun.
        btnCustom.setOnClickListener(v -> keDashboard());
    }

    // --- FUNGSI KUSTOM: SIMPAN TEMPLATE KE CLOUD DATABASE ---
    // Fungsi ini menerima parameter berupa objek 'rakitan' yang datanya sudah diisi dari klik tombol di atas.
    private void simpanRekomendasi(KomponenModel rakitan) {
        // .push().getKey() berfungsi menghasilkan ID acak, unik, dan aman (Primary Key) dari Firebase.
        // Contoh bentuk ID: "-Nxy12345ABCDxyz..."
        String idRakitan = dbRakitan.push().getKey();

        if (idRakitan != null) {
            // .setValue() adalah proses Asynchronous (berjalan di background) untuk melempar objek ke Cloud.
            dbRakitan.child(idRakitan).setValue(rakitan)
                    .addOnSuccessListener(aVoid -> {
                        // Jika data sukses mendarat di server Google, tampilkan Toast dan pindah ke Dashboard
                        Toast.makeText(this, "Rakitan Berhasil Ditambahkan!", Toast.LENGTH_SHORT).show();
                        keDashboard();
                    })
                    .addOnFailureListener(e -> {
                        // Jika proses upload gagal (misal: internet mati), tangkap pesan errornya dan tampilkan
                        Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // --- FUNGSI KUSTOM: PINDAH KE DASHBOARD ---
    // Dipisah menjadi fungsi tersendiri (Clean Code) karena dipanggil berulang kali di berbagai tempat
    private void keDashboard() {
        startActivity(new Intent(RekomendasiActivity.this, MainActivity.class));
        finish();
    }
}