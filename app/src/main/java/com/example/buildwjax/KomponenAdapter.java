package com.example.buildwjax;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

// Kelas KomponenAdapter bertugas sebagai "Jembatan" (Adapter) antara data rakitan (ArrayList)
// dengan tampilan daftar bergeser (RecyclerView) di layar utama.
public class KomponenAdapter extends RecyclerView.Adapter<KomponenAdapter.ViewHolder> {

    // Variabel penampung daftar data rakitan yang dikirim dari MainActivity
    private ArrayList<KomponenModel> listKomponen;

    // Constructor: Fungsi yang pertama kali dipanggil saat adapter ini dibuat.
    // Tugasnya menerima kiriman data ArrayList dari MainActivity dan memasukkannya ke variabel lokal.
    public KomponenAdapter(ArrayList<KomponenModel> listKomponen) {
        this.listKomponen = listKomponen;
    }

    // Fungsi ini bertugas menciptakan "wujud fisik" dari setiap baris item di daftar.
    // Ia akan membaca file layout XML (item_komponen.xml) dan mengubahnya menjadi View.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_komponen, parent, false);
        return new ViewHolder(view);
    }

    // Fungsi ini bertugas mengisi/menempelkan data (nama, harga, kategori) ke tampilan View
    // yang sudah dibuat di onCreateViewHolder pada urutan (position) tertentu.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Mengambil satu buah objek data rakitan berdasarkan urutannya (position)
        KomponenModel komponen = listKomponen.get(position);

        // Menempelkan teks Nama dan Kategori ke UI (Layar)
        holder.tvNama.setText(komponen.getNama());
        holder.tvKategori.setText(komponen.getKategori() + " | " + komponen.getBrand());

        // Blok Try-Catch untuk mengubah format angka mentah (String) menjadi format Rupiah (Rp 1.000.000)
        try {
            // Mengubah string harga menjadi tipe data angka panjang (Long)
            long hargaLong = Long.parseLong(komponen.getHarga());
            // Memformat angka tersebut menjadi standar regional Indonesia
            NumberFormat nf = NumberFormat.getInstance(new Locale("id", "ID"));
            holder.tvHarga.setText("Rp " + nf.format(hargaLong));
        } catch (Exception e) {
            // Jika terjadi error (misal datanya cacat/bukan angka murni), tampilkan apa adanya
            holder.tvHarga.setText("Rp " + komponen.getHarga());
        }

        // Memberikan aksi (Listener) jika SATU BARIS item (kartu rakitan) tersebut diklik oleh user
        holder.itemView.setOnClickListener(v -> {
            // Membuat "Kurir" (Intent) untuk berpindah dari halaman saat ini ke halaman EditDataActivity
            Intent intent = new Intent(v.getContext(), EditDataActivity.class);

            // Kurir membawa paketan data (sebagai tiket masuk) ke halaman Edit menggunakan putExtra
            intent.putExtra("KEY", komponen.getKey()); // ID Unik di Firebase agar tau data mana yang mau diupdate/hapus
            intent.putExtra("NAMA", komponen.getNama());
            intent.putExtra("KATEGORI", komponen.getKategori());

            // Bawa seluruh detail sejarah komponen pendukung agar spinner di halaman Edit otomatis terpilih
            intent.putExtra("CPU", komponen.cpu);
            intent.putExtra("MOBO", komponen.mobo);
            intent.putExtra("VGA", komponen.vga);
            intent.putExtra("RAM", komponen.ram);
            intent.putExtra("SSD", komponen.ssd);
            intent.putExtra("HDD", komponen.hdd);
            intent.putExtra("COOLER", komponen.cooler);
            intent.putExtra("PSU", komponen.psu);
            intent.putExtra("CASING", komponen.casing);
            intent.putExtra("FAN", komponen.fan);

            // Mengeksekusi perpindahan halaman beserta membawa semua data di atas
            v.getContext().startActivity(intent);
        });
    }

    // Fungsi untuk memberi tahu RecyclerView berapa jumlah total data yang harus ditampilkan.
    // Jika listKomponen kosong (size 0), maka layarnya tidak akan memunculkan apa-apa.
    @Override
    public int getItemCount() { return listKomponen.size(); }

    // Kelas ViewHolder ibarat "Kerangka Penyangga" yang bertugas mencari dan menyimpan ID dari elemen UI
    // (TextView dll) di dalam file XML item_komponen. Tujuannya agar aplikasi tidak perlu
    // mencari ID (findViewById) berulang kali setiap kali digeser, sehingga memori lebih irit.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama, tvKategori, tvHarga;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Menghubungkan variabel Java lokal dengan ID dari layout item_komponen.xml
            tvNama = itemView.findViewById(R.id.tvNamaKomponen);
            tvKategori = itemView.findViewById(R.id.tvKategori);
            tvHarga = itemView.findViewById(R.id.tvHarga);
        }
    }
}