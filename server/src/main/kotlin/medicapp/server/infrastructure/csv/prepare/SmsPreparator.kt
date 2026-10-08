package medicapp.server.infrastructure.csv.prepare

import kotlin.collections.iterator


class SmsPreparator : DataPreparator<Map<String, String>, Map<String, String>> {

    override fun prepare(
        input: List<Map<String, String>>
    ): PreparationResult<Map<String, String>> {

        val filtered = input
            .filter { it["Substance_Domain"] == "Human use" }
            .groupBy { it["#SMS_ID"] }

        val kept = mutableListOf<Map<String, String>>()
        var dropped = 0

        for ((_, group) in filtered) {
            val selected =
                group.firstOrNull { it["Language"] == "French" }
                    ?: group.firstOrNull { it["Language"] == "English" }

            if (selected != null) {
                kept += selected
            } else {
                dropped++
            }
        }

        return PreparationResult(
            data = kept,
            dropped = dropped,
            warnings = listOf(
                "SMS: $dropped entrées sans langue FR/EN ignorées"
            )
        )
    }
}