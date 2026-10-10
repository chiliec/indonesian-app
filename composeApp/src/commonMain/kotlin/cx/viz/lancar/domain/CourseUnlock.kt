package cx.viz.lancar.domain

/** Completed lessons plus the first lesson in [order] that is not completed. */
fun unlockedLessonIds(order: List<String>, completed: Set<String>): Set<String> {
    val open = order.filterTo(mutableSetOf()) { it in completed }
    order.firstOrNull { it !in completed }?.let { open += it }
    return open
}
