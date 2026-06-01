# ☠️ Death Keys & Sistem Immunity

Setelah player mati dengan suatu cara, damage dari cause itu akan **di-cancel** secara otomatis. Setiap death key punya **efek immunity unik** yang aktif selama sisa ronde.

Total death key: **75**

---

## 🧱 Environment

| Key | Display | Efek Immunity |
|---|---|---|
| `CACTUS` | 🌵 Kaktus | Kaktus di sekitar player otomatis diganti ke versi pot bunga — tidak bisa melukai |
| `CAMPFIRE` | 🏕️ Campfire | Damage di-cancel, fire ticks dikosongkan, campfire terdekat dimatikan |
| `DROWNING` | 💧 Drowning | Saat di dalam air, player didorong ke permukaan seperti efek soul sand |
| `FALL` | 🌠 Fall | Saat jatuh dari ketinggian > 4 blok, seekor ayam di-spawn diatas player — ayam diberi Slow Falling sebagai parasut |
| `FIRE` | 🔥 Fire | Damage api biasa di-cancel, fire ticks dikosongkan, partikel love di-spawn |
| `FREEZE` | ❄️ Powder Snow | Damage freeze di-cancel sepenuhnya |
| `LAVA` | 🌋 Lava | Damage di-cancel, fire ticks dikosongkan, diberi Fire Resistance 2 detik, loop efek partikel love aktif |
| `LIGHTNING` | ⚡ Lightning | Damage petir di-cancel sepenuhnya |
| `MAGMA` | 🟥 Magma Block | Damage di cancel. Partikel smoke di-spawn |
| `SWEET_BERRY` | 🍓 Sweet Berry | Sweet berry di sekitar player direset jadi tanaman baru yang belum tumbuh |

---

## 💥 Explosive

| Key | Display | Efek Immunity |
|---|---|---|
| `BED` | 🛏️ Bed | Bed di Nether/End yang akan meledak dihancurkan sebelum meledak menggunakan partikel wind charge |
| `CREEPER` | 💚 Creeper | Damage ledakan Creeper di-cancel |
| `END_CRYSTAL` | 💎 End Crystal | Damage End Crystal di-cancel. Selain itu, End Crystal di sekitar player dihancurkan secara otomatis |
| `FIREWORK` | 🎆 Firework | Damage firework di-cancel |
| `RESPAWN_ANCHOR` | ⚓ Respawn Anchor | Respawn Anchor yang akan meledak dihancurkan sebelum meledak |
| `TNT` | 💣 TNT/Ledakan | TNT yang dinyalakan di dekat player langsung dihilangkan sebelum meledak, disertai partikel dan sound effect |

---

## 🏹 Projectile & Mekanik

| Key | Display | Efek Immunity |
|---|---|---|
| `ANVIL` | ⚒️ Anvil | Damage Anvil jatuh di-cancel serta anvil dihancurkan dan drop |
| `ARROW` | 🏹 Arrow | Damage panah di-cancel. |
| `DRIPSTONE` | 🗡️ Dripstone | Dripstone dihancurkan, baik yang jatuh maupun yang dibawah |
| `FIRE_CHARGE` | 🧨 Fire Charge | Damage Fire Charge dari dispenser di-cancel |
| `ELYTRA` | 🪂 Elytra | Damage menabrak dinding saat terbang dengan Elytra di-cancel. Penggunaan Firework saat gliding juga diblokir |
| `ENDER_PEARL` | 🔮 Ender Pearl | Damage dari teleport Ender Pearl di-cancel. Player tetap bisa teleport tanpa kena damage |
| `THORNS` | 🛡️ Thorns | Damage Thorns dari armor musuh di-cancel |
| `TRIDENT` | 🔱 Trident | Damage Trident dari Drowned di-cancel |

---

## 🌍 World & Mekanik Khusus

| Key | Display | Efek Immunity |
|---|---|---|
| `HUNGER` | 🍖 Hunger | Damage starvation di-cancel, food level dan saturasi otomatis diisi penuh |
| `MAGIC` | 🧪 Magic/Potion | Damage magic/potion di-cancel. |
| `SUFFOCATION` | 😤 Suffocated | Damage suffocation di-cancel, blok di muka player otomatis di-break |
| `VOID` | 🕳️ Void | Saat Y < -3, player diberi efek Levitation 10 detik untuk mencegah jatuh ke void |
| `WORLD_BORDER` | 🌍 World Border | Damage border di-cancel. Jika player berada di luar border, di-teleport kembali ke dalam border secara aman |

---

## 🐝 Mob Netral

Mob netral hanya menyerang saat diprovokasi. Setelah player mati dari mob ini, mob tersebut tidak akan menyerang player lagi dan memiliki perilaku khusus.

