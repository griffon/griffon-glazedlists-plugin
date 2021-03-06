/*
 * Copyright 2009-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import griffon.core.GriffonApplication

import ca.odell.glazedlists.EventList
import ca.odell.glazedlists.SortedList
import ca.odell.glazedlists.TextFilterator
import ca.odell.glazedlists.TreeList
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser
import ca.odell.glazedlists.swing.*
import ca.odell.glazedlists.util.concurrent.Lock
import griffon.glazedlists.factory.*

import javax.swing.JComboBox
import javax.swing.JTable
import javax.swing.ListSelectionModel
import java.text.Format

/**
 * @author Andres Almiray
 */
class GlazedlistsGriffonAddon {
    void addonInit(GriffonApplication app) {
        final withLockHandler = { Lock lock, Closure closure ->
            lock.lock()
            try { closure() }
            finally { lock.unlock() }
        }
        Lock.metaClass.withLock = { Closure closure ->
            withLockHandler(delegate, closure)
        }

        EventList.metaClass.withReadLock = { Closure closure ->
            withLockHandler(delegate.readWriteLock.readLock(), closure)
        }

        EventList.metaClass.withWriteLock = { Closure closure ->
            withLockHandler(delegate.readWriteLock.writeLock(), closure)
        }
    }

    Map factories = [
        defaultTableFormat: new DefaultTableFormatFactory(),
        defaultWritableTableFormat: new DefaultWritableTableFormatFactory(),
        defaultAdvancedTableFormat: new DefaultAdvancedTableFormatFactory(),
        eventComboBoxModel: new EventComboBoxModelFactory(),
        eventListModel: new EventListModelFactory(),
        eventTreeModel: new EventTreeModelFactory(),
        afterEdit: new EventTreeModelUpdateFactory(),
        eventTableModel: new EventTableModelFactory(),
        eventJXTableModel: new EventJXTableModelFactory()
    ]

    Map methods = [
        installTableComparatorChooser: { Map args ->
            def params = [target: current, strategy: AbstractTableComparatorChooser.SINGLE_COLUMN] + args
            if (!(params.target instanceof JTable)) {
                throw new IllegalArgumentException("target: must be a JTable!")
            }
            if (!(params.source instanceof EventList)) {
                throw new IllegalArgumentException("source: must be an EventList!")
            }
            TableComparatorChooser.install(params.target, params.source, params.strategy)
        },

        installTreeTableSupport: { Map args ->
            def params = [target: current, index: 1i] + args
            if (!(params.target instanceof JTable)) {
                throw new IllegalArgumentException("target: must be a JTable!")
            }
            if (!(params.source instanceof TreeList)) {
                throw new IllegalArgumentException("source: must be an TreeList!")
            }
            TreeTableSupport.install(params.target, params.source, params.index as int)
        },

        installComboBoxAutoCompleteSupport: { Map args ->
            def params = [target: current] + args
            if (!(params.target instanceof JComboBox)) {
                throw new IllegalArgumentException("target: must be a JComboBox!")
            }
            if (!(params.items instanceof EventList)) {
                throw new IllegalArgumentException("items: must be an EventList!")
            }
            if (args.textFilterator) {
                if (!(params.textFilterator instanceof TextFilterator)) {
                    throw new IllegalArgumentException("textFilterator: must be an ${TextFilterator.class.name}!")
                }
                if (args.format) {
                    if (!(params.format instanceof Format)) {
                        throw new IllegalArgumentException("format: must be an ${Format.class.name}!")
                    }
                    AutoCompleteSupport.install(params.target, params.items, params.textFilterator, params.format)
                } else {
                    AutoCompleteSupport.install(params.target, params.items, params.textFilterator)
                }
            } else {
                AutoCompleteSupport.install(params.target, params.items)
            }
        },

        installEventSelectionModel: { Map args ->
            def params = [target: current, mode: AbstractTableComparatorChooser.SINGLE_COLUMN] + args
            if (!(params.target instanceof JTable)) {
                throw new IllegalArgumentException("target: must be a JTable!")
            }
            if (!(params.source instanceof EventList)) {
                throw new IllegalArgumentException("source: must be an EventList!")
            }
            if (!params.containsKey('selectionMode')) {
                params.selectionMode = ListSelectionModel.SINGLE_SELECTION
            }
            if (!(params.selectionMode instanceof Integer) || !(0..2).contains(params.selectionMode)) {
                throw new IllegalArgumentException("source: must be a one of ListSelectionModel.SINGLE_SELECTION, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION or ListSelectionModel.SINGLE_INTERVAL_SELECTION!")
            }
            def selectionModel = new EventSelectionModel(params.source)
            selectionModel.selectionMode = params.selectionMode
            params.target.selectionModel = selectionModel
            return selectionModel
        },

        installJXTableSorting: { Map args ->
            def params = [target: current] + args
            Class jxtableClass
            try {
                jxtableClass = Class.forName("org.jdesktop.swingx.JXTable")
            } catch (e) {}
            if (!(jxtableClass && jxtable.isAssignableFrom(params.target))) {
                throw new IllegalArgumentException("target: must be a JXTable!")
            }
            if (!(params.source instanceof SortedList)) {
                throw new IllegalArgumentException("source: must be an SortedList!")
            }
            if (!params.containsKey('multiple')) {
                params.multiple = false
            }
            def jxts = EventListJXTableSorting.install(params.target, params.source)
            jxts.multipleColumnSort = params.multiple
            jxts
        }
    ]
}
