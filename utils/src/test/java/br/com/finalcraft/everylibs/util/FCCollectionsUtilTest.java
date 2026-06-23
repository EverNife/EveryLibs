package br.com.finalcraft.everylibs.util;

import br.com.finalcraft.everylibs.commons.MergeListResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FCCollectionsUtilTest {

    static class Ent {
        Integer id;
        String name;

        Ent(Integer id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static class Dto {
        Integer id;
        String name;

        Dto(Integer id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static MergeListResult<Ent> merge(List<Ent> entities, List<Dto> dtos) {
        return FCCollectionsUtil.mergeListWithDTO(
                entities, dtos,
                e -> e.id,
                d -> d.id,
                (d, e) -> e.name = d.name,
                d -> new Ent(null, d.name));
    }

    @Test
    void addsUpdatesAndRemovesInPlace() {
        List<Ent> entities = new ArrayList<>(Arrays.asList(new Ent(1, "a"), new Ent(2, "b"), new Ent(3, "c")));
        List<Dto> dtos = Arrays.asList(new Dto(1, "a2"), new Dto(3, "c2"), new Dto(null, "new"));

        MergeListResult<Ent> result = merge(entities, dtos);

        assertEquals(1, result.getAdded().size());
        assertEquals("new", result.getAdded().get(0).name);

        assertEquals(1, result.getRemoved().size());
        assertEquals(2, (int) result.getRemoved().get(0).id);

        assertEquals(2, result.getUpdated().size());

        // The list is synchronized in place: [1(a2), 3(c2), new].
        assertEquals(3, entities.size());
        assertEquals("a2", entities.get(0).name);
        assertEquals("c2", entities.get(1).name);
        assertEquals("new", entities.get(2).name);
    }

    @Test
    void rejectsDuplicateIncomingIds() {
        List<Ent> entities = new ArrayList<>();
        List<Dto> dtos = Arrays.asList(new Dto(1, "x"), new Dto(1, "y"));
        assertThrows(IllegalArgumentException.class, () -> merge(entities, dtos));
    }

    @Test
    void rejectsDtoReferencingUnknownId() {
        List<Ent> entities = new ArrayList<>(Arrays.asList(new Ent(1, "a")));
        List<Dto> dtos = Arrays.asList(new Dto(99, "x"));
        assertThrows(IllegalArgumentException.class, () -> merge(entities, dtos));
    }

    @Test
    void rejectsDuplicateExistingIds() {
        List<Ent> entities = new ArrayList<>(Arrays.asList(new Ent(1, "a"), new Ent(1, "b")));
        List<Dto> dtos = Arrays.asList(new Dto(1, "x"));
        assertThrows(IllegalArgumentException.class, () -> merge(entities, dtos));
    }
}
