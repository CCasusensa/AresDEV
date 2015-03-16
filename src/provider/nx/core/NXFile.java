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
package provider.nx.core;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import provider.nx.core.format.NXHeader;
import provider.nx.core.format.NXNode;
import provider.nx.core.format.nodes.NXAudioNode;
import provider.nx.core.format.nodes.NXBitmapNode;
import provider.nx.core.format.nodes.NXStringNode;
import provider.nx.core.util.NodeParser;
import provider.nx.core.util.SeekableLittleEndianAccessor;

/**
 * An object for reading PKG4 NX files, defaults to being memory-mapped.
 *
 * @author Aaron Weiss
 * @version 1.1.0
 * @since 5/26/13
 */
public class NXFile {
	public static final Logger logger = LoggerFactory.getLogger(NXFile.class);
	private final SeekableLittleEndianAccessor slea;
	private boolean parsed = false;

	private NXHeader header;
	private NXNode[] nodes;

	/**
	 * Creates a new {@code NXFile} from the specified {@code path}.
	 *
	 * @param path
	 *            the absolute or relative path to the file
	 * @throws IOException
	 *             if something goes wrong in reading the file
	 */
	public NXFile(String path) throws IOException {
		this(Paths.get(path));
	}

	/**
	 * Creates a new {@code NXFile} from the specified {@code path}.
	 *
	 * @param path
	 *            the absolute or relative path to the file
	 * @throws IOException
	 *             if something goes wrong in reading the file
	 */
	public NXFile(Path path) throws IOException {
		this(path, LibraryMode.MEMORY_MAPPED);
	}

	/**
	 * Creates a new {@code NXFile} from the specified {@code path} with the
	 * option to parse later.
	 *
	 * @param path
	 *            the absolute or relative path to the file
	 * @param parsedImmediately
	 *            whether or not to parse all nodes immediately
	 * @throws IOException
	 *             if something goes wrong in reading the file
	 * @deprecated As of 1.1.0, users should use
	 *             {@link #NXFile(String, LibraryMode)} instead.
	 */
	@Deprecated
	public NXFile(String path, boolean parsedImmediately) throws IOException {
		this(Paths.get(path), parsedImmediately);
	}

	/**
	 * Creates a new {@code NXFile} from the specified {@code path} with the
	 * option to parse later.
	 *
	 * @param path
	 *            the absolute or relative path to the file
	 * @param parsedImmediately
	 *            whether or not to parse the file immediately
	 * @throws IOException
	 *             if something goes wrong in reading the file
	 * @deprecated As of 1.1.0, users should use
	 *             {@link #NXFile(java.nio.file.Path, LibraryMode)} instead.
	 */
	@Deprecated
	public NXFile(Path path, boolean parsedImmediately) throws IOException {
		this(path, (parsedImmediately) ? LibraryMode.MAPPED_AND_PARSED
				: LibraryMode.MEMORY_MAPPED);
	}

	/**
	 * Creates a new {@code NXFile} from the specified {@code path} in the
	 * desired {@code mode}.
	 *
	 * @param path
	 *            the absolute or relative path to the file
	 * @param mode
	 *            the {@code LibraryMode} for handling this file
	 * @throws IOException
	 *             if something goes wrong in reading the file
	 */
	public NXFile(String path, LibraryMode mode) throws IOException {
		this(Paths.get(path), mode);
	}

	/**
	 * Creates a new {@code NXFile} from the specified {@code path} in the
	 * desired {@code mode}.
	 *
	 * @param path
	 *            the absolute or relative path to the file
	 * @param mode
	 *            the {@code LibraryMode} for handling this file
	 * @throws IOException
	 *             if something goes wrong in reading the file
	 */
	public NXFile(Path path, LibraryMode mode) throws IOException {
		if (mode.isMemoryMapped()) {
			final FileChannel channel = FileChannel.open(path);
			this.slea = new SeekableLittleEndianAccessor(channel.map(
					FileChannel.MapMode.READ_ONLY, 0, channel.size()));
		} else {
			this.slea = new SeekableLittleEndianAccessor(
					Files.readAllBytes(path));
		}

		if (mode.isParsedImmediately()) {
			this.parse();
		}
	}