| Key | Display | Efek Immunity |
|---|---|---|
| `BEE` | 🐝 Bee | Bee tidak menyerang, anger di-reset. Saat player memukul bee, damage di-cancel — bee menghadap player dan memunculkan partikel madu |
| `CAVE_SPIDER` | 🕷️ Cave Spider | Saat siang: Cave Spider tidak menyerang. Jika dipukul saat siang, damage di-cancel dan cobweb di-spawn di sekitar spider |
| `SPIDER` | 🕸️ Spider | Saat siang: Spider tidak menyerang. Jika dipukul saat siang, damage di-cancel dan cobweb di-spawn di sekitar spider |
| `IRON_GOLEM` | 🌹 Iron Golem | Iron Golem tidak mentarget player. Jika player memukul Golem, Golem memberi bunga Poppy ke player, dan broadcast "<Iron Golem> ngapain lu colek-colek, nih bunga buat lu"|
| `LLAMA` | 🦙 Llama | Llama tidak menyerang. Jika dipukul, Llama menoleh dan meludahi player (partikel air) |
| `TRADER_LLAMA` | 🦙 Trader Llama | Sama seperti Llama |
| `POLAR_BEAR` | 🐻 Polar Bear | Polar Bear tidak menyerang player |
| `WOLF` | 🐺 Wolf | Wolf tidak menyerang. Jika dipukul, Wolf duduk dan menoleh ke player |
| `PANDA` | 🐼 Panda | Panda tidak menyerang. Jika dipukul, Panda berguling dan mengeluarkan suara bersin |
| `ZOMBIFIED_PIGLIN` | 🐷 Zombified Piglin | Jika player memukul Zombified Piglin, Piglin langsung mati — tidak memicu agresi kelompok |

---

## 👽 Mob Khusus

| Key | Display | Efek Immunity |
|---|---|---|
| `ENDERMAN` | 👽 Enderman | Saat player menatap Enderman, Enderman langsung mati dengan partikel portal dan suara scream. Jika dipukul, damage di-cancel dan Enderman teleport acak |

---

## ⚔️ Hostile Mob

Setelah mati dari hostile mob, mob tersebut akan **kabur** (flee) dari player dan berhenti menyerang. Beberapa mob punya perilaku khusus tambahan.

| Key | Display | Perilaku Khusus |
|---|---|---|
| `PIGLIN` | 🐷 Piglin | Kabur |
| `PUFFERFISH` | 🐡 Pufferfish | Tidak mengembang (`puffState = 0`) saat player mendekat |
| `BLAZE` | 🔥 Blaze | Kabur, Fire Charge di-cancel |
| `BOGGED` | 🦴 Bogged | Kabur |
| `BREEZE` | 🌪️ Breeze | Kabur |
| `CREAKING` | 🌳 Creaking | damage di cancel |
| `DROWNED` | 🤽 Drowned | Kabur, Trident di-cancel |
| `ELDER_GUARDIAN` | 👁️ Elder Guardian | Kabur |
| `ENDERMITE` | 🪲 Endermite | Kabur |
| `EVOKER` | 📖 Evoker | Kabur |
| `GHAST` | 👻 Ghast | Kabur, tembakan Fireball di-cancel |
| `GUARDIAN` | 🐟 Guardian | Kabur |
| `HOGLIN` | 🐗 Hoglin | Kabur |
| `HUSK` | 🏜️ Husk | Kabur |
| `MAGMA_CUBE` | 🟥 Magma Cube | Kabur |
| `PARCHED` | 🥵 Parched | Kabur |
| `PIGLIN_BRUTE` | 🪓 Piglin Brute | Kabur |
| `PILLAGER` | 🏹 Pillager | Kabur |
| `RAVAGER` | 🐂 Ravager | Kabur |
| `SILVERFISH` | 🐛 Silverfish | Kabur |
| `SKELETON` | 💀 Skeleton | Kabur |
| `SLIME` | 🟢 Slime | Kabur |
| `STRAY` | ❄️ Stray | Kabur |
| `VEX` | 👻 Vex | Kabur |
| `VINDICATOR` | 🪓 Vindicator | Kabur |
| `WARDEN` | 🔊 Warden | Kabur |
| `WITCH` | 🧪 Witch | Kabur |
| `WITHER_SKELETON` | ☠️ Wither Skeleton | Kabur |
| `ZOGLIN` | 🧟🐗 Zoglin | Kabur |
| `ZOMBIE` | 🧟 Zombie | Kabur |
| `ZOMBIE_VILLAGER` | 🧟 Villager Zombie | Kabur |
| `SHULKER` | 🐚 Shulker | Shell tertutup (`peek = 0`), target di-reset, dibuat silent — tidak menyerang |
| `PHANTOM` | 👻 Phantom | Tidak menyerang. Phantom malah orbit mengelilingi player di ketinggian +8 blok |
| `WITHER` | ☠️ Wither | Kabur. Damage Wither skull di-cancel |
| `ENDER_DRAGON` | 🐉 Ender Dragon | Dragon tidak mentarget player. Jika player mati dari Dragon, Dragon di-remove dan Dragon Egg di-spawn di lokasi kematian |

---

## 📋 Catatan

- **Mode Solo:** setiap player punya death list sendiri — immunity hanya berlaku untuk player yang sudah mati dari cause tersebut
- **Mode Global:** satu death list bersama — setelah satu player mati dari suatu cara, **tidak ada player lain yang bisa dapat poin**
- Mob flee menggunakan Pathfinder API — mob diarahkan menjauhi player, bukan sekadar `setTarget(null)`
- Khusus Creeper, TNT, dan ledakan: dicegah di level `ExtraListener` sebelum damage terjadi