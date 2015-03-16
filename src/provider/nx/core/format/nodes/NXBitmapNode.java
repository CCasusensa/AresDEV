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
package provider.nx.core.format.nodes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.awt.image.BufferedImage;

import provider.nx.core.NXFile;
import provider.nx.core.format.NXHeader;
import provider.nx.core.format.NXNode;
import provider.nx.core.util.Decompressor;
import provider.nx.core.util.SeekableLittleEndianAccessor;

/**
 * An {@code NXNode} representing an Audio {@code ByteBuf}.
 *
 * @author Aaron Weiss
 * @version 1.1.0
 * @since 5/27/13
 */
public class NXBitmapNode extends NXNode {
	private static Bitmap[] bitmaps;
	private final long bitmapIndex;
	private final int width, height;

	/**
	 * Creates a new {@code NXBitmapNode}.
	 *
	 * @param name
	 *            the name of the node
	 * @param file
	 *            the file the node is from
	 * @param childIndex
	 *            the index of the first child of the node
	 * @param childCount
	 *            the number of children
	 * @param slea
	 *            the {@code SeekableLittleEndianAccessor} to read from
	 */
	public NXBitmapNode(String name, NXFile file, long childIndex,
			int childCount, SeekableLittleEndianAccessor slea) {
		super(name, file, childIndex, childCount);
		this.bitmapIndex = slea.getUnsignedInt();
		this.width = slea.getUnsignedShort();
		this.height = slea.getUnsignedShort();
	}

	@Override
	public Object get() {
		return this.getImage();
	}

	/**
	 * Gets the value of this node as a {@code BufferedImage}.
	 *
	 * @return the node value
	 */
	public BufferedImage getImage() {
		return bitmaps[(int) this.bitmapIndex]
				.getImage(this.width, this.height);
	}

	/**
	 * Populates the lazy-loaded table for {@code Bitmap}s.
	 *
	 * @param header
	 *            the header corresponding to the file
	 * @param slea
	 *            the {@code SeekableLittleEndianAccessor} to read from
	 */
	public static void populateBitmapsTable(NXHeader header,
			SeekableLittleEndianAccessor slea) {
		slea.seek(header.getBitmapOffset());
		bitmaps = new Bitmap[(int) header.getBitmapCount()];
		for (int i = 0; i < bitmaps.length; i++) {
			bitmaps[i] = new Bitmap(slea);
		}
	}

	/**
	 * A lazy-loaded equivalent of {@code BufferedImage}.
	 *
	 * @author Aaron Weiss
	 * @version 1.0
	 * @since 5/27/13
	 */
	private static class Bitmap {
		private final SeekableLittleEndianAccessor slea;
		private final long bitmapOffset;

		/**
		 * Creates a lazy-loaded {@code BufferedImage}.
		 *
		 * @param slea
		 */
		public Bitmap(SeekableLittleEndianAccessor slea) {
			this.slea = slea;
			this.bitmapOffset = slea.getLong();
		}

		/**
		 * Loads a {@code BufferedImage} of the desired {@code width} and
		 * {@code height}.
		 *
		 * @param width
		 *            the width of the image
		 * @param height
		 *            the height of the image
		 * @return the loaded image
		 */
		public BufferedImage getImage(int width, int height) {
			this.slea.seek(this.bitmapOffset);
			final ByteBuf image = Unpooled.wrappedBuffer(Decompressor
					.decompress(this.slea.getBytes((int) this.slea
							.getUnsignedInt()), width * height * 4));
			final BufferedImage ret = new BufferedImage(width, height,
					BufferedImage.TYPE_INT_ARGB);
			for (int h = 0; h < height; h++) {
				for (int w = 0; w < width; w++) {
					final int b = image.readUnsignedByte();
					final int g = image.readUnsignedByte();
					final int r = image.readUnsignedByte();
					final int a = image.readUnsignedByte();
					ret.setRGB(w, h, (a << 24) | (r << 16) | (g << 8) | b);
				}
			}
			return ret;
		}
	}
}
