/*
 * Copyright (C) 2016 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
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
package de.fraunhofer.iosb.ilt.sta.query.expression.constant;

import de.fraunhofer.iosb.ilt.sta.query.expression.ExpressionVisitor;
import org.joda.time.Interval;

/**
 *
 * @author scf
 */
public class IntervalConstant extends Constant<Interval> {

    // TODO: Convert to threeten's PeriodDuration
    // https://www.threeten.org/threeten-extra/apidocs/org.threeten.extra/org/threeten/extra/PeriodDuration.html
    public IntervalConstant(Interval value) {
        super(value);
    }

    public IntervalConstant(String value) {
        super(Interval.parse(value));
    }

    @Override
    public String toUrl() {
        return getValue().toString();
    }

    @Override
    public <O> O accept(ExpressionVisitor<O> visitor) {
        return visitor.visit(this);
    }

}
