/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2013 Aaron Weiss
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package provider.nx.core.format;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import provider.nx.core.NXFile;

/**
 * The basic information container for the NX file format.
 *
 * @author Aaron Weiss
 * @version 1.0.1
 * @since 5/26/13
 */
public abstract class NXNode implements Iterable<NXNode> {
	private static final EmptyNodeIterator EMPTY_NODE_ITERATOR = new EmptyNodeIterator();

	protected final String name;
	protected final NXFile file;
	protected final long childIndex;
	protected final int childCount;
	private final Map<String, NXNode> children;

	/**
	 * Sets up the basic information for the {@code NXNode}.
	 *
	 * @param name
	 *            the name of the node
	 * @param file
	 *            the file the node is from
	 * @param childIndex
	 *            the index of the first child of the node
	 * @param childCount
	 *            the number of children
	 */
	public NXNode(String name, NXFile file, long childIndex, int childCount) {
		this.name = name;
		this.file = file;
		this.childIndex = childIndex;
		this.childCount = childCount;
		if (childCount > 0) {
			this.children = new HashMap<String, NXNode>();
		} else {
			this.children = null;
		}
	}

	/**
	 * Populates the children {@code Map} for this node.
	 */
	public void populateChildren() {
		if ((this.childCount == 0) || !this.children.isEmpty()) {
			return;
		}
		final NXNode[] nodes = this.file.getNodes();
		for (int i = (int) this.childIndex; i < (this.childIndex + this.childCount); i++) {
			this.children.put(nodes[i].getName(), nodes[i]);
		}
	}

	/**
	 * Gets the value of this node universally.
	 *
	 * @return the value as an {@code Object}
	 */
	public abstract Object get();

	/**
	 * Gets a child node by {@code name}. Returns null if child is not present.
	 *
	 * @param name
	 *            the name of the child
	 * @return the child {@code NXNode}
	 */
	public NXNode getChild(String name) {
		if (this.childCount == 0) {
			return null;
		}
		return this.children.get(name);
	}

	/**
	 * Determines whether or not this node has a child by the specified
	 * {@code name}.
	 *
	 * @param name
	 *            the name of the child
	 * @return whether or not this node has a child by the specified
	 *         {@code name}
	 */
	public boolean hasChild(String name) {
		if (this.childCount == 0) {
			return false;
		}
		return this.children.get(name) != null;
	}

	/**
	 * Gets the name of the node.
	 *
	 * @return the name of this node
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the file that the node belonged to.
	 *
	 * @return the file owning this node
	 */
	public NXFile getFile() {
		return this.file;
	}

	/**
	 * Gets the number of children had by this node.
	 *
	 * @return number of child nodes
	 */
	public int getChildCount() {
		return this.childCount;
	}

	/**
	 * Gets the index of the first child of this node.
	 *
	 * @return first child node index
	 */
	public long getFirstChildIndex() {
		return this.childIndex;
	}

	@Override
	public String toString() {
		return this.getName();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (!(obj instanceof NXNode)) {
			return false;
		} else {
			return ((NXNode) obj).getName().equals(this.getName())
					&& (((NXNode) obj).getChildCount() == this.getChildCount())
					&& (((NXNode) obj).getFirstChildIndex() == this
							.getFirstChildIndex())
					&& (((((NXNode) obj).get() == null) && (this.get() == null)) || ((NXNode) obj)
							.get().equals(this.get()));
		}
	}

	@Override
	public Iterator<NXNode> iterator() {
		return (this.childCount == 0) ? EMPTY_NODE_ITERATOR : this.children
				.values().iterator();
	}

	/**
	 * A silent, empty iterator for childless {@code NXNode}s.
	 *
	 * @author Aaron Weiss
	 * @version 1.0
	 * @since 5/26/13
	 */
	private static class EmptyNodeIterator implements Iterator<NXNode> {
		/**
		 * Creates an {@code EmptyNodeIterator}.
		 */
		private EmptyNodeIterator() {
			return;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public NXNode next() {
			return null;
		}

		@Override
		public void remove() {
			return;
		}
	}
}
