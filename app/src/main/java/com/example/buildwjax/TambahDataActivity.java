    package com.example.buildwjax;

    import android.os.Bundle;
    import android.view.View;
    import android.widget.AdapterView;
    import android.widget.ArrayAdapter;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.RadioGroup;
    import android.widget.Spinner;
    import android.widget.TextView;
    import android.widget.Toast;
    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AppCompatActivity;

    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;
    import java.text.NumberFormat;
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.Locale;

    // Kelas TambahDataActivity berfungsi sebagai form input bagi user
    // untuk meracik komponen PC (Custom Build) secara manual dari nol.
    public class TambahDataActivity extends AppCompatActivity {

        // --- DEKLARASI VARIABEL UI ---
        private EditText etNama, etKategori;
        private RadioGroup rgPlatform;
        private Spinner spinCPU, spinMobo, spinVGA, spinRAM, spinSSD, spinHDD, spinCooler, spinPSU, spinCasing, spinFan;
        private TextView tvTotalHarga, btnBack;
        private Button btnSimpan;

        // --- DEKLARASI FIREBASE ---
        // dbComponents: Jalur untuk membaca daftar (katalog) suku cadang
        // dbRakitan: Jalur untuk menyimpan hasil akhir rakitan user
        private DatabaseReference dbComponents, dbRakitan;
        private long totalHarga = 0;

        // --- MANAJEMEN MEMORI KOMPONEN (ARRAYLIST) ---
        // masterCPU & masterMobo: Menyimpan "Semua" data prosesor & motherboard (Gabungan Intel + AMD)
        // Tujuannya agar aplikasi tidak perlu download ulang dari internet saat user ganti-ganti kubu (Intel/AMD)
        private ArrayList<KomponenItem> masterCPU = new ArrayList<>();
        private ArrayList<KomponenItem> masterMobo = new ArrayList<>();

        // listCPU & listMobo: Daftar yang ditampilkan di layar (Hanya Intel saja, atau AMD saja)
        private ArrayList<KomponenItem> listCPU = new ArrayList<>(), listMobo = new ArrayList<>();

        // Daftar komponen netral (bisa dipakai Intel/AMD)
        private ArrayList<KomponenItem> listVGA = new ArrayList<>(), listRAM = new ArrayList<>();
        private ArrayList<KomponenItem> listSSD = new ArrayList<>(), listHDD = new ArrayList<>();
        private ArrayList<KomponenItem> listCooler = new ArrayList<>(), listPSU = new ArrayList<>();
        private ArrayList<KomponenItem> listCasing = new ArrayList<>(), listFan = new ArrayList<>();

        // Adapter penengah antara ArrayList dan Spinner (Dropdown)
        private ArrayAdapter<KomponenItem> adpCPU, adpMobo, adpVGA, adpRAM, adpSSD, adpHDD, adpCooler, adpPSU, adpCasing, adpFan;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_tambah_data);

            inisialisasiUI();
            siapkanAdapter();

            // Mengarahkan panah ke "Gudang Komponen" di Cloud
            dbComponents = FirebaseDatabase.getInstance().getReference("Components");

            // --- UPDATE PENTING: ISOLASI DATA BERDASARKAN UID USER ---
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // Mengarahkan panah penyimpanan ke "Kamar Pribadi" user di node Rakitan
            dbRakitan = FirebaseDatabase.getInstance().getReference("Rakitan").child(uid);

            // Menjalankan fungsi untuk mendownload katalog komponen
            tarikDataDanSortir();

            // --- LOGIKA DYNAMIC FILTERING (Radio Button Intel/AMD) ---
