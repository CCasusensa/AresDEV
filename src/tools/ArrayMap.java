/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tools;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ArrayMap<K, V> extends AbstractMap<K, V> {

	static class Entry<K, V> implements Map.Entry<K, V> {
		protected K key;
		protected V value;

		public Entry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return this.key;
		}

		@Override
		public V getValue() {
			return this.value;
		}

		@Override
		public V setValue(V newValue) {
			final V oldValue = this.value;
			this.value = newValue;
			return oldValue;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Map.Entry<?, ?>)) {
				return false;
			}
			final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			return (this.key == null ? e.getKey() == null : this.key.equals(e
					.getKey()))
					&& (this.value == null ? e.getValue() == null : this.value
							.equals(e.getValue()));
		}

		@Override
		public int hashCode() {
			final int keyHash = (this.key == null ? 0 : this.key.hashCode());
			final int valueHash = (this.value == null ? 0 : this.value
					.hashCode());
			return keyHash ^ valueHash;
		}

		@Override
		public String toString() {
			return this.key + "=" + this.value;
		}
	}

	private Set<? extends java.util.Map.Entry<K, V>> entries = null;
	private final ArrayList<Entry<K, V>> list;

	public ArrayMap() {
		this.list = new ArrayList<>();
	}

	public ArrayMap(Map<K, V> map) {
		this.list = new ArrayList<>();
		this.putAll(map);
	}

	public ArrayMap(int initialCapacity) {
		this.list = new ArrayList<>(initialCapacity);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		if (this.entries == null) {
			this.entries = new AbstractSet<Entry<K, V>>() {
				@Override
				public void clear() {
					throw new UnsupportedOperationException();
				}

				@Override
				public Iterator<Entry<K, V>> iterator() {
					return ArrayMap.this.list.iterator();
				}

				@Override
				public int size() {
					return ArrayMap.this.list.size();
				}
			};
		}
		return (Set<java.util.Map.Entry<K, V>>) this.entries;
	}

	@Override
	public V put(K key, V value) {
		final int size = this.list.size();
		Entry<K, V> entry = null;
		int i;
		if (key == null) {
			for (i = 0; i < size; i++) {
				entry = (this.list.get(i));
				if (entry.getKey() == null) {
					break;
				}
			}
		} else {
			for (i = 0; i < size; i++) {
				entry = (this.list.get(i));
				if (key.equals(entry.getKey())) {
					break;
				}
			}
		}
		V oldValue = null;
		if (i < size) {
			oldValue = entry.getValue();
			entry.setValue(value);
		} else {
			this.list.add(new Entry<>(key, value));
		}
		return oldValue;
	}
}
