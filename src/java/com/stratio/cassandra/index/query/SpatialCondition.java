/*
 * Copyright 2014, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.cassandra.index.query;

import com.spatial4j.core.context.SpatialContext;
import com.stratio.cassandra.index.schema.ColumnMapper;
import com.stratio.cassandra.index.schema.ColumnMapperShape;
import com.stratio.cassandra.index.schema.Schema;
import com.stratio.cassandra.index.spatial.Shape;
import org.apache.lucene.search.Query;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * A {@link com.stratio.cassandra.index.query.Condition} implementation that matches documents containing a value for a
 * field.
 *
 * @author Andres de la Pena <adelapena@stratio.com>
 */
public class SpatialCondition extends Condition {

    /** The name of the field to be matched. */
    @JsonProperty("field")
    private final String field;

    /** The name of the field to be matched. */
    @JsonProperty("operation")
    private final String operation;

    /** The value of the field to be matched. */
    @JsonProperty("shape")
    private Shape shape;

    /**
     * Constructor using the field name and the value to be matched.
     *
     * @param boost The boost for this query clause. Documents matching this clause will (in addition to the normal
     *              weightings) have their score multiplied by {@code boost}. If {@code null}, then {@link
     *              #DEFAULT_BOOST} is used as default.
     * @param field The name of the field to be matched.
     * @param shape The shape to be matched.
     */
    @JsonCreator
    public SpatialCondition(@JsonProperty("boost") Float boost,
                            @JsonProperty("field") String field,
                            @JsonProperty("operation") String operation,
                            @JsonProperty("shape") Shape shape) {
        super(boost);
        this.field = field;
        this.shape = shape;
        this.operation = operation;
    }

    /** {@inheritDoc} */
    @Override
    public Query query(Schema schema) {

        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name required");
        }
        if (shape == null) {
            throw new IllegalArgumentException("Field value required");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Operation required");
        }

        SpatialOperation spatialOperation = spatialOperation();
        if (spatialOperation == null) {
            throw new IllegalArgumentException("Operation is invalid");
        }

        ColumnMapper columnMapper = schema.getMapper(field);
        if (columnMapper == null || !(columnMapper instanceof ColumnMapperShape)) {
            throw new IllegalArgumentException("Not mapper found");
        }
        ColumnMapperShape mapper = (ColumnMapperShape) columnMapper;
        SpatialContext spatialContext = mapper.getSpatialContext();
        SpatialStrategy spatialStrategy = mapper.getStrategy(field);
        SpatialArgs args = new SpatialArgs(spatialOperation, shape.toSpatial4j(spatialContext));
        Query query = spatialStrategy.makeQuery(args);
        query.setBoost(boost);
        return query;
    }

    private SpatialOperation spatialOperation() {
        switch (operation.toLowerCase()) {
            case "bboxwithin":
                return SpatialOperation.BBoxWithin;
            case "contains":
                return SpatialOperation.Contains;
            case "intersects":
                return SpatialOperation.Intersects;
            case "isequalto":
                return SpatialOperation.IsEqualTo;
            case "isdisjointto":
                return SpatialOperation.IsDisjointTo;
            case "iswithin":
                return SpatialOperation.IsWithin;
            case "overlaps":
                return SpatialOperation.Overlaps;
            default:
                return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SpatialCondition{");
        sb.append("field='").append(field).append('\'');
        sb.append(", operation='").append(operation).append('\'');
        sb.append(", shape=").append(shape);
        sb.append('}');
        return sb.toString();
    }
}