//            // Listener ini akan langsung bereaksi saat user mengganti pilihan platform
            rgPlatform.setOnCheckedChangeListener((group, checkedId) -> {
                String platformTerpilih = (checkedId == R.id.rbIntel) ? "Intel" : "AMD";
                filterPlatform(platformTerpilih); // Jalankan fungsi penyaring lokal
            });

            // Aksi simpan ke Firebase
            btnSimpan.setOnClickListener(v -> simpanKeDatabase());
        }

        private void inisialisasiUI() {
            // Logika tombol kembali
            btnBack = findViewById(R.id.btnBack);
            btnBack.setOnClickListener(v -> finish());

            etNama = findViewById(R.id.etNama);
            etKategori = findViewById(R.id.etKategori);
            rgPlatform = findViewById(R.id.rgPlatform);
            tvTotalHarga = findViewById(R.id.tvTotalHarga);
            btnSimpan = findViewById(R.id.btnSimpan);

            spinCPU = findViewById(R.id.spinCPU);
            spinMobo = findViewById(R.id.spinMobo);
            spinVGA = findViewById(R.id.spinVGA);
            spinRAM = findViewById(R.id.spinRAM);
            spinSSD = findViewById(R.id.spinSSD);
            spinHDD = findViewById(R.id.spinHDD);
            spinCooler = findViewById(R.id.spinCooler);
            spinPSU = findViewById(R.id.spinPSU);
            spinCasing = findViewById(R.id.spinCasing);
            spinFan = findViewById(R.id.spinFan);

            // --- OPTIMASI KODE (CLEAN CODE) ---
            // Daripada menulis setOnItemSelectedListener 10 kali, kita buat 1 Listener
            // lalu kita loop/pasangkan ke 10 spinner sekaligus. Jauh lebih rapi!
            AdapterView.OnItemSelectedListener listenerHitung = new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { hitungTotalHarga(); }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            };

            Spinner[] spinners = {spinCPU, spinMobo, spinVGA, spinRAM, spinSSD, spinHDD, spinCooler, spinPSU, spinCasing, spinFan};
            for (Spinner s : spinners) {
                s.setOnItemSelectedListener(listenerHitung);
            }
        }

        private void siapkanAdapter() {
            // Memasangkan Array data ke dalam UI Spinner
            adpCPU = new ArrayAdapter<>(this, R.layout.item_spinner, listCPU); spinCPU.setAdapter(adpCPU);
            adpMobo = new ArrayAdapter<>(this, R.layout.item_spinner, listMobo); spinMobo.setAdapter(adpMobo);
            adpVGA = new ArrayAdapter<>(this, R.layout.item_spinner, listVGA); spinVGA.setAdapter(adpVGA);
            adpRAM = new ArrayAdapter<>(this, R.layout.item_spinner, listRAM); spinRAM.setAdapter(adpRAM);
            adpSSD = new ArrayAdapter<>(this, R.layout.item_spinner, listSSD); spinSSD.setAdapter(adpSSD);
            adpHDD = new ArrayAdapter<>(this, R.layout.item_spinner, listHDD); spinHDD.setAdapter(adpHDD);
            adpCooler = new ArrayAdapter<>(this, R.layout.item_spinner, listCooler); spinCooler.setAdapter(adpCooler);
            adpPSU = new ArrayAdapter<>(this, R.layout.item_spinner, listPSU); spinPSU.setAdapter(adpPSU);
            adpCasing = new ArrayAdapter<>(this, R.layout.item_spinner, listCasing); spinCasing.setAdapter(adpCasing);
            adpFan = new ArrayAdapter<>(this, R.layout.item_spinner, listFan); spinFan.setAdapter(adpFan);
        }

        private void tarikDataDanSortir() {
            // addListenerForSingleValueEvent: Berbeda dengan di Dashboard, di sini kita hanya
            // butuh baca data SATU KALI saja (tidak memantau terus-menerus), sehingga hemat kuota internet/memori.
            dbComponents.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Membongkar JSON dari Firebase menjadi Data per komponen
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String nama = ds.child("nama").getValue(String.class);
                        Long harga = ds.child("harga").getValue(Long.class);
                        String kategori = ds.child("kategori").getValue(String.class);
                        String brand = ds.child("brand").getValue(String.class);

                        if (nama != null && harga != null && kategori != null) {
                            KomponenItem item = new KomponenItem(nama, harga, brand);
                            // Mengelompokkan komponen berdasarkan kategorinya menggunakan Switch-Case
                            switch (kategori) {
                                case "CPU": masterCPU.add(item); break;
                                case "Motherboard": masterMobo.add(item); break;
                                case "VGA": listVGA.add(item); break;
                                case "RAM": listRAM.add(item); break;
                                case "Storage_SSD": listSSD.add(item); break;
                                case "Storage_HDD": listHDD.add(item); break;
                                case "CPU_Cooler": listCooler.add(item); break;
                                case "PSU": listPSU.add(item); break;
                                case "Casing": listCasing.add(item); break;
                                case "Fan": listFan.add(item); break;
                            }
                        }
                    }

                    // Setelah list terkumpul, urutkan harganya & tambahkan opsi "Pilih (Rp 0)" di baris teratas
                    urutkanDanSetDefault(listVGA, "Pilih Kartu Grafis", adpVGA);
                    urutkanDanSetDefault(listRAM, "Pilih RAM", adpRAM);
                    urutkanDanSetDefault(listSSD, "Pilih SSD", adpSSD);
                    urutkanDanSetDefault(listHDD, "Pilih HDD", adpHDD);
                    urutkanDanSetDefault(listCooler, "Pilih Cooler", adpCooler);
                    urutkanDanSetDefault(listPSU, "Pilih PSU", adpPSU);
                    urutkanDanSetDefault(listCasing, "Pilih Casing", adpCasing);
                    urutkanDanSetDefault(listFan, "Pilih Fan", adpFan);

                    // Secara default aplikasi memicu filter ke Intel pertama kali dibuka
                    filterPlatform("Intel");
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        // --- LOGIKA FILTERING (SANGAT PENTING JIKA DITANYA DOSEN) ---
        // Fungsi ini tidak meminta data ke internet, melainkan mengolah data 'master' yang sudah ada di memori HP.
        // Ini membuat perpindahan Intel/AMD sangat instan tanpa lag.
        private void filterPlatform(String platform) {
            // Kosongkan list layar
            listCPU.clear();
            listMobo.clear();

            // Cari dari master data, masukkan ke list layar hanya yang brand-nya cocok (Intel/AMD)
            for (KomponenItem item : masterCPU) {
                if (item.brand != null && item.brand.equalsIgnoreCase(platform)) listCPU.add(item);
            }
            for (KomponenItem item : masterMobo) {
                if (item.brand != null && item.brand.equalsIgnoreCase(platform)) listMobo.add(item);
            }

            // Urutkan ulang list yang sudah difilter
            urutkanDanSetDefault(listCPU, "Pilih Prosesor " + platform, adpCPU);
            urutkanDanSetDefault(listMobo, "Pilih Motherboard " + platform, adpMobo);

            // Hitung ulang harga jika pilihan komponen terre-set akibat beda platform
            hitungTotalHarga();
        }

        private void urutkanDanSetDefault(ArrayList<KomponenItem> list, String teksDefault, ArrayAdapter adapter) {
            // Collection.sort digunakan untuk mengurutkan harga komponen Ascending (Murah -> Mahal)
            Collections.sort(list, (a, b) -> Long.compare(a.harga, b.harga));
            // index 0 artinya posisi paling atas pada Dropdown
            list.add(0, new KomponenItem(teksDefault + " (Rp 0)", 0, ""));
            adapter.notifyDataSetChanged();
        }

        private void hitungTotalHarga() {
            totalHarga = 0;
            Spinner[] spinners = {spinCPU, spinMobo, spinVGA, spinRAM, spinSSD, spinHDD, spinCooler, spinPSU, spinCasing, spinFan};

            for (Spinner s : spinners) {
                // Jika ada yang dipilih, tambahkan harganya ke totalHarga
                if (s.getSelectedItem() != null) {
                    totalHarga += ((KomponenItem) s.getSelectedItem()).harga;
                }
            }

            NumberFormat formatRupiah = NumberFormat.getInstance(new Locale("id", "ID"));
            tvTotalHarga.setText("Rp " + formatRupiah.format(totalHarga));
        }

        private void simpanKeDatabase() {
            String namaRakitan = etNama.getText().toString().trim();
            String kategori = etKategori.getText().toString().trim();

            if (namaRakitan.isEmpty() || totalHarga == 0) {
                Toast.makeText(this, "Nama Rakitan dan minimal 1 komponen wajib diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Meminta ID Random dari Firebase
            String idRakitan = dbRakitan.push().getKey();

            // Mengemas seluruh pilihan user ke dalam Objek KomponenModel
            KomponenModel rakitanBaru = new KomponenModel(
                    namaRakitan,
                    "Custom BuildWJax Build",
                    kategori,
                    String.valueOf(totalHarga),
                    spinCPU.getSelectedItem().toString(),
                    spinMobo.getSelectedItem().toString(),
                    spinVGA.getSelectedItem().toString(),
                    spinRAM.getSelectedItem().toString(),
                    spinSSD.getSelectedItem().toString(),
                    spinHDD.getSelectedItem().toString(),
                    spinCooler.getSelectedItem().toString(),
                    spinPSU.getSelectedItem().toString(),
                    spinCasing.getSelectedItem().toString(),
                    spinFan.getSelectedItem().toString()
            );

            if (idRakitan != null) {
                // Mengunggah Objek tersebut ke Cloud
                dbRakitan.child(idRakitan).setValue(rakitanBaru).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Rakitan Berhasil Disimpan!", Toast.LENGTH_SHORT).show();
                    finish(); // Tutup form dan kembali ke Dashboard
                });
            }
        }

        // --- INNER CLASS (OOP) ---
        // Class kecil di dalam class besar. Berfungsi merepresentasikan satu buah barang (item) dalam daftar dropdown.
        class KomponenItem {
            String nama; long harga; String brand;

            public KomponenItem(String nama, long harga, String brand) {
                this.nama = nama; this.harga = harga; this.brand = brand;
            }

            // OVERRIDE toString()
            // Method bawaan Java ini ditimpa (Override) agar Spinner Android menampilkan gabungan Nama dan Harga,
            // bukan menampilkan ID alamat memori objek yang sulit dibaca manusia.
            @Override public String toString() {
                if (harga == 0) return nama; // Jika harga 0 (untuk opsi 'Pilih...'), jangan tampilkan Rp 0
                return nama + " - Rp " + NumberFormat.getInstance(new Locale("id", "ID")).format(harga);
            }
        }
    }