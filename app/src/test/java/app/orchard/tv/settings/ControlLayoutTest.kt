package app.orchard.tv.settings

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], manifest = Config.NONE)
class ControlLayoutTest {
    @Test fun savedOrderSizeAndVisibilitySurviveRoundTrip() {
        val values = ControlLayout.defaults().reversed().mapIndexed { i, spec -> spec.copy(size = ControlSize.entries[i % 3], visible = i % 2 == 0) }
        assertEquals(values, ControlLayout.decode(ControlLayout.encode(values)))
    }

    @Test fun partialAndMalformedPreferencesPreserveKnownControlsAndAddMissingOnes() {
        val decoded = ControlLayout.decode("""[{"id":"network","size":"Large","visible":false},{"id":"network"},{"id":"unknown"},{"id":"care","size":"invalid"}]""")
        assertEquals(ControlKind.entries.size, decoded.size)
        assertEquals(ControlSpec(ControlKind.Network, ControlSize.Large, false), decoded.first())
        assertEquals(ControlSize.Compact, decoded[1].size)
        assertEquals(ControlLayout.defaults(), ControlLayout.decode("broken"))
    }

    @Test fun mixedSizesRemainInBoundsWithoutOverlappingAfterReorder() {
        for (seed in 0..80) {
            val values = ControlLayout.defaults().mapIndexed { i, spec -> spec.copy(size = ControlSize.entries[(seed + i * 7) % 3], visible = (seed + i) % 5 != 0) }
            val moved = ControlLayout.move(values, ControlKind.Network, seed % 5 - 2)
            val used = mutableSetOf<Pair<Int, Int>>()
            val placed = ControlLayout.place(moved)
            assertEquals(moved.filter { it.visible }.map { it.kind }, placed.map { it.spec.kind })
            placed.forEach { item ->
                assertTrue(item.column >= 0 && item.column + item.spec.size.columns <= 4)
                for (r in item.row until item.row + item.spec.size.rows) {
                    for (c in item.column until item.column + item.spec.size.columns) assertTrue(used.add(c to r))
                }
            }
        }
    }

    @Test fun hidingEveryControlDoesNotRestoreUnwantedCards() {
        val hidden = ControlLayout.defaults().map { it.copy(visible = false) }
        assertTrue(ControlLayout.place(ControlLayout.decode(ControlLayout.encode(hidden))).isEmpty())
        assertEquals(ControlLayout.defaults(), ControlLayout.move(ControlLayout.defaults(), ControlKind.Inputs, -1))
    }
}
