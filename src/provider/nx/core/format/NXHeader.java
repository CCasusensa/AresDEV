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

import provider.nx.core.NXException;
import provider.nx.core.NXFile;
import provider.nx.core.util.SeekableLittleEndianAccessor;

/**
 * The file header for all NX files.
 *
 * @author Aaron Weiss
 * @version 1.0.0
 * @since 5/26/13
 */
public class NXHeader {
	/**
	 * The expected "magic" file format string.
	 */
	public static final String MAGIC = "PKG4";

	private final NXFile file;
	private final String magic;
	private final long nodeCount, nodeOffset;
	private final long stringCount, stringOffset;
	private final long bitmapCount, bitmapOffset;
	private final long soundCount, soundOffset;

	/**
	 * Creates a new {@code NXHeader} from a
	 * {@code SeekableLittleEndianAccessor}.
	 *
	 * @param slea
	 *            the accessor to read from
	 */
	public NXHeader(NXFile file, SeekableLittleEndianAccessor slea) {
		this.file = file;
		slea.seek(0);
		this.magic = slea.getUTFString(4);
		if (!this.magic.equals(MAGIC)) {
			throw new NXException("Cannot read file. Invalid format "
					+ this.magic + ", expecting " + MAGIC);
		}
		this.nodeCount = slea.getUnsignedInt();
		this.nodeOffset = slea.getLong();
		this.stringCount = slea.getUnsignedInt();
		this.stringOffset = slea.getLong();
		this.bitmapCount = slea.getUnsignedInt();
		this.bitmapOffset = slea.getLong();
		this.soundCount = slea.getUnsignedInt();
		this.soundOffset = slea.getLong();
	}

	/**
	 * Gets the {@code NXFile} that the header was read from.
	 *
	 * @return the header's file
	 */
	public NXFile getFile() {
		return this.file;
	}

	/**
	 * Gets the total number of nodes in the file.
	 *
	 * @return total number of nodes
	 */
	public long getNodeCount() {
		return this.nodeCount;
	}

	/**
	 * Gets the first offset for the node block.
	 *
	 * @return first node offset
	 */
	public long getNodeOffset() {
		return this.nodeOffset;
	}

	/**
	 * Gets the total number of strings in the file.
	 *
	 * @return total number of strings
	 */
	public long getStringCount() {
		return this.stringCount;
	}

	/**
	 * Gets the first offset for the string block.
	 *
	 * @return first string offset
	 */
	public long getStringOffset() {
		return this.stringOffset;
	}

	/**
	 * Gets the total number of bitmaps in the file.
	 *
	 * @return total number of bitmaps
	 */
	public long getBitmapCount() {
		return this.bitmapCount;
	}

	/**
	 * Gets the first offset for the bitmap block.
	 *
	 * @return first bitmap offset
	 */
	public long getBitmapOffset() {
		return this.bitmapOffset;
	}

	/**
	 * Gets the total number of MP3 sounds in the file.
	 *
	 * @return total number of MP3s
	 */
	public long getSoundCount() {
		return this.soundCount;
	}

	/**
	 * Gets the first offset for the MP3 sound block.
	 *
	 * @return first MP3 offset
	 */
	public long getSoundOffset() {
		return this.soundOffset;
	}
}
