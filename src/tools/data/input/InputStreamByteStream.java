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

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides an abstract wrapper to a stream of bytes.
 *
 * @author Frz
 * @version 1.0
 * @since Revision 323
 */
public class InputStreamByteStream implements ByteInputStream {
	private final InputStream is;
	private long read = 0;

	/**
	 * Class constructor. Provide an input stream to wrap this around.
	 *
	 * @param is
	 *            The input stream to wrap this object around.
	 */
	public InputStreamByteStream(InputStream is) {
		this.is = is;
	}

	/**
	 * Reads the next byte from the stream.
	 *
	 * @return Then next byte in the stream.
	 */
	@Override
	public int readByte() {
		int temp;
		try {
			temp = this.is.read();
			if (temp == -1) {
				throw new RuntimeException("EOF");
			}
			this.read++;
			return temp;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the number of bytes read from the stream.
	 *
	 * @return The number of bytes read as a long integer.
	 */
	@Override
	public long getBytesRead() {
		return this.read;
	}

	/**
	 * Returns the number of bytes left in the stream.
	 *
	 * @return The number of bytes available for reading as a long integer.
	 */
	@Override
	public long available() {
		try {
			return this.is.available();
		} catch (final IOException e) {
			System.out.println("ERROR" + e);
			return 0;
		}
	}
}
