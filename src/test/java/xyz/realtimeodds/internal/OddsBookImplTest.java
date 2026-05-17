package xyz.realtimeodds.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import xyz.realtimeodds.OddsContext;
import xyz.realtimeodds.entities.BasketballMatch;
import xyz.realtimeodds.entities.BasketballMoneyline;
import xyz.realtimeodds.entities.BasketballPeriod;
import xyz.realtimeodds.entities.Bookmaker;
import xyz.realtimeodds.entities.Quote;
import xyz.realtimeodds.entities.Selection;
import xyz.realtimeodds.entities.SelectionKind;
import xyz.realtimeodds.entities.SelectionResult;
import xyz.realtimeodds.entities.SportEvent;

class OddsBookImplTest {

    private BasketballMatch makeEvent(String id, String home, String away) {
        Selection homeSel = new Selection(
                id + ":m:home", SelectionKind.HOME_AWAY, SelectionResult.HOME,
                new Quote(1.91, null, 0L), null);
        Selection awaySel = new Selection(
                id + ":m:away", SelectionKind.HOME_AWAY, SelectionResult.AWAY,
                new Quote(1.95, null, 0L), null);
        BasketballMoneyline market = new BasketballMoneyline(
                id + ":m", false,
                Map.of(homeSel.id(), homeSel, awaySel.id(), awaySel),
                home, away, BasketballPeriod.FULL_MATCH);
        return new BasketballMatch(
                id, "comp:basketball.nba",
                Map.of(market.id(), market),
                null, null, null, home, away);
    }

    @Test
    void upsertIndexesMarketsAndSelections() {
        OddsBookImpl book = new OddsBookImpl();
        book.upsert(makeEvent("vmid:ps3838:1", "Lakers", "Celtics"));

        assertThat(book.size()).isEqualTo(1);
        assertThat(book.getSportEvent("vmid:ps3838:1")).isNotNull();
        assertThat(book.getMarket("vmid:ps3838:1:m")).isNotNull();
        assertThat(book.getSelection("vmid:ps3838:1:m:home")).isNotNull();
        assertThat(book.getSelection("vmid:ps3838:1:m:away")).isNotNull();
    }

    @Test
    void findContextReturnsFullHierarchy() {
        OddsBookImpl book = new OddsBookImpl();
        book.upsert(makeEvent("vmid:ps3838:1", "Lakers", "Celtics"));

        OddsContext ctx = book.findContext("vmid:ps3838:1:m:home");
        assertThat(ctx).isNotNull();
        assertThat(ctx.sportEvent().id()).isEqualTo("vmid:ps3838:1");
        assertThat(ctx.market().id()).isEqualTo("vmid:ps3838:1:m");
        assertThat(ctx.selection().id()).isEqualTo("vmid:ps3838:1:m:home");
        assertThat(ctx.selection().quote().price()).isEqualTo(1.91);

        assertThat(book.findContext("vmid:unknown:1:m:nope")).isNull();
    }

    @Test
    void lookupsReturnNullForUnknownIds() {
        OddsBookImpl book = new OddsBookImpl();
        book.upsert(makeEvent("vmid:ps3838:1", "Lakers", "Celtics"));

        assertThat(book.getSportEvent("vmid:unknown:9")).isNull();
        assertThat(book.getMarket("vmid:unknown:9:m")).isNull();
        assertThat(book.getSelection("vmid:unknown:9:m:home")).isNull();
    }

    @Test
    void removeDropsIndexes() {
        OddsBookImpl book = new OddsBookImpl();
        SportEvent ev = makeEvent("vmid:ps3838:1", "Lakers", "Celtics");
        book.upsert(ev);
        book.remove(ev.id());

        assertThat(book.size()).isZero();
        assertThat(book.getSportEvent("vmid:ps3838:1")).isNull();
        assertThat(book.getMarket("vmid:ps3838:1:m")).isNull();
        assertThat(book.getSelection("vmid:ps3838:1:m:home")).isNull();
    }

    @Test
    void clearBookmakerDropsOnlyTargetBookmaker() {
        OddsBookImpl book = new OddsBookImpl();
        book.upsert(makeEvent("vmid:ps3838:1", "Lakers", "Celtics"));
        book.upsert(makeEvent("vmid:ps3838:2", "Bulls", "Heat"));
        book.upsert(makeEvent("vmid:polymarket:3", "Warriors", "Suns"));

        int removed = book.clearBookmaker(Bookmaker.PS3838);

        assertThat(removed).isEqualTo(2);
        assertThat(book.size()).isEqualTo(1);
        assertThat(book.getSportEvent("vmid:polymarket:3")).isNotNull();
        assertThat(book.getSportEvent("vmid:ps3838:1")).isNull();
        assertThat(book.getSportEvent("vmid:ps3838:2")).isNull();
    }

    @Test
    void clearBookmakerUnknownReturnsZero() {
        OddsBookImpl book = new OddsBookImpl();
        book.upsert(makeEvent("vmid:ps3838:1", "Lakers", "Celtics"));
        assertThat(book.clearBookmaker(Bookmaker.WINAMAX)).isZero();
        assertThat(book.size()).isEqualTo(1);
    }

    @Test
    void replaceBookmakerSwapsSlice() {
        OddsBookImpl book = new OddsBookImpl();
        book.upsert(makeEvent("vmid:ps3838:1", "Lakers", "Celtics"));
        book.upsert(makeEvent("vmid:ps3838:2", "Bulls", "Heat"));
        book.upsert(makeEvent("vmid:polymarket:3", "Warriors", "Suns"));

        SportEvent newEv = makeEvent("vmid:ps3838:9", "Knicks", "Nets");
        book.replaceBookmaker(Bookmaker.PS3838, List.of(newEv));

        assertThat(book.size()).isEqualTo(2);
        assertThat(book.getSportEvent("vmid:ps3838:1")).isNull();
        assertThat(book.getSportEvent("vmid:ps3838:2")).isNull();
        assertThat(book.getSportEvent("vmid:ps3838:9")).isSameAs(newEv);
        assertThat(book.getSportEvent("vmid:polymarket:3")).isNotNull();
    }

    @Test
    void cloneIsIndependent() {
        OddsBookImpl book = new OddsBookImpl();
        book.upsert(makeEvent("vmid:ps3838:1", "Lakers", "Celtics"));

        OddsBookImpl frozen = book.cloneBook();
        assertThat(frozen.size()).isEqualTo(1);

        // Mutate the original; the clone should be unaffected.
        book.upsert(makeEvent("vmid:ps3838:2", "Bulls", "Heat"));
        book.remove("vmid:ps3838:1");

        assertThat(book.size()).isEqualTo(1);
        assertThat(frozen.size()).isEqualTo(1);
        assertThat(frozen.getSportEvent("vmid:ps3838:1")).isNotNull();
    }

    @Test
    void iterationAndMaterialization() {
        OddsBookImpl book = new OddsBookImpl();
        book.upsert(makeEvent("vmid:ps3838:1", "Lakers", "Celtics"));
        book.upsert(makeEvent("vmid:ps3838:2", "Bulls", "Heat"));

        int count = 0;
        for (SportEvent ev : book) {
            assertThat(ev.id()).startsWith("vmid:ps3838:");
            count++;
        }
        assertThat(count).isEqualTo(2);
        assertThat(book.sportEvents()).hasSize(2);
    }
}
