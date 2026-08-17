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

public class EditDataActivity extends AppCompatActivity {

    // --- DEKLARASI VARIABEL UI ---
    private EditText etNama, etKategori;
    private RadioGroup rgPlatform;
    private Spinner spinCPU, spinMobo, spinVGA, spinRAM, spinSSD, spinHDD, spinCooler, spinPSU, spinCasing, spinFan;
    private TextView tvTotalHarga, btnBack;
    private Button btnUpdate, btnDelete;

    // --- DEKLARASI FIREBASE & VARIABEL PENDUKUNG ---
    private DatabaseReference dbComponents, dbRakitan;
    private long totalHarga = 0; // Menyimpan total harga rakitan secara real-time
    private String keyID; // Menyimpan ID unik dari rakitan yang sedang di-edit

    // Variabel untuk menyimpan data rakitan lama yang dilempar dari MainActivity
    private String sCpu, sMobo, sVga, sRam, sSsd, sHdd, sCooler, sPsu, sCasing, sFan;

    // --- DEKLARASI ARRAY LIST UNTUK SPINNER (DROPDOWN) ---
    // masterCPU & masterMobo: Menyimpan SEMUA data CPU & Mobo (Intel + AMD) dari database sebelum difilter
    private ArrayList<KomponenItem> masterCPU = new ArrayList<>(), masterMobo = new ArrayList<>();
    // listCPU & listMobo: Menyimpan data CPU & Mobo yang SUDAH DIFILTER sesuai platform yang dipilih (Intel/AMD)
    private ArrayList<KomponenItem> listCPU = new ArrayList<>(), listMobo = new ArrayList<>(), listVGA = new ArrayList<>(), listRAM = new ArrayList<>(), listSSD = new ArrayList<>(), listHDD = new ArrayList<>(), listCooler = new ArrayList<>(), listPSU = new ArrayList<>(), listCasing = new ArrayList<>(), listFan = new ArrayList<>();
    private ArrayAdapter<KomponenItem> adpCPU, adpMobo, adpVGA, adpRAM, adpSSD, adpHDD, adpCooler, adpPSU, adpCasing, adpFan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_data);

        inisialisasiUI(); // Memanggil fungsi untuk menghubungkan variabel dengan ID di XML
        siapkanAdapter(); // Memanggil fungsi untuk memasang wadah data (adapter) ke setiap Spinner

        // Mengarahkan koneksi ke tabel "Components" di Firebase (Gudang master komponen)
        dbComponents = FirebaseDatabase.getInstance().getReference("Components");

        // --- UPDATE PENTING: ISOLASI DATA BERDASARKAN UID USER ---
        // Mengambil User ID (UID) dari akun yang sedang login saat ini
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Mengarahkan koneksi ke tabel "Rakitan" -> "UID User" agar data tidak bercampur dengan user lain
        dbRakitan = FirebaseDatabase.getInstance().getReference("Rakitan").child(uid);

        // Menangkap data lama yang dikirim (Intent) dari MainActivity saat tombol Edit diklik
        keyID = getIntent().getStringExtra("KEY");
        etNama.setText(getIntent().getStringExtra("NAMA"));
        etKategori.setText(getIntent().getStringExtra("KATEGORI"));

        // Menyimpan status komponen lama ke dalam variabel string
        sCpu = getIntent().getStringExtra("CPU"); sMobo = getIntent().getStringExtra("MOBO");
        sVga = getIntent().getStringExtra("VGA"); sRam = getIntent().getStringExtra("RAM");
        sSsd = getIntent().getStringExtra("SSD"); sHdd = getIntent().getStringExtra("HDD");
        sCooler = getIntent().getStringExtra("COOLER"); sPsu = getIntent().getStringExtra("PSU");
        sCasing = getIntent().getStringExtra("CASING"); sFan = getIntent().getStringExtra("FAN");

        // Menjalankan fungsi untuk menarik master komponen dari Firebase
        tarikDataDanSortir();

        // Listener: Jika user mengganti pilihan radio button (Intel/AMD)
        rgPlatform.setOnCheckedChangeListener((group, checkedId) -> {
            filterPlatform((checkedId == R.id.rbIntelEdit) ? "Intel" : "AMD");
        });

        // Listener: Aksi ketika tombol Update & Delete diklik
        btnUpdate.setOnClickListener(v -> updateDatabase());
        btnDelete.setOnClickListener(v -> hapusData());
    }

    // Fungsi untuk menghubungkan variabel Java dengan ID komponen yang ada di XML layout
    private void inisialisasiUI() {
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish()); // Menutup halaman ini dan kembali ke halaman sebelumnya

        etNama = findViewById(R.id.etEditNama);
        etKategori = findViewById(R.id.etEditKategori);
        rgPlatform = findViewById(R.id.rgPlatformEdit);
        tvTotalHarga = findViewById(R.id.tvTotalHargaEdit);
        btnUpdate = findViewById(R.id.btnUpdateEdit);
        btnDelete = findViewById(R.id.btnDeleteEdit);

        spinCPU = findViewById(R.id.spinCPUEdit);
        spinMobo = findViewById(R.id.spinMoboEdit);
        spinVGA = findViewById(R.id.spinVGAEdit);
        spinRAM = findViewById(R.id.spinRAMEdit);
        spinSSD = findViewById(R.id.spinSSDEdit);
        spinHDD = findViewById(R.id.spinHDDEdit);
        spinCooler = findViewById(R.id.spinCoolerEdit);
        spinPSU = findViewById(R.id.spinPSUEdit);
        spinCasing = findViewById(R.id.spinCasingEdit);
        spinFan = findViewById(R.id.spinFanEdit);

        // Membuat pendeteksi perubahan (listener) massal.
        // Jika user memilih item baru di spinner mana pun, otomatis jalankan fungsi hitungTotalHarga()
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { hitungTotalHarga(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        Spinner[] sArr = {spinCPU, spinMobo, spinVGA, spinRAM, spinSSD, spinHDD, spinCooler, spinPSU, spinCasing, spinFan};
        for (Spinner s : sArr) s.setOnItemSelectedListener(listener);
    }

    // Fungsi untuk menginisialisasi ArrayAdapter dan memasangkannya ke masing-masing Spinner
    private void siapkanAdapter() {
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

    // Fungsi utama untuk menarik data komponen mentah dari Firebase Database "Components"
    private void tarikDataDanSortir() {
        dbComponents.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Melakukan perulangan (looping) untuk membaca seluruh isi data komponen
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String nama = ds.child("nama").getValue(String.class);
                    Long harga = ds.child("harga").getValue(Long.class);
                    String kategori = ds.child("kategori").getValue(String.class);
                    String brand = ds.child("brand").getValue(String.class);

                    // Memasukkan data mentah ke dalam ArrayList yang sesuai berdasarkan kategorinya
                    if (nama != null && harga != null && kategori != null) {
                        KomponenItem item = new KomponenItem(nama, harga, brand);
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

                // Mengurutkan harga dari murah ke mahal dan menambahkan opsi "Pilih (Rp 0)" di paling atas
                urutkan(listVGA, "Kartu Grafis", adpVGA); urutkan(listRAM, "RAM", adpRAM); urutkan(listSSD, "SSD", adpSSD); urutkan(listHDD, "HDD", adpHDD); urutkan(listCooler, "Cooler", adpCooler); urutkan(listPSU, "PSU", adpPSU); urutkan(listCasing, "Casing", adpCasing); urutkan(listFan, "Fan", adpFan);

                // Mengecek komponen CPU lama milik user, apakah dia pakai AMD atau Intel?
                // Lalu menekan (check) tombol RadioButton secara otomatis sesuai kubu platformnya.
                if (sCpu != null && sCpu.contains("AMD")) {
                    rgPlatform.check(R.id.rbAMDEdit);
                    filterPlatform("AMD");
                } else {
                    rgPlatform.check(R.id.rbIntelEdit);
                    filterPlatform("Intel");
                }

                // Setelah data komponen masuk ke Spinner, kita atur otomatis pilihan spinner-nya
                // agar menunjuk ke komponen lama milik user yang akan di-edit
                setOtomatis(spinVGA, sVga); setOtomatis(spinRAM, sRam);
                setOtomatis(spinSSD, sSsd); setOtomatis(spinHDD, sHdd);
                setOtomatis(spinCooler, sCooler); setOtomatis(spinPSU, sPsu);
                setOtomatis(spinCasing, sCasing); setOtomatis(spinFan, sFan);

                // Menghitung ulang total harga berdasarkan pilihan yang otomatis ter-set di atas
                hitungTotalHarga();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Fungsi untuk menyaring (filter) isi list CPU dan Mobo berdasarkan kubu (Intel atau AMD)
    private void filterPlatform(String platform) {
        listCPU.clear(); listMobo.clear();
        for (KomponenItem i : masterCPU) if (i.brand != null && i.brand.equalsIgnoreCase(platform)) listCPU.add(i);
        for (KomponenItem i : masterMobo) if (i.brand != null && i.brand.equalsIgnoreCase(platform)) listMobo.add(i);

        urutkan(listCPU, "Prosesor " + platform, adpCPU); urutkan(listMobo, "Motherboard " + platform, adpMobo);

        // Setelah list difilter, atur ulang posisi default pilihan spinner ke komponen lamanya
        setOtomatis(spinCPU, sCpu);
        setOtomatis(spinMobo, sMobo);

        hitungTotalHarga();
    }

    // Fungsi pembantu untuk mengurutkan harga komponen dari yang termurah (Ascending)
    private void urutkan(ArrayList<KomponenItem> list, String def, ArrayAdapter adp) {
        Collections.sort(list, (a, b) -> Long.compare(a.harga, b.harga));
        list.add(0, new KomponenItem("Pilih " + def + " (Rp 0)", 0, ""));
        adp.notifyDataSetChanged();
    }

    // Fungsi pintar (Smart Matching) untuk mencari nama komponen lama di dalam daftar Spinner,
    // lalu mengatur posisi (setSelection) Spinner ke komponen tersebut secara otomatis
    private void setOtomatis(Spinner spinner, String teksLama) {
        if (teksLama == null || teksLama.isEmpty()) return;

        for (int i = 0; i < spinner.getCount(); i++) {
            String itemSpinner = spinner.getItemAtPosition(i).toString();
            // Mengecek apakah string sama persis, ATAU mengandung nama (mencegah error jika format penulisan beda)
            if (itemSpinner.equals(teksLama) || itemSpinner.contains(teksLama) || teksLama.contains(itemSpinner.split(" -")[0])) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    // Fungsi untuk melooping ke-10 spinner, mengambil nilai harganya, dan menjumlahkannya jadi total akhir
    private void hitungTotalHarga() {
        totalHarga = 0;
        Spinner[] sArr = {spinCPU, spinMobo, spinVGA, spinRAM, spinSSD, spinHDD, spinCooler, spinPSU, spinCasing, spinFan};
        for (Spinner s : sArr) if (s.getSelectedItem() != null) totalHarga += ((KomponenItem) s.getSelectedItem()).harga;

        // Memformat angka ribuan ke format Rupiah Indonesia (contoh: 1000000 -> 1.000.000)
        NumberFormat nf = NumberFormat.getInstance(new Locale("id", "ID"));
        tvTotalHarga.setText("Rp " + nf.format(totalHarga));
    }

    // Fungsi untuk menimpa/menyimpan data rakitan yang sudah diubah (Update) kembali ke Firebase
    private void updateDatabase() {
        String nama = etNama.getText().toString().trim();
        if (nama.isEmpty() || totalHarga == 0) {
            Toast.makeText(this, "Nama dan komponen wajib diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Membungkus seluruh inputan user ke dalam sebuah objek KomponenModel
        KomponenModel up = new KomponenModel(nama, "Custom BuildWJax Build", etKategori.getText().toString().trim(), String.valueOf(totalHarga),
                spinCPU.getSelectedItem().toString(), spinMobo.getSelectedItem().toString(),
                spinVGA.getSelectedItem().toString(), spinRAM.getSelectedItem().toString(),
                spinSSD.getSelectedItem().toString(), spinHDD.getSelectedItem().toString(),
                spinCooler.getSelectedItem().toString(), spinPSU.getSelectedItem().toString(),
                spinCasing.getSelectedItem().toString(), spinFan.getSelectedItem().toString());

        // Menyimpan objek tersebut ke Firebase di cabang berdasarkan ID rakitan (keyID) yang sedang diedit
        dbRakitan.child(keyID).setValue(up).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Data Berhasil Diupdate!", Toast.LENGTH_SHORT).show();
            finish(); // Tutup halaman edit jika update berhasil
        });
    }

    // Fungsi untuk menghapus permanen data rakitan ini dari Firebase Database
    private void hapusData() {
        dbRakitan.child(keyID).removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Data Berhasil Dihapus!", Toast.LENGTH_SHORT).show();
            finish(); // Tutup halaman edit jika delete berhasil
        });
    }

    // Kelas kerangka kecil (Model/POJO) untuk mendefinisikan struktur satu barang komponen di dalam Spinner
    class KomponenItem {
        String nama; long harga; String brand;

        public KomponenItem(String n, long h, String b) { nama = n; harga = h; brand = b; }

        // Mengubah format tampilan teks yang akan muncul di dropdown Spinner
        // Contoh keluaran: "RTX 4060 - Rp 5.000.000"
        @Override public String toString() {
            if (harga == 0) return nama;
            return nama + " - Rp " + NumberFormat.getInstance(new Locale("id", "ID")).format(harga);
        }
    }
}