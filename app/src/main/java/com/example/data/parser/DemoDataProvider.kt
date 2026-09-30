package com.example.data.parser

import com.example.data.model.AppSettings
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.model.UserProfile

object DemoDataProvider {
    const val DEMO_SOURCE_ID = "tivions_demo"

    // Verified High-Availability Enterprise Streams (CORS Enabled, No Bot Blocks)
    const val STREAM_HLS_MUX_BBB = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
    const val STREAM_HLS_MUX_TEST = "https://test-streams.mux.dev/test_001/stream.m3u8"
    const val STREAM_HLS_MUX_PTS = "https://test-streams.mux.dev/pts_shift/master.m3u8"
    const val STREAM_HLS_APPLE_16X9 = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"
    const val STREAM_HLS_APPLE_4X3 = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"
    const val STREAM_HLS_APPLE_FMP4 = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8"
    const val STREAM_HLS_TEARS = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
    const val STREAM_HLS_AKAMAI = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

    val defaultProfiles = listOf(
        UserProfile(
            id = "profile_1",
            name = "Ahmet",
            avatarColorHex = 0xFF6366F1,
            isKidsProfile = false,
            pinCode = null,
            isActive = true
        ),
        UserProfile(
            id = "profile_2",
            name = "Çocuk Modu",
            avatarColorHex = 0xFF10B981,
            isKidsProfile = true,
            pinCode = "1234",
            isActive = false
        ),
        UserProfile(
            id = "profile_3",
            name = "Misafir",
            avatarColorHex = 0xFF38BDF8,
            isKidsProfile = false,
            pinCode = null,
            isActive = false
        )
    )

    val defaultSettings = AppSettings(
        language = "Türkçe",
        autoRefreshInterval = "3 Günlük",
        liveStreamFormat = ".ts",
        videoPlayerEngine = "EXO",
        livePlayerEngine = "Advanced Exo Player",
        backgroundPlayback = true,
        epgSource = "M3U/XMLTV'den",
        epgAutoMatch = true,
        epgMatchByName = true,
        epgCache = true,
        epgAutoUpdate = "Otomatik",
        epgShowPastPrograms = false,
        epgTimeShiftMinutes = 0,
        bufferSetting = "Otomatik",
        isDarkMode = true,
        isParentalControlEnabled = false,
        parentalPin = "0000"
    )

