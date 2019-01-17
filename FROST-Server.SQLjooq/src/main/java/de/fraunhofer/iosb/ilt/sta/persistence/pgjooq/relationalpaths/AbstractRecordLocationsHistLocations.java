package de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.relationalpaths;

import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;

public abstract class AbstractRecordLocationsHistLocations<J> extends UpdatableRecordImpl<AbstractRecordLocationsHistLocations<J>> implements Record2<J, J> {

    private static final long serialVersionUID = -1432180691;

    /**
     * Setter for <code>public.LOCATIONS_HIST_LOCATIONS.LOCATION_ID</code>.
     */
    public final void setLocationId(J value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.LOCATIONS_HIST_LOCATIONS.LOCATION_ID</code>.
     */
    public final J getLocationId() {
        return (J) get(0);
    }

    /**
     * Setter for <code>public.LOCATIONS_HIST_LOCATIONS.HIST_LOCATION_ID</code>.
     */
    public final void setHistLocationId(J value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.LOCATIONS_HIST_LOCATIONS.HIST_LOCATION_ID</code>.
     */
    public final J getHistLocationId() {
        return (J) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public final Record2<J, J> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public final Row2<J, J> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Row2<J, J> valuesRow() {
        return (Row2) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final J component1() {
        return getLocationId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final J component2() {
        return getHistLocationId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final J value1() {
        return getLocationId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final J value2() {
        return getHistLocationId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final AbstractRecordLocationsHistLocations value1(J value) {
        setLocationId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final AbstractRecordLocationsHistLocations value2(J value) {
        setHistLocationId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final AbstractRecordLocationsHistLocations values(J value1, J value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    /**
     * Create a detached LocationsHistLocationsRecord
     */
    public AbstractRecordLocationsHistLocations(AbstractTableLocationsHistLocations<J> table) {
        super(table);
    }

    /**
     * Create a detached, initialised LocationsHistLocationsRecord
     */
    public AbstractRecordLocationsHistLocations(AbstractTableLocationsHistLocations<J> table, J locationId, J histLocationId) {
        super(table);

        set(0, locationId);
        set(1, histLocationId);
    }
}