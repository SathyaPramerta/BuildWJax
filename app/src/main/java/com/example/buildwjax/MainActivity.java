package com.example.buildwjax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

// Kelas MainActivity adalah halaman Dashboard utama yang menampilkan riwayat rakitan (Custom PC) user.
public class MainActivity extends AppCompatActivity {

    // --- DEKLARASI VARIABEL UTAMA ---
    // RecyclerView: Komponen UI canggih Android untuk menampilkan daftar panjang secara efisien (menghemat memori)
    private RecyclerView rvKomponen;
    private KomponenAdapter adapter; // Jembatan penghubung data ke RecyclerView
    private ArrayList<KomponenModel> list; // Wadah lokal penyimpan kumpulan objek rakitan
    private DatabaseReference database; // Jalur koneksi ke database Firebase
    private FloatingActionButton fabTambah; // Tombol bulat melayang di pojok kanan bawah
    private ProgressBar progressBar; // Animasi loading berputar sebelum data muncul

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- 1. LOGIKA TOMBOL LOGOUT (KEAMANAN SESI) ---
        TextView btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            // Memerintahkan Firebase Auth untuk menghancurkan sesi login (token) saat ini
            FirebaseAuth.getInstance().signOut();
            // Lempar user kembali ke halaman gerbang utama (LoginActivity)
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish(); // Tutup dashboard agar tidak bisa diakses lewat tombol 'Back' bawaan HP
        });

        // --- 2. LOGIKA TOMBOL KEMBALI KE REKOMENDASI ---
        Button btnBackToRec = findViewById(R.id.btnBackToRec);
        btnBackToRec.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RekomendasiActivity.class));
            finish();
        });

        // --- 3. INISIALISASI UI & RECYCLERVIEW ---
        progressBar = findViewById(R.id.progressBar);
        rvKomponen = findViewById(R.id.rvKomponen);

        // Optimasi: Memberitahu sistem bahwa ukuran kerangka list tidak akan berubah-ubah
        rvKomponen.setHasFixedSize(true);
        // Mengatur susunan daftar menjadi vertikal (dari atas ke bawah)
        rvKomponen.setLayoutManager(new LinearLayoutManager(this));

        // Menyiapkan wadah kosong (ArrayList) dan memasangkannya ke Adapter
        list = new ArrayList<>();
        adapter = new KomponenAdapter(list);
        rvKomponen.setAdapter(adapter);

        // --- 4. LOGIKA TOMBOL FAB (TAMBAH DATA) ---
        fabTambah = findViewById(R.id.fabTambah);
        fabTambah.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TambahDataActivity.class);
            startActivity(intent);
        });

        // --- 5. TARIK DATA REALTIME DARI FIREBASE ---
        // " mengambil UID (User ID) unik milik user yang sedang login, lalu menjadikannya
        // sebagai 'Kamar Khusus' di dalam node 'Rakitan' menggunakan child(uid)."
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        database = FirebaseDatabase.getInstance().getReference("Rakitan").child(uid);

        // addValueEventListener: Berfungsi sebagai "Kamera Pengintai" Real-time.
        // Jika ada perubahan data di Firebase (tambah, edit, hapus), fungsi ini OTOMATIS tertrigger
//==         // tanpa perlu user me-refresh aplikasi. Ini bedanya dengan addListenerForSingleValueEvent.
        database.addValueEventListener(new ValueEventListener() {

            // onDataChange akan dieksekusi saat aplikasi berhasil membaca data dari Firebase
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Bersihkan list lokal terlebih dahulu agar data tidak ganda saat refresh
                list.clear();

//==                // Melakukan perulangan (Looping) untuk membongkar setiap paket data dari cloud
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // MENGUBAH DATA JSON MENJADI OBJEK JAVA (Deserialization)
                    // Firebase otomatis mencocokkan nama variabel di cloud dengan yang ada di KomponenModel
                    KomponenModel model = dataSnapshot.getValue(KomponenModel.class);

                    if (model != null) {
                        // Menyisipkan ID Unik (Key) ke dalam objek agar nanti bisa diedit/dihapus
                        model.setKey(dataSnapshot.getKey());
                        list.add(model); // Masukkan objek utuh ke dalam daftar
                    }
                }
                // Memberi tahu adapter bahwa data telah berubah, tolong perbarui tampilan di layar!
                adapter.notifyDataSetChanged();
                // Matikan animasi loading putar-putar karena data sudah siap
                progressBar.setVisibility(View.GONE);
            }

            // onCancelled akan dieksekusi jika terjadi gagal baca (misal: internet putus, atau akses ditolak rules Firebase)
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Koneksi Terputus: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}