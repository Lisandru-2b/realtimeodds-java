package xyz.realtimeodds.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import xyz.realtimeodds.entities.BasketballMatch;
import xyz.realtimeodds.entities.BasketballMoneyline;
import xyz.realtimeodds.entities.BasketballPeriod;
import xyz.realtimeodds.entities.Quote;
import xyz.realtimeodds.entities.Selection;
import xyz.realtimeodds.entities.SelectionKind;
import xyz.realtimeodds.entities.SelectionResult;

class OddsStoreTest {

    private BasketballMatch makeEvent() {
        Selection home = new Selection(
                "vmid:ps3838:1:m:home", SelectionKind.HOME_AWAY, SelectionResult.HOME,
                new Quote(1.91, null, 0L), null);
        Selection away = new Selection(
                "vmid:ps3838:1:m:away", SelectionKind.HOME_AWAY, SelectionResult.AWAY,
                new Quote(1.95, null, 0L), null);
        BasketballMoneyline market = new BasketballMoneyline(
                "vmid:ps3838:1:m", false,
                Map.of(home.id(), home, away.id(), away),
                "Lakers", "Celtics", BasketballPeriod.FULL_MATCH);
        return new BasketballMatch(
                "vmid:ps3838:1", "comp:basketball.nba",
                Map.of(market.id(), market),
                null, null, null, "Lakers", "Celtics");
    }

    @Test
    void upsertEmitsIsNewThenNotNew() {
        OddsStore store = new OddsStore();
        List<OddsStore.UpsertedPayload> seen = new ArrayList<>();
        store.events().<OddsStore.UpsertedPayload>on("sportEvent:upserted", seen::add);

        BasketballMatch ev = makeEvent();
        store.upsertSportEvent(ev);
        store.upsertSportEvent(ev);

        assertThat(seen).hasSize(2);
        assertThat(seen.get(0).isNew()).isTrue();
        assertThat(seen.get(1).isNew()).isFalse();
    }

    @Test
    void updatePricesReplacesQuoteAndEmits() {
        OddsStore store = new OddsStore();
        BasketballMatch ev = makeEvent();
        store.upsertSportEvent(ev);

        List<OddsStore.PricesUpdatedPayload> captured = new ArrayList<>();
        store.events().<OddsStore.PricesUpdatedPayload>on("prices:updated", captured::add);

        boolean ok = store.updatePrices(ev.id(), Map.of("vmid:ps3838:1:m:home", 2.00));
        assertThat(ok).isTrue();
        assertThat(captured).hasSize(1);

        Selection updated = store.getSelection("vmid:ps3838:1:m:home");
        assertThat(updated).isNotNull();
        assertThat(updated.price()).isEqualTo(2.00);
    }

    @Test
    void removeSportEventEmitsAndReturnsTrue() {
        OddsStore store = new OddsStore();
        BasketballMatch ev = makeEvent();
        store.upsertSportEvent(ev);

        List<OddsStore.RemovedPayload> captured = new ArrayList<>();
        store.events().<OddsStore.RemovedPayload>on("sportEvent:removed", captured::add);

        assertThat(store.removeSportEvent(ev.id())).isTrue();
        assertThat(store.removeSportEvent(ev.id())).isFalse();
        assertThat(captured).hasSize(1);
        assertThat(store.size()).isZero();
    }

    @Test
    void clearResetsAndEmits() {
        OddsStore store = new OddsStore();
        store.upsertSportEvent(makeEvent());

        int[] seen = {0};
        store.events().<Object>on("store:cleared", p -> seen[0]++);

        store.clear();
        assertThat(store.size()).isZero();
        assertThat(seen[0]).isEqualTo(1);
    }

    @Test
    void resyncReplacesStateAtomicallyAndEmits() {
        OddsStore store = new OddsStore();
        BasketballMatch existing = makeEvent();
        store.upsertSportEvent(existing);

        // Resync to a single new event with a different id
        BasketballMatch fresh = new BasketballMatch(
                "vmid:ps3838:2", "comp:basketball.nba", Map.of(),
                null, null, null, "Bulls", "Knicks");

        List<OddsStore.ResyncedPayload> captured = new ArrayList<>();
        store.events().<OddsStore.ResyncedPayload>on("store:resynced", captured::add);

        store.resync("primary_changed", List.of(fresh));

        assertThat(store.size()).isEqualTo(1);
        assertThat(store.getSportEvent(existing.id())).isNull();
        assertThat(store.getSportEvent(fresh.id())).isNotNull();
        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).reason()).isEqualTo("primary_changed");
    }
}
