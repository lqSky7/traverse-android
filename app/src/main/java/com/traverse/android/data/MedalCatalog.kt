package com.traverse.android.data

/**
 * The badge artwork shipped in `res/drawable-nodpi` as `medal_<slug>`.
 *
 * The renders come from the community catalogue of Apple Fitness award badges
 * (projects.peterwunder.de/achievements) — Blender re-creations of the models, textures and
 * stickers Apple uses for Limited Edition challenges. The originals are Apple's property and
 * are reproduced here as award artwork for this app.
 */
object MedalCatalog {

    /** Drawable slugs, matching the `medal_` prefix in `res/drawable-nodpi`. */
    val all: List<String> = listOf(
        "medal_china_fitness_day_2018",
        "medal_china_fitness_day_2019",
        "medal_china_fitness_day_2020",
        "medal_china_fitness_day_2021",
        "medal_china_fitness_day_2022",
        "medal_china_fitness_day_2023",
        "medal_china_fitness_day_2024",
        "medal_china_fitness_day_2025",
        "medal_china_fitness_day_2026",
        "medal_close_your_rings_day_2025",
        "medal_dance_day_2021",
        "medal_dance_day_2022",
        "medal_dance_day_2023",
        "medal_dance_day_2024",
        "medal_dance_day_2026",
        "medal_earth_day_2017",
        "medal_earth_day_2018",
        "medal_earth_day_2019",
        "medal_earth_day_2021",
        "medal_earth_day_2022",
        "medal_earth_day_2023",
        "medal_earth_day_2024",
        "medal_earth_day_2025",
        "medal_earth_day_2026",
        "medal_environment_day_2020",
        "medal_heart_month_2018",
        "medal_heart_month_2019",
        "medal_heart_month_2020",
        "medal_heart_month_2021",
        "medal_heart_month_2022",
        "medal_heart_month_2023",
        "medal_heart_month_2024",
        "medal_heart_month_2025",
        "medal_heart_month_2026",
        "medal_japan_health_day_2019",
        "medal_lunar_new_year_2022",
        "medal_lunar_new_year_2023",
        "medal_meditation_day_2024",
        "medal_meditation_day_2025",
        "medal_mindful_month_2024",
        "medal_mindful_month_2025",
        "medal_mothers_day_us_2017",
        "medal_national_parks_2017",
        "medal_national_parks_2018",
        "medal_national_parks_2019",
        "medal_national_parks_2020",
        "medal_national_parks_2021",
        "medal_national_parks_2022",
        "medal_national_parks_2023",
        "medal_national_parks_2024",
        "medal_national_parks_2025",
        "medal_national_parks_2026",
        "medal_new_year_2017",
        "medal_new_year_2018",
        "medal_new_year_2020",
        "medal_new_year_2021",
        "medal_new_year_2022",
        "medal_new_year_2023",
        "medal_new_year_2024",
        "medal_new_year_2025",
        "medal_new_year_2026",
        "medal_running_day_2024",
        "medal_running_day_2025",
        "medal_running_day_2026",
        "medal_russia_fitness_day_2021",
        "medal_turkey_trot",
        "medal_turkey_trot_2017",
        "medal_turkey_trot_2019",
        "medal_turkey_trot_2020",
        "medal_unity_month_2021",
        "medal_unity_month_2022",
        "medal_unity_month_2023",
        "medal_veterans_day_2017",
        "medal_veterans_day_2018",
        "medal_veterans_day_2019",
        "medal_veterans_day_2020",
        "medal_veterans_day_2021",
        "medal_veterans_day_2022",
        "medal_veterans_day_2023",
        "medal_veterans_day_2024",
        "medal_veterans_day_2025",
        "medal_womens_day_2018",
        "medal_womens_day_2019",
        "medal_womens_day_2020",
        "medal_womens_day_2021",
        "medal_womens_day_2022",
        "medal_womens_day_2023",
        "medal_yoga_day_2019",
        "medal_yoga_day_2020",
        "medal_yoga_day_2021",
        "medal_yoga_day_2022",
        "medal_yoga_day_2023",
        "medal_yoga_day_2024",
        "medal_yoga_day_2026",
    )

    private val lookup = all.toSet()

    /**
     * Deterministic pick for achievements the server hasn't assigned artwork to yet, so a
     * badge never renders blank and never changes between launches.
     */
    fun fallback(key: String): String {
        if (all.isEmpty()) return ""
        var hash: Long = 5381
        for (byte in key.toByteArray(Charsets.UTF_8)) {
            hash = (hash * 33 + (byte.toLong() and 0xFF)) and 0xFFFFFFFFL
        }
        return all[(hash % all.size).toInt()]
    }

    fun contains(slug: String): Boolean = lookup.contains(slug)
}
