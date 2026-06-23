package br.com.finalcraft.everylibs.util;

import br.com.finalcraft.everylibs.commons.MergeListResult;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class FCCollectionsUtil {

    public static <T> List<T> reversed(List<T> list){
        Collections.reverse(list);
        return list;
    }

    /**
     * Splits a list of elements into a specified number of sublists, evenly distributing the elements.
     *
     * @param elements the list of elements to be split
     * @param parts    the number of sublists to split the list into
     * @return a list of sublists, each containing an equal distribution of elements
     *
     * @example:
     *    // Input: ["a", "b", "c", "d", "e", "f", "g", "h", "i", "j"], 4
     *    // Output: [["a", "b", "c"], ["d", "e"], ["f", "g", "h"], ["i", "j"]]
     * <p>
     *    // Input: ["a", "b"], 7
     *    // Output: [["a"], [], [], ["b"], [], [], []]
     * <p>
     *    // Input: ["a", "b"], 8
     *    // Output: [["a"], [], [], [], ["b"], [], [], []]
     * <p>
     *    // Input: ["a", "b"], 0
     *    // Output: []
     */
    public static <T> List<List<T>> partitionEvenly(List<T> elements, int parts){
        if (parts <= 0){
            return new ArrayList<>();
        }

        List<List<T>> result = new ArrayList<>(parts);

        if (parts == 1){
            result.add(new ArrayList<>(elements));
            return result;
        }

        int chunkSize = (int) Math.floor(elements.size() / (double) parts);
        int leftOver = (int) Math.floor(elements.size() % (double) parts);

        int gap = leftOver <= 0 ? 0 : parts / leftOver;
        int gapCount = gap;
        int gapNiddle = 0;

        for (int i = 0; i < parts; i++) {
            List<T> subList = new ArrayList<>();

            int start = (i * chunkSize) + gapNiddle;
            int end = start + chunkSize;

            if (chunkSize > 0){ //Evenly split these Chunks into even parts
                subList.addAll(elements.subList(start, end));
            }

            if (leftOver > 0){ //We still have some leftOver
                if (gapCount == gap){ //If we are into a gap position we add one more element
                    gapCount = 0;
                    leftOver--;
                    gapNiddle++; //We move the gapNiddle to the right to prevent element duplication
                    subList.add(elements.get(end));
                }
                gapCount++;
            }

            result.add(subList);
        }

        return result;
    }

    /**
     * Synchronizes an existing entity list, in place, with the desired state described
     * by a list of DTOs:
     * <ul>
     *   <li>a DTO with a {@code null} id creates a new entity;</li>
     *   <li>a DTO whose id matches an existing entity updates that entity;</li>
     *   <li>an existing entity whose id is absent from the DTO list is removed.</li>
     * </ul>
     *
     * <pre>{@code
     * List<User> users = getUsersFromDatabase();
     * List<UserDTO> userDTOs = getUpdatedUserData();
     *
     * MergeListResult<User> result = FCCollectionsUtil.mergeListWithDTO(
     *     users,
     *     userDTOs,
     *     User::getId,        // id from an entity
     *     UserDTO::getId,     // id from a DTO (null for new users)
     *     (dto, entity) -> {  // update an existing entity
     *         entity.setName(dto.getName());
     *         entity.setEmail(dto.getEmail());
     *     },
     *     dto -> {            // create a new entity
     *         User user = new User();
     *         user.setName(dto.getName());
     *         user.setEmail(dto.getEmail());
     *         return user;
     *     }
     * );
     * }</pre>
     *
     * @param <ENTITY>         the entity type
     * @param <DTO>            the DTO type carrying the desired state
     * @param <ID>             the id type used to match entities to DTOs
     * @param existingEntities the current entities, synchronized in place (must be mutable)
     * @param incomingDtos     the DTOs describing the desired state
     * @param entityIdGetter   extracts the id from an entity
     * @param dtoIdGetter      extracts the id from a DTO ({@code null} means "new entity")
     * @param applyUpdates     copies DTO data onto a matched entity
     * @param newEntityFactory builds a new entity from a DTO
     * @return a {@link MergeListResult} holding the updated, removed and added entities
     * @throws IllegalArgumentException if the DTO list repeats an id, if the existing
     *                                  entities repeat an id, or if a DTO carries an id
     *                                  that no existing entity owns
     */
    public static <ENTITY, DTO, ID> MergeListResult<ENTITY> mergeListWithDTO(
            List<ENTITY> existingEntities,
            List<DTO> incomingDtos,
            Function<ENTITY, ID> entityIdGetter,
            Function<DTO, ID> dtoIdGetter,
            BiConsumer<DTO, ENTITY> applyUpdates,
            Function<DTO, ENTITY> newEntityFactory
    ) {
        Objects.requireNonNull(existingEntities, "existingEntities must not be null");
        Objects.requireNonNull(incomingDtos, "incomingDtos must not be null");
        Objects.requireNonNull(entityIdGetter, "entityIdGetter must not be null");
        Objects.requireNonNull(dtoIdGetter, "dtoIdGetter must not be null");
        Objects.requireNonNull(applyUpdates, "applyUpdates must not be null");
        Objects.requireNonNull(newEntityFactory, "newEntityFactory must not be null");

        List<ENTITY> added = new ArrayList<>();
        List<ENTITY> removed = new ArrayList<>();
        List<ENTITY> updated = new ArrayList<>();

        // 1 - Collect the incoming ids (rejecting duplicates) to drive removal and matching.
        Set<ID> incomingIds = new HashSet<>();
        for (DTO dto : incomingDtos) {
            ID dtoId = dtoIdGetter.apply(dto);
            if (dtoId != null && !incomingIds.add(dtoId)) {
                throw new IllegalArgumentException(String.format(
                        "There is a duplicated ID (%s) in the incoming DTOs List<%s>",
                        dtoId, dto.getClass().getSimpleName()));
            }
        }

        // 2 - Remove existing entities whose id is no longer present in the DTOs.
        Iterator<ENTITY> iterator = existingEntities.iterator();
        while (iterator.hasNext()) {
            ENTITY entity = iterator.next();
            ID entityId = entityIdGetter.apply(entity);
            if (entityId != null && !incomingIds.contains(entityId)) {
                iterator.remove();
                removed.add(entity);
            }
        }

        // 3 - Index the surviving entities by id for O(1) matching (ids must be unique).
        Map<ID, ENTITY> entitiesById = new HashMap<>();
        for (ENTITY entity : existingEntities) {
            ID entityId = entityIdGetter.apply(entity);
            if (entityId != null && entitiesById.put(entityId, entity) != null) {
                throw new IllegalArgumentException(String.format(
                        "There is a duplicated ID (%s) in the existing entities List<%s>",
                        entityId, entity.getClass().getSimpleName()));
            }
        }

        // 4 - Create entities for id-less DTOs; update the rest against their matched id.
        for (DTO dto : incomingDtos) {
            ID dtoId = dtoIdGetter.apply(dto);
            if (dtoId == null) {
                ENTITY newEntity = newEntityFactory.apply(dto);
                existingEntities.add(newEntity);
                added.add(newEntity);
            } else {
                ENTITY existing = entitiesById.get(dtoId);
                if (existing == null) {
                    throw new IllegalArgumentException(
                            "DTO refers to an entity that does not belong to the aggregate. ID='" + dtoId
                                    + "'\n Should be one of IDs: " + entitiesById.keySet());
                }
                applyUpdates.accept(dto, existing);
                updated.add(existing);
            }
        }

        return new MergeListResult<>(updated, removed, added);
    }

}
