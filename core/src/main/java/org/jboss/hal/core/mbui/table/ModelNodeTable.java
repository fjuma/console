/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.hal.core.mbui.table;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.hal.ballroom.table.DataTable;
import org.jboss.hal.ballroom.table.DataTableButton;
import org.jboss.hal.dmr.ModelNode;
import org.jboss.hal.dmr.Property;
import org.jboss.hal.meta.description.ResourceDescription;
import org.jboss.hal.meta.security.SecurityContext;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jboss.hal.dmr.ModelDescriptionConstants.ATTRIBUTES;

/**
 * @author Harald Pehl
 */
public class ModelNodeTable<T extends ModelNode> extends DataTable<T> {

    public static class Builder<T extends ModelNode> {

        final String id;
        final ProvidesKey<T> keyProvider;
        final SecurityContext securityContext;
        final ResourceDescription resourceDescription;
        final List<String> columns;
        final LinkedListMultimap<String, DataTableButton> buttons;

        public Builder(@NonNls final String id, final ProvidesKey<T> keyProvider, final SecurityContext securityContext,
                final ResourceDescription resourceDescription) {
            this.id = id;
            this.keyProvider = keyProvider;
            this.securityContext = securityContext;
            this.resourceDescription = resourceDescription;
            this.columns = new ArrayList<>();
            this.buttons = LinkedListMultimap.create();
        }

        public Builder<T> addColumn(final String first, final String... rest) {
            columns.addAll(Lists.asList(first, rest));
            return this;
        }

        public Builder<T> addButton(DataTableButton button) {
            return addButton(button, DEFAULT_BUTTON_GROUP);
        }

        public Builder<T> addButton(DataTableButton button, String group) {
            buttons.put(group, button);
            return this;
        }

        public ModelNodeTable<T> build() {
            validate();
            return new ModelNodeTable<>(this);
        }

        private void validate() {
            if (columns.isEmpty()) {
                throw new IllegalStateException(tableId() + ": No columns specified");
            }
            if (!resourceDescription.hasDefined(ATTRIBUTES)) {
                throw new IllegalStateException(tableId() + ": No attributes found in resource description\n" + resourceDescription);
            }
        }

        String tableId() {
            return "dataTable(" + id + ")"; //NON-NLS
        }
    }


    private static final Logger logger = LoggerFactory.getLogger(ModelNodeTable.class);

    private final ColumnFactory columnFactory;

    private ModelNodeTable(Builder<T> builder) {
        super(builder.id, builder.keyProvider, builder.securityContext);

        columnFactory = new ColumnFactory();
        for (String column : builder.columns) {
            Property attributeDescription = findDescription(builder.resourceDescription.getAttributes(), column);
            if (attributeDescription == null) {
                logger.error("{}: No attribute description for column '{}' found in resource description\n{}", //NON-NLS
                        builder.tableId(), column, builder.resourceDescription);
                continue;
            }
            ColumnFactory.HeaderColumn<T> headerColumn = columnFactory.createHeaderColumn(attributeDescription);
            addColumn(headerColumn.column, headerColumn.header);
        }

        for (Map.Entry<String, DataTableButton> entry : builder.buttons.entries()) {
            addButton(entry.getValue(), entry.getKey());
        }
    }

    private Property findDescription(final List<Property> attributeDescriptions, final String column) {
        for (Property attributeDescription : attributeDescriptions) {
            if (attributeDescription.getName().equals(column)) {
                return attributeDescription;
            }
        }
        return null;
    }
}
