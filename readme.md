# ☠️ DeathRace

Plugin Minecraft (Paper 1.21) untuk mini-game **Death Race** — player berlomba mati dengan cara sebanyak-banyaknya dalam batas waktu. Setiap cara mati yang unik menghasilkan 1 poin.

---

## Cara Main

1. Daftarkan player dengan `/regis <nama>`
2. Atur timer jika perlu dengan `/settimer`
3. Atur health jika perlu dengan `/maxhealth`
4. Mulai game dengan `/startsolo` atau `/startglobal`
5. Player berlomba mati dengan 75 cara yang berbeda
6. Game selesai saat timer habis atau `/stopgame` dipanggil
7. Ranking otomatis ditampilkan di chat

---

## Mode Game

### 🧍 Solo
Setiap player punya **death list masing-masing**. Player A mati dari kaktus tidak mempengaruhi Player B — semua orang bisa klaim semua 75 death secara independen. Cocok untuk kompetisi yang lebih fair.

### 👥 Global
Semua player **berbagi satu death list**. Siapa yang pertama mati dengan suatu cara akan "mengklaim" cara itu — tidak ada player lain yang bisa dapat poin dari cara yang sama. Race to be first. Lebih chaotic dan kompetitif.

---

## Commands

| Command | Permission | Deskripsi |
|---|---|---|
| `/regis <player>` | `deathrace.admin` | Daftarkan player ke game |
| `/unregis <player>` | `deathrace.admin` | Keluarkan player dari game |
| `/listplayer` | `deathrace.admin` | Lihat daftar player & score saat ini |
| `/startsolo` | `deathrace.admin` | Mulai game mode Solo |
| `/startglobal` | `deathrace.admin` | Mulai game mode Global |
| `/stopgame` | `deathrace.admin` | Hentikan game & tampilkan ranking |
| `/settimer <detik> atau <(menit)m>` | `deathrace.admin` | Set durasi ronde. Contoh: `/settimer 300` atau `/settimer 10m` |
| `/maxhealth <player> <hp>` | `deathrace.admin` | Lihat atau set max health player |
| `/listdeath` | `deathrace.use` | Lihat death yang sudah dipakai (player = milik sendiri, admin = semua) |
| `/listdeathall` | `deathrace.admin` | Lihat semua 75 death key beserta info siapa yang mati, diurutkan terbanyak |

### Detail `/maxhealth`
- `/maxhealth` — tampilkan max health semua registered player
- `/maxhealth <hp>` — set semua registered player sekaligus
- `/maxhealth <player> <hp>` — set player spesifik

> HP menggunakan satuan game (20 HP = 10 ❤). Minimal 2, maksimal 2048.

---

## Saat Game Start

Ketika `/startsolo` atau `/startglobal` dijalankan, plugin otomatis:

- Set semua registered player ke **Gamemode Survival**
- **Clear inventory** semua player (termasuk armor)
- Reset **health & hunger** ke penuh
- Set **difficulty ke Hard**
- Matikan **PVP** di world
- Pasang **World Border 10.000 blok** dari posisi operator yang menjalankan command
- **Teleport** semua player ke posisi operator

---

## Sistem Immunity

Setelah player mati dengan suatu cara, mereka **tidak bisa mati dari cara yang sama lagi**. Setiap death key punya efek immunity unik — lihat [Death.md](Death.md) untuk detail lengkap per death key.

---

## Permissions

| Permission | Default | Deskripsi |
|---|---|---|
| `deathrace.admin` | OP | Akses semua command admin |
| `deathrace.use` | Semua player | Akses `/listdeath` |

---

## Informasi Teknis

- **Platform:** Paper 1.21+
- **Java:** 21+
- **Tidak butuh** dependency plugin lain
- Skull player di `/listdeathall` menggunakan raw JSON text component — skin ke-render langsung di chat tanpa resource pack