	/**
	 * Parses the file completely.
	 */
	public void parse() {
		if (this.parsed) {
			return;
		}
		this.header = new NXHeader(this, this.slea);
		NXStringNode.populateStringTable(this.header, this.slea);
		NXBitmapNode.populateBitmapsTable(this.header, this.slea);
		NXAudioNode.populateAudioBufTable(this.header, this.slea);
		this.populateNodesTable();
		this.populateNodeChildren();
		this.parsed = true;
	}

	/**
	 * Populates the node table by parsing all nodes.
	 */
	private void populateNodesTable() {
		this.slea.seek(this.header.getNodeOffset());
		this.nodes = new NXNode[(int) this.header.getNodeCount()];
		for (int i = 0; i < this.nodes.length; i++) {
			this.nodes[i] = NodeParser.parseNode(this.header, this.slea);
		}
	}

	/**
	 * Populates the children of all nodes.
	 */
	private void populateNodeChildren() {
		for (final NXNode node : this.nodes) {
			node.populateChildren();
		}
	}

	/**
	 * Gets the {@code NXHeader} of this file.
	 *
	 * @return this file's header
	 */
	public NXHeader getHeader() {
		return this.header;
	}

	/**
	 * Gets whether or not this file has been parsed.
	 *
	 * @return whether or not this file has been parsed
	 */
	public boolean isParsed() {
		return this.parsed;
	}

	/**
	 * Gets an array of all of the {@code NXNode}s in this file.
	 *
	 * @return an array of all the nodes in this file
	 */
	public NXNode[] getNodes() {
		return this.nodes;
	}

	/**
	 * Gets the root {@code NXNode} of the file.
	 *
	 * @return the file's root node
	 */
	public NXNode getRoot() {
		return this.nodes[0];
	}

	/**
	 * Resolves the desired {@code path} to an {@code NXNode}.
	 *
	 * @param path
	 *            the path to the node
	 * @return the desired node
	 */
	public NXNode resolve(String path) {
		return this.resolve(path.split("/"));
	}

	/**
	 * Resolves the desired {@code path} to an {@code NXNode}.
	 *
	 * @param path
	 *            the path to the node
	 * @return the desired node
	 */
	public NXNode resolve(String[] path) {
		NXNode cursor = this.getRoot();
		for (final String element : path) {
			if (cursor == null) {
				return null;
			}
			cursor = cursor.getChild(element);
		}
		return cursor;
	}

	/**
	 * An enumeration of possible modes for using pkgnx.
	 *
	 * @author Aaron Weiss
	 * @version 1.0.0
	 * @since 6/8/13
	 */
	public static enum LibraryMode {
		/**
		 * Fully loads file into memory and parses data on command.
		 */
		FULL_LOAD_ON_DEMAND(false, false),

		/**
		 * Parses data on command using a memory-mapped file.
		 */
		MEMORY_MAPPED(false, true),

		/**
		 * Fully loads file into memory and parses data immediately.
		 */
		PARSED_IMMEDIATELY(true, false),

		/**
		 * Parses data immediately using a memory-mapped file.
		 */
		MAPPED_AND_PARSED(true, true);
		private final boolean parsedImmediately, memoryMapped;

		/**
		 * Creates a new {@code LibraryMode} for pkgnx.
		 *
		 * @param parsedImmediately
		 *            whether or not to parse on file construction
		 * @param memoryMapped
		 *            whether or not to use memory-mapped files
		 */
		private LibraryMode(boolean parsedImmediately, boolean memoryMapped) {
			this.parsedImmediately = parsedImmediately;
			this.memoryMapped = memoryMapped;
		}

		/**
		 * Gets whether or not this mode causes files to parse immediately.
		 *
		 * @return whether or not to parse on file construction
		 */
		public boolean isParsedImmediately() {
			return this.parsedImmediately;
		}

		/**
		 * Gets whether or not this mode uses memory mapped files.
		 *
		 * @return whether or not to use memory-mapped files
		 */
		public boolean isMemoryMapped() {
			return this.memoryMapped;
		}
	}
}