    fun getDemoItems(): List<PlaylistItem> {
        val list = mutableListOf<PlaylistItem>()

        // ================= FILMS =================
        list.add(
            PlaylistItem(
                id = "movie_kizil_ufuk",
                name = "Kızıl Ufuk (2026)",
                category = "Aksiyon",
                type = ItemType.VOD_MOVIE,
                rating = 8.4,
                year = "2026",
                duration = "128 dk",
                description = "Sınır ötesi bir operasyonda her şeyini kaybeden bir ajanın intikam yolculuğu. Zaman daralırken gerçek düşman da yavaş yavaş ortaya çıkıyor.",
                cast = "Emre Kaya, Deniz Aksoy, Burak Yıldırım, Selin Kurt, Cem Demir",
                subtitleInfo = "Aksiyon / Gerilim",
                streamUrl = STREAM_HLS_TEARS,
                playlistSourceId = DEMO_SOURCE_ID,
                isFavorite = true,
                ratingValue = 8.4,
                rating5based = 4.2,
                releaseYear = 2026,
                globalOrderIndex = 1,
                genre = "Aksiyon, Gerilim"
            )
        )

        list.add(
            PlaylistItem(
                id = "movie_gece_nobeti",
                name = "Gece Nöbeti (2026)",
                category = "Aksiyon",
                type = ItemType.VOD_MOVIE,
                rating = 7.6,
                year = "2026",
                duration = "104 dk",
                description = "Bir gece bekçisinin şehri saran gizemli olaylarla mücadelesi.",
                cast = "Kerem Sancak, Ayla Doğan, Mehmet Çetin",
                subtitleInfo = "Aksiyon / Gerilim",
                streamUrl = STREAM_HLS_MUX_TEST,
                playlistSourceId = DEMO_SOURCE_ID,
                ratingValue = 7.6,
                rating5based = 3.8,
                releaseYear = 2026,
                globalOrderIndex = 2,
                genre = "Aksiyon, Gerilim"
            )
        )

        list.add(
            PlaylistItem(
                id = "movie_celik_yumruk",
                name = "Çelik Yumruk (2026)",
                category = "Aksiyon",
                type = ItemType.VOD_MOVIE,
                rating = 7.9,
                year = "2026",
                duration = "112 dk",
                description = "Eski bir boksörün ringe dönerek geçmişiyle hesaplaşma çabası.",
                cast = "Tolga Aydemir, Selin Kurt, Onur Aktaş",
                subtitleInfo = "Aksiyon / Dram",
                streamUrl = STREAM_HLS_APPLE_FMP4,
                playlistSourceId = DEMO_SOURCE_ID,
                ratingValue = 7.9,
                rating5based = 4.0,
                releaseYear = 2026,
                globalOrderIndex = 3,
                genre = "Aksiyon, Dram"
            )
        )

        list.add(
            PlaylistItem(
                id = "movie_son_sinir",
                name = "Son Sınır (2026)",
                category = "Aksiyon",
                type = ItemType.VOD_MOVIE,
                rating = 7.8,
                year = "2026",
                duration = "115 dk",
                description = "Kutup bölgesindeki araştırma istasyonunda mahsur kalan bilim ekibinin hayatta kalma savaşı.",
                cast = "Murat Can, Zeynep Erdem",
                subtitleInfo = "Aksiyon / Macera",
                streamUrl = STREAM_HLS_MUX_PTS,
                playlistSourceId = DEMO_SOURCE_ID,
                ratingValue = 7.8,
                rating5based = 3.9,
                releaseYear = 2026,
                globalOrderIndex = 4,
                genre = "Aksiyon, Macera"
            )
        )

        list.add(
            PlaylistItem(
                id = "movie_kahkaha_ekspresi",
                name = "Kahkaha Ekspresi (2026)",
                category = "Komedi",
                type = ItemType.VOD_MOVIE,
                rating = 7.5,
                year = "2026",
                duration = "98 dk",
                description = "Yanlış trene binen iki arkadaşın başlarına gelen inanılmaz komik serüvenler.",
                cast = "Cem Şen, Arda Balcı",
                subtitleInfo = "Komedi / Macera",
                streamUrl = STREAM_HLS_MUX_BBB,
                playlistSourceId = DEMO_SOURCE_ID,
                ratingValue = 7.5,
                rating5based = 3.7,
                releaseYear = 2026,
                globalOrderIndex = 5,
                genre = "Komedi, Macera"
            )
        )

        list.add(
            PlaylistItem(
                id = "movie_sessiz_liman",
                name = "Sessiz Liman (2026)",
                category = "Dram",
                type = ItemType.VOD_MOVIE,
                rating = 8.1,
                year = "2026",
                duration = "120 dk",
                description = "Ege kasabasında unutulmuş bir ailenin kuşaklar boyu süren duygusal yüzleşmesi.",
                cast = "Haluk Güven, Vahide Perçin",
                subtitleInfo = "Dram / Aile",
                streamUrl = STREAM_HLS_APPLE_4X3,
                playlistSourceId = DEMO_SOURCE_ID,
                ratingValue = 8.1,
                rating5based = 4.1,
                releaseYear = 2026,
                globalOrderIndex = 6,
                genre = "Dram"
            )
        )

        list.add(
            PlaylistItem(
                id = "movie_kuantum_lab",
                name = "Kuantum Sıfır (2026)",
                category = "Bilim Kurgu",
                type = ItemType.VOD_MOVIE,
                rating = 8.3,
                year = "2026",
                duration = "132 dk",
                description = "Zaman çizgisini korumaya çalışan kuantum fizikçisinin macerası.",
                cast = "Alişan Baran, Melisa Sözen",
                subtitleInfo = "Bilim Kurgu / Gizem",
                streamUrl = STREAM_HLS_TEARS,
                playlistSourceId = DEMO_SOURCE_ID,
                ratingValue = 8.3,
                rating5based = 4.2,
                releaseYear = 2026,
                globalOrderIndex = 7,
                genre = "Bilim Kurgu"
            )
        )

        list.add(
            PlaylistItem(
                id = "movie_bogazda_vals",
                name = "Boğazda Vals (2026)",
                category = "Romantik",
                type = ItemType.VOD_MOVIE,
                rating = 7.7,
                year = "2026",
                duration = "108 dk",
                description = "İstanbul'un büyülü atmosferinde yolları kesişen iki müzisyenin aşkı.",
                cast = "Selin Yılmaz, Kaan Özdemir",
                subtitleInfo = "Romantik / Müzik",
                streamUrl = STREAM_HLS_MUX_TEST,
                playlistSourceId = DEMO_SOURCE_ID,
                ratingValue = 7.7,
                rating5based = 3.9,
                releaseYear = 2026,
                globalOrderIndex = 8,
                genre = "Romantik"
            )
        )

        list.add(
            PlaylistItem(
                id = "movie_karanlik_kanyon",
                name = "Karanlık Kanyon (2026)",
                category = "Korku",
                type = ItemType.VOD_MOVIE,
                rating = 7.3,
                year = "2026",
                duration = "95 dk",
                description = "Terk edilmiş maden ocağında mahsur kalan dağcıların korku dolu gecesi.",
                cast = "Bora Çelik, Derya Tunç",
                subtitleInfo = "Korku / Gerilim",
                streamUrl = STREAM_HLS_APPLE_16X9,
                playlistSourceId = DEMO_SOURCE_ID,
                ratingValue = 7.3,
                rating5based = 3.6,
                releaseYear = 2026,
                globalOrderIndex = 9,
                genre = "Korku"
            )
        )

        list.add(
            PlaylistItem(
                id = "movie_kutup_masali",
                name = "Kutup Masalı (2026)",
                category = "Animasyon",
                type = ItemType.VOD_MOVIE,
                rating = 8.0,
                year = "2026",
                duration = "88 dk",
                description = "Kuzey ışıklarının peşine düşen sevimli kutup ayısının neşeli yolculuğu.",
                cast = "Seslendirme: Ece Tan, Barış Yalçın",
                subtitleInfo = "Animasyon / Aile",
                streamUrl = STREAM_HLS_MUX_BBB,
                playlistSourceId = DEMO_SOURCE_ID,
                ratingValue = 8.0,
                rating5based = 4.0,
                releaseYear = 2026,
                globalOrderIndex = 10,
                genre = "Animasyon"
            )
        )

        // ================= SERIES =================
        // Türk Dizileri
        list.add(
            PlaylistItem(
                id = "series_sahsiyet",
                name = "Şahsiyet",
                category = "Türk Dizileri",
                type = ItemType.VOD_SERIES,
                rating = 9.0,
                year = "2026",
                duration = "2 Sezon",
                description = "Alzheimer teşhisi konan emekli bir adli katibin adalet arayışı.",
                cast = "Haluk Bilginer, Cansu Dere, Metin Akdülger",
                subtitleInfo = "Suç / Gizem / Dram",
                streamUrl = STREAM_HLS_TEARS,
                playlistSourceId = DEMO_SOURCE_ID,
                isFavorite = true,
                ratingValue = 9.0,
                rating5based = 4.5,
                releaseYear = 2026,
                globalOrderIndex = 1,
                genre = "Suç, Dram"
            )
        )
        for (ep in 1..6) {
            list.add(
                PlaylistItem(
                    id = "ep_sahsiyet_s1_e$ep",
                    name = "$ep. Bölüm",
                    category = "Türk Dizileri",
                    type = ItemType.EPISODE,
                    rating = 9.0,
                    year = "2018",
                    duration = "60 dk",
                    description = "Agâh Beyoğlu ve Nevra Elmas'ın yolları kesişir.",
                    streamUrl = STREAM_HLS_TEARS,
                    seriesId = "series_sahsiyet",
                    seasonNumber = 1,
                    episodeNumber = ep,
                    playlistSourceId = DEMO_SOURCE_ID
                )
            )
        }

        list.add(
            PlaylistItem(
                id = "series_ezel",
                name = "Ezel",
                category = "Türk Dizileri",
                type = ItemType.VOD_SERIES,
                rating = 8.7,
                year = "2026",
                duration = "2 Sezon",
                description = "En yakın dostlarının ve sevdiği kadının ihanetine uğrayan Ömer'in intikam hikayesi.",
                cast = "Kenan İmirzalıoğlu, Cansu Dere, Yiğit Özşener, Tuncel Kurtiz",
                subtitleInfo = "Aksiyon / Dram / Suç",
                streamUrl = STREAM_HLS_MUX_TEST,
                playlistSourceId = DEMO_SOURCE_ID,
                ratingValue = 8.7,
                rating5based = 4.3,
                releaseYear = 2026,
                globalOrderIndex = 2,
                genre = "Aksiyon, Dram, Suç"
            )
        )
        list.add(
            PlaylistItem(
                id = "series_kurtlar_vadisi",
                name = "Kurtlar Vadisi",
                category = "Türk Dizileri",
                type = ItemType.VOD_SERIES,
                rating = 8.9,
                year = "2026",
                duration = "4 Sezon",
                description = "Bu bir mafya dizisidir. Devlet adına mafyanın içine sızan Ali Candan'ın Polat Alemdar'a dönüşüm öyküsü.",
                cast = "Necati Şaşmaz, Özgü Namal, Selçuk Yöntem, Oktay Kaynarca",
                subtitleInfo = "Aksiyon / Suç / Dram",
                streamUrl = STREAM_HLS_APPLE_FMP4,
                playlistSourceId = DEMO_SOURCE_ID,
                ratingValue = 8.9,
                rating5based = 4.5,
                releaseYear = 2026,
                globalOrderIndex = 3,
                genre = "Aksiyon, Suç, Dram"
            )
        )
        list.add(
            PlaylistItem(
                id = "series_kardes_payi",
                name = "Kardeş Payı",
                category = "Türk Dizileri",
                type = ItemType.VOD_SERIES,
                rating = 8.6,
                year = "2026",
                duration = "2 Sezon",
                description = "Tesisatçı iki kardeşin dünyayı kurtaracak bir icat yapma serüveni.",
                cast = "Ahmet Kural, Murat Cemcir, Seda Bakan",
                subtitleInfo = "Komedi / Macera",
                streamUrl = STREAM_HLS_MUX_BBB,
                playlistSourceId = DEMO_SOURCE_ID,
                ratingValue = 8.6,
                rating5based = 4.3,
                releaseYear = 2026,
                globalOrderIndex = 4,
                genre = "Komedi, Macera"
            )
        )

        // Kore Dizileri
        list.add(
            PlaylistItem(
                id = "series_squid_game",
                name = "Squid Game",
                category = "Kore Dizileri",
                type = ItemType.VOD_SERIES,
                rating = 8.0,
                year = "2026",
                duration = "2 Sezon",
                description = "Büyük para ödülü için ölümcül çocuk oyunlarında yarışan çaresiz yarışmacılar.",
                cast = "Lee Jung-jae, Park Hae-soo, Wi Ha-joon",
                subtitleInfo = "Aksiyon / Gerilim / Dram",
                streamUrl = STREAM_HLS_TEARS,
                playlistSourceId = DEMO_SOURCE_ID,
                isFavorite = true,
                ratingValue = 8.0,
                rating5based = 4.0,
                releaseYear = 2026,
                globalOrderIndex = 5,
                genre = "Aksiyon, Gerilim, Dram"
            )
        )
        for (ep in 1..6) {
            list.add(
                PlaylistItem(
                    id = "ep_sg_s1_e$ep",
                    name = "$ep. Bölüm: Kırmızı Işık, Yeşil Işık",
                    category = "Kore Dizileri",
                    type = ItemType.EPISODE,
                    rating = 8.0,
                    year = "2021",
                    duration = "55 dk",
                    description = "Yarışmacılar arenaya getirilir.",
                    streamUrl = STREAM_HLS_TEARS,
                    seriesId = "series_squid_game",
                    seasonNumber = 1,
                    episodeNumber = ep,
                    playlistSourceId = DEMO_SOURCE_ID
                )
            )
        }
        list.add(
            PlaylistItem(
                id = "series_all_of_us_are_dead",
                name = "All of Us Are Dead",
                category = "Kore Dizileri",
                type = ItemType.VOD_SERIES,
                rating = 7.7,
                year = "2022",
                duration = "1 Sezon",
                description = "Bir lisede patlak veren zombi virüsü sonrası öğrencilerin hayatta kalma mücadelesi.",
                cast = "Park Ji-hu, Yoon Chan-young, Cho Yi-hyun",
                subtitleInfo = "Korku / Aksiyon / Dram",
                streamUrl = STREAM_HLS_MUX_TEST,
                playlistSourceId = DEMO_SOURCE_ID
            )
        )
        list.add(
            PlaylistItem(
                id = "series_glory",
                name = "The Glory",
                category = "Kore Dizileri",
                type = ItemType.VOD_SERIES,
                rating = 8.1,
                year = "2022",
                duration = "1 Sezon",
                description = "Lisede ağır zorbalığa uğrayan bir kadının yıllar sonra hazırladığı intikam planı.",
                cast = "Song Hye-kyo, Lee Do-hyun, Lim Ji-yeon",
                subtitleInfo = "Dram / Gizem / Gerilim",
                streamUrl = STREAM_HLS_APPLE_16X9,
                playlistSourceId = DEMO_SOURCE_ID
            )
        )

        // Netflix Dizileri
        list.add(
            PlaylistItem(
                id = "series_stranger_things",
                name = "Stranger Things",
                category = "Netflix Dizileri",
                type = ItemType.VOD_SERIES,
                rating = 8.7,
                year = "2016",
                duration = "4 Sezon",
                description = "Hawkins kasabasında kaybolan bir çocuk ve ortaya çıkan telekinetik yetenekli kız.",
                cast = "Millie Bobby Brown, Finn Wolfhard, Winona Ryder",
                subtitleInfo = "Bilim Kurgu / Korku / Dram",
                streamUrl = STREAM_HLS_TEARS,
                playlistSourceId = DEMO_SOURCE_ID,
                isFavorite = true
            )
        )
        list.add(
            PlaylistItem(
                id = "series_wednesday",
                name = "Wednesday",
                category = "Netflix Dizileri",
                type = ItemType.VOD_SERIES,
                rating = 8.1,
                year = "2022",
                duration = "1 Sezon",
                description = "Wednesday Addams'ın Nevermore Akademisi'ndeki cinayet gizemini çözme macerası.",
                cast = "Jenna Ortega, Gwendoline Christie, Riki Lindhome",
                subtitleInfo = "Komedi / Korku / Fantastik",
                streamUrl = STREAM_HLS_MUX_BBB,
                playlistSourceId = DEMO_SOURCE_ID
            )
        )
        list.add(
            PlaylistItem(
                id = "series_dark",
                name = "Dark",
                category = "Netflix Dizileri",
                type = ItemType.VOD_SERIES,
                rating = 8.8,
                year = "2017",
                duration = "3 Sezon",
                description = "Winden kasabasındaki gizemli mağarada zamanda yolculuk ve iç içe geçmiş aile trajedileri.",
                cast = "Louis Hofmann, Oliver Masucci, Karoline Eichhorn",
                subtitleInfo = "Bilim Kurgu / Gizem / Dram",
                streamUrl = STREAM_HLS_APPLE_FMP4,
                playlistSourceId = DEMO_SOURCE_ID
            )
        )

        // Aksiyon Dizileri
        list.add(
            PlaylistItem(
                id = "series_the_boys",
                name = "The Boys",
                category = "Aksiyon Dizileri",
                type = ItemType.VOD_SERIES,
                rating = 8.7,
                year = "2019",
                duration = "4 Sezon",
                description = "Süper güçlerini kötüye kullanan süper kahramanlara karşı sıradan insanların savaşı.",
                cast = "Karl Urban, Jack Quaid, Antony Starr",
                subtitleInfo = "Aksiyon / Komedi / Bilim Kurgu",
                streamUrl = STREAM_HLS_TEARS,
                playlistSourceId = DEMO_SOURCE_ID
            )
        )
        list.add(
            PlaylistItem(
                id = "series_reacher",
                name = "Reacher",
                category = "Aksiyon Dizileri",
                type = ItemType.VOD_SERIES,
                rating = 8.1,
                year = "2022",
                duration = "2 Sezon",
                description = "Eski askeri polis Jack Reacher'ın adaleti kendi elleriyle sağlama yolculuğu.",
                cast = "Alan Ritchson, Malcolm Goodwin, Willa Fitzgerald",
                subtitleInfo = "Aksiyon / Suç / Dram",
                streamUrl = STREAM_HLS_MUX_TEST,
                playlistSourceId = DEMO_SOURCE_ID
            )
        )
        list.add(
            PlaylistItem(
                id = "series_demir_cember",
                name = "Demir Çember",
                category = "Aksiyon Dizileri",
                type = ItemType.VOD_SERIES,
                rating = 8.5,
                year = "2024",
                duration = "2 Sezon",
                description = "Metropolün yer altı şebekesini çökertmeye çalışan özel timin nefes kesen operasyonu.",
                cast = "Cihan Ertaş, Elif Kurt, Barış Demir",
                subtitleInfo = "Aksiyon / Suç",
                streamUrl = STREAM_HLS_APPLE_16X9,
                playlistSourceId = DEMO_SOURCE_ID
            )
        )

        // ================= LIVE TV CHANNELS =================
        // Spor
        list.add(
            PlaylistItem(
                id = "tv_spor_max_1",
                name = "Spor Max",
                category = "Spor",
                type = ItemType.LIVE_TV,
                subtitleInfo = "Canlı: Süper Lig Maçı",
                streamUrl = STREAM_HLS_AKAMAI,
                playlistSourceId = DEMO_SOURCE_ID,
                isFavorite = true,
                epgChannelId = "tv_spor_max_1"
            )
        )

        list.add(
            PlaylistItem(
                id = "tv_spor_max_2",
                name = "Spor Max 2",
                category = "Spor",
                type = ItemType.LIVE_TV,
                subtitleInfo = "Basketbol: Avrupa Ligi",
                streamUrl = STREAM_HLS_MUX_BBB,
                playlistSourceId = DEMO_SOURCE_ID,
                epgChannelId = "tv_spor_max_2"
            )
        )

        list.add(
            PlaylistItem(
                id = "tv_aksiyon_spor",
                name = "Aksiyon Spor",
                category = "Spor",
                type = ItemType.LIVE_TV,
                subtitleInfo = "Voleybol: Şampiyonlar Ligi",
                streamUrl = STREAM_HLS_APPLE_16X9,
                playlistSourceId = DEMO_SOURCE_ID,
                epgChannelId = "tv_aksiyon_spor"
            )
        )

        list.add(
            PlaylistItem(
                id = "tv_golf_sport",
                name = "Golf Sport",
                category = "Spor",
                type = ItemType.LIVE_TV,
                subtitleInfo = "PGA Turu Özeti",
                streamUrl = STREAM_HLS_APPLE_4X3,
                playlistSourceId = DEMO_SOURCE_ID,
                epgChannelId = "tv_golf_sport"
            )
        )

        list.add(
            PlaylistItem(
                id = "tv_motor_sporlari",
                name = "Motor Sporları",
                category = "Spor",
                type = ItemType.LIVE_TV,
                subtitleInfo = "Formula 1: Antrenman",
                streamUrl = STREAM_HLS_TEARS,
                playlistSourceId = DEMO_SOURCE_ID,
                epgChannelId = "tv_motor_sporlari"
            )
        )

        list.add(
            PlaylistItem(
                id = "tv_boks_max",
                name = "Boks HD",
                category = "Spor",
                type = ItemType.LIVE_TV,
                subtitleInfo = "Canlı: Ağır Sıklet Maçı",
                streamUrl = STREAM_HLS_AKAMAI,
                playlistSourceId = DEMO_SOURCE_ID,
                epgChannelId = "tv_boks_max"
            )
        )

        // Sinema
        list.add(
            PlaylistItem(
                id = "tv_sinema_tv",
                name = "Sinema TV HD",
                category = "Sinema",
                type = ItemType.LIVE_TV,
                subtitleInfo = "Yabancı Sinema Kuşağı",
                streamUrl = STREAM_HLS_MUX_BBB,
                playlistSourceId = DEMO_SOURCE_ID,
                isFavorite = true,
                epgChannelId = "tv_sinema_tv"
            )
        )

        list.add(
            PlaylistItem(
                id = "tv_sinema_aksiyon",
                name = "Sinema Aksiyon",
                category = "Sinema",
                type = ItemType.LIVE_TV,
                subtitleInfo = "Kesintisiz Aksiyon Kuşağı",
                streamUrl = STREAM_HLS_TEARS,
                playlistSourceId = DEMO_SOURCE_ID,
                epgChannelId = "tv_sinema_aksiyon"
            )
        )

        // Ulusal
        list.add(
            PlaylistItem(
                id = "tv_trt1",
                name = "TRT 1 HD",
                category = "Ulusal",
                type = ItemType.LIVE_TV,
                subtitleInfo = "Ana Haber Bülteni",
                streamUrl = STREAM_HLS_APPLE_16X9,
                playlistSourceId = DEMO_SOURCE_ID,
                epgChannelId = "tv_trt1"
            )
        )

        list.add(
            PlaylistItem(
                id = "tv_atv",
                name = "ATV HD",
                category = "Ulusal",
                type = ItemType.LIVE_TV,
                subtitleInfo = "Canlı Yayın",
                streamUrl = STREAM_HLS_MUX_BBB,
                playlistSourceId = DEMO_SOURCE_ID,
                epgChannelId = "tv_atv"
            )
        )

        // Haber
        list.add(
            PlaylistItem(
                id = "tv_trt_haber",
                name = "TRT Haber",
                category = "Haber",
                type = ItemType.LIVE_TV,
                subtitleInfo = "Dünyadan ve Türkiye'den Son Dakika",
                streamUrl = STREAM_HLS_AKAMAI,
                playlistSourceId = DEMO_SOURCE_ID,
                epgChannelId = "tv_trt_haber"
            )
        )

        list.add(
            PlaylistItem(
                id = "tv_ntv",
                name = "NTV HD",
                category = "Haber",
                type = ItemType.LIVE_TV,
                subtitleInfo = "Ekonomi ve Piyasa Analizi",
                streamUrl = STREAM_HLS_APPLE_16X9,
                playlistSourceId = DEMO_SOURCE_ID,
                epgChannelId = "tv_ntv"
            )
        )

        // Çocuk
        list.add(
            PlaylistItem(
                id = "tv_trt_cocuk",
                name = "TRT Çocuk",
                category = "Çocuk",
                type = ItemType.LIVE_TV,
                subtitleInfo = "Rafadan Tayfa",
                streamUrl = STREAM_HLS_MUX_BBB,
                playlistSourceId = DEMO_SOURCE_ID,
                epgChannelId = "tv_trt_cocuk"
            )
        )

        list.add(
            PlaylistItem(
                id = "tv_cartoon_network",
                name = "Cartoon Network",
                category = "Çocuk",
                type = ItemType.LIVE_TV,
                subtitleInfo = "Gumball Maceraları",
                streamUrl = STREAM_HLS_MUX_BBB,
                playlistSourceId = DEMO_SOURCE_ID,
                epgChannelId = "tv_cartoon_network"
            )
        )

        return list
    }

