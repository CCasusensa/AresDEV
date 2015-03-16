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
import provider.nx.core.NXFile;
import provider.nx.core.format.NXHeader;
import provider.nx.core.format.NXNode;
import provider.nx.core.util.SeekableLittleEndianAccessor;

/**
 * An {@code NXNode} representing an Audio {@code ByteBuf}.
 *
 * @author Aaron Weiss
 * @version 1.1.0
 * @since 5/27/13
 */
public class NXAudioNode extends NXNode {
	private static AudioBuf[] audioBufs;
	private final long mp3Index, length;

	/**
	 * Creates a new {@code NXAudioNode}.
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
	public NXAudioNode(String name, NXFile file, long childIndex,
			int childCount, SeekableLittleEndianAccessor slea) {
		super(name, file, childIndex, childCount);
		this.mp3Index = slea.getUnsignedInt();
		this.length = slea.getUnsignedInt();
	}

	@Override
	public Object get() {
		return this.getAudioBuf();
	}

	/**
	 * Gets the value of this node as a {@code ByteBuf}.
	 *
	 * @return the node value
	 */
	public ByteBuf getAudioBuf() {
		return audioBufs[(int) this.mp3Index].getAudioBuf(this.length);
	}

	/**
	 * Populates the lazy-loaded table for Audio {@code ByteBuf}s.
	 *
	 * @param header
	 *            the header corresponding to the file
	 * @param slea
	 *            the {@code SeekableLittleEndianAccessor} to read from
	 */
	public static void populateAudioBufTable(NXHeader header,
			SeekableLittleEndianAccessor slea) {
		slea.seek(header.getSoundOffset());
		audioBufs = new AudioBuf[(int) header.getSoundCount()];
		for (int i = 0; i < audioBufs.length; i++) {
			audioBufs[i] = new AudioBuf(slea);
		}
	}

	/**
	 * A lazy-loaded equivalent of {@code ByteBuf}.
	 *
	 * @author Aaron Weiss
	 * @version 1.0
	 * @since 5/27/13
	 */
	private static class AudioBuf {
		private final SeekableLittleEndianAccessor slea;
		private final long audioOffset;
		private ByteBuf audioBuf;

		/**
		 * Creates a lazy-loaded {@code ByteBuf} for audio.
		 *
		 * @param slea
		 */
		public AudioBuf(SeekableLittleEndianAccessor slea) {
			this.slea = slea;
			this.audioOffset = slea.getLong();
		}

		/**
		 * Loads a {@code ByteBuf} of the desired {@code length}.
		 *
		 * @param length
		 *            the length of the audio
		 * @return the audio buffer
		 */
		public ByteBuf getAudioBuf(long length) {
			if (this.audioBuf == null) {
				this.slea.seek(this.audioOffset);
				this.audioBuf = Unpooled.wrappedBuffer(this.slea
						.getBytes((int) length));
			}
			return this.audioBuf;
		}
	}
}
