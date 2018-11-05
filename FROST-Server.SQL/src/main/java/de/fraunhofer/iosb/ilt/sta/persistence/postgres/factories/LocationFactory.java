/*
 * Copyright (C) 2018 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.sta.persistence.postgres.factories;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import de.fraunhofer.iosb.ilt.sta.messagebus.EntityChangedMessage;
import de.fraunhofer.iosb.ilt.sta.model.HistoricalLocation;
import de.fraunhofer.iosb.ilt.sta.model.Location;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.sta.path.EntityProperty;
import de.fraunhofer.iosb.ilt.sta.path.EntityType;
import de.fraunhofer.iosb.ilt.sta.path.Property;
import de.fraunhofer.iosb.ilt.sta.persistence.postgres.DataSize;
import de.fraunhofer.iosb.ilt.sta.persistence.postgres.EntityFactories;
import static de.fraunhofer.iosb.ilt.sta.persistence.postgres.EntityFactories.CAN_NOT_BE_NULL;
import static de.fraunhofer.iosb.ilt.sta.persistence.postgres.EntityFactories.CHANGED_MULTIPLE_ROWS;
import static de.fraunhofer.iosb.ilt.sta.persistence.postgres.EntityFactories.CREATED_HL;
import static de.fraunhofer.iosb.ilt.sta.persistence.postgres.EntityFactories.LINKED_L_TO_HL;
import static de.fraunhofer.iosb.ilt.sta.persistence.postgres.EntityFactories.LINKED_L_TO_T;
import static de.fraunhofer.iosb.ilt.sta.persistence.postgres.EntityFactories.UNLINKED_L_FROM_T;
import de.fraunhofer.iosb.ilt.sta.persistence.postgres.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.sta.persistence.postgres.Utils;
import de.fraunhofer.iosb.ilt.sta.persistence.postgres.relationalpaths.AbstractQHistLocations;
import de.fraunhofer.iosb.ilt.sta.persistence.postgres.relationalpaths.AbstractQLocations;
import de.fraunhofer.iosb.ilt.sta.persistence.postgres.relationalpaths.AbstractQLocationsHistLocations;
import de.fraunhofer.iosb.ilt.sta.persistence.postgres.relationalpaths.AbstractQThingsLocations;
import de.fraunhofer.iosb.ilt.sta.persistence.postgres.relationalpaths.QCollection;
import de.fraunhofer.iosb.ilt.sta.query.Query;
import de.fraunhofer.iosb.ilt.sta.util.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.sta.util.NoSuchEntityException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hylke van der Schaaf
 * @param <I> The type of path used for the ID fields.
 * @param <J> The type of the ID fields.
 */
public class LocationFactory<I extends SimpleExpression<J> & Path<J>, J> implements EntityFactory<Location, I, J> {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationFactory.class);

    private final EntityFactories<I, J> entityFactories;
    private final AbstractQLocations<?, I, J> qInstance;
    private final QCollection<I, J> qCollection;

    public LocationFactory(EntityFactories<I, J> factories, AbstractQLocations<?, I, J> qInstance) {
        this.entityFactories = factories;
        this.qInstance = qInstance;
        this.qCollection = factories.qCollection;
    }

    @Override
    public Location create(Tuple tuple, Query query, DataSize dataSize) {
        Set<Property> select = query == null ? Collections.emptySet() : query.getSelect();
        Location entity = new Location();
        J id = entityFactories.getIdFromTuple(tuple, qInstance.getId());
        if (id != null) {
            entity.setId(entityFactories.idFromObject(id));
        }
        entity.setName(tuple.get(qInstance.name));
        entity.setDescription(tuple.get(qInstance.description));
        String encodingType = tuple.get(qInstance.encodingType);
        entity.setEncodingType(encodingType);
        if (select.isEmpty() || select.contains(EntityProperty.LOCATION)) {
            String locationString = tuple.get(qInstance.location);
            dataSize.increase(locationString == null ? 0 : locationString.length());
            entity.setLocation(Utils.locationFromEncoding(encodingType, locationString));
        }
        if (select.isEmpty() || select.contains(EntityProperty.PROPERTIES)) {
            String props = tuple.get(qInstance.properties);
            entity.setProperties(Utils.jsonToObject(props, Map.class));
        }
        return entity;
    }

    @Override
    public boolean insert(PostgresPersistenceManager<I, J> pm, Location l) throws NoSuchEntityException, IncompleteEntityException {
        SQLQueryFactory qFactory = pm.createQueryFactory();
        AbstractQLocations<? extends AbstractQLocations, I, J> ql = qCollection.qLocations;
        SQLInsertClause insert = qFactory.insert(ql);
        insert.set(ql.name, l.getName());
        insert.set(ql.description, l.getDescription());
        insert.set(ql.properties, EntityFactories.objectToJson(l.getProperties()));

        String encodingType = l.getEncodingType();
        insert.set(ql.encodingType, encodingType);
        EntityFactories.insertGeometry(insert, ql.location, ql.geom, encodingType, l.getLocation());

        entityFactories.insertUserDefinedId(pm, insert, ql.getId(), l);

        J locationId = insert.executeWithKey(ql.getId());
        LOGGER.debug("Inserted Location. Created id = {}.", locationId);
        l.setId(entityFactories.idFromObject(locationId));

        // Link Things
        EntitySet<Thing> things = l.getThings();
        for (Thing t : things) {
            entityFactories.entityExistsOrCreate(pm, t);
            linkThingToLocation(qFactory, t, locationId);
        }

        return true;
    }

    @Override
    public EntityChangedMessage update(PostgresPersistenceManager<I, J> pm, Location l, J locationId) throws NoSuchEntityException, IncompleteEntityException {
        SQLQueryFactory qFactory = pm.createQueryFactory();
        AbstractQLocations<? extends AbstractQLocations, I, J> ql = qCollection.qLocations;
        SQLUpdateClause update = qFactory.update(ql);
        EntityChangedMessage message = new EntityChangedMessage();

        if (l.isSetName()) {
            if (l.getName() == null) {
                throw new IncompleteEntityException("name" + CAN_NOT_BE_NULL);
            }
            update.set(ql.name, l.getName());
            message.addField(EntityProperty.NAME);
        }
        if (l.isSetDescription()) {
            if (l.getDescription() == null) {
                throw new IncompleteEntityException(EntityProperty.DESCRIPTION.jsonName + CAN_NOT_BE_NULL);
            }
            update.set(ql.description, l.getDescription());
            message.addField(EntityProperty.DESCRIPTION);
        }
        if (l.isSetProperties()) {
            update.set(ql.properties, EntityFactories.objectToJson(l.getProperties()));
            message.addField(EntityProperty.PROPERTIES);
        }

        if (l.isSetEncodingType() && l.getEncodingType() == null) {
            throw new IncompleteEntityException("encodingType" + CAN_NOT_BE_NULL);
        }
        if (l.isSetLocation() && l.getLocation() == null) {
            throw new IncompleteEntityException("locations" + CAN_NOT_BE_NULL);
        }
        if (l.isSetEncodingType() && l.getEncodingType() != null && l.isSetLocation() && l.getLocation() != null) {
            String encodingType = l.getEncodingType();
            update.set(ql.encodingType, encodingType);
            EntityFactories.insertGeometry(update, ql.location, ql.geom, encodingType, l.getLocation());
            message.addField(EntityProperty.ENCODINGTYPE);
            message.addField(EntityProperty.LOCATION);
        } else if (l.isSetEncodingType() && l.getEncodingType() != null) {
            String encodingType = l.getEncodingType();
            update.set(ql.encodingType, encodingType);
            message.addField(EntityProperty.ENCODINGTYPE);
        } else if (l.isSetLocation() && l.getLocation() != null) {
            String encodingType = qFactory.select(ql.encodingType)
                    .from(ql)
                    .where(ql.getId().eq(locationId))
                    .fetchFirst();
            Object parsedObject = EntityFactories.reParseGeometry(encodingType, l.getLocation());
            EntityFactories.insertGeometry(update, ql.location, ql.geom, encodingType, parsedObject);
            message.addField(EntityProperty.LOCATION);
        }

        update.where(ql.getId().eq(locationId));
        long count = 0;
        if (!update.isEmpty()) {
            count = update.execute();
        }
        if (count > 1) {
            LOGGER.error("Updating Location {} caused {} rows to change!", locationId, count);
            throw new IllegalStateException(CHANGED_MULTIPLE_ROWS);
        }
        LOGGER.debug("Updated Location {}", locationId);

        // Link HistoricalLocation.
        for (HistoricalLocation hl : l.getHistoricalLocations()) {
            if (hl.getId() == null) {
                throw new IllegalArgumentException("HistoricalLocation with no id.");
            }
            J hlId = (J) hl.getId().getValue();

            AbstractQLocationsHistLocations<? extends AbstractQLocationsHistLocations, I, J> qlhl = qCollection.qLocationsHistLocations;
            SQLInsertClause insert = qFactory.insert(qlhl);
            insert.set(qlhl.getHistLocationId(), hlId);
            insert.set(qlhl.getLocationId(), locationId);
            insert.execute();
            LOGGER.debug(LINKED_L_TO_HL, locationId, hlId);
        }

        // Link Things
        EntitySet<Thing> things = l.getThings();
        for (Thing t : things) {
            if (!entityFactories.entityExists(pm, t)) {
                throw new NoSuchEntityException("Thing not found.");
            }
            linkThingToLocation(qFactory, t, locationId);
        }
        return message;
    }

    private void linkThingToLocation(SQLQueryFactory qFactory, Thing t, J locationId) {
        J thingId = (J) t.getId().getValue();

        // Unlink old Locations from Thing.
        AbstractQThingsLocations<? extends AbstractQThingsLocations, I, J> qtl = qCollection.qThingsLocations;
        long delCount = qFactory.delete(qtl).where(qtl.getThingId().eq(thingId)).execute();
        LOGGER.debug(UNLINKED_L_FROM_T, delCount, thingId);

        // Link new Location to thing.
        SQLInsertClause linkInsert = qFactory.insert(qtl);
        linkInsert.set(qtl.getThingId(), thingId);
        linkInsert.set(qtl.getLocationId(), locationId);
        linkInsert.execute();
        LOGGER.debug(LINKED_L_TO_T, locationId, thingId);

        // Create HistoricalLocation for Thing
        AbstractQHistLocations<? extends AbstractQHistLocations, I, J> qhl = qCollection.qHistLocations;
        linkInsert = qFactory.insert(qhl);
        linkInsert.set(qhl.getThingId(), thingId);
        linkInsert.set(qhl.time, new Timestamp(Calendar.getInstance().getTimeInMillis()));
        // TODO: maybe use histLocationId based on locationId
        J histLocationId = linkInsert.executeWithKey(qhl.getId());
        LOGGER.debug(CREATED_HL, histLocationId);

        // Link Location to HistoricalLocation.
        AbstractQLocationsHistLocations<? extends AbstractQLocationsHistLocations, I, J> qlhl = qCollection.qLocationsHistLocations;
        qFactory.insert(qlhl)
                .set(qlhl.getHistLocationId(), histLocationId)
                .set(qlhl.getLocationId(), locationId)
                .execute();
        LOGGER.debug(LINKED_L_TO_HL, locationId, histLocationId);
    }

    @Override
    public I getPrimaryKey() {
        return qInstance.getId();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.LOCATION;
    }

}