    /**
     * Generates a fully compliant XMLTV XML document for all live channels,
     * with timestamps dynamically calculated around [baseTimeMillis]
     * so schedules are always alive and active at runtime.
     */
    fun generateDemoXmlTv(baseTimeMillis: Long = System.currentTimeMillis()): String {
        val sdf = java.text.SimpleDateFormat("yyyyMMddHHmmss +0000", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }

        fun timeOffset(minutes: Int): String {
            return sdf.format(java.util.Date(baseTimeMillis + (minutes * 60 * 1000L)))
        }

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>
<tv generator-info-name="Tivions IPTV XMLTV Generator">
""")

        // Channels definitions
        val channels = listOf(
            Triple("tv_spor_max_1", "Spor Max", "Spor"),
            Triple("tv_spor_max_2", "Spor Max 2", "Spor"),
            Triple("tv_aksiyon_spor", "Aksiyon Spor", "Spor"),
            Triple("tv_golf_sport", "Golf Sport", "Spor"),
            Triple("tv_motor_sporlari", "Motor Sporları", "Spor"),
            Triple("tv_boks_max", "Boks HD", "Spor"),
            Triple("tv_sinema_tv", "Sinema TV HD", "Sinema"),
            Triple("tv_sinema_aksiyon", "Sinema Aksiyon", "Sinema"),
            Triple("tv_trt1", "TRT 1 HD", "Ulusal"),
            Triple("tv_atv", "ATV HD", "Ulusal"),
            Triple("tv_trt_haber", "TRT Haber", "Haber"),
            Triple("tv_ntv", "NTV HD", "Haber"),
            Triple("tv_trt_cocuk", "TRT Çocuk", "Çocuk"),
            Triple("tv_cartoon_network", "Cartoon Network", "Çocuk")
        )

        for (ch in channels) {
            sb.append("""  <channel id="${ch.first}">
    <display-name>${ch.second}</display-name>
  </channel>
""")
        }

        // Program Schedules definition helper
        data class DemoProg(val startMin: Int, val endMin: Int, val title: String, val desc: String, val cat: String)

        val channelSchedules = mapOf(
            "tv_spor_max_1" to listOf(
                DemoProg(-180, -90, "Süper Lig Özetler & Goller", "Haftanın tüm maçlarından en heyecanlı anlar ve usta yorumcuların değerlendirmeleri.", "Spor"),
                DemoProg(-90, 45, "Canlı: Galatasaray - Fenerbahçe Derbisi", "Süper Lig dev derbisinde kıyasıya mücadele! Canlı yayın ve stadyumdan anlık bağlantılar.", "Spor"),
                DemoProg(45, 120, "Maç Sonu & Teknik Analiz", "Derbinin ardından teknik direktör açıklamaları ve tartışmalı pozisyonların analizi.", "Spor"),
                DemoProg(120, 240, "Avrupa'dan Futbol & Yıldızlar", "Avrupa liglerinde haftanın golleri ve yıldız futbolcuların performansları.", "Spor"),
                DemoProg(240, 360, "Şampiyonlar Ligi Özel Dosyası", "Devler Ligi'nde son 16 turu öncesi takımların son form durumları.", "Spor"),
                DemoProg(360, 500, "Canlı: Real Madrid - Barcelona El Clasico", "Dünyanın gözü bu maçta! Bernabeu'dan nefes kesen El Clasico mücadelesi.", "Spor")
            ),
            "tv_spor_max_2" to listOf(
                DemoProg(-120, -15, "Euroleague Basketbol Magazin", "Avrupa basketbolunun en iyileri, haftanın smaçları ve oyuncu röportajları.", "Spor"),
                DemoProg(-15, 90, "Canlı: Panathinaikos - Fenerbahçe Beko", "Euroleague heyecanı tam gaz! OAKA Arena'da nefes kesen mücadele.", "Spor"),
                DemoProg(90, 180, "NBA Action & En İyi Hareketler", "NBA'de gecenin en iyi 10 hareketi ve haftanın maç özetleri.", "Spor"),
                DemoProg(180, 300, "Tenis: Wimbledon Yarı Finalleri", "Merkez korttan muhteşem ralliler ve yarı final kapışması.", "Spor")
            ),
            "tv_aksiyon_spor" to listOf(
                DemoProg(-120, 15, "Voleybol: Vakıfbank - Eczacıbaşı", "Sultanlar Ligi derbisinde şampiyonluk yolunda kritik karşılaşma.", "Spor"),
                DemoProg(15, 120, "Canlı: CEV Şampiyonlar Ligi Finali", "Avrupa'nın zirvesi için kıyasıya voleybol mücadelesi.", "Spor"),
                DemoProg(120, 240, "Ekstrem Sporlar: Red Bull Cliff Diving", "Kayalıklardan serbest düşüş ve adrenalin dolu atlayışlar.", "Spor")
            ),
            "tv_golf_sport" to listOf(
                DemoProg(-120, 30, "PGA Turu Özeti & Highlights", "Masters şampiyonasının en kritik vuruşları.", "Spor"),
                DemoProg(30, 180, "Canlı: European Tour 3. Gün", "İskoçya links sahasından gün boyu canlı golf yayını.", "Spor"),
                DemoProg(180, 300, "Golf Akademisi: Swing Teknikleri", "Profesyonellerden vuruş teknikleri ve saha taktikleri.", "Spor")
            ),
            "tv_motor_sporlari" to listOf(
                DemoProg(-90, 30, "Formula 1: Sıralama Turları", "Pole pozisyonu için kıyasıya zaman mücadelesi.", "Spor"),
                DemoProg(30, 150, "Canlı: F1 Monaco Grand Prix", "Monaco sokaklarında hız ve taktik savaşı.", "Spor"),
                DemoProg(150, 270, "MotoGP: Mugello Yarışı Özeti", "İki tekerlek üzerinde saatte 350 km hızla giden dev kapışma.", "Spor")
            ),
            "tv_boks_max" to listOf(
                DemoProg(-120, 10, "Ağır Sıklet Klasikleri: Tyson vs Holyfield", "Boks tarihine damga vuran unutulmaz unvan maçları.", "Spor"),
                DemoProg(10, 130, "Canlı: Dünya Ağır Sıklet Unvan Maçı", "WBC ve WBA kemerleri için 12 rauntluk dev düello.", "Spor"),
                DemoProg(130, 240, "UFC Gecesi: En İyi Nakavtlar", "Kafeste nefes kesen submission ve nakavt serisi.", "Spor")
            ),
            "tv_sinema_tv" to listOf(
                DemoProg(-150, -30, "Yıldızlararası (Interstellar)", "İnsanlığın yeni bir yuva bulması için kara deliğe uzanan epik uzay serüveni.", "Sinema"),
                DemoProg(-30, 110, "Kızıl Ufuk - TV İlk Gösterim", "Sınır ötesi operasyonda intikam peşine düşen ajanın nefes kesen mücadelesi.", "Sinema"),
                DemoProg(110, 230, "Başlangıç (Inception)", "Rüyaların içine girerek fikir çalma ve yerleştirme operasyonu.", "Sinema"),
                DemoProg(230, 350, "Bıçak Sırtı 2049", "Geleceğin Los Angeles'ında insan ve replikant kimlik arayışı.", "Sinema")
            ),
            "tv_sinema_aksiyon" to listOf(
                DemoProg(-120, 0, "Hızlı ve Öfkeli: Tokyo Yarışı", "Modifiyeli arabalar ve drift tutkunlarının Tokyo yeraltı yarışı.", "Sinema"),
                DemoProg(0, 120, "John Wick: 4. Bölüm", "Yüksek Şura'ya karşı özgürlük mücadelesi veren efsanevi suikastçı.", "Sinema"),
                DemoProg(120, 240, "Görevimiz Tehlike: Ölümcül Hesaplaşma", "Yapay zekanın tehdit ettiği küresel güvenlik için imkansız görev.", "Sinema")
            ),
            "tv_trt1" to listOf(
                DemoProg(-120, -30, "Gündem Ötesi", "Tarih, kültür ve medeniyet dünyamıza dair merak edilen derin konular.", "Kültür"),
                DemoProg(-30, 45, "Ana Haber Bülteni", "Türkiye ve dünya gündeminin en sıcak başlıkları, canlı bağlantılar.", "Haber"),
                DemoProg(45, 180, "Kudüs Fatihi Selahaddin Eyyubi", "Haçlı ordularına karşı birlik ve adalet mücadelesi veren büyük kumandanın destanı.", "Dizi"),
                DemoProg(180, 300, "Teşkilat (Tekrar)", "Milli İstihbarat Teşkilatı'nın gizli kahramanlarının nefes kesen operasyonları.", "Dizi")
            ),
            "tv_atv" to listOf(
                DemoProg(-90, 0, "Esra Erol'da", "Kayıpların bulunduğu ve kavuşmaların yaşandığı stüdyo programı.", "Magazin"),
                DemoProg(0, 45, "ATV Ana Haber", "Haftanın en önemli gelişmeleri ve özel haber dosyaları.", "Haber"),
                DemoProg(45, 180, "Kuruluş Osman (Yeni Bölüm)", "Osman Bey'in kurduğu cihan devletinin ilk temelleri ve büyük savaşlar.", "Dizi"),
                DemoProg(180, 300, "Kim Milyoner Olmak İster?", "Büyük ödül için yarışan adaylar ve genel kültür mücadelesi.", "Yarışma")
            ),
            "tv_trt_haber" to listOf(
                DemoProg(-60, 0, "Dünya Gündemi", "Uluslararası ilişkiler, diplomasi ve dış politika analizleri.", "Haber"),
                DemoProg(0, 60, "Canlı: Gün Ortası Haber Bülteni", "Son dakika gelişmeleri ve uzman konukların yorumları.", "Haber"),
                DemoProg(60, 120, "Ekonomi 7/24", "Piyasalar, enflasyon verileri ve sektör analizleri.", "Haber"),
                DemoProg(120, 180, "Stratejik Analiz", "Küresel krizler ve Türkiye'nin dış politika hamleleri.", "Haber")
            ),
            "tv_ntv" to listOf(
                DemoProg(-60, 0, "Geri Sayım", "Borsa ve döviz piyasalarında günün ilk rakamları.", "Ekonomi"),
                DemoProg(0, 60, "Canlı: Piyasa Ekranı", "BIST 100, altın ve petrol piyasalarındaki anlık değişimler.", "Ekonomi"),
                DemoProg(60, 120, "Yakın Plan", "Günün öne çıkan toplumsal ve siyasi olayları derinlemesine masada.", "Haber"),
                DemoProg(120, 180, "Doğrudan Siyaset", "Siyasi partilerin haftalık grup toplantıları ve gündem açıklamaları.", "Haber")
            ),
            "tv_trt_cocuk" to listOf(
                DemoProg(-60, 0, "Keloğlan Masalları", "Keloğlan ve arkadaşlarının masallar diyarındaki eğlenceli serüveni.", "Çocuk"),
                DemoProg(0, 60, "Rafadan Tayfa: Dehliz Macerası", "Mahallenin sevilen ekibi gizemli dehlizleri keşfe çıkıyor.", "Çocuk"),
                DemoProg(60, 120, "İbi ve Tosi'nin Maceraları", "Baldiyar'da matematik ve mantık oyunlarıyla dolu macera.", "Çocuk"),
                DemoProg(120, 180, "Ege ile Gaga", "Doğayı ve hayvanlar alemini araştıran meraklı ikili.", "Çocuk")
            ),
            "tv_cartoon_network" to listOf(
                DemoProg(-60, 0, "Gumball'ın Muhteşem Dünyası", "Elmore kasabasında Gumball ve Darwin'in absürt maceraları.", "Çocuk"),
                DemoProg(0, 60, "Adventure Time: Fionna & Cake", "Büyülü Ooo diyarında zamanda yolculuk ve epik serüven.", "Çocuk"),
                DemoProg(60, 120, "Sürekli Dizi (Regular Show)", "Mordecai ve Rigby'nin parkta başlarına açtığı çılgın işler.", "Çocuk"),
                DemoProg(120, 180, "Kafadar Ayılar (We Bare Bears)", "İnsanların dünyasında yaşamaya çalışan üç sevimli ayı kardeş.", "Çocuk")
            )
        )

        for ((channelId, progs) in channelSchedules) {
            for (p in progs) {
                val start = timeOffset(p.startMin)
                val stop = timeOffset(p.endMin)
                sb.append("""  <programme start="$start" stop="$stop" channel="$channelId">
    <title lang="tr">${escapeXml(p.title)}</title>
    <desc lang="tr">${escapeXml(p.desc)}</desc>
    <category lang="tr">${escapeXml(p.cat)}</category>
  </programme>
""")
            }
        }

        sb.append("</tv>\n")
        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

