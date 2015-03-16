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
package tools.data.input;

import java.awt.Point;
import java.io.ByteArrayOutputStream;

/**
 * Provides a generic interface to a Little Endian stream of bytes.
 *
 * @version 1.0
 * @author Frz
 * @since Revision 323
 */
public class GenericLittleEndianAccessor implements LittleEndianAccessor {
	private final ByteInputStream bs;

	/**
	 * Class constructor - Wraps the accessor around a stream of bytes.
	 *
	 * @param bs
	 *            The byte stream to wrap the accessor around.
	 */
	public GenericLittleEndianAccessor(ByteInputStream bs) {
		this.bs = bs;
	}

	/**
	 * Read a single byte from the stream.
	 *
	 * @return The byte read.
	 * @see tools.data.input.ByteInputStream#readByte
	 */
	@Override
	public byte readByte() {
		return (byte) this.bs.readByte();
	}

	/**
	 * Reads an integer from the stream.
	 *
	 * @return The integer read.
	 */
	@Override
	public int readInt() {
		return this.bs.readByte() + (this.bs.readByte() << 8)
				+ (this.bs.readByte() << 16) + (this.bs.readByte() << 24);
	}

	/**
	 * Reads a short integer from the stream.
	 *
	 * @return The short read.
	 */
	@Override
	public short readShort() {
		return (short) (this.bs.readByte() + (this.bs.readByte() << 8));
	}

	/**
	 * Reads a single character from the stream.
	 *
	 * @return The character read.
	 */
	@Override
	public char readChar() {
		return (char) this.readShort();
	}

	/**
	 * Reads a long integer from the stream.
	 *
	 * @return The long integer read.
	 */
	@Override
	public long readLong() {
		final long byte1 = this.bs.readByte();
		final long byte2 = this.bs.readByte();
		final long byte3 = this.bs.readByte();
		final long byte4 = this.bs.readByte();
		final long byte5 = this.bs.readByte();
		final long byte6 = this.bs.readByte();
		final long byte7 = this.bs.readByte();
		final long byte8 = this.bs.readByte();
		return (byte8 << 56) + (byte7 << 48) + (byte6 << 40) + (byte5 << 32)
				+ (byte4 << 24) + (byte3 << 16) + (byte2 << 8) + byte1;
	}

	/**
	 * Reads a floating point integer from the stream.
	 *
	 * @return The float-type integer read.
	 */
	@Override
	public float readFloat() {
		return Float.intBitsToFloat(this.readInt());
	}

	/**
	 * Reads a double-precision integer from the stream.
	 *
	 * @return The double-type integer read.
	 */
	@Override
	public double readDouble() {
		return Double.longBitsToDouble(this.readLong());
	}

	/**
	 * Reads an ASCII string from the stream with length <code>n</code>.
	 *
	 * @param n
	 *            Number of characters to read.
	 * @return The string read.
	 */
	@Override
	public final String readAsciiString(int n) {
		final char ret[] = new char[n];
		for (int x = 0; x < n; x++) {
			ret[x] = (char) this.readByte();
		}
		return String.valueOf(ret);
	}

	/**
	 * Reads a null-terminated string from the stream.
	 *
	 * @return The string read.
	 */
	@Override
	public final String readNullTerminatedAsciiString() {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte b;
		while (true) {
			b = this.readByte();
			if (b == 0) {
				break;
			}
			baos.write(b);
		}
		final byte[] buf = baos.toByteArray();
		final char[] chrBuf = new char[buf.length];
		for (int x = 0; x < buf.length; x++) {
			chrBuf[x] = (char) buf[x];
		}
		return String.valueOf(chrBuf);
	}

	/**
	 * Gets the number of bytes read from the stream so far.
	 *
	 * @return A long integer representing the number of bytes read.
	 * @see tools.data.input.ByteInputStream#getBytesRead()
	 */
	@Override
	public long getBytesRead() {
		return this.bs.getBytesRead();
	}

	/**
	 * Reads a MapleStory convention lengthed ASCII string. This consists of a
	 * short integer telling the length of the string, then the string itself.
	 *
	 * @return The string read.
	 */
	@Override
	public String readMapleAsciiString() {
		return this.readAsciiString(this.readShort());
	}

	/**
	 * Reads <code>num</code> bytes off the stream.
	 *
	 * @param num
	 *            The number of bytes to read.
	 * @return An array of bytes with the length of <code>num</code>
	 */
	@Override
	public byte[] read(int num) {
		final byte[] ret = new byte[num];
		for (int x = 0; x < num; x++) {
			ret[x] = this.readByte();
		}
		return ret;
	}

	/**
	 * Reads a MapleStory Position information. This consists of 2 short
	 * integer.
	 *
	 * @return The Position read.
	 */
	@Override
	public final Point readPos() {
		final int x = this.readShort();
		final int y = this.readShort();
		return new Point(x, y);
	}

	/**
	 * Skips the current position of the stream <code>num</code> bytes ahead.
	 *
	 * @param num
	 *            Number of bytes to skip.
	 */
	@Override
	public void skip(int num) {
		for (int x = 0; x < num; x++) {
			this.readByte();
		}
	}

	/**
	 * @see tools.data.input.ByteInputStream#available
	 */
	@Override
	public long available() {
		return this.bs.available();
	}

	/**
	 * @see java.lang.Object#toString
	 */
	@Override
	public String toString() {
		return this.bs.toString();
	}
}