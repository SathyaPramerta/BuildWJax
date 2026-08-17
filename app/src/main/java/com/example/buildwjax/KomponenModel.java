package com.example.buildwjax;
import com.google.firebase.database.Exclude;

// Kelas KomponenModel adalah sebuah "Cetak Biru" (Blueprint) atau POJO (Plain Old Java Object).
// Kelas ini berfungsi sebagai wadah atau cetakan untuk menyatukan berbagai data spesifikasi PC
// menjadi satu objek utuh sebelum dikirim ke Firebase, atau saat ditarik dari Firebase.
public class KomponenModel {

    // @Exclude artinya variabel 'key' ini TIDAK AKAN ikut tersimpan ke dalam database Firebase.
    // Variabel ini hanya dipakai secara lokal di dalam aplikasi untuk menyimpan ID unik (Push Key)
    // dari sebuah rakitan agar kita tahu data persis mana yang akan di-edit atau dihapus.
    @Exclude private String key;

    // Deklarasi variabel publik untuk menyimpan informasi utama dari rakitan PC
    public String nama, brand, kategori, harga;

    // 10 Wadah penyimpan riwayat detail komponen yang dipilih user
    public String cpu, mobo, vga, ram, ssd, hdd, cooler, psu, casing, fan;

    // CONSTRUCTOR KOSONG (Sangat Penting!)
    // Jika dosen tanya, jawab: "Firebase membutuhkan constructor kosong ini agar bisa
    // melakukan proses deserialisasi (mengubah data JSON dari cloud menjadi objek Java)."
    public KomponenModel() {}

    // Constructor Pertama (Hanya informasi dasar)
    // Jika dosen tanya kenapa ada 2 constructor, jawab: "Ini adalah penerapan polimorfisme
    // yaitu konsep OVERLOADING di OOP. Kita bisa membuat objek dengan cara yang berbeda."
    public KomponenModel(String nama, String brand, String kategori, String harga) {
        this.nama = nama; this.brand = brand; this.kategori = kategori; this.harga = harga;
    }

    // Constructor Kedua yang super komplit (Untuk data baru)
    // Fungsi ini dipanggil oleh TambahDataActivity dan EditDataActivity saat user menyimpan rakitan.
    // Semua data inputan disuntikkan ke sini sekaligus untuk dirakit menjadi satu objek utuh.
    public KomponenModel(String nama, String brand, String kategori, String harga,
                         String cpu, String mobo, String vga, String ram, String ssd,
                         String hdd, String cooler, String psu, String casing, String fan) {
        this.nama = nama; this.brand = brand; this.kategori = kategori; this.harga = harga;
        this.cpu = cpu; this.mobo = mobo; this.vga = vga; this.ram = ram; this.ssd = ssd;
        this.hdd = hdd; this.cooler = cooler; this.psu = psu; this.casing = casing; this.fan = fan;
    }

    // PENERAPAN ENCAPSULATION (Kapsulasi)
    // Getter & Setter khusus untuk 'key' karena variabelnya bersifat private.
    // Mengizinkan kelas lain membaca/mengubah 'key' lewat pintu resmi, bukan akses langsung.
    @Exclude public String getKey() { return key; }
    @Exclude public void setKey(String key) { this.key = key; }

    // Fungsi-fungsi Getter (Pengambil nilai)
    // Berfungsi untuk membaca isi variabel dari objek ini saat dibutuhkan oleh kelas lain
    // (contohnya saat datanya ingin ditampilkan di KomponenAdapter).
    public String getNama() { return nama; }
    public String getBrand() { return brand; }
    public String getKategori() { return kategori; }
    public String getHarga() { return harga; }